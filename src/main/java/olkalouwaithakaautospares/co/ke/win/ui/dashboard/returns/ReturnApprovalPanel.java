package olkalouwaithakaautospares.co.ke.win.ui.dashboard.returns;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * ReturnApprovalPanel — For managers/admins to approve or reject returns
 *
 * Left: Pending returns list (select to copy data to form)
 * Right: Approval form + Approval history (global, loaded at start; filtered locally when selecting a return)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class ReturnApprovalPanel extends JPanel {
    private final BaseClient client;
    private final ObjectMapper mapper;
    private final UserSessionManager session;

    private final List<Map<String, Object>> pendingReturns = new ArrayList<>();
    private final List<Map<String, Object>> approvalHistory = new ArrayList<>(); // global history from server

    // Pending returns components
    private JTable pendingTable;
    private DefaultTableModel pendingModel;

    // Approval form components
    private JTextField returnIdField;
    private JTextField saleIdField;
    private JTextField customerField;
    private JTextField itemField;
    private JTextField quantityField;
    private JTextField reasonField;
    private JComboBox<String> decisionCombo;
    private JTextArea remarksArea;

    // Approval history components
    private JTable historyTable;
    private DefaultTableModel historyModel;
    private JLabel historySummaryLabel;
    private JScrollPane historyScrollPane;

    // UI refs
    private JTabbedPane rightTabs;

    public ReturnApprovalPanel() {
        this.client = BaseClient.getInstance();
        this.mapper = client.getMapper();
        this.session = UserSessionManager.getInstance();
        initUI();

        // load both lists by default
        loadPendingReturns();
        loadAllApprovalHistory();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 247, 250));

        JPanel header = createHeader();
        add(header, BorderLayout.NORTH);

        JPanel pendingPanel = createPendingReturnsPanel();

        rightTabs = new JTabbedPane();
        JPanel approvalPanel = createApprovalFormPanel();
        JPanel historyPanel = createApprovalHistoryPanel();

        rightTabs.addTab("Approve/Reject", approvalPanel);
        rightTabs.addTab("Approval History", historyPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pendingPanel, rightTabs);
        splitPane.setDividerLocation(600);
        splitPane.setResizeWeight(0.6);
        splitPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("Return Approvals");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(30, 33, 57));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        JButton refreshBtn = new JButton("Refresh All");
        JButton clearBtn = new JButton("Clear Form");

        styleButton(refreshBtn, new Color(33, 150, 243));
        styleButton(clearBtn, new Color(158, 158, 158));

        refreshBtn.addActionListener(e -> {
            loadPendingReturns();
            loadAllApprovalHistory();
            // if a return is selected, keep it in form (no API call on select)
            String rid = returnIdField.getText().trim();
            if (!rid.isEmpty()) {
                try {
                    int r = Integer.parseInt(rid);
                    // after refresh we re-filter client-side history to show that return's approvals
                    filterHistoryForReturn(r);
                } catch (NumberFormatException ignored) {}
            }
        });

        clearBtn.addActionListener(e -> clearApprovalForm());

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

    private JPanel createPendingReturnsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel("Pending Returns");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        String[] columns = {"Return ID", "Sale ID", "Customer", "Item", "Quantity", "Reason", "Created", "Cashier"};
        pendingModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        pendingTable = new JTable(pendingModel);
        pendingTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pendingTable.setRowHeight(36);
        pendingTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        pendingTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        pendingTable.setFillsViewportHeight(true);
        pendingTable.setAutoCreateRowSorter(true);

        // IMPORTANT: selecting a pending return should NOT call any API.
        // It should only copy data into the form and filter locally-loaded history.
        pendingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) onPendingReturnSelected();
        });

        JScrollPane scrollPane = new JScrollPane(pendingTable);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(560, 520));

        JLabel summaryLabel = new JLabel("Select a return to copy details to the form");
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        summaryLabel.setForeground(new Color(100, 100, 100));
        summaryLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        panel.add(title, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(summaryLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createApprovalFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel formTitle = new JLabel("Process Return");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // --- fields ---
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3; formPanel.add(new JLabel("Return ID:"), gbc);
        returnIdField = new JTextField(); returnIdField.setEditable(false); returnIdField.setBackground(new Color(245,245,245));
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7; formPanel.add(returnIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3; formPanel.add(new JLabel("Sale ID:"), gbc);
        saleIdField = new JTextField(); saleIdField.setEditable(false); saleIdField.setBackground(new Color(245,245,245));
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7; formPanel.add(saleIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3; formPanel.add(new JLabel("Customer:"), gbc);
        customerField = new JTextField(); customerField.setEditable(false); customerField.setBackground(new Color(245,245,245));
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7; formPanel.add(customerField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3; formPanel.add(new JLabel("Item:"), gbc);
        itemField = new JTextField(); itemField.setEditable(false); itemField.setBackground(new Color(245,245,245));
        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 0.7; formPanel.add(itemField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3; formPanel.add(new JLabel("Quantity:"), gbc);
        quantityField = new JTextField(); quantityField.setEditable(false); quantityField.setBackground(new Color(245,245,245));
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.7; formPanel.add(quantityField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3; formPanel.add(new JLabel("Reason:"), gbc);
        reasonField = new JTextField(); reasonField.setEditable(false); reasonField.setBackground(new Color(245,245,245));
        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.7; formPanel.add(reasonField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.3; formPanel.add(new JLabel("Decision:"), gbc);
        decisionCombo = new JComboBox<>(new String[]{"APPROVED", "REJECTED"});
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 0.7; formPanel.add(decisionCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.3; formPanel.add(new JLabel("Remarks:"), gbc);
        remarksArea = new JTextArea(3, 20); remarksArea.setLineWrap(true); remarksArea.setWrapStyleWord(true);
        remarksArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        JScrollPane remarksScroll = new JScrollPane(remarksArea);
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.7; formPanel.add(remarksScroll, gbc);

        // buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        JButton approveBtn = new JButton("Approve Return");
        JButton rejectBtn = new JButton("Reject Return");
        JButton clearBtn = new JButton("Clear");
        styleButton(approveBtn, new Color(76, 175, 80));
        styleButton(rejectBtn, new Color(244, 67, 54));
        styleButton(clearBtn, new Color(158, 158, 158));

        approveBtn.addActionListener(e -> { decisionCombo.setSelectedItem("APPROVED"); processApproval(); });
        rejectBtn.addActionListener(e -> { decisionCombo.setSelectedItem("REJECTED"); processApproval(); });
        clearBtn.addActionListener(e -> clearApprovalForm());

        buttonPanel.add(clearBtn); buttonPanel.add(rejectBtn); buttonPanel.add(approveBtn);

        gbc.gridx = 0; gbc.gridy = 8; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.CENTER;
        formPanel.add(buttonPanel, gbc);

        panel.add(formTitle, BorderLayout.NORTH);
        panel.add(formPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createApprovalHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JLabel title = new JLabel("Approval History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        historySummaryLabel = new JLabel("Approval history — loaded automatically");
        historySummaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historySummaryLabel.setForeground(new Color(100, 100, 100));

        String[] columns = {"ID", "Return ID", "Admin", "Decision", "Remarks", "Decided At"};
        historyModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        historyTable = new JTable(historyModel);
        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        historyTable.setRowHeight(36);
        historyTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        historyTable.setFillsViewportHeight(true);
        historyTable.setAutoCreateRowSorter(true);

        historyScrollPane = new JScrollPane(historyTable);
        historyScrollPane.setBorder(null);
        historyScrollPane.setPreferredSize(new Dimension(520, 520));
        historyScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Layout: title top, table center, summary bottom (matching pending panel)
        panel.add(title, BorderLayout.NORTH);
        panel.add(historyScrollPane, BorderLayout.CENTER);
        panel.add(historySummaryLabel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadPendingReturns() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> returns = new ArrayList<>();

            @Override protected Void doInBackground() {
                try {
                    String resp = null;
                    try {
                        resp = client.get("/api/secure/returns/pending");
                    } catch (Exception ignored) {}
                    if (resp == null || resp.trim().isEmpty()) {
                        resp = client.get("/api/secure/returns");
                    }
                    if (resp != null && !resp.trim().isEmpty()) {
                        try {
                            returns = client.parseResponseList(resp);
                        } catch (Exception ex) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && r.containsKey("data") && r.get("data") instanceof List) {
                                returns = (List<Map<String, Object>>) r.get("data");
                            } else {
                                returns = new ArrayList<>();
                            }
                        }
                    }
                    // Filter pending
                    List<Map<String, Object>> pendingOnly = new ArrayList<>();
                    for (Map<String, Object> ret : returns) {
                        String status = Objects.toString(ret.get("status"), "");
                        if ("PENDING".equalsIgnoreCase(status) || status.trim().isEmpty()) {
                            pendingOnly.add(ret);
                        }
                    }
                    returns = pendingOnly;
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    showError("Failed to load pending returns: " + error.getMessage());
                    return;
                }
                pendingReturns.clear();
                pendingModel.setRowCount(0);
                if (returns != null && !returns.isEmpty()) {
                    pendingReturns.addAll(returns);
                    updatePendingTable();
                } else {
                    pendingModel.addRow(new Object[]{"N/A", "N/A", "No pending", "returns", "N/A", "N/A", "N/A", "N/A"});
                }
            }
        };
        worker.execute();
    }

    private void updatePendingTable() {
        SwingUtilities.invokeLater(() -> {
            pendingModel.setRowCount(0);
            for (Map<String, Object> ret : pendingReturns) {
                Integer id = safeIntegerFromObject(ret.get("id"));
                Integer saleId = safeIntegerFromObject(ret.get("saleId"));
                String customer = Objects.toString(ret.get("customerName"), "") +
                        (ret.get("customerPhone") != null ? " (" + Objects.toString(ret.get("customerPhone"), "") + ")" : "");
                String item = Objects.toString(ret.get("productName"), "");
                if (item.isEmpty()) item = "Item #" + Objects.toString(ret.get("saleItemId"), "N/A");
                Integer qty = safeIntegerFromObject(ret.get("quantity"));
                String reason = Objects.toString(ret.get("reason"), "");
                String created = formatDate(Objects.toString(ret.get("createdAt"), ""));
                String cashier = Objects.toString(ret.get("saleCashierName"), "N/A");

                pendingModel.addRow(new Object[]{
                        id,
                        saleId,
                        customer.isEmpty() ? "N/A" : customer,
                        item,
                        qty != null ? qty : 0,
                        reason,
                        created.length() > 16 ? created.substring(0, 16) : created,
                        cashier
                });
            }
            pendingTable.revalidate();
            pendingTable.repaint();
        });
    }

    private void onPendingReturnSelected() {
        int selectedRow = pendingTable.getSelectedRow();
        if (selectedRow >= 0) {
            int modelRow = pendingTable.convertRowIndexToModel(selectedRow);
            if (modelRow < pendingReturns.size()) {
                Map<String, Object> ret = pendingReturns.get(modelRow);
                Integer returnId = safeIntegerFromObject(ret.get("id"));
                if (returnId != null) {
                    // Populate using the selected row's data (NO API call)
                    populateApprovalForm(ret);

                    // Filter locally-loaded global history to show only records for this return
                    filterHistoryForReturn(returnId);

                    // switch to approval tab
                    if (rightTabs != null) rightTabs.setSelectedIndex(0);
                } else {
                    showError("Selected row does not contain a valid return id");
                }
            } else {
                showError("Unable to determine selected return");
            }
        }
    }

    private void populateApprovalForm(Map<String, Object> ret) {
        Integer id = safeIntegerFromObject(ret.get("id"));
        Integer saleId = safeIntegerFromObject(ret.get("saleId"));
        String customerName = Objects.toString(ret.get("customerName"), "");
        String customerPhone = Objects.toString(ret.get("customerPhone"), "");
        String item = Objects.toString(ret.get("productName"), "");
        if (item.isEmpty()) item = "Item #" + Objects.toString(ret.get("saleItemId"), "N/A");
        Integer qty = safeIntegerFromObject(ret.get("quantity"));
        String reason = Objects.toString(ret.get("reason"), "");

        if (id != null) returnIdField.setText(String.valueOf(id));
        if (saleId != null) saleIdField.setText(String.valueOf(saleId));
        String customerDisplay = (!customerName.isEmpty() ? customerName : "") +
                (!customerPhone.isEmpty() ? (" (" + customerPhone + ")") : "");
        customerField.setText(customerDisplay.isEmpty() ? "N/A" : customerDisplay);
        itemField.setText(item);
        quantityField.setText(qty != null ? String.valueOf(qty) : "0");
        reasonField.setText(reason);

        decisionCombo.setSelectedIndex(0);
        remarksArea.setText("");
    }

    private void loadReturnDetails(int returnId) {
        // keep for completeness; not used on selection (selection must not call API).
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private Map<String, Object> ret = null;

            @Override protected Void doInBackground() {
                try {
                    try {
                        String resp = client.get("/api/secure/returns/" + returnId);
                        if (resp != null && !resp.trim().isEmpty()) {
                            Map<String, Object> r = client.parseResponse(resp);
                            if (r != null && !r.isEmpty()) {
                                if (r.containsKey("data") && r.get("data") instanceof Map) {
                                    ret = (Map<String, Object>) r.get("data");
                                } else {
                                    ret = r;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // fallback: fetch all returns and search
                        try {
                            String allResp = client.get("/api/secure/returns");
                            if (allResp != null && !allResp.trim().isEmpty()) {
                                List<Map<String, Object>> all = client.parseResponseList(allResp);
                                for (Map<String, Object> candidate : all) {
                                    Integer id = safeIntegerFromObject(candidate.get("id"));
                                    if (id != null && id == returnId) {
                                        ret = candidate;
                                        break;
                                    }
                                }
                            }
                        } catch (Exception ex2) {
                            error = e; // original exception
                        }
                    }
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    showError("Failed to load return details: " + error.getMessage());
                    return;
                }
                if (ret != null && !ret.isEmpty()) {
                    populateApprovalForm(ret);
                } else {
                    showError("Return details not found for ID: " + returnId + ". If your API does not expose GET /api/secure/returns/{id}, ensure the returns list contains necessary fields.");
                }
            }
        };
        worker.execute();
    }

    /**
     * Load global approval history (GET /api/secure/return-approvals).
     * Stored in approvalHistory; UI shows this by default.
     */
    private void loadAllApprovalHistory() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private Exception error = null;
            private List<Map<String, Object>> approvals = new ArrayList<>();

            @Override protected Void doInBackground() {
                try {
                    String resp = client.get("/api/secure/return-approvals");
                    System.out.println("DEBUG: raw global approval response -> " + resp);

                    if (resp == null || resp.trim().isEmpty()) {
                        approvals = new ArrayList<>();
                        return null;
                    }

                    try {
                        approvals = client.parseResponseList(resp);
                        if (approvals == null) approvals = new ArrayList<>();
                        return null;
                    } catch (Exception ignored) {}

                    try {
                        Map<String, Object> obj = client.parseResponse(resp);
                        if (obj != null && !obj.isEmpty()) {
                            String[] keys = new String[]{"data", "approvals", "items", "results"};
                            for (String key : keys) {
                                Object maybe = obj.get(key);
                                if (maybe instanceof List) {
                                    approvals = (List<Map<String, Object>>) maybe;
                                    return null;
                                }
                            }
                            // single object -> wrap
                            if (obj.containsKey("id") && obj.containsKey("returnId")) {
                                approvals = new ArrayList<>();
                                approvals.add(obj);
                                return null;
                            }
                        }
                    } catch (Exception ex) {
                        // fall through
                    }

                    // fallback: substring array
                    try {
                        int start = resp.indexOf('[');
                        int end = resp.lastIndexOf(']');
                        if (start >= 0 && end > start) {
                            String arrJson = resp.substring(start, end + 1);
                            approvals = client.parseResponseList(arrJson);
                            if (approvals == null) approvals = new ArrayList<>();
                            return null;
                        }
                    } catch (Exception ex) {
                        // nothing left
                    }
                } catch (Exception ex) {
                    error = ex;
                    ex.printStackTrace();
                }
                return null;
            }

            @Override protected void done() {
                if (error != null) {
                    showError("Failed to load approval history: " + error.getMessage());
                    return;
                }
                approvalHistory.clear();
                historyModel.setRowCount(0);
                if (approvals != null && !approvals.isEmpty()) {
                    approvalHistory.addAll(approvals);
                    updateHistoryTableFromList(approvalHistory);
                    historySummaryLabel.setText(String.format("All approvals (%d records)", approvals.size()));
                } else {
                    historySummaryLabel.setText("No approval history found");
                    historyModel.setRowCount(0);
                    if (historyScrollPane != null) historyScrollPane.getViewport().setViewPosition(new Point(0,0));
                }
            }
        };
        worker.execute();
    }

    /**
     * Filter the already-loaded global approvalHistory for a given returnId,
     * then display results in the history table without calling the API.
     */
    private void filterHistoryForReturn(int returnId) {
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> ap : approvalHistory) {
            Integer rid = safeIntegerFromObject(ap.get("returnId"));
            if (rid != null && rid == returnId) filtered.add(ap);
        }
        if (!filtered.isEmpty()) {
            updateHistoryTableFromList(filtered);
            historySummaryLabel.setText(String.format("Approval history for Return ID: %d (%d records)", returnId, filtered.size()));
        } else {
            historyModel.setRowCount(0);
            historySummaryLabel.setText("No approval history found for Return ID: " + returnId);
            if (historyScrollPane != null) historyScrollPane.getViewport().setViewPosition(new Point(0,0));
        }
    }

    private void updateHistoryTableFromList(List<Map<String, Object>> list) {
        SwingUtilities.invokeLater(() -> {
            historyModel.setRowCount(0);
            for (Map<String, Object> approval : list) {
                Integer id = safeIntegerFromObject(approval.get("id"));
                Integer returnId = safeIntegerFromObject(approval.get("returnId"));
                Integer adminId = safeIntegerFromObject(approval.get("adminId"));
                String adminName = Objects.toString(approval.get("adminName"), "").trim();
                String adminDisplay = !adminName.isEmpty() ? adminName : (adminId != null ? "Admin #" + adminId : "N/A");
                String decision = Objects.toString(approval.get("decision"), "N/A");
                String remarks = Objects.toString(approval.get("remarks"), "");
                String decidedAt = formatDate(Objects.toString(approval.get("decidedAt"), ""));

                historyModel.addRow(new Object[]{
                        id,
                        returnId,
                        adminDisplay,
                        decision,
                        remarks.length() > 30 ? remarks.substring(0, 30) + "..." : remarks,
                        decidedAt.length() > 16 ? decidedAt.substring(0, 16) : decidedAt
                });
            }

            historyTable.revalidate();
            historyTable.repaint();

            // ensure scrolled to top (so table "starts from top" visually)
            if (historyScrollPane != null) {
                historyScrollPane.getViewport().setViewPosition(new Point(0, 0));
            }
        });
    }

    /**
     * POST to /api/secure/return-approvals with payload:
     * { returnId, adminId, decision, remarks }
     *
     * Accept both wrapper and direct-object responses. If server returns the object
     * you provided as sample, mark success and append to local history.
     */
    private void processApproval() {
        String returnIdText = returnIdField.getText().trim();
        String decision = (String) decisionCombo.getSelectedItem();
        String remarks = remarksArea.getText().trim();

        if (returnIdText.isEmpty()) {
            showError("Please select a return to process");
            return;
        }

        if (remarks.isEmpty()) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to " + decision + " without any remarks?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        int returnId, adminId;
        try {
            returnId = Integer.parseInt(returnIdText);
            adminId = session.getUserId();
        } catch (Exception e) {
            showError("Invalid return ID or admin ID");
            return;
        }

        Map<String, Object> request = new HashMap<>();
        request.put("returnId", returnId);
        request.put("adminId", adminId);
        request.put("decision", decision);
        request.put("remarks", remarks);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            private boolean success = false;
            private String message = "";
            private Map<String, Object> createdApproval = null;

            @Override protected Void doInBackground() {
                try {
                    String resp = client.post("/api/secure/return-approvals", request);
                    System.out.println("DEBUG: post approval response -> " + resp);

                    if (resp == null || resp.trim().isEmpty()) {
                        // not ideal, but treat as failure
                        success = false;
                        message = "Empty response from server";
                        return null;
                    }

                    // Try to treat common wrapper with data: {...} or direct object
                    try {
                        Map<String, Object> obj = client.parseResponse(resp);
                        if (obj != null && !obj.isEmpty()) {
                            // If wrapper contains data (object) or contains id directly
                            if (obj.containsKey("data") && obj.get("data") instanceof Map) {
                                Map<String, Object> d = (Map<String, Object>) obj.get("data");
                                if (d.containsKey("id")) {
                                    createdApproval = d;
                                    success = true;
                                    return null;
                                }
                            }
                            // Direct approval object
                            if (obj.containsKey("id") && obj.containsKey("returnId")) {
                                createdApproval = obj;
                                success = true;
                                return null;
                            }

                            // Some servers return {"success": true, "data": { ... } }
                            if (obj.containsKey("success") && Boolean.TRUE.equals(obj.get("success"))) {
                                if (obj.containsKey("data") && obj.get("data") instanceof Map) {
                                    Map<String, Object> d = (Map<String, Object>) obj.get("data");
                                    if (d.containsKey("id")) {
                                        createdApproval = d;
                                        success = true;
                                        return null;
                                    }
                                } else {
                                    // success without data: accept success, but we don't have created object
                                    success = true;
                                    message = client.getResponseMessage(resp);
                                    return null;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        // fall back to try parse as array
                    }

                    // try parse as list (unlikely for POST but safe)
                    try {
                        List<Map<String, Object>> list = client.parseResponseList(resp);
                        if (list != null && !list.isEmpty()) {
                            createdApproval = list.get(0);
                            success = true;
                            return null;
                        }
                    } catch (Exception ignored) {}

                    // final fallback: if client.isResponseSuccessful exists
                    try {
                        if (client.isResponseSuccessful(resp)) {
                            success = true;
                            message = client.getResponseMessage(resp);
                        }
                    } catch (Exception ignored) {
                        // can't determine, mark failure
                        success = false;
                        message = "Unable to parse approval response";
                    }

                } catch (Exception e) {
                    success = false;
                    message = "Error: " + e.getMessage();
                    e.printStackTrace();
                }
                return null;
            }

            @Override protected void done() {
                if (success) {
                    // Inform user
                    JOptionPane.showMessageDialog(ReturnApprovalPanel.this, "Return " + decision.toLowerCase() + " successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                    // If we got the created approval object, append to local history
                    if (createdApproval != null) {
                        approvalHistory.add(0, createdApproval); // add at top (newest first)
                        // If currently filtered to this return, show it; otherwise keep showing global list
                        String ridText = returnIdField.getText().trim();
                        if (!ridText.isEmpty()) {
                            try {
                                int rid = Integer.parseInt(ridText);
                                // if the created approval belongs to the currently selected return, show filtered view
                                Integer createdRid = safeIntegerFromObject(createdApproval.get("returnId"));
                                if (createdRid != null && createdRid == rid) {
                                    filterHistoryForReturn(rid);
                                } else {
                                    // show global
                                    updateHistoryTableFromList(approvalHistory);
                                    historySummaryLabel.setText(String.format("All approvals (%d records)", approvalHistory.size()));
                                }
                            } catch (NumberFormatException ignored) {
                                updateHistoryTableFromList(approvalHistory);
                                historySummaryLabel.setText(String.format("All approvals (%d records)", approvalHistory.size()));
                            }
                        } else {
                            updateHistoryTableFromList(approvalHistory);
                            historySummaryLabel.setText(String.format("All approvals (%d records)", approvalHistory.size()));
                        }
                    } else {
                        // we didn't get object back, refresh everything as server might have accepted
                        loadPendingReturns();
                        loadAllApprovalHistory();
                    }

                    // refresh pending list (so approved items are removed from pending)
                    loadPendingReturns();

                    // clear form
                    clearApprovalForm();
                } else {
                    JOptionPane.showMessageDialog(ReturnApprovalPanel.this, message.isEmpty() ? "Failed to process approval" : message, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void clearApprovalForm() {
        returnIdField.setText("");
        saleIdField.setText("");
        customerField.setText("");
        itemField.setText("");
        quantityField.setText("");
        reasonField.setText("");
        decisionCombo.setSelectedIndex(0);
        remarksArea.setText("");
        historyModel.setRowCount(0);
        historySummaryLabel.setText("Approval history — loaded automatically");
        if (historyScrollPane != null) historyScrollPane.getViewport().setViewPosition(new Point(0,0));
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) return "";
        try {
            if (dateString.length() > 10) return dateString.substring(0, 16).replace("T", " ");
            return dateString;
        } catch (Exception e) { return dateString; }
    }

    private Integer safeIntegerFromObject(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    private void showError(String message) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE));
    }
}
