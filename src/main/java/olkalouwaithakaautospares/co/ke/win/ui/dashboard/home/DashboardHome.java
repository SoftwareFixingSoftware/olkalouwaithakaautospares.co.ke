package olkalouwaithakaautospares.co.ke.win.ui.dashboard.home;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class DashboardHome extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    // Stat card labels
    private JLabel totalSalesLabel;
    private JLabel newCustomersLabel;
    private JLabel pendingReturnsLabel;
    private JLabel lowStockLabel;
    private JLabel creditSalesLabel;

    // Description labels
    private JLabel totalSalesDesc;
    private JLabel newCustomersDesc;
    private JLabel pendingReturnsDesc;
    private JLabel lowStockDesc;
    private JLabel creditSalesDesc;

    // Refresh button
    private JButton refreshBtn;

    // Date formatter (consistent with Reports)
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Data caches (like in Reports panel)
    private final List<Map<String, Object>> salesData = new ArrayList<>();
    private final List<Map<String, Object>> returnsData = new ArrayList<>();
    private final List<Map<String, Object>> productsData = new ArrayList<>();
    private final List<Map<String, Object>> stockBatchesData = new ArrayList<>();
    private final List<Map<String, Object>> customersData = new ArrayList<>();

    public DashboardHome() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();

        // Load once when the dashboard is first displayed (no periodic auto-refresh).
        // Subsequent refreshes happen only when the user clicks the Refresh button.
        SwingUtilities.invokeLater(this::loadDashboardData);
    }

    // ---------- UI Initialization ----------
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        // Stats cards
        JPanel statsPanel = createStatsPanel();
        add(statsPanel, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Dashboard Overview    ");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(30, 33, 57));

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
        JLabel date = new JLabel(today);
        date.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        date.setForeground(new Color(150, 150, 150));

        refreshBtn = new JButton("Refresh");
        styleButton(refreshBtn, new Color(33, 150, 243));
        refreshBtn.addActionListener(e -> loadDashboardData());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.add(refreshBtn);

        header.add(title, BorderLayout.WEST);
        header.add(date, BorderLayout.CENTER);
        header.add(rightPanel, BorderLayout.EAST);

        return header;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 247, 250));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // Create stat cards with loading state
        totalSalesLabel = new JLabel("ksh 0.00");
        totalSalesLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        totalSalesDesc = new JLabel("Loading...");

        newCustomersLabel = new JLabel("0");
        newCustomersLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        newCustomersDesc = new JLabel("Loading...");

        pendingReturnsLabel = new JLabel("0");
        pendingReturnsLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        pendingReturnsDesc = new JLabel("Loading...");

        lowStockLabel = new JLabel("0");
        lowStockLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lowStockDesc = new JLabel("Loading...");

        creditSalesLabel = new JLabel("ksh 0.00");
        creditSalesLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        creditSalesDesc = new JLabel("Loading...");

        // Layout: 3 columns x 2 rows (last cell unused)
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStatCard("Total Sales", totalSalesLabel, totalSalesDesc, new Color(76, 175, 80)), gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(createStatCard("New Customers", newCustomersLabel, newCustomersDesc, new Color(156, 39, 176)), gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        panel.add(createStatCard("Pending Returns", pendingReturnsLabel, pendingReturnsDesc, new Color(255, 152, 0)), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createStatCard("Low Stock Items", lowStockLabel, lowStockDesc, new Color(244, 67, 54)), gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(createStatCard("Credit Sales", creditSalesLabel, creditSalesDesc, new Color(96, 125, 139)), gbc);

        return panel;
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

    // ---------- Data Loading Methods (Reports-style) ----------
    private void loadDashboardData() {
        // prevent double-click starting duplicate loads
        if (!refreshBtn.isEnabled()) return;

        refreshBtn.setEnabled(false);
        refreshBtn.setText("Loading...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private DashboardStats stats = new DashboardStats();

            @Override
            protected Void doInBackground() {
                try {
                    loadAllData();
                    calculateStats();

                    // Attempt to load the daily report for today (same source used in ReportingPanel)
                    String today = LocalDate.now().format(dateFormatter);
                    try {
                        Map<String, Object> dailyReport = loadDailyReport(today);
                        if (dailyReport != null && !dailyReport.isEmpty()) {
                            // If report provides totalSales we show it as authoritative for the day's total
                            if (dailyReport.containsKey("totalSales") && dailyReport.get("totalSales") != null) {
                                stats.totalSales = safeDouble(dailyReport.get("totalSales"), stats.totalSales);
                            }
                            // If report provides credit totals, keep override behavior
                            if (dailyReport.containsKey("creditSales") && dailyReport.get("creditSales") != null) {
                                stats.creditSales = safeDouble(dailyReport.get("creditSales"), stats.creditSales);
                            }
                            if (dailyReport.containsKey("creditTransactions") && dailyReport.get("creditTransactions") != null) {
                                stats.creditTransactions = safeInteger(dailyReport.get("creditTransactions"), stats.creditTransactions);
                            }
                        }
                    } catch (Exception ex) {
                        // keep computed values if report call fails
                        ex.printStackTrace();
                    }
                } catch (Exception e) {
                    error = e;
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                SwingUtilities.invokeLater(() -> {
                    refreshBtn.setEnabled(true);
                    refreshBtn.setText("Refresh");

                    if (error != null) {
                        showError("Failed to load dashboard data: " + error.getMessage());
                        return;
                    }

                    updateDashboardUI(stats);
                });
            }

            private void loadAllData() throws Exception {
                // Clear previous data
                salesData.clear();
                returnsData.clear();
                productsData.clear();
                stockBatchesData.clear();
                customersData.clear();

                // Load all data in sequence
                salesData.addAll(loadSalesData());
                returnsData.addAll(loadReturnsData());
                productsData.addAll(loadProductsData());
                stockBatchesData.addAll(loadStockBatchesData());
                customersData.addAll(loadCustomersData());
            }

            private List<Map<String, Object>> loadSalesData() throws Exception {
                return loadDataList("/api/secure/sales");
            }

            private List<Map<String, Object>> loadReturnsData() throws Exception {
                return loadDataList("/api/secure/returns");
            }

            private List<Map<String, Object>> loadProductsData() throws Exception {
                return loadDataList("/api/secure/products");
            }

            private List<Map<String, Object>> loadStockBatchesData() throws Exception {
                return loadDataList("/api/secure/stock-batches");
            }

            private List<Map<String, Object>> loadCustomersData() throws Exception {
                return loadDataList("/api/secure/customers");
            }

            @SuppressWarnings("unchecked")
            private List<Map<String, Object>> loadDataList(String endpoint) throws Exception {
                String response = client.get(endpoint);
                if (response != null && !response.trim().isEmpty()) {
                    try {
                        // try parse as direct list
                        return mapper.readValue(response, new TypeReference<List<Map<String, Object>>>() {});
                    } catch (Exception ex) {
                        // parse wrapped { "data": [...] } response
                        Map<String, Object> parsed = mapper.readValue(response, new TypeReference<Map<String, Object>>() {});
                        if (parsed != null && parsed.containsKey("data") && parsed.get("data") instanceof List) {
                            return (List<Map<String, Object>>) parsed.get("data");
                        }
                        return new ArrayList<>();
                    }
                }
                return new ArrayList<>();
            }

            private void calculateStats() {
                String today = LocalDate.now().format(dateFormatter);

                // Reset stats
                stats = new DashboardStats();

                // Define payment statuses that should be treated as "credit/pending"
                Set<String> creditStatuses = new HashSet<>(Arrays.asList(
                        "CREDIT", "PENDING", "PARTIAL", "PARTIALLY_PAID", "OWING"
                ));

                // Calculate sales statistics (keeps existing behavior but totalSales may be overridden by report)
                for (Map<String, Object> sale : salesData) {
                    Double total = safeDouble(sale.get("totalAmount"));
                    if (total == null) continue;

                    stats.totalSales += total;

                    String saleDate = safeString(sale.get("saleDate"));
                    String paymentStatus = safeString(sale.get("paymentStatus"));

                    // If the sale happened today, add to today's counters (note: Today revenue card removed)
                    if (saleDate != null && saleDate.contains(today)) {
                        stats.todaySalesCount++;

                        // If today's sale is a credit/pending, include in today's credit totals
                        if (paymentStatus != null && creditStatuses.contains(paymentStatus.toUpperCase())) {
                            stats.creditSales += total;
                            stats.creditTransactions++;
                        }
                    }
                }

                // Calculate returns statistics
                for (Map<String, Object> ret : returnsData) {
                    String status = safeString(ret.get("status"));
                    if ("PENDING".equalsIgnoreCase(status)) {
                        stats.pendingReturns++;
                    }
                }

                // Calculate low stock items
                for (Map<String, Object> product : productsData) {
                    Integer prodId = safeInteger(product.get("id"));
                    Integer reorderLevel = safeInteger(product.get("reorderLevel"), 0);

                    int stock = calculateProductStock(prodId);
                    if (stock <= reorderLevel && stock > 0) {
                        stats.lowStockItems++;
                    }
                }

                // Calculate new customers
                for (Map<String, Object> customer : customersData) {
                    String createdAt = safeString(customer.get("createdAt"));
                    if (createdAt != null && createdAt.contains(today)) {
                        stats.newCustomers++;
                    }
                }

                // NOTE: We intentionally do NOT compute today's revenue here (card removed). Credit totals
                // will be overridden by daily reports when available (handled in doInBackground override above).
            }

            private int calculateProductStock(Integer productId) {
                int stock = 0;
                for (Map<String, Object> batch : stockBatchesData) {
                    if (safeInteger(batch.get("productId")).equals(productId)) {
                        stock += safeInteger(batch.get("quantityRemaining"), 0);
                    }
                }
                return stock;
            }
        };

        worker.execute();
    }

    // ---------- UI Update Methods ----------
    private void updateDashboardUI(DashboardStats stats) {
        DecimalFormat df = new DecimalFormat("#,##0.00");

        // Update value labels
        totalSalesLabel.setText("ksh " + df.format(stats.totalSales));
        newCustomersLabel.setText(String.valueOf(stats.newCustomers));
        pendingReturnsLabel.setText(String.valueOf(stats.pendingReturns));
        lowStockLabel.setText(String.valueOf(stats.lowStockItems));
        creditSalesLabel.setText("ksh " + df.format(stats.creditSales));

        // Update description labels
        totalSalesDesc.setText(String.format("%d transactions", salesData.size()));
        newCustomersDesc.setText(String.format("%d total customers", customersData.size()));
        pendingReturnsDesc.setText(String.format("%d total returns", returnsData.size()));
        lowStockDesc.setText(String.format("%d total products", productsData.size()));
        creditSalesDesc.setText(String.format("%d credit transactions", stats.creditTransactions));

        // Color coding for alerts
        if (stats.lowStockItems > 0) {
            lowStockLabel.setForeground(new Color(244, 67, 54));
            lowStockLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        } else {
            lowStockLabel.setForeground(new Color(96, 125, 139));
            lowStockLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        }

        if (stats.pendingReturns > 0) {
            pendingReturnsLabel.setForeground(new Color(255, 152, 0));
            pendingReturnsLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        } else {
            pendingReturnsLabel.setForeground(new Color(96, 125, 139));
            pendingReturnsLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        }
    }

    // ---------- Helper Classes ----------
    private static class DashboardStats {
        double totalSales = 0.0;
        double todayRevenue = 0.0; // kept for compatibility but not displayed
        double creditSales = 0.0;
        int newCustomers = 0;
        int pendingReturns = 0;
        int lowStockItems = 0;
        int todaySalesCount = 0;
        int creditTransactions = 0;
    }

    // ---------- Helper Methods (consistent with Reports) ----------
    private JPanel createStatCard(String title, JLabel valueLabel, JLabel descriptionLabel, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Title
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(new Color(80, 80, 80));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Top border accent
        JPanel accent = new JPanel();
        accent.setBackground(color);
        accent.setPreferredSize(new Dimension(0, 4));
        accent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        accent.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(accent);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(valueLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(descriptionLabel);

        return card;
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE)
        );
    }

    // ---------- Safe Data Extraction Methods (consistent with Reports) ----------
    private Double safeDouble(Object o) {
        return safeDouble(o, 0.0);
    }

    private Double safeDouble(Object o, Double defaultValue) {
        if (o == null) return defaultValue;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try {
            return Double.parseDouble(o.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Integer safeInteger(Object o) {
        return safeInteger(o, 0);
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

    private String safeString(Object o) {
        if (o == null) return "";
        return o.toString();
    }

    /**
     * Loads a single-day report from the Reports API and returns the map.
     * Supports both direct object response and wrapped { "data": { ... } } responses.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> loadDailyReport(String date) throws Exception {
        String resp = client.get("/api/secure/reports/daily?date=" + date);
        if (resp == null || resp.trim().isEmpty()) return Collections.emptyMap();

        try {
            // Try parse as direct Map
            Map<String, Object> parsed = mapper.readValue(resp, new TypeReference<Map<String, Object>>() {});
            if (parsed == null) return Collections.emptyMap();

            // If wrapped in { "data": { ... } }
            Object data = parsed.get("data");
            if (data instanceof Map) {
                return (Map<String, Object>) data;
            }

            // Otherwise assume parsed itself is the report
            return parsed;
        } catch (Exception ex) {
            // fallback: try to use client's parsing helper if available
            try {
                Map<String, Object> alt = client.parseResponse(resp);
                if (alt != null && alt.containsKey("data") && alt.get("data") instanceof Map) {
                    return (Map<String, Object>) alt.get("data");
                }
                if (alt != null) return alt;
            } catch (Exception e) {
                // give up
            }
            throw ex;
        }
    }
}
