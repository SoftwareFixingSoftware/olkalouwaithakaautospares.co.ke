package olkalouwaithakaautospares.co.ke.win.ui.dashboard;

import olkalouwaithakaautospares.co.ke.win.ui.dashboard.customer.CustomerPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.home.DashboardHome;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.master.InventoryPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.reports.EmailReportsPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.reports.ReportingPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.returns.ReturnApprovalPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.returns.ReturnPanel;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.sales.SalesPanel;
import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

public class MainDashboard extends JFrame {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JPanel sidebarPanel;

    private final Map<String, JButton> navButtons = new HashMap<>();
    private JButton selectedButton = null;
    private final Map<JButton, Timer> animTimers = new HashMap<>();

    private static final Color SIDEBAR_BG = new Color(6, 84, 92);
    private static final Color DEFAULT_BG = new Color(40, 44, 75);
    private static final Color HOVER_BG = new Color(50, 54, 95);
    private static final Color SELECTED_BG = new Color(26, 161, 155);
    private static final Color EXIT_BG = new Color(220, 80, 80);

    private static final int ANIM_DURATION_MS = 220;
    private static final int ANIM_DELAY_MS = 15;

    // Minimum window size to ensure usability
    private static final int MIN_WIDTH = 1024;
    private static final int MIN_HEIGHT = 768;

    public MainDashboard() {
        initUI();
    }

    private void initUI() {
        setTitle("POS Pro - Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set minimum size before maximizing
        setMinimumSize(new Dimension(MIN_WIDTH, MIN_HEIGHT));

        // Start maximized
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
        } catch (Exception e) {
            e.printStackTrace();
        }

        setLayout(new BorderLayout());
        sidebarPanel = createSidebar();
        add(sidebarPanel, BorderLayout.WEST);

        contentPanel = new JPanel();
        cardLayout = new CardLayout();
        contentPanel.setLayout(cardLayout);
        contentPanel.setBackground(new Color(245, 247, 250));

        contentPanel.add(new DashboardHome(), "HOME");
        contentPanel.add(new SalesPanel(), "SALES");
        contentPanel.add(new CustomerPanel(), "CUSTOMERS");
        contentPanel.add(new InventoryPanel(), "INVENTORY");
        contentPanel.add(new ReturnPanel(), "RETURNS");
        contentPanel.add(new ReturnApprovalPanel(), "RETURNS-APPROVAL");
        contentPanel.add(new ReportingPanel(), "REPORTS");
        contentPanel.add(new EmailReportsPanel(), "EMAIL-REPORTS");

        add(contentPanel, BorderLayout.CENTER);

        cardLayout.show(contentPanel, "HOME");
        SwingUtilities.invokeLater(() -> {
            JButton homeBtn = navButtons.get("HOME");
            if (homeBtn != null) setSelectedButton(homeBtn);
        });

        // Center the window on screen
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBackground(SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 10, 20, 10));

