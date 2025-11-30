// User.java
public class User {
    private int userId;
    private String username;
    private String password;
    private String fullName;
    private String address;
    private String role;

    private boolean mustChangePassword;

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean b) { mustChangePassword = b; }


    // Getters and setters
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
