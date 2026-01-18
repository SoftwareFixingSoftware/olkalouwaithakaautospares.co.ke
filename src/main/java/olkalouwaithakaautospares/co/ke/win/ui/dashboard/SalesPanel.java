package olkalouwaithakaautospares.co.ke.win.ui.dashboard;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

public class SalesPanel extends JPanel {
    private BaseClient client;
    private ObjectMapper mapper;
    private UserSessionManager session;

    private List<Map<String, Object>> products = new ArrayList<>();
    private List<CartItem> cartItems = new ArrayList<>();
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel totalLabel;
    private JComboBox<String> paymentMethodCombo;
    private JPanel productGrid; // Reference to product grid panel
    private JTextField searchField;

    class CartItem {
        Integer productId;
        String productName;
        Double unitPrice;
        Integer quantity;
        Double total;

        CartItem(Integer productId, String productName, Double unitPrice, Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
            this.total = unitPrice * quantity;
        }
    }

    public SalesPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();
        loadProducts();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Main content - Split between product selection and cart
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);

        // Left side - Product selection
        JPanel productPanel = createProductPanel();

        // Right side - Cart and checkout
        JPanel cartPanel = createCartPanel();

        splitPane.setLeftComponent(productPanel);
        splitPane.setRightComponent(cartPanel);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(splitPane, BorderLayout.CENTER);
    }

    private void loadProducts() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private Exception error = null;
            private List<Map<String, Object>> loadedProducts = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String response = client.get("/api/secure/products");
                    if (response != null && !response.trim().isEmpty()) {
                        try {
                            // Try to parse as a list directly
                            loadedProducts = client.parseResponseList(response);
                        } catch (Exception e) {
                            // If that fails, try to get data from response
                            Map<String, Object> result = client.parseResponse(response);
                            if (result.containsKey("data") && result.get("data") instanceof List) {
                                @SuppressWarnings("unchecked")
                                List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
                                loadedProducts = data;
                            } else {
                                throw new Exception("Invalid products response format");
                            }
                        }
                    }
                } catch (Exception e) {
                    error = e;
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to load products: " + error.getMessage());
                } else {
                    products.clear();
                    products.addAll(loadedProducts);
                    updateProductGrid();
                    System.out.println("Loaded " + products.size() + " products");
                }
            }
        };
        worker.execute();
    }

    private void updateProductGrid() {
        if (productGrid == null) {
            System.err.println("Product grid is not initialized");
            return;
        }

        productGrid.removeAll();

        if (products.isEmpty()) {
            JLabel noProductsLabel = new JLabel("No products available", SwingConstants.CENTER);
            noProductsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            noProductsLabel.setForeground(new Color(150, 150, 150));
            productGrid.add(noProductsLabel);
        } else {
            for (Map<String, Object> product : products) {
                try {
                    String name = getStringValue(product, "name", "Unknown Product");
                    Double price = getDoubleValue(product, "minimumSellingPrice", 0.0);
                    Integer productId = getIntegerValue(product, "id", 0);

                    JButton productBtn = createProductButton(name, price, productId);
                    productGrid.add(productBtn);
                } catch (Exception e) {
                    System.err.println("Error creating product button: " + e.getMessage());
                }
            }
        }

        productGrid.revalidate();
        productGrid.repaint();
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    private Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Integer getIntegerValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null || !map.containsKey(key)) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Point of Sale");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(30, 33, 57));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton newSaleBtn = new JButton("New Sale");
        JButton refreshBtn = new JButton("Refresh Products");

        styleButton(newSaleBtn, new Color(76, 175, 80));
        styleButton(refreshBtn, new Color(33, 150, 243));

        newSaleBtn.addActionListener(e -> clearCart());
        refreshBtn.addActionListener(e -> loadProducts());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(newSaleBtn);

        header.add(title, BorderLayout.WEST);
        header.add(buttonPanel, BorderLayout.EAST);

        return header;
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
    }

    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Search bar
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Search products by name or SKU...");

        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(new Color(33, 150, 243));
        searchBtn.setForeground(Color.WHITE);
        searchBtn.setBorderPainted(false);
        searchBtn.setFocusPainted(false);
        searchBtn.addActionListener(e -> searchProducts(searchField.getText()));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);

        // Product grid
        productGrid = new JPanel(new GridLayout(0, 3, 10, 10));
        productGrid.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane(productGrid);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JButton createProductButton(String name, double price, Integer productId) {
        JButton button = new JButton("<html><center><b>" + name + "</b><br/>₦ " +
                String.format("%.2f", price) + "</center></html>");
        button.putClientProperty("productId", productId);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(20, 10, 20, 10)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);

        button.addActionListener(e -> {
            Integer id = (Integer) button.getClientProperty("productId");
            addToCart(id, name, price);
        });

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(245, 247, 250));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
            }
        });

        return button;
    }

    private void searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadProducts();
            return;
        }

        // Filter products based on query
        List<Map<String, Object>> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Map<String, Object> product : products) {
            String name = getStringValue(product, "name", "").toLowerCase();
            String sku = getStringValue(product, "sku", "").toLowerCase();

            if (name.contains(lowerQuery) || sku.contains(lowerQuery)) {
                filtered.add(product);
            }
        }

        // Update product grid with filtered products
        productGrid.removeAll();
        for (Map<String, Object> product : filtered) {
            try {
                String name = getStringValue(product, "name", "Unknown Product");
                Double price = getDoubleValue(product, "minimumSellingPrice", 0.0);
                Integer productId = getIntegerValue(product, "id", 0);

                JButton productBtn = createProductButton(name, price, productId);
                productGrid.add(productBtn);
            } catch (Exception e) {
                System.err.println("Error creating product button: " + e.getMessage());
            }
        }

        if (filtered.isEmpty()) {
            JLabel noResultsLabel = new JLabel("No products found for: " + query, SwingConstants.CENTER);
            noResultsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            noResultsLabel.setForeground(new Color(150, 150, 150));
            productGrid.add(noResultsLabel);
        }

        productGrid.revalidate();
        productGrid.repaint();
    }

    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Cart title
        JLabel cartTitle = new JLabel("Shopping Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cartTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        // Cart table
        String[] columns = {"Product", "Price", "Qty", "Total", ""};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 4; // Only quantity and remove are editable
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return Integer.class; // Quantity column
                return String.class;
            }
        };

        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cartTable.setRowHeight(35);
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Set column widths
        cartTable.getColumnModel().getColumn(0).setPreferredWidth(150); // Product
        cartTable.getColumnModel().getColumn(1).setPreferredWidth(80);  // Price
        cartTable.getColumnModel().getColumn(2).setPreferredWidth(50);  // Qty
        cartTable.getColumnModel().getColumn(3).setPreferredWidth(80);  // Total
        cartTable.getColumnModel().getColumn(4).setPreferredWidth(80);  // Remove

        cartTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        cartTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));

        // Customer info
        JPanel customerPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        customerPanel.setBorder(BorderFactory.createTitledBorder("Customer Information"));
        customerPanel.setBackground(new Color(250, 250, 250));

        JTextField phoneField = new JTextField();
        phoneField.putClientProperty("JTextField.placeholderText", "Phone number");

        JTextField nameField = new JTextField();
        nameField.putClientProperty("JTextField.placeholderText", "Name (optional)");

        paymentMethodCombo = new JComboBox<>(new String[]{"CASH", "CREDIT"});

        customerPanel.add(new JLabel("Phone:"));
        customerPanel.add(phoneField);
        customerPanel.add(new JLabel("Name:"));
        customerPanel.add(nameField);
        customerPanel.add(new JLabel("Payment:"));
        customerPanel.add(paymentMethodCombo);

        // Total and checkout
        JPanel checkoutPanel = new JPanel(new BorderLayout());
        checkoutPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        totalLabel = new JLabel("Total: ₦ 0.00", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton checkoutBtn = new JButton("Process Sale");
        checkoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        checkoutBtn.setBackground(new Color(76, 175, 80));
        checkoutBtn.setForeground(Color.WHITE);
        checkoutBtn.setBorderPainted(false);
        checkoutBtn.setFocusPainted(false);
        checkoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkoutBtn.addActionListener(e -> processSale(
                phoneField.getText(),
                nameField.getText(),
                (String) paymentMethodCombo.getSelectedItem()
        ));

        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.add(totalLabel, BorderLayout.CENTER);

        checkoutPanel.add(totalPanel, BorderLayout.CENTER);
        checkoutPanel.add(checkoutBtn, BorderLayout.SOUTH);

        panel.add(cartTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(customerPanel, BorderLayout.SOUTH);
        panel.add(checkoutPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void addToCart(Integer productId, String product, double price) {
        // Check if product already in cart
        for (CartItem item : cartItems) {
            if (item.productId.equals(productId)) {
                item.quantity++;
                item.total = item.unitPrice * item.quantity;
                updateCartTable();
                calculateTotal();
                return;
            }
        }

        // Add new item to cart
        cartItems.add(new CartItem(productId, product, price, 1));
        updateCartTable();
        calculateTotal();
    }

    private void updateCartTable() {
        cartModel.setRowCount(0);
        for (int i = 0; i < cartItems.size(); i++) {
            CartItem item = cartItems.get(i);
            cartModel.addRow(new Object[]{
                    item.productName,
                    String.format("₦ %.2f", item.unitPrice),
                    item.quantity,
                    String.format("₦ %.2f", item.total),
                    "Remove"
            });
        }
    }

    private void calculateTotal() {
        double total = cartItems.stream().mapToDouble(item -> item.total).sum();
        totalLabel.setText(String.format("Total: ₦ %.2f", total));
    }

    private void clearCart() {
        cartItems.clear();
        updateCartTable();
        calculateTotal();
    }

    private void processSale(String phone, String name, String paymentMethod) {
        if (cartItems.isEmpty()) {
            showError("Cart is empty!");
            return;
        }

        if (phone.trim().isEmpty()) {
            showError("Please enter customer phone number");
            return;
        }

        // Validate payment method
        if (!"CASH".equals(paymentMethod) && !"CREDIT".equals(paymentMethod)) {
            showError("Invalid payment method");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private String result;
            private boolean success;

            @Override
            protected Void doInBackground() {
                try {
                    // Prepare sale request
                    Map<String, Object> saleRequest = new HashMap<>();
                    saleRequest.put("customerPhone", phone);

                    List<Map<String, Object>> items = new ArrayList<>();
                    for (CartItem item : cartItems) {
                        Map<String, Object> saleItem = new HashMap<>();
                        saleItem.put("productId", item.productId);
                        saleItem.put("quantity", item.quantity);
                        saleItem.put("unitPrice", item.unitPrice);
                        saleItem.put("discount", 0);
                        items.add(saleItem);
                    }
                    saleRequest.put("items", items);
                    saleRequest.put("discountTotal", 0);

                    // Create sale
                    String saleResponse = client.post("/api/secure/sales", saleRequest);
                    Map<String, Object> saleResult = client.parseResponse(saleResponse);

                    success = client.isResponseSuccessful(saleResponse);
                    result = client.getResponseMessage(saleResponse);

                    if (success) {
                        // If cash payment, record payment
                        if ("CASH".equals(paymentMethod)) {
                            try {
                                // Extract sale ID - you might need to adjust this based on your API response
                                Map<String, Object> saleData = (Map<String, Object>) saleResult.get("data");
                                if (saleData != null && saleData.containsKey("id")) {
                                    Integer saleId = (Integer) saleData.get("id");

                                    // Create payment
                                    Map<String, Object> paymentRequest = new HashMap<>();
                                    paymentRequest.put("saleId", saleId);
                                    paymentRequest.put("paymentMethod", "CASH");
                                    paymentRequest.put("amount", calculateTotalAmount());
                                    paymentRequest.put("reference", "CASH-" + System.currentTimeMillis());

                                    client.post("/api/secure/payments", paymentRequest);
                                }
                            } catch (Exception e) {
                                System.err.println("Error recording payment: " + e.getMessage());
                                // Payment recording failed, but sale was successful
                            }
                        }
                    }

                } catch (Exception e) {
                    success = false;
                    result = "Error: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(SalesPanel.this, result,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearCart();
                    if (searchField != null) {
                        searchField.setText("");
                    }
                } else {
                    JOptionPane.showMessageDialog(SalesPanel.this, result,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private double calculateTotalAmount() {
        return cartItems.stream().mapToDouble(item -> item.total).sum();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Custom renderer and editor for remove button in table
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setText("Remove");
            setBackground(new Color(244, 67, 54));
            setForeground(Color.WHITE);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(new Color(244, 67, 54));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setText("Remove");
            button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            button.addActionListener(e -> {
                fireEditingStopped();
                if (row >= 0 && row < cartItems.size()) {
                    cartItems.remove(row);
                    updateCartTable();
                    calculateTotal();
                }
            });
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }

        public Object getCellEditorValue() {
            return "Remove";
        }
    }
}