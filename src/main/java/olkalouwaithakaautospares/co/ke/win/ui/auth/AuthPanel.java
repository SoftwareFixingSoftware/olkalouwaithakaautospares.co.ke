package olkalouwaithakaautospares.co.ke.win.ui.auth;

import olkalouwaithakaautospares.co.ke.win.utils.BaseClient;
import olkalouwaithakaautospares.co.ke.win.utils.UserSessionManager;
import olkalouwaithakaautospares.co.ke.win.ui.dashboard.MainDashboard;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * AuthPanel - Modern authentication panel with BaseClient integration
 */
public class AuthPanel extends JPanel {
    // UI Components
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel authCards = new JPanel(cardLayout);

    // Input fields - signup
    private final JTextField signupFullNameField = new JTextField();
    private final JTextField signupEmailField = new JTextField();
    private final JPasswordField signupPasswordField = new JPasswordField();
    private final JPasswordField signupConfirmPasswordField = new JPasswordField();
    private final JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Cashier", "Admin"});
    private final JButton signupBtn = new JButton("Create Account");
    private final JLabel signupInlineMsg = new JLabel(" ");

    // Input fields - login
    private final JTextField loginEmailField = new JTextField();
    private final JPasswordField loginPasswordField = new JPasswordField();
    private final JButton loginBtn = new JButton("Sign In");
    private final JLabel loginInlineMsg = new JLabel(" ");
    private final JCheckBox rememberMeCheckbox = new JCheckBox("Remember me");

    // Container layers
    private final JPanel layeredRoot = new JPanel();
    private final JPanel baseLayer = new JPanel(new BorderLayout());
    private final JPanel overlayLayer = new JPanel(new GridBagLayout());
    private final JSplitPane splitPane = new JSplitPane();

    // State
    private boolean isLoginMode = true;

    public AuthPanel() {
        // Initialize BaseClient (will use shared cookie manager)
        BaseClient.getInstance();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(18, 18, 18));

        // Build hero and auth panels
        JPanel heroPanel = createHeroPanel();
        JPanel rightAuth = createRightAuthContainer();

        splitPane.setLeftComponent(heroPanel);
        splitPane.setRightComponent(rightAuth);
        splitPane.setDividerLocation(480);
        splitPane.setResizeWeight(0.0);
        splitPane.setDividerSize(4);
        splitPane.setBorder(null);
        splitPane.setOpaque(false);

        baseLayer.setOpaque(false);
        baseLayer.add(splitPane, BorderLayout.CENTER);

        // Overlay (initially hidden)
        overlayLayer.setOpaque(false);
        overlayLayer.setVisible(false);
        overlayLayer.add(createLoadingOverlay());

        // OverlayLayout stacking
        layeredRoot.setLayout(new OverlayLayout(layeredRoot));
        layeredRoot.add(overlayLayer);
        layeredRoot.add(baseLayer);

        add(layeredRoot, BorderLayout.CENTER);

