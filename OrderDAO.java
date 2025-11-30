// OrderDAO.java
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDAO {

    public int createOrder(int customerId, Integer couponId, double totalAmount) {
        String sql = "INSERT INTO orders (customer_id, coupon_id, total_amount) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, customerId);
            if (couponId == null) {
                stmt.setNull(2, Types.INTEGER);
            } else {
                stmt.setInt(2, couponId);
            }
            stmt.setDouble(3, totalAmount);

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }
    public List<String> getOrderDetails(int orderId) {
        List<String> list = new ArrayList<>();

        String sql = "SELECT p.name, oi.quantity, oi.unit_price, oi.line_total, o.coupon_id, c.code, c.discount_type, c.discount_value " +
                "FROM order_items oi " +
                "JOIN products p ON oi.product_id = p.product_id " +
                "JOIN orders o ON oi.order_id = o.order_id " +
                "LEFT JOIN coupons c ON o.coupon_id = c.coupon_id " +
                "WHERE oi.order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            while(rs.next()) {
                String item = String.format("%s x%d  @ $%.2f → $%.2f",
                        rs.getString("p.name"),
                        rs.getInt("oi.quantity"),
                        rs.getDouble("oi.unit_price"),
                        rs.getDouble("oi.line_total"));
                list.add(item);
            }

            // Add coupon info only once
            rs.beforeFirst();
            if(rs.next() && rs.getString("code") != null) {
                String discount = String.format("Coupon Applied: %s (%.2f %s OFF)",
                        rs.getString("code"),
                        rs.getDouble("discount_value"),
                        rs.getString("discount_type").equals("PERCENT") ? "%" : "$");
                list.add(discount);
            }

        } catch(Exception e) { e.printStackTrace(); }

        return list;
    }

    public boolean addOrderItem(int orderId, int productId, int quantity, double unitPrice) {
        double lineTotal = unitPrice * quantity;
        String sql = "INSERT INTO order_items (order_id, product_id, quantity, unit_price, line_total) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.setDouble(4, unitPrice);
            stmt.setDouble(5, lineTotal);

            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<String> listAllOrdersWithCustomerName() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT o.order_id, u.full_name, o.status, o.total_amount, o.created_at " +
                "FROM orders o JOIN users u ON o.customer_id = u.user_id " +
                "ORDER BY u.full_name";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String row = "Order #" + rs.getInt("order_id") +
                        " | Customer: " + rs.getString("full_name") +
                        " | Status: " + rs.getString("status") +
                        " | Total: " + rs.getDouble("total_amount") +
                        " | At: " + rs.getTimestamp("created_at");
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<String> listOrdersForCustomer(int customerId) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT order_id, status, total_amount, created_at " +
                "FROM orders WHERE customer_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String row = "Order #" + rs.getInt("order_id") +
                        " | Status: " + rs.getString("status") +
                        " | Total: " + rs.getDouble("total_amount") +
                        " | At: " + rs.getTimestamp("created_at");
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean cancelOrder(int orderId, int customerId) {
        String sql = "DELETE FROM orders WHERE order_id = ? AND customer_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            stmt.setInt(2, customerId);
            int rows = stmt.executeUpdate();
            return rows > 0; // order deleted (and items via FK)
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean changeOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setInt(2, orderId);
            int rows = stmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ADMIN — Delete an entire order (items removed by FK CASCADE)
    public boolean deleteOrder(int orderId) {
        String sql = "DELETE FROM orders WHERE order_id=?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public List<OrderItem> getOrderItems(int orderId) {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT product_id, quantity, unit_price, line_total FROM order_items WHERE order_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                OrderItem item = new OrderItem();
                item.setProductId(rs.getInt("product_id"));
                item.setQuantity(rs.getInt("quantity"));
                item.setUnitPrice(rs.getDouble("unit_price"));
                item.setLineTotal(rs.getDouble("line_total"));
                items.add(item);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
    public Coupon getCouponForOrder(int orderId) {
        String sql = "SELECT c.coupon_id, c.code, c.discount_type, c.discount_value " +
                "FROM orders o " +
                "JOIN coupons c ON o.coupon_id = c.coupon_id " +
                "WHERE o.order_id = ? AND o.coupon_id IS NOT NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, orderId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Coupon c = new Coupon();
                c.setCouponId(rs.getInt("coupon_id"));
                c.setCode(rs.getString("code"));
                c.setDiscountType(rs.getString("discount_type"));
                c.setDiscountValue(rs.getDouble("discount_value"));
                return c;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
