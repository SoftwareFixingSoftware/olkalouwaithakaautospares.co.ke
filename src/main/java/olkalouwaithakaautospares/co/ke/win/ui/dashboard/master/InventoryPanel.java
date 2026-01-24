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
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * InventoryPanel - Complete inventory management system with Edit Product and Edit Stock Batch functionality
 *
 * Tabbed interface for managing:
 * 1. Categories
 * 2. Brands (Vehicle Brands)
 * 3. Stock Conditions
 * 4. Products (with CRUD operations)
 * 5. Stock Batches (with CRUD operations)
 */
@SuppressWarnings("unchecked")
public class InventoryPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    // Data lists
    private final List<Map<String, Object>> categoriesList = new ArrayList<>();
    private final List<Map<String, Object>> brandsList = new ArrayList<>();
    private final List<Map<String, Object>> stockConditionsList = new ArrayList<>();
    private final List<Map<String, Object>> productsList = new ArrayList<>();
    private final List<Map<String, Object>> stockBatchesList = new ArrayList<>();

    // Category components
    private JTable categoriesTable;
    private DefaultTableModel categoriesModel;
    private JTextField categoryNameField;
    private JTextArea categoryDescArea;

    // Brand components (Vehicle Brands)
    private JTable brandsTable;
    private DefaultTableModel brandsModel;
    private JTextField brandNameField;
    private JTextField brandCountryField;
    private JButton editBrandBtn;
    private JButton deleteBrandBtn;
    private Integer selectedBrandId = null;

    // Stock Condition components
    private JTable stockConditionsTable;
    private DefaultTableModel stockConditionsModel;
    private JTextField conditionNameField;
    private JTextArea conditionDescArea;

    // Product components
    private JTable productsTable;
    private DefaultTableModel productsModel;
    private JComboBox<String> productCategoryCombo;
    private JPanel productBrandsCheckboxPanel;
    private JScrollPane brandsScrollPane;
    private JTextField productSkuField;
    private JTextField productNameField;
    private JTextArea productDescArea;
    private JTextField productMinPriceField;
    private JTextField productReorderField;
    private JCheckBox productActiveCheck;
    private Map<Integer, String> categoriesMap = new HashMap<>();
    private Map<Integer, String> brandsMap = new HashMap<>();
    private Map<Integer, JCheckBox> brandCheckboxes = new HashMap<>();

    // Product Edit functionality
    private JButton addProdBtn;
    private JButton updateProdBtn;
    private JButton clearProdBtn;
    private JButton editProductBtn;
    private Integer selectedProductId = null;
    private boolean isEditMode = false;

    // Stock Batch components - UPDATED with Edit functionality
    private JTable stockBatchesTable;
    private DefaultTableModel stockBatchesModel;
    private JComboBox<String> batchProductCombo;
    private JComboBox<String> batchStockConditionCombo;
    private JTextField batchNumberField;
    private JTextField batchBuyingPriceField;
    private JTextField batchSellingPriceField;
    private JTextField batchQuantityReceivedField;
    private JTextField batchQuantityRemainingField;
    private JTextField batchExpiryField;
    private Map<Integer, String> productsMap = new HashMap<>();
    private Map<Integer, String> stockConditionsMap = new HashMap<>();

    // NEW: Stock Batch Edit functionality
    private JButton addBatchBtn;
    private JButton updateBatchBtn;
    private JButton clearBatchBtn;
    private JButton editStockBatchBtn;
    private JButton cancelEditBatchBtn;
    private Integer selectedStockBatchId = null;
    private boolean isEditBatchMode = false;

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

        // Create tabs in logical order
        JPanel categoriesTab = createCategoriesTab();
        JPanel brandsTab = createBrandsTab();
        JPanel stockConditionsTab = createStockConditionsTab();
        JPanel productsTab = createProductsTab();
        JPanel stockBatchesTab = createStockBatchesTab();

        tabbedPane.addTab("Categories", categoriesTab);
        tabbedPane.addTab("Vehicle Brands", brandsTab);
        tabbedPane.addTab("Stock Conditions", stockConditionsTab);
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
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Vehicle Brands"));

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

        // Add selection listener
        brandsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = brandsTable.getSelectedRow();
                if (row >= 0) {
                    int modelRow = brandsTable.convertRowIndexToModel(row);
                    selectedBrandId = (Integer) brandsModel.getValueAt(modelRow, 0);
                    String brandName = (String) brandsModel.getValueAt(modelRow, 1);
                    String country = (String) brandsModel.getValueAt(modelRow, 2);

                    // Populate form with selected brand data
                    brandNameField.setText(brandName);
                    brandCountryField.setText(country != null ? country : "");

                    // Enable edit/delete buttons
                    editBrandBtn.setEnabled(true);
                    deleteBrandBtn.setEnabled(true);
                } else {
                    selectedBrandId = null;
                    editBrandBtn.setEnabled(false);
                    deleteBrandBtn.setEnabled(false);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(brandsTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Manage Vehicle Brands"));
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
        clearBrandBtn.addActionListener(e -> {
            clearBrandForm();
            brandsTable.clearSelection();
            selectedBrandId = null;
            editBrandBtn.setEnabled(false);
            deleteBrandBtn.setEnabled(false);
        });

        JButton addBrandBtn = new JButton("Add Brand");
        styleButton(addBrandBtn, new Color(76, 175, 80));
        addBrandBtn.addActionListener(e -> addBrand());

        editBrandBtn = new JButton("Update Brand");
        styleButton(editBrandBtn, new Color(255, 152, 0));
        editBrandBtn.setEnabled(false);
        editBrandBtn.addActionListener(e -> updateBrand());

        deleteBrandBtn = new JButton("Delete Brand");
        styleButton(deleteBrandBtn, new Color(244, 67, 54));
        deleteBrandBtn.setEnabled(false);
        deleteBrandBtn.addActionListener(e -> deleteBrand());

        buttonPanel.add(clearBrandBtn);
        buttonPanel.add(addBrandBtn);
        buttonPanel.add(editBrandBtn);
        buttonPanel.add(deleteBrandBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formPanel.add(buttonPanel, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, formPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.7);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ========== STOCK CONDITIONS TAB ==========
    private JPanel createStockConditionsTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Stock Conditions"));

        String[] columns = {"ID", "Name"};
        stockConditionsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        stockConditionsTable = new JTable(stockConditionsModel);
        stockConditionsTable.setAutoCreateRowSorter(true);
        stockConditionsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stockConditionsTable.setRowHeight(35);
        stockConditionsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        stockConditionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(stockConditionsTable);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Stock Condition"));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Condition Name:"), gbc);
        conditionNameField = new JTextField(20);
        conditionNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8;
        formPanel.add(conditionNameField, gbc);

        // Row 2: Description
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Description:"), gbc);
        conditionDescArea = new JTextArea(3, 20);
        conditionDescArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        conditionDescArea.setLineWrap(true);
        JScrollPane conditionDescScroll = new JScrollPane(conditionDescArea);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.8;
        formPanel.add(conditionDescScroll, gbc);

        // Row 3: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        JButton clearConditionBtn = new JButton("Clear");
        styleButton(clearConditionBtn, new Color(158, 158, 158));
        clearConditionBtn.addActionListener(e -> clearConditionForm());

        JButton addConditionBtn = new JButton("Add Condition");
        styleButton(addConditionBtn, new Color(76, 175, 80));
        addConditionBtn.addActionListener(e -> addStockCondition());

        buttonPanel.add(clearConditionBtn);
        buttonPanel.add(addConditionBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
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

        // Table panel with Action buttons
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Products"));

        // Add action buttons panel to table header
        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setBackground(Color.WHITE);

        JLabel tableTitle = new JLabel("All Products");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtonsPanel.setOpaque(false);

        JButton refreshProductsBtn = new JButton("Refresh");
        styleSmallButton(refreshProductsBtn, new Color(33, 150, 243));
        refreshProductsBtn.addActionListener(e -> loadProducts());

        editProductBtn = new JButton("Edit Selected");
        styleSmallButton(editProductBtn, new Color(255, 152, 0));
        editProductBtn.setEnabled(false);
        editProductBtn.addActionListener(e -> editSelectedProduct());

        actionButtonsPanel.add(refreshProductsBtn);
        actionButtonsPanel.add(editProductBtn);

        tableHeaderPanel.add(tableTitle, BorderLayout.WEST);
        tableHeaderPanel.add(actionButtonsPanel, BorderLayout.EAST);

        String[] columns = {"ID", "SKU", "Name", "Category", "Compatible Brands", "Min Price", "Reorder Level", "Active", "Stock"};
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
        productsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add selection listener for products table
        productsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = productsTable.getSelectedRow();
                if (row >= 0) {
                    editProductBtn.setEnabled(true);
                } else {
                    editProductBtn.setEnabled(false);
                }
            }
        });

        // Color active status
        productsTable.getColumnModel().getColumn(7).setCellRenderer(new StatusRenderer());

        JScrollPane scrollPane = new JScrollPane(productsTable);

        tablePanel.add(tableHeaderPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Product Management"));
        formPanel.setBackground(Color.WHITE);
        formPanel.setPreferredSize(new Dimension(1100, 650));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: SKU and Name
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.15;
        JLabel skuLabel = new JLabel("SKU:");
        skuLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(skuLabel, gbc);

        productSkuField = new JTextField(20);
        productSkuField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productSkuField.setEditable(true);
        productSkuField.setText(generateSKU());
        productSkuField.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.35;
        formPanel.add(productSkuField, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.15;
        JLabel nameLabel = new JLabel("Product Name:");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(nameLabel, gbc);

        productNameField = new JTextField(25);
        productNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productNameField.setPreferredSize(new Dimension(250, 30));
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.35;
        formPanel.add(productNameField, gbc);

        // Row 1: Category and Reorder Level
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.15;
        JLabel catLabel = new JLabel("Category:");
        catLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(catLabel, gbc);

        productCategoryCombo = new JComboBox<>();
        productCategoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productCategoryCombo.setPreferredSize(new Dimension(200, 30));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.35;
        formPanel.add(productCategoryCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.15;
        JLabel reorderLabel = new JLabel("Reorder Level:");
        reorderLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(reorderLabel, gbc);

        productReorderField = new JTextField(15);
        productReorderField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productReorderField.setText("10"); // Default value
        productReorderField.setPreferredSize(new Dimension(150, 30));
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.35;
        formPanel.add(productReorderField, gbc);

        // Row 2: Minimum Price and Active Status
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.15;
        JLabel minPriceLabel = new JLabel("Min Price (ksh):");
        minPriceLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(minPriceLabel, gbc);

        productMinPriceField = new JTextField(15);
        productMinPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productMinPriceField.setPreferredSize(new Dimension(150, 30));
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.35;
        formPanel.add(productMinPriceField, gbc);

        productActiveCheck = new JCheckBox("Active Product");
        productActiveCheck.setSelected(true);
        productActiveCheck.setFont(new Font("Segoe UI", Font.BOLD, 13));
        productActiveCheck.setBackground(Color.WHITE);
        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 0.5;
        formPanel.add(productActiveCheck, gbc);

        // Row 3: Description
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.15;
        JLabel descLabel = new JLabel("Description:");
        descLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        formPanel.add(descLabel, gbc);

        productDescArea = new JTextArea(4, 30);
        productDescArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        productDescArea.setLineWrap(true);
        productDescArea.setWrapStyleWord(true);
        JScrollPane prodDescScroll = new JScrollPane(productDescArea);
        prodDescScroll.setPreferredSize(new Dimension(400, 100));
        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; gbc.weightx = 0.85;
        gbc.fill = GridBagConstraints.BOTH;
        formPanel.add(prodDescScroll, gbc);

        // Row 4: Compatible Brands - LARGE SCROLLABLE AREA
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.15;
        JLabel brandsLabel = new JLabel("Compatible Brands:");
        brandsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        brandsLabel.setVerticalAlignment(SwingConstants.TOP);
        formPanel.add(brandsLabel, gbc);

        // Create a large panel for checkboxes with scroll
        productBrandsCheckboxPanel = new JPanel();
        productBrandsCheckboxPanel.setLayout(new GridLayout(0, 4, 15, 8)); // 4 columns with spacing
        productBrandsCheckboxPanel.setBackground(Color.WHITE);
        productBrandsCheckboxPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        brandsScrollPane = new JScrollPane(productBrandsCheckboxPanel);
        brandsScrollPane.setPreferredSize(new Dimension(600, 180));
        brandsScrollPane.setBorder(BorderFactory.createTitledBorder("Select Compatible Vehicle Brands"));
        brandsScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        brandsScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 3; gbc.weightx = 0.85;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.4;
        formPanel.add(brandsScrollPane, gbc);

        // Row 5: Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        clearProdBtn = new JButton("Clear Form");
        clearProdBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clearProdBtn.setBackground(new Color(158, 158, 158));
        clearProdBtn.setForeground(Color.WHITE);
        clearProdBtn.setBorderPainted(false);
        clearProdBtn.setFocusPainted(false);
        clearProdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearProdBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        clearProdBtn.addActionListener(e -> clearProductForm());

        addProdBtn = new JButton("Add Product");
        addProdBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addProdBtn.setBackground(new Color(76, 175, 80));
        addProdBtn.setForeground(Color.WHITE);
        addProdBtn.setBorderPainted(false);
        addProdBtn.setFocusPainted(false);
        addProdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addProdBtn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        addProdBtn.addActionListener(e -> addProduct());

        updateProdBtn = new JButton("Update Product");
        updateProdBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        updateProdBtn.setBackground(new Color(255, 152, 0));
        updateProdBtn.setForeground(Color.WHITE);
        updateProdBtn.setBorderPainted(false);
        updateProdBtn.setFocusPainted(false);
        updateProdBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        updateProdBtn.setBorder(BorderFactory.createEmptyBorder(12, 30, 12, 30));
        updateProdBtn.setVisible(false);
        updateProdBtn.addActionListener(e -> updateProduct());

        JButton cancelEditBtn = new JButton("Cancel Edit");
        cancelEditBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelEditBtn.setBackground(new Color(158, 158, 158));
        cancelEditBtn.setForeground(Color.WHITE);
        cancelEditBtn.setBorderPainted(false);
        cancelEditBtn.setFocusPainted(false);
        cancelEditBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancelEditBtn.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        cancelEditBtn.setVisible(false);
        cancelEditBtn.addActionListener(e -> cancelEdit());

        buttonPanel.add(clearProdBtn);
        buttonPanel.add(addProdBtn);
        buttonPanel.add(updateProdBtn);
        buttonPanel.add(cancelEditBtn);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 4; gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.SOUTH;
        formPanel.add(buttonPanel, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, formPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.4);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ========== STOCK BATCHES TAB - UPDATED WITH EDIT FUNCTIONALITY ==========
    private JPanel createStockBatchesTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Table panel with Action buttons
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("All Stock Batches"));

        // Add action buttons panel to table header
        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setBackground(Color.WHITE);

        JLabel tableTitle = new JLabel("All Stock Batches");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JPanel actionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actionButtonsPanel.setOpaque(false);

        JButton refreshBatchesBtn = new JButton("Refresh");
        styleSmallButton(refreshBatchesBtn, new Color(33, 150, 243));
        refreshBatchesBtn.addActionListener(e -> loadStockBatches());

        editStockBatchBtn = new JButton("Edit Selected");
        styleSmallButton(editStockBatchBtn, new Color(255, 152, 0));
        editStockBatchBtn.setEnabled(false);
        editStockBatchBtn.addActionListener(e -> editSelectedStockBatch());

        actionButtonsPanel.add(refreshBatchesBtn);
        actionButtonsPanel.add(editStockBatchBtn);

        tableHeaderPanel.add(tableTitle, BorderLayout.WEST);
        tableHeaderPanel.add(actionButtonsPanel, BorderLayout.EAST);

        String[] columns = {"ID", "Product", "Condition", "Batch No", "Buy Price", "Sell Price", "Qty Received", "Qty Left", "Received Date", "Added By"};
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
        stockBatchesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add selection listener for stock batches table
        stockBatchesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = stockBatchesTable.getSelectedRow();
                if (row >= 0) {
                    editStockBatchBtn.setEnabled(true);
                } else {
                    editStockBatchBtn.setEnabled(false);
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(stockBatchesTable);

        tablePanel.add(tableHeaderPanel, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Form panel - UPDATED with Edit mode
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Add New Stock Batch"));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 1: Product and Stock Condition
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Product:"), gbc);
        batchProductCombo = new JComboBox<>();
        batchProductCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(batchProductCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Stock Condition:"), gbc);
        batchStockConditionCombo = new JComboBox<>();
        batchStockConditionCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(batchStockConditionCombo, gbc);

        // Row 2: Batch Number and Prices
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Batch Number:"), gbc);
        batchNumberField = new JTextField(15);
        batchNumberField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(batchNumberField, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Buying Price (ksh):"), gbc);
        batchBuyingPriceField = new JTextField(15);
        batchBuyingPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 0.3;
        formPanel.add(batchBuyingPriceField, gbc);

        // Row 3: Selling Price and Quantity Received
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Selling Price (ksh):"), gbc);
        batchSellingPriceField = new JTextField(15);
        batchSellingPriceField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(batchSellingPriceField, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Quantity Received:"), gbc);
        batchQuantityReceivedField = new JTextField(15);
        batchQuantityReceivedField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        batchQuantityReceivedField.setEditable(true); // Can edit when adding
        gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 0.3;
        formPanel.add(batchQuantityReceivedField, gbc);

        // Row 4: Quantity Remaining and Expiry Date
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Quantity Remaining:"), gbc);
        batchQuantityRemainingField = new JTextField(15);
        batchQuantityRemainingField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        batchQuantityRemainingField.setEditable(true); // Can edit when updating
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.3;
        formPanel.add(batchQuantityRemainingField, gbc);

        gbc.gridx = 2; gbc.gridy = 3; gbc.weightx = 0.2;
        formPanel.add(new JLabel("Expiry Date (optional):"), gbc);
        batchExpiryField = new JTextField(15);
        batchExpiryField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        batchExpiryField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        gbc.gridx = 3; gbc.gridy = 3; gbc.weightx = 0.3;
        formPanel.add(batchExpiryField, gbc);

        // Row 5: Buttons - UPDATED with Add/Update toggle
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        clearBatchBtn = new JButton("Clear Form");
        styleButton(clearBatchBtn, new Color(158, 158, 158));
        clearBatchBtn.addActionListener(e -> clearBatchForm());

        addBatchBtn = new JButton("Add Stock Batch");
        styleButton(addBatchBtn, new Color(76, 175, 80));
        addBatchBtn.addActionListener(e -> addStockBatch());

        updateBatchBtn = new JButton("Update Stock Batch");
        styleButton(updateBatchBtn, new Color(255, 152, 0));
        updateBatchBtn.setVisible(false);
        updateBatchBtn.addActionListener(e -> updateStockBatch());

        cancelEditBatchBtn = new JButton("Cancel Edit");
        styleButton(cancelEditBatchBtn, new Color(158, 158, 158));
        cancelEditBatchBtn.setVisible(false);
        cancelEditBatchBtn.addActionListener(e -> cancelBatchEdit());

        buttonPanel.add(clearBatchBtn);
        buttonPanel.add(addBatchBtn);
        buttonPanel.add(updateBatchBtn);
        buttonPanel.add(cancelEditBatchBtn);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4; gbc.weightx = 1.0;
        formPanel.add(buttonPanel, gbc);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, formPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.7);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    // ========== DATA LOADING METHODS ==========
    private void loadAllData() {
        // Load in logical order
        loadCategories();
        loadBrands();
        loadStockConditions();
        loadProducts();
        loadStockBatches();
    }

    private void loadCategories() {
        SwingWorker<Void, Void> categoriesWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/categories");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> categories = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        categoriesList.clear();
                        categoriesList.addAll(categories);

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
                updateProductCategoryCombo();
            }
        };
        categoriesWorker.execute();
    }

    private void loadBrands() {
        SwingWorker<Void, Void> brandsWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/brands");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> brands = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        brandsList.clear();
                        brandsList.addAll(brands);

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
                updateBrandCheckboxes();
            }
        };
        brandsWorker.execute();
    }

    private void loadStockConditions() {
        SwingWorker<Void, Void> conditionsWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/stock-conditions");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> conditions = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        stockConditionsList.clear();
                        stockConditionsList.addAll(conditions);

                        stockConditionsMap.clear();
                        for (Map<String, Object> condition : conditions) {
                            Integer id = safeInteger(condition.get("id"));
                            String name = Objects.toString(condition.get("name"), "");
                            if (id != null && !name.isEmpty()) {
                                stockConditionsMap.put(id, name);
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
                updateStockConditionsTable();
                updateBatchStockConditionCombo();
            }
        };
        conditionsWorker.execute();
    }

    private void loadProducts() {
        SwingWorker<Void, Void> productsWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/products");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> products = mapper.readValue(resp, new TypeReference<List<Map<String, Object>>>() {});
                        productsList.clear();
                        productsList.addAll(products);

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
                updateBatchProductCombo();
            }
        };
        productsWorker.execute();
    }

    private void loadStockBatches() {
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

    private void updateStockConditionsTable() {
        SwingUtilities.invokeLater(() -> {
            stockConditionsModel.setRowCount(0);
            for (Map<String, Object> condition : stockConditionsList) {
                Integer id = safeInteger(condition.get("id"));
                String name = Objects.toString(condition.get("name"), "");
                String description = Objects.toString(condition.get("description"), "");
                String createdAtRaw = Objects.toString(condition.get("createdAt"), "");
                String createdAt = formatDate(createdAtRaw);

                stockConditionsModel.addRow(new Object[]{
                        id, name, description, createdAt
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

                // Get compatible brands
                List<Integer> compatibleBrandIds = new ArrayList<>();
                try {
                    String compatibleBrandsJson = Objects.toString(prod.get("compatibleBrandIds"), "[]");
                    if (!compatibleBrandsJson.isEmpty()) {
                        List<Integer> brands = mapper.readValue(compatibleBrandsJson, new TypeReference<List<Integer>>() {});
                        compatibleBrandIds.addAll(brands);
                    }
                } catch (Exception e) {
                    // Handle JSON parsing error
                }

                // Build compatible brands string
                StringBuilder compatibleBrandsStr = new StringBuilder();
                for (Integer brandId : compatibleBrandIds) {
                    String brandName = brandsMap.get(brandId);
                    if (brandName != null) {
                        if (compatibleBrandsStr.length() > 0) {
                            compatibleBrandsStr.append(", ");
                        }
                        compatibleBrandsStr.append(brandName);
                    }
                }
                if (compatibleBrandsStr.length() == 0) {
                    compatibleBrandsStr.append("None");
                }

                Double minPrice = safeDouble(prod.get("minimumSellingPrice"));
                Integer reorderLevel = safeInteger(prod.get("reorderLevel"), 0);
                Boolean isActive = safeBoolean(prod.get("isActive"));

                // Calculate stock
                int totalStock = calculateProductStock(id);

                productsModel.addRow(new Object[]{
                        id, sku, name, categoryName, compatibleBrandsStr.toString(),
                        String.format("ksh %,.2f", minPrice != null ? minPrice : 0.0),
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

                Integer conditionId = safeInteger(batch.get("conditionId"));
                String conditionName = stockConditionsMap.getOrDefault(conditionId, "N/A");

                String batchNo = Objects.toString(batch.get("batchNumber"), "");
                Double buyingPrice = safeDouble(batch.get("buyingPrice"));
                Double sellingPrice = safeDouble(batch.get("sellingPrice"));
                Integer qtyReceived = safeInteger(batch.get("quantityReceived"), 0);
                Integer qtyRemaining = safeInteger(batch.get("quantityRemaining"), 0);
                String receivedDateRaw = Objects.toString(batch.get("receivedDate"), "");
                String receivedDate = formatDate(receivedDateRaw);
                String createdBy = Objects.toString(batch.get("createdByName"), "Admin");

                stockBatchesModel.addRow(new Object[]{
                        id, productName, conditionName, batchNo,
                        String.format("ksh %,.2f", buyingPrice != null ? buyingPrice : 0.0),
                        String.format("ksh %,.2f", sellingPrice != null ? sellingPrice : 0.0),
                        qtyReceived, qtyRemaining, receivedDate, createdBy
                });
            }
        });
    }

    // ========== CHECKBOX UPDATES ==========
    private void updateBrandCheckboxes() {
        SwingUtilities.invokeLater(() -> {
            productBrandsCheckboxPanel.removeAll();
            brandCheckboxes.clear();

            // Create checkboxes in 4 columns for better visibility
            for (Map.Entry<Integer, String> entry : brandsMap.entrySet()) {
                Integer brandId = entry.getKey();
                String brandName = entry.getValue();

                JCheckBox checkBox = new JCheckBox(brandName);
                checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                checkBox.setBackground(Color.WHITE);
                checkBox.setPreferredSize(new Dimension(180, 25));

                brandCheckboxes.put(brandId, checkBox);
                productBrandsCheckboxPanel.add(checkBox);
            }

            productBrandsCheckboxPanel.revalidate();
            productBrandsCheckboxPanel.repaint();
            brandsScrollPane.revalidate();
            brandsScrollPane.repaint();
        });
    }

    // ========== COMBOBOX UPDATES ==========
    private void updateProductCategoryCombo() {
        SwingUtilities.invokeLater(() -> {
            productCategoryCombo.removeAllItems();
            productCategoryCombo.addItem("-- Select Category --");
            for (String categoryName : categoriesMap.values()) {
                productCategoryCombo.addItem(categoryName);
            }
        });
    }

    private void updateBatchProductCombo() {
        SwingUtilities.invokeLater(() -> {
            batchProductCombo.removeAllItems();
            batchProductCombo.addItem("-- Select Product --");
            for (String productName : productsMap.values()) {
                batchProductCombo.addItem(productName);
            }
        });
    }

    private void updateBatchStockConditionCombo() {
        SwingUtilities.invokeLater(() -> {
            batchStockConditionCombo.removeAllItems();
            batchStockConditionCombo.addItem("-- Select Condition --");
            for (String conditionName : stockConditionsMap.values()) {
                batchStockConditionCombo.addItem(conditionName);
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
        brandsTable.clearSelection();
        selectedBrandId = null;
        editBrandBtn.setEnabled(false);
        deleteBrandBtn.setEnabled(false);
    }

    private void clearConditionForm() {
        conditionNameField.setText("");
        conditionDescArea.setText("");
    }

    private void clearProductForm() {
        selectedProductId = null;
        isEditMode = false;

        productSkuField.setText(generateSKU());
        productSkuField.setEditable(true);
        productNameField.setText("");
        productDescArea.setText("");
        productMinPriceField.setText("");
        productReorderField.setText("10"); // Reset to default
        productActiveCheck.setSelected(true);
        if (productCategoryCombo.getItemCount() > 0) productCategoryCombo.setSelectedIndex(0);

        // Clear all checkboxes
        for (JCheckBox checkBox : brandCheckboxes.values()) {
            checkBox.setSelected(false);
        }

        // Switch back to Add mode
        addProdBtn.setVisible(true);
        updateProdBtn.setVisible(false);

        // Update form title
        JPanel formPanel = (JPanel) getComponent(0); // Get the main panel
        JTabbedPane tabbedPane = (JTabbedPane) formPanel.getComponent(0);
        Component productsTab = tabbedPane.getComponentAt(3); // Products tab is at index 3
        if (productsTab instanceof JPanel) {
            JPanel productsPanel = (JPanel) productsTab;
            JSplitPane splitPane = (JSplitPane) productsPanel.getComponent(0);
            JPanel bottomPanel = (JPanel) splitPane.getRightComponent();
            bottomPanel.setBorder(BorderFactory.createTitledBorder("Product Management"));
        }
    }

    private void clearBatchForm() {
        selectedStockBatchId = null;
        isEditBatchMode = false;

        if (batchProductCombo.getItemCount() > 0) batchProductCombo.setSelectedIndex(0);
        if (batchStockConditionCombo.getItemCount() > 0) batchStockConditionCombo.setSelectedIndex(0);
        batchNumberField.setText("");
        batchBuyingPriceField.setText("");
        batchSellingPriceField.setText("");
        batchQuantityReceivedField.setText("");
        batchQuantityRemainingField.setText("");
        batchExpiryField.setText("");

        // Enable quantity fields
        batchQuantityReceivedField.setEditable(true);
        batchQuantityRemainingField.setEditable(true);

        // Switch back to Add mode
        addBatchBtn.setVisible(true);
        updateBatchBtn.setVisible(false);
        cancelEditBatchBtn.setVisible(false);

        // Update form title
        JPanel formPanel = (JPanel) getComponent(0); // Get the main panel
        JTabbedPane tabbedPane = (JTabbedPane) formPanel.getComponent(0);
        Component batchesTab = tabbedPane.getComponentAt(4); // Batches tab is at index 4
        if (batchesTab instanceof JPanel) {
            JPanel batchesPanel = (JPanel) batchesTab;
            JSplitPane splitPane = (JSplitPane) batchesPanel.getComponent(0);
            JPanel bottomPanel = (JPanel) splitPane.getRightComponent();
            bottomPanel.setBorder(BorderFactory.createTitledBorder("Add New Stock Batch"));
        }
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
                    success = true;
                    message = "Vehicle brand added successfully!";
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

    private void updateBrand() {
        if (selectedBrandId == null) {
            JOptionPane.showMessageDialog(this, "Please select a brand to update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

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
                    String resp = client.put("/api/secure/brands/" + selectedBrandId, brandData);
                    success = true;
                    message = "Vehicle brand updated successfully!";
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

    private void deleteBrand() {
        if (selectedBrandId == null) {
            JOptionPane.showMessageDialog(this, "Please select a brand to delete", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete vehicle brand? This will affect product compatibility.",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.delete("/api/secure/brands/" + selectedBrandId);
                    success = true;
                    message = "Vehicle brand deleted successfully!";
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

    private void addStockCondition() {
        String name = conditionNameField.getText().trim();
        String description = conditionDescArea.getText().trim();

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Condition name is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Object> conditionData = new HashMap<>();
        conditionData.put("name", name);
        conditionData.put("description", description);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.post("/api/secure/stock-conditions", conditionData);
                    success = true;
                    message = "Stock condition added successfully!";
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
                    clearConditionForm();
                    loadAllData();
                } else {
                    JOptionPane.showMessageDialog(InventoryPanel.this, message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ========== PRODUCT CRUD METHODS ==========
    private void addProduct() {
        if (isEditMode) {
            JOptionPane.showMessageDialog(this, "You are in edit mode. Please update or cancel edit first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String sku = productSkuField.getText().trim();
        String name = productNameField.getText().trim();
        String description = productDescArea.getText().trim();
        String minPriceStr = productMinPriceField.getText().trim();
        String reorderStr = productReorderField.getText().trim();

        if (sku.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "SKU and Product Name are required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedCategory = (String) productCategoryCombo.getSelectedItem();
        if ("-- Select Category --".equals(selectedCategory) || selectedCategory == null) {
            JOptionPane.showMessageDialog(this, "Please select a Category", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer categoryId = getKeyByValue(categoriesMap, selectedCategory);

        if (categoryId == null) {
            JOptionPane.showMessageDialog(this, "Please select a valid Category", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get selected compatible brands from checkboxes
        List<Integer> compatibleBrandIds = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> entry : brandCheckboxes.entrySet()) {
            Integer brandId = entry.getKey();
            JCheckBox checkBox = entry.getValue();
            if (checkBox.isSelected()) {
                compatibleBrandIds.add(brandId);
            }
        }

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
        productData.put("sku", sku);
        productData.put("name", name);
        productData.put("description", description);
        productData.put("minimumSellingPrice", minPrice);
        productData.put("reorderLevel", reorderLevel);
        productData.put("isActive", productActiveCheck.isSelected());
        productData.put("compatibleBrandIds", compatibleBrandIds);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    System.out.println("Sending product data: " + productData);
                    String resp = client.post("/api/secure/products", productData);
                    System.out.println("Response: " + resp);
                    success = true;
                    message = "Product added successfully!";
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

    // Product Edit functionality
    private void editSelectedProduct() {
        int row = productsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a product to edit", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int modelRow = productsTable.convertRowIndexToModel(row);
        selectedProductId = (Integer) productsModel.getValueAt(modelRow, 0);

        // Load product details
        loadProductDetails(selectedProductId);
    }

    private void loadProductDetails(Integer productId) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Map<String, Object> productData = null;
            private String errorMessage = null;

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/products/" + productId);
                    if (resp != null && !resp.trim().isEmpty()) {
                        productData = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                    }
                } catch (Exception e) {
                    errorMessage = "Error loading product: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(InventoryPanel.this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (productData != null) {
                    populateProductForm(productData);
                    setEditMode(true);
                }
            }
        };
        worker.execute();
    }

    private void populateProductForm(Map<String, Object> productData) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Populate basic fields
                productSkuField.setText(Objects.toString(productData.get("sku"), ""));
                productSkuField.setEditable(true);
                productNameField.setText(Objects.toString(productData.get("name"), ""));
                productDescArea.setText(Objects.toString(productData.get("description"), ""));

                // Set category
                Integer categoryId = safeInteger(productData.get("categoryId"));
                if (categoryId != null) {
                    String categoryName = categoriesMap.get(categoryId);
                    if (categoryName != null) {
                        productCategoryCombo.setSelectedItem(categoryName);
                    }
                }

                // Set prices
                Double minPrice = safeDouble(productData.get("minimumSellingPrice"));
                if (minPrice != null) {
                    productMinPriceField.setText(String.format("%.2f", minPrice));
                }

                Integer reorderLevel = safeInteger(productData.get("reorderLevel"));
                if (reorderLevel != null) {
                    productReorderField.setText(reorderLevel.toString());
                }

                // Set active status
                Boolean isActive = safeBoolean(productData.get("isActive"));
                productActiveCheck.setSelected(isActive != null ? isActive : true);

                // Set compatible brands
                List<Integer> compatibleBrandIds = new ArrayList<>();
                try {
                    String compatibleBrandsJson = Objects.toString(productData.get("compatibleBrandIds"), "[]");
                    if (!compatibleBrandsJson.isEmpty()) {
                        compatibleBrandIds = mapper.readValue(compatibleBrandsJson, new TypeReference<List<Integer>>() {});
                    }
                } catch (Exception e) {
                    // Handle JSON parsing error
                }

                // Clear all checkboxes first
                for (JCheckBox checkBox : brandCheckboxes.values()) {
                    checkBox.setSelected(false);
                }

                // Select compatible brands
                for (Integer brandId : compatibleBrandIds) {
                    JCheckBox checkBox = brandCheckboxes.get(brandId);
                    if (checkBox != null) {
                        checkBox.setSelected(true);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading product data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void updateProduct() {
        if (selectedProductId == null) {
            JOptionPane.showMessageDialog(this, "No product selected for update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sku = productSkuField.getText().trim();
        String name = productNameField.getText().trim();
        String description = productDescArea.getText().trim();
        String minPriceStr = productMinPriceField.getText().trim();
        String reorderStr = productReorderField.getText().trim();

        if (sku.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "SKU and Product Name are required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String selectedCategory = (String) productCategoryCombo.getSelectedItem();
        if ("-- Select Category --".equals(selectedCategory) || selectedCategory == null) {
            JOptionPane.showMessageDialog(this, "Please select a Category", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer categoryId = getKeyByValue(categoriesMap, selectedCategory);

        if (categoryId == null) {
            JOptionPane.showMessageDialog(this, "Please select a valid Category", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get selected compatible brands from checkboxes
        List<Integer> compatibleBrandIds = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> entry : brandCheckboxes.entrySet()) {
            Integer brandId = entry.getKey();
            JCheckBox checkBox = entry.getValue();
            if (checkBox.isSelected()) {
                compatibleBrandIds.add(brandId);
            }
        }

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
        productData.put("sku", sku);
        productData.put("name", name);
        productData.put("description", description);
        productData.put("minimumSellingPrice", minPrice);
        productData.put("reorderLevel", reorderLevel);
        productData.put("isActive", productActiveCheck.isSelected());
        productData.put("compatibleBrandIds", compatibleBrandIds);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    System.out.println("Updating product " + selectedProductId + " with data: " + productData);
                    String resp = client.put("/api/secure/products/" + selectedProductId, productData);
                    System.out.println("Response: " + resp);
                    success = true;
                    message = "Product updated successfully!";
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

    private void setEditMode(boolean edit) {
        isEditMode = edit;

        if (edit) {
            addProdBtn.setVisible(false);
            updateProdBtn.setVisible(true);

            // Update form title
            JPanel formPanel = (JPanel) getComponent(0); // Get the main panel
            JTabbedPane tabbedPane = (JTabbedPane) formPanel.getComponent(0);
            Component productsTab = tabbedPane.getComponentAt(3); // Products tab is at index 3
            if (productsTab instanceof JPanel) {
                JPanel productsPanel = (JPanel) productsTab;
                JSplitPane splitPane = (JSplitPane) productsPanel.getComponent(0);
                JPanel bottomPanel = (JPanel) splitPane.getRightComponent();
                bottomPanel.setBorder(BorderFactory.createTitledBorder("Edit Product (ID: " + selectedProductId + ")"));
            }
        } else {
            addProdBtn.setVisible(true);
            updateProdBtn.setVisible(false);
        }
    }

    private void cancelEdit() {
        clearProductForm();
        productsTable.clearSelection();
        editProductBtn.setEnabled(false);
    }

    // ========== STOCK BATCH CRUD METHODS ==========
    private void addStockBatch() {
        if (isEditBatchMode) {
            JOptionPane.showMessageDialog(this, "You are in edit mode. Please update or cancel edit first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String batchNo = batchNumberField.getText().trim();
        String buyingPriceStr = batchBuyingPriceField.getText().trim();
        String sellingPriceStr = batchSellingPriceField.getText().trim();
        String quantityReceivedStr = batchQuantityReceivedField.getText().trim();
        String quantityRemainingStr = batchQuantityRemainingField.getText().trim();
        String expiry = batchExpiryField.getText().trim();

        String selectedProduct = (String) batchProductCombo.getSelectedItem();
        String selectedCondition = (String) batchStockConditionCombo.getSelectedItem();

        if ("-- Select Product --".equals(selectedProduct) || selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Please select a product", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if ("-- Select Condition --".equals(selectedCondition) || selectedCondition == null) {
            JOptionPane.showMessageDialog(this, "Please select a stock condition", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer productId = getKeyByValue(productsMap, selectedProduct);
        Integer stockConditionId = getKeyByValue(stockConditionsMap, selectedCondition);

        if (productId == null) {
            JOptionPane.showMessageDialog(this, "Please select a product", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (stockConditionId == null) {
            JOptionPane.showMessageDialog(this, "Please select a stock condition", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (batchNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Batch number is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Double buyingPrice, sellingPrice;
        Integer quantityReceived, quantityRemaining;

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
            quantityReceived = Integer.parseInt(quantityReceivedStr);
            if (quantityReceived <= 0) {
                JOptionPane.showMessageDialog(this, "Quantity received must be greater than 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity received format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // For adding new batch, quantity remaining defaults to quantity received if not specified
        if (quantityRemainingStr.isEmpty()) {
            quantityRemaining = quantityReceived;
        } else {
            try {
                quantityRemaining = Integer.parseInt(quantityRemainingStr);
                if (quantityRemaining < 0) {
                    JOptionPane.showMessageDialog(this, "Quantity remaining cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (quantityRemaining > quantityReceived) {
                    JOptionPane.showMessageDialog(this, "Quantity remaining cannot be greater than quantity received", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Invalid quantity remaining format", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        Map<String, Object> batchData = new HashMap<>();
        batchData.put("productId", productId);
        batchData.put("conditionId", stockConditionId);
        batchData.put("batchNumber", batchNo);
        batchData.put("buyingPrice", buyingPrice);
        batchData.put("sellingPrice", sellingPrice);
        batchData.put("quantityReceived", quantityReceived);
        batchData.put("quantityRemaining", quantityRemaining);

        DateTimeFormatter isoFmt = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        batchData.put("receivedDate", LocalDateTime.now().format(isoFmt));

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

    // NEW: Stock Batch Edit functionality
    private void editSelectedStockBatch() {
        int row = stockBatchesTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a stock batch to edit", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int modelRow = stockBatchesTable.convertRowIndexToModel(row);
        selectedStockBatchId = (Integer) stockBatchesModel.getValueAt(modelRow, 0);

        // Load stock batch details
        loadStockBatchDetails(selectedStockBatchId);
    }

    private void loadStockBatchDetails(Integer batchId) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Map<String, Object> batchData = null;
            private String errorMessage = null;

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/stock-batches/" + batchId);
                    if (resp != null && !resp.trim().isEmpty()) {
                        batchData = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
                    }
                } catch (Exception e) {
                    errorMessage = "Error loading stock batch: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(InventoryPanel.this, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (batchData != null) {
                    populateStockBatchForm(batchData);
                    setBatchEditMode(true);
                }
            }
        };
        worker.execute();
    }

    private void populateStockBatchForm(Map<String, Object> batchData) {
        SwingUtilities.invokeLater(() -> {
            try {
                // Populate basic fields
                batchNumberField.setText(Objects.toString(batchData.get("batchNumber"), ""));

                // Set product
                Integer productId = safeInteger(batchData.get("productId"));
                if (productId != null) {
                    String productName = productsMap.get(productId);
                    if (productName != null) {
                        batchProductCombo.setSelectedItem(productName);
                    }
                }

                // Set stock condition
                Integer conditionId = safeInteger(batchData.get("conditionId"));
                if (conditionId != null) {
                    String conditionName = stockConditionsMap.get(conditionId);
                    if (conditionName != null) {
                        batchStockConditionCombo.setSelectedItem(conditionName);
                    }
                }

                // Set prices
                Double buyingPrice = safeDouble(batchData.get("buyingPrice"));
                if (buyingPrice != null) {
                    batchBuyingPriceField.setText(String.format("%.2f", buyingPrice));
                }

                Double sellingPrice = safeDouble(batchData.get("sellingPrice"));
                if (sellingPrice != null) {
                    batchSellingPriceField.setText(String.format("%.2f", sellingPrice));
                }

                // Set quantities
                Integer quantityReceived = safeInteger(batchData.get("quantityReceived"));
                if (quantityReceived != null) {
                    batchQuantityReceivedField.setText(quantityReceived.toString());
                    // When editing, quantity received is read-only
                    batchQuantityReceivedField.setEditable(false);
                }

                Integer quantityRemaining = safeInteger(batchData.get("quantityRemaining"));
                if (quantityRemaining != null) {
                    batchQuantityRemainingField.setText(quantityRemaining.toString());
                    // Quantity remaining is editable when updating
                    batchQuantityRemainingField.setEditable(true);
                }

                // Set expiry date
                String expiryDate = Objects.toString(batchData.get("expiryDate"), "");
                if (expiryDate != null && !expiryDate.isEmpty()) {
                    // Format the date for display
                    try {
                        LocalDateTime expiry = LocalDateTime.parse(expiryDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        batchExpiryField.setText(expiry.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    } catch (Exception e) {
                        batchExpiryField.setText(expiryDate);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error loading stock batch data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void updateStockBatch() {
        if (selectedStockBatchId == null) {
            JOptionPane.showMessageDialog(this, "No stock batch selected for update", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String batchNo = batchNumberField.getText().trim();
        String buyingPriceStr = batchBuyingPriceField.getText().trim();
        String sellingPriceStr = batchSellingPriceField.getText().trim();
        String quantityRemainingStr = batchQuantityRemainingField.getText().trim();
        String expiry = batchExpiryField.getText().trim();

        String selectedProduct = (String) batchProductCombo.getSelectedItem();
        String selectedCondition = (String) batchStockConditionCombo.getSelectedItem();

        if ("-- Select Product --".equals(selectedProduct) || selectedProduct == null) {
            JOptionPane.showMessageDialog(this, "Please select a product", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if ("-- Select Condition --".equals(selectedCondition) || selectedCondition == null) {
            JOptionPane.showMessageDialog(this, "Please select a stock condition", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Integer productId = getKeyByValue(productsMap, selectedProduct);
        Integer stockConditionId = getKeyByValue(stockConditionsMap, selectedCondition);

        if (productId == null) {
            JOptionPane.showMessageDialog(this, "Please select a product", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (stockConditionId == null) {
            JOptionPane.showMessageDialog(this, "Please select a stock condition", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (batchNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Batch number is required", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Double buyingPrice, sellingPrice;
        Integer quantityRemaining;
        Integer quantityReceived = safeInteger(batchQuantityReceivedField.getText().trim(), 0);

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
            quantityRemaining = Integer.parseInt(quantityRemainingStr);
            if (quantityRemaining < 0) {
                JOptionPane.showMessageDialog(this, "Quantity remaining cannot be negative", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (quantityRemaining > quantityReceived) {
                JOptionPane.showMessageDialog(this, "Quantity remaining cannot be greater than quantity received (" + quantityReceived + ")", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid quantity remaining format", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Object> batchData = new HashMap<>();
        batchData.put("productId", productId);
        batchData.put("conditionId", stockConditionId);
        batchData.put("batchNumber", batchNo);
        batchData.put("buyingPrice", buyingPrice);
        batchData.put("sellingPrice", sellingPrice);
        batchData.put("quantityReceived", quantityReceived);
        batchData.put("quantityRemaining", quantityRemaining);

        if (!expiry.isEmpty()) {
            batchData.put("expiryDate", expiry);
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    System.out.println("Updating stock batch " + selectedStockBatchId + " with data: " + batchData);
                    String resp = client.put("/api/secure/stock-batches/" + selectedStockBatchId, batchData);
                    System.out.println("Response: " + resp);
                    success = true;
                    message = "Stock batch updated successfully!";
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

    private void setBatchEditMode(boolean edit) {
        isEditBatchMode = edit;

        if (edit) {
            addBatchBtn.setVisible(false);
            updateBatchBtn.setVisible(true);
            cancelEditBatchBtn.setVisible(true);

            // Update form title
            JPanel formPanel = (JPanel) getComponent(0); // Get the main panel
            JTabbedPane tabbedPane = (JTabbedPane) formPanel.getComponent(0);
            Component batchesTab = tabbedPane.getComponentAt(4); // Batches tab is at index 4
            if (batchesTab instanceof JPanel) {
                JPanel batchesPanel = (JPanel) batchesTab;
                JSplitPane splitPane = (JSplitPane) batchesPanel.getComponent(0);
                JPanel bottomPanel = (JPanel) splitPane.getRightComponent();
                bottomPanel.setBorder(BorderFactory.createTitledBorder("Edit Stock Batch (ID: " + selectedStockBatchId + ")"));
            }
        } else {
            addBatchBtn.setVisible(true);
            updateBatchBtn.setVisible(false);
            cancelEditBatchBtn.setVisible(false);
        }
    }

    private void cancelBatchEdit() {
        clearBatchForm();
        stockBatchesTable.clearSelection();
        editStockBatchBtn.setEnabled(false);
    }

    // ========== HELPER METHODS ==========
    private String generateSKU() {
        return "PROD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int calculateProductStock(Integer productId) {
        int totalStock = 0;
        for (Map<String, Object> batch : stockBatchesList) {
            Integer batchProdId = safeInteger(batch.get("productId"));
            if (batchProdId != null && batchProdId.equals(productId)) {
                totalStock += safeInteger(batch.get("quantityRemaining"), 0);
            }
        }
        return totalStock;
    }

    private void showInventoryReport() {
        StringBuilder report = new StringBuilder();
        report.append("INVENTORY REPORT - OL KALOU WAITHAKA AUTO SPARES\n");
        report.append("=================================================\n\n");

        report.append("SUMMARY\n");
        report.append("-------\n");
        report.append(String.format("Total Categories: %d\n", categoriesList.size()));
        report.append(String.format("Total Vehicle Brands: %d\n", brandsList.size()));
        report.append(String.format("Total Stock Conditions: %d\n", stockConditionsList.size()));
        report.append(String.format("Total Products: %d\n", productsList.size()));
        report.append(String.format("Total Stock Batches: %d\n", stockBatchesList.size()));

        double totalStockValue = 0.0;
        int lowStockProducts = 0;
        Map<Integer, Integer> brandProductCount = new HashMap<>();

        for (Map<String, Object> prod : productsList) {
            Integer prodId = safeInteger(prod.get("id"));
            Integer reorderLevel = safeInteger(prod.get("reorderLevel"), 0);

            int stock = calculateProductStock(prodId);
            double stockValue = 0;

            for (Map<String, Object> batch : stockBatchesList) {
                if (safeInteger(batch.get("productId")).equals(prodId)) {
                    int qty = safeInteger(batch.get("quantityRemaining"), 0);
                    Double buyingPrice = safeDouble(batch.get("buyingPrice"));
                    stockValue += qty * (buyingPrice != null ? buyingPrice : 0);
                }
            }

            totalStockValue += stockValue;

            if (stock <= reorderLevel && stock > 0) {
                lowStockProducts++;
            }

            // Count products per brand
            try {
                String compatibleBrandsJson = Objects.toString(prod.get("compatibleBrandIds"), "[]");
                if (!compatibleBrandsJson.isEmpty()) {
                    List<Integer> brandIds = mapper.readValue(compatibleBrandsJson, new TypeReference<List<Integer>>() {});
                    for (Integer brandId : brandIds) {
                        brandProductCount.put(brandId, brandProductCount.getOrDefault(brandId, 0) + 1);
                    }
                }
            } catch (Exception e) {
                // Ignore JSON parsing errors
            }
        }

        report.append(String.format("Total Stock Value: ksh %,.2f\n", totalStockValue));
        report.append(String.format("Products Low on Stock: %d\n\n", lowStockProducts));

        // Low stock warning
        report.append("LOW STOCK WARNINGS\n");
        report.append("------------------\n");
        for (Map<String, Object> prod : productsList) {
            Integer prodId = safeInteger(prod.get("id"));
            String prodName = Objects.toString(prod.get("name"), "");
            Integer reorderLevel = safeInteger(prod.get("reorderLevel"), 0);

            int stock = calculateProductStock(prodId);

            if (stock <= reorderLevel) {
                report.append(String.format("⚠ %s: %d in stock (Reorder at: %d)\n",
                        prodName, stock, reorderLevel));
            }
        }

        // Brand compatibility summary
        report.append("\nPRODUCTS PER VEHICLE BRAND\n");
        report.append("-------------------------\n");
        for (Map.Entry<Integer, String> entry : brandsMap.entrySet()) {
            Integer brandId = entry.getKey();
            String brandName = entry.getValue();
            int productCount = brandProductCount.getOrDefault(brandId, 0);
            report.append(String.format("%s: %d compatible products\n", brandName, productCount));
        }

        // Stock condition summary
        report.append("\nSTOCK BY CONDITION\n");
        report.append("-----------------\n");
        Map<Integer, Integer> conditionStockCount = new HashMap<>();
        Map<Integer, Double> conditionStockValue = new HashMap<>();

        for (Map<String, Object> batch : stockBatchesList) {
            Integer conditionId = safeInteger(batch.get("conditionId"));
            Integer qty = safeInteger(batch.get("quantityRemaining"), 0);
            Double buyingPrice = safeDouble(batch.get("buyingPrice"));

            if (conditionId != null) {
                conditionStockCount.put(conditionId, conditionStockCount.getOrDefault(conditionId, 0) + qty);
                conditionStockValue.put(conditionId, conditionStockValue.getOrDefault(conditionId, 0.0) + (qty * (buyingPrice != null ? buyingPrice : 0)));
            }
        }

        for (Map.Entry<Integer, String> entry : stockConditionsMap.entrySet()) {
            Integer conditionId = entry.getKey();
            String conditionName = entry.getValue();
            int count = conditionStockCount.getOrDefault(conditionId, 0);
            double value = conditionStockValue.getOrDefault(conditionId, 0.0);

            if (count > 0) {
                report.append(String.format("%s: %d units worth ksh %,.2f\n",
                        conditionName, count, value));
            }
        }

        report.append("\nReport Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()));

        // Display report
        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 500));

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

    private String formatDate(Object value) {
        if (value == null) return "";
        String s = value.toString();
        DateTimeFormatter uiFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        try {
            LocalDateTime ldt = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return ldt.format(uiFmt);
        } catch (DateTimeParseException ignored) {}

        try {
            DateTimeFormatter dbFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            LocalDateTime ldt = LocalDateTime.parse(s, dbFmt);
            return ldt.format(uiFmt);
        } catch (DateTimeParseException ignored) {}

        try {
            DateTimeFormatter dbFmt2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(s, dbFmt2);
            return ldt.format(uiFmt);
        } catch (DateTimeParseException ignored) {}

        return s;
    }

    // Helper method for small buttons
    private void styleSmallButton(JButton button, Color color) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
    }
}