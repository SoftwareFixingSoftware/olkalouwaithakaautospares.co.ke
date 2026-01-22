package olkalouwaithakaautospares.co.ke.win.ui.dashboard.sales;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
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
 *   - "Paid Sales" (completed sales - no modifications allowed)
 *   - "Credit Sales" (credit sales with payment update functionality)
 */
@SuppressWarnings("unchecked")
public class SalesPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    private final List<Map<String, Object>> products = new ArrayList<>();
    private final List<CartItem> cartItems = new ArrayList<>();
    private final List<Map<String, Object>> paidSales = new ArrayList<>();
    private final List<Map<String, Object>> creditSales = new ArrayList<>();
    private final List<Map<String, Object>> recentPayments = new ArrayList<>();

    // POS components
    private JTable cartTable;
    private DefaultTableModel cartModel;
    private JLabel totalLabel;
    private JComboBox<String> paymentMethodCombo;
    private JPanel productGrid;
    private JTextField searchField;

    // New customer / receipt components
    private JCheckBox sendReceiptCheckbox;
    private JTextField emailField;
    private JButton checkoutBtn;

    // Tabs
    private JTabbedPane rightTabs;
    private JPanel paidSalesPanel;
    private JPanel creditSalesPanel;

    // Paid Sales components
    private JTable paidSalesTable;
    private DefaultTableModel paidSalesModel;
    private TableRowSorter<DefaultTableModel> paidSalesSorter;
    private JTextField paidSalesSearchField;

    // Credit Sales components
    private JTable creditSalesTable;
    private DefaultTableModel creditSalesModel;
    private TableRowSorter<DefaultTableModel> creditSalesSorter;
    private JTextField creditSalesSearchField;
    private JLabel selectedCreditSaleLabel;
    private JTable creditPaymentsTable;
    private DefaultTableModel creditPaymentsModel;

    // Date formatter
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Cart item model
    class CartItem {
        Integer productId;
        String productName;
        Double unitPrice;
        Double originalPrice; // Minimum selling price
        Integer quantity;
        Double total;

        CartItem(Integer productId, String productName, Double unitPrice, Double originalPrice, Integer quantity) {
            this.productId = productId;
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.originalPrice = originalPrice; // Store minimum selling price
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
        loadRecentSales();
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

        // Right: tabbed pane containing Cart + Paid Sales + Credit Sales
        rightTabs = new JTabbedPane();
        JPanel cartPanel = createCartPanel();
        paidSalesPanel = createPaidSalesPanel();
        creditSalesPanel = createCreditSalesPanel();

        rightTabs.addTab("Cart", cartPanel);
        rightTabs.addTab("Paid Sales", paidSalesPanel);
        rightTabs.addTab("Credit Sales", creditSalesPanel);

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
        searchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10)); // Limit height
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Search products by name or SKU...");

        // Add DocumentListener for real-time search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchProducts(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchProducts(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchProducts(searchField.getText());
            }
        });

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

    private JButton createProductButton(
            String name,
            String description,
            double price,
            Integer productId
    ) {
        String safeDescription = (description == null || description.isBlank())
                ? ""
                : "<span style='font-size:10px;color:#777;'>" + description + "</span><br/>";

        JButton button = new JButton(
                "<html><center>"
                        + "<b>" + name + "</b><br/>"
                        + safeDescription
                        + "₦ " + String.format("%,.2f", price)
                        + "</center></html>"
        );

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

            Double minPrice = price;
            for (Map<String, Object> product : products) {
                Integer prodId = getIntegerValue(product, "id", 0);
                if (prodId.equals(id)) {
                    minPrice = getDoubleValue(product, "minimumSellingPrice", price);
                    break;
                }
            }

            addToCart(id, name, price, minPrice);
            rightTabs.setSelectedIndex(0);
        });

        return button;
    }


    private void searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Show all products
            productGrid.removeAll();
            for (Map<String, Object> product : products) {
                String name = getStringValue(product, "name", "Unknown Product");
                Double price = getDoubleValue(product, "minimumSellingPrice", 0.0);
                Integer productId = getIntegerValue(product, "id", 0);
                String description = getStringValue(product,"description","no description");
                productGrid.add(createProductButton(name,description, price, productId));
            }
        } else {
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
                    String description = getStringValue(product,"description","no description");
                    productGrid.add(createProductButton(name, description, price, productId));
                }
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
                return column == 1 || column == 2 || column == 4; // Price, Qty, Remove
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1 || columnIndex == 3) return Double.class;
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

        // Custom cell editor for price column with validation
        cartTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                try {
                    String value = ((JTextField) getComponent()).getText();
                    // Remove currency symbol and commas
                    value = value.replace("₦", "").replace(",", "").trim();
                    Double price = Double.parseDouble(value);

                    int row = cartTable.getEditingRow();
                    if (row >= 0 && row < cartItems.size()) {
                        CartItem item = cartItems.get(row);
                        if (price < item.originalPrice) {
                            JOptionPane.showMessageDialog(cartTable,
                                    "Price cannot be less than minimum: ₦ " + String.format("%,.2f", item.originalPrice),
                                    "Invalid Price", JOptionPane.ERROR_MESSAGE);
                            return false;
                        }
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(cartTable, "Invalid price format", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return super.stopCellEditing();
            }
        });

        // Custom cell editor for quantity column
        cartTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            @Override
            public boolean stopCellEditing() {
                try {
                    String value = ((JTextField) getComponent()).getText();
                    int qty = Integer.parseInt(value.trim());
                    if (qty < 1) {
                        JOptionPane.showMessageDialog(cartTable, "Quantity must be at least 1", "Error", JOptionPane.ERROR_MESSAGE);
                        return false;
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(cartTable, "Invalid quantity", "Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
                return super.stopCellEditing();
            }
        });

        cartModel.addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {
                    int row = e.getFirstRow();
                    int col = e.getColumn();

                    if (row >= 0 && row < cartItems.size()) {
                        CartItem item = cartItems.get(row);

                        if (col == 1) { // Price column
                            Object val = cartModel.getValueAt(row, 1);
                            if (val instanceof Double) {
                                double newPrice = (Double) val;
                                if (newPrice < item.originalPrice) {
                                    JOptionPane.showMessageDialog(SalesPanel.this,
                                            "Price cannot be less than minimum: ₦ " + String.format("%,.2f", item.originalPrice),
                                            "Invalid Price", JOptionPane.ERROR_MESSAGE);
                                    // Revert to original price
                                    cartModel.setValueAt(item.unitPrice, row, 1);
                                } else {
                                    item.unitPrice = newPrice;
                                    item.total = round(item.unitPrice * item.quantity);
                                    cartModel.setValueAt(String.format("₦ %,.2f", item.total), row, 3);
                                    calculateTotal();
                                }
                            } else if (val instanceof String) {
                                // Handle string input (e.g., user types "1200" instead of 1200.0)
                                try {
                                    String strVal = ((String) val).replace("₦", "").replace(",", "").trim();
                                    double newPrice = Double.parseDouble(strVal);
                                    if (newPrice < item.originalPrice) {
                                        JOptionPane.showMessageDialog(SalesPanel.this,
                                                "Price cannot be less than minimum: ₦ " + String.format("%,.2f", item.originalPrice),
                                                "Invalid Price", JOptionPane.ERROR_MESSAGE);
                                        cartModel.setValueAt(item.unitPrice, row, 1);
                                    } else {
                                        item.unitPrice = newPrice;
                                        item.total = round(item.unitPrice * item.quantity);
                                        cartModel.setValueAt(String.format("₦ %,.2f", item.total), row, 3);
                                        calculateTotal();
                                    }
                                } catch (NumberFormatException ex) {
                                    // If invalid, revert to original price
                                    cartModel.setValueAt(item.unitPrice, row, 1);
                                }
                            }
                        } else if (col == 2) { // Quantity column
                            Object val = cartModel.getValueAt(row, 2);
                            int qty = 1;
                            try {
                                if (val instanceof Number) qty = ((Number) val).intValue();
                                else qty = Integer.parseInt(val.toString());
                            } catch (Exception ex) {
                                qty = item.quantity; // Keep current quantity if invalid
                            }
                            if (qty < 1) qty = 1;
                            item.quantity = qty;
                            item.total = round(item.unitPrice * item.quantity);
                            cartModel.setValueAt(String.format("₦ %,.2f", item.total), row, 3);
                            calculateTotal();
                        }
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

        JPanel customerPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        customerPanel.setBorder(BorderFactory.createTitledBorder("Customer Information"));
        customerPanel.setBackground(new Color(250, 250, 250));

        JTextField phoneField = new JTextField();
        phoneField.putClientProperty("JTextField.placeholderText", "Phone number");

        JTextField nameField = new JTextField();
        nameField.putClientProperty("JTextField.placeholderText", "Name (optional)");

        paymentMethodCombo = new JComboBox<>(new String[]{"CASH", "CREDIT"});

        sendReceiptCheckbox = new JCheckBox("Send receipt via email");
        emailField = new JTextField();
        emailField.putClientProperty("JTextField.placeholderText", "Email address for receipt");
        emailField.setVisible(false);

        sendReceiptCheckbox.addActionListener(e -> {
            boolean sel = sendReceiptCheckbox.isSelected();
            emailField.setVisible(sel);
            SwingUtilities.invokeLater(() -> {
                customerPanel.revalidate();
                customerPanel.repaint();
            });
        });

        customerPanel.add(new JLabel("Phone:"));
        customerPanel.add(phoneField);
        customerPanel.add(new JLabel("Name:"));
        customerPanel.add(nameField);
        customerPanel.add(new JLabel("Payment:"));
        customerPanel.add(paymentMethodCombo);
        customerPanel.add(sendReceiptCheckbox);
        customerPanel.add(new JLabel(""));
        customerPanel.add(new JLabel("Email (if sending):"));
        customerPanel.add(emailField);

        JPanel checkoutPanel = new JPanel(new BorderLayout());
        checkoutPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        checkoutPanel.setBackground(new Color(250, 250, 250));

        totalLabel = new JLabel("Total: ₦ 0.00", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        checkoutBtn = new JButton("Process Sale");
        styleButton(checkoutBtn, new Color(76, 175, 80));
        checkoutBtn.addActionListener(e -> {
            processSale(
                    phoneField.getText(),
                    nameField.getText(),
                    (String) paymentMethodCombo.getSelectedItem(),
                    sendReceiptCheckbox.isSelected(),
                    emailField.getText()
            );
        });

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

    // ---------- Paid Sales Tab ----------
    private JPanel createPaidSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Header with title and refresh button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Paid Sales - View Only");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(96, 125, 139));

        JButton refreshBtn = new JButton("Refresh");
        styleButton(refreshBtn, new Color(96, 125, 139));
        refreshBtn.addActionListener(e -> loadRecentSales());

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        // Compact search bar for Paid Sales
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        // constrain height so it doesn't grow
        searchPanel.setPreferredSize(new Dimension(0, 40));

        paidSalesSearchField = new JTextField();
        paidSalesSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        paidSalesSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        paidSalesSearchField.putClientProperty("JTextField.placeholderText", "Search paid sales...");

        // Paid sales table - read only
        String[] paidCols = {"Sale ID", "Sale #", "Customer", "Total", "Payment Method", "Date"};
        paidSalesModel = new DefaultTableModel(paidCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // No editing allowed for paid sales
            }
        };
        paidSalesTable = new JTable(paidSalesModel);
        paidSalesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        paidSalesTable.setEnabled(true);

        // Setup TableRowSorter for filtering
        paidSalesSorter = new TableRowSorter<>(paidSalesModel);
        paidSalesTable.setRowSorter(paidSalesSorter);

        // Add DocumentListener for real-time search
        paidSalesSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterPaidSales(paidSalesSearchField.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { filterPaidSales(paidSalesSearchField.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterPaidSales(paidSalesSearchField.getText()); }
        });

        JButton paidSearchBtn = new JButton("Search");
        styleButton(paidSearchBtn, new Color(33, 150, 243));
        paidSearchBtn.setPreferredSize(new Dimension(80, 30));
        paidSearchBtn.addActionListener(e -> filterPaidSales(paidSalesSearchField.getText()));

        searchPanel.add(paidSalesSearchField, BorderLayout.CENTER);
        searchPanel.add(paidSearchBtn, BorderLayout.EAST);

        // Put header + search into a top container and place it in NORTH
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        JScrollPane paidSalesScroll = new JScrollPane(paidSalesTable);
        paidSalesScroll.setBorder(BorderFactory.createTitledBorder("Paid Sales List"));

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(paidSalesScroll, BorderLayout.CENTER);

        return panel;
    }

    private void filterPaidSales(String query) {
        if (query == null || query.trim().isEmpty()) {
            paidSalesSorter.setRowFilter(null);
        } else {
            RowFilter<DefaultTableModel, Integer> rowFilter = RowFilter.regexFilter("(?i)" + query);
            paidSalesSorter.setRowFilter(rowFilter);
        }
    }

    // ---------- Credit Sales Tab ----------
    // ---------- Credit Sales Tab ----------
    private JPanel createCreditSalesPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Header with title and refresh button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Credit Sales");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(new Color(96, 125, 139));

        JButton refreshBtn = new JButton("Refresh");
        styleButton(refreshBtn, new Color(96, 125, 139));
        refreshBtn.addActionListener(e -> {
            selectedCreditSaleLabel.setText("No credit sale selected");
            creditPaymentsModel.setRowCount(0);
            recentPayments.clear();
            loadRecentSales();
        });

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(refreshBtn, BorderLayout.EAST);

        // Compact search bar for Credit Sales
        JPanel searchPanel = new JPanel(new BorderLayout(5, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        searchPanel.setPreferredSize(new Dimension(0, 40));

        creditSalesSearchField = new JTextField();
        creditSalesSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        creditSalesSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        creditSalesSearchField.putClientProperty("JTextField.placeholderText", "Search credit sales...");

        String[] creditCols = {"Sale ID", "Sale #", "Customer", "Total", "Date"};
        creditSalesModel = new DefaultTableModel(creditCols, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        creditSalesTable = new JTable(creditSalesModel);
        creditSalesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Setup TableRowSorter for filtering
        creditSalesSorter = new TableRowSorter<>(creditSalesModel);
        creditSalesTable.setRowSorter(creditSalesSorter);

        creditSalesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onCreditSaleSelected();
        });

        // Add DocumentListener for real-time search
        creditSalesSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterCreditSales(creditSalesSearchField.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { filterCreditSales(creditSalesSearchField.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { filterCreditSales(creditSalesSearchField.getText()); }
        });

        JButton creditSearchBtn = new JButton("Search");
        styleButton(creditSearchBtn, new Color(33, 150, 243));
        creditSearchBtn.setPreferredSize(new Dimension(80, 30));
        creditSearchBtn.addActionListener(e -> filterCreditSales(creditSalesSearchField.getText()));

        searchPanel.add(creditSalesSearchField, BorderLayout.CENTER);
        searchPanel.add(creditSearchBtn, BorderLayout.EAST);

        // Top panel with header + search
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        topPanel.add(headerPanel, BorderLayout.NORTH);
        topPanel.add(searchPanel, BorderLayout.SOUTH);

        JScrollPane creditSalesScroll = new JScrollPane(creditSalesTable);
        creditSalesScroll.setBorder(BorderFactory.createTitledBorder("Credit Sales List"));

        // Bottom: payments view + update payment button
        JPanel bottom = new JPanel(new BorderLayout(8, 8));

        creditPaymentsModel = new DefaultTableModel(new String[]{"ID", "Method", "Amount", "Reference", "Paid At"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        creditPaymentsTable = new JTable(creditPaymentsModel);
        creditPaymentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane paymentsScroll = new JScrollPane(creditPaymentsTable);
        paymentsScroll.setBorder(BorderFactory.createTitledBorder("Payments for selected credit sale"));

        // Selected sale label + action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        actions.setOpaque(false);
        selectedCreditSaleLabel = new JLabel("No credit sale selected");
        selectedCreditSaleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JButton updatePaymentBtn = new JButton("Update Payment");
        styleButton(updatePaymentBtn, new Color(255, 193, 7));
        updatePaymentBtn.addActionListener(e -> showUpdatePaymentDialog());

        actions.add(selectedCreditSaleLabel);
        actions.add(Box.createHorizontalStrut(20));
        actions.add(updatePaymentBtn);

        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(paymentsScroll, BorderLayout.CENTER);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(creditSalesScroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }


    private void filterCreditSales(String query) {
        if (query == null || query.trim().isEmpty()) {
            creditSalesSorter.setRowFilter(null);
        } else {
            RowFilter<DefaultTableModel, Integer> rowFilter = RowFilter.regexFilter("(?i)" + query);
            creditSalesSorter.setRowFilter(rowFilter);
        }
    }

    private void onCreditSaleSelected() {
        int row = creditSalesTable.getSelectedRow();
        if (row < 0) return;

        // Convert view row to model row
        int modelRow = creditSalesTable.convertRowIndexToModel(row);

        if (modelRow < 0 || modelRow >= creditSales.size()) {
            selectedCreditSaleLabel.setText("No credit sale selected");
            creditPaymentsModel.setRowCount(0);
            recentPayments.clear();
            return;
        }
        Map<String, Object> sale = creditSales.get(modelRow);
        String saleNumber = Objects.toString(sale.get("saleNumber"), "N/A");
        String status = Objects.toString(sale.get("paymentStatus"), "N/A");
        Object saleIdObj = sale.get("saleId") != null ? sale.get("saleId") : sale.get("id");
        Integer saleId = safeIntegerFromObject(saleIdObj);
        selectedCreditSaleLabel.setText("Selected: " + saleNumber + " | Status: " + status);
        fetchPaymentsForSale(saleId);
    }

    private void showUpdatePaymentDialog() {
        int row = creditSalesTable.getSelectedRow();
        if (row < 0) {
            showError("Select a credit sale first");
            return;
        }

        // Convert view row to model row
        int modelRow = creditSalesTable.convertRowIndexToModel(row);

        if (modelRow < 0 || modelRow >= creditSales.size()) {
            showError("Select a credit sale first");
            return;
        }

        Map<String, Object> sale = creditSales.get(modelRow);
        Integer saleId = safeIntegerFromObject(sale.get("saleId") != null ? sale.get("saleId") : sale.get("id"));
        if (saleId == null) {
            showError("Sale has no valid ID");
            return;
        }

        // Get sale total
        Double saleTotal = safeDoubleFromObject(sale.get("totalAmount") != null ? sale.get("totalAmount") : sale.get("total"));
        if (saleTotal == null) saleTotal = 0.0;

        // Create dialog
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Update Payment", true);
        dialog.setLayout(new BorderLayout());
        dialog.setMinimumSize(new Dimension(400, 250));

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel saleIdLabel = new JLabel("Sale ID:");
        JTextField saleIdField = new JTextField(String.valueOf(saleId));
        saleIdField.setEditable(false);

        JLabel saleTotalLabel = new JLabel("Sale Total:");
        JTextField saleTotalField = new JTextField(String.format("₦ %,.2f", saleTotal));
        saleTotalField.setEditable(false);

        JLabel paymentAmountLabel = new JLabel("Payment Amount:");
        JTextField paymentAmountField = new JTextField(String.format("%.2f", saleTotal));
        paymentAmountField.setEditable(false);

        JLabel emailLabel = new JLabel("Send Receipt:");
        JCheckBox sendReceiptCheckbox = new JCheckBox();
        JTextField emailField = new JTextField();
        emailField.setVisible(false);

        String customerEmail = getEmailForSale(sale);
        if (customerEmail != null && !customerEmail.isEmpty()) {
            emailField.setText(customerEmail);
            sendReceiptCheckbox.setSelected(true);
            emailField.setVisible(true);
        } else {
            // Try to get email from customer details
            customerEmail = getCustomerEmail(sale);
            if (customerEmail != null && !customerEmail.isEmpty()) {
                emailField.setText(customerEmail);
                sendReceiptCheckbox.setSelected(true);
                emailField.setVisible(true);
            }
        }

        sendReceiptCheckbox.addActionListener(e -> {
            emailField.setVisible(sendReceiptCheckbox.isSelected());
            dialog.pack();
        });

        form.add(saleIdLabel);
        form.add(saleIdField);
        form.add(saleTotalLabel);
        form.add(saleTotalField);
        form.add(paymentAmountLabel);
        form.add(paymentAmountField);
        form.add(emailLabel);
        form.add(sendReceiptCheckbox);
        form.add(new JLabel("Email:"));
        form.add(emailField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancelBtn = new JButton("Cancel");
        JButton updateBtn = new JButton("Add Payment");

        styleButton(updateBtn, new Color(76, 175, 80));
        cancelBtn.addActionListener(e -> dialog.dispose());
        updateBtn.addActionListener(e -> {
            try {
                Double paymentAmount = Double.parseDouble(paymentAmountField.getText().trim());
                if (paymentAmount <= 0) {
                    showError("Payment amount must be greater than 0");
                    return;
                }

                if (sendReceiptCheckbox.isSelected() &&
                        (emailField.getText() == null || emailField.getText().trim().isEmpty())) {
                    showError("Please provide email address for receipt");
                    return;
                }

                processPaymentUpdate(saleId, paymentAmount,
                        sendReceiptCheckbox.isSelected() ? emailField.getText().trim() : null);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                showError("Invalid payment amount format");
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(updateBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private Double calculatePaidAmount(Integer saleId) {
        double totalPaid = 0.0;
        for (Map<String, Object> payment : recentPayments) {
            Integer paymentSaleId = safeIntegerFromObject(payment.get("saleId"));
            if (paymentSaleId != null && paymentSaleId.equals(saleId)) {
                Double amount = safeDoubleFromObject(payment.get("amount"));
                if (amount != null) {
                    totalPaid += amount;
                }
            }
        }
        return totalPaid;
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

    private void loadRecentSales() {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> fetched = new ArrayList<>();

            @Override protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/sales");
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
                    showError("Failed to load sales from server: " + error.getMessage());
                    return;
                }

                // Separate sales into paid and credit based on payment_status
                paidSales.clear();
                creditSales.clear();

                if (fetched != null) {
                    for (Map<String, Object> sale : fetched) {
                        String status = Objects.toString(sale.get("paymentStatus"), "").toLowerCase();

                        // Check for pending/credit status (based on your requirements)
                        if ("paid".equals(status) || "completed".equals(status) || "cash".equals(status)) {
                            paidSales.add(sale);
                        } else if ("pending".equals(status) || "credit".equals(status) ||
                                "partially paid".equals(status) || "unpaid".equals(status) ||
                                "partial".equals(status)) {
                            creditSales.add(sale);
                        } else {
                            // Default: if status is not recognized, check if there's a total payment
                            Double total = safeDoubleFromObject(sale.get("totalAmount"));
                            Double paid = calculatePaidAmountForSale(sale);
                            if (paid != null && total != null && Math.abs(paid - total) < 0.01) {
                                paidSales.add(sale);
                            } else {
                                creditSales.add(sale);
                            }
                        }
                    }
                }

                updatePaidSalesTable();
                updateCreditSalesTable();
            }
        };
        w.execute();
    }

    private Double calculatePaidAmountForSale(Map<String, Object> sale) {
        Integer saleId = safeIntegerFromObject(sale.get("saleId") != null ? sale.get("saleId") : sale.get("id"));
        if (saleId == null) return 0.0;

        // Try to get from payments if available in the sale object
        if (sale.containsKey("payments") && sale.get("payments") instanceof List) {
            List<Map<String, Object>> payments = (List<Map<String, Object>>) sale.get("payments");
            double totalPaid = 0.0;
            for (Map<String, Object> payment : payments) {
                Double amount = safeDoubleFromObject(payment.get("amount"));
                if (amount != null) {
                    totalPaid += amount;
                }
            }
            return totalPaid;
        }
        return 0.0;
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
                String description = getStringValue(product, "description", "");
                Double price = getDoubleValue(product, "minimumSellingPrice", 0.0);
                Integer productId = getIntegerValue(product, "id", 0);

                productGrid.add(
                        createProductButton(name, description, price, productId)
                );
            }

        }
        productGrid.revalidate();
        productGrid.repaint();
    }

    private void updatePaidSalesTable() {
        SwingUtilities.invokeLater(() -> {
            paidSalesModel.setRowCount(0);
            for (Map<String, Object> sale : paidSales) {
                addSaleToTable(paidSalesModel, sale);
            }
            filterPaidSales(paidSalesSearchField != null ? paidSalesSearchField.getText() : "");
        });
    }

    private void updateCreditSalesTable() {
        SwingUtilities.invokeLater(() -> {
            creditSalesModel.setRowCount(0);
            for (Map<String, Object> sale : creditSales) {
                addSaleToTable(creditSalesModel, sale);
            }
            filterCreditSales(creditSalesSearchField != null ? creditSalesSearchField.getText() : "");
        });
    }

    private void addSaleToTable(DefaultTableModel model, Map<String, Object> sale) {
        Integer saleId = safeIntegerFromObject(sale.get("saleId") != null ? sale.get("saleId") : sale.get("id"));
        String saleNumber = Objects.toString(sale.get("saleNumber"), Objects.toString(sale.get("saleNo"), "N/A"));

        String customerDisplay = getCustomerDisplay(sale);
        Double total = safeDoubleFromObject(sale.get("totalAmount") != null ? sale.get("totalAmount") : sale.get("total"));
        String date = Objects.toString(sale.get("saleDate"), "");

        if (model == paidSalesModel) {
            String paymentMethod = getPaymentMethod(sale);
            model.addRow(new Object[]{
                    saleId,
                    saleNumber,
                    customerDisplay,
                    total != null ? total : 0.0,
                    paymentMethod,
                    date.length() > 16 ? date.substring(0, 16) : date
            });
        } else if (model == creditSalesModel) {
            // REMOVED Balance Due column and calculation
            model.addRow(new Object[]{
                    saleId,
                    saleNumber,
                    customerDisplay,
                    total != null ? total : 0.0,
                    date.length() > 16 ? date.substring(0, 16) : date
            });
        }
    }

    private String getCustomerDisplay(Map<String, Object> sale) {
        if (sale.containsKey("customerName") || sale.containsKey("customerPhone")) {
            String n = Objects.toString(sale.get("customerName"), "").trim();
            String p = Objects.toString(sale.get("customerPhone"), "").trim();
            if (!n.isEmpty() && !p.isEmpty()) return n + " (" + p + ")";
            else if (!n.isEmpty()) return n;
            else if (!p.isEmpty()) return p;
        } else if (sale.containsKey("customer") && sale.get("customer") instanceof Map) {
            Map<String, Object> c = (Map<String, Object>) sale.get("customer");
            String n = Objects.toString(c.get("name"), "").trim();
            String p = Objects.toString(c.get("phone"), "").trim();
            if (!n.isEmpty() && !p.isEmpty()) return n + " (" + p + ")";
            else if (!n.isEmpty()) return n;
            else if (!p.isEmpty()) return p;
        }
        return Objects.toString(sale.get("customerId"), Objects.toString(sale.get("customerPhone"), "N/A"));
    }

    private String getPaymentMethod(Map<String, Object> sale) {
        String method = Objects.toString(sale.get("paymentMethod"), "N/A");
        String status = Objects.toString(sale.get("paymentStatus"), "");

        if ("paid".equalsIgnoreCase(status) && "N/A".equals(method)) {
            return "CASH";
        }
        return method;
    }

    private String getCustomerEmail(Map<String, Object> sale) {
        if (sale.containsKey("customer") && sale.get("customer") instanceof Map) {
            Map<String, Object> c = (Map<String, Object>) sale.get("customer");
            return Objects.toString(c.get("email"), "").trim();
        }
        return "";
    }

    private void fetchPaymentsForSale(Integer saleId) {
        if (saleId == null) {
            creditPaymentsModel.setRowCount(0);
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
                creditPaymentsModel.setRowCount(0);
                recentPayments.clear();
                if (payments != null) {
                    recentPayments.addAll(payments);
                    for (Map<String, Object> p : payments) {
                        Integer id = safeIntegerFromObject(p.get("id"));
                        String method = Objects.toString(p.get("paymentMethod"), "N/A");
                        Double amount = safeDoubleFromObject(p.get("amount"));
                        String reference = Objects.toString(p.get("reference"), "N/A");
                        String paidAt = Objects.toString(p.get("paidAt"), "");
                        creditPaymentsModel.addRow(new Object[]{
                                id,
                                method,
                                amount != null ? amount : 0.0,
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
    private void processSale(String phone, String name, String paymentMethod, boolean sendReceipt, String email) {
        if (cartItems.isEmpty()) { showError("Cart is empty!"); return; }
        if (phone == null || phone.trim().isEmpty()) { showError("Please enter customer phone number"); return; }
        if (!"CASH".equalsIgnoreCase(paymentMethod) && !"CREDIT".equalsIgnoreCase(paymentMethod)) {
            showError("Invalid payment method"); return;
        }

        if (sendReceipt && (email == null || email.trim().isEmpty())) {
            showError("Please provide an email address to send receipt"); return;
        }

        SwingUtilities.invokeLater(() -> checkoutBtn.setEnabled(false));

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private String resultMessage = "Unknown error";
            private boolean success = false;
            private Map<String, Object> createdSaleData = null;

            @Override protected Void doInBackground() {
                try {
                    Map<String, Object> saleRequest = new HashMap<>();
                    saleRequest.put("customerPhone", phone);
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

                    createdSaleData = new HashMap<>(saleData);

                    Integer saleId = safeIntegerFromObject(saleData.get("saleId") != null ? saleData.get("saleId") : saleData.get("id"));
                    Double authoritativeTotal = safeDoubleFromObject(saleData.get("totalAmount") != null ? saleData.get("totalAmount") : saleData.get("total"));

                    if ("CASH".equalsIgnoreCase(paymentMethod)) {
                        if (saleId == null) {
                            resultMessage = "Sale created but server did not return sale id; cannot record payment automatically.";
                            success = true;
                            return null;
                        }
                        double amountToPay = authoritativeTotal != null ? authoritativeTotal : calculateTotalAmount();
                        Map<String, Object> paymentRequest = new HashMap<>();
                        paymentRequest.put("saleId", saleId);
                        paymentRequest.put("paymentMethod", "CASH");
                        paymentRequest.put("amount", amountToPay);
                        paymentRequest.put("reference", "CASH-" + System.currentTimeMillis());

                        if (sendReceipt && email != null && !email.trim().isEmpty()) {
                            paymentRequest.put("email", email.trim());
                        }

                        String payResp = client.post("/api/secure/payments", paymentRequest);
                        boolean paySuccess = client.isResponseSuccessful(payResp);
                        String payMsg = client.getResponseMessage(payResp);

                        if (paySuccess) {
                            resultMessage = "Sale and payment recorded successfully (Sale ID: " + saleId + ").";
                            success = true;
                            try { refreshSaleFromServer(saleId); } catch (Exception ignored) {}
                        } else {
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
                        if (saleId != null) {
                            Map<String, Object> creditPayment = new HashMap<>();
                            creditPayment.put("saleId", saleId);
                            creditPayment.put("paymentMethod", "CREDIT");
                            creditPayment.put("amount", amountToPay);
                            creditPayment.put("reference", "CREDIT-" + System.currentTimeMillis());

                            if (sendReceipt && email != null && !email.trim().isEmpty()) {
                                creditPayment.put("email", email.trim());
                            }
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
                SwingUtilities.invokeLater(() -> checkoutBtn.setEnabled(true));

                if (success) {
                    JOptionPane.showMessageDialog(SalesPanel.this, resultMessage, "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearCart();
                    if (searchField != null) searchField.setText("");
                    loadRecentSales();
                } else {
                    JOptionPane.showMessageDialog(SalesPanel.this, resultMessage, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void processPaymentUpdate(Integer saleId, double paymentAmount, String email) {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            private String resultMessage = "Unknown error";
            private boolean success = false;

            @Override protected Void doInBackground() {
                try {
                    Map<String, Object> paymentRequest = new HashMap<>();
                    paymentRequest.put("saleId", saleId);
                    paymentRequest.put("paymentMethod", "CASH"); // Always CASH for payment updates
                    paymentRequest.put("amount", paymentAmount);
                    paymentRequest.put("reference", "PAYMENT-UPDATE-" + System.currentTimeMillis());

                    if (email != null && !email.trim().isEmpty()) {
                        paymentRequest.put("email", email.trim());
                    }

                    String payResp = client.post("/api/secure/payments", paymentRequest);
                    boolean paySuccess = client.isResponseSuccessful(payResp);
                    String payMsg = client.getResponseMessage(payResp);

                    if (paySuccess) {
                        resultMessage = "Payment added successfully for Sale ID: " + saleId;
                        success = true;

                        // Refresh data
                        fetchPaymentsForSale(saleId);
                        loadRecentSales();
                    } else {
                        resultMessage = "Payment update failed: " + payMsg;
                        success = false;
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
                    // Switch to credit sales tab to see updated status
                    rightTabs.setSelectedComponent(creditSalesPanel);
                } else {
                    JOptionPane.showMessageDialog(SalesPanel.this, resultMessage, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }

    private void addSaleToHistory(Map<String, Object> saleData) {
        String status = Objects.toString(saleData.get("paymentStatus"), "").toLowerCase();
        Integer incomingId = safeIntegerFromObject(saleData.get("saleId") != null ? saleData.get("saleId") : saleData.get("id"));

        if ("paid".equals(status) || "completed".equals(status) || "cash".equals(status)) {
            // Add to paid sales
            boolean exists = false;
            for (Map<String, Object> s : paidSales) {
                Integer sid = safeIntegerFromObject(s.get("saleId") != null ? s.get("saleId") : s.get("id"));
                if (sid != null && sid.equals(incomingId)) { exists = true; break; }
            }
            if (!exists) {
                paidSales.add(0, saleData);
                updatePaidSalesTable();
            }
        } else if ("pending".equals(status) || "credit".equals(status) ||
                "partially paid".equals(status) || "unpaid".equals(status) ||
                "partial".equals(status)) {
            // Add to credit sales
            boolean exists = false;
            for (Map<String, Object> s : creditSales) {
                Integer sid = safeIntegerFromObject(s.get("saleId") != null ? s.get("saleId") : s.get("id"));
                if (sid != null && sid.equals(incomingId)) { exists = true; break; }
            }
            if (!exists) {
                creditSales.add(0, saleData);
                updateCreditSalesTable();
            }
        }
    }

    private void refreshSaleFromServer(Integer saleId) {
        if (saleId == null) return;
        try {
            String resp = client.get("/api/secure/sales/" + saleId);
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
                addSaleToHistory(saleData);
            }
        } catch (Exception ignored) {}
    }

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

    // ---------- Helpers ----------
    private void addToCart(Integer productId, String product, double price, double minPrice) {
        for (CartItem item : cartItems) {
            if (item.productId.equals(productId)) {
                item.quantity++;
                item.total = round(item.unitPrice * item.quantity);
                updateCartTable();
                calculateTotal();
                return;
            }
        }
        cartItems.add(new CartItem(productId, product, price, minPrice, 1));
        updateCartTable();
        calculateTotal();
    }

    private void updateCartTable() {
        cartModel.setRowCount(0);
        for (CartItem item : cartItems) {
            cartModel.addRow(new Object[]{
                    item.productName,
                    item.unitPrice,
                    item.quantity,
                    item.total,
                    "Remove"
            });
        }
    }

    private void calculateTotal() {
        double total = cartItems.stream().mapToDouble(i -> i.total).sum();
        totalLabel.setText(String.format("Total: ₦ %,.2f", total));
    }

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

    private String getEmailForSale(Map<String, Object> sale) {
        if (sale == null) return null;
        String e = Objects.toString(sale.get("email"), "").trim();
        if (!e.isEmpty()) return e;
        e = Objects.toString(sale.get("customerEmail"), "").trim();
        if (!e.isEmpty()) return e;
        if (sale.containsKey("customer") && sale.get("customer") instanceof Map) {
            Map<String, Object> c = (Map<String, Object>) sale.get("customer");
            e = Objects.toString(c.get("email"), "").trim();
            if (!e.isEmpty()) return e;
        }
        return null;
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