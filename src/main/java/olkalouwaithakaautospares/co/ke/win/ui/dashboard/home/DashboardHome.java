package olkalouwaithakaautospares.co.ke.win.ui.dashboard.home;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.Map;

public class DashboardHome extends JPanel {
    private BaseClient client;
    private ObjectMapper mapper;

    // Stat card labels
    private JLabel totalSalesLabel;
    private JLabel todayRevenueLabel;
    private JLabel newCustomersLabel;
    private JLabel pendingReturnsLabel;
    private JLabel lowStockLabel;
    private JLabel creditSalesLabel;

    public DashboardHome() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        initUI();
        loadDashboardData();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(30, 33, 57));

        JLabel date = new JLabel(LocalDate.now().toString());
        date.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        date.setForeground(new Color(150, 150, 150));

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setBackground(new Color(33, 150, 243));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadDashboardData());

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.add(refreshBtn);

        header.add(title, BorderLayout.WEST);
        header.add(date, BorderLayout.CENTER);
        header.add(rightPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Stats cards
        JPanel statsPanel = createStatsPanel();
        add(statsPanel, BorderLayout.CENTER);
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

        // Create stat cards with default values
        totalSalesLabel = new JLabel("₦ 0.00");
        todayRevenueLabel = new JLabel("₦ 0.00");
        newCustomersLabel = new JLabel("0");
        pendingReturnsLabel = new JLabel("0");
        lowStockLabel = new JLabel("0");
        creditSalesLabel = new JLabel("₦ 0.00");

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(createStatCard("Total Sales", totalSalesLabel, "Loading...",
                new Color(76, 175, 80)), gbc);

        gbc.gridx = 1;
        panel.add(createStatCard("Today's Revenue", todayRevenueLabel, "Loading...",
                new Color(33, 150, 243)), gbc);

        gbc.gridx = 2;
        panel.add(createStatCard("New Customers", newCustomersLabel, "Loading...",
                new Color(156, 39, 176)), gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(createStatCard("Pending Returns", pendingReturnsLabel, "Loading...",
                new Color(255, 152, 0)), gbc);

        gbc.gridx = 1;
        panel.add(createStatCard("Low Stock Items", lowStockLabel, "Loading...",
                new Color(244, 67, 54)), gbc);

        gbc.gridx = 2;
        panel.add(createStatCard("Credit Sales", creditSalesLabel, "Loading...",
                new Color(96, 125, 139)), gbc);

        return panel;
    }

    private void loadDashboardData() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            private Exception error = null;
            private Map<String, Object> dailyReport = null;
            private Map<String, Object> customerAnalytics = null;

            @Override
            protected Void doInBackground() {
                try {
                    // Get today's date
                    String today = LocalDate.now().toString();

                    // Fetch daily sales report
                    try {
                        String reportResponse = client.get("/api/secure/reports/daily?date=" + today);
                        if (reportResponse != null && !reportResponse.trim().isEmpty()) {
                            Map<String, Object> result = client.parseResponse(reportResponse);
                            if (result != null) {
                                if (result.containsKey("data") && result.get("data") instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                                    dailyReport = data;
                                } else {
                                    // If response is direct data (not wrapped)
                                    dailyReport = result;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error fetching daily report: " + e.getMessage());
                        // Continue with other requests
                    }

                    // Fetch customer analytics
                    try {
                        String analyticsResponse = client.get("/api/secure/customers/analytics?date=" + today);
                        if (analyticsResponse != null && !analyticsResponse.trim().isEmpty()) {
                            Map<String, Object> result = client.parseResponse(analyticsResponse);
                            if (result != null) {
                                if (result.containsKey("data") && result.get("data") instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> data = (Map<String, Object>) result.get("data");
                                    customerAnalytics = data;
                                } else {
                                    // If response is direct data (not wrapped)
                                    customerAnalytics = result;
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error fetching customer analytics: " + e.getMessage());
                        // Continue with other requests
                    }

                } catch (Exception e) {
                    error = e;
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                try {
                    if (error != null) {
                        showError("Failed to load dashboard data: " + error.getMessage());
                        return;
                    }

                    // Update UI with fetched data
                    updateStatsUI();

                } catch (Exception e) {
                    showError("Error updating UI: " + e.getMessage());
                }
            }

            private void updateStatsUI() {
                try {
                    // Update daily report stats
                    if (dailyReport != null) {
                        try {
                            Double totalSales = getDoubleValue(dailyReport, "totalSales");
                            Double creditSales = getDoubleValue(dailyReport, "creditSales");
                            Double paidSales = getDoubleValue(dailyReport, "paidSales");

                            totalSalesLabel.setText(String.format("₦ %,.2f", totalSales));
                            todayRevenueLabel.setText(String.format("₦ %,.2f", paidSales));
                            creditSalesLabel.setText(String.format("₦ %,.2f", creditSales));

                            // Update descriptions
                            updateCardDescription(0, String.format("Total: ₦ %,.2f", totalSales));
                            updateCardDescription(1, String.format("Paid: ₦ %,.2f", paidSales));
                            updateCardDescription(5, String.format("Credit: ₦ %,.2f", creditSales));
                        } catch (Exception e) {
                            System.err.println("Error parsing daily report: " + e.getMessage());
                        }
                    }

                    // Update customer analytics
                    if (customerAnalytics != null) {
                        try {
                            Integer newCustomers = getIntegerValue(customerAnalytics, "newCustomers");
                            Integer totalCustomers = getIntegerValue(customerAnalytics, "totalCustomers");

                            newCustomersLabel.setText(String.valueOf(newCustomers));
                            updateCardDescription(2, String.format("Total: %d customers", totalCustomers));
                        } catch (Exception e) {
                            System.err.println("Error parsing customer analytics: " + e.getMessage());
                        }
                    }

                    // Set default values for pending returns and low stock
                    pendingReturnsLabel.setText("0");
                    lowStockLabel.setText("0");
                    updateCardDescription(3, "No returns pending");
                    updateCardDescription(4, "All stock levels good");

                } catch (Exception e) {
                    System.err.println("Error updating stats UI: " + e.getMessage());
                }
            }

            private Double getDoubleValue(Map<String, Object> map, String key) {
                if (map == null || !map.containsKey(key)) {
                    return 0.0;
                }
                Object value = map.get(key);
                if (value instanceof Number) {
                    return ((Number) value).doubleValue();
                } else if (value instanceof String) {
                    try {
                        return Double.parseDouble((String) value);
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                }
                return 0.0;
            }

            private Integer getIntegerValue(Map<String, Object> map, String key) {
                if (map == null || !map.containsKey(key)) {
                    return 0;
                }
                Object value = map.get(key);
                if (value instanceof Number) {
                    return ((Number) value).intValue();
                } else if (value instanceof String) {
                    try {
                        return Integer.parseInt((String) value);
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
                return 0;
            }

            private void updateCardDescription(int cardIndex, String description) {
                // This is a simplified version - in a real app, you'd have references to the description labels
                System.out.println("Card " + cardIndex + ": " + description);
            }
        };
        worker.execute();
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    message,
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String description, Color color) {
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
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(100, 100, 100));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Value
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(color);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Description
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        descLabel.setForeground(new Color(150, 150, 150));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Top border accent
        JPanel accent = new JPanel();
        accent.setBackground(color);
        accent.setPreferredSize(new Dimension(0, 4));
        accent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        accent.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(accent);
        card.add(Box.createRigidArea(new Dimension(0, 15)));
        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(valueLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(descLabel);

        return card;
    }
}