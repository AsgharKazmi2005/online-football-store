import java.sql.*;

public class DBConnection {

    public static Connection connection;

    // Load driver and create initial connection once
    public static void connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost/online_store?serverTimezone=EST",
                    "root",
                    "@Asghar786"
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Safe method used by DAOs to always get a live connection
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect(); // reconnect if needed
        }
        return connection;
    }
}
