import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StatisticsFrame extends JFrame {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JLabel totalOrdersLabel, topItemLabel, totalRevenueLabel;
    private JButton backButton;

    public StatisticsFrame() {
        setTitle("Статистика");
        setSize(500, 300);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(4, 1, 10, 10));

        totalOrdersLabel = new JLabel();
        topItemLabel = new JLabel();
        totalRevenueLabel = new JLabel();

        backButton = new JButton("Назад");
        backButton.addActionListener(e -> dispose());

        add(totalOrdersLabel);
        add(topItemLabel);
        add(totalRevenueLabel);
        add(backButton);

        loadStatistics();

        setVisible(true);
        setLocationRelativeTo(null);
    }

    private void loadStatistics() {
        try (Connection conn = DATABASE.getConnection()) {
            //-----------------------------------------
            //total orders
            //-----------------------------------------
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) AS total FROM Orders")) {
                if (rs.next()) {
                    totalOrdersLabel.setText("Общо поръчки: " + rs.getInt("total"));
                }
            }

            //-----------------------------------------
            //most popular dish
            //-----------------------------------------
            String topQuery = """
                SELECT m.name, SUM(oi.quantity) AS total_qty
                FROM OrderItems oi
                JOIN MenuItems m ON oi.item_id = m.item_id
                GROUP BY m.name
                ORDER BY total_qty DESC
                LIMIT 1
            """;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(topQuery)) {
                if (rs.next()) {
                    topItemLabel.setText("Най-продавано ястие: " + rs.getString("name") + " (" + rs.getInt("total_qty") + " броя)");
                } else {
                    topItemLabel.setText("Няма данни за ястия.");
                }
            }

            //-----------------------------------------
            //total sum
            //-----------------------------------------            
            String revenueQuery = """
                SELECT SUM(m.price * oi.quantity) AS revenue
                FROM OrderItems oi
                JOIN MenuItems m ON oi.item_id = m.item_id
            """;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(revenueQuery)) {
                if (rs.next()) {
                    totalRevenueLabel.setText("Общо приходи: " + rs.getBigDecimal("revenue") + " лв.");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Грешка при зареждане на статистика.");
        }
    }
}
