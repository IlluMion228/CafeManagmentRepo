import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class CustomerFrame extends JFrame {
    private JTable table;
    private JTextField nameField, phoneField;
    private JButton insertButton, loadButton, switchButton, toOrdersBtn, deleteButton, editButton;

    public CustomerFrame() {
        setTitle("Клиенти");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        table = new JTable();
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());

        nameField = new JTextField(10);
        phoneField = new JTextField(10);
        insertButton = new JButton("Добавить");
        loadButton = new JButton("Обновить");
        switchButton = new JButton("Към менюто");
        toOrdersBtn = new JButton("Към поръчки");
        deleteButton = new JButton("Изтрий");
        editButton = new JButton("Редактирай");

        panel.add(new JLabel("Име:"));
        panel.add(nameField);
        panel.add(new JLabel("Телефон:"));
        panel.add(phoneField);
        panel.add(insertButton);
        panel.add(loadButton);
        panel.add(switchButton);
        panel.add(toOrdersBtn);
        panel.add(deleteButton);
        panel.add(editButton);
        add(panel, BorderLayout.SOUTH);
        
        setVisible(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        //-----------------------------------------
        //Adding clients
        //-----------------------------------------
        insertButton.addActionListener(e -> {
            try (Connection conn = DATABASE.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO Customers (name, phone) VALUES (?, ?)");
                ps.setString(1, nameField.getText());
                ps.setString(2, phoneField.getText());
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Клиент добавен.");
                loadData(); 
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Грешка при добавяне.");
            }
        });
        //-----------------------------------------
        //Refreshing table button
        //-----------------------------------------
        loadButton.addActionListener(e -> loadData());

        loadData();
        //-----------------------------------------
        //Switch button menu
        //-----------------------------------------
        switchButton.addActionListener(e -> {
            dispose();
            new MenuItemsFrame().setVisible(true);
        });
        //-----------------------------------------
        //Switch button to orders
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
                PreparedStatement ps = conn.prepareStatement("DELETE FROM Customers WHERE customer_id = ?");
                ps.setObject(1, id);
                ps.executeUpdate();
                loadData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
        //-----------------------------------------
        //selection of table
        //-----------------------------------------
        table.getSelectionModel().addListSelectionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                nameField.setText(table.getValueAt(row, 1).toString());
                phoneField.setText(table.getValueAt(row, 2).toString());
            }
        });
        //-----------------------------------------
        //Edit button
        //-----------------------------------------
        editButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Изберете ред за редакция.");
                return;
            }

            Object id = table.getValueAt(row, 0); // customer_id
            String name = nameField.getText().trim();
            String phone = phoneField.getText().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Попълнете всички полета.");
                return;
            }

            try (Connection conn = DATABASE.getConnection()) {
                PreparedStatement ps = conn.prepareStatement("UPDATE Customers SET name=?, phone=? WHERE customer_id=?");
                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setObject(3, id);
                ps.executeUpdate();
                loadData();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });


    }
    //-----------------------------------------
    //loading data from DB
    //-----------------------------------------
    private void loadData() {
        try (Connection conn = DATABASE.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Customers")) {
            table.setModel(new MyModel(rs));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    //-----------------------------------------
    //getting value from selected row
    //-----------------------------------------
    private Object getValueAtSelectedRow(int columnIndex) {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Моля, изберете ред.");
            return null;
        }
        return table.getValueAt(row, columnIndex);
    }

}
