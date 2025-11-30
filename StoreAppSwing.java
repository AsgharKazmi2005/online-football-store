import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


public class StoreAppSwing extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainPanel = new JPanel(cardLayout);

    private final UserDAO userDAO = new UserDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final CouponDAO couponDAO = new CouponDAO();
    private final OrderDAO orderDAO = new OrderDAO();

    private User currentUser;

    private LoginPanel loginPanel;
    private CustomerPanel customerPanel;
    private EmployeePanel employeePanel;
    private AdminPanel adminPanel;

    private static void styleField(JTextField f) {
        f.setPreferredSize(new Dimension(150, 26));
        f.setMaximumSize(new Dimension(150, 26));
    }

    public StoreAppSwing() {
        setTitle("Online Football Shirt Store");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        initUI();
    }

    private void initUI() {
        loginPanel = new LoginPanel(this);
        customerPanel = new CustomerPanel(this);
        employeePanel = new EmployeePanel(this);
        adminPanel = new AdminPanel(this);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(customerPanel, "CUSTOMER");
        mainPanel.add(employeePanel, "EMPLOYEE");
        mainPanel.add(adminPanel, "ADMIN");

        add(mainPanel);
        showLogin();
    }

    public void showLogin() {
        currentUser = null;
        cardLayout.show(mainPanel, "LOGIN");
    }

    public UserDAO getUserDAO() { return userDAO; }
    public ProductDAO getProductDAO() { return productDAO; }
    public CouponDAO getCouponDAO() { return couponDAO; }
    public OrderDAO getOrderDAO() { return orderDAO; }
    public User getCurrentUser() { return currentUser; }

    public void loginAs(User user) {
        currentUser = user;
        switch (user.getRole()) {
            case "CUSTOMER":
                customerPanel.setLoggedInUser(user);
                cardLayout.show(mainPanel, "CUSTOMER");
                break;
            case "EMPLOYEE":
                employeePanel.setLoggedInUser(user);
                cardLayout.show(mainPanel, "EMPLOYEE");
                break;
            case "ADMIN":
                adminPanel.setLoggedInUser(user);
                cardLayout.show(mainPanel, "ADMIN");
                break;
            default:
                JOptionPane.showMessageDialog(this, "Invalid role.");
                showLogin();
        }
    }

    public void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Log out?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) showLogin();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StoreAppSwing().setVisible(true));
    }

    // LOGIN PANEL
    private static class LoginPanel extends JPanel {
        private final StoreAppSwing app;
        private final JTextField userField;
        private final JPasswordField passField;

        public LoginPanel(StoreAppSwing app) {
            this.app = app;
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(100, 300, 100, 300));

            JLabel title = new JLabel("Online Football Shirt Store", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 26));
            add(title, BorderLayout.NORTH);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(BorderFactory.createTitledBorder("Login"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.anchor = GridBagConstraints.WEST;

            gbc.gridx = 0; gbc.gridy = 0;
            form.add(new JLabel("Username:"), gbc);

            gbc.gridx = 1;
            userField = new JTextField();
            styleField(userField);
            form.add(userField, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            form.add(new JLabel("Password:"), gbc);

            gbc.gridx = 1;
            passField = new JPasswordField();
            styleField(passField);
            form.add(passField, gbc);

            JButton loginBtn = new JButton("Login");
            JButton signupBtn = new JButton("Sign Up");

            JPanel buttons = new JPanel();
            buttons.add(loginBtn);
            buttons.add(signupBtn);

            loginBtn.addActionListener(e -> {
                User user = app.getUserDAO().findByUsername(userField.getText().trim());
                if (user != null && user.getPassword().equals(new String(passField.getPassword()))) {
                    app.loginAs(user);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid login.");
                }
            });

            signupBtn.addActionListener(e -> {
                JTextField u = new JTextField(), f = new JTextField(), a = new JTextField();
                styleField(u); styleField(f); styleField(a);
                JPasswordField p = new JPasswordField();
                styleField(p);

                Object[] fields = { "Username:", u, "Password:", p, "Full Name:", f, "Address:", a };
                if (JOptionPane.showConfirmDialog(this, fields, "New Customer",
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    User newUser = new User();
                    newUser.setUsername(u.getText().trim());
                    newUser.setPassword(new String(p.getPassword()));
                    newUser.setFullName(f.getText().trim());
                    newUser.setAddress(a.getText().trim());
                    newUser.setRole("CUSTOMER");
                    if (app.getUserDAO().createCustomer(newUser))
                        JOptionPane.showMessageDialog(this, "Account created!");
                }
            });

            add(form, BorderLayout.CENTER);
            add(buttons, BorderLayout.SOUTH);
        }
    }

    // CUSTOMER PANEL
    private static class CustomerPanel extends JPanel {

        private final StoreAppSwing app;
        private User user;
        private DefaultTableModel productsModel, cartModel;
        private JTable productsTable, cartTable;
        private JSpinner qtySpinner;
        private JLabel totalLbl;
        private DefaultListModel<String> ordersModel;
        private JList<String> ordersList;
        private JTextField cancelField;

        private static class CartEntry {
            Product p; int qty;
            CartEntry(Product p,int q){this.p=p;this.qty=q;}
        }
        private final List<CartEntry> cart = new ArrayList<>();

        public CustomerPanel(StoreAppSwing app) {
            this.app = app;
            setLayout(new BorderLayout(8,8));
            JTabbedPane tabs = new JTabbedPane();
            tabs.add("Shop", shop());
            tabs.add("Orders", orders());
            tabs.add("Profile", profile());
            add(tabs, BorderLayout.CENTER);

            JButton logout = new JButton("Logout");
            logout.addActionListener(e -> app.logout());
            add(logout, BorderLayout.NORTH);
        }

        public void setLoggedInUser(User u){
            this.user = u;
            refreshProducts();
            refreshOrders();
            loadProfile();
            refreshCart();
        }

        private JPanel shop() {
            JPanel p = new JPanel(new BorderLayout(5,5));
            productsModel=new DefaultTableModel(new Object[]{"ID","Name","Price","Stock","Image"},0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 4) return ImageIcon.class;
                    return Object.class;
                }
                @Override
                public boolean isCellEditable(int row, int col) {
                    return false;
                }
            };            productsTable = new JTable(productsModel);
            p.add(new JScrollPane(productsTable), BorderLayout.CENTER);

            cartModel = new DefaultTableModel(new Object[]{"Product","Qty","Price","Total"},0);
            cartTable = new JTable(cartModel);
            JScrollPane right = new JScrollPane(cartTable);
            right.setPreferredSize(new Dimension(400,300));
            p.add(right,BorderLayout.EAST);

            JPanel controls = new JPanel();
            controls.setBorder(BorderFactory.createTitledBorder("Cart"));

            qtySpinner = new JSpinner(new SpinnerNumberModel(1,1,99,1));
            controls.add(new JLabel("Qty:"));
            controls.add(qtySpinner);

            JButton addBtn = new JButton("Add");
            addBtn.addActionListener(e->addCart());
            controls.add(addBtn);

            totalLbl = new JLabel("Total: $0.00");
            JButton checkout = new JButton("Checkout");
            checkout.addActionListener(e->checkout());

            controls.add(totalLbl);
            controls.add(checkout);

            p.add(controls, BorderLayout.SOUTH);
            return p;
        }

        private void refreshProducts() {
            productsModel.setRowCount(0);

            for (Product p : app.getProductDAO().listActiveProducts()) {
                String path = p.getImagePath();
                ImageIcon icon;

                if (path == null || path.isEmpty()) {
                    icon = new ImageIcon("placeholder.png"); // ensure this exists!
                } else {
                    icon = new ImageIcon(path);
                }

                Image scaled = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);

                productsModel.addRow(new Object[]{
                        p.getProductId(),
                        p.getName(),
                        String.format("$%.2f", p.getPrice()),
                        p.getStockQuantity(),
                        icon
                });
            }

            productsTable.setRowHeight(55);
        }


        private void addCart() {
            int row = productsTable.getSelectedRow();
            if(row<0)return;
            int id = (int)productsModel.getValueAt(row,0);
            Product p = app.getProductDAO().findById(id);
            int qty = (int)qtySpinner.getValue();
            if(qty<=p.getStockQuantity()) {
                cart.add(new CartEntry(p,qty));
                refreshCart();
            }
        }

        private void refreshCart() {
            cartModel.setRowCount(0);
            double total = 0;
            for (CartEntry ce : cart) {
                double line = ce.p.getPrice()*ce.qty;
                total+=line;
                cartModel.addRow(new Object[]{ ce.p.getName(), ce.qty,
                        String.format("$%.2f",ce.p.getPrice()),
                        String.format("$%.2f",line)});
            }
            totalLbl.setText(String.format("Total: $%.2f",total));
        }

        private void checkout() {
            if(cart.isEmpty())return;
            String code=JOptionPane.showInputDialog(this,"Coupon Code (optional):");
            Coupon c=null;
            if(code!=null && !code.trim().isEmpty()){
                c=app.getCouponDAO().findByCode(code.trim());
            }
            double total=cart.stream()
                    .mapToDouble(ce->ce.p.getPrice()*ce.qty)
                    .sum();
            Integer cId=null;
            if(c!=null){
                cId=c.getCouponId();
                if("PERCENT".equals(c.getDiscountType()))
                    total-=total*(c.getDiscountValue()/100.0);
                else total-=c.getDiscountValue();
                if(total<0) total=0;
            }
            int orderId=app.getOrderDAO().createOrder(user.getUserId(),cId,total);
            if(orderId>0){
                for (CartEntry ce : cart){
                    app.getOrderDAO().addOrderItem(orderId,
                            ce.p.getProductId(),ce.qty,ce.p.getPrice());
                    app.getProductDAO().updateStock(ce.p.getProductId(),
                            ce.p.getStockQuantity()-ce.qty);
                }
                JOptionPane.showMessageDialog(this,"Order placed! ID="+orderId);
                cart.clear();refreshCart();refreshProducts();refreshOrders();
            }
        }

        private JPanel orders() {
            JPanel p = new JPanel(new BorderLayout(5,5));
            p.setBorder(BorderFactory.createTitledBorder("Your Orders"));

            ordersModel=new DefaultListModel<>();
            ordersList=new JList<>(ordersModel);
            p.add(new JScrollPane(ordersList),BorderLayout.CENTER);

            JPanel bot=new JPanel(new GridBagLayout());
            GridBagConstraints gbc=new GridBagConstraints();
            gbc.insets=new Insets(5,5,5,5);
            gbc.anchor=GridBagConstraints.WEST;

            gbc.gridx=0; gbc.gridy=0;
            bot.add(new JLabel("Order ID:"),gbc);

            gbc.gridx=1;
            cancelField=new JTextField();
            styleField(cancelField);
            bot.add(cancelField,gbc);

            JButton cancelBtn=new JButton("Cancel");
            gbc.gridx=2;
            cancelBtn.addActionListener(e->cancelOrder());
            bot.add(cancelBtn,gbc);

            p.add(bot,BorderLayout.SOUTH);
            return p;
        }

        private void refreshOrders() {
            ordersModel.clear();
            for(String s: app.getOrderDAO().listOrdersForCustomer(user.getUserId()))
                ordersModel.addElement(s);
        }

        private void cancelOrder() {
            try {
                int id=Integer.parseInt(cancelField.getText().trim());
                if(app.getOrderDAO().cancelOrder(id,user.getUserId())){
                    refreshOrders();
                    JOptionPane.showMessageDialog(this,"Order canceled.");
                }
            } catch(Exception ignored){}
        }

        private JTextField nameF,addrF,userF;

        private JPasswordField passF;
        private JPanel profile() {
            JPanel p=new JPanel(new GridBagLayout());
            p.setBorder(BorderFactory.createTitledBorder("Profile"));
            GridBagConstraints gbc=new GridBagConstraints();
            gbc.insets=new Insets(6,6,6,6);
            gbc.anchor=GridBagConstraints.WEST;

            nameF=new JTextField(); styleField(nameF);
            addrF=new JTextField(); styleField(addrF);
            userF=new JTextField(); styleField(userF);
            passF=new JPasswordField(); styleField(passF);

            gbc.gridx=0; gbc.gridy=0;
            p.add(new JLabel("Full Name:"),gbc);
            gbc.gridx=1;
            p.add(nameF,gbc);

            gbc.gridx=0; gbc.gridy=1;
            p.add(new JLabel("Address:"),gbc);
            gbc.gridx=1;
            p.add(addrF,gbc);

            gbc.gridx=0; gbc.gridy=2;
            p.add(new JLabel("Username:"),gbc);
            gbc.gridx=1;
            p.add(userF,gbc);

            gbc.gridx=0; gbc.gridy=3;
            p.add(new JLabel("Password:"),gbc);
            gbc.gridx=1;
            p.add(passF,gbc);

            JButton save=new JButton("Save");
            gbc.gridx=1; gbc.gridy=4;
            save.addActionListener(e->saveProfile());
            p.add(save,gbc);

            return p;
        }

        private void loadProfile(){
            nameF.setText(user.getFullName());
            addrF.setText(user.getAddress());
            userF.setText(user.getUsername());
            passF.setText(user.getPassword());
        }

        private void saveProfile(){
            user.setFullName(nameF.getText().trim());
            user.setAddress(addrF.getText().trim());
            user.setUsername(userF.getText().trim());
            user.setPassword(new String(passF.getPassword()));
            if(app.getUserDAO().updateCustomerInfo(user))
                JOptionPane.showMessageDialog(this,"Saved!");
        }
    }

    // EMPLOYEE PANEL
    private static class EmployeePanel extends JPanel {
        private final StoreAppSwing app;
        private User user;
        JTable prodTable, custTable;
        DefaultTableModel prodModel,custModel;
        JTextField nameF,descF,priceF,qtyF;
        JLabel imgLbl;
        DefaultListModel<String> ordersModel;
        JList<String> ordersList;
        JTextField codeF,valF,descC;
        JComboBox<String> typeC;

        public EmployeePanel(StoreAppSwing app) {
            this.app=app;
            setLayout(new BorderLayout(8,8));
            JTabbedPane tabs=new JTabbedPane();
            tabs.add("Products",products());
            tabs.add("Customers",customers());
            tabs.add("Orders",orders());
            tabs.add("Coupons",coupons());
            add(tabs,BorderLayout.CENTER);

            JButton logout=new JButton("Logout");
            logout.addActionListener(e->app.logout());
            add(logout,BorderLayout.NORTH);
        }

        public void setLoggedInUser(User u){
            this.user=u;
            refreshProducts();
            refreshCustomers();
            refreshOrders();
        }

        private JPanel products() {
            JPanel p = new JPanel(new BorderLayout(3,3));
            p.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

            prodModel = new DefaultTableModel(new Object[]{"ID","Name","Price","Stock","Image"},0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 4) return ImageIcon.class;
                    return Object.class;
                }

                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };            prodTable = new JTable(prodModel);
            p.add(new JScrollPane(prodTable), BorderLayout.CENTER);

            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(BorderFactory.createTitledBorder("Create Product"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);
            gbc.anchor = GridBagConstraints.WEST;

            nameF = new JTextField(); styleField(nameF);
            descF = new JTextField(); styleField(descF);
            priceF = new JTextField(); styleField(priceF);
            qtyF = new JTextField(); styleField(qtyF);

            JButton imgBtn = new JButton("Upload Image");
            imgLbl = new JLabel("(none selected)");

            imgBtn.addActionListener(e -> {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileFilter(new FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));

                if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    try {
                        File f = chooser.getSelectedFile();
                        if (f != null && f.exists()) {
                            imgLbl.setText(f.getName());
                            imgLbl.putClientProperty("file", f.getAbsolutePath());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                                "Could not select file. Please choose a valid file location.",
                                "File Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });


            // ===== Ordered Layout Fix =====
            gbc.gridx = 0; gbc.gridy = 0;
            form.add(new JLabel("Name:"), gbc);
            gbc.gridx = 1;
            form.add(nameF, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            form.add(new JLabel("Desc:"), gbc);
            gbc.gridx = 1;
            form.add(descF, gbc);

            gbc.gridx = 0; gbc.gridy = 2;
            form.add(new JLabel("Price:"), gbc);
            gbc.gridx = 1;
            form.add(priceF, gbc);

            gbc.gridx = 0; gbc.gridy = 3;
            form.add(new JLabel("Stock:"), gbc);
            gbc.gridx = 1;
            form.add(qtyF, gbc);

            gbc.gridx = 0; gbc.gridy = 4;
            form.add(new JLabel("Image:"), gbc);
            gbc.gridx = 1;
            form.add(imgBtn, gbc);

            gbc.gridx = 0; gbc.gridy = 5;
            form.add(new JLabel("Selected:"), gbc);
            gbc.gridx = 1;
            form.add(imgLbl, gbc);

            gbc.gridx = 1; gbc.gridy = 6;
            JButton create = new JButton("Create");
            create.addActionListener(e -> createProduct());
            form.add(create, gbc);

            p.add(form, BorderLayout.SOUTH);
            return p;
        }


        private void refreshProducts() {
            prodModel.setRowCount(0);

            for (Product p : app.getProductDAO().listActiveProducts()) {

                String path = p.getImagePath();
                ImageIcon icon;

                if (path == null || path.isEmpty()) {
                    icon = new ImageIcon("placeholder.png"); // make sure this exists
                } else {
                    icon = new ImageIcon(path);
                }

                Image scaled = icon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                icon = new ImageIcon(scaled);

                prodModel.addRow(new Object[]{
                        p.getProductId(),
                        p.getName(),
                        String.format("$%.2f", p.getPrice()),
                        p.getStockQuantity(),
                        icon
                });
            }

            prodTable.setRowHeight(55);
        }


        private void createProduct() {
            try {
                Product p = new Product();
                p.setName(nameF.getText().trim());
                p.setDescription(descF.getText().trim());
                p.setPrice(Double.parseDouble(priceF.getText().trim()));
                p.setStockQuantity(Integer.parseInt(qtyF.getText().trim()));

                // --- Handle image copying into project folder ---
                String selectedPath = (String) imgLbl.getClientProperty("file");
                String copiedPath = null;

                if (selectedPath != null) {
                    try {
                        File src = new File(selectedPath);

                        // Ensure folder exists
                        File destDir = new File("images/products/");
                        destDir.mkdirs();

                        // Copy file into project’s images folder
                        File dest = new File(destDir, src.getName());
                        Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

                        copiedPath = dest.getPath();  // store relative path
                    } catch (Exception imgEx) {
                        imgEx.printStackTrace();
                    }
                }

                p.setImagePath(copiedPath);

                if (app.getProductDAO().createProduct(p)) {
                    JOptionPane.showMessageDialog(this, "Product created!");
                    refreshProducts(); // show new product in list
                }

            } catch (Exception ignored) {
                ignored.printStackTrace();
                JOptionPane.showMessageDialog(this,
                        "Invalid input. Please check values.",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }


        private JPanel customers() {
            JPanel p=new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createTitledBorder("Customers"));
            custModel=new DefaultTableModel(new Object[]{"ID","Name","Username"},0);
            custTable=new JTable(custModel);
            p.add(new JScrollPane(custTable),BorderLayout.CENTER);
            return p;
        }

        private void refreshCustomers(){
            custModel.setRowCount(0);
            for(User u:app.getUserDAO().listCustomers()){
                custModel.addRow(new Object[]{u.getUserId(),u.getFullName(),u.getUsername()});
            }
        }

        private JPanel orders() {
            JPanel p=new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createTitledBorder("Orders"));
            ordersModel=new DefaultListModel<>();
            ordersList=new JList<>(ordersModel);
            p.add(new JScrollPane(ordersList),BorderLayout.CENTER);
            return p;
        }

        private void refreshOrders(){
            ordersModel.clear();
            for(String s: app.getOrderDAO().listAllOrdersWithCustomerName())
                ordersModel.addElement(s);
        }

        private JTable couponTable;
        private DefaultTableModel couponModel;

        private JPanel coupons() {
            JPanel p = new JPanel(new BorderLayout());
            p.setBorder(BorderFactory.createTitledBorder("Create & View Coupons"));

            JPanel form = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6,6,6,6);
            gbc.anchor = GridBagConstraints.WEST;

            codeF = new JTextField(); styleField(codeF);
            valF = new JTextField(); styleField(valF);
            descC = new JTextField(); styleField(descC);
            typeC = new JComboBox<>(new String[]{"PERCENT","FIXED"});

            gbc.gridx=0; gbc.gridy=0; form.add(new JLabel("Code:"), gbc);
            gbc.gridx=1; form.add(codeF, gbc);

            gbc.gridx=0; gbc.gridy=1; form.add(new JLabel("Value:"), gbc);
            gbc.gridx=1; form.add(valF, gbc);

            gbc.gridx=0; gbc.gridy=2; form.add(new JLabel("Type:"), gbc);
            gbc.gridx=1; form.add(typeC, gbc);

            gbc.gridx=0; gbc.gridy=3; form.add(new JLabel("Desc:"), gbc);
            gbc.gridx=1; form.add(descC, gbc);

            JButton createBtn = new JButton("Create");
            gbc.gridx=1; gbc.gridy=4;
            createBtn.addActionListener(e -> createCoupon());
            form.add(createBtn, gbc);

            p.add(form, BorderLayout.NORTH);

            // === Coupon table below ===
            couponModel = new DefaultTableModel(new Object[]{"ID","Code","Type","Value"},0);
            couponTable = new JTable(couponModel);
            JScrollPane scroll = new JScrollPane(couponTable);
            scroll.setPreferredSize(new Dimension(400,200));
            p.add(scroll, BorderLayout.CENTER);

            refreshCoupons();
            return p;
        }

        private void refreshCoupons() {
            couponModel.setRowCount(0);
            for (Coupon c : app.getCouponDAO().listAllCoupons()) {
                couponModel.addRow(new Object[]{
                        c.getCouponId(), c.getCode(), c.getDiscountType(), c.getDiscountValue()
                });
            }
        }

        private void createCoupon(){
            try {
                Coupon c = new Coupon();
                c.setCode(codeF.getText().trim());
                c.setDiscountValue(Double.parseDouble(valF.getText().trim()));
                c.setDiscountType((String) typeC.getSelectedItem());
                c.setDescription(descC.getText().trim());

                if(app.getCouponDAO().createCoupon(c)){
                    JOptionPane.showMessageDialog(this,"Coupon created!");
                    refreshCoupons();
                }
            } catch(Exception ex){
                JOptionPane.showMessageDialog(this,"Invalid input.","Error",JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ADMIN PANEL
    private static class AdminPanel extends JPanel {
        private final StoreAppSwing app;
        private User user;

        private JTextField empUser, empPass, empName;
        private JTextField orderIdChg, userIdDel;
        private JComboBox<String> statusBox;

        public AdminPanel(StoreAppSwing app) {
            this.app = app;
            setLayout(new BorderLayout(8,8));
            JTabbedPane tabs=new JTabbedPane();
            tabs.add("Employees", employees());
            tabs.add("Orders", orders());
            add(tabs,BorderLayout.CENTER);

            JButton logout=new JButton("Logout");
            logout.addActionListener(e->app.logout());
            add(logout,BorderLayout.NORTH);
        }

        public void setLoggedInUser(User u){this.user=u;}

        private JPanel employees() {
            JPanel p=new JPanel(new GridBagLayout());
            p.setBorder(BorderFactory.createTitledBorder("Manage Users"));
            GridBagConstraints gbc=new GridBagConstraints();
            gbc.insets=new Insets(6,6,6,6);
            gbc.anchor=GridBagConstraints.WEST;

            empUser=new JTextField(); styleField(empUser);
            empPass=new JTextField(); styleField(empPass);
            empName=new JTextField(); styleField(empName);

            gbc.gridx=0; gbc.gridy=0;
            p.add(new JLabel("Username:"),gbc); gbc.gridx=1;
            p.add(empUser,gbc);

            gbc.gridx=0; gbc.gridy=1;
            p.add(new JLabel("Temp Password:"),gbc); gbc.gridx=1;
            p.add(empPass,gbc);

            gbc.gridx=0; gbc.gridy=2;
            p.add(new JLabel("Full Name:"),gbc); gbc.gridx=1;
            p.add(empName,gbc);

            JButton create=new JButton("Create");
            gbc.gridx=1; gbc.gridy=3;
            create.addActionListener(e->createEmployee());
            p.add(create,gbc);

            userIdDel=new JTextField(); styleField(userIdDel);

            gbc.gridx=0; gbc.gridy=4;
            p.add(new JLabel("Delete User ID:"),gbc);
            gbc.gridx=1;
            p.add(userIdDel,gbc);

            JButton del=new JButton("Delete");
            gbc.gridx=1; gbc.gridy=5;
            del.addActionListener(e->deleteUser());
            p.add(del,gbc);

            return p;
        }

        private void createEmployee(){
            if(empUser.getText().trim().isEmpty()) return;
            User u = new User();
            u.setUsername(empUser.getText().trim());
            u.setPassword(empPass.getText().trim());
            u.setFullName(empName.getText().trim());
            u.setRole("EMPLOYEE");

            if(app.getUserDAO().createEmployee(u))
                JOptionPane.showMessageDialog(this,"Employee created.");
        }

        private void deleteUser(){
            try{
                int id=Integer.parseInt(userIdDel.getText().trim());
                if(app.getUserDAO().deleteUser(id))
                    JOptionPane.showMessageDialog(this,"User deleted.");
            }catch(Exception ignored){}
        }

        private JPanel orders() {
            JPanel p=new JPanel(new GridBagLayout());
            p.setBorder(BorderFactory.createTitledBorder("Order Status"));
            GridBagConstraints gbc=new GridBagConstraints();
            gbc.insets=new Insets(6,6,6,6);
            gbc.anchor=GridBagConstraints.WEST;

            orderIdChg=new JTextField(); styleField(orderIdChg);
            statusBox=new JComboBox<>(new String[]{"PENDING","PROCESSING","SHIPPED","DELIVERED","CANCELLED"});

            gbc.gridx=0; gbc.gridy=0;
            p.add(new JLabel("Order ID:"),gbc); gbc.gridx=1;
            p.add(orderIdChg,gbc);

            gbc.gridx=0; gbc.gridy=1;
            p.add(new JLabel("Status:"),gbc); gbc.gridx=1;
            p.add(statusBox,gbc);

            JButton update=new JButton("Update");
            gbc.gridx=1; gbc.gridy=2;
            update.addActionListener(e->changeStatus());
            p.add(update,gbc);

            return p;
        }

        private void changeStatus(){
            try{
                int id=Integer.parseInt(orderIdChg.getText().trim());
                String s=(String)statusBox.getSelectedItem();
                app.getOrderDAO().changeOrderStatus(id,s);
                JOptionPane.showMessageDialog(this,"Updated!");
            }catch(Exception ignored){}
        }
    }
}
