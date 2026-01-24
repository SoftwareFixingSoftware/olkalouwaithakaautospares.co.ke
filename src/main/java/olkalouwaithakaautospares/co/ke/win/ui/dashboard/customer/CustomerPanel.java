package olkalouwaithakaautospares.co.ke.win.ui.dashboard.customer;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/**
 * CustomerPanel — Manage customers and view analytics
 *
 * Left: customer list with search
 * Right: tabbed pane with:
 *   - "Details" (view/edit customer info)
 *   - "Analytics" (customer analytics with date filters)
 */
@SuppressWarnings("unchecked")
public class CustomerPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    private final List<Map<String, Object>> customers = new ArrayList<>();
    private final List<Map<String, Object>> analyticsData = new ArrayList<>();

    // Customer list components
    private JTable customerTable;
    private DefaultTableModel customerModel;
    private TableRowSorter<DefaultTableModel> customerSorter;
    private JTextField searchField;

    // Details tab components
    private JTextField idField;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextField creditLimitField;
    private JCheckBox activeCheckBox;
    private JTextField createdAtField;
    private JButton saveButton;
    private JButton deleteButton;
    private JButton clearButton;

    // Analytics tab components
    private JTable analyticsTable;
    private DefaultTableModel analyticsModel;
    private JTextField analyticsDateField;
    private JTextField fromDateField;
    private JTextField toDateField;
    private JLabel analyticsSummaryLabel;

    // Buttons exposed so workers can enable/disable them
    private JButton refreshBtn;
    private JButton fetchDateBtn;
    private JButton fetchRangeBtn;

    // Date formatters
    private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public CustomerPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();
        loadCustomers();
    }

    // ---------- UI Initialization ----------
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Left customer panel
        JPanel customerListPanel = createCustomerListPanel();

        // Right: tabbed pane containing Details + Analytics
        JTabbedPane rightTabs = new JTabbedPane();
        JPanel detailsPanel = createDetailsPanel();
        JPanel analyticsPanel = createAnalyticsPanel();

        rightTabs.addTab("Details", detailsPanel);
        rightTabs.addTab("Analytics", analyticsPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, customerListPanel, rightTabs);
        splitPane.setDividerLocation(500);
        splitPane.setResizeWeight(0.5);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Customer Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(30, 33, 57));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);

        refreshBtn = new JButton("Refresh");
        styleButton(refreshBtn, new Color(33, 150, 243));

        refreshBtn.addActionListener(e -> {
            loadCustomers();
            loadAnalyticsForToday();
        });

        // NOTE: New Customer button removed (customers are created from Sales)
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

    // ---------- Customer List Panel ----------
    private JPanel createCustomerListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        // Search panel
        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        searchField.putClientProperty("JTextField.placeholderText", "Search by name or phone...");

        // Add key listener for real-time search
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterCustomers(searchField.getText());
            }
        });

        JButton searchBtn = new JButton("Search");
        styleButton(searchBtn, new Color(33, 150, 243));
        searchBtn.addActionListener(e -> filterCustomers(searchField.getText()));

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchBtn, BorderLayout.EAST);

        // Customer table
        String[] columns = {"ID", "Name", "Phone", "Credit Limit", "Status", "Created"};
        customerModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        customerTable = new JTable(customerModel);
        customerTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        customerTable.setRowHeight(36);
        customerTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        customerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Add sorter for filtering
        customerSorter = new TableRowSorter<>(customerModel);
        customerTable.setRowSorter(customerSorter);

        customerTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    onCustomerSelected();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(customerTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        scrollPane.setBorder(null);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    // ---------- Details Panel ----------
    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel detailsTitle = new JLabel("Customer Details");
        detailsTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        detailsTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: ID
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Customer ID:"), gbc);
        idField = new JTextField();
        idField.setEditable(false);
        idField.setBackground(new Color(245, 245, 245));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        formPanel.add(idField, gbc);

        // Row 1: Name
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Name:"), gbc);
        nameField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        formPanel.add(nameField, gbc);

        // Row 2: Phone
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        formPanel.add(new JLabel("Phone:"), gbc);
        phoneField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        formPanel.add(phoneField, gbc);

        // Row 3: Credit Limit
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        formPanel.add(new JLabel("Credit Limit:"), gbc);
        creditLimitField = new JTextField();
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        formPanel.add(creditLimitField, gbc);

        // Row 4: Active Status
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        formPanel.add(new JLabel("Active:"), gbc);
        activeCheckBox = new JCheckBox();
        activeCheckBox.setSelected(true);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        formPanel.add(activeCheckBox, gbc);

        // Row 5: Created At
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        formPanel.add(new JLabel("Created:"), gbc);
        createdAtField = new JTextField();
        createdAtField.setEditable(false);
        createdAtField.setBackground(new Color(245, 245, 245));
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0;
        formPanel.add(createdAtField, gbc);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));

        saveButton = new JButton("Save");
        deleteButton = new JButton("Deactivate");
        clearButton = new JButton("Clear");

        styleButton(saveButton, new Color(76, 175, 80));
        styleButton(deleteButton, new Color(244, 67, 54));
        styleButton(clearButton, new Color(158, 158, 158));

        saveButton.addActionListener(e -> saveCustomer());
        deleteButton.addActionListener(e -> toggleActivationForCustomer());
        clearButton.addActionListener(e -> clearDetailsForm());

        buttonPanel.add(clearButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);

        panel.add(detailsTitle, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ---------- Analytics Panel ----------
    private JPanel createAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

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

        analyticsDateField = new JTextField(LocalDate.now().format(dateFormatter));
        analyticsDateField.setColumns(10);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        filterPanel.add(analyticsDateField, gbc);

        fetchDateBtn = new JButton("Fetch Date");
        styleButton(fetchDateBtn, new Color(33, 150, 243));
        fetchDateBtn.addActionListener(e -> loadAnalyticsForDate());
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0;
        filterPanel.add(fetchDateBtn, gbc);

        // Date range filter
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        filterPanel.add(new JLabel("Date Range:"), gbc);

        JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        rangePanel.setOpaque(false);

        fromDateField = new JTextField(LocalDate.now().minusDays(7).format(dateFormatter));
        fromDateField.setColumns(8);
        toDateField = new JTextField(LocalDate.now().format(dateFormatter));
        toDateField.setColumns(8);

        rangePanel.add(new JLabel("From:"));
        rangePanel.add(fromDateField);
        rangePanel.add(new JLabel("To:"));
        rangePanel.add(toDateField);

        fetchRangeBtn = new JButton("Fetch Range");
        styleButton(fetchRangeBtn, new Color(255, 152, 0));
        fetchRangeBtn.addActionListener(e -> loadAnalyticsForRange());

        rangePanel.add(fetchRangeBtn);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.weightx = 1.0;
        filterPanel.add(rangePanel, gbc);

        // Summary label
        analyticsSummaryLabel = new JLabel("Select a date or range to view analytics");
        analyticsSummaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        analyticsSummaryLabel.setForeground(new Color(100, 100, 100));
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 3; gbc.weightx = 1.0;
        filterPanel.add(analyticsSummaryLabel, gbc);

        // Analytics table
        String[] columns = {"Date", "Total Customers", "New Customers", "Repeat Customers", "Credit Customers", "Created"};
        analyticsModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        analyticsTable = new JTable(analyticsModel);
        analyticsTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        analyticsTable.setRowHeight(36);
        analyticsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane scrollPane = new JScrollPane(analyticsTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Customer Analytics"));

        panel.add(filterPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Load today's analytics by default
        loadAnalyticsForDate();

        return panel;
    }

    // ---------- Data Loading ----------
    private void loadCustomers() {
        // disable refresh while loading
        setRefreshEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> loaded = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/customers");
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            loaded = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            // Try alternate parse
                            try {
                                Map<String, Object> r = client.parseResponse(resp);
                                if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                    loaded = (List<Map<String, Object>>) r.get("data");
                                } else {
                                    // If response is a single object or some other structure, gracefully produce empty list
                                    loaded = new ArrayList<>();
                                }
                            } catch (Exception ex2) {
                                throw ex; // original parse error
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
                setRefreshEnabled(true);
                if (error != null) {
                    showError("Failed to load customers: " + error.getMessage());
                    return;
                }
                customers.clear();
                customers.addAll(loaded);
                updateCustomerTable();
            }
        };
        worker.execute();
    }

    private void setRefreshEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> refreshBtn.setEnabled(enabled));
    }

    private void loadCustomerDetails(Integer customerId) {
        if (customerId == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Map<String, Object> customerData = null;
            private Exception error = null;

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/customers/" + customerId);
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            customerData = client.parseResponse(resp);
                        } catch (Exception ex) {
                            try {
                                Map<String, Object> r = client.parseResponse(resp);
                                if (r != null && r.containsKey("data") && r.get("data") instanceof Map) {
                                    customerData = (Map<String, Object>) r.get("data");
                                } else {
                                    customerData = r;
                                }
                            } catch (Exception ex2) {
                                throw ex;
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
                    showError("Failed to load customer details: " + error.getMessage());
                    return;
                }
                if (customerData != null) {
                    populateDetailsForm(customerData);
                }
            }
        };
        worker.execute();
    }

    /**
     * Load analytics for a single date.
     * If backend returns no data (empty/404/no-content or 400 with 'No analytics found'), show an INFO dialog:
     * "No customers for <date>" and set summary accordingly.
     */
    private void loadAnalyticsForDate() {
        final String dateStr = analyticsDateField.getText().trim();
        String effectiveDate = dateStr.isEmpty() ? LocalDate.now().format(dateFormatter) : dateStr;
        analyticsDateField.setText(effectiveDate);

        // Validate date format early
        try {
            LocalDate.parse(effectiveDate, dateFormatter);
        } catch (DateTimeParseException e) {
            showError("Invalid date format. Use yyyy-MM-dd");
            return;
        }

        setAnalyticsControlsEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private Map<String, Object> analytics = null;
            private boolean noData = false;
            private String dateForSummary = effectiveDate;

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/customers/analytics?date=" + dateForSummary);

                    if (resp == null || resp.trim().isEmpty()) {
                        // treat as "no data" for this date
                        noData = true;
                    } else {
                        try {
                            Map<String, Object> parsed = client.parseResponse(resp);
                            if (parsed == null || parsed.isEmpty()) {
                                noData = true;
                            } else {
                                // Some APIs wrap the payload in "data"
                                if (parsed.containsKey("data") && parsed.get("data") instanceof Map) {
                                    analytics = (Map<String, Object>) parsed.get("data");
                                } else {
                                    analytics = parsed;
                                }
                                if (analytics == null || analytics.isEmpty()) {
                                    noData = true;
                                } else {
                                    // Normalize fields
                                    analytics.putIfAbsent("analyticsDate", dateForSummary);
                                    analytics.putIfAbsent("totalCustomers", 0);
                                    analytics.putIfAbsent("newCustomers", 0);
                                    analytics.putIfAbsent("repeatCustomers", 0);
                                    analytics.putIfAbsent("creditCustomers", 0);
                                }
                            }
                        } catch (Exception ex) {
                            // Parsing error -> treat as no-data (single-date case)
                            noData = true;
                        }
                    }
                } catch (Exception ex) {
                    // If backend returned 400 with "No analytics found for date" or 404 / no data, treat as no analytics (do not show error dialog).
                    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                    if (msg.contains("400") && msg.contains("no analytics")) {
                        noData = true;
                    } else if (msg.contains("404") || msg.contains("not found") || msg.contains("no content")) {
                        noData = true;
                    } else {
                        // Other exceptions are real errors
                        error = ex;
                        ex.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                setAnalyticsControlsEnabled(true);

                if (error != null) {
                    showError("Failed to load analytics: " + error.getMessage());
                    return;
                }

                analyticsData.clear();
                analyticsModel.setRowCount(0);

                if (noData) {
                    // Show informational dialog (not error) for single-date no-data
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(CustomerPanel.this,
                                "No customers for " + dateForSummary,
                                "No Data",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
                    analyticsSummaryLabel.setText("No customers for " + dateForSummary);
                    return;
                }

                if (analytics != null) {
                    analyticsData.add(analytics);
                    updateAnalyticsTable();

                    Integer total = safeIntegerFromObject(analytics.get("totalCustomers"), 0);
                    Integer newCust = safeIntegerFromObject(analytics.get("newCustomers"), 0);
                    Integer repeat = safeIntegerFromObject(analytics.get("repeatCustomers"), 0);
                    Integer credit = safeIntegerFromObject(analytics.get("creditCustomers"), 0);

                    String summary = String.format(
                            "Date: %s | Total: %d | New: %d | Repeat: %d | Credit: %d",
                            dateForSummary,
                            total,
                            newCust,
                            repeat,
                            credit
                    );

                    analyticsSummaryLabel.setText(summary);
                } else {
                    analyticsSummaryLabel.setText("No analytics data for " + dateForSummary);
                }
            }
        };

        worker.execute();
    }



    private void loadAnalyticsForRange() {
        final String fromStr = fromDateField.getText().trim();
        final String toStr = toDateField.getText().trim();

        if (fromStr.isEmpty() || toStr.isEmpty()) {
            showError("Please enter both from and to dates");
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

        setAnalyticsControlsEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> analyticsList = new ArrayList<>();

            @Override
            protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/customers/analytics/range?from=" + fromStr + "&to=" + toStr);
                    if (resp == null || resp.trim().isEmpty()) {
                        analyticsList = new ArrayList<>();
                    } else {
                        try {
                            analyticsList = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            try {
                                Map<String, Object> r = client.parseResponse(resp);
                                if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                    analyticsList = (List<Map<String, Object>>) r.get("data");
                                } else {
                                    // Could be a single object -> wrap it
                                    if (r != null && !r.isEmpty()) {
                                        analyticsList = new ArrayList<>();
                                        analyticsList.add(r);
                                    } else {
                                        analyticsList = new ArrayList<>();
                                    }
                                }
                            } catch (Exception ex2) {
                                analyticsList = new ArrayList<>();
                            }
                        }
                    }
                } catch (Exception ex) {
                    // treat 404/no-content as empty list; other exceptions bubble up
                    String msg = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                    if (msg.contains("404") || msg.contains("not found") || msg.contains("no content")) {
                        analyticsList = new ArrayList<>();
                    } else {
                        error = ex;
                        ex.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                setAnalyticsControlsEnabled(true);
                if (error != null) {
                    showError("Failed to load analytics: " + error.getMessage());
                    return;
                }
                analyticsData.clear();
                analyticsModel.setRowCount(0);

                if (analyticsList != null && !analyticsList.isEmpty()) {
                    analyticsData.addAll(analyticsList);
                    updateAnalyticsTable();

                    // Calculate totals for summary
                    int totalCustomers = 0;
                    for (Map<String, Object> analytics : analyticsList) {
                        totalCustomers += safeIntegerFromObject(analytics.get("totalCustomers"), 0);
                    }

                    String summary = String.format(
                            "Range: %s to %s | Total Days: %d | Avg Total/Day: %.1f",
                            fromStr, toStr, analyticsList.size(),
                            (double) totalCustomers / analyticsList.size()
                    );
                    analyticsSummaryLabel.setText(summary);
                } else {
                    analyticsSummaryLabel.setText("No analytics data for range " + fromStr + " to " + toStr);
                }
            }
        };
        worker.execute();
    }

    private void loadAnalyticsForToday() {
        String today = LocalDate.now().format(dateFormatter);
        analyticsDateField.setText(today);
        loadAnalyticsForDate();
    }

    // Enable/disable analytics controls to prevent repeated clicks while a request runs
    private void setAnalyticsControlsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            fetchDateBtn.setEnabled(enabled);
            fetchRangeBtn.setEnabled(enabled);
            analyticsDateField.setEnabled(enabled);
            fromDateField.setEnabled(enabled);
            toDateField.setEnabled(enabled);
        });
    }

    // ---------- UI Updates ----------
    private void updateCustomerTable() {
        SwingUtilities.invokeLater(() -> {
            customerModel.setRowCount(0);
            for (Map<String, Object> customer : customers) {
                Integer id = safeIntegerFromObject(customer.get("id"), null);
                String name = Objects.toString(customer.get("name"), "Unknown");
                String phone = Objects.toString(customer.get("phone"), "N/A");
                Double creditLimit = safeDoubleFromObject(customer.get("creditLimit"));
                Boolean isActive = getBooleanValue(customer, "isActive", true);
                String createdAt = formatDate(Objects.toString(customer.get("createdAt"), ""));

                customerModel.addRow(new Object[]{
                        id,
                        name,
                        phone,
                        String.format("ksh %,.2f", creditLimit != null ? creditLimit : 0.0),
                        isActive ? "Active" : "Inactive",
                        createdAt.length() > 10 ? createdAt.substring(0, 10) : createdAt
                });
            }
        });
    }

    private void updateAnalyticsTable() {
        SwingUtilities.invokeLater(() -> {
            analyticsModel.setRowCount(0);
            for (Map<String, Object> analytics : analyticsData) {
                String date = Objects.toString(analytics.get("analyticsDate"), "N/A");
                Integer total = safeIntegerFromObject(analytics.get("totalCustomers"), 0);
                Integer newCust = safeIntegerFromObject(analytics.get("newCustomers"), 0);
                Integer repeat = safeIntegerFromObject(analytics.get("repeatCustomers"), 0);
                Integer credit = safeIntegerFromObject(analytics.get("creditCustomers"), 0);
                String createdAt = formatDate(Objects.toString(analytics.get("createdAt"), ""));

                analyticsModel.addRow(new Object[]{
                        date,
                        total,
                        newCust,
                        repeat,
                        credit,
                        createdAt.length() > 10 ? createdAt.substring(0, 10) : createdAt
                });
            }
        });
    }

    private void filterCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            customerSorter.setRowFilter(null);
        } else {
            try {
                RowFilter<DefaultTableModel, Object> rf = RowFilter.regexFilter("(?i)" + query, 1, 2); // Search name and phone columns
                customerSorter.setRowFilter(rf);
            } catch (Exception e) {
                // Invalid regex, ignore
            }
        }
    }

    private void populateDetailsForm(Map<String, Object> customer) {
        SwingUtilities.invokeLater(() -> {
            idField.setText(Objects.toString(customer.get("id"), ""));
            nameField.setText(Objects.toString(customer.get("name"), ""));
            phoneField.setText(Objects.toString(customer.get("phone"), ""));

            Double creditLimit = safeDoubleFromObject(customer.get("creditLimit"));
            creditLimitField.setText(creditLimit != null ? String.format("%.2f", creditLimit) : "0.00");

            Boolean isActive = getBooleanValue(customer, "isActive", true);
            activeCheckBox.setSelected(isActive);

            String createdAt = formatDate(Objects.toString(customer.get("createdAt"), ""));
            createdAtField.setText(createdAt);

            // Update button text based on customer status
            if (!isActive) {
                deleteButton.setText("Activate");
                deleteButton.setBackground(new Color(76, 175, 80));
            } else {
                deleteButton.setText("Deactivate");
                deleteButton.setBackground(new Color(244, 67, 54));
            }
        });
    }

    private void clearDetailsForm() {
        idField.setText("");
        nameField.setText("");
        phoneField.setText("");
        creditLimitField.setText("0.00");
        activeCheckBox.setSelected(true);
        createdAtField.setText("");
        deleteButton.setText("Deactivate");
        deleteButton.setBackground(new Color(244, 67, 54));
    }

    // ---------- Event Handlers ----------
    private void onCustomerSelected() {
        int selectedRow = customerTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = customerTable.convertRowIndexToModel(selectedRow);
            Integer customerId = safeIntegerFromObject(customerModel.getValueAt(modelRow, 0), null);
            if (customerId != null) {
                loadCustomerDetails(customerId);
            }
        }
    }

    private void saveCustomer() {
        String idText = idField.getText().trim();
        String name = nameField.getText().trim();
        String phone = phoneField.getText().trim();
        String creditLimitText = creditLimitField.getText().trim();

        // Validation
        if (name.isEmpty()) {
            showError("Customer name is required");
            return;
        }
        if (phone.isEmpty()) {
            showError("Phone number is required");
            return;
        }

        double creditLimit;
        try {
            creditLimit = Double.parseDouble(creditLimitText);
            if (creditLimit < 0) {
                showError("Credit limit cannot be negative");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid credit limit amount");
            return;
        }

        boolean isActive = activeCheckBox.isSelected();

        Map<String, Object> customerData = new HashMap<>();
        // Build a full payload to be safe (controller expects full Customer)
        if (!idText.isEmpty()) {
            try {
                customerData.put("id", Integer.parseInt(idText));
            } catch (Exception ignored) { }
        }
        customerData.put("name", name);
        customerData.put("phone", phone);
        customerData.put("creditLimit", creditLimit);
        customerData.put("isActive", isActive);

        // preserve createdAt if present in form; convert to ISO LocalDateTime string
        String createdAtRaw = createdAtField.getText().trim();
        String isoCreatedAt = toIsoDateTimeString(createdAtRaw);
        if (isoCreatedAt != null) customerData.put("createdAt", isoCreatedAt);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";

            @Override
            protected Void doInBackground() {
                try {
                    String resp;
                    if (idText.isEmpty()) {
                        // Create new customer - NOT allowed from this panel
                        message = "New customers are auto-created through sales. Use Sales panel to create customers.";
                        success = false;
                    } else {
                        // Update existing customer (PUT) with the full payload
                        Integer customerId = Integer.parseInt(idText);
                        resp = client.put("/api/secure/customers/" + customerId, customerData);
                        success = client.isResponseSuccessful(resp);
                        message = client.getResponseMessage(resp);
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
                try {
                    // If doInBackground() completed without throwing, the update succeeded
                    get(); // forces exception propagation if something actually failed

                    String display = nonEmptyMessage(message, "Customer updated successfully.");
                    JOptionPane.showMessageDialog(
                            CustomerPanel.this,
                            display,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    loadCustomers(); // Refresh list

                    try {
                        Integer id = Integer.parseInt(idField.getText().trim());
                        loadCustomerDetails(id);
                    } catch (Exception ignored) {}

                } catch (Exception ex) {
                    // This only runs if HTTP request truly failed (non-2xx or parsing error)
                    String errDisplay = nonEmptyMessage(
                            message,
                            "Failed to update customer. See server logs for details."
                    );

                    JOptionPane.showMessageDialog(
                            CustomerPanel.this,
                            errDisplay,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        };
        worker.execute();
    }

    /**
     * Toggle activation/deactivation:
     * As you specified — we call PUT /api/secure/customers/{id} and send the full customer payload
     * with isActive set to true/false depending on desired state.
     *
     * We'll prefer to read the existing full record from the server; if that fails we build the payload
     * from the fields currently shown in the details form (id, name, phone, creditLimit, createdAt).
     */
    private void toggleActivationForCustomer() {
        String idText = idField.getText().trim();
        if (idText.isEmpty()) {
            showError("No customer selected");
            return;
        }

        boolean currentlyActive = activeCheckBox.isSelected();
        boolean desiredActive = !currentlyActive; // toggle
        String action = desiredActive ? "activate" : "deactivate";

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + action + " this customer?",
                "Confirm",
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
                    Integer customerId = Integer.parseInt(idText);

                    // Try to GET full customer from server first
                    Map<String, Object> fullCustomer = null;
                    try {
                        String getResp = client.get("/api/secure/customers/" + customerId);
                        if (getResp != null && !getResp.trim().isEmpty()) {
                            Map<String, Object> parsed = client.parseResponse(getResp);
                            if (parsed != null) {
                                if (parsed.containsKey("data") && parsed.get("data") instanceof Map) {
                                    fullCustomer = (Map<String, Object>) parsed.get("data");
                                } else {
                                    fullCustomer = parsed;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // ignore - we'll build from UI fields below
                        fullCustomer = null;
                    }

                    if (fullCustomer == null) {
                        // Build payload from UI fields (ensures required fields exist)
                        fullCustomer = new HashMap<>();
                        fullCustomer.put("id", customerId);
                        fullCustomer.put("name", nameField.getText().trim());
                        fullCustomer.put("phone", phoneField.getText().trim());

                        Double creditLimit = safeDoubleFromObject(creditLimitField.getText().trim());
                        fullCustomer.put("creditLimit", creditLimit != null ? creditLimit : 0.0);

                        // createdAt: try to use displayed createdAt, otherwise current timestamp
                        String createdAtRaw = createdAtField.getText().trim();
                        String iso = toIsoDateTimeString(createdAtRaw);
                        if (iso == null) {
                            iso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        }
                        fullCustomer.put("createdAt", iso);
                    } else {
                        // Ensure createdAt (if present) is normalized to ISO format before sending
                        Object ca = fullCustomer.get("createdAt");
                        if (ca != null) {
                            String iso = toIsoDateTimeString(ca.toString());
                            if (iso != null) fullCustomer.put("createdAt", iso);
                        }
                    }

                    // Set desired active state
                    fullCustomer.put("isActive", desiredActive);

                    // Ensure id present and correct
                    fullCustomer.put("id", customerId);

                    // Call PUT with full payload (controller expects full Customer)
                    String putResp = client.put("/api/secure/customers/" + customerId, fullCustomer);
                    success = client.isResponseSuccessful(putResp);
                    message = client.getResponseMessage(putResp);

                } catch (Exception e) {
                    success = false;
                    message = "Error: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                String successDefault = "Customer status updated successfully.";
                String errorDefault = "Failed to update customer status. See server logs for details.";

                try {
                    // Forces exception propagation if doInBackground() failed
                    get();

                    String display = nonEmptyMessage(message, successDefault);
                    JOptionPane.showMessageDialog(
                            CustomerPanel.this,
                            display,
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    loadCustomers();

                    // Reload details so UI reflects new active/inactive state
                    try {
                        Integer id = Integer.parseInt(idText);
                        loadCustomerDetails(id);
                    } catch (Exception ignored) {}

                } catch (Exception ex) {
                    String errDisplay = nonEmptyMessage(message, errorDefault);
                    JOptionPane.showMessageDialog(
                            CustomerPanel.this,
                            errDisplay,
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

        };
        worker.execute();
    }

    // ---------- Helper Methods ----------
    private String formatDate(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return "";
        }

        // Try a few reasonable ISO parsing strategies and fall back to raw string
        try {
            // If it's an ISO datetime: "2026-01-19T10:45:30" or "2026-01-19T10:45:30Z"
            if (dateString.contains("T")) {
                String base = dateString;
                // strip trailing 'Z' if present
                if (base.endsWith("Z")) base = base.substring(0, base.length() - 1);

                // If there's at least "yyyy-MM-ddTHH:mm:ss" (19 chars), parse LocalDateTime
                if (base.length() >= 19) {
                    try {
                        LocalDateTime ldt = LocalDateTime.parse(base.substring(0, 19));
                        return ldt.format(displayFormatter);
                    } catch (DateTimeParseException ignore) {
                        // fallback below
                    }
                }

                // fallback to just date part
                if (base.length() >= 10) {
                    LocalDate ld = LocalDate.parse(base.substring(0, 10));
                    return ld.format(dateFormatter);
                }
            } else {
                // plain date like "2026-01-19"
                if (dateString.length() >= 10) {
                    LocalDate ld = LocalDate.parse(dateString.substring(0, 10));
                    return ld.format(dateFormatter);
                }

                // also tolerate "yyyy-MM-dd HH:mm[:ss]"
                try {
                    DateTimeFormatter f1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                    LocalDateTime ldt = LocalDateTime.parse(dateString, f1);
                    return ldt.format(displayFormatter);
                } catch (Exception ignored) {}

                try {
                    DateTimeFormatter f2 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime ldt = LocalDateTime.parse(dateString, f2);
                    return ldt.format(displayFormatter);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
            // swallow and fall through to return original
        }
        return dateString;
    }

    /**
     * Convert a user/display datetime string into ISO local datetime string acceptable to server
     * (e.g. 2026-01-19T11:21:00). Returns null if input is empty/invalid.
     *
     * Accepts:
     * - yyyy-MM-dd'T'HH:mm:ss
     * - yyyy-MM-dd HH:mm:ss
     * - yyyy-MM-dd HH:mm
     * - yyyy-MM-dd
     */
    private String toIsoDateTimeString(String input) {
        if (input == null) return null;
        input = input.trim();
        if (input.isEmpty()) return null;

        // If already in ISO local format with T, try parse directly (and ensure seconds)
        try {
            if (input.contains("T")) {
                LocalDateTime ldt = LocalDateTime.parse(input);
                return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (Exception ignored) {}

        // Try "yyyy-MM-dd HH:mm:ss"
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime ldt = LocalDateTime.parse(input, f);
            return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        // Try "yyyy-MM-dd HH:mm"
        try {
            DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(input, f);
            // append seconds = 00
            return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        // Try plain date "yyyy-MM-dd"
        try {
            LocalDate ld = LocalDate.parse(input, dateFormatter);
            LocalDateTime ldt = ld.atStartOfDay();
            return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}

        // If all parsing fails, return null to avoid sending invalid string
        return null;
    }

    /**
     * Return a non-empty display message: use `msg` if non-empty, otherwise fallback to `fallback`.
     */
    private String nonEmptyMessage(String msg, String fallback) {
        if (msg == null) return fallback;
        String t = msg.trim();
        return t.isEmpty() ? fallback : t;
    }

    private Boolean getBooleanValue(Map<String, Object> map, String key, Boolean defaultValue) {
        if (map == null || !map.containsKey(key)) return defaultValue;
        Object v = map.get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof String) return Boolean.parseBoolean((String) v);
        if (v instanceof Number) return ((Number) v).intValue() != 0;
        return defaultValue;
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

    private Integer safeIntegerFromObject(Object o) {
        return safeIntegerFromObject(o, null);
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
}
