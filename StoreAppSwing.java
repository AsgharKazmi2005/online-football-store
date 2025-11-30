import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

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
        int confirm = JOptionPane.showConfirmDialog(this,
                "Log out?", "Confirm", JOptionPane.YES_NO_OPTION);
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
            setLayout(new BorderLayout(20,20));
            setBorder(new EmptyBorder(50,200,50,200));

            JLabel title = new JLabel("Online Football Shirt Store",SwingConstants.CENTER);
            title.setFont(new Font("SansSerif",Font.BOLD,28));

            JPanel form = new JPanel(new GridLayout(0,1,8,8));
            form.add(new JLabel("Username:"));
            userField = new JTextField();
            form.add(userField);
            form.add(new JLabel("Password:"));
            passField = new JPasswordField();
            form.add(passField);

            JButton loginBtn = new JButton("Login");
            loginBtn.addActionListener(e->{
                User user = app.getUserDAO().findByUsername(userField.getText().trim());
                if(user!=null && user.getPassword().equals(new String(passField.getPassword()))) {
                    app.loginAs(user);
                } else {
                    JOptionPane.showMessageDialog(this,"Invalid login.","Error",JOptionPane.ERROR_MESSAGE);
                }
            });

            JButton signup = new JButton("Sign Up");
            signup.addActionListener(e -> {
                JTextField u=new JTextField(), f=new JTextField(), a=new JTextField();
                JPasswordField p=new JPasswordField();
                Object[] msg = {
                        "Username:",u,
                        "Password:",p,
                        "Full Name:",f,
                        "Address:",a
                };
                if(JOptionPane.showConfirmDialog(this,msg,"New Customer",
                        JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
                    User newUser = new User();
                    newUser.setUsername(u.getText().trim());
                    newUser.setPassword(new String(p.getPassword()));
                    newUser.setFullName(f.getText().trim());
                    newUser.setAddress(a.getText().trim());
                    newUser.setRole("CUSTOMER");
                    if(app.getUserDAO().createCustomer(newUser))
                        JOptionPane.showMessageDialog(this,"Account created!");
                }
            });

            JPanel buttons = new JPanel();
            buttons.add(loginBtn);
            buttons.add(signup);

            add(title,BorderLayout.NORTH);
            add(form,BorderLayout.CENTER);
            add(buttons,BorderLayout.SOUTH);
        }
    }


    // CUSTOMER PANEL BEGIN
    private static class CustomerPanel extends JPanel {

        private final StoreAppSwing app;
        private User user;
        private DefaultTableModel productsModel, cartModel;
        private JTable productsTable,cartTable;
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
            this.app=app;
            setLayout(new BorderLayout(10,10));
            JTabbedPane tabs = new JTabbedPane();
            tabs.add("Shop", shop());
            tabs.add("Orders", orders());
            tabs.add("Profile", profile());
            add(tabs,BorderLayout.CENTER);

            JButton logout = new JButton("Logout");
            logout.addActionListener(e->app.logout());
            add(logout,BorderLayout.NORTH);
        }

        public void setLoggedInUser(User u){
            this.user=u;
            refreshProducts();
            refreshOrders();
            loadProfile();
            refreshCart();
        }

        private JPanel shop() {
            JPanel p=new JPanel(new BorderLayout(5,5));
            productsModel=new DefaultTableModel(new Object[]{"ID","Name","Price","Stock"},0);
            productsTable=new JTable(productsModel);

            JScrollPane left=new JScrollPane(productsTable);
            p.add(left,BorderLayout.CENTER);

            cartModel=new DefaultTableModel(new Object[]{"Product","Qty","Price","Total"},0);
            cartTable=new JTable(cartModel);
            JScrollPane right=new JScrollPane(cartTable);
            right.setPreferredSize(new Dimension(400,300));
            p.add(right,BorderLayout.EAST);

            JPanel controls=new JPanel();
            qtySpinner=new JSpinner(new SpinnerNumberModel(1,1,99,1));
            JButton addBtn=new JButton("Add to Cart");
            addBtn.addActionListener(e->addCart());
            controls.add(new JLabel("Qty:"));
            controls.add(qtySpinner);
            controls.add(addBtn);

            totalLbl=new JLabel("Total: $0.00");
            JButton checkout=new JButton("Checkout");
            checkout.addActionListener(e->checkout());

            controls.add(totalLbl);
            controls.add(checkout);

            p.add(controls,BorderLayout.SOUTH);
            return p;
        }
        private void refreshProducts() {
            productsModel.setRowCount(0);
            for(Product p : app.getProductDAO().listActiveProducts()) {
                productsModel.addRow(new Object[]{
                        p.getProductId(),
                        p.getName(),
                        String.format("$%.2f",p.getPrice()),
                        p.getStockQuantity()
                });
            }
        }

        private void addCart() {
            int row=productsTable.getSelectedRow();
            if(row<0)return;
            int id=(int)productsModel.getValueAt(row,0);
            Product p=app.getProductDAO().findById(id);
            int qty=(int)qtySpinner.getValue();
            if(qty<=p.getStockQuantity()) {
                cart.add(new CartEntry(p,qty));
                refreshCart();
            }
        }

        private void refreshCart() {
            cartModel.setRowCount(0);
            double total=0;
            for(CartEntry ce:cart){
                double line=ce.p.getPrice()*ce.qty;
                total+=line;
                cartModel.addRow(new Object[]{ce.p.getName(),ce.qty,
                        String.format("$%.2f",ce.p.getPrice()),
                        String.format("$%.2f",line)});
            }
            totalLbl.setText(String.format("Total: $%.2f",total));
        }

        private void checkout() {
            if(cart.isEmpty())return;
            String code=JOptionPane.showInputDialog(this,
                    "Coupon code (optional):");
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
                for(CartEntry ce:cart){
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
            JPanel p=new JPanel(new BorderLayout());
            ordersModel=new DefaultListModel<>();
            ordersList=new JList<>(ordersModel);
            p.add(new JScrollPane(ordersList),BorderLayout.CENTER);

            JPanel bot=new JPanel();
            cancelField=new JTextField(8);
            JButton cancelBtn=new JButton("Cancel Order");
            cancelBtn.addActionListener(e->cancelOrder());
            bot.add(new JLabel("Order ID:"));
            bot.add(cancelField);
            bot.add(cancelBtn);
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

        // ===== Profile tab =====
        private JTextField nameF,addrF,userF;
        private JPasswordField passF;
        private JPanel profile() {
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.setBorder(new EmptyBorder(40,200,40,200));
            p.add(new JLabel("Full Name:")); nameF=new JTextField(); p.add(nameF);
            p.add(new JLabel("Address:")); addrF=new JTextField(); p.add(addrF);
            p.add(new JLabel("Username:")); userF=new JTextField(); p.add(userF);
            p.add(new JLabel("Password:")); passF=new JPasswordField(); p.add(passF);
            JButton save=new JButton("Save");
            save.addActionListener(e->saveProfile());
            p.add(new JLabel());p.add(save);
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
        DefaultListModel<String> ordersModel;
        JList<String> ordersList;
        JTextField codeF,valF,descC;
        JComboBox<String> typeC;

        public EmployeePanel(StoreAppSwing app) {
            this.app=app;
            setLayout(new BorderLayout(10,10));
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
            JPanel p=new JPanel(new BorderLayout());
            prodModel=new DefaultTableModel(new Object[]{"ID","Name","Price","Stock"},0);
            prodTable=new JTable(prodModel);
            p.add(new JScrollPane(prodTable),BorderLayout.CENTER);

            JPanel form=new JPanel(new GridLayout(0,2,5,5));
            nameF=new JTextField(); descF=new JTextField();
            priceF=new JTextField(); qtyF=new JTextField();
            form.add(new JLabel("Name:")); form.add(nameF);
            form.add(new JLabel("Desc:")); form.add(descF);
            form.add(new JLabel("Price:")); form.add(priceF);
            form.add(new JLabel("Stock:")); form.add(qtyF);
            JButton create=new JButton("Create Product");
            create.addActionListener(e->createProduct());
            form.add(new JLabel()); form.add(create);

            p.add(form,BorderLayout.SOUTH);
            return p;
        }

        private void refreshProducts(){
            prodModel.setRowCount(0);
            for(Product p:app.getProductDAO().listActiveProducts()){
                prodModel.addRow(new Object[]{
                        p.getProductId(),p.getName(),
                        String.format("$%.2f",p.getPrice()),
                        p.getStockQuantity()
                });
            }
        }

        private void createProduct(){
            try{
                Product p=new Product();
                p.setName(nameF.getText().trim());
                p.setDescription(descF.getText().trim());
                p.setPrice(Double.parseDouble(priceF.getText().trim()));
                p.setStockQuantity(Integer.parseInt(qtyF.getText().trim()));
                app.getProductDAO().createProduct(p);
                refreshProducts();
            } catch(Exception ignored){}
        }

        private JPanel customers() {
            JPanel p=new JPanel(new BorderLayout());
            custModel=new DefaultTableModel(new Object[]{"ID","Name","Username"},0);
            custTable=new JTable(custModel);
            p.add(new JScrollPane(custTable),BorderLayout.CENTER);
            return p;
        }

        private void refreshCustomers(){
            custModel.setRowCount(0);
            for(User u:app.getUserDAO().listCustomers()){
                custModel.addRow(new Object[]{
                        u.getUserId(),u.getFullName(),u.getUsername()
                });
            }
        }

        private JPanel orders() {
            JPanel p=new JPanel(new BorderLayout());
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

        private JPanel coupons() {
            JPanel p=new JPanel(new GridLayout(0,2,6,6));
            codeF=new JTextField();
            valF=new JTextField();
            descC=new JTextField();
            typeC=new JComboBox<>(new String[]{"PERCENT","FIXED"});

            p.add(new JLabel("Code:")); p.add(codeF);
            p.add(new JLabel("Value:")); p.add(valF);
            p.add(new JLabel("Type:")); p.add(typeC);
            p.add(new JLabel("Desc:")); p.add(descC);

            JButton c=new JButton("Create");
            c.addActionListener(e->createCoupon());
            p.add(new JLabel()); p.add(c);

            return p;
        }

        private void createCoupon(){
            try{
                Coupon c=new Coupon();
                c.setCode(codeF.getText().trim());
                c.setDiscountValue(Double.parseDouble(valF.getText().trim()));
                c.setDiscountType((String)typeC.getSelectedItem());
                c.setDescription(descC.getText().trim());
                app.getCouponDAO().createCoupon(c);
            }catch(Exception ignored){}
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
            setLayout(new BorderLayout(10,10));
            JTabbedPane tabs=new JTabbedPane();
            tabs.add("Employees & Users", employees());
            tabs.add("Orders", orders());
            add(tabs,BorderLayout.CENTER);

            JButton logout=new JButton("Logout");
            logout.addActionListener(e->app.logout());
            add(logout,BorderLayout.NORTH);
        }

        public void setLoggedInUser(User u){this.user=u;}

        private JPanel employees() {
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.setBorder(new EmptyBorder(20,200,20,200));

            empUser=new JTextField();
            empPass=new JTextField();
            empName=new JTextField();
            p.add(new JLabel("Username:")); p.add(empUser);
            p.add(new JLabel("Temp Password:")); p.add(empPass);
            p.add(new JLabel("Full Name:")); p.add(empName);

            JButton create=new JButton("Create Employee");
            create.addActionListener(e->createEmployee());
            p.add(new JLabel()); p.add(create);

            userIdDel=new JTextField();
            JButton del=new JButton("Delete User");
            del.addActionListener(e->deleteUser());
            p.add(new JLabel("Delete User ID:")); p.add(userIdDel);
            p.add(new JLabel()); p.add(del);

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
                JOptionPane.showMessageDialog(this,"Employee created: ID "+u.getUserId());
        }

        private void deleteUser(){
            try{
                int id=Integer.parseInt(userIdDel.getText().trim());
                if(app.getUserDAO().deleteUser(id))
                    JOptionPane.showMessageDialog(this,"User deleted.");
            }catch(Exception ignored){}
        }

        private JPanel orders() {
            JPanel p=new JPanel(new GridLayout(0,2,8,8));
            p.setBorder(new EmptyBorder(20,200,20,200));

            orderIdChg=new JTextField();
            String[] st={"PENDING","PROCESSING","SHIPPED","CANCELLED"};
            statusBox = new JComboBox<>(st);

            p.add(new JLabel("Order ID:")); p.add(orderIdChg);
            p.add(new JLabel("New Status:")); p.add(statusBox);

            JButton update=new JButton("Change Status");
            update.addActionListener(e->changeStatus());
            p.add(new JLabel()); p.add(update);

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
