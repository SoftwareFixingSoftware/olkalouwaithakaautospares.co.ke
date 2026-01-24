package olkalouwaithakaautospares.co.ke.win.ui.dashboard.reports;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ReportingPanel — View sales reports and analytics
 *
 * Tabbed pane with:
 *   - "Daily Summary" (daily sales totals and metrics)
 *   - "Product Stats" (product performance)
 *   - "Cashier Performance" (cashier productivity)
 */
@SuppressWarnings("unchecked")
public class ReportingPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    // Date formatters
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // Daily Summary components
    private JTable dailyTable;
    private DefaultTableModel dailyModel;
    private JTextField dailyDateField;
    private JTextField dailyFromField;
    private JTextField dailyToField;
    // CHANGED: use JTextArea so multi-line summaries are visible and wrap
    private JTextArea dailySummaryArea;

    // Product Stats components
    private JTable productTable;
    private DefaultTableModel productModel;
    private TableRowSorter<DefaultTableModel> productSorter;
    private JTextField productDateField;
    private JTextField productFromField;
    private JTextField productToField;
    // CHANGED:
    private JTextArea productSummaryArea;
    private JTextField productSearchField;

    // Cashier Performance components
    private JTable cashierTable;
    private DefaultTableModel cashierModel;
    private JTextField cashierDateField;
    private JTextField cashierFromField;
    private JTextField cashierToField;
    // CHANGED:
    private JTextArea cashierSummaryArea;

    // Data caches
    private final List<Map<String, Object>> dailyData = new ArrayList<>();
    private final List<Map<String, Object>> productData = new ArrayList<>();
    private final List<Map<String, Object>> cashierData = new ArrayList<>();

    // Cache for product and cashier names
    private final Map<Integer, String> productNameCache = new HashMap<>();
    private final Map<Integer, String> cashierNameCache = new HashMap<>();

    public ReportingPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();
        loadDailySummaryForToday();
        loadProductStatsForToday();
        loadCashierPerformanceForToday();
        preloadProductNames();
        preloadCashierNames();
    }

    // ---------- UI Initialization ----------
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Main tabbed pane
        JTabbedPane mainTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        mainTabs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        mainTabs.setBackground(Color.WHITE);

        JPanel dailyPanel = createDailySummaryPanel();
        JPanel productPanel = createProductStatsPanel();
        JPanel cashierPanel = createCashierPerformancePanel();

        mainTabs.addTab("Daily Summary", dailyPanel);
        mainTabs.addTab("Product Stats", productPanel);
        mainTabs.addTab("Cashier Performance", cashierPanel);

        add(mainTabs, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Reports & Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(30, 33, 57));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        JButton refreshBtn = new JButton("Refresh All Reports");
        JButton exportBtn = new JButton("Export Data");

        styleButton(refreshBtn, new Color(33, 150, 243));
        styleButton(exportBtn, new Color(76, 175, 80));

        refreshBtn.addActionListener(e -> refreshAllReports());
        exportBtn.addActionListener(e -> exportData());

        buttonPanel.add(refreshBtn);
        buttonPanel.add(exportBtn);

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

    // ---------- Daily Summary Panel ----------
    private JPanel createDailySummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Filter panel
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createTitledBorder("Date Filters"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Single date filter
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        filterPanel.add(new JLabel("Single Date:"), gbc);

        dailyDateField = new JTextField(LocalDate.now().format(dateFormatter));
        dailyDateField.setColumns(10);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        filterPanel.add(dailyDateField, gbc);

        JButton fetchDateBtn = new JButton("Get Daily Report");
        styleButton(fetchDateBtn, new Color(33, 150, 243));
        fetchDateBtn.addActionListener(e -> loadDailySummaryForDate());
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        filterPanel.add(fetchDateBtn, gbc);

        // Date range filter
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        filterPanel.add(new JLabel("Date Range:"), gbc);

        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.setOpaque(false);

        dailyFromField = new JTextField(LocalDate.now().minusDays(7).format(dateFormatter));
        dailyFromField.setColumns(8);
        dailyToField = new JTextField(LocalDate.now().format(dateFormatter));
        dailyToField.setColumns(8);

        rangePanel.add(new JLabel("From:"));
        rangePanel.add(dailyFromField);
        rangePanel.add(new JLabel("To:"));
        rangePanel.add(dailyToField);

        JButton fetchRangeBtn = new JButton("Get Range Report");
        styleButton(fetchRangeBtn, new Color(255, 152, 0));
        fetchRangeBtn.addActionListener(e -> loadDailySummaryForRange());

        rangePanel.add(fetchRangeBtn);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        filterPanel.add(rangePanel, gbc);

        // Summary area (multi-line)
        dailySummaryArea = new JTextArea("Select a date or range to view daily sales summary");
        dailySummaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dailySummaryArea.setForeground(new Color(100, 100, 100));
        dailySummaryArea.setEditable(false);
        dailySummaryArea.setLineWrap(true);
        dailySummaryArea.setWrapStyleWord(true);
        dailySummaryArea.setOpaque(false);
        dailySummaryArea.setFocusable(false);
        dailySummaryArea.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        dailySummaryArea.setRows(2);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weightx = 1.0;
        filterPanel.add(dailySummaryArea, gbc);

        // Daily table
        String[] columns = {"Date", "Total Sales", "Total Profit", "Paid Sales", "Credit Sales", "Transactions"};
        dailyModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        dailyTable = new JTable(dailyModel);
        dailyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dailyTable.setRowHeight(36);
        dailyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Add selection listener to show details
        dailyTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDailySummaryDetails();
            }
        });

        JScrollPane scrollPane = new JScrollPane(dailyTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Daily Sales Summary"));

        // Summary cards panel
        JPanel summaryCards = createSummaryCardsPanel();

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.add(scrollPane, BorderLayout.CENTER);
        mainContent.add(summaryCards, BorderLayout.SOUTH);

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(mainContent, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSummaryCardsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 10));
        panel.setBackground(new Color(245, 247, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));


        return panel;
    }

    private JPanel createSummaryCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLabel.setForeground(new Color(100, 100, 100));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(color);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    // ---------- Product Stats Panel ----------
    private JPanel createProductStatsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top panel with filters and search
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBackground(Color.WHITE);

        // Filter panel
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createTitledBorder("Date Filters"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Single date filter
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        filterPanel.add(new JLabel("Single Date:"), gbc);

        productDateField = new JTextField(LocalDate.now().format(dateFormatter));
        productDateField.setColumns(10);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        filterPanel.add(productDateField, gbc);

        JButton fetchDateBtn = new JButton("Get Product Stats");
        styleButton(fetchDateBtn, new Color(33, 150, 243));
        fetchDateBtn.addActionListener(e -> loadProductStatsForDate());
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        filterPanel.add(fetchDateBtn, gbc);

        // Date range filter
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        filterPanel.add(new JLabel("Date Range:"), gbc);

        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.setOpaque(false);

        productFromField = new JTextField(LocalDate.now().minusDays(7).format(dateFormatter));
        productFromField.setColumns(8);
        productToField = new JTextField(LocalDate.now().format(dateFormatter));
        productToField.setColumns(8);

        rangePanel.add(new JLabel("From:"));
        rangePanel.add(productFromField);
        rangePanel.add(new JLabel("To:"));
        rangePanel.add(productToField);

        JButton fetchRangeBtn = new JButton("Get Range Stats");
        styleButton(fetchRangeBtn, new Color(255, 152, 0));
        fetchRangeBtn.addActionListener(e -> loadProductStatsForRange());

        rangePanel.add(fetchRangeBtn);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        filterPanel.add(rangePanel, gbc);

        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(new JLabel("Search:"));

        // create search field (placeholder behavior handled by FlatLaf or custom if needed)
        productSearchField = new JTextField(15);
        productSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productSearchField.setForeground(Color.black);
        // NOTE: do NOT attach action listener solely for search button press — we will apply live filtering via DocumentListener below.

        JButton searchBtn = new JButton("Search");
        styleButton(searchBtn, new Color(158, 158, 158));
        searchBtn.addActionListener(e -> filterProducts(productSearchField.getText())); // still available if user prefers click

        searchPanel.add(productSearchField);
        searchPanel.add(searchBtn);

        topPanel.add(filterPanel, BorderLayout.CENTER);
        topPanel.add(searchPanel, BorderLayout.EAST);

        // Summary area (multi-line)
        productSummaryArea = new JTextArea("Select a date or range to view product statistics");
        productSummaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productSummaryArea.setForeground(new Color(100, 100, 100));
        productSummaryArea.setEditable(false);
        productSummaryArea.setLineWrap(true);
        productSummaryArea.setWrapStyleWord(true);
        productSummaryArea.setOpaque(false);
        productSummaryArea.setFocusable(false);
        productSummaryArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        productSummaryArea.setRows(2);

        // Product table
        String[] columns = {"Product ID", "Product Name", "Date", "Quantity Sold", "Revenue", "Profit", "Profit %"};
        productModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        productTable = new JTable(productModel);
        productTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        productTable.setRowHeight(36);
        productTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Add sorter for filtering
        productSorter = new TableRowSorter<>(productModel);
        productTable.setRowSorter(productSorter);

        // --- LIVE FILTER: attach DocumentListener after sorter exists ---
        productSearchField.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String q = productSearchField.getText().trim();
                filterProducts(q);
            }
            @Override
            public void insertUpdate(DocumentEvent e) { apply(); }
            @Override
            public void removeUpdate(DocumentEvent e) { apply(); }
            @Override
            public void changedUpdate(DocumentEvent e) { apply(); }
        });

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Product Sales Statistics"));

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(productSummaryArea, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    // ---------- Cashier Performance Panel ----------
    private JPanel createCashierPerformancePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Filter panel
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createTitledBorder("Date Filters"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Single date filter
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        filterPanel.add(new JLabel("Single Date:"), gbc);

        cashierDateField = new JTextField(LocalDate.now().format(dateFormatter));
        cashierDateField.setColumns(10);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        filterPanel.add(cashierDateField, gbc);

        JButton fetchDateBtn = new JButton("Get Cashier Stats");
        styleButton(fetchDateBtn, new Color(33, 150, 243));
        fetchDateBtn.addActionListener(e -> loadCashierPerformanceForDate());
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        filterPanel.add(fetchDateBtn, gbc);

        // Date range filter
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        filterPanel.add(new JLabel("Date Range:"), gbc);

        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.setOpaque(false);

        cashierFromField = new JTextField(LocalDate.now().minusDays(7).format(dateFormatter));
        cashierFromField.setColumns(8);
        cashierToField = new JTextField(LocalDate.now().format(dateFormatter));
        cashierToField.setColumns(8);

        rangePanel.add(new JLabel("From:"));
        rangePanel.add(cashierFromField);
        rangePanel.add(new JLabel("To:"));
        rangePanel.add(cashierToField);

        JButton fetchRangeBtn = new JButton("Get Range Stats");
        styleButton(fetchRangeBtn, new Color(255, 152, 0));
        fetchRangeBtn.addActionListener(e -> loadCashierPerformanceForRange());

        rangePanel.add(fetchRangeBtn);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        filterPanel.add(rangePanel, gbc);

        // Summary area (multi-line)
        cashierSummaryArea = new JTextArea("Select a date or range to view cashier performance");
        cashierSummaryArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cashierSummaryArea.setForeground(new Color(100, 100, 100));
        cashierSummaryArea.setEditable(false);
        cashierSummaryArea.setLineWrap(true);
        cashierSummaryArea.setWrapStyleWord(true);
        cashierSummaryArea.setOpaque(false);
        cashierSummaryArea.setFocusable(false);
        cashierSummaryArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        cashierSummaryArea.setRows(2);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weightx = 1.0;
        filterPanel.add(cashierSummaryArea, gbc);

        // Cashier table
        String[] columns = {"Cashier ID", "Cashier Name", "Date", "Total Sales", "Paid Sales", "Credit Sales", "Transactions", "Avg. Sale"};
        cashierModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        cashierTable = new JTable(cashierModel);
        cashierTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cashierTable.setRowHeight(36);
        cashierTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(cashierTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Cashier Performance"));

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    // ---------- Data Loading Methods ----------
    private void loadDailySummaryForDate() {
        String inputDate = dailyDateField.getText().trim();

        if (inputDate.isEmpty()) {
            inputDate = LocalDate.now().format(dateFormatter);
            dailyDateField.setText(inputDate);
        }

        if (!validateDate(inputDate)) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        final String dateStr = inputDate;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private Map<String, Object> summary = null;

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/reports/daily?date=" + dateStr);
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            summary = client.parseResponse(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof Map) {
                                summary = (Map<String, Object>) r.get("data");
                            } else {
                                summary = r;
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
                    showError("Failed to load daily summary: " + error.getMessage());
                    return;
                }

                dailyData.clear();
                dailyModel.setRowCount(0);

                if (summary != null) {
                    dailyData.add(summary);
                    updateDailyTable();

                    Double totalSales = safeDoubleFromObject(summary.get("totalSales"));
                    Double totalProfit = safeDoubleFromObject(summary.get("totalProfit"));
                    Integer transactions = safeIntegerFromObject(summary.get("totalTransactions"));

                    String summaryText = String.format(
                            "Date: %s | Total Sales: ksh %,.2f | Profit: ksh %,.2f | Transactions: %d",
                            formatDisplayDate(dateStr),
                            totalSales != null ? totalSales : 0.0,
                            totalProfit != null ? totalProfit : 0.0,
                            transactions != null ? transactions : 0
                    );
                    SwingUtilities.invokeLater(() -> {
                        dailySummaryArea.setText(summaryText);
                        dailySummaryArea.revalidate();
                        dailySummaryArea.repaint();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> dailySummaryArea.setText("No data for " + formatDisplayDate(dateStr)));
                }
            }
        };

        worker.execute();
    }

    private void loadDailySummaryForRange() {
        String fromInput = dailyFromField.getText().trim();
        String toInput = dailyToField.getText().trim();

        if (fromInput.isEmpty() || toInput.isEmpty()) {
            showError("Please enter both from and to dates");
            return;
        }

        if (!validateDate(fromInput) || !validateDate(toInput)) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        try {
            LocalDate from = LocalDate.parse(fromInput, dateFormatter);
            LocalDate to = LocalDate.parse(toInput, dateFormatter);

            if (from.isAfter(to)) {
                showError("From date must be before or equal to To date");
                return;
            }
        } catch (DateTimeParseException e) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        final String fromStr = fromInput;
        final String toStr = toInput;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> summaries = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get(
                            "/api/secure/reports/daily/range?from=" + fromStr + "&to=" + toStr
                    );

                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            summaries = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                summaries = (List<Map<String, Object>>) r.get("data");
                            } else {
                                summaries = new ArrayList<>();
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
                    showError("Failed to load daily summaries: " + error.getMessage());
                    return;
                }

                dailyData.clear();
                dailyModel.setRowCount(0);

                if (summaries != null && !summaries.isEmpty()) {
                    dailyData.addAll(summaries);
                    updateDailyTable();

                    double totalSales = 0;
                    double totalProfit = 0;
                    int totalTransactions = 0;

                    for (Map<String, Object> summary : summaries) {
                        totalSales += safeDoubleFromObject(summary.get("totalSales"), 0.0);
                        totalProfit += safeDoubleFromObject(summary.get("totalProfit"), 0.0);
                        totalTransactions += safeIntegerFromObject(summary.get("totalTransactions"), 0);
                    }

                    double avgSale = totalTransactions > 0
                            ? totalSales / totalTransactions
                            : 0;

                    String summaryText = String.format(
                            "Range: %s to %s | Days: %d | Total Sales: ksh %,.2f | Total Profit: ksh %,.2f | Avg/Day: ksh %,.2f",
                            formatDisplayDate(fromStr),
                            formatDisplayDate(toStr),
                            summaries.size(),
                            totalSales,
                            totalProfit,
                            totalSales / summaries.size()
                    );

                    SwingUtilities.invokeLater(() -> {
                        dailySummaryArea.setText(summaryText);
                        dailySummaryArea.revalidate();
                        dailySummaryArea.repaint();
                    });
                } else {
                    SwingUtilities.invokeLater(() -> dailySummaryArea.setText(
                            "No data for range " + formatDisplayDate(fromStr) + " to " + formatDisplayDate(toStr)
                    ));
                }
            }
        };

        worker.execute();
    }


    private void loadProductStatsForDate() {
        String inputDate = productDateField.getText().trim();

        if (inputDate.isEmpty()) {
            inputDate = LocalDate.now().format(dateFormatter);
            productDateField.setText(inputDate);
        }

        if (!validateDate(inputDate)) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        final String dateStr = inputDate; // ✅ final copy for inner class

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> stats = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/reports/products?date=" + dateStr);
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            stats = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                stats = (List<Map<String, Object>>) r.get("data");
                            } else {
                                stats = new ArrayList<>();
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
                    showError("Failed to load product stats: " + error.getMessage());
                    return;
                }

                productData.clear();
                productModel.setRowCount(0);

                if (stats != null && !stats.isEmpty()) {
                    productData.addAll(stats);
                    updateProductTable();

                    double totalRevenue = 0;
                    double totalProfit = 0;
                    int totalQuantity = 0;

                    for (Map<String, Object> stat : stats) {
                        totalRevenue += safeDoubleFromObject(stat.get("revenue"), 0.0);
                        totalProfit += safeDoubleFromObject(stat.get("profit"), 0.0);
                        totalQuantity += safeIntegerFromObject(stat.get("quantitySold"), 0);
                    }

                    double profitMargin = totalRevenue > 0
                            ? (totalProfit / totalRevenue * 100)
                            : 0;

                    String summaryText = String.format(
                            "Date: %s | Products: %d | Total Revenue: ksh %,.2f | Total Profit: ksh %,.2f | Profit Margin: %.1f%% | Total Units: %d",
                            formatDisplayDate(dateStr),
                            stats.size(),
                            totalRevenue,
                            totalProfit,
                            profitMargin,
                            totalQuantity
                    );
                    SwingUtilities.invokeLater(() -> productSummaryArea.setText(summaryText));
                } else {
                    SwingUtilities.invokeLater(() -> productSummaryArea.setText("No product sales for " + formatDisplayDate(dateStr)));
                }
            }
        };

        worker.execute();
    }

    private void loadProductStatsForRange() {
        String fromStr = productFromField.getText().trim();
        String toStr = productToField.getText().trim();

        if (fromStr.isEmpty() || toStr.isEmpty()) {
            showError("Please enter both from and to dates");
            return;
        }

        if (!validateDate(fromStr) || !validateDate(toStr)) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        try {
            LocalDate from = LocalDate.parse(fromStr, dateFormatter);
            LocalDate to = LocalDate.parse(toStr, dateFormatter);

            if (from.isAfter(to)) {
                showError("From date must be before or equal to To date");
                return;
            }
        } catch (DateTimeParseException e) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> stats = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/reports/products/range?from=" + fromStr + "&to=" + toStr);
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            stats = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                stats = (List<Map<String, Object>>) r.get("data");
                            } else {
                                stats = new ArrayList<>();
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
                    showError("Failed to load product stats: " + error.getMessage());
                    return;
                }

                productData.clear();
                productModel.setRowCount(0);

                if (stats != null && !stats.isEmpty()) {
                    productData.addAll(stats);
                    updateProductTable();

                    double totalRevenue = 0;
                    double totalProfit = 0;
                    int totalQuantity = 0;

                    for (Map<String, Object> stat : stats) {
                        totalRevenue += safeDoubleFromObject(stat.get("revenue"), 0.0);
                        totalProfit += safeDoubleFromObject(stat.get("profit"), 0.0);
                        totalQuantity += safeIntegerFromObject(stat.get("quantitySold"), 0);
                    }

                    double profitMargin = totalRevenue > 0 ? (totalProfit / totalRevenue * 100) : 0;

                    String summaryText = String.format(
                            "Range: %s to %s | Products: %d | Total Revenue: ksh %,.2f | Total Profit: ksh %,.2f | Profit Margin: %.1f%%",
                            formatDisplayDate(fromStr), formatDisplayDate(toStr), stats.size(), totalRevenue, totalProfit, profitMargin
                    );
                    SwingUtilities.invokeLater(() -> productSummaryArea.setText(summaryText));
                } else {
                    SwingUtilities.invokeLater(() -> productSummaryArea.setText("No product sales for range " + formatDisplayDate(fromStr) + " to " + formatDisplayDate(toStr)));
                }
            }
        };
        worker.execute();
    }

    private void loadCashierPerformanceForDate() {
        String inputDate = cashierDateField.getText().trim();

        if (inputDate.isEmpty()) {
            inputDate = LocalDate.now().format(dateFormatter);
            cashierDateField.setText(inputDate);
        }

        if (!validateDate(inputDate)) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        final String dateStr = inputDate; // ✅ final copy for SwingWorker

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> performances = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/reports/cashiers?date=" + dateStr);
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            performances = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                performances = (List<Map<String, Object>>) r.get("data");
                            } else {
                                performances = new ArrayList<>();
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
                    showError("Failed to load cashier performance: " + error.getMessage());
                    return;
                }

                cashierData.clear();
                cashierModel.setRowCount(0);

                if (performances != null && !performances.isEmpty()) {
                    cashierData.addAll(performances);
                    updateCashierTable();

                    double totalSales = 0;
                    double paidSales = 0;
                    double creditSales = 0;
                    int totalTransactions = 0;

                    for (Map<String, Object> perf : performances) {
                        totalSales += safeDoubleFromObject(perf.get("totalSales"), 0.0);
                        paidSales += safeDoubleFromObject(perf.get("paidSales"), 0.0);
                        creditSales += safeDoubleFromObject(perf.get("creditSales"), 0.0);
                        totalTransactions += safeIntegerFromObject(perf.get("totalTransactions"), 0);
                    }

                    double avgSale = totalTransactions > 0
                            ? totalSales / totalTransactions
                            : 0;

                    String summaryText = String.format(
                            "Date: %s | Cashiers: %d | Total Sales: ksh %,.2f | Paid: ksh %,.2f | Credit: ksh %,.2f | Avg Sale: ksh %,.2f",
                            formatDisplayDate(dateStr),
                            performances.size(),
                            totalSales,
                            paidSales,
                            creditSales,
                            avgSale
                    );
                    SwingUtilities.invokeLater(() -> cashierSummaryArea.setText(summaryText));
                } else {
                    SwingUtilities.invokeLater(() -> cashierSummaryArea.setText("No cashier performance data for " + formatDisplayDate(dateStr)));
                }
            }
        };

        worker.execute();
    }

    private void loadCashierPerformanceForRange() {
        String fromStr = cashierFromField.getText().trim();
        String toStr = cashierToField.getText().trim();

        if (fromStr.isEmpty() || toStr.isEmpty()) {
            showError("Please enter both from and to dates");
            return;
        }

        if (!validateDate(fromStr) || !validateDate(toStr)) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        try {
            LocalDate from = LocalDate.parse(fromStr, dateFormatter);
            LocalDate to = LocalDate.parse(toStr, dateFormatter);

            if (from.isAfter(to)) {
                showError("From date must be before or equal to To date");
                return;
            }
        } catch (DateTimeParseException e) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> performances = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/reports/cashiers/range?from=" + fromStr + "&to=" + toStr);
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            performances = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                performances = (List<Map<String, Object>>) r.get("data");
                            } else {
                                performances = new ArrayList<>();
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
                    showError("Failed to load cashier performance: " + error.getMessage());
                    return;
                }

                cashierData.clear();
                cashierModel.setRowCount(0);

                if (performances != null && !performances.isEmpty()) {
                    cashierData.addAll(performances);
                    updateCashierTable();

                    double totalSales = 0;
                    double paidSales = 0;
                    double creditSales = 0;
                    int totalTransactions = 0;

                    for (Map<String, Object> perf : performances) {
                        totalSales += safeDoubleFromObject(perf.get("totalSales"), 0.0);
                        paidSales += safeDoubleFromObject(perf.get("paidSales"), 0.0);
                        creditSales += safeDoubleFromObject(perf.get("creditSales"), 0.0);
                        totalTransactions += safeIntegerFromObject(perf.get("totalTransactions"), 0);
                    }

                    double avgSale = totalTransactions > 0 ? totalSales / totalTransactions : 0;

                    String summaryText = String.format(
                            "Range: %s to %s | Cashiers: %d | Total Sales: ksh %,.2f | Paid: ksh %,.2f | Credit: ksh %,.2f",
                            formatDisplayDate(fromStr), formatDisplayDate(toStr), performances.size(), totalSales, paidSales, creditSales
                    );
                    SwingUtilities.invokeLater(() -> cashierSummaryArea.setText(summaryText));
                } else {
                    SwingUtilities.invokeLater(() -> cashierSummaryArea.setText("No cashier performance data for range " + formatDisplayDate(fromStr) + " to " + formatDisplayDate(toStr)));
                }
            }
        };
        worker.execute();
    }

    // ---------- Default Loaders ----------
    private void loadDailySummaryForToday() {
        String today = LocalDate.now().format(dateFormatter);
        dailyDateField.setText(today);
        loadDailySummaryForDate();
    }

    private void loadProductStatsForToday() {
        String today = LocalDate.now().format(dateFormatter);
        productDateField.setText(today);
        loadProductStatsForDate();
    }

    private void loadCashierPerformanceForToday() {
        String today = LocalDate.now().format(dateFormatter);
        cashierDateField.setText(today);
        loadCashierPerformanceForDate();
    }

    // ---------- UI Update Methods ----------
    private void updateDailyTable() {
        SwingUtilities.invokeLater(() -> {
            dailyModel.setRowCount(0);
            for (Map<String, Object> summary : dailyData) {
                String date = formatDisplayDate(Objects.toString(summary.get("saleDate"), ""));
                Double totalSales = safeDoubleFromObject(summary.get("totalSales"));
                Double totalProfit = safeDoubleFromObject(summary.get("totalProfit"));
                Double paidSales = safeDoubleFromObject(summary.get("paidSales"));
                Double creditSales = safeDoubleFromObject(summary.get("creditSales"));
                Integer transactions = safeIntegerFromObject(summary.get("totalTransactions"));

                dailyModel.addRow(new Object[]{
                        date,
                        String.format("ksh %,.2f", totalSales != null ? totalSales : 0.0),
                        String.format("ksh %,.2f", totalProfit != null ? totalProfit : 0.0),
                        String.format("ksh %,.2f", paidSales != null ? paidSales : 0.0),
                        String.format("ksh %,.2f", creditSales != null ? creditSales : 0.0),
                        transactions != null ? transactions : 0
                });
            }
        });
    }

    private void updateProductTable() {
        SwingUtilities.invokeLater(() -> {
            productModel.setRowCount(0);
            for (Map<String, Object> stat : productData) {
                Integer productId = safeIntegerFromObject(stat.get("productId"));
                String productName = getProductName(productId);
                String date = formatDisplayDate(Objects.toString(stat.get("saleDate"), ""));
                Integer quantity = safeIntegerFromObject(stat.get("quantitySold"));
                Double revenue = safeDoubleFromObject(stat.get("revenue"));
                Double profit = safeDoubleFromObject(stat.get("profit"));

                double profitPercent = (revenue != null && revenue > 0 && profit != null) ?
                        (profit / revenue * 100) : 0;

                productModel.addRow(new Object[]{
                        productId != null ? productId : "N/A",
                        productName,
                        date,
                        quantity != null ? quantity : 0,
                        String.format("ksh %,.2f", revenue != null ? revenue : 0.0),
                        String.format("ksh %,.2f", profit != null ? profit : 0.0),
                        String.format("%.1f%%", profitPercent)
                });
            }
        });
    }

    private void updateCashierTable() {
        SwingUtilities.invokeLater(() -> {
            cashierModel.setRowCount(0);
            for (Map<String, Object> perf : cashierData) {
                Integer cashierId = safeIntegerFromObject(perf.get("cashierId"));
                String cashierName = getCashierName(cashierId);
                String date = formatDisplayDate(Objects.toString(perf.get("saleDate"), ""));
                Double totalSales = safeDoubleFromObject(perf.get("totalSales"));
                Double paidSales = safeDoubleFromObject(perf.get("paidSales"));
                Double creditSales = safeDoubleFromObject(perf.get("creditSales"));
                Integer transactions = safeIntegerFromObject(perf.get("totalTransactions"));

                double avgSale = (totalSales != null && transactions != null && transactions > 0) ?
                        totalSales / transactions : 0;

                cashierModel.addRow(new Object[]{
                        cashierId != null ? cashierId : "N/A",
                        cashierName,
                        date,
                        String.format("ksh %,.2f", totalSales != null ? totalSales : 0.0),
                        String.format("ksh %,.2f", paidSales != null ? paidSales : 0.0),
                        String.format("ksh %,.2f", creditSales != null ? creditSales : 0.0),
                        transactions != null ? transactions : 0,
                        String.format("ksh %,.2f", avgSale)
                });
            }
        });
    }

    // ---------- Preloading Data Methods ----------
    private void preloadProductNames() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/products");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> products = client.parseResponseList(resp);
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

    private void preloadCashierNames() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/auth/me");
                    if (resp != null && !resp.trim().isEmpty()) {
                        List<Map<String, Object>> users = client.parseResponseList(resp);
                        for (Map<String, Object> user : users) {
                            Integer id = safeIntegerFromObject(user.get("id"));
                            String name = Objects.toString(user.get("name"), "");
                            String role = Objects.toString(user.get("role"), "");
                            if (id != null && !name.isEmpty() && "CASHIER".equalsIgnoreCase(role)) {
                                cashierNameCache.put(id, name);
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

    private String getProductName(Integer productId) {
        if (productId == null) return "Unknown Product";
        return productNameCache.getOrDefault(productId, "Product #" + productId);
    }

    private String getCashierName(Integer cashierId) {
        if (cashierId == null) return "Unknown Cashier";
        return cashierNameCache.getOrDefault(cashierId, "Cashier #" + cashierId);
    }

    /**
     * Live filter for products.
     * - empty -> show all
     * - numeric input -> filter by product id starting with the digits
     * - non-numeric input -> fallback to product name contains (case-insensitive)
     */
    private void filterProducts(String query) {
        if (query == null || query.isEmpty()) {
            productSorter.setRowFilter(null);
            return;
        }

        // Trim whitespace
        query = query.trim();

        // If query is all digits -> filter by product ID column (column 0) using starts-with regex
        if (query.matches("\\d+")) {
            // anchor at start so "12" matches 12, 120, 1234 etc.
            String regex = "^" + Pattern.quote(query);
            try {
                RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter(regex, 0);
                productSorter.setRowFilter(rf);
            } catch (Exception ex) {
                // fallback to no filter on error
                productSorter.setRowFilter(null);
            }
            return;
        }

        // Non-numeric -> search product name column (column 1), case-insensitive substring
        try {
            RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + Pattern.quote(query), 1);
            productSorter.setRowFilter(rf);
        } catch (Exception ex) {
            productSorter.setRowFilter(null);
        }
    }

    // ---------- Event Handlers ----------
    private void refreshAllReports() {
        loadDailySummaryForToday();
        loadProductStatsForToday();
        loadCashierPerformanceForToday();
    }

    private void exportData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Report Data");
        fileChooser.setSelectedFile(new java.io.File("report_" +
                LocalDate.now().format(dateFormatter) + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this,
                    "Export functionality would be implemented here.\n" +
                            "Selected file: " + fileChooser.getSelectedFile().getName(),
                    "Export", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showDailySummaryDetails() {
        int selectedRow = dailyTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = dailyTable.convertRowIndexToModel(selectedRow);
            if (modelRow < dailyData.size()) {
                Map<String, Object> summary = dailyData.get(modelRow);

                StringBuilder details = new StringBuilder();
                details.append("Daily Sales Details\n");
                details.append("===================\n");
                details.append(String.format("Date: %s%n",
                        formatDisplayDate(Objects.toString(summary.get("saleDate"), ""))));
                details.append(String.format("Total Sales: ksh %,.2f%n",
                        safeDoubleFromObject(summary.get("totalSales"), 0.0)));
                details.append(String.format("Total Profit: ksh %,.2f%n",
                        safeDoubleFromObject(summary.get("totalProfit"), 0.0)));
                details.append(String.format("Paid Sales: ksh %,.2f%n",
                        safeDoubleFromObject(summary.get("paidSales"), 0.0)));
                details.append(String.format("Credit Sales: ksh %,.2f%n",
                        safeDoubleFromObject(summary.get("creditSales"), 0.0)));
                details.append(String.format("Transactions: %d%n",
                        safeIntegerFromObject(summary.get("totalTransactions"), 0)));

                JTextArea textArea = new JTextArea(details.toString());
                textArea.setEditable(false);
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(400, 200));

                JOptionPane.showMessageDialog(this, scrollPane,
                        "Daily Sales Details", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    // ---------- Helper Methods ----------
    private boolean validateDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, dateFormatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String formatDisplayDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr, dateFormatter);
            return date.format(displayFormatter);
        } catch (Exception e) {
            return dateStr;
        }
    }

    private Double safeDoubleFromObject(Object o) {
        return safeDoubleFromObject(o, null);
    }

    private Double safeDoubleFromObject(Object o, Double defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
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

    private void showError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }
}
