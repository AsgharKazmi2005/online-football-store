// CouponDAO.java
import java.sql.*;

public class CouponDAO {

    public boolean createCoupon(Coupon c) {
        String sql = "INSERT INTO coupons (code, description, discount_type, discount_value, active) " +
                "VALUES (?, ?, ?, ?, 1)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, c.getCode());
            stmt.setString(2, c.getDescription());
            stmt.setString(3, c.getDiscountType());
            stmt.setDouble(4, c.getDiscountValue());

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    c.setCouponId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Coupon findByCode(String code) {
        String sql = "SELECT * FROM coupons WHERE code = ? AND active = 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Coupon c = new Coupon();
                c.setCouponId(rs.getInt("coupon_id"));
                c.setCode(rs.getString("code"));
                c.setDescription(rs.getString("description"));
                c.setDiscountType(rs.getString("discount_type"));
                c.setDiscountValue(rs.getDouble("discount_value"));
                return c;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