        // Build auth cards
        authCards.add(createLoginPanel(), "LOGIN");
        authCards.add(createSignupPanel(), "SIGNUP");
        cardLayout.show(authCards, "LOGIN");
    }

    // ---------------------------
    // UI Creation Methods
    // ---------------------------
    private JPanel createRightAuthContainer() {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(Color.black);
        outer.setOpaque(true);
        outer.setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel glass = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
            }
        };
        glass.setOpaque(false);
        glass.setBorder(new EmptyBorder(10, 10, 10, 10));
        glass.add(authCards, BorderLayout.CENTER);

        outer.add(glass, BorderLayout.CENTER);
        return outer;
    }

    private JPanel createHeroPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(6, 84, 92), getWidth(), getHeight(), new Color(4, 58, 72));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Subtle dot pattern
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.04f));
                g2d.setColor(Color.WHITE);
                for (int x = 0; x < getWidth(); x += 36) {
                    for (int y = 0; y < getHeight(); y += 36) {
                        g2d.fillOval(x, y, 2, 2);
                    }
                }
                g2d.dispose();
            }
        };
        panel.setPreferredSize(new Dimension(480, 600));
        panel.setBorder(new EmptyBorder(48, 32, 48, 32));
        panel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        // Logo
        JLabel logo = new JLabel();
        logo.setPreferredSize(new Dimension(350, 350));
        BufferedImage img = loadLogoFromResources("/assets/logo.png");
        if (img != null) {
            logo.setIcon(new ImageIcon(scaleTo(img, 300, 300)));
        } else {
            logo.setIcon(new ImageIcon(createFallbackLogoImage(120)));
        }
        gbc.gridy = 0;
        panel.add(logo, gbc);

        // Title
        JLabel title = new JLabel("Win POS Pro");
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setForeground(new Color(230, 245, 245));
        gbc.gridy++;
        panel.add(title, gbc);

        // Subtitle
        JLabel subtitle = new JLabel("<html><center>Modern POS · Inventory · Analytics</center></html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(new Color(220, 238, 238, 220));
        gbc.gridy++;
        panel.add(subtitle, gbc);

        // Features list
        gbc.gridy++;
        JPanel features = new JPanel(new GridLayout(0, 1, 0, 10));
        features.setOpaque(false);
        features.setBorder(new EmptyBorder(20, 10, 20, 10));
        String[] feats = { "Real-time sales & analytics", "Smart inventory & low-stock alerts",
                "Multi-user roles & permissions", "Secure design" };
        for (String f : feats) {
            JLabel li = new JLabel("  " + f);
            li.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            li.setForeground(new Color(225, 245, 245));
            li.setIcon(new ImageIcon(createCheckIcon()));
            features.add(li);
        }
        panel.add(features, gbc);

        // Spacer
        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(Box.createVerticalGlue(), gbc);

        // Footer
        gbc.gridy++;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets = new Insets(8, 0, 0, 0);
        gbc.weightx = 1.0;

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        footerPanel.setOpaque(false);

        JLabel footer = new JLabel(
                "<html>" +
                        "<span style='color:#7fa8a8;font-size:11px'>" +
                        "© 2026 <a href='https://edtech.winnasacademy.co.ke' " +
                        "style='color:#7fdcdc;text-decoration:none;'><b>       Winnas EdTech KE</b></a>. All rights reserved." +
                        "</span>" +
                        "</html>"
        );
        footer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.setOpaque(false);

        footer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://edtech.winnasacademy.co.ke"));
                } catch (Exception ignored) { }
            }
        });

        footerPanel.add(footer);
        panel.add(footerPanel, gbc);

        return panel;
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(true);
        panel.setBackground(new Color(12, 0, 30));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 6, 0);

        JLabel header = new JLabel("Welcome Back");
        header.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.setForeground(Color.WHITE);
        gbc.gridy = 0;
        panel.add(header, gbc);

        JLabel sub = new JLabel("Sign in to access your Win POS dashboard");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(190, 190, 190));
        gbc.gridy = 1;
        panel.add(sub, gbc);

        // Email field
        gbc.gridy = 2;
        panel.add(createModernInputField("Email Address", loginEmailField, "you@example.com"), gbc);

        // Password field
        gbc.gridy = 3;
        panel.add(createModernPasswordField("Password", loginPasswordField), gbc);

        // Options
        gbc.gridy = 4;
        JPanel options = new JPanel(new BorderLayout());
        options.setOpaque(false);
        rememberMeCheckbox.setOpaque(false);
        rememberMeCheckbox.setForeground(new Color(180,180,180));
        options.add(rememberMeCheckbox, BorderLayout.WEST);

        JLabel forgot = new JLabel("<html><a href='#' style='color:#bde6e6'>Forgot password?</a></html>");
        forgot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        forgot.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        forgot.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { showForgotDialog(); }
        });
        options.add(forgot, BorderLayout.EAST);
        panel.add(options, gbc);

        // Inline message
        gbc.gridy = 5;
        loginInlineMsg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        loginInlineMsg.setForeground(new Color(255, 100, 100));
        panel.add(loginInlineMsg, gbc);

        // Sign-in button
        gbc.gridy = 6;
        gbc.insets = new Insets(12, 0, 8, 0);
        stylePrimaryButton(loginBtn);
        loginBtn.addActionListener(e -> onLogin());
        panel.add(loginBtn, gbc);

        // Switch to signup
        gbc.gridy = 7;
        JLabel toSignup = new JLabel("<html><span style='color:#aaa'>Don't have an account? </span><a href='#' style='color:#bde6e6'><b>Sign Up</b></a></html>");
        toSignup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toSignup.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchToSignup(); }
        });
        panel.add(toSignup, gbc);

        // Enter key support
        addEnterKeySupport(loginEmailField, this::onLogin);
        addEnterKeySupport(loginPasswordField, this::onLogin);

        return panel;
    }

    private JPanel createSignupPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8,8,8,8));
        panel.setBackground(new Color(12,0,30));
        panel.setOpaque(true);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.insets = new Insets(6, 0, 6, 0);

        JLabel header = new JLabel("Create Account");
        header.setFont(new Font("Segoe UI", Font.BOLD, 26));
        header.setForeground(Color.WHITE);
        gbc.gridy = 0;
        panel.add(header, gbc);

        JLabel sub = new JLabel("Join Win POS and streamline your business operations");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(190, 190, 190));
        gbc.gridy = 1;
        panel.add(sub, gbc);

        // Form fields
        gbc.gridy = 2;
        panel.add(createModernInputField("Full Name", signupFullNameField, "John Doe"), gbc);

        gbc.gridy = 3;
        panel.add(createModernInputField("Email Address", signupEmailField, "you@example.com"), gbc);

        gbc.gridy = 4;
        panel.add(createModernPasswordField("Password", signupPasswordField), gbc);

        gbc.gridy = 5;
        panel.add(createModernPasswordField("Confirm Password", signupConfirmPasswordField), gbc);

        // Role selection
        gbc.gridy = 6;
        JPanel rolePanel = new JPanel(new BorderLayout(8, 0));
        rolePanel.setPreferredSize(new Dimension(130, 24));
        rolePanel.setOpaque(false);
        JLabel roleLabel = new JLabel("Role:");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleLabel.setForeground(Color.white);
        roleLabel.setBackground(new Color(40,40,40));
        roleLabel.setOpaque(true);
        rolePanel.add(roleLabel, BorderLayout.WEST);

        styleComboBox(roleCombo);
        roleCombo.setPreferredSize(new Dimension(130, 24));
        rolePanel.add(roleCombo, BorderLayout.CENTER);
        panel.add(rolePanel, gbc);

        // Inline message
        gbc.gridy = 7;
        signupInlineMsg.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signupInlineMsg.setForeground(new Color(255, 100, 100));
        panel.add(signupInlineMsg, gbc);

        // Signup button
        gbc.gridy = 8;
        gbc.insets = new Insets(12, 0, 8, 0);
        stylePrimaryButton(signupBtn);
        signupBtn.addActionListener(e -> onSignup());
        panel.add(signupBtn, gbc);

        // Switch to login
        gbc.gridy = 9;
        JLabel toLogin = new JLabel("<html><span style='color:#aaa'>Already have an account? </span><a href='#' style='color:#bde6e6'><b>Sign In</b></a></html>");
        toLogin.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        toLogin.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { switchToLogin(); }
        });
        panel.add(toLogin, gbc);

        // Enter key support
        addEnterKeySupport(signupFullNameField, this::onSignup);
        addEnterKeySupport(signupEmailField, this::onSignup);
        addEnterKeySupport(signupPasswordField, this::onSignup);
        addEnterKeySupport(signupConfirmPasswordField, this::onSignup);

        return panel;
    }

    // ---------------------------
    // Network Actions
    // ---------------------------
    private void onSignup() {
        final String fullName = signupFullNameField.getText().trim();
        final String email = signupEmailField.getText().trim();
        final String password = new String(signupPasswordField.getPassword());
        final String confirm = new String(signupConfirmPasswordField.getPassword());
        final int role = roleCombo.getSelectedIndex() == 0 ? 2 : 1;

        // Validation
        if (fullName.isEmpty() || fullName.equals("John Doe")) {
            signupInlineMsg.setText("Please enter your full name");
            return;
        }
        if (email.isEmpty() || email.equals("you@example.com")) {
            signupInlineMsg.setText("Please enter your email address");
            return;
        }
        if (!isValidEmail(email)) {
            signupInlineMsg.setText("Please enter a valid email address");
            return;
        }
        if (password.isEmpty()) {
            signupInlineMsg.setText("Please enter a password");
            return;
        }
        if (password.length() < 6) {
            signupInlineMsg.setText("Password must be at least 6 characters");
            return;
        }
        if (!password.equals(confirm)) {
            signupInlineMsg.setText("Passwords do not match");
            return;
        }

        signupInlineMsg.setText(" ");
        setOverlayVisible(true);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>() {
            private String error = null;

            @Override
            protected Map<String, Object> doInBackground() {
                try {
                    BaseClient client = BaseClient.getInstance();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("fullName", fullName);
                    payload.put("email", email);
                    payload.put("password", password);
                    payload.put("role", role);

                    String response = client.post("/api/auth/signup", payload);
                    Map<String, Object> result = client.parseResponse(response);

                    if (!client.isResponseSuccessful(response)) {
                        error = client.getResponseMessage(response);
                        return null;
                    }

                    return (Map<String, Object>) result.get("data");

                } catch (Exception ex) {
                    error = "Network error: " + ex.getMessage();
                    ex.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                setOverlayVisible(false);
                try {
                    Map<String, Object> result = get();
                    if (error != null) {
                        showToast(error, true);
                        signupInlineMsg.setText(error);
                    } else if (result != null) {
                        showToast("Account created successfully! Please sign in.", false);
                        clearSignupForm();
                        switchToLogin();
                    }
                } catch (Exception ex) {
                    showToast("An error occurred: " + ex.getMessage(), true);
                }
            }
        };
        worker.execute();
    }

    private void onLogin() {
        final String email = loginEmailField.getText().trim();
        final String password = new String(loginPasswordField.getPassword());

        if (email.isEmpty() || email.equals("you@example.com")) {
            loginInlineMsg.setText("Please enter your email address");
            return;
        }
        if (!isValidEmail(email)) {
            loginInlineMsg.setText("Please enter a valid email address");
            return;
        }
        if (password.isEmpty()) {
            loginInlineMsg.setText("Please enter your password");
            return;
        }

        loginInlineMsg.setText(" ");
        setOverlayVisible(true);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>() {
            private String error = null;

            @Override
            protected Map<String, Object> doInBackground() {
                try {
                    BaseClient client = BaseClient.getInstance();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("email", email);
                    payload.put("password", password);

                    String response = client.post("/api/auth/login", payload);
                    Map<String, Object> result = client.parseResponse(response);

                    if (!client.isResponseSuccessful(response)) {
                        error = client.getResponseMessage(response);
                        return null;
                    }

                    return (Map<String, Object>) result.get("data");

                } catch (Exception ex) {
                    error = "Network error: " + ex.getMessage();
                    ex.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                setOverlayVisible(false);
                try {
                    Map<String, Object> result = get();
                    if (error != null) {
                        showToast(error, true);
                        loginInlineMsg.setText(error);
                    } else if (result != null) {
                        showToast("Login successful! Welcome back.", false);
                        loginEmailField.setText("");
                        loginPasswordField.setText("");
                        fetchCurrentUser();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showToast("An error occurred: " + ex.getMessage(), true);
                }
            }
        };
        worker.execute();
    }

    private void fetchCurrentUser() {
        setOverlayVisible(true);
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<Map<String, Object>, Void>() {
            private String error = null;

            @Override
            protected Map<String, Object> doInBackground() {
                try {
                    BaseClient client = BaseClient.getInstance();

                    // First verify connection
                    if (!client.testConnection()) {
                        error = "Cannot connect to server. Please check your connection.";
                        return null;
                    }

                    // Get current user
                    String response = client.get("/api/auth/me");
                    Map<String, Object> result = client.parseResponse(response);

                    if (!client.isResponseSuccessful(response)) {
                        error = "Session expired or invalid. Please login again.";
                        return null;
                    }

                    Map<String, Object> userData = (Map<String, Object>) result.get("data");
                    if (userData == null) {
                        error = "No user data returned";
                        return null;
                    }

                    // Validate required fields
                    if (userData.get("id") == null || userData.get("roleId") == null) {
                        error = "Invalid user data received";
                        return null;
                    }

                    return userData;

                } catch (Exception ex) {
                    error = "Failed to fetch user: " + ex.getMessage();
                    ex.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                setOverlayVisible(false);
                try {
                    Map<String, Object> user = get();
                    if (error != null) {
                        showToast(error, true);
                        // Clear any invalid session
                        clearInvalidSession();
                    } else if (user != null) {
                        // Set user data in session manager
                        UserSessionManager sessionManager = UserSessionManager.getInstance();
                        sessionManager.setUserData(user);

                        // Show welcome message
                        Object name = user.get("fullName") != null ? user.get("fullName") : user.get("email");
                        Integer roleId = (Integer) user.get("roleId");
                        String roleName = roleId == 1 ? "Administrator" : "Cashier";

                        showToast("Welcome, " + name + " (" + roleName + ")", false);

                        // Close login window and open dashboard
                        Window parentWindow = SwingUtilities.getWindowAncestor(AuthPanel.this);
                        if (parentWindow instanceof JFrame) {
                            parentWindow.dispose();
                        }

                        // Open dashboard
                        SwingUtilities.invokeLater(() -> {
                            try {
                                MainDashboard dashboard = new MainDashboard();
                                dashboard.setVisible(true);

                                // Log successful login
                                System.out.println("User logged in successfully: " + name + ", Role: " + roleName);

                            } catch (Exception ex) {
                                ex.printStackTrace();
                                showToast("Failed to initialize dashboard: " + ex.getMessage(), true);

                                // Fallback to login screen
                                showLoginScreen();
                            }
                        });
                    } else {
                        showToast("Authenticated but no user details returned.", true);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showToast("An error occurred: " + ex.getMessage(), true);
                    clearInvalidSession();
                }
            }
        };
        worker.execute();
    }

    private void clearInvalidSession() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    BaseClient client = BaseClient.getInstance();
                    client.logout();
                } catch (Exception ex) {
                    // Silent fail
                }
                UserSessionManager.getInstance().logout();
                return null;
            }
        };
        worker.execute();
    }

    private void showForgotDialog() {
        String email = JOptionPane.showInputDialog(
                SwingUtilities.getWindowAncestor(this),
                "Enter your account email:",
                "Forgot Password",
                JOptionPane.PLAIN_MESSAGE
        );

        if (email == null || email.trim().isEmpty()) return;

        if (!isValidEmail(email.trim())) {
            showToast("Please enter a valid email address", true);
            return;
        }

        setOverlayVisible(true);
        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            private String error = null;

            @Override
            protected String doInBackground() {
                try {
                    BaseClient client = BaseClient.getInstance();
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("email", email.trim());

                    String response = client.post("/api/auth/forgot-password", payload);
                    Map<String, Object> result = client.parseResponse(response);

                    if (!client.isResponseSuccessful(response)) {
                        error = client.getResponseMessage(response);
                        return null;
                    }

                    return client.getResponseMessage(response);

                } catch (Exception ex) {
                    error = "Network error: " + ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                setOverlayVisible(false);
                try {
                    String result = get();
                    if (error != null) {
                        showToast(error, true);
                    } else {
                        showToast("If the email exists, an OTP was sent. Check your email.", false);
                    }
                } catch (Exception ex) {
                    showToast("An error occurred: " + ex.getMessage(), true);
                }
            }
        };
        worker.execute();
    }

    // ---------------------------
    // UI Helper Methods
    // ---------------------------
    private JPanel createModernInputField(String label, JTextField textField, String placeholder) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(200, 200, 200));
        panel.add(lbl, BorderLayout.NORTH);

        RoundedInput input = new RoundedInput(textField);
        input.setPreferredSize(new Dimension(380, 44));

        textField.setOpaque(false);
        textField.setBorder(BorderFactory.createEmptyBorder());
        textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textField.setCaretColor(new Color(12, 97, 103));

        textField.setText(placeholder);
        textField.setForeground(new Color(150, 150, 150));
        textField.setToolTipText(label);

        textField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                }
                textField.setForeground(Color.WHITE);
                input.repaint();
            }
            @Override public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(new Color(150, 150, 150));
                } else {
                    textField.setForeground(Color.WHITE);
                }
                input.repaint();
            }
        });

        input.add(textField, BorderLayout.CENTER);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createModernPasswordField(String label, JPasswordField passwordField) {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(200, 200, 200));
        panel.add(lbl, BorderLayout.NORTH);

        RoundedInput input = new RoundedInput(passwordField);
        input.setPreferredSize(new Dimension(380, 44));

        passwordField.setOpaque(false);
        passwordField.setBorder(BorderFactory.createEmptyBorder());
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(new Color(12, 97, 103));
        passwordField.setEchoChar('•');
        passwordField.setToolTipText(label);

        JToggleButton toggle = new JToggleButton();
        toggle.setPreferredSize(new Dimension(30, 30));
        toggle.setOpaque(false);
        toggle.setBorder(BorderFactory.createEmptyBorder());
        toggle.setContentAreaFilled(false);
        toggle.setIcon(new ImageIcon(createEyeIcon(false)));
        toggle.setSelectedIcon(new ImageIcon(createEyeIcon(true)));

        toggle.addActionListener(e -> {
            if (toggle.isSelected()) passwordField.setEchoChar((char) 0);
            else passwordField.setEchoChar('•');
            input.repaint();
        });

        input.add(passwordField, BorderLayout.CENTER);
        input.add(toggle, BorderLayout.EAST);
        panel.add(input, BorderLayout.CENTER);
        return panel;
    }

    private void stylePrimaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setForeground(Color.WHITE);
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = b.getWidth(), h = b.getHeight();
                GradientPaint gp;
                if (b.getModel().isPressed()) gp = new GradientPaint(0,0,new Color(8,65,70), w,h,new Color(10,80,85));
                else if (b.getModel().isRollover()) gp = new GradientPaint(0,0,new Color(18,110,116), w,h,new Color(10,80,85));
                else gp = new GradientPaint(0,0,new Color(12,97,103), w,h,new Color(10,80,85));

                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 10, 10);
                g2.setColor(new Color(0,0,0,45));
                g2.drawRoundRect(0,0,w-1,h-1,10,10);
                g2.dispose();
                super.paint(g, c);
            }
        });
    }

    private void styleComboBox(JComboBox<String> combo) {
        combo.setBackground(new Color(40, 40, 40));
        combo.setForeground(Color.black);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60,60,60),1),
                BorderFactory.createEmptyBorder(4,8,4,8)
        ));
        combo.setFocusable(false);
        combo.setOpaque(true);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setOpaque(true);
                setBackground(isSelected ? new Color(12,97,103) : new Color(40,40,40));
                setForeground(Color.WHITE);
                setFont(new Font("Segoe UI", Font.PLAIN, 13));
                setBorder(new EmptyBorder(5,8,5,8));
                return this;
            }
        });
    }

    // ---------------------------
    // Utility Methods
    // ---------------------------
    private void setOverlayVisible(boolean visible) {
        overlayLayer.setVisible(visible);
        overlayLayer.revalidate();
        overlayLayer.repaint();
        signupBtn.setEnabled(!visible);
        loginBtn.setEnabled(!visible);
    }

    private boolean isValidEmail(String email) {
        return Pattern.compile("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,}$").matcher(email).matches();
    }

    private void clearSignupForm() {
        signupFullNameField.setText("John Doe");
        signupFullNameField.setForeground(new Color(150,150,150));
        signupEmailField.setText("you@example.com");
        signupEmailField.setForeground(new Color(150,150,150));
        signupPasswordField.setText("");
        signupConfirmPasswordField.setText("");
        roleCombo.setSelectedIndex(0);
        signupInlineMsg.setText(" ");
    }

    private void showToast(String msg, boolean error) {
        JWindow toast = new JWindow();
        toast.setLayout(new BorderLayout());
        JLabel label = new JLabel(msg, SwingConstants.CENTER);
        System.out.println(msg);
        label.setBorder(new EmptyBorder(12, 20, 12, 20));
        label.setOpaque(true);
        label.setBackground(error ? new Color(220,80,80) : new Color(12,97,103));
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        toast.add(label);
        toast.pack();

        try {
            Point frameLoc = getLocationOnScreen();
            Dimension frameSize = getSize();
            Dimension toastSize = toast.getSize();
            toast.setLocation(
                    frameLoc.x + (frameSize.width - toastSize.width)/2,
                    frameLoc.y + frameSize.height - toastSize.height - 20
            );
        } catch (Exception ex) {
            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            toast.setLocation(
                    (screen.width - toast.getWidth())/2,
                    screen.height - toast.getHeight() - 100
            );
        }

        toast.setVisible(true);
        Timer t = new Timer(3000, e -> toast.dispose());
        t.setRepeats(false);
        t.start();
    }

    private void addEnterKeySupport(JComponent component, Runnable action) {
        component.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        component.getActionMap().put("submit", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void switchToLogin() {
        isLoginMode = true;
        cardLayout.show(authCards, "LOGIN");
        SwingUtilities.invokeLater(loginEmailField::requestFocusInWindow);
    }

    private void switchToSignup() {
        isLoginMode = false;
        cardLayout.show(authCards, "SIGNUP");
        SwingUtilities.invokeLater(signupFullNameField::requestFocusInWindow);
    }

    private void showLoginScreen() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Win POS Pro - Login");
            frame.setContentPane(new AuthPanel());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    // ---------------------------
    // Icon Creation Methods
    // ---------------------------
    private Image createCheckIcon() {
        int s = 16;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(76,217,100));
        g.setStroke(new BasicStroke(2f));
        g.drawPolyline(new int[]{3,7,13}, new int[]{8,12,4}, 3);
        g.dispose();
        return img;
    }

    private Image createEyeIcon(boolean visible) {
        int size = 20;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(200,200,200));
        g.drawOval(3,6,14,8);
        if (visible) g.fillOval(9,8,3,3);
        else { g.setStroke(new BasicStroke(2f)); g.drawLine(4,16,16,4); }
        g.dispose();
        return img;
    }

    private Image createFallbackLogoImage(int dim) {
        BufferedImage img = new BufferedImage(dim, dim, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255,255,255,30));
        g.fillOval(4,4,dim-8,dim-8);
        GradientPaint gp = new GradientPaint(0,0,new Color(255,255,255,150), dim, dim, new Color(255,255,255,50));
        g.setPaint(gp);
        g.fillOval(10,10,dim-20,dim-20);
        g.setFont(new Font("Segoe UI", Font.BOLD, dim/2));
        g.setColor(new Color(6,84,92));
        FontMetrics fm = g.getFontMetrics();
        String t = "W";
        int tx = (dim - fm.stringWidth(t))/2;
        int ty = (dim + fm.getAscent())/2 - fm.getDescent();
        g.drawString(t, tx, ty);
        g.dispose();
        return img;
    }

    private BufferedImage loadLogoFromResources(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in != null) return ImageIO.read(in);
        } catch (IOException ignored) {}
        return null;
    }

    private Image scaleTo(BufferedImage src, int maxW, int maxH) {
        int w = src.getWidth(), h = src.getHeight();
        double scale = Math.min((double)maxW/w, (double)maxH/h);
        int nw = (int)Math.round(w*scale), nh = (int)Math.round(h*scale);
        return src.getScaledInstance(nw, nh, Image.SCALE_SMOOTH);
    }

    private JComponent createLoadingOverlay() {
        JPanel root = new JPanel(new GridBagLayout()) {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(new Color(0, 0, 0, 160));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setOpaque(false);

        JPanel box = new JPanel(new BorderLayout(0,8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255,255,255,10));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        box.setOpaque(false);
        box.setPreferredSize(new Dimension(220,120));
        box.setBorder(new EmptyBorder(16,16,16,16));

        JLabel spinner = new JLabel() {
            private float a = 0f;
            { new Timer(16, e -> { a += 0.12f; repaint(); }).start(); }
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                int cx = w/2, cy = h/2, r = Math.min(cx,cy) - 6;
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(12,97,103));
                g2.drawArc(cx-r, cy-r, r*2, r*2, (int)Math.toDegrees(a), 260);
                g2.dispose();
            }
        };
        spinner.setPreferredSize(new Dimension(64,64));
        box.add(spinner, BorderLayout.CENTER);

        JLabel text = new JLabel("Processing...", SwingConstants.CENTER);
        text.setForeground(Color.WHITE);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        box.add(text, BorderLayout.SOUTH);

        root.add(box);
        return root;
    }

    // ---------------------------
    // Inner Classes
    // ---------------------------
    private static class RoundedInput extends JPanel {
        RoundedInput(JComponent child) {
            super(new BorderLayout());
            setOpaque(false);
            setBorder(new EmptyBorder(6,10,6,6));
            add(child, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(40,40,40,220));
            g2.fillRoundRect(0,0,w,h,12,12);

            boolean focused = false;
            for (Component c : getComponents()) {
                if (c.isFocusOwner()) {
                    focused = true;
                    break;
                }
            }

            if (focused) {
                g2.setColor(new Color(12,97,103));
                g2.setStroke(new BasicStroke(2f));
            } else {
                g2.setColor(new Color(60,60,60));
                g2.setStroke(new BasicStroke(1f));
            }
            g2.drawRoundRect(0,0,w-1,h-1,12,12);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}