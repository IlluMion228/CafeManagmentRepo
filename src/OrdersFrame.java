import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;

public class OrdersFrame extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTable table;
    private JComboBox<String> customerCombo;
    private JComboBox<String> itemCombo;
    private JTextField quantityField, customerFilterField, itemFilterField;
    private JButton addOrderButton, loadButton, toCustomersBtn, toMenuBtn, searchButton, deleteButton, statsButton;

    private Map<String, Integer> customerMap = new HashMap<>();
    private Map<String, Integer> itemMap = new HashMap<>();

    public OrdersFrame() {
        setTitle("Поръчки");
        setSize(700, 450);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        table = new JTable();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel(new FlowLayout());

        customerCombo = new JComboBox<>();
        itemCombo = new JComboBox<>();
        quantityField = new JTextField(3);

        addOrderButton = new JButton("Добави поръчка");
        loadButton = new JButton("Обнови");

        toCustomersBtn = new JButton("Към клиентите");
        toMenuBtn = new JButton("Към менюто");
        deleteButton = new JButton("Изтрий");
        statsButton = new JButton("Статистика");

        panel.add(new JLabel("Клиент:"));
        panel.add(customerCombo);
        panel.add(new JLabel("Продукт:"));
        panel.add(itemCombo);
        panel.add(new JLabel("Брой:"));
        panel.add(quantityField);
        panel.add(addOrderButton);
        panel.add(loadButton);
        panel.add(toCustomersBtn);
        panel.add(toMenuBtn);
        panel.add(deleteButton);
        panel.add(statsButton);

        panel.add(new JLabel("Филтър: клиент"));
        customerFilterField = new JTextField(8);
        panel.add(customerFilterField);

        panel.add(new JLabel("продукт"));
        itemFilterField = new JTextField(8);
        panel.add(itemFilterField);

        searchButton = new JButton("Търси");
        panel.add(searchButton);

        add(panel, BorderLayout.SOUTH);

        setVisible(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        addOrderButton.addActionListener(e -> addOrder());
        loadButton.addActionListener(e -> loadData());

        toCustomersBtn.addActionListener(e -> {
            dispose();
            new CustomerFrame().setVisible(true);
        });

        toMenuBtn.addActionListener(e -> {
            dispose();
            new MenuItemsFrame().setVisible(true);
        });
        
        searchButton.addActionListener(e -> filterOrders());
        
        deleteButton.addActionListener(e -> {
            Object id = getValueAtSelectedRow(0); // order_id
            if (id == null) return;

            int confirm = JOptionPane.showConfirmDialog(this, "Сигурни ли сте?", "Потвърждение", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            try (Connection conn = DATABASE.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM Orders WHERE order_id = ?");
                ps.setObject(1, id);
                ps.executeUpdate();
                loadData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        
        //-----------------------------------------
        //Status button
        //-----------------------------------------
        statsButton.addActionListener(e -> new StatisticsFrame());
        
        loadData();
        loadCombos();
        loadData();
    }

    private void loadCombos() {
        try (Connection conn = DATABASE.getConnection()) {
            // Load customers
            customerCombo.removeAllItems();
            customerMap.clear();
            ResultSet rs = conn.createStatement().executeQuery("SELECT customer_id, name FROM Customers");
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("customer_id");
                customerCombo.addItem(name);
                customerMap.put(name, id);
            }

            // Load menu items
            itemCombo.removeAllItems();
            itemMap.clear();
            rs = conn.createStatement().executeQuery("SELECT item_id, name FROM MenuItems");
            while (rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("item_id");
                itemCombo.addItem(name);
                itemMap.put(name, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        String sql = "SELECT o.order_id, c.name AS customer, o.order_date, m.name AS item, oi.quantity " +
                     "FROM Orders o " +
                     "JOIN Customers c ON o.customer_id = c.customer_id " +
                     "JOIN OrderItems oi ON o.order_id = oi.order_id " +
                     "JOIN MenuItems m ON oi.item_id = m.item_id";
        try (Connection conn = DATABASE.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            table.setModel(new MyModel(rs));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addOrder() {
        String customerName = (String) customerCombo.getSelectedItem();
        String itemName = (String) itemCombo.getSelectedItem();
        String quantityText = quantityField.getText();

        if (customerName == null || itemName == null || quantityText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Моля, попълнете всички полета.");
            return;
        }

        try {
            int customerId = customerMap.get(customerName);
            int itemId = itemMap.get(itemName);
            int qty = Integer.parseInt(quantityText);

            try (Connection conn = DATABASE.getConnection()) {
                conn.setAutoCommit(false);

                // Insert into Orders
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO Orders (customer_id, order_date) VALUES (?, CURRENT_DATE)", Statement.RETURN_GENERATED_KEYS);
                ps.setInt(1, customerId);
                ps.executeUpdate();

                ResultSet generated = ps.getGeneratedKeys();
                if (generated.next()) {
                    int orderId = generated.getInt(1);

                    // Insert into OrderItems
                    ps = conn.prepareStatement("INSERT INTO OrderItems (order_id, item_id, quantity) VALUES (?, ?, ?)");
                    ps.setInt(1, orderId);
                    ps.setInt(2, itemId);
                    ps.setInt(3, qty);
                    ps.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Поръчка добавена успешно.");
                loadData();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Грешка при добавяне на поръчка.");
        }
    }
    private void filterOrders() {
        String customer = customerFilterField.getText().trim();
        String item = itemFilterField.getText().trim();

        String sql = "SELECT o.order_id, c.name AS customer, o.order_date, m.name AS item, oi.quantity " +
                     "FROM Orders o " +
                     "JOIN Customers c ON o.customer_id = c.customer_id " +
                     "JOIN OrderItems oi ON o.order_id = oi.order_id " +
                     "JOIN MenuItems m ON oi.item_id = m.item_id " +
                     "WHERE 1=1 ";

        if (!customer.isEmpty()) {
            sql += "AND LOWER(c.name) LIKE LOWER('%" + customer + "%') ";
        }
        if (!item.isEmpty()) {
            sql += "AND LOWER(m.name) LIKE LOWER('%" + item + "%') ";
        }

        try (Connection conn = DATABASE.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            table.setModel(new MyModel(rs));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private Object getValueAtSelectedRow(int columnIndex) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете ред.");
            return null;
        }
        return table.getValueAt(row, columnIndex);
    }


}
