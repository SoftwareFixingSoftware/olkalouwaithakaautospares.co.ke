package olkalouwaithakaautospares.co.ke.win;

import olkalouwaithakaautospares.co.ke.win.ui.auth.AuthPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Application bootstrap / entry point.
 * - Undecorated custom window chrome
 * - Text-only title bar ("AUTOSPARES")
 * - Deterministic EDT startup
 */
public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::bootstrap);
    }

    private static void bootstrap() {
        applyLookAndFeelAndDefaults();

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setUndecorated(true);
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setLayout(new BorderLayout());

        // Main content
        AuthPanel authPanel = new AuthPanel();
        authPanel.setOpaque(true);
        authPanel.setBackground(new Color(18, 18, 18));

        // Custom title bar
        JPanel titleBar = buildTitleBar(frame);

        frame.add(titleBar, BorderLayout.NORTH);
        frame.add(authPanel, BorderLayout.CENTER);

        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void applyLookAndFeelAndDefaults() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        UIManager.put("Button.arc", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("TextComponent.arc", 10);

        Font uiFont = new Font("Segoe UI", Font.PLAIN, 13);
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        for (Object key : defaults.keySet()) {
            Object val = defaults.get(key);
            if (val instanceof Font) {
                UIManager.put(key, uiFont);
            }
        }

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    /**
     * Builds a text-only title bar.
     */
    private static JPanel buildTitleBar(JFrame frame) {
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(18, 18, 18));
        titleBar.setPreferredSize(new Dimension(100, 40));
        titleBar.setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 60, 60))
        );

        // ===== Title text only
        JLabel title = new JLabel("Olkalou waithaka Autospares");
        title.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 0));
        title.setForeground(new Color(220, 220, 220));
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setVerticalAlignment(SwingConstants.CENTER);

        titleBar.add(title, BorderLayout.WEST);

        // ===== Window controls
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        controls.setOpaque(false);

        JButton minimizeBtn = createTitleBarButton("—");
        JButton closeBtn = createTitleBarButton("×");

        minimizeBtn.addActionListener(e -> frame.setState(Frame.ICONIFIED));
        closeBtn.addActionListener(e -> System.exit(0));

        controls.add(minimizeBtn);
        controls.add(closeBtn);

        titleBar.add(controls, BorderLayout.EAST);

        // ===== Drag support
        MouseAdapter drag = new MouseAdapter() {
            private Point offset;

            @Override
            public void mousePressed(MouseEvent e) {
                offset = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point screen = e.getLocationOnScreen();
                frame.setLocation(screen.x - offset.x, screen.y - offset.y);
            }
        };

        titleBar.addMouseListener(drag);
        titleBar.addMouseMotionListener(drag);

        return titleBar;
    }

    private static JButton createTitleBarButton(String text) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        b.setContentAreaFilled(false);
        b.setForeground(new Color(180, 180, 180));
        b.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setOpaque(true);
                b.setForeground(Color.WHITE);
                b.setBackground("×".equals(text)
                        ? new Color(232, 17, 35)
                        : new Color(60, 60, 60));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setOpaque(false);
                b.setBackground(null);
                b.setForeground(new Color(180, 180, 180));
            }
        });

        return b;
    }
}
