import java.sql.Connection;
import java.sql.Statement;

public class MainClass {
    public static void main(String[] args) {
        try (Connection conn = DATABASE.getConnection()) {
            Statement stmt = conn.createStatement();

        } catch (Exception e) {
            e.printStackTrace();
        }

        javax.swing.SwingUtilities.invokeLater(() -> {
            new CustomerFrame().setVisible(true);
        });
    }
}
