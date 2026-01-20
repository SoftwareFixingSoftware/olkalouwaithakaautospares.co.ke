
package olkalouwaithakaautospares.co.ke.win.ui.dashboard.master;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * InventoryPanel - Complete inventory management system
 *
 * Tabbed interface for managing:
 * 1. Categories
 * 2. Brands
 * 3. Products
 * 4. Stock Batches
 */
@SuppressWarnings("unchecked")
public class InventoryPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    // Data lists
    private final List<Map<String, Object>> categoriesList = new ArrayList<>();
    private final List<Map<String, Object>> brandsList = new ArrayList<>();
    private final List<Map<String, Object>> productsList = new ArrayList<>();
    private final List<Map<String, Object>> stockBatchesList = new ArrayList<>();

    // Category components
    private JTable categoriesTable;
    private DefaultTableModel categoriesModel;
    private JTextField categoryNameField;
    private JTextArea categoryDescArea;

    // Brand components
    private JTable brandsTable;
    private DefaultTableModel brandsModel;
    private JTextField brandNameField;
    private JTextField brandCountryField;

    // Product components
    private JTable productsTable;
    private DefaultTableModel productsModel;
    private JComboBox<String> productCategoryCombo;
    private JComboBox<String> productBrandCombo;
    private JTextField productSkuField;
    private JTextField productNameField;
    private JTextArea productDescArea;
    private JTextField productMinPriceField;
    private JTextField productReorderField;
    private JCheckBox productActiveCheck;
    private Map<Integer, String> categoriesMap = new HashMap<>();
    private Map<Integer, String> brandsMap = new HashMap<>();

    // Stock Batch components
    private JTable stockBatchesTable;
    private DefaultTableModel stockBatchesModel;
    private JComboBox<String> batchProductCombo;
    private JTextField batchNumberField;
    private JTextField batchBuyingPriceField;
    private JTextField batchSellingPriceField;
    private JTextField batchQuantityField;
    private JTextField batchExpiryField;
    private Map<Integer, String> productsMap = new HashMap<>();

    public InventoryPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();

        initUI();
        loadAllData();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Main tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // Create tabs
        JPanel categoriesTab = createCategoriesTab();
        JPanel brandsTab = createBrandsTab();
        JPanel productsTab = createProductsTab();
        JPanel stockBatchesTab = createStockBatchesTab();

        tabbedPane.addTab("Categories", categoriesTab);
        tabbedPane.addTab("Brands", brandsTab);
        tabbedPane.addTab("Products", productsTab);
        tabbedPane.addTab("Stock Batches", stockBatchesTab);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Inventory Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(30, 33, 57));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton refreshBtn = new JButton("Refresh All");
        styleButton(refreshBtn, new Color(33, 150, 243));
        refreshBtn.addActionListener(e -> loadAllData());

        JButton reportBtn = new JButton("Generate Report");
        styleButton(reportBtn, new Color(156, 39, 176));
        reportBtn.addActionListener(e -> showInventoryReport());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(reportBtn);

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

    // ========== CATEGORIES TAB ==========
    private JPanel createCategoriesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Categories"));

        String[] columns = {"ID", "Name", "Description", "Created"};
        categoriesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        categoriesTable = new JTable(categoriesModel);
        categoriesTable.setAutoCreateRowSorter(true);
        categoriesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoriesTable.setRowHeight(35);
        categoriesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        categoriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Set column widths
        categoriesTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        categoriesTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        categoriesTable.getColumnModel().getColumn(2).setPreferredWidth(250);
        categoriesTable.getColumnModel().getColumn(3).setPreferredWidth(150);

        JScrollPane scrollPane = new JScrollPane(categoriesTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Category"));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Category Name:"), gbc);
        categoryNameField = new JTextField(20);
        categoryNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        formPanel.add(categoryNameField, gbc);

        // Row 2: Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Description:"), gbc);
        categoryDescArea = new JTextArea(3, 20);
        categoryDescArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        categoryDescArea.setLineWrap(true);
        JScrollPane descScroll = new JScrollPane(categoryDescArea);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.8;
        formPanel.add(descScroll, gbc);

        // Row 3: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton clearCatBtn = new JButton("Clear");
        styleButton(clearCatBtn, new Color(158, 158, 158));
        clearCatBtn.addActionListener(e -> clearCategoryForm());

        JButton addCatBtn = new JButton("Add Category");
        styleButton(addCatBtn, new Color(76, 175, 80));
        addCatBtn.addActionListener(e -> addCategory());

        buttonPanel.add(clearCatBtn);
        buttonPanel.add(addCatBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(buttonPanel, gbc);

        // Split pane for table and form
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, formPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.7);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ========== BRANDS TAB ==========
    private JPanel createBrandsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Brands"));

        String[] columns = {"ID", "Name", "Country", "Created"};
        brandsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        brandsTable = new JTable(brandsModel);
        brandsTable.setAutoCreateRowSorter(true);
        brandsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        brandsTable.setRowHeight(35);
        brandsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        brandsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(brandsTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Brand"));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Brand Name:"), gbc);
        brandNameField = new JTextField(20);
        brandNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        formPanel.add(brandNameField, gbc);

        // Row 2: Country
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Country:"), gbc);
        brandCountryField = new JTextField(20);
        brandCountryField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.8;
        formPanel.add(brandCountryField, gbc);

        // Row 3: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton clearBrandBtn = new JButton("Clear");
        styleButton(clearBrandBtn, new Color(158, 158, 158));
        clearBrandBtn.addActionListener(e -> clearBrandForm());

        JButton addBrandBtn = new JButton("Add Brand");
        styleButton(addBrandBtn, new Color(76, 175, 80));
        addBrandBtn.addActionListener(e -> addBrand());

        buttonPanel.add(clearBrandBtn);
        buttonPanel.add(addBrandBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formPanel.add(buttonPanel, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, formPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.7);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ========== PRODUCTS TAB ==========
    private JPanel createProductsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Products"));

        String[] columns = {"ID", "SKU", "Name", "Category", "Brand", "Min Price", "Reorder Level", "Active", "Stock"};
        productsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productsTable = new JTable(productsModel);
        productsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productsTable.setRowHeight(35);
        productsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Color active status
        productsTable.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());

        JScrollPane scrollPane = new JScrollPane(productsTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Product"));
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(800, 400));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: SKU and Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("SKU:"), gbc);
        productSkuField = new JTextField(15);
        productSkuField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productSkuField.setEditable(false);
        productSkuField.setText(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(productSkuField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Product Name:"), gbc);
        productNameField = new JTextField(15);
        productNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(productNameField, gbc);

        // Row 2: Category and Brand
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Category:"), gbc);
        productCategoryCombo = new JComboBox<>();
        productCategoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(productCategoryCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Brand:"), gbc);
        productBrandCombo = new JComboBox<>();
        productBrandCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(productBrandCombo, gbc);

        // Row 3: Prices
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Min Price (₦):"), gbc);
        productMinPriceField = new JTextField(15);
        productMinPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(productMinPriceField, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Reorder Level:"), gbc);
        productReorderField = new JTextField(15);
        productReorderField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(productReorderField, gbc);

        // Row 4: Description
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Description:"), gbc);
        productDescArea = new JTextArea(3, 20);
        productDescArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productDescArea.setLineWrap(true);
        JScrollPane prodDescScroll = new JScrollPane(productDescArea);
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 0.5;
        formPanel.add(prodDescScroll, gbc);

        // Row 4 (cont): Active checkbox
        productActiveCheck = new JCheckBox("Active");
        productActiveCheck.setSelected(true);
        productActiveCheck.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 3; gbc.gridwidth = 1; gbc.weightx = 0.3;
        formPanel.add(productActiveCheck, gbc);

        // Row 5: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton clearProdBtn = new JButton("Clear");
        styleButton(clearProdBtn, new Color(158, 158, 158));
        clearProdBtn.addActionListener(e -> clearProductForm());

        JButton addProdBtn = new JButton("Add Product");
        styleButton(addProdBtn, new Color(76, 175, 80));
        addProdBtn.addActionListener(e -> addProduct());

        buttonPanel.add(clearProdBtn);
        buttonPanel.add(addProdBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(buttonPanel, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, formPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.6);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ========== STOCK BATCHES TAB ==========
    private JPanel createStockBatchesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Stock Batches"));

        String[] columns = {"ID", "Product", "Batch No", "Buying Price", "Selling Price", "Qty Received", "Qty Remaining", "Received Date", "Added By"};
        stockBatchesModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        stockBatchesTable = new JTable(stockBatchesModel);
        stockBatchesTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stockBatchesTable.setRowHeight(35);
        stockBatchesTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(stockBatchesTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Stock Batch"));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Product and Batch Number
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Product:"), gbc);
        batchProductCombo = new JComboBox<>();
        batchProductCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(batchProductCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Batch Number:"), gbc);
        batchNumberField = new JTextField(15);
        batchNumberField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(batchNumberField, gbc);

        // Row 2: Prices
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Buying Price (₦):"), gbc);
        batchBuyingPriceField = new JTextField(15);
        batchBuyingPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(batchBuyingPriceField, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Selling Price (₦):"), gbc);
        batchSellingPriceField = new JTextField(15);
        batchSellingPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(batchSellingPriceField, gbc);

        // Row 3: Quantity and Expiry
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Quantity:"), gbc);
        batchQuantityField = new JTextField(15);
        batchQuantityField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(batchQuantityField, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Expiry Date (optional):"), gbc);
        batchExpiryField = new JTextField(15);
        batchExpiryField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        batchExpiryField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(batchExpiryField, gbc);

        // Row 4: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton clearBatchBtn = new JButton("Clear");
        styleButton(clearBatchBtn, new Color(158, 158, 158));
        clearBatchBtn.addActionListener(e -> clearBatchForm());

        JButton addBatchBtn = new JButton("Add Stock Batch");
        styleButton(addBatchBtn, new Color(76, 175, 80));
        addBatchBtn.addActionListener(e -> addStockBatch());

        buttonPanel.add(clearBatchBtn);
        buttonPanel.add(addBatchBtn);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4; gbc.weightx = 1.0;
        formPanel.add(buttonPanel, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, formPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.7);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ========== DATA LOADING METHODS ==========
    private void loadAllData() {
        // Load categories
        SwingWorker<Void, Void> categoriesWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/categories");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> categories = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        categoriesList.clear();
                        categoriesList.addAll(categories);

                        // Update categories map for combobox
                        categoriesMap.clear();
                        for (Map<String, Object> cat : categories) {
                            Integer id = safeInteger(cat.get("id"));
                            String name = Objects.toString(cat.get("name"), "");
                            if (id != null && !name.isEmpty()) {
                                categoriesMap.put(id, name);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                updateCategoriesTable();
                updateCategoryCombo();
            }
        };
        categoriesWorker.execute();

        // Load brands
        SwingWorker<Void, Void> brandsWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/brands");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> brands = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        brandsList.clear();
                        brandsList.addAll(brands);

                        // Update brands map for combobox
                        brandsMap.clear();
                        for (Map<String, Object> brand : brands) {
                            Integer id = safeInteger(brand.get("id"));
                            String name = Objects.toString(brand.get("name"), "");
                            if (id != null && !name.isEmpty()) {
                                brandsMap.put(id, name);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                updateBrandsTable();
                updateBrandCombo();
            }
        };
        brandsWorker.execute();

        // Load products
        SwingWorker<Void, Void> productsWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/products");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> products = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        productsList.clear();
                        productsList.addAll(products);

                        // Update products map for combobox
                        productsMap.clear();
                        for (Map<String, Object> prod : products) {
                            Integer id = safeInteger(prod.get("id"));
                            String name = Objects.toString(prod.get("name"), "");
                            if (id != null && !name.isEmpty()) {
                                productsMap.put(id, name);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                updateProductsTable();
                updateProductCombo();
            }
        };
        productsWorker.execute();

        // Load stock batches
        SwingWorker<Void, Void> batchesWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/stock-batches");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> batches = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        stockBatchesList.clear();
                        stockBatchesList.addAll(batches);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                updateStockBatchesTable();
            }
        };
        batchesWorker.execute();
    }

    // ========== TABLE UPDATES ==========
    private void updateCategoriesTable() {
        SwingUtilities.invokeLater(() -> {
            categoriesModel.setRowCount(0);
            for (Map<String, Object> cat : categoriesList) {
                Integer id = safeInteger(cat.get("id"));
                String name = Objects.toString(cat.get("name"), "");
                String description = Objects.toString(cat.get("description"), "");
                String createdAtRaw = Objects.toString(cat.get("createdAt"), "");
                String createdAt = formatDate(createdAtRaw);

                categoriesModel.addRow(new Object[]{
                        id, name, description, createdAt
                });
            }
        });
    }

    private void updateBrandsTable() {
        SwingUtilities.invokeLater(() -> {
            brandsModel.setRowCount(0);
            for (Map<String, Object> brand : brandsList) {
                Integer id = safeInteger(brand.get("id"));
                String name = Objects.toString(brand.get("name"), "");
                String country = Objects.toString(brand.get("country"), "");
                String createdAtRaw = Objects.toString(brand.get("createdAt"), "");
                String createdAt = formatDate(createdAtRaw);

                brandsModel.addRow(new Object[]{
                        id, name, country, createdAt
                });
            }
        });
    }

    private void updateProductsTable() {
        SwingUtilities.invokeLater(() -> {
            productsModel.setRowCount(0);
            for (Map<String, Object> prod : productsList) {
                Integer id = safeInteger(prod.get("id"));
                String sku = Objects.toString(prod.get("sku"), "");
                String name = Objects.toString(prod.get("name"), "");

                Integer categoryId = safeInteger(prod.get("categoryId"));
                String categoryName = categoriesMap.getOrDefault(categoryId, "N/A");

                Integer brandId = safeInteger(prod.get("brandId"));
                String brandName = brandsMap.getOrDefault(brandId, "N/A");

                Double minPrice = safeDouble(prod.get("minimumSellingPrice"));
                Integer reorderLevel = safeInteger(prod.get("reorderLevel"), 0);
                Boolean isActive = safeBoolean(prod.get("isActive"));

                // Calculate stock (sum from stock batches)
                int totalStock = 0;
                for (Map<String, Object> batch : stockBatchesList) {
                    if (safeInteger(batch.get("productId")).equals(id)) {
                        totalStock += safeInteger(batch.get("quantityRemaining"), 0);
                    }
                }

                productsModel.addRow(new Object[]{
                        id, sku, name, categoryName, brandName,
                        String.format("₦ %,.2f", minPrice != null ? minPrice : 0.0),
                        reorderLevel,
                        isActive != null && isActive ? "Active" : "Inactive",
                        totalStock
                });
            }
        });
    }

    private void updateStockBatchesTable() {
        SwingUtilities.invokeLater(() -> {
            stockBatchesModel.setRowCount(0);
            for (Map<String, Object> batch : stockBatchesList) {
                Integer id = safeInteger(batch.get("id"));

                Integer productId = safeInteger(batch.get("productId"));
                String productName = productsMap.getOrDefault(productId, "Product #" + productId);

                String batchNo = Objects.toString(batch.get("batchNumber"), "");
                Double buyingPrice = safeDouble(batch.get("buyingPrice"));
                Double sellingPrice = safeDouble(batch.get("sellingPrice"));
                Integer qtyReceived = safeInteger(batch.get("quantityReceived"), 0);
                Integer qtyRemaining = safeInteger(batch.get("quantityRemaining"), 0);
                String receivedDateRaw = Objects.toString(batch.get("receivedDate"), "");
                String receivedDate = formatDate(receivedDateRaw);
                String createdBy = Objects.toString(batch.get("createdByName"), "Admin");

                stockBatchesModel.addRow(new Object[]{
                        id, productName, batchNo,
                        String.format("₦ %,.2f", buyingPrice != null ? buyingPrice : 0.0),
                        String.format("₦ %,.2f", sellingPrice != null ? sellingPrice : 0.0),
                        qtyReceived, qtyRemaining, receivedDate, createdBy
                });
            }
        });
    }

    // ========== COMBOBOX UPDATES ==========
    private void updateCategoryCombo() {
        SwingUtilities.invokeLater(() -> {
            productCategoryCombo.removeAllItems();
            for (String categoryName : categoriesMap.values()) {
                productCategoryCombo.addItem(categoryName);
            }
        });
    }

    private void updateBrandCombo() {
        SwingUtilities.invokeLater(() -> {
            productBrandCombo.removeAllItems();
            for (String brandName : brandsMap.values()) {
                productBrandCombo.addItem(brandName);
            }
        });
    }

    private void updateProductCombo() {
        SwingUtilities.invokeLater(() -> {
            batchProductCombo.removeAllItems();
            for (String productName : productsMap.values()) {
                batchProductCombo.addItem(productName);
            }
        });
    }

    // ========== FORM CLEAR METHODS ==========
    private void clearCategoryForm() {
        categoryNameField.setText("");
        categoryDescArea.setText("");
    }

    private void clearBrandForm() {
        brandNameField.setText("");
        brandCountryField.setText("");
    }

    private void clearProductForm() {
        productSkuField.setText(UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        productNameField.setText("");
        productDescArea.setText("");
        productMinPriceField.setText("");
        productReorderField.setText("");
        productActiveCheck.setSelected(true);
        if (productCategoryCombo.getItemCount() > 0) productCategoryCombo.setSelectedIndex(0);
        if (productBrandCombo.getItemCount() > 0) productBrandCombo.setSelectedIndex(0);
    }

    private void clearBatchForm() {
        if (batchProductCombo.getItemCount() > 0) batchProductCombo.setSelectedIndex(0);
        batchNumberField.setText("");
        batchBuyingPriceField.setText("");
        batchSellingPriceField.setText("");
        batchQuantityField.setText("");
        batchExpiryField.setText("");
    }

    // ========== ADD/CREATE METHODS ==========
    private void addCategory() {
        String name = categoryNameField.getText().trim();
        String description = categoryDescArea.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Category name is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("name", name);
        categoryData.put("description", description);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.post("/api/secure/categories", categoryData);
                    Map<String, Object> response = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                    success = true;
                    message = "Category added successfully!";
                } catch (Exception e) {
                    success = false;
                    message = "Error: " + e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearCategoryForm();
                    loadAllData();
                } else {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void addBrand() {
        String name = brandNameField.getText().trim();
        String country = brandCountryField.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Brand name is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Object> brandData = new HashMap<>();
        brandData.put("name", name);
        brandData.put("country", country);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.post("/api/secure/brands", brandData);
                    Map<String, Object> response = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                    success = true;
                    message = "Brand added successfully!";
                } catch (Exception e) {
                    success = false;
                    message = "Error: " + e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearBrandForm();
                    loadAllData();
                } else {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void addProduct() {
        // Validate fields
        String sku = productSkuField.getText().trim();
        String name = productNameField.getText().trim();
        String description = productDescArea.getText().trim();
        String minPriceStr = productMinPriceField.getText().trim();
        String reorderStr = productReorderField.getText().trim();

        if (sku.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "SKU and Product Name are required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get selected category and brand IDs
        String selectedCategory = (String) productCategoryCombo.getSelectedItem();
        String selectedBrand = (String) productBrandCombo.getSelectedItem();

        Integer categoryId = getKeyByValue(categoriesMap, selectedCategory);
        Integer brandId = getKeyByValue(brandsMap, selectedBrand);

        if (categoryId == null || brandId == null) {
            JOptionPane.showMessageDialog(this, "Please select valid Category and Brand", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Parse numbers
        Double minPrice = null;
        Integer reorderLevel = null;

        try {
            minPrice = Double.parseDouble(minPriceStr);
            if (minPrice <= 0) {
                JOptionPane.showMessageDialog(this, "Minimum price must be greater than 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid minimum price format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            reorderLevel = Integer.parseInt(reorderStr);
            if (reorderLevel < 0) {
                JOptionPane.showMessageDialog(this, "Reorder level cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid reorder level format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Object> productData = new HashMap<>();
        productData.put("categoryId", categoryId);
        productData.put("brandId", brandId);
        productData.put("sku", sku);
        productData.put("name", name);
        productData.put("description", description);
        productData.put("minimumSellingPrice", minPrice);
        productData.put("reorderLevel", reorderLevel);
        productData.put("isActive", productActiveCheck.isSelected());

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.post("/api/secure/products", productData);
                    Map<String, Object> response = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                    success = true;
                    message = "Product added successfully!";
                } catch (Exception e) {
                    success = false;
                    message = "Error: " + e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearProductForm();
                    loadAllData();
                } else {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void addStockBatch() {
        // Validate fields
        String batchNo = batchNumberField.getText().trim();
        String buyingPriceStr = batchBuyingPriceField.getText().trim();
        String sellingPriceStr = batchSellingPriceField.getText().trim();
        String quantityStr = batchQuantityField.getText().trim();
        String expiry = batchExpiryField.getText().trim();

        // Get selected product ID
        String selectedProduct = (String) batchProductCombo.getSelectedItem();
        Integer productId = getKeyByValue(productsMap, selectedProduct);

        if (productId == null) {
            JOptionPane.showMessageDialog(this, "Please select a product", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (batchNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Batch number is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Double buyingPrice, sellingPrice;
        Integer quantity;

        try {
            buyingPrice = Double.parseDouble(buyingPriceStr);
            if (buyingPrice <= 0) {
                JOptionPane.showMessageDialog(this, "Buying price must be greater than 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid buying price format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            sellingPrice = Double.parseDouble(sellingPriceStr);
            if (sellingPrice <= 0) {
                JOptionPane.showMessageDialog(this, "Selling price must be greater than 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid selling price format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity must be greater than 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Object> batchData = new HashMap<>();
        batchData.put("productId", productId);
        batchData.put("batchNumber", batchNo);
        batchData.put("buyingPrice", buyingPrice);
        batchData.put("sellingPrice", sellingPrice);
        batchData.put("quantityReceived", quantity);
        batchData.put("quantityRemaining", quantity); // Initially same as received

        // Use DB-friendly timestamp format: "yyyy-MM-dd HH:mm:ss.SSS"
        DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        batchData.put("receivedDate", LocalDateTime.now().format(dbFmt));

        if (!expiry.isEmpty()) {
            batchData.put("expiryDate", expiry);
        }

        batchData.put("createdBy", session.getUserId());

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.post("/api/secure/stock-batches", batchData);
                    Map<String, Object> response = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                    success = true;
                    message = "Stock batch added successfully!";
                } catch (Exception e) {
                    success = false;
                    message = "Error: " + e.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Success", JOptionPane.INFORMATION_MESSAGE);
                    clearBatchForm();
                    loadAllData();
                } else {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ========== HELPER METHODS ==========
    private void showInventoryReport() {
        StringBuilder report = new StringBuilder();
        report.append("INVENTORY REPORT - OL KALOU WAITHAKA AUTO SPARES\n");
        report.append("=================================================\n\n");

        report.append("SUMMARY\n");
        report.append("-------\n");
        report.append(String.format("Total Categories: %d\n", categoriesList.size()));
        report.append(String.format("Total Brands: %d\n", brandsList.size()));
        report.append(String.format("Total Products: %d\n", productsList.size()));

        double totalStockValue = 0.0;
        int lowStockProducts = 0;

        for (Map<String, Object> prod : productsList) {
            Integer prodId = safeInteger(prod.get("id"));
            Integer reorderLevel = safeInteger(prod.get("reorderLevel"), 0);

            int stock = 0;
            double stockValue = 0;

            for (Map<String, Object> batch : stockBatchesList) {
                if (safeInteger(batch.get("productId")).equals(prodId)) {
                    int qty = safeInteger(batch.get("quantityRemaining"), 0);
                    Double buyingPrice = safeDouble(batch.get("buyingPrice"));
                    stock += qty;
                    stockValue += qty * (buyingPrice != null ? buyingPrice : 0);
                }
            }

            totalStockValue += stockValue;

            if (stock <= reorderLevel && stock > 0) {
                lowStockProducts++;
            }
        }

        report.append(String.format("Total Stock Value: ₦ %,.2f\n", totalStockValue));
        report.append(String.format("Products Low on Stock: %d\n\n", lowStockProducts));

        // Low stock warning
        report.append("LOW STOCK WARNINGS\n");
        report.append("------------------\n");
        for (Map<String, Object> prod : productsList) {
            Integer prodId = safeInteger(prod.get("id"));
            String prodName = Objects.toString(prod.get("name"), "");
            Integer reorderLevel = safeInteger(prod.get("reorderLevel"), 0);

            int stock = 0;
            for (Map<String, Object> batch : stockBatchesList) {
                if (safeInteger(batch.get("productId")).equals(prodId)) {
                    stock += safeInteger(batch.get("quantityRemaining"), 0);
                }
            }

            if (stock <= reorderLevel) {
                report.append(String.format("⚠ %s: %d in stock (Reorder at: %d)\n",
                        prodName, stock, reorderLevel));
            }
        }

        report.append("\nReport Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

        // Display report
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "Inventory Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private Integer getKeyByValue(Map<Integer, String> map, String value) {
        if (value == null) return null;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Integer safeInteger(Object o) {
        return safeInteger(o, null);
    }

    private Integer safeInteger(Object o, Integer defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Double safeDouble(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean safeBoolean(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean) return (Boolean) o;
        if (o instanceof Number) return ((Number) o).intValue() != 0;
        try {
            return Boolean.parseBoolean(o.toString());
        } catch (Exception e) {
            return null;
        }
    }

    // ========== TABLE CELL RENDERER ==========
    class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            JLabel label = (JLabel) c;
            label.setHorizontalAlignment(SwingConstants.CENTER);

            if ("Active".equals(value)) {
                label.setForeground(new Color(76, 175, 80));
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            } else if ("Inactive".equals(value)) {
                label.setForeground(new Color(244, 67, 54));
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }

            return label;
        }
    }

    // utility method for DB timestamps (expects: yyyy-MM-dd HH:mm:ss.SSS)
    private String formatDate(Object value) {
        if (value == null) return "";
        try {
            DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            DateTimeFormatter uiFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(value.toString(), dbFmt);
            return ldt.format(uiFmt);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
