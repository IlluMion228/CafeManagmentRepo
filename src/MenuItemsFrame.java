import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class MenuItemsFrame extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTable table;
    private JTextField nameField, priceField, categoryField;
    private JButton insertButton, loadButton, switchButton, toOrdersBtn, deleteButton, editButton;
    private JComboBox<String> categoryFilterBox;
    public MenuItemsFrame() {
        setTitle("Меню");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        table = new JTable();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel(new FlowLayout());

        nameField = new JTextField(10);
        priceField = new JTextField(5);
        categoryField = new JTextField(8);

        insertButton = new JButton("Добави");
        loadButton = new JButton("Обнови");
        switchButton = new JButton("Към клиентите");
        toOrdersBtn = new JButton("Към поръчки");
        deleteButton = new JButton("Изтрий");
        editButton = new JButton("Редактирай");
        categoryFilterBox = new JComboBox<>();
        categoryFilterBox.addItem("Всички категории");
        loadCategories(categoryFilterBox);
        panel.add(new JLabel("Име на ястие:"));
        panel.add(nameField);
        panel.add(new JLabel("Цена:"));
        panel.add(priceField);
        panel.add(new JLabel("Категория:"));
        panel.add(categoryField);
        panel.add(insertButton);
        panel.add(loadButton);
        panel.add(switchButton);
        panel.add(toOrdersBtn);
        panel.add(deleteButton);
        panel.add(editButton);
        

        add(panel, BorderLayout.SOUTH);
        //-----------------------------------------
        //window visibility 
        //-----------------------------------------
        setVisible(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        //-----------------------------------------
        //Adding button
        //-----------------------------------------
        insertButton.addActionListener(e -> {
            try (Connection conn = DATABASE.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO MenuItems (name, price, category) VALUES (?, ?, ?)");
                ps.setString(1, nameField.getText());
                ps.setBigDecimal(2, new java.math.BigDecimal(priceField.getText()));
                ps.setString(3, categoryField.getText());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Добавено успешно!");
                loadData();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Грешка при добавяне.");
            }
        });
        //-----------------------------------------
        //Update button
        //-----------------------------------------
        loadButton.addActionListener(e -> loadData());
        //-----------------------------------------
        //CustomerFrame switch button
        //-----------------------------------------
        switchButton.addActionListener(e -> {
            dispose(); // затваря текущия прозорец
            new CustomerFrame().setVisible(true);
        });
        //-----------------------------------------
        //OrderFrame switch button
        //-----------------------------------------
        toOrdersBtn.addActionListener(e -> {
            dispose();
            new OrdersFrame().setVisible(true);
        });
        //-----------------------------------------
        //Delete button
        //-----------------------------------------
        deleteButton.addActionListener(e -> {
            Object id = getValueAtSelectedRow(0);
            if (id == null) return;

            int confirm = JOptionPane.showConfirmDialog(this, "Сигурни ли сте?", "Потвърждение", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            try (Connection conn = DATABASE.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM MenuItems WHERE item_id = ?");
                ps.setObject(1, id);
                ps.executeUpdate();
                loadData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        //-----------------------------------------
        //Selection
        //-----------------------------------------
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                nameField.setText(table.getValueAt(row, 1).toString());
                priceField.setText(table.getValueAt(row, 2).toString());
            }
        });
        //-----------------------------------------
        //Edit button
        //-----------------------------------------
        editButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Изберете ред.");
                return;
            }

            Object id = table.getValueAt(row, 0); // item_id
            String name = nameField.getText().trim();
            String priceStr = priceField.getText().trim();

            if (name.isEmpty() || priceStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Попълнете всички полета.");
                return;
            }

            try {
                double price = Double.parseDouble(priceStr);
                try (Connection conn = DATABASE.getConnection()) {
                    PreparedStatement ps = conn.prepareStatement("UPDATE MenuItems SET name=?, price=? WHERE item_id=?");
                    ps.setString(1, name);
                    ps.setDouble(2, price);
                    ps.setObject(3, id);
                    ps.executeUpdate();
                    loadData();
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Невалидна цена.");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        //-----------------------------------------
        //Search button
        //-----------------------------------------
        JButton filterBtn = new JButton("Филтрирай");
        filterBtn.addActionListener(e -> {
            String selected = (String) categoryFilterBox.getSelectedItem();
            if (selected.equals("Всички категории")) {
                loadData();
            } else {
                filterByCategory(selected);
            }
        });
        
        panel.add(new JLabel("Филтър по категория:"));
        panel.add(categoryFilterBox);
        panel.add(filterBtn);
        
        loadData();
    }
    //-----------------------------------------
    //Loading from menuItems table
    //-----------------------------------------
    private void loadData() {
        try (Connection conn = DATABASE.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM MenuItems")) {
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
    
    private void loadCategories(JComboBox<String> box) {
        try (Connection conn = DATABASE.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT category FROM MenuItems")) {
            while (rs.next()) {
                box.addItem(rs.getString("category"));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void filterByCategory(String category) {
        try (Connection conn = DATABASE.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM MenuItems WHERE category = ?")) {
            ps.setString(1, category);
            ResultSet rs = ps.executeQuery();
            table.setModel(new MyModel(rs));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
