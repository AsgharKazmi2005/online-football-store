// UserDAO.java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
public class UserDAO {

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public User findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRowToUser(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean createCustomer(User user) {
        String sql = "INSERT INTO users (username, password, full_name, address, role) " +
                "VALUES (?, ?, ?, ?, 'CUSTOMER')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getAddress());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    user.setUserId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createEmployee(User u) {
        String sql = "INSERT INTO users (username, password, full_name, role, must_change_password) " +
                "VALUES (?, ?, ?, 'EMPLOYEE', 1)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, u.getUsername());
            stmt.setString(2, u.getPassword());
            stmt.setString(3, u.getFullName());

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public void updatePasswordAndDisableFlag(User u) {
        String sql = "UPDATE users SET password=?, must_change_password=0 WHERE user_id=?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, u.getPassword());
            stmt.setInt(2, u.getUserId());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<User> listCustomers() {
        List<User> customers = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = 'CUSTOMER' ORDER BY full_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                customers.add(mapRowToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return customers;
    }

    public boolean updateCustomerInfo(User user) {
        String sql = "UPDATE users SET full_name = ?, address = ?, username = ?, password = ? " +
                "WHERE user_id = ? AND role = 'CUSTOMER'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getAddress());
            stmt.setString(3, user.getUsername());
            stmt.setString(4, user.getPassword());
            stmt.setInt(5, user.getUserId());

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, userId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private User mapRowToUser(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPassword(rs.getString("password"));
        u.setFullName(rs.getString("full_name"));
        u.setAddress(rs.getString("address"));
        u.setRole(rs.getString("role"));

        try {
            u.setMustChangePassword(rs.getBoolean("must_change_password"));
        } catch (SQLException ex) {
            u.setMustChangePassword(false);
        }

        return u;
    }
    // ADMIN — Update Customer (full name, address, username, password)
    public boolean updateCustomerByAdmin(int userId, String fullName, String address,
                                         String username, String password) {
        String sql = "UPDATE users SET full_name=?, address=?, username=?, password=? " +
                "WHERE user_id=? AND role='CUSTOMER'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, address);
            stmt.setString(3, username);
            stmt.setString(4, password);
            stmt.setInt(5, userId);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ADMIN — Update Employee (full name, username, password)
    public boolean updateEmployeeByAdmin(int userId, String fullName,
                                         String username, String password) {
        String sql = "UPDATE users SET full_name=?, username=?, password=? " +
                "WHERE user_id=? AND role='EMPLOYEE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fullName);
            stmt.setString(2, username);
            stmt.setString(3, password);
            stmt.setInt(4, userId);

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
