package olkalouwaithakaautospares.co.ke.win.ui.dashboard.reports;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * EmailReportsPanel - Send reports via email (daily, weekly, monthly, custom)
 * Works with existing ReportController endpoints
 *
 * NOTE: "Include Report Sections" removed. Reports include the standard sections by default.
 */
public class EmailReportsPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

    // UI Components
    private JComboBox<String> reportTypeCombo;
    private JTextField emailField;
    private JTextField dateField;
    private JTextField fromField;
    private JTextField toField;
    private JTextArea statusArea;
    private JButton sendButton;
    private JButton previewButton;
    private JLabel dateLabel;
    private JPanel customRangePanel;

    public EmailReportsPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();
        // Ensure visibility and formatting reflect the initial selection
        updateDateFieldsVisibility();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Main panel with vertical arrangement
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);

        // Title
        JLabel titleLabel = new JLabel("Email Reports");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(new Color(30, 33, 57));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // Configuration panel
        JPanel configPanel = createConfigPanel();
        configPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Button panel
        JPanel buttonPanel = createButtonPanel();
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Status panel
        JPanel statusPanel = createStatusPanel();
        statusPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Add all components
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(configPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        // NOTE: removed sections panel (report sections are included by default)
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(statusPanel);

        // Scroll pane if needed
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                "Report Configuration",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                new Color(60, 60, 60)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Row 0: Email Address
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        panel.add(new JLabel("Recipient Email:"), gbc);

        emailField = new JTextField(30);
        emailField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        // Try to get user email from session (safe)
        String userEmail = "";
        try {
            Map<String, Object> currentUser = session.getUserData();
            if (currentUser != null && currentUser.containsKey("email")) {
                Object e = currentUser.get("email");
                if (e != null) userEmail = String.valueOf(e);
            }
        } catch (Exception ignored) {
        }
        emailField.setText(userEmail);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        panel.add(emailField, gbc);

        // Row 1: Report Type
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        panel.add(new JLabel("Report Type:"), gbc);

        String[] reportTypes = {"DAILY", "WEEKLY", "MONTHLY", "CUSTOM"};
        reportTypeCombo = new JComboBox<>(reportTypes);
        reportTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reportTypeCombo.addActionListener(e -> updateDateFieldsVisibility());
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        panel.add(reportTypeCombo, gbc);

        // Row 2: Date field (for DAILY, WEEKLY, MONTHLY)
        dateLabel = new JLabel("Date:");
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        panel.add(dateLabel, gbc);

        dateField = new JTextField(LocalDate.now().format(dateFormatter));
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dateField.setColumns(15);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        panel.add(dateField, gbc);

        // Row 3: Custom Range Fields (initially hidden)
        customRangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        customRangePanel.setOpaque(false);
        customRangePanel.setVisible(false);

        fromField = new JTextField(LocalDate.now().minusDays(7).format(dateFormatter));
        fromField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fromField.setColumns(10);

        toField = new JTextField(LocalDate.now().format(dateFormatter));
        toField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        toField.setColumns(10);

        customRangePanel.add(new JLabel("From:"));
        customRangePanel.add(fromField);
        customRangePanel.add(new JLabel("To:"));
        customRangePanel.add(toField);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        panel.add(customRangePanel, gbc);

        // Row 4: Quick Date Buttons
        JPanel quickDatePanel = createQuickDatePanel();
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1.0;
        panel.add(quickDatePanel, gbc);

        return panel;
    }

    private JPanel createQuickDatePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);

        JButton todayBtn = createQuickButton("Today");
        JButton yesterdayBtn = createQuickButton("Yesterday");
        JButton thisWeekBtn = createQuickButton("This Week");
        JButton thisMonthBtn = createQuickButton("This Month");
        JButton lastWeekBtn = createQuickButton("Last Week");
        JButton lastMonthBtn = createQuickButton("Last Month");

        todayBtn.addActionListener(e -> setQuickDate("today"));
        yesterdayBtn.addActionListener(e -> setQuickDate("yesterday"));
        thisWeekBtn.addActionListener(e -> setQuickDate("thisWeek"));
        thisMonthBtn.addActionListener(e -> setQuickDate("thisMonth"));
        lastWeekBtn.addActionListener(e -> setQuickDate("lastWeek"));
        lastMonthBtn.addActionListener(e -> setQuickDate("lastMonth"));

        panel.add(new JLabel("Quick Select:"));
        panel.add(todayBtn);
        panel.add(yesterdayBtn);
        panel.add(thisWeekBtn);
        panel.add(thisMonthBtn);
        panel.add(lastWeekBtn);
        panel.add(lastMonthBtn);

        return panel;
    }

    private JButton createQuickButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        button.setBackground(new Color(240, 240, 240));
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(3, 8, 3, 8)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        return button;
    }

    private void setQuickDate(String period) {
        LocalDate today = LocalDate.now();
        String reportType = (String) reportTypeCombo.getSelectedItem();

        if (!"today".equals(period)) {
            // fall back to logic for other buttons
            applyNonTodayQuickDate(period);
            return;
        }

        // 🔒 HARD GUARANTEE: "Today" always means today
        if ("CUSTOM".equals(reportType)) {
            fromField.setText(today.format(dateFormatter));
            toField.setText(today.format(dateFormatter));
            return;
        }

        if ("MONTHLY".equals(reportType)) {
            // When user explicitly clicks Today while MONTHLY is selected,
            // keep it simple: show today's date as the start (user might use this as explicit start)
            dateField.setText(today.format(dateFormatter));
            return;
        }

        if ("WEEKLY".equals(reportType)) {
            // Weekly → show today's date as the start (guarantee: Today is today's date)
            dateField.setText(today.format(dateFormatter));
            return;
        }

        // DAILY (default & most important)
        dateField.setText(today.format(dateFormatter));
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panel.setBackground(Color.WHITE);

        previewButton = new JButton("Preview Report");
        styleButton(previewButton, new Color(33, 150, 243)); // Blue
        previewButton.addActionListener(e -> previewReport());

        sendButton = new JButton("Send Report");
        styleButton(sendButton, new Color(46, 125, 50)); // Green
        sendButton.addActionListener(e -> sendReportEmail());

        JButton clearButton = new JButton("Clear Form");
        styleButton(clearButton, new Color(158, 158, 158)); // Gray
        clearButton.addActionListener(e -> clearForm());

        panel.add(previewButton);
        panel.add(sendButton);
        panel.add(clearButton);

        return panel;
    }

    private void styleButton(JButton button, Color color) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setPreferredSize(new Dimension(180, 40));
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        statusArea = new JTextArea(6, 50);
        statusArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusArea.setEditable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setBackground(new Color(250, 250, 250));
        statusArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Status"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        statusArea.setText("Configure your report and click 'Send Report via Email' to send.");

        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(0, 120));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void updateDateFieldsVisibility() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        boolean isCustom = "CUSTOM".equals(reportType);

        // Show/hide custom range panel
        customRangePanel.setVisible(isCustom);

        // Update date label based on report type and set sensible formats
        switch (reportType) {
            case "DAILY":
                dateLabel.setText("Date:");
                if (dateField.getText() == null || dateField.getText().isEmpty()) {
                    dateField.setText(LocalDate.now().format(dateFormatter));
                }
                break;
            case "WEEKLY":
                dateLabel.setText("Start Date:");
                if (dateField.getText() == null || dateField.getText().isEmpty()) {
                    dateField.setText(LocalDate.now().minusDays(7).format(dateFormatter));
                }
                break;
            case "MONTHLY":
                dateLabel.setText("Month (yyyy-MM) or Start Date (yyyy-MM-dd):");
                if (dateField.getText() == null || dateField.getText().isEmpty()) {
                    dateField.setText(YearMonth.now().format(monthFormatter));
                }
                break;
            case "CUSTOM":
                dateLabel.setText("Date Range:");
                break;
            default:
                dateLabel.setText("Date:");
        }

        revalidate();
        repaint();
    }

    private void sendReportEmail() {
        // Validate email
        String email = emailField.getText().trim();
        if (email.isEmpty()) {
            showError("Please enter an email address");
            return;
        }

        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }

        // Validate report type and dates
        String reportType = (String) reportTypeCombo.getSelectedItem();
        Map<String, String> validation = validateReportDates(reportType);
        if (!"true".equals(validation.get("valid"))) {
            showError(validation.get("message"));
            return;
        }

        // Build minimal request payload (only the fields server needs)
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("email", email);
        requestData.put("reportType", reportType);

        // Send a single "date" string for DAILY/WEEKLY/MONTHLY (server will interpret it)
        // For CUSTOM you'd use startDate/endDate but you said you only want minimal JSON.
        String dateValue;
        if ("CUSTOM".equals(reportType)) {
            // If you ever enable CUSTOM, send startDate/endDate instead; here we block it.
            showError("Custom reports are not supported in this minimal request mode.");
            return;
        } else {
            // Ensure we send a yyyy-MM-dd string. For MONTHLY we accept yyyy-MM or yyyy-MM-dd
            dateValue = dateField.getText().trim();
            if (reportType.equals("MONTHLY")) {
                // if user entered yyyy-MM, convert to yyyy-MM-01
                if (dateValue.matches("\\d{4}-\\d{2}")) {
                    dateValue = dateValue + "-01";
                }
            }
            requestData.put("date", dateValue);
        }

        // UI feedback
        sendButton.setEnabled(false);
        sendButton.setText("Sending...");
        statusArea.setText("Sending report to " + email + "...\n");
        statusArea.setForeground(Color.BLUE);

        // Async send
        String finalDateValue = dateValue;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private Map<String, Object> response = null;

            @Override
            protected Void doInBackground() {
                try {
                    // IMPORTANT: pass the Map (object) directly to BaseClient.post.
                    // BaseClient will call mapper.writeValueAsString(body) exactly once.
                    String resp = client.post("/api/secure/reports/email", requestData);

                    if (resp != null && !resp.trim().isEmpty()) {
                        response = mapper.readValue(resp, Map.class);
                    }
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                sendButton.setEnabled(true);
                sendButton.setText("Send Report via Email");

                if (error != null) {
                    statusArea.setText("Error sending email: " + error.getMessage());
                    statusArea.setForeground(Color.RED);
                    showError("Failed to send report: " + error.getMessage());
                    return;
                }

                if (response != null && "success".equals(response.get("status"))) {
                    String successMsg = "✓ Report queued successfully!\n" +
                            "Recipient: " + email + "\n" +
                            "Report Type: " + reportType + "\n" +
                            "Date: " + finalDateValue + "\n" +
                            "Timestamp: " + LocalDate.now().format(dateFormatter) + "\n\n" +
                            "The report will be delivered shortly.";
                    statusArea.setText(successMsg);
                    statusArea.setForeground(new Color(0, 100, 0)); // Dark green

                    Toolkit.getDefaultToolkit().beep();
                } else {
                    String errorMsg = "Failed to send report. Server response: " + response;
                    statusArea.setText(errorMsg);
                    statusArea.setForeground(Color.RED);
                    showError("Failed to send report. Please try again.");
                }
            }
        };

        worker.execute();
    }

    private Map<String, String> validateReportDates(String reportType) {
        Map<String, String> result = new HashMap<>();

        if ("CUSTOM".equals(reportType)) {
            String fromStr = fromField.getText().trim();
            String toStr = toField.getText().trim();

            if (fromStr.isEmpty() || toStr.isEmpty()) {
                result.put("valid", "false");
                result.put("message", "Please enter both from and to dates for custom range");
                return result;
            }

            if (!validateDate(fromStr) || !validateDate(toStr)) {
                result.put("valid", "false");
                result.put("message", "Invalid date format. Use yyyy-MM-dd");
                return result;
            }

            try {
                LocalDate from = LocalDate.parse(fromStr, dateFormatter);
                LocalDate to = LocalDate.parse(toStr, dateFormatter);

                if (from.isAfter(to)) {
                    result.put("valid", "false");
                    result.put("message", "From date must be before or equal to To date");
                    return result;
                }

                long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(from, to);
                if (daysBetween > 365) {
                    result.put("valid", "false");
                    result.put("message", "Date range cannot exceed 1 year");
                    return result;
                }
            } catch (DateTimeParseException e) {
                result.put("valid", "false");
                result.put("message", "Invalid date format. Use yyyy-MM-dd");
                return result;
            }
        } else {
            String dateStr = dateField.getText().trim();

            if (dateStr.isEmpty()) {
                result.put("valid", "false");
                result.put("message", "Please enter a date");
                return result;
            }

            if ("MONTHLY".equals(reportType)) {
                // Accept either yyyy-MM (month) OR yyyy-MM-dd (explicit start date for 30-day window)
                if (!validateMonthFormat(dateStr) && !validateDate(dateStr)) {
                    result.put("valid", "false");
                    result.put("message", "For monthly reports use yyyy-MM (e.g., 2024-01) or a start date yyyy-MM-dd");
                    return result;
                }
            } else if (!validateDate(dateStr)) {
                result.put("valid", "false");
                result.put("message", "Invalid date format. Use yyyy-MM-dd");
                return result;
            }
        }

        result.put("valid", "true");
        result.put("message", "Validation successful");
        return result;
    }

    private void previewReport() {
        String reportType = (String) reportTypeCombo.getSelectedItem();
        Map<String, String> validation = validateReportDates(reportType);

        if (!"true".equals(validation.get("valid"))) {
            showError(validation.get("message"));
            return;
        }

        // Show preview dialog
        String previewText = generatePreviewText();

        JTextArea previewArea = new JTextArea(previewText);
        previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        previewArea.setEditable(false);
        previewArea.setLineWrap(true);
        previewArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(previewArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane,
                "Report Preview - " + reportType, JOptionPane.INFORMATION_MESSAGE);
    }

    private String generatePreviewText() {
        StringBuilder sb = new StringBuilder();
        String reportType = (String) reportTypeCombo.getSelectedItem();

        sb.append("REPORT PREVIEW\n");
        sb.append("==============\n\n");

        sb.append("Configuration:\n");
        sb.append("--------------\n");
        sb.append("Recipient: ").append(emailField.getText()).append("\n");
        sb.append("Report Type: ").append(reportType).append("\n");

        if ("CUSTOM".equals(reportType)) {
            sb.append("Date Range: ").append(fromField.getText()).append(" to ").append(toField.getText()).append("\n");
        } else {
            sb.append("Date: ").append(dateField.getText()).append("\n");
        }

        sb.append("\nSections:\n");
        sb.append("---------\n");
        sb.append("All standard report sections are included by default and cannot be toggled here.\n");
        sb.append("Included: Daily Summary, Product Statistics, Cashier Performance\n");

        sb.append("\nEstimated Report Content:\n");
        sb.append("------------------------\n");

        // Unconditionally describe the standard content (since sections are default)
        sb.append("- Daily Sales Summary\n");
        sb.append("  • Total Sales Amount\n");
        sb.append("  • Total Profit\n");
        sb.append("  • Paid vs Credit Sales\n");
        sb.append("  • Transaction Count\n");

        sb.append("- Product Performance\n");
        sb.append("  • Top Selling Products\n");
        sb.append("  • Revenue by Product\n");
        sb.append("  • Profit Margins\n");
        sb.append("  • Quantity Sold\n");

        sb.append("- Cashier Performance\n");
        sb.append("  • Sales by Cashier\n");
        sb.append("  • Transaction Counts\n");
        sb.append("  • Average Sale Amount\n");
        sb.append("  • Paid vs Credit Sales\n");

        sb.append("\nFormat: PDF attachment with detailed breakdown\n");
        sb.append("Delivery: Email with summary in body and PDF attachment\n");

        return sb.toString();
    }

    private void clearForm() {
        emailField.setText("");
        reportTypeCombo.setSelectedIndex(0);
        dateField.setText(LocalDate.now().format(dateFormatter));
        fromField.setText(LocalDate.now().minusDays(7).format(dateFormatter));
        toField.setText(LocalDate.now().format(dateFormatter));
        statusArea.setText("Form cleared. Configure your report and click 'Send Report via Email' to send.");
        statusArea.setForeground(Color.BLACK);
        updateDateFieldsVisibility();
    }

    private boolean validateDate(String dateStr) {
        try {
            LocalDate.parse(dateStr, dateFormatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private boolean validateMonthFormat(String monthStr) {
        try {
            String[] parts = monthStr.split("-");
            if (parts.length != 2) return false;

            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            if (year < 2000 || year > 2100) return false;
            if (month < 1 || month > 12) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * applyNonTodayQuickDate implements the "logical" ranges:
     * - thisWeek = last 7 days up to today
     * - lastWeek = previous 7-day window
     * - thisMonth = last 30 days up to today
     * - lastMonth = previous 30-day window
     */
    private void applyNonTodayQuickDate(String period) {
        LocalDate now = LocalDate.now();
        LocalDate fromDate = now;
        LocalDate toDate = now;

        switch (period) {
            case "yesterday":
                fromDate = now.minusDays(1);
                toDate = fromDate;
                break;
            case "thisWeek":
                // logical "this week" = last 7 days up to today
                fromDate = now.minusDays(7);
                toDate = now;
                break;
            case "lastWeek":
                // previous 7-day window
                fromDate = now.minusDays(14);
                toDate = now.minusDays(7);
                break;
            case "thisMonth":
                // logical "this month" = last 30 days up to today
                fromDate = now.minusDays(30);
                toDate = now;
                break;
            case "lastMonth":
                // previous 30-day window
                fromDate = now.minusDays(60);
                toDate = now.minusDays(30);
                break;
            default:
                fromDate = now;
                toDate = now;
        }

        String reportType = (String) reportTypeCombo.getSelectedItem();

        if ("CUSTOM".equals(reportType)) {
            fromField.setText(fromDate.format(dateFormatter));
            toField.setText(toDate.format(dateFormatter));
        } else if ("MONTHLY".equals(reportType) || "WEEKLY".equals(reportType)) {
            // For WEEKLY and MONTHLY we set the start date (so server interprets as start -> to)
            dateField.setText(fromDate.format(dateFormatter));
        } else {
            // DAILY and others: show the end date (usually today or a single day)
            dateField.setText(toDate.format(dateFormatter));
        }
    }

}