        JLabel logo = new JLabel("Manage. Sell. Grow.");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 24));
        logo.setForeground(Color.WHITE);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(logo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 24)));

        UserSessionManager session = UserSessionManager.getInstance();
        String userName = session.getUserFullName();
        String role = session.isAdmin() ? "Administrator" : "Cashier";

        JLabel userLabel = new JLabel("<html><center>" + userName + "<br/><small>" + role + "</small></center></html>");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        userLabel.setForeground(new Color(200, 200, 200));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(userLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 20)));

        boolean isAdmin = session.isAdmin();
        String[] navItems;
        String[] panelNames;

        if (isAdmin) {
            navItems = new String[]{"Dashboard", "Sales", "Customers", "Inventory", "Returns", "Returns Approval", "Reports", "Email Reports", "Exit"};
            panelNames = new String[]{"HOME", "SALES", "CUSTOMERS", "INVENTORY", "RETURNS", "RETURNS-APPROVAL", "REPORTS", "EMAIL-REPORTS", "EXIT"};
        } else {
            navItems = new String[]{"Dashboard", "Sales", "Customers", "Returns", "Email Reports", "Exit"};
            panelNames = new String[]{"HOME", "SALES", "CUSTOMERS", "RETURNS", "EMAIL-REPORTS", "EXIT"};
        }

        for (int i = 0; i < navItems.length; i++) {
            JButton navButton = createNavButton(navItems[i], panelNames[i]);
            navButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            sidebar.add(navButton);
            sidebar.add(Box.createVerticalGlue());
            navButtons.put(panelNames[i], navButton);
        }

        JLabel version = new JLabel("<html><center>&copy; " + java.time.Year.now() + "<br/>Powered by <b>WINNAS EDTECH KE</b></center></html>");
        version.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        version.setForeground(new Color(180, 180, 180));
        version.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(version);

        return sidebar;
    }

    private JButton createNavButton(String text, String panelName) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(panelName.equals("EXIT") ? EXIT_BG : DEFAULT_BG);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(240, 52));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setActionCommand(panelName);

        button.addActionListener(e -> {
            String cmd = e.getActionCommand();
            if (cmd.equals("EXIT")) {
                exitApplication();
            } else {
                cardLayout.show(contentPanel, cmd);
                setSelectedButton(button);
            }
        });

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                if (button != selectedButton && !button.getActionCommand().equals("EXIT")) {
                    cancelAnim(button);
                    button.setBackground(HOVER_BG);
                }
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (button == selectedButton) {
                    cancelAnim(button);
                    button.setBackground(SELECTED_BG);
                } else {
                    cancelAnim(button);
                    button.setBackground(button.getActionCommand().equals("EXIT") ? EXIT_BG : DEFAULT_BG);
                }
            }
        });

        return button;
    }

    private void exitApplication() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to exit? This will clear your session and close the application.",
                "Confirm Exit",
                JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // Attempt server-side logout (invalidates server session & cookies)
            try {
                BaseClient.getInstance().logout();
            } catch (Exception e) {
                // Log but continue with client-side cleanup — do not block exit
                System.err.println("Server logout failed: " + e.getMessage());
            }

            // Clear client-side session manager
            UserSessionManager.getInstance().logout();

            // Ensure cookies cleared locally as well
            try {
                BaseClient.getCookieStore().removeAll();
            } catch (Exception ignored) {}

            // Close UI and stop the JVM
            dispose();
            System.exit(0);
        }
    }

    private void setSelectedButton(JButton button) {
        if (button == null) return;
        String cmd = button.getActionCommand();
        if ("EXIT".equals(cmd)) return;
        if (selectedButton != null && selectedButton != button) {
            JButton prev = selectedButton;
            cancelAnim(prev);
            prev.setBackground(DEFAULT_BG);
        }
        selectedButton = button;
        animateBackground(button, button.getBackground(), SELECTED_BG);
    }

    private void animateBackground(JButton button, Color start, Color end) {
        if (button == null) return;
        cancelAnim(button);
        if (start.equals(end)) { button.setBackground(end); return; }

        final int steps = Math.max(1, ANIM_DURATION_MS / ANIM_DELAY_MS);
        final int[] step = {0};
        final int rStart = start.getRed(), gStart = start.getGreen(), bStart = start.getBlue();
        final int rEnd = end.getRed(), gEnd = end.getGreen(), bEnd = end.getBlue();

        Timer t = new Timer(ANIM_DELAY_MS, null);
        t.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                step[0]++;
                float frac = Math.min(1f, (float) step[0] / steps);
                int r = rStart + Math.round((rEnd - rStart) * frac);
                int g = gStart + Math.round((gEnd - gStart) * frac);
                int b = bStart + Math.round((bEnd - bStart) * frac);
                button.setBackground(new Color(r, g, b));
                if (frac >= 1f) cancelAnim(button);
            }
        });
        animTimers.put(button, t);
        t.start();
    }

    private void cancelAnim(JButton button) {
        Timer existing = animTimers.remove(button);
        if (existing != null) existing.stop();
    }
}