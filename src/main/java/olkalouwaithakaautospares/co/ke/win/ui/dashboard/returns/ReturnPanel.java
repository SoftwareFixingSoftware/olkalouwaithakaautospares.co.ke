package olkalouwaithakaautospares.co.ke.win.ui.dashboard.returns;

import com.fasterxml.jackson.core.type.TypeReference;
import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * ReturnPanel — For cashiers to create returns from existing sales
 *
 * Left: Recent sales list (select a sale)
 * Right: Tabbed pane with:
 *   - "Create Return" (select items from the selected sale)
 *   - "Returns History" (view previous returns)
 */
@SuppressWarnings("unchecked")
public class ReturnPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    private final List<Map<String, Object>> recentSales = new ArrayList<>();
    private final List<Map<String, Object>> saleItems = new ArrayList<>();
    private final List<Map<String, Object>> returnsList = new ArrayList<>();
    private final Map<Integer, String> productNameCache = new HashMap<>(); // Cache for product names

    // Sales list components
    private JTable salesTable;
    private DefaultTableModel salesModel;
    private JTextField searchField;
    private JLabel selectedSaleLabel;

    // Create Return components
    private JTable saleItemsTable;
    private DefaultTableModel saleItemsModel;
    private JComboBox<String> reasonCombo;
    private JTextField quantityField;
    private JTextArea notesArea;
    private JLabel selectedItemLabel;

    // Returns History components
    private JTable returnsTable;
    private DefaultTableModel returnsModel;
    private JLabel returnsSummaryLabel;
    private JCheckBox showAllReturnsCheckbox;

    // Predefined return reasons (matching your DB)
    private final Map<Integer, String> returnReasons = new HashMap<Integer, String>() {{
        put(1, "Damaged item");
        put(2, "Wrong item sent");
        put(3, "Customer changed mind");
    }};

    // Current selection
    private Integer selectedSaleId = null;
    private Integer selectedSaleItemId = null;
    private Integer selectedItemQuantity = 0;
    private Integer selectedProductId = null;

    public ReturnPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();
        loadRecentSales();
        loadAllProducts(); // Preload product names
        loadAllReturns(); // Load all returns initially
    }

    // ---------- UI Initialization ----------
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Left: Sales list
        JPanel salesPanel = createSalesListPanel();

        // Right: Tabbed pane
        JTabbedPane rightTabs = new JTabbedPane();
        JPanel createReturnPanel = createReturnFormPanel();
        JPanel returnsHistoryPanel = createReturnsHistoryPanel();

        rightTabs.addTab("Create Return", createReturnPanel);
        rightTabs.addTab("Returns History", returnsHistoryPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, salesPanel, rightTabs);
        splitPane.setDividerLocation(500);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Product Returns");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(30, 33, 57));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        JButton clearBtn = new JButton("Clear Form");
        JButton refreshBtn = new JButton("Refresh All");

        styleButton(clearBtn, new Color(158, 158, 158));
        styleButton(refreshBtn, new Color(33, 150, 243));

        clearBtn.addActionListener(e -> clearReturnForm());
        refreshBtn.addActionListener(e -> {
            loadRecentSales();
            loadAllProducts();
            loadAllReturns();
            clearReturnForm();
        });

        buttonPanel.add(clearBtn);
        buttonPanel.add(refreshBtn);

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

    // ---------- Sales List Panel ----------
    private JPanel createSalesListPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Search sales by customer or sale number...");

        JButton searchBtn = new JButton("Search");
        styleButton(searchBtn, new Color(33, 150, 243));
        searchBtn.addActionListener(e -> searchSales(searchField.getText()));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);

        // Sales table
        String[] columns = {"Sale ID", "Sale #", "Customer", "Total", "Status", "Date"};
        salesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        salesTable = new JTable(salesModel);
        salesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        salesTable.setRowHeight(36);
        salesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        salesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        salesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onSaleSelected();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(salesTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Recent Sales (Select one to view items)"));

        // Selected sale label
        selectedSaleLabel = new JLabel("No sale selected");
        selectedSaleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        selectedSaleLabel.setForeground(new Color(100, 100, 100));
        selectedSaleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(selectedSaleLabel, BorderLayout.SOUTH);

        return panel;
    }

    // ---------- Create Return Panel ----------
    private JPanel createReturnFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel formTitle = new JLabel("Create Return from Selected Sale");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Sale items table
        String[] columns = {"Item ID", "Product", "Product ID", "Quantity", "Price", "Total", ""};
        saleItemsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only the action column is editable
            }
        };

        saleItemsTable = new JTable(saleItemsModel);
        saleItemsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        saleItemsTable.setRowHeight(36);
        saleItemsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        saleItemsTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        saleItemsTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox()));

        saleItemsTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onSaleItemSelected();
                }
            }
        });

        JScrollPane itemsScroll = new JScrollPane(saleItemsTable);
        itemsScroll.setBorder(BorderFactory.createTitledBorder("Sale Items (Click 'Select' to return an item)"));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Selected item info
        selectedItemLabel = new JLabel("No item selected");
        selectedItemLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formPanel.add(selectedItemLabel, gbc);

        // Row 1: Reason
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Return Reason:"), gbc);
        String[] reasonNames = returnReasons.values().toArray(new String[0]);
        reasonCombo = new JComboBox<>(reasonNames);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7;
        formPanel.add(reasonCombo, gbc);

        // Row 2: Quantity
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Quantity to Return:"), gbc);
        quantityField = new JTextField();
        quantityField.putClientProperty("JTextField.placeholderText", "Max: 0");
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7;
        formPanel.add(quantityField, gbc);

        // Row 3: Notes
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Notes:"), gbc);
        notesArea = new JTextArea(3, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JScrollPane notesScroll = new JScrollPane(notesArea);
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.7;
        formPanel.add(notesScroll, gbc);

        // Submit button
        JButton submitBtn = new JButton("Submit Return Request");
        styleButton(submitBtn, new Color(76, 175, 80));
        submitBtn.addActionListener(e -> createReturn());

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 8, 0, 8);
        formPanel.add(submitBtn, gbc);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.add(itemsScroll, BorderLayout.CENTER);
        mainPanel.add(formPanel, BorderLayout.SOUTH);

        panel.add(formTitle, BorderLayout.NORTH);
        panel.add(mainPanel, BorderLayout.CENTER);

        return panel;
    }

    // ---------- Returns History Panel ----------
    private JPanel createReturnsHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel("Returns History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Search/Filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Show all returns checkbox
        showAllReturnsCheckbox = new JCheckBox("Show All Returns");
        showAllReturnsCheckbox.setSelected(true);
        showAllReturnsCheckbox.addActionListener(e -> {
            if (showAllReturnsCheckbox.isSelected()) {
                loadAllReturns();
            } else if (selectedSaleId != null) {
                loadReturnsForSelectedSale();
            } else {
                returnsModel.setRowCount(0);
                returnsSummaryLabel.setText("Select a sale to view its returns, or check 'Show All Returns'");
            }
        });

        JTextField searchReturnsField = new JTextField(15);
        searchReturnsField.putClientProperty("JTextField.placeholderText", "Search by sale ID, customer, or product...");

        JButton searchReturnsBtn = new JButton("Search");
        styleButton(searchReturnsBtn, new Color(33, 150, 243));
        searchReturnsBtn.addActionListener(e -> {
            String searchText = searchReturnsField.getText().trim();
            if (!searchText.isEmpty()) {
                searchReturns(searchText);
            } else {
                if (showAllReturnsCheckbox.isSelected()) {
                    loadAllReturns();
                } else if (selectedSaleId != null) {
                    loadReturnsForSelectedSale();
                }
            }
        });

        JButton refreshReturnsBtn = new JButton("Refresh");
        styleButton(refreshReturnsBtn, new Color(158, 158, 158));
        refreshReturnsBtn.addActionListener(e -> {
            if (showAllReturnsCheckbox.isSelected()) {
                loadAllReturns();
            } else if (selectedSaleId != null) {
                loadReturnsForSelectedSale();
            }
        });

        filterPanel.add(showAllReturnsCheckbox);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchReturnsField);
        filterPanel.add(searchReturnsBtn);
        filterPanel.add(refreshReturnsBtn);

        // Returns table
        String[] columns = {"Return ID", "Sale ID", "Product", "Quantity", "Reason", "Status", "Created", "Customer", "Cashier"};
        returnsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        returnsTable = new JTable(returnsModel);
        returnsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        returnsTable.setRowHeight(36);
        returnsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        returnsTable.setFillsViewportHeight(true);
        returnsTable.setAutoCreateRowSorter(true);
        returnsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Set up custom renderer for status column (column 5)
        returnsTable.getColumnModel().getColumn(5).setCellRenderer(new StatusCellRenderer());

        returnsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showReturnDetails();
            }
        });

        JScrollPane scrollPane = new JScrollPane(returnsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Returns"));
        scrollPane.setPreferredSize(new Dimension(700, 320));

        // Summary label
        returnsSummaryLabel = new JLabel("Loading all returns...");
        returnsSummaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        returnsSummaryLabel.setForeground(new Color(100, 100, 100));
        returnsSummaryLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        // Build top area with title + filters so table gets full center space
        JPanel topArea = new JPanel(new BorderLayout());
        topArea.setOpaque(false);
        topArea.add(title, BorderLayout.NORTH);
        topArea.add(filterPanel, BorderLayout.SOUTH);

        panel.add(topArea, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(returnsSummaryLabel, BorderLayout.SOUTH);

        return panel;
    }

    // ---------- Data Methods ----------
    private void loadRecentSales() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> sales = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/sales");
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            sales = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                sales = (List<Map<String, Object>>) r.get("data");
                            } else {
                                sales = new ArrayList<>();
                            }
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to load sales: " + error.getMessage());
                    return;
                }

                // Filter out sales that are in PENDING status
                recentSales.clear();
                if (sales != null && !sales.isEmpty()) {
                    for (Map<String, Object> sale : sales) {
                        if (!isSalePending(sale)) {
                            recentSales.add(sale);
                        }
                    }
                }

                updateSalesTable();

                // Clear selections
                selectedSaleId = null;
                selectedSaleLabel.setText("No sale selected");
                clearSaleItems();
                clearReturnForm();
            }
        };
        worker.execute();
    }

    private void loadAllProducts() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/products");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> products = client.parseResponseList(resp);
                        productNameCache.clear();
                        for (Map<String, Object> product : products) {
                            Integer id = safeIntegerFromObject(product.get("id"));
                            String name = Objects.toString(product.get("name"), "");
                            if (id != null && !name.isEmpty()) {
                                productNameCache.put(id, name);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }
        };
        worker.execute();
    }

    private void loadAllReturns() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> returns = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/returns");
                    System.out.println("DEBUG - Response from /api/secure/returns: " + resp); // Debug line

                    if (resp != null && !resp.trim().isEmpty()) {
                        // Try to parse as JSON array first
                        try {
                            returns = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                            System.out.println("DEBUG - Parsed as array, size: " + returns.size()); // Debug
                        } catch (Exception ex) {
                            System.out.println("DEBUG - Not an array, trying as object: " + ex.getMessage()); // Debug
                            // Try as object with data field
                            Map<String, Object> responseMap = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                            if (responseMap != null && responseMap.containsKey("data")) {
                                Object data = responseMap.get("data");
                                if (data instanceof List) {
                                    returns = (List<Map<String, Object>>) data;
                                    System.out.println("DEBUG - Found data in object, size: " + returns.size()); // Debug
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to load returns: " + error.getMessage());
                    return;
                }

                returnsList.clear();
                returnsModel.setRowCount(0);

                if (returns != null && !returns.isEmpty()) {
                    System.out.println("DEBUG - Adding " + returns.size() + " returns to table"); // Debug
                    returnsList.addAll(returns);
                    updateReturnsTable();

                    int pendingCount = 0;
                    int approvedCount = 0;
                    int rejectedCount = 0;

                    for (Map<String, Object> ret : returns) {
                        String status = Objects.toString(ret.get("status"), "");
                        if ("PENDING".equalsIgnoreCase(status)) pendingCount++;
                        else if ("APPROVED".equalsIgnoreCase(status)) approvedCount++;
                        else if ("REJECTED".equalsIgnoreCase(status)) rejectedCount++;
                    }

                    returnsSummaryLabel.setText(String.format(
                            "All Returns: %d total (Pending: %d, Approved: %d, Rejected: %d)",
                            returns.size(), pendingCount, approvedCount, rejectedCount
                    ));
                } else {
                    returnsSummaryLabel.setText("No returns found in the system");
                }
            }
        };
        worker.execute();
    }
    private String getProductName(Integer productId) {
        if (productId == null) return "Unknown Product";
        return productNameCache.getOrDefault(productId, "Product #" + productId);
    }

    private void searchSales(String query) {
        // Simple client-side search
        if (query == null || query.trim().isEmpty()) {
            updateSalesTable();
            return;
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Map<String, Object> sale : recentSales) {
            // Search in sale number
            String saleNumber = Objects.toString(sale.get("saleNumber"), "").toLowerCase();
            // Search in customer info
            String customerDisplay = getCustomerDisplay(sale).toLowerCase();

            if (saleNumber.contains(lowerQuery) || customerDisplay.contains(lowerQuery)) {
                filtered.add(sale);
            }
        }

        SwingUtilities.invokeLater(() -> {
            salesModel.setRowCount(0);
            for (Map<String, Object> sale : filtered) {
                addSaleToTable(sale);
            }
        });
    }

    private void searchReturns(String query) {
        if (query == null || query.trim().isEmpty()) {
            if (showAllReturnsCheckbox.isSelected()) {
                loadAllReturns();
            } else if (selectedSaleId != null) {
                loadReturnsForSelectedSale();
            }
            return;
        }

        List<Map<String, Object>> filtered = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Map<String, Object> ret : returnsList) {
            // Search in sale ID
            Integer saleId = safeIntegerFromObject(ret.get("saleId"));
            if (saleId != null && String.valueOf(saleId).contains(lowerQuery)) {
                filtered.add(ret);
                continue;
            }

            // Search in customer name
            String customerName = Objects.toString(ret.get("customerName"), "").toLowerCase();
            if (customerName.contains(lowerQuery)) {
                filtered.add(ret);
                continue;
            }

            // Search in customer phone
            String customerPhone = Objects.toString(ret.get("customerPhone"), "").toLowerCase();
            if (customerPhone.contains(lowerQuery)) {
                filtered.add(ret);
                continue;
            }

            // Search in product name (via productId)
            Integer productId = safeIntegerFromObject(ret.get("productId"));
            if (productId != null) {
                String productName = getProductName(productId).toLowerCase();
                if (productName.contains(lowerQuery)) {
                    filtered.add(ret);
                    continue;
                }
            }

            // Search in return reason
            String reason = Objects.toString(ret.get("reason"), "").toLowerCase();
            if (reason.contains(lowerQuery)) {
                filtered.add(ret);
                continue;
            }

            // Search in cashier name
            String cashierName = Objects.toString(ret.get("saleCashierName"), "").toLowerCase();
            if (cashierName.contains(lowerQuery)) {
                filtered.add(ret);
            }
        }

        SwingUtilities.invokeLater(() -> {
            returnsModel.setRowCount(0);
            for (Map<String, Object> ret : filtered) {
                addReturnToTable(ret);
            }

            returnsSummaryLabel.setText(String.format(
                    "Showing %d of %d returns for search: '%s'",
                    filtered.size(), returnsList.size(), query
            ));
        });
    }

    private void updateSalesTable() {
        SwingUtilities.invokeLater(() -> {
            salesModel.setRowCount(0);
            for (Map<String, Object> sale : recentSales) {
                addSaleToTable(sale);
            }
        });
    }

    private void addSaleToTable(Map<String, Object> sale) {
        Integer saleId = safeIntegerFromObject(sale.get("saleId") != null ? sale.get("saleId") : sale.get("id"));
        String saleNumber = Objects.toString(sale.get("saleNumber"), Objects.toString(sale.get("saleNo"), "N/A"));
        String customerDisplay = getCustomerDisplay(sale);
        Double total = safeDoubleFromObject(sale.get("totalAmount") != null ? sale.get("totalAmount") : sale.get("total"));
        String status = Objects.toString(sale.get("paymentStatus"), "N/A");
        String date = Objects.toString(sale.get("saleDate"), "");

        salesModel.addRow(new Object[]{
                saleId,
                saleNumber,
                customerDisplay,
                String.format("ksh %,.2f", total != null ? total : 0.0),
                status,
                date.length() > 16 ? date.substring(0, 16) : date
        });
    }

    private String getCustomerDisplay(Map<String, Object> sale) {
        if (sale.containsKey("customerName") || sale.containsKey("customerPhone")) {
            String name = Objects.toString(sale.get("customerName"), "").trim();
            String phone = Objects.toString(sale.get("customerPhone"), "").trim();
            if (!name.isEmpty() && !phone.isEmpty()) return name + " (" + phone + ")";
            else if (!name.isEmpty()) return name;
            else if (!phone.isEmpty()) return phone;
        }
        return "N/A";
    }

    private void onSaleSelected() {
        int selectedRow = salesTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = salesTable.convertRowIndexToModel(selectedRow);
            if (modelRow < recentSales.size()) {
                Map<String, Object> sale = recentSales.get(modelRow);
                selectedSaleId = safeIntegerFromObject(sale.get("saleId") != null ? sale.get("saleId") : sale.get("id"));

                String saleNumber = Objects.toString(sale.get("saleNumber"), "N/A");
                String customer = getCustomerDisplay(sale);
                selectedSaleLabel.setText(String.format("Selected: %s | Customer: %s", saleNumber, customer));

                loadSaleItems(selectedSaleId);

                // If "Show All Returns" is not checked, load returns for this sale
                if (!showAllReturnsCheckbox.isSelected()) {
                    loadReturnsForSelectedSale();
                }
            }
        }
    }

    private void loadSaleItems(Integer saleId) {
        if (saleId == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> items = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    // First, get the sale details to get items
                    String resp = client.get("/api/secure/sales/" + saleId);
                    if (resp != null && !resp.trim().isEmpty()) {
                        Map<String, Object> saleDetail = client.parseResponse(resp);
                        if (saleDetail != null) {
                            // Try to extract items from different possible structures
                            if (saleDetail.containsKey("items") && saleDetail.get("items") instanceof List) {
                                items = (List<Map<String, Object>>) saleDetail.get("items");
                            } else if (saleDetail.containsKey("data")) {
                                Object data = saleDetail.get("data");
                                if (data instanceof Map) {
                                    Map<String, Object> dataMap = (Map<String, Object>) data;
                                    if (dataMap.containsKey("items") && dataMap.get("items") instanceof List) {
                                        items = (List<Map<String, Object>>) dataMap.get("items");
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to load sale items: " + error.getMessage());
                    return;
                }

                saleItems.clear();
                saleItemsModel.setRowCount(0);

                if (items != null && !items.isEmpty()) {
                    saleItems.addAll(items);
                    updateSaleItemsTable();
                } else {
                    saleItemsModel.addRow(new Object[]{
                            "N/A", "No items found", "N/A", "N/A", "N/A", "N/A", "N/A"
                    });
                }

                clearReturnForm();
            }
        };
        worker.execute();
    }

    private void updateSaleItemsTable() {
        SwingUtilities.invokeLater(() -> {
            saleItemsModel.setRowCount(0);
            for (Map<String, Object> item : saleItems) {
                Integer itemId = safeIntegerFromObject(item.get("id"));
                Integer productId = safeIntegerFromObject(item.get("productId"));
                String productName = getProductName(productId);
                Integer quantity = safeIntegerFromObject(item.get("quantity"));
                Double unitPrice = safeDoubleFromObject(item.get("unitPrice"));
                Double total = safeDoubleFromObject(item.get("total"));

                if (itemId != null) {
                    saleItemsModel.addRow(new Object[]{
                            itemId,
                            productName,
                            productId != null ? productId : "N/A",
                            quantity != null ? quantity : 0,
                            String.format("ksh %,.2f", unitPrice != null ? unitPrice : 0.0),
                            String.format("ksh %,.2f", total != null ? total : 0.0),
                            "Select"
                    });
                }
            }
        });
    }

    private void onSaleItemSelected() {
        int selectedRow = saleItemsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = saleItemsTable.convertRowIndexToModel(selectedRow);
            if (modelRow < saleItems.size()) {
                Map<String, Object> item = saleItems.get(modelRow);
                selectedSaleItemId = safeIntegerFromObject(item.get("id"));
                selectedProductId = safeIntegerFromObject(item.get("productId"));
                selectedItemQuantity = safeIntegerFromObject(item.get("quantity"), 0);

                String productName = getProductName(selectedProductId);
                Integer maxQuantity = selectedItemQuantity;

                selectedItemLabel.setText(String.format(
                        "Selected: %s (Product ID: %d) | Available Quantity: %d",
                        productName, selectedProductId, maxQuantity
                ));

                quantityField.putClientProperty("JTextField.placeholderText",
                        "Max: " + maxQuantity);
            }
        }
    }

    private void loadReturnsForSelectedSale() {
        if (selectedSaleId == null) {
            returnsModel.setRowCount(0);
            returnsSummaryLabel.setText("Select a sale to view its returns, or check 'Show All Returns'");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> returns = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/returns/sale/" + selectedSaleId);
                    System.out.println("DEBUG - Response for sale " + selectedSaleId + ": " + resp); // Debug

                    if (resp != null && !resp.trim().isEmpty()) {
                        // Try to parse as JSON array first
                        try {
                            returns = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        } catch (Exception ex) {
                            // Try as object with data field
                            Map<String, Object> responseMap = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                            if (responseMap != null && responseMap.containsKey("data")) {
                                Object data = responseMap.get("data");
                                if (data instanceof List) {
                                    returns = (List<Map<String, Object>>) data;
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to load returns: " + error.getMessage());
                    return;
                }

                returnsList.clear();
                returnsModel.setRowCount(0);

                if (returns != null && !returns.isEmpty()) {
                    returnsList.addAll(returns);
                    updateReturnsTable();

                    int pendingCount = 0;
                    int approvedCount = 0;
                    int rejectedCount = 0;

                    for (Map<String, Object> ret : returns) {
                        String status = Objects.toString(ret.get("status"), "");
                        if ("PENDING".equalsIgnoreCase(status)) pendingCount++;
                        else if ("APPROVED".equalsIgnoreCase(status)) approvedCount++;
                        else if ("REJECTED".equalsIgnoreCase(status)) rejectedCount++;
                    }

                    returnsSummaryLabel.setText(String.format(
                            "Sale ID: %d | Returns: %d (Pending: %d, Approved: %d, Rejected: %d)",
                            selectedSaleId, returns.size(), pendingCount, approvedCount, rejectedCount
                    ));
                } else {
                    returnsSummaryLabel.setText("No returns found for Sale ID: " + selectedSaleId);
                }
            }
        };
        worker.execute();
    }

    private void updateReturnsTable() {
        SwingUtilities.invokeLater(() -> {
            returnsModel.setRowCount(0);
            for (Map<String, Object> ret : returnsList) {
                addReturnToTable(ret);
            }
            // Refresh UI explicitly
            returnsTable.revalidate();
            returnsTable.repaint();
        });
    }

    private void addReturnToTable(Map<String, Object> ret) {
        Integer id = safeIntegerFromObject(ret.get("id"));
        Integer saleId = safeIntegerFromObject(ret.get("saleId"));

        // Try to get product name for the return
        String productName = "Unknown Product";
        Integer productId = safeIntegerFromObject(ret.get("productId"));
        if (productId != null) {
            productName = getProductName(productId);
        } else {
            // Try to get from sale item ID (would need additional API call)
            // For now, just show generic
            productName = "Item #" + Objects.toString(ret.get("saleItemId"), "N/A");
        }

        Integer quantity = safeIntegerFromObject(ret.get("quantity"));
        String reason = Objects.toString(ret.get("reason"), "N/A");
        String status = Objects.toString(ret.get("status"), "N/A");
        String createdAt = formatDate(Objects.toString(ret.get("createdAt"), ""));

        String customerName = Objects.toString(ret.get("customerName"), "");
        String customerPhone = Objects.toString(ret.get("customerPhone"), "");
        String customerDisplay = customerName.isEmpty() ? customerPhone :
                customerName + " (" + customerPhone + ")";
        if (customerDisplay.isEmpty()) customerDisplay = "N/A";

        String cashierName = Objects.toString(ret.get("saleCashierName"), "N/A");

        // Store status as a string - the renderer will handle the coloring
        // Text color is handled by the StatusCellRenderer to ensure it's black
        returnsModel.addRow(new Object[]{
                id,
                saleId,
                productName,
                quantity,
                reason,
                status,  // Just the string, renderer will color it
                createdAt.length() > 16 ? createdAt.substring(0, 16) : createdAt,
                customerDisplay,
                cashierName
        });
    }

    private void createReturn() {
        // Validation
        if (selectedSaleId == null) {
            showError("Please select a sale first");
            return;
        }

        if (selectedSaleItemId == null) {
            showError("Please select an item to return");
            return;
        }

        if (selectedProductId == null) {
            showError("Unable to determine the product. Please select the item again.");
            return;
        }

        String quantityText = quantityField.getText().trim();
        if (quantityText.isEmpty()) {
            showError("Please enter the quantity to return");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityText);
            if (quantity <= 0) {
                showError("Quantity must be greater than 0");
                return;
            }
            if (quantity > selectedItemQuantity) {
                showError(String.format("Cannot return more than purchased quantity (Max: %d)", selectedItemQuantity));
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid quantity number");
            return;
        }

        // Get reason ID
        String selectedReason = (String) reasonCombo.getSelectedItem();
        int reasonId = getKeyByValue(returnReasons, selectedReason);

        if (reasonId == -1) {
            showError("Please select a valid return reason");
            return;
        }

        // Get current user ID
        Integer cashierId = session.getUserId();
        if (cashierId == null) {
            showError("Unable to determine your user ID. Please log in again.");
            return;
        }

        // Prepare request
        Map<String, Object> request = new HashMap<>();
        request.put("saleId", selectedSaleId);
        request.put("saleItemId", selectedSaleItemId);
        request.put("reasonId", reasonId);
        request.put("quantity", quantity);
        request.put("cashierId", cashierId);

        String notes = notesArea.getText().trim();
        if (!notes.isEmpty()) {
            request.put("notes", notes);
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";
            private Map<String, Object> createdReturn = null;

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.post("/api/secure/returns", request);

                    // Try to parse the response as a map
                    Map<String, Object> responseMap = client.parseResponse(resp);

                    // Check if response has "success" field (error response)
                    if (responseMap.containsKey("success") && Boolean.FALSE.equals(responseMap.get("success"))) {
                        success = false;
                        message = Objects.toString(responseMap.get("message"), "Unknown error");
                    } else {
                        // Success response - the API returns the return object directly
                        success = true;
                        createdReturn = responseMap;
                        message = "Return created successfully!";
                    }
                } catch (Exception e) {
                    success = false;
                    message = "Error: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    // Create a detailed success message
                    String productName = getProductName(selectedProductId);
                    String customerName = createdReturn != null ?
                            Objects.toString(createdReturn.get("customerName"), "N/A") : "N/A";
                    String returnId = createdReturn != null ?
                            Objects.toString(createdReturn.get("id"), "N/A") : "N/A";

                    StringBuilder successMsg = new StringBuilder();
                    successMsg.append("RETURN REQUEST SUBMITTED SUCCESSFULLY!\n\n");
                    successMsg.append(String.format("Return ID: %s\n", returnId));
                    successMsg.append(String.format("Sale ID: %d\n", selectedSaleId));
                    successMsg.append(String.format("Product: %s\n", productName));
                    successMsg.append(String.format("Quantity: %d\n", quantity));
                    successMsg.append(String.format("Customer: %s\n", customerName));
                    successMsg.append(String.format("Reason: %s\n", selectedReason));
                    successMsg.append(String.format("Status: %s\n",
                            createdReturn != null ? Objects.toString(createdReturn.get("status"), "PENDING") : "PENDING"));

                    JTextArea textArea = new JTextArea(successMsg.toString());
                    textArea.setEditable(false);
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

                    JScrollPane scrollPane = new JScrollPane(textArea);
                    scrollPane.setPreferredSize(new Dimension(400, 200));

                    JOptionPane.showMessageDialog(ReturnPanel.this, scrollPane,
                            "Return Request Successful", JOptionPane.INFORMATION_MESSAGE);

                    // Clear form and refresh data
                    clearReturnForm();

                    // Refresh returns list based on current view
                    if (showAllReturnsCheckbox.isSelected()) {
                        loadAllReturns();
                    } else {
                        loadReturnsForSelectedSale();
                    }

                    // Refresh sale items (quantity might have changed)
                    loadSaleItems(selectedSaleId);
                } else {
                    // Show custom dialog for quantity error
                    String message = "<html><b>Cannot Return More Than Purchased Quantity</b><br><br>"
                            + "Possible reasons:<br>"
                            + "• Item may have been returned already<br>"
                            + "• You entered a quantity higher than what was sold<br>"
                            + "• Check return history for previous returns<br><br>"
                            + "<b>Action:</b> Enter the exact quantity available for return as shown in the sale details.</html>";

                    JOptionPane.showMessageDialog(ReturnPanel.this,
                            message,
                            "Return Validation Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ---------- Helper Methods ----------
    private void clearSaleItems() {
        saleItems.clear();
        saleItemsModel.setRowCount(0);
        saleItemsModel.addRow(new Object[]{
                "N/A", "Select a sale to view items", "N/A", "N/A", "N/A", "N/A", "N/A"
        });
    }

    private void clearReturnForm() {
        selectedSaleItemId = null;
        selectedProductId = null;
        selectedItemQuantity = 0;
        selectedItemLabel.setText("No item selected");
        reasonCombo.setSelectedIndex(0);
        quantityField.setText("");
        notesArea.setText("");
        quantityField.putClientProperty("JTextField.placeholderText", "Max: 0");
    }

    private void showReturnDetails() {
        int selectedRow = returnsTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = returnsTable.convertRowIndexToModel(selectedRow);
            if (modelRow < returnsList.size()) {
                Map<String, Object> returnData = returnsList.get(modelRow);

                StringBuilder details = new StringBuilder();
                details.append("RETURN DETAILS\n");
                details.append("==============\n\n");

                details.append(String.format("Return ID: %s%n",
                        Objects.toString(returnData.get("id"), "N/A")));
                details.append(String.format("Sale ID: %s%n",
                        Objects.toString(returnData.get("saleId"), "N/A")));
                details.append(String.format("Sale Item ID: %s%n",
                        Objects.toString(returnData.get("saleItemId"), "N/A")));
                details.append(String.format("Quantity: %s%n",
                        Objects.toString(returnData.get("quantity"), "N/A")));
                details.append(String.format("Reason: %s%n",
                        Objects.toString(returnData.get("reason"), "N/A")));
                details.append(String.format("Status: %s%n",
                        Objects.toString(returnData.get("status"), "N/A")));
                details.append(String.format("Created: %s%n",
                        formatDate(Objects.toString(returnData.get("createdAt"), ""))));

                details.append("\nCUSTOMER INFO\n");
                details.append("-------------\n");
                details.append(String.format("Customer: %s%n",
                        Objects.toString(returnData.get("customerName"), "N/A")));
                details.append(String.format("Phone: %s%n",
                        Objects.toString(returnData.get("customerPhone"), "N/A")));

                details.append("\nSALE INFORMATION\n");
                details.append("----------------\n");
                details.append(String.format("Sale Cashier: %s%n",
                        Objects.toString(returnData.get("saleCashierName"), "N/A")));
                details.append(String.format("Return Cashier ID: %s%n",
                        Objects.toString(returnData.get("cashierId"), "N/A")));

                // Try to get product info
                Integer productId = safeIntegerFromObject(returnData.get("productId"));
                if (productId != null) {
                    details.append(String.format("Product: %s (ID: %d)%n",
                            getProductName(productId), productId));
                }

                // Display in dialog
                JTextArea textArea = new JTextArea(details.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(450, 350));

                JOptionPane.showMessageDialog(this, scrollPane,
                        "Return Details", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "";
        }
        try {
            if (dateString.length() > 10) {
                return dateString.substring(0, 16).replace("T", " ");
            }
            return dateString;
        } catch (Exception e) {
            return dateString;
        }
    }

    private int getKeyByValue(Map<Integer, String> map, String value) {
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    private Integer safeIntegerFromObject(Object o) {
        return safeIntegerFromObject(o, null);
    }

    private Integer safeIntegerFromObject(Object o, Integer defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Double safeDoubleFromObject(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    // ---------- Table button renderer/editor ----------
    class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setText("Select");
            setBackground(new Color(33, 150, 243));
            setForeground(Color.WHITE);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 11));
        }

        public Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
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
            button.setBackground(new Color(33, 150, 243));
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setText("Select");
            button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            button.addActionListener(e -> {
                fireEditingStopped();
                if (row >= 0 && row < saleItems.size()) {
                    saleItemsTable.setRowSelectionInterval(row, row);
                    onSaleItemSelected();
                }
            });
        }

        public Component getTableCellEditorComponent(javax.swing.JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            return button;
        }

        public Object getCellEditorValue() { return "Select"; }
    }

    // ---------- Status Cell Renderer (for black text) ----------
    class StatusCellRenderer extends DefaultTableCellRenderer {
        public StatusCellRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            // default
            setForeground(Color.BLACK);
            setOpaque(true);

            if (value != null) {
                String status = value.toString();

                // Set background color based on status
                if ("PENDING".equalsIgnoreCase(status)) {
                    setBackground(new Color(255, 193, 7)); // Yellow
                } else if ("APPROVED".equalsIgnoreCase(status)) {
                    setBackground(new Color(76, 175, 80)); // Green
                } else if ("REJECTED".equalsIgnoreCase(status)) {
                    setBackground(new Color(244, 67, 54)); // Red
                } else {
                    setBackground(Color.WHITE);
                }
            } else {
                setBackground(Color.WHITE);
            }

            return this;
        }
    }

    /**
     * Returns true if the sale (or nested data) has any of the common status keys set to "PENDING".
     */
    private boolean isSalePending(Map<String, Object> sale) {
        if (sale == null) return false;

        // Check common status keys on the root object
        String[] keys = {"status", "paymentStatus", "saleStatus"};
        for (String key : keys) {
            String v = Objects.toString(sale.get(key), "");
            if ("PENDING".equalsIgnoreCase(v)) return true;
        }

        // If payload has a nested "data" object, check there too
        Object data = sale.get("data");
        if (data instanceof Map) {
            Map<?, ?> dm = (Map<?, ?>) data;
            for (String key : keys) {
                String v = Objects.toString(dm.get(key), "");
                if ("PENDING".equalsIgnoreCase(v)) return true;
            }
        }

        return false;
    }

}
