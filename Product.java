// Product.java
public class Product {
    private int productId;
    private String name;
    private String description;
    private double price;
    private int stockQuantity;
    private boolean active;
    private String imagePath;


    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public String getName() { return name; }
    public String getImagePath() { return imagePath; }

    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
}
