import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductDAO {

    public boolean createProduct(Product p) {
        String sql = "INSERT INTO products (name, description, price, stock_quantity, active, image_path) " +
                "VALUES (?, ?, ?, ?, 1, ?)";

        String imgPath = p.getImagePath();
        if (imgPath == null || imgPath.isEmpty()) {
            imgPath = "images/products/placeholder.png";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, p.getName());
            stmt.setString(2, p.getDescription());
            stmt.setDouble(3, p.getPrice());
            stmt.setInt(4, p.getStockQuantity());
            stmt.setString(5, imgPath);

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                p.setProductId(rs.getInt(1));
            }
            return true;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public Product findById(int id) {
        String sql = "SELECT * FROM products WHERE product_id = ? AND active = 1";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setPrice(rs.getDouble("price"));
                p.setStockQuantity(rs.getInt("stock_quantity"));
                p.setActive(rs.getBoolean("active"));
                p.setImagePath(rs.getString("image_path"));
                return p;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Product> listActiveProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE active = 1 ORDER BY product_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Product p = new Product();
                p.setProductId(rs.getInt("product_id"));
                p.setName(rs.getString("name"));
                p.setDescription(rs.getString("description"));
                p.setPrice(rs.getDouble("price"));
                p.setStockQuantity(rs.getInt("stock_quantity"));
                p.setActive(rs.getBoolean("active"));
                p.setImagePath(rs.getString("image_path"));
                list.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean updateStock(int productId, int newStock) {
        String sql = "UPDATE products SET stock_quantity = ? WHERE product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, newStock);
            stmt.setInt(2, productId);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteProduct(int productId) {
        String sql = "UPDATE products SET active = 0 WHERE product_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productId);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
