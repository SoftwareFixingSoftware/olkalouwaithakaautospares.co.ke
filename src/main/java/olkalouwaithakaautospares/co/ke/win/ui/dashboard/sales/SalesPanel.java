package olkalouwaithakaautospares.co.ke.win.ui.dashboard.sales;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * SalesPanel — POS + Sales/Payments history pane.
 *
 * Left: product grid (unchanged)
 * Right: tabbed pane with:
 *   - "Cart" (point-of-sale)
 *   - "Sales" (recent sales table + payments view + record-payment action)
 */
@SuppressWarnings("unchecked")
public class SalesPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    private final List<Map<String, Object>> products = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();
    private final List<Map<String, Object>> recentSales = new ArrayList<>(); // local cache (server-authoritative)
    private final List<Map<String, Object>> recentPayments = new ArrayList<>(); // cache for payments of selected sale

    // POS components
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel totalLabel;
    private JComboBox<String> paymentMethodCombo;
    private JPanel productGrid;
    private JTextField searchField;

    // Sales/Payments components
    private JTable salesTable;
    private DefaultTableModel salesModel;
    private JTable paymentsTable;
    private DefaultTableModel paymentsModel;
    private JLabel selectedSaleLabel;

    // Date formatter
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Cart item model
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
            this.total = round(unitPrice * quantity);
        }
    }

    public SalesPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();
        loadProducts();
        loadRecentSales(); // load authoritative sales from DB on start
    }

    // ---------- UI Initialization ----------
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Left product panel
        JPanel productPanel = createProductPanel();

        // Right: tabbed pane containing Cart + Sales history
        JTabbedPane rightTabs = new JTabbedPane();
        JPanel cartPanel = createCartPanel();
        JPanel salesHistoryPanel = createSalesHistoryPanel();

        rightTabs.addTab("Cart", cartPanel);
        rightTabs.addTab("Sales", salesHistoryPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, productPanel, rightTabs);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Point of Sale");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(30, 33, 57));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        JButton newSaleBtn = new JButton("New Sale");
        JButton refreshBtn = new JButton("Refresh Products & Sales");

        styleButton(newSaleBtn, new Color(76, 175, 80));
        styleButton(refreshBtn, new Color(33, 150, 243));

        newSaleBtn.addActionListener(e -> clearCart());
        refreshBtn.addActionListener(e -> {
            loadProducts();
            loadRecentSales();
        });

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
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    // ---------- Product grid ----------
    private JPanel createProductPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Search
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Search products by name or SKU...");

        JButton searchBtn = new JButton("Search");
        styleButton(searchBtn, new Color(33, 150, 243));
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
                String.format("%,.2f", price) + "</center></html>");
        button.putClientProperty("productId", productId);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setBackground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(18, 8, 18, 8)
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
        List<Map<String, Object>> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Map<String, Object> product : products) {
            String name = getStringValue(product, "name", "").toLowerCase();
            String sku = getStringValue(product, "sku", "").toLowerCase();
            if (name.contains(lowerQuery) || sku.contains(lowerQuery)) filtered.add(product);
        }
        productGrid.removeAll();
        if (filtered.isEmpty()) {
            JLabel noResultsLabel = new JLabel("No products found for: " + query, SwingConstants.CENTER);
            noResultsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            noResultsLabel.setForeground(new Color(150, 150, 150));
            productGrid.add(noResultsLabel);
        } else {
            for (Map<String, Object> product : filtered) {
                String name = getStringValue(product, "name", "Unknown Product");
                Double price = getDoubleValue(product, "minimumSellingPrice", 0.0);
                Integer productId = getIntegerValue(product, "id", 0);
                productGrid.add(createProductButton(name, price, productId));
            }
        }
        productGrid.revalidate();
        productGrid.repaint();
    }

    // ---------- Cart / Checkout ----------
    private JPanel createCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel cartTitle = new JLabel("Shopping Cart");
        cartTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cartTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        String[] columns = {"Product", "Price", "Qty", "Total", ""};
        cartModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 || column == 4;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 2) return Integer.class;
                return String.class;
            }
        };

        cartTable = new JTable(cartModel);
        cartTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cartTable.setRowHeight(36);
        cartTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        cartTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        cartTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));

        cartModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int col = e.getColumn();
                    if (col == 2 && row >= 0 && row < cartItems.size()) {
                        Object val = cartModel.getValueAt(row, 2);
                        int qty = 1;
                        try {
                            if (val instanceof Number) qty = ((Number) val).intValue();
                            else qty = Integer.parseInt(val.toString());
                        } catch (Exception ex) { qty = 1; }
                        if (qty < 1) qty = 1;
                        CartItem item = cartItems.get(row);
                        item.quantity = qty;
                        item.total = round(item.unitPrice * item.quantity);
                        cartModel.setValueAt(String.format("₦ %,.2f", item.total), row, 3);
                        calculateTotal();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(cartTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));

        // bottom area
        JPanel bottomArea = new JPanel();
        bottomArea.setLayout(new BoxLayout(bottomArea, BoxLayout.Y_AXIS));
        bottomArea.setOpaque(true);
        bottomArea.setBackground(new Color(250, 250, 250));
        bottomArea.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        JPanel customerPanel = new JPanel(new GridLayout(3, 2, 8, 8));
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

        JPanel checkoutPanel = new JPanel(new BorderLayout());
        checkoutPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        checkoutPanel.setBackground(new Color(250, 250, 250));

        totalLabel = new JLabel("Total: ₦ 0.00", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JButton checkoutBtn = new JButton("Process Sale");
        styleButton(checkoutBtn, new Color(76, 175, 80));
        checkoutBtn.addActionListener(e -> processSale(
                phoneField.getText(),
                nameField.getText(),
                (String) paymentMethodCombo.getSelectedItem()
        ));

        checkoutPanel.add(totalLabel, BorderLayout.CENTER);
        checkoutPanel.add(checkoutBtn, BorderLayout.EAST);

        bottomArea.add(customerPanel);
        bottomArea.add(Box.createRigidArea(new Dimension(0, 8)));
        bottomArea.add(checkoutPanel);

        panel.add(cartTitle, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(bottomArea, BorderLayout.SOUTH);

        return panel;
    }

    // ---------- Sales / Payments History ----------
    private JPanel createSalesHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Top: sales list
        String[] salesCols = {"Sale ID", "Sale #", "Customer", "Total", "Status", "Date"};
        salesModel = new DefaultTableModel(salesCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        salesTable = new JTable(salesModel);
        salesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        salesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) onSaleSelected();
            }
        });

        JScrollPane salesScroll = new JScrollPane(salesTable);
        salesScroll.setBorder(BorderFactory.createTitledBorder("Recent Sales"));

        // Bottom: payments view + actions
        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        paymentsModel = new DefaultTableModel(new String[]{"ID", "Method", "Amount", "Reference", "Paid At"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        paymentsTable = new JTable(paymentsModel);
        paymentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // double-click to edit payment
        paymentsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = paymentsTable.rowAtPoint(e.getPoint());
                    if (r >= 0) {
                        paymentsTable.setRowSelectionInterval(r, r);
                        showEditPaymentDialogForSelectedPayment();
                    }
                }
            }
        });

        JScrollPane paymentsScroll = new JScrollPane(paymentsTable);
        paymentsScroll.setBorder(BorderFactory.createTitledBorder("Payments for selected sale"));

        // Selected sale label + action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.setOpaque(false);
        selectedSaleLabel = new JLabel("No sale selected");
        JButton refreshBtn = new JButton("Refresh Sales");
        JButton editPaymentBtn = new JButton("Edit Payment"); // explicit edit action

        styleButton(refreshBtn, new Color(96, 125, 139));
        styleButton(editPaymentBtn, new Color(255, 193, 7));

        // Refresh now clears current selection/payments and reloads authoritative sales from server
        refreshBtn.addActionListener(e -> {
            selectedSaleLabel.setText("No sale selected");
            paymentsModel.setRowCount(0);
            recentPayments.clear();
            loadRecentSales();
        });

        editPaymentBtn.addActionListener(e -> showEditPaymentDialogForSelectedPayment());

        actions.add(selectedSaleLabel);
        actions.add(refreshBtn);
        actions.add(editPaymentBtn);

        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(paymentsScroll, BorderLayout.CENTER);

        panel.add(salesScroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void onSaleSelected() {
        int row = salesTable.getSelectedRow();
        if (row < 0 || row >= recentSales.size()) {
            selectedSaleLabel.setText("No sale selected");
            paymentsModel.setRowCount(0);
            recentPayments.clear();
            return;
        }
        Map<String, Object> sale = recentSales.get(row);
        String saleNumber = Objects.toString(sale.get("saleNumber"), "N/A");
        String status = Objects.toString(sale.get("paymentStatus"), "N/A");
        Object saleIdObj = sale.get("saleId") != null ? sale.get("saleId") : sale.get("id");
        Integer saleId = safeIntegerFromObject(saleIdObj);
        selectedSaleLabel.setText("Selected: " + saleNumber + " | Status: " + status);
        fetchPaymentsForSale(saleId);
    }

    // ---------- Networking / Data ----------
    private void loadProducts() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> loaded = new ArrayList<>();

            @Override protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/products");
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            loaded = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                loaded = (List<Map<String, Object>>) r.get("data");
                            } else throw ex;
                        }
                    }
                } catch (Exception ex) { error = ex; ex.printStackTrace(); }
                return null;
            }

            @Override protected void done() {
                if (error != null) { showError("Failed to load products: " + error.getMessage()); return; }
                products.clear();
                products.addAll(loaded);
                updateProductGrid();
            }
        };
        w.execute();
    }

    /**
     * Loads ALL sales from the server and replaces local cache.
     * This method is intentionally authoritative: server results always override local history.
     */
    private void loadRecentSales() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> fetched = new ArrayList<>();
            @Override protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/sales"); // authoritative source
                    if (resp == null || resp.trim().isEmpty()) return null;
                    try {
                        fetched = client.parseResponseList(resp);
                    } catch (Exception e) {
                        Map<String, Object> r = client.parseResponse(resp);
                        if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                            fetched = (List<Map<String, Object>>) r.get("data");
                        }
                    }
                } catch (Exception ex) { error = ex; ex.printStackTrace(); }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    // If fetching failed, do not modify the cache; show error
                    showError("Failed to load sales from server: " + error.getMessage());
                    return;
                }
                // Always replace recentSales with the fetched list (server-authoritative).
                recentSales.clear();
                if (fetched != null && !fetched.isEmpty()) recentSales.addAll(fetched);
                // If fetched is empty, recentSales remains empty (server says no sales)
                updateSalesTable();
            }
        };
        w.execute();
    }

    private void updateProductGrid() {
        productGrid.removeAll();
        if (products.isEmpty()) {
            JLabel noProductsLabel = new JLabel("No products available", SwingConstants.CENTER);
            noProductsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            noProductsLabel.setForeground(new Color(150, 150, 150));
            productGrid.add(noProductsLabel);
        } else {
            for (Map<String, Object> product : products) {
                String name = getStringValue(product, "name", "Unknown Product");
                Double price = getDoubleValue(product, "minimumSellingPrice", 0.0);
                Integer productId = getIntegerValue(product, "id", 0);
                productGrid.add(createProductButton(name, price, productId));
            }
        }
        productGrid.revalidate();
        productGrid.repaint();
    }

    private void updateSalesTable() {
        SwingUtilities.invokeLater(() -> {
            salesModel.setRowCount(0);
            for (Map<String, Object> sale : recentSales) {
                Integer saleId = safeIntegerFromObject(sale.get("saleId") != null ? sale.get("saleId") : sale.get("id"));
                String saleNumber = Objects.toString(sale.get("saleNumber"), Objects.toString(sale.get("saleNo"), "N/A"));

                // Prefer showing "Name (phone)" if available, else fallback to phone or id
                String customerDisplay = "N/A";
                if (sale.containsKey("customerName") || sale.containsKey("customerPhone")) {
                    String n = Objects.toString(sale.get("customerName"), "").trim();
                    String p = Objects.toString(sale.get("customerPhone"), "").trim();
                    if (!n.isEmpty() && !p.isEmpty()) customerDisplay = n + " (" + p + ")";
                    else if (!n.isEmpty()) customerDisplay = n;
                    else if (!p.isEmpty()) customerDisplay = p;
                } else if (sale.containsKey("customer") && sale.get("customer") instanceof Map) {
                    Map<String, Object> c = (Map<String, Object>) sale.get("customer");
                    String n = Objects.toString(c.get("name"), "").trim();
                    String p = Objects.toString(c.get("phone"), "").trim();
                    if (!n.isEmpty() && !p.isEmpty()) customerDisplay = n + " (" + p + ")";
                    else if (!n.isEmpty()) customerDisplay = n;
                    else if (!p.isEmpty()) customerDisplay = p;
                } else {
                    customerDisplay = Objects.toString(sale.get("customerId"), Objects.toString(sale.get("customerPhone"), "N/A"));
                }

                Double total = safeDoubleFromObject(sale.get("totalAmount") != null ? sale.get("totalAmount") : sale.get("total"));
                String status = Objects.toString(sale.get("paymentStatus"), "N/A");
                String date = Objects.toString(sale.get("saleDate"), "");
                salesModel.addRow(new Object[]{
                        saleId,
                        saleNumber,
                        customerDisplay,
                        String.format("₦ %,.2f", total == null ? 0.0 : total),
                        status,
                        date.length() > 16 ? date.substring(0, 16) : date
                });
            }
        });
    }

    private void fetchPaymentsForSale(Integer saleId) {
        if (saleId == null) {
            paymentsModel.setRowCount(0);
            recentPayments.clear();
            return;
        }
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private List<Map<String, Object>> payments = new ArrayList<>();
            @Override protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/payments/sale/" + saleId);
                    if (resp == null || resp.trim().isEmpty()) return null;
                    try {
                        payments = client.parseResponseList(resp);
                    } catch (Exception e) {
                        Map<String, Object> r = client.parseResponse(resp);
                        if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                            payments = (List<Map<String, Object>>) r.get("data");
                        }
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
                return null;
            }
            @Override protected void done() {
                paymentsModel.setRowCount(0);
                recentPayments.clear();
                if (payments != null) {
                    recentPayments.addAll(payments);
                    for (Map<String, Object> p : payments) {
                        Integer id = safeIntegerFromObject(p.get("id"));
                        String method = Objects.toString(p.get("paymentMethod"), "N/A");
                        Double amount = safeDoubleFromObject(p.get("amount"));
                        String reference = Objects.toString(p.get("reference"), "N/A");
                        String paidAt = Objects.toString(p.get("paidAt"), "");
                        paymentsModel.addRow(new Object[]{
                                id,
                                method,
                                String.format("₦ %,.2f", amount == null ? 0.0 : amount),
                                reference,
                                paidAt.length() > 16 ? paidAt.substring(0, 16) : paidAt
                        });
                    }
                }
            }
        };
        w.execute();
    }


    // ---------- Process Sale ----------
    private void processSale(String phone, String name, String paymentMethod) {
        if (cartItems.isEmpty()) { showError("Cart is empty!"); return; }
        if (phone == null || phone.trim().isEmpty()) { showError("Please enter customer phone number"); return; }
        if (!"CASH".equalsIgnoreCase(paymentMethod) && !"CREDIT".equalsIgnoreCase(paymentMethod)) {
            showError("Invalid payment method"); return;
        }

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private String resultMessage = "Unknown error";
            private boolean success = false;
            private Map<String, Object> createdSaleData = null;

            @Override protected Void doInBackground() {
                try {
                    Map<String, Object> saleRequest = new HashMap<>();
                    saleRequest.put("customerPhone", phone);
                    // include optional name if provided so service can persist it
                    if (name != null && !name.trim().isEmpty()) saleRequest.put("customerName", name.trim());
                    saleRequest.put("discountTotal", 0);

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

                    String saleResponse = client.post("/api/secure/sales", saleRequest);
                    Map<String, Object> saleResult = client.parseResponse(saleResponse);

                    success = client.isResponseSuccessful(saleResponse);
                    resultMessage = client.getResponseMessage(saleResponse);

                    if (!success) return null;

                    Map<String, Object> saleData = null;
                    if (saleResult != null) {
                        if (saleResult.containsKey("data") && saleResult.get("data") instanceof Map) {
                            saleData = (Map<String, Object>) saleResult.get("data");
                        } else {
                            saleData = saleResult;
                        }
                    }
                    if (saleData == null) { success = false; resultMessage = "Sale created but server returned no sale data."; return null; }

                    // Add created sale to local history only if not already present.
                    createdSaleData = new HashMap<>(saleData);
                    addSaleToHistory(createdSaleData);

                    Integer saleId = safeIntegerFromObject(saleData.get("saleId") != null ? saleData.get("saleId") : saleData.get("id"));
                    Double authoritativeTotal = safeDoubleFromObject(saleData.get("totalAmount") != null ? saleData.get("totalAmount") : saleData.get("total"));

                    if ("CASH".equalsIgnoreCase(paymentMethod)) {
                        if (saleId == null) {
                            resultMessage = "Sale created but server did not return sale id; cannot record payment automatically.";
                            success = true; // sale created but payment not recorded
                            return null;
                        }
                        double amountToPay = authoritativeTotal != null ? authoritativeTotal : calculateTotalAmount();
                        Map<String, Object> paymentRequest = new HashMap<>();
                        paymentRequest.put("saleId", saleId);
                        paymentRequest.put("paymentMethod", "CASH");
                        paymentRequest.put("amount", amountToPay);
                        paymentRequest.put("reference", "CASH-" + System.currentTimeMillis());

                        // For brand-new sale there won't be a payment to update; POST is appropriate here.
                        String payResp = client.post("/api/secure/payments", paymentRequest);
                        boolean paySuccess = client.isResponseSuccessful(payResp);
                        String payMsg = client.getResponseMessage(payResp);

                        // if server returns success, we can refresh the sale entry (it may have been updated to PAID)
                        if (paySuccess) {
                            resultMessage = "Sale and payment recorded successfully (Sale ID: " + saleId + ").";
                            success = true;
                            try { refreshSaleFromServer(saleId); } catch (Exception ignored) {}
                        } else {
                            // fallback: try to verify by reading payments for the sale
                            double amountToVerify = amountToPay;
                            boolean verified = verifyPaymentByListing(saleId, amountToVerify);
                            if (verified) {
                                resultMessage = "Sale and payment recorded (verified) (Sale ID: " + saleId + ").";
                                success = true;
                                try { refreshSaleFromServer(saleId); } catch (Exception ignored) {}
                            } else {
                                resultMessage = "Sale created (ID: " + saleId + ") but payment failed: " + payMsg;
                                success = false;
                            }
                        }
                    } else { // CREDIT
                        double amountToPay = authoritativeTotal != null ? authoritativeTotal : calculateTotalAmount();
                        // optionally record a zero-amount credit payment (best-effort)
                        if (saleId != null) {
                            Map<String, Object> creditPayment = new HashMap<>();
                            creditPayment.put("saleId", saleId);
                            creditPayment.put("paymentMethod", "CREDIT");
                            creditPayment.put("amount", amountToPay);
                            creditPayment.put("reference", "CREDIT-" + System.currentTimeMillis());
                            try {
                                client.post("/api/secure/payments", creditPayment);
                            } catch (Exception ignored) {}
                        }
                        resultMessage = "Sale recorded as CREDIT.";
                        success = true;
                    }
                } catch (Exception e) {
                    success = false;
                    resultMessage = "Error: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(SalesPanel.this, resultMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearCart();
                    if (searchField != null) searchField.setText("");
                    // after creating sale we rely on server-authoritative load; but still update UI
                    loadRecentSales(); // refresh authoritative list
                } else {
                    JOptionPane.showMessageDialog(SalesPanel.this, resultMessage, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    // ---------- Add sale to local history (immediate UI feedback) ----------
    private void addSaleToHistory(Map<String, Object> saleData) {
        Integer incomingId = safeIntegerFromObject(saleData.get("saleId") != null ? saleData.get("saleId") : saleData.get("id"));
        // if the server list already contains this sale id, skip adding local duplicate
        boolean exists = false;
        if (incomingId != null) {
            for (Map<String, Object> s : recentSales) {
                Integer sid = safeIntegerFromObject(s.get("saleId") != null ? s.get("saleId") : s.get("id"));
                if (sid != null && sid.equals(incomingId)) { exists = true; break; }
            }
        }
        if (!exists) {
            Map<String, Object> normalized = new HashMap<>(saleData);
            if (!normalized.containsKey("saleId") && normalized.containsKey("id")) {
                normalized.put("saleId", normalized.get("id"));
            }
            if (!normalized.containsKey("totalAmount") && normalized.containsKey("total")) {
                normalized.put("totalAmount", normalized.get("total"));
            }
            recentSales.add(0, normalized);
            if (recentSales.size() > 200) recentSales.remove(recentSales.size() - 1);
            updateSalesTable();
        }
    }

    // ---------- Try refresh single sale from server (if endpoint exists) ----------
    private void refreshSaleFromServer(Integer saleId) {
        if (saleId == null) return;
        try {
            String resp = client.get("/api/secure/sales/" + saleId); // optimistic endpoint
            if (resp == null || resp.trim().isEmpty()) return;
            Map<String, Object> result = client.parseResponse(resp);
            Map<String, Object> saleData = null;
            if (result != null) {
                if (result.containsKey("data") && result.get("data") instanceof Map) {
                    saleData = (Map<String, Object>) result.get("data");
                } else {
                    saleData = result;
                }
            }
            if (saleData != null) {
                for (int i = 0; i < recentSales.size(); i++) {
                    Integer sid = safeIntegerFromObject(recentSales.get(i).get("saleId"));
                    if (sid != null && sid.equals(saleId)) {
                        recentSales.set(i, saleData);
                        updateSalesTable();
                        return;
                    }
                }
                // If not found, add it at top
                addSaleToHistory(saleData);
            }
        } catch (Exception ignored) {}
    }

    // ---------- Verify payment by listing payments for sale ----------
    private boolean verifyPaymentByListing(Integer saleId, double expectedAmount) {
        if (saleId == null) return false;
        try {
            String paymentsResp = client.get("/api/secure/payments/sale/" + saleId);
            List<Map<String, Object>> payments = null;
            try {
                payments = client.parseResponseList(paymentsResp);
            } catch (Exception ex) {
                Map<String, Object> maybe = client.parseResponse(paymentsResp);
                if (maybe != null && maybe.containsKey("data") && maybe.get("data") instanceof List) {
                    payments = (List<Map<String, Object>>) maybe.get("data");
                }
            }
            if (payments == null || payments.isEmpty()) return false;
            double EPS = 0.01;
            for (Map<String, Object> p : payments) {
                String method = Objects.toString(p.get("paymentMethod"), "");
                Double amt = safeDoubleFromObject(p.get("amount"));
                if (method.equalsIgnoreCase("CASH") && amt != null && Math.abs(amt - expectedAmount) <= EPS) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    // ---------- Show dialog to record a manual payment for selected sale ----------
    private void showRecordPaymentDialogForSelectedSale() {
        int saleRow = salesTable.getSelectedRow();
        if (saleRow < 0 || saleRow >= recentSales.size()) {
            showError("Select a sale first");
            return;
        }

        if (recentPayments.isEmpty()) {
            showError(
                    "This sale has no payment record.\n" +
                            "Payments can only be created at checkout (Cart tab)."
            );
            return;
        }

        int selPaymentRow = paymentsTable.getSelectedRow();
        if (selPaymentRow < 0 || selPaymentRow >= recentPayments.size()) {
            showError("Select the payment you want to update.");
            return;
        }

        Map<String, Object> sale = recentSales.get(saleRow);
        Map<String, Object> payment = recentPayments.get(selPaymentRow);

        Integer saleId = safeIntegerFromObject(
                sale.get("saleId") != null ? sale.get("saleId") : sale.get("id")
        );
        Integer paymentIdToUpdate = safeIntegerFromObject(payment.get("id"));

        if (paymentIdToUpdate == null) {
            showError("Selected payment has no ID and cannot be updated.");
            return;
        }

        Double existingAmount = safeDoubleFromObject(payment.get("amount"));

        // ---- Dialog ----
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));

        JTextField saleIdField = new JTextField(String.valueOf(saleId));
        saleIdField.setEditable(false);

        JTextField paymentIdField = new JTextField(String.valueOf(paymentIdToUpdate));
        paymentIdField.setEditable(false);

        JTextField amountField = new JTextField(
                existingAmount == null ? "0.00" : String.format("%.2f", existingAmount)
        );

        JComboBox<String> methodCb = new JComboBox<>(new String[]{"CASH", "CREDIT"});
        methodCb.setSelectedItem(
                Objects.toString(payment.get("paymentMethod"), "CREDIT")
        );

        JTextField refField = new JTextField(
                Objects.toString(payment.get("reference"), "MANUAL-" + System.currentTimeMillis())
        );

        form.add(new JLabel("Sale ID:"));      form.add(saleIdField);
        form.add(new JLabel("Payment ID:"));   form.add(paymentIdField);
        form.add(new JLabel("Amount:"));       form.add(amountField);
        form.add(new JLabel("Method:"));       form.add(methodCb);
        form.add(new JLabel("Reference:"));    form.add(refField);

        int option = JOptionPane.showConfirmDialog(
                this,
                form,
                "Update Payment for Sale " + saleId,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) return;

        double newAmount;
        try {
            newAmount = Double.parseDouble(amountField.getText().trim());
        } catch (Exception e) {
            showError("Invalid amount");
            return;
        }

        String newMethod = (String) methodCb.getSelectedItem();
        String newReference = refField.getText().trim();

        // ---- Update payment (PUT ONLY) ----
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private boolean ok = false;
            private String msg = "Unknown error";

            @Override
            protected Void doInBackground() {
                try {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("amount", newAmount);
                    payload.put("paymentMethod", newMethod);
                    payload.put("reference", newReference);

                    String resp = client.put(
                            "/api/secure/payments/" + paymentIdToUpdate,
                            payload
                    );

                    boolean success = client.isResponseSuccessful(resp);
                    String serverMsg = client.getResponseMessage(resp);

                    if (!success) {
                        msg = "Payment update failed: " + serverMsg;
                        return null;
                    }

                    ok = true;
                    msg = "Payment updated successfully";

                    // Refresh UI from server
                    fetchPaymentsForSale(saleId);
                    refreshSaleFromServer(saleId);

                } catch (Exception ex) {
                    msg = "Error: " + ex.getMessage();
                    ok = false;
                }
                return null;
            }

            @Override
            protected void done() {
                if (ok) {
                    JOptionPane.showMessageDialog(
                            SalesPanel.this,
                            msg,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    updateSalesTable();
                } else {
                    JOptionPane.showMessageDialog(
                            SalesPanel.this,
                            msg,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        w.execute();
    }


    // ---------- Edit Payment (explicit) ----------
    private void showEditPaymentDialogForSelectedPayment() {
        int selSaleRow = salesTable.getSelectedRow();
        int selPaymentRow = paymentsTable.getSelectedRow();
        if (selSaleRow < 0 || selSaleRow >= recentSales.size()) { showError("Select a sale first"); return; }
        if (selPaymentRow < 0 || selPaymentRow >= recentPayments.size()) { showError("Select a payment to edit"); return; }

        Map<String, Object> payment = recentPayments.get(selPaymentRow);
        Integer paymentId = safeIntegerFromObject(payment.get("id"));
        if (paymentId == null) { showError("Selected payment has no id"); return; }

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        JTextField idField = new JTextField(String.valueOf(paymentId));
        idField.setEditable(false);
        Double amt = safeDoubleFromObject(payment.get("amount"));
        JTextField amountField = new JTextField(amt == null ? "0.00" : String.format("%.2f", amt));
        JComboBox<String> methodCb = new JComboBox<>(new String[]{"CASH", "CREDIT"});
        methodCb.setSelectedItem(Objects.toString(payment.get("paymentMethod"), "CASH"));
        JTextField refField = new JTextField(Objects.toString(payment.get("reference"), "MANUAL-" + System.currentTimeMillis()));

        form.add(new JLabel("Payment ID:")); form.add(idField);
        form.add(new JLabel("Amount:")); form.add(amountField);
        form.add(new JLabel("Method:")); form.add(methodCb);
        form.add(new JLabel("Reference:")); form.add(refField);

        int option = JOptionPane.showConfirmDialog(this, form,
                "Edit Payment " + paymentId, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option != JOptionPane.OK_OPTION) return;

        double newAmount;
        try { newAmount = Double.parseDouble(amountField.getText().trim()); } catch (Exception e) { showError("Invalid amount"); return; }
        String newMethod = (String) methodCb.getSelectedItem();
        String newRef = refField.getText().trim();

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private boolean ok = false;
            private String msg = "Unknown error";
            @Override protected Void doInBackground() {
                try {
                    Map<String, Object> pm = new HashMap<>();
                    pm.put("amount", newAmount);
                    pm.put("paymentMethod", newMethod);
                    pm.put("reference", newRef);

                    String resp;
                    try {
                        resp = client.put("/api/secure/payments/" + paymentId, pm);
                    } catch (NoSuchMethodError nsme) {
                        resp = client.post("/api/secure/payments/" + paymentId, pm);
                    }

                    boolean success = client.isResponseSuccessful(resp);
                    String serverMsg = client.getResponseMessage(resp);
                    if (success) {
                        ok = true;
                        msg = "Payment updated: " + serverMsg;
                        // refresh payments and sale
                        int saleRow = salesTable.getSelectedRow();
                        Map<String, Object> sale = recentSales.get(saleRow);
                        Integer saleId = safeIntegerFromObject(sale.get("saleId") != null ? sale.get("saleId") : sale.get("id"));
                        fetchPaymentsForSale(saleId);
                        try { refreshSaleFromServer(saleId); } catch (Exception ignored) {}
                    } else {
                        ok = false;
                        msg = "Update failed: " + serverMsg;
                    }
                } catch (Exception ex) {
                    ok = false;
                    msg = "Error: " + ex.getMessage();
                }
                return null;
            }
            @Override
            protected void done() {
                if (ok) {
                    JOptionPane.showMessageDialog(SalesPanel.this, msg, "Success", JOptionPane.INFORMATION_MESSAGE);
                    updateSalesTable();
                } else {
                    JOptionPane.showMessageDialog(SalesPanel.this, msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    // ---------- Helpers ----------
    private void addToCart(Integer productId, String product, double price) {
        for (CartItem item : cartItems) {
            if (item.productId.equals(productId)) {
                item.quantity++;
                item.total = round(item.unitPrice * item.quantity);
                updateCartTable();
                calculateTotal();
                return;
            }
        }
        cartItems.add(new CartItem(productId, product, price, 1));
        updateCartTable();
        calculateTotal();
    }

    private void updateCartTable() {
        cartModel.setRowCount(0);
        for (CartItem item : cartItems) {
            cartModel.addRow(new Object[]{
                    item.productName,
                    String.format("₦ %,.2f", item.unitPrice),
                    item.quantity,
                    String.format("₦ %,.2f", item.total),
                    "Remove"
            });
        }
    }

    private void calculateTotal() {
        double total = cartItems.stream().mapToDouble(i -> i.total).sum();
        totalLabel.setText(String.format("Total: ₦ %,.2f", total));
    }

    // >>> New fixed method (was missing)
    private double calculateTotalAmount() {
        return cartItems.stream().mapToDouble(i -> i.total).sum();
    }

    private void clearCart() {
        cartItems.clear();
        updateCartTable();
        calculateTotal();
    }

    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object v = map.get(key);
        return v != null ? v.toString() : defaultValue;
    }

    private Double getDoubleValue(Map<String, Object> map, String key, Double defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return defaultValue; }
    }

    private Integer getIntegerValue(Map<String, Object> map, String key, Integer defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object v = map.get(key);
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return defaultValue; }
    }

    private Integer safeIntegerFromObject(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    private Double safeDoubleFromObject(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return null; }
    }

    private double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE));
    }

    // ---------- Table button renderer/editor ----------
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

        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
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

        public Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }

        public Object getCellEditorValue() { return "Remove"; }
    }
}
