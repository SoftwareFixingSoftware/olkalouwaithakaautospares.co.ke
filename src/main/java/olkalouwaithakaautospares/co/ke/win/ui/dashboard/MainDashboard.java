package olkalouwaithakaautospares.co.ke.win.ui.dashboard;

import olkalouwaithakaautospares.co.ke.win.ui.auth.AuthPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.customer.CustomerPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.home.DashboardHome;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.master.InventoryPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.reports.ReportingPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.returns.ReturnApprovalPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.returns.ReturnPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.sales.SalesPanel;
import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class MainDashboard extends JFrame {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JPanel sidebarPanel;

    public MainDashboard() {
        initUI();
    }

    private void initUI() {
        setTitle("Win POS Pro - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Main layout
        setLayout(new BorderLayout());

        // Create sidebar
        sidebarPanel = createSidebar();
        add(sidebarPanel, BorderLayout.WEST);

        // Create content area
        contentPanel = new JPanel();
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);
        contentPanel.setBackground(new Color(245, 247, 250));

        // Add panels to content area
        contentPanel.add(new DashboardHome(), "HOME");
        contentPanel.add(new SalesPanel(), "SALES");
        contentPanel.add(new CustomerPanel(), "CUSTOMERS");
        contentPanel.add(new InventoryPanel(), "INVENTORY");
         contentPanel.add(new ReturnPanel(), "RETURNS");
        contentPanel.add(new ReturnApprovalPanel(), "RETURNS-APPROVAL");
        contentPanel.add(new ReportingPanel(), "REPORTS");
      //  contentPanel.add(new SettingsPanel(), "SETTINGS");

        add(contentPanel, BorderLayout.CENTER);

        // Show home panel by default
        cardLayout.show(contentPanel, "HOME");

        setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBackground(new Color(30, 33, 57));
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        // Logo/Header
        JLabel logo = new JLabel("WIN POS");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // User info
        UserSessionManager session = UserSessionManager.getInstance();
        String userName = session.getUserFullName();
        String role = session.isAdmin() ? "Administrator" : "Cashier";

        JLabel userLabel = new JLabel("<html><center>" + userName + "<br/><small>" + role + "</small></center></html>");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setForeground(new Color(200, 200, 200));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(userLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Navigation buttons - Admin only buttons
        boolean isAdmin = session.isAdmin();
        String[] navItems;
        String[] panelNames;

        if (isAdmin) {
            navItems = new String[]{"Dashboard", "Sales", "Customers", "Inventory",
                    "Returns", "Reports","Returns-approval", "Settings", "Logout"};
            panelNames = new String[]{"HOME", "SALES", "CUSTOMERS", "INVENTORY",
                    "RETURNS", "REPORTS","RETURNS-APPROVAL", "SETTINGS", "LOGOUT"};
        } else {
            // Cashier only sees Sales, Customers, Returns
            navItems = new String[]{"Dashboard", "Sales", "Customers", "Returns", "Logout"};
            panelNames = new String[]{"HOME", "SALES", "CUSTOMERS", "RETURNS", "LOGOUT"};
        }

        for (int i = 0; i < navItems.length; i++) {
            JButton navButton = createNavButton(navItems[i], panelNames[i]);
            navButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebar.add(navButton);
            sidebar.add(Box.createRigidArea(new Dimension(0, 8)));
        }

        sidebar.add(Box.createVerticalGlue());

        // Footer/version
        JLabel version = new JLabel("v1.0.0");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(150, 150, 150));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(version);

        return sidebar;
    }

    private JButton createNavButton(String text, String panelName) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(40, 44, 75));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(240, 45));
        button.setHorizontalAlignment(SwingConstants.LEFT);

        // Add icon based on panel name
        if (panelName.equals("LOGOUT")) {
            button.setBackground(new Color(220, 80, 80));
        }

        button.addActionListener(e -> {
            if (panelName.equals("LOGOUT")) {
                logout();
            } else {
                cardLayout.show(contentPanel, panelName);
            }
        });

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (!panelName.equals("LOGOUT")) {
                    button.setBackground(new Color(50, 54, 95));
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!panelName.equals("LOGOUT")) {
                    button.setBackground(new Color(40, 44, 75));
                }
            }
        });

        return button;
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to logout?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                BaseClient client = BaseClient.getInstance();
                client.post("/api/auth/logout", Map.of());
            } catch (Exception e) {
                // Log but continue
            }

            UserSessionManager.getInstance().logout();
            dispose();

            // Show login screen again
            SwingUtilities.invokeLater(() -> {
                new AuthPanel().setVisible(true);
            });
        }
    }
}