import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.*;


/**
 * Online Banking System (Swing GUI)
 * Features:
 *  - Create Account with validation
 *  - Secure login (SHA-256 hashed passwords)
 *  - Deposit / Withdraw / Transfer
 *  - Mini-statement (last 5 transactions)
 *  - Full statement (all transactions)
 *  - Apply simple interest (daily pro-rated)
 *  - Change password
 *  - CSV persistence (bank_data/accounts.csv, bank_data/transactions.csv)
 *
 * How to run:
 *   javac BankAppGUI.java && java BankAppGUI
 */
public class BankAppGUI {
    // UI Constants
    public static final Color PRIMARY_BLUE = new Color(0, 102, 204);
    public static final Color SECONDARY_BLUE = new Color(51, 153, 255);
    public static final Color LIGHT_BLUE = new Color(173, 216, 230);
    public static final Color DARK_BLUE = new Color(0, 51, 102);
    public static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 24);
    public static final Font HEADER_FONT = new Font("Arial", Font.BOLD, 18);
    public static final Font BODY_FONT = new Font("Arial", Font.PLAIN, 14);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AppFrame().setVisible(true));
    }
}

// ============================= UI LAYER ============================= //
class AppFrame extends JFrame {
    private final CardLayout card = new CardLayout();
    private final JPanel root = new JPanel(card);
    private final Bank bank = new Bank();

    private Account session; // currently logged in

    public AppFrame() {
        super("Online Banking System — Swing GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 650);
        setLocationRelativeTo(null);

        // Set Nimbus Look and Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, use default
        }

        bank.bootstrap();

        LoginPanel loginPanel = new LoginPanel(this, bank);
        RegisterPanel registerPanel = new RegisterPanel(this, bank);
        root.add(loginPanel, "login");
        root.add(registerPanel, "register");

        setContentPane(root);
        showLogin();
    }

    void showLogin() { card.show(root, "login"); }
    void showRegister() { card.show(root, "register"); }

    void onLoginSuccess(Account acc) {
        this.session = acc;
        DashboardPanel dash = new DashboardPanel(this, bank, session);
        root.add(dash, "dashboard");
        card.show(root, "dashboard");
    }

    void logout() {
        bank.persist();
        session = null;
        showLogin();
    }
}

class LoginPanel extends JPanel {
    public LoginPanel(AppFrame app, Bank bank) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = bigLabel("🏦 Online Banking System");
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; add(title, c);

        c.gridwidth = 1;
        JLabel lUser = new JLabel("👤 Username");
        lUser.setFont(BankAppGUI.BODY_FONT);
        JTextField tfUser = new JTextField(16);
        tfUser.setToolTipText("Enter your username (4-16 alphanumeric characters)");
        tfUser.setFont(BankAppGUI.BODY_FONT);
        JLabel lPass = new JLabel("🔒 Password");
        lPass.setFont(BankAppGUI.BODY_FONT);
        JPasswordField pfPass = new JPasswordField(16);
        pfPass.setToolTipText("Enter your password");
        pfPass.setFont(BankAppGUI.BODY_FONT);
        JButton btnLogin = new JButton("🔐 Login");
        btnLogin.setToolTipText("Login to your account");
        btnLogin.setFont(BankAppGUI.BODY_FONT);
        JButton btnRegister = new JButton("➕ Create new account");
        btnRegister.setToolTipText("Register a new account");
        btnRegister.setFont(BankAppGUI.BODY_FONT);

        c.gridy = 1; c.gridx = 0; add(lUser, c);
        c.gridx = 1; add(tfUser, c);
        c.gridy = 2; c.gridx = 0; add(lPass, c);
        c.gridx = 1; add(pfPass, c);
        c.gridy = 3; c.gridx = 0; add(btnLogin, c);
        c.gridx = 1; add(btnRegister, c);

        // Add hover effects
        addHoverEffect(btnLogin);
        addHoverEffect(btnRegister);

        btnLogin.addActionListener(e -> {
            String u = tfUser.getText().trim();
            String p = new String(pfPass.getPassword());
            Optional<Account> acc = bank.login(u, p);
            if (acc.isPresent()) {
                JOptionPane.showMessageDialog(this, "Welcome, " + acc.get().getFullName() + "!");
                app.onLoginSuccess(acc.get());
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRegister.addActionListener(e -> app.showRegister());
    }

    private JLabel bigLabel(String t) {
        JLabel l = new JLabel(t, SwingConstants.CENTER);
        l.setFont(BankAppGUI.TITLE_FONT);
        l.setForeground(BankAppGUI.PRIMARY_BLUE);
        return l;
    }

    private void addHoverEffect(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BankAppGUI.SECONDARY_BLUE);
                button.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(UIManager.getColor("Button.background"));
                button.setForeground(UIManager.getColor("Button.foreground"));
            }
        });
    }
}

class RegisterPanel extends JPanel {
    public RegisterPanel(AppFrame app, Bank bank) {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = bigLabel("📝 Create Account");
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; add(title, c);

        c.gridwidth = 1;
        JLabel lUser = new JLabel("👤 Username (4-16 letters/numbers)");
        lUser.setFont(BankAppGUI.BODY_FONT);
        JTextField tfUser = new JTextField(16);
        tfUser.setToolTipText("Enter a unique username (4-16 alphanumeric characters)");
        tfUser.setFont(BankAppGUI.BODY_FONT);
        JLabel lName = new JLabel("👨 Full Name");
        lName.setFont(BankAppGUI.BODY_FONT);
        JTextField tfName = new JTextField(20);
        tfName.setToolTipText("Enter your full name");
        tfName.setFont(BankAppGUI.BODY_FONT);
        JLabel lPass = new JLabel("🔒 Password (min 6)");
        lPass.setFont(BankAppGUI.BODY_FONT);
        JPasswordField pfPass = new JPasswordField(16);
        pfPass.setToolTipText("Enter a password with at least 6 characters");
        pfPass.setFont(BankAppGUI.BODY_FONT);
        JLabel lCPass = new JLabel("🔒 Confirm Password");
        lCPass.setFont(BankAppGUI.BODY_FONT);
        JPasswordField pfCPass = new JPasswordField(16);
        pfCPass.setToolTipText("Re-enter your password to confirm");
        pfCPass.setFont(BankAppGUI.BODY_FONT);
        JLabel lOpen = new JLabel("💰 Opening Deposit (optional)");
        lOpen.setFont(BankAppGUI.BODY_FONT);
        JTextField tfOpen = new JTextField(10);
        tfOpen.setToolTipText("Enter an optional opening deposit amount");
        tfOpen.setFont(BankAppGUI.BODY_FONT);
        JButton btnCreate = new JButton("✅ Create Account");
        btnCreate.setToolTipText("Create your new account");
        btnCreate.setFont(BankAppGUI.BODY_FONT);
        JButton btnBack = new JButton("⬅️ Back to Login");
        btnBack.setToolTipText("Return to the login screen");
        btnBack.setFont(BankAppGUI.BODY_FONT);

        int r = 1;
        c.gridy = r; c.gridx = 0; add(lUser, c); c.gridx = 1; add(tfUser, c);
        r++; c.gridy = r; c.gridx = 0; add(lName, c); c.gridx = 1; add(tfName, c);
        r++; c.gridy = r; c.gridx = 0; add(lPass, c); c.gridx = 1; add(pfPass, c);
        r++; c.gridy = r; c.gridx = 0; add(lCPass, c); c.gridx = 1; add(pfCPass, c);
        r++; c.gridy = r; c.gridx = 0; add(lOpen, c); c.gridx = 1; add(tfOpen, c);
        r++; c.gridy = r; c.gridx = 0; add(btnCreate, c); c.gridx = 1; add(btnBack, c);

        // Add hover effects
        addHoverEffect(btnCreate);
        addHoverEffect(btnBack);

        btnBack.addActionListener(e -> app.showLogin());
        btnCreate.addActionListener((ActionEvent e) -> {
            String u = tfUser.getText().trim();
            String name = tfName.getText().trim();
            String p1 = new String(pfPass.getPassword());
            String p2 = new String(pfCPass.getPassword());
            String open = tfOpen.getText().trim();
            double opening = 0.0;
            if (!open.isEmpty()) {
                try { opening = Double.parseDouble(open); if (opening < 0) throw new NumberFormatException(); }
                catch (NumberFormatException ex) { warn("Enter a valid non-negative amount."); return; }
            }
            if (!Validators.username(u)) { warn("Invalid username format."); return; }
            if (bank.userExists(u)) { warn("Username already exists."); return; }
            if (name.isEmpty()) { warn("Full name required."); return; }
            if (!Validators.password(p1)) { warn("Password too weak (min 6)."); return; }
            if (!p1.equals(p2)) { warn("Passwords do not match."); return; }
            try {
                Account acc = bank.createAccount(u, name, p1, opening);
                JOptionPane.showMessageDialog(this, "Account created! Account No: " + acc.getAccountNumber());
                app.showLogin();
            } catch (Exception ex) {
                error(ex.getMessage());
            }
        });
    }

    private void warn(String m) { JOptionPane.showMessageDialog(this, m, "Warning", JOptionPane.WARNING_MESSAGE); }
    private void error(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }
    private JLabel bigLabel(String t) { JLabel l = new JLabel(t, SwingConstants.CENTER); l.setFont(BankAppGUI.HEADER_FONT); l.setForeground(BankAppGUI.PRIMARY_BLUE); return l; }

    private void addHoverEffect(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BankAppGUI.SECONDARY_BLUE);
                button.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(UIManager.getColor("Button.background"));
                button.setForeground(UIManager.getColor("Button.foreground"));
            }
        });
    }
}

class DashboardPanel extends JPanel {
    private final AppFrame app;
    private final Bank bank;
    private final Account acc;

    private final JLabel lblWelcome = new JLabel();
    private final JLabel lblBalance = new JLabel();
    private final JLabel lblStatus = new JLabel("Ready");

    private final DefaultTableModel txModel = new DefaultTableModel(new Object[]{"Time", "Type", "Amount", "Balance", "Related", "Details"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };

    DashboardPanel(AppFrame app, Bank bank, Account acc) {
        this.app = app; this.bank = bank; this.acc = acc;
        setLayout(new BorderLayout());

        // Header panel with logo and welcome
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BankAppGUI.PRIMARY_BLUE);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        JLabel logo = new JLabel("🏦 Online Banking System", SwingConstants.CENTER);
        logo.setFont(BankAppGUI.TITLE_FONT);
        logo.setForeground(Color.WHITE);
        header.add(logo, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // Top bar with user info and logout
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        lblWelcome.setText("Hello, " + acc.getFullName() + " (Acc: " + acc.getAccountNumber() + ")");
        lblWelcome.setFont(BankAppGUI.HEADER_FONT);
        lblWelcome.setForeground(BankAppGUI.PRIMARY_BLUE);
        JButton btnLogout = new JButton("🚪 Logout");
        btnLogout.setToolTipText("Logout and return to login screen");
        btnLogout.setFont(BankAppGUI.BODY_FONT);
        addHoverEffect(btnLogout);
        btnLogout.addActionListener(e -> app.logout());
        top.add(lblWelcome, BorderLayout.WEST);
        top.add(btnLogout, BorderLayout.EAST);

        // Status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        lblStatus.setFont(BankAppGUI.BODY_FONT);
        statusBar.add(lblStatus, BorderLayout.WEST);

        // Main content
        JPanel main = new JPanel(new BorderLayout());
        main.add(top, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(BankAppGUI.BODY_FONT);
        tabs.addTab("📊 Overview", overviewPanel());
        tabs.addTab("💰 Deposit / Withdraw", cashPanel());
        tabs.addTab("🔄 Transfer", transferPanel());
        tabs.addTab("📜 Statements", statementPanel());
        tabs.addTab("⚙️ Settings", settingsPanel());
        main.add(tabs, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        refreshBalance();
        loadMiniStatement();
    }

    private JPanel overviewPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel head = new JLabel("📊 Account Overview");
        head.setFont(BankAppGUI.HEADER_FONT);
        head.setForeground(BankAppGUI.PRIMARY_BLUE);
        c.gridx = 0; c.gridy = 0; c.gridwidth = 2; p.add(head, c);

        c.gridwidth = 1;
        JLabel lBal = new JLabel("💰 Current Balance:");
        lBal.setFont(BankAppGUI.BODY_FONT);
        lblBalance.setFont(BankAppGUI.HEADER_FONT);
        lblBalance.setForeground(BankAppGUI.DARK_BLUE);
        c.gridy = 1; c.gridx = 0; p.add(lBal, c); c.gridx = 1; p.add(lblBalance, c);

        JButton btnInterest = new JButton("💸 Apply Interest");
        btnInterest.setToolTipText("Apply accrued interest from last application");
        btnInterest.setFont(BankAppGUI.BODY_FONT);
        JButton btnRefresh = new JButton("🔄 Refresh");
        btnRefresh.setToolTipText("Refresh balance and statements");
        btnRefresh.setFont(BankAppGUI.BODY_FONT);
        c.gridy = 2; c.gridx = 0; p.add(btnInterest, c); c.gridx = 1; p.add(btnRefresh, c);

        // Add hover effects
        addHoverEffect(btnInterest);
        addHoverEffect(btnRefresh);

        btnRefresh.addActionListener(e -> {
            refreshBalance();
            loadMiniStatement();
            lblStatus.setText("Data refreshed");
        });
        btnInterest.addActionListener(e -> {
            double added = bank.applyInterest(acc);
            JOptionPane.showMessageDialog(this, String.format("Interest added: ₹%.2f", added));
            refreshBalance();
            loadMiniStatement();
            lblStatus.setText("Interest applied");
        });

        return p;
    }

    private JPanel cashPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel lDep = new JLabel("💰 Deposit Amount (₹)");
        lDep.setFont(BankAppGUI.BODY_FONT);
        JTextField tfDep = new JTextField(10);
        tfDep.setToolTipText("Enter the amount to deposit");
        tfDep.setFont(BankAppGUI.BODY_FONT);
        JButton btnDep = new JButton("➕ Deposit");
        btnDep.setToolTipText("Deposit money into your account");
        btnDep.setFont(BankAppGUI.BODY_FONT);

        JLabel lW = new JLabel("💸 Withdraw Amount (₹)");
        lW.setFont(BankAppGUI.BODY_FONT);
        JTextField tfW = new JTextField(10);
        tfW.setToolTipText("Enter the amount to withdraw");
        tfW.setFont(BankAppGUI.BODY_FONT);
        JButton btnW = new JButton("➖ Withdraw");
        btnW.setToolTipText("Withdraw money from your account");
        btnW.setFont(BankAppGUI.BODY_FONT);

        int r = 0;
        c.gridy = r; c.gridx = 0; p.add(lDep, c); c.gridx = 1; p.add(tfDep, c); c.gridx = 2; p.add(btnDep, c);
        r++; c.gridy = r; c.gridx = 0; p.add(lW, c); c.gridx = 1; p.add(tfW, c); c.gridx = 2; p.add(btnW, c);

        // Add hover effects
        addHoverEffect(btnDep);
        addHoverEffect(btnW);

        btnDep.addActionListener(e -> {
            try {
                double amt = parsePositive(tfDep.getText());
                int confirm = JOptionPane.showConfirmDialog(this, "Confirm deposit of ₹" + fmt(amt) + "?", "Confirm Deposit", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    bank.deposit(acc, amt);
                    info("Deposited ₹" + fmt(amt));
                    lblStatus.setText("Deposit successful");
                    tfDep.setText("");
                }
            } catch (Exception ex) {
                error(ex.getMessage());
            }
            refreshBalance();
            loadMiniStatement();
        });
        btnW.addActionListener(e -> {
            try {
                double amt = parsePositive(tfW.getText());
                int confirm = JOptionPane.showConfirmDialog(this, "Confirm withdrawal of ₹" + fmt(amt) + "?", "Confirm Withdrawal", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    bank.withdraw(acc, amt);
                    info("Withdrew ₹" + fmt(amt));
                    lblStatus.setText("Withdrawal successful");
                    tfW.setText("");
                }
            } catch (Exception ex) {
                error(ex.getMessage());
            }
            refreshBalance();
            loadMiniStatement();
        });

        return p;
    }

    private JPanel transferPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel lTo = new JLabel("👤 To Username");
        lTo.setFont(BankAppGUI.BODY_FONT);
        JTextField tfTo = new JTextField(16);
        tfTo.setToolTipText("Enter the username of the recipient");
        tfTo.setFont(BankAppGUI.BODY_FONT);
        JLabel lAmt = new JLabel("💸 Amount (₹)");
        lAmt.setFont(BankAppGUI.BODY_FONT);
        JTextField tfAmt = new JTextField(10);
        tfAmt.setToolTipText("Enter the amount to transfer");
        tfAmt.setFont(BankAppGUI.BODY_FONT);
        JButton btn = new JButton("🔄 Transfer");
        btn.setToolTipText("Transfer money to another account");
        btn.setFont(BankAppGUI.BODY_FONT);

        c.gridy = 0; c.gridx = 0; p.add(lTo, c); c.gridx = 1; p.add(tfTo, c);
        c.gridy = 1; c.gridx = 0; p.add(lAmt, c); c.gridx = 1; p.add(tfAmt, c);
        c.gridy = 2; c.gridx = 0; c.gridwidth = 2; p.add(btn, c);

        // Add hover effects
        addHoverEffect(btn);

        btn.addActionListener(e -> {
            try {
                String to = tfTo.getText().trim();
                double amt = parsePositive(tfAmt.getText());
                int confirm = JOptionPane.showConfirmDialog(this, "Confirm transfer of ₹" + fmt(amt) + " to " + to + "?", "Confirm Transfer", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    bank.transfer(acc, to, amt);
                    info("Transferred ₹" + fmt(amt) + " to " + to);
                    lblStatus.setText("Transfer successful");
                    tfTo.setText("");
                    tfAmt.setText("");
                }
            } catch (Exception ex) {
                error(ex.getMessage());
            }
            refreshBalance();
            loadMiniStatement();
        });

        return p;
    }

    private JPanel statementPanel() {
        JPanel p = new JPanel(new BorderLayout());
        JTable table = new JTable(txModel);
        table.setFillsViewportHeight(true);
        table.setFont(BankAppGUI.BODY_FONT);
        table.setRowHeight(25);
        JTableHeader header = table.getTableHeader();
        header.setFont(BankAppGUI.HEADER_FONT);
        header.setBackground(BankAppGUI.LIGHT_BLUE);
        header.setForeground(BankAppGUI.DARK_BLUE);
        p.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnMini = new JButton("📄 Mini Statement (Last 5)");
        btnMini.setToolTipText("View the last 5 transactions");
        btnMini.setFont(BankAppGUI.BODY_FONT);
        JButton btnFull = new JButton("📋 Full Statement");
        btnFull.setToolTipText("View all transactions");
        btnFull.setFont(BankAppGUI.BODY_FONT);
        actions.add(btnMini); actions.add(btnFull);
        p.add(actions, BorderLayout.NORTH);

        // Add hover effects
        addHoverEffect(btnMini);
        addHoverEffect(btnFull);

        btnMini.addActionListener(e -> loadMiniStatement());
        btnFull.addActionListener(e -> loadFullStatement());
        return p;
    }

    private JPanel settingsPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel l1 = new JLabel("🔒 Current Password");
        l1.setFont(BankAppGUI.BODY_FONT);
        JPasswordField p1 = new JPasswordField(16);
        p1.setToolTipText("Enter your current password");
        p1.setFont(BankAppGUI.BODY_FONT);
        JLabel l2 = new JLabel("🔑 New Password (min 6)");
        l2.setFont(BankAppGUI.BODY_FONT);
        JPasswordField p2 = new JPasswordField(16);
        p2.setToolTipText("Enter a new password with at least 6 characters");
        p2.setFont(BankAppGUI.BODY_FONT);
        JLabel l3 = new JLabel("🔒 Confirm New Password");
        l3.setFont(BankAppGUI.BODY_FONT);
        JPasswordField p3 = new JPasswordField(16);
        p3.setToolTipText("Re-enter the new password to confirm");
        p3.setFont(BankAppGUI.BODY_FONT);
        JButton btn = new JButton("🔄 Change Password");
        btn.setToolTipText("Change your account password");
        btn.setFont(BankAppGUI.BODY_FONT);

        c.gridy = 0; c.gridx = 0; p.add(l1, c); c.gridx = 1; p.add(p1, c);
        c.gridy = 1; c.gridx = 0; p.add(l2, c); c.gridx = 1; p.add(p2, c);
        c.gridy = 2; c.gridx = 0; p.add(l3, c); c.gridx = 1; p.add(p3, c);
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2; p.add(btn, c);

        // Add hover effects
        addHoverEffect(btn);

        btn.addActionListener((ActionEvent e) -> {
            String cur = new String(p1.getPassword());
            String n1 = new String(p2.getPassword());
            String n2 = new String(p3.getPassword());
            if (bank.login(acc.getUsername(), cur).isEmpty()) { error("Current password is incorrect."); return; }
            if (!Validators.password(n1)) { error("New password too weak (min 6)."); return; }
            if (!n1.equals(n2)) { error("Passwords do not match."); return; }
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to change your password?", "Confirm Password Change", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                bank.changePassword(acc, n1);
                info("Password changed successfully.");
                lblStatus.setText("Password changed");
                p1.setText(""); p2.setText(""); p3.setText("");
            }
        });

        return p;
    }

    private void refreshBalance() {
        lblBalance.setText("₹" + fmt(bank.refreshBalance(acc)));
    }

    private void loadMiniStatement() {
        populate(bank.getMiniStatement(acc.getAccountNumber(), 5));
    }

    private void loadFullStatement() {
        populate(bank.getFullStatement(acc.getAccountNumber()));
    }

    private void populate(List<Transaction> txs) {
        txModel.setRowCount(0);
        DateTimeFormatter dt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Transaction t : txs) {
            txModel.addRow(new Object[]{
                    t.getTimestamp().format(dt),
                    t.getType().name(),
                    fmt(t.getAmount()),
                    fmt(t.getBalanceAfter()),
                    t.getRelatedAccount() == null ? "" : t.getRelatedAccount(),
                    t.getDetails()
            });
        }
    }

    private double parsePositive(String s) {
        try { double v = Double.parseDouble(s.trim()); if (v <= 0) throw new NumberFormatException(); return round2(v);} 
        catch (Exception e) { throw new IllegalArgumentException("Enter a valid positive amount"); }
    }

    private String fmt(double v) { return String.format(Locale.US, "%.2f", v); }
    private double round2(double v) { return Math.round(v*100.0)/100.0; }
    private void info(String m) { JOptionPane.showMessageDialog(this, m, "Info", JOptionPane.INFORMATION_MESSAGE); }
    private void error(String m) { JOptionPane.showMessageDialog(this, m, "Error", JOptionPane.ERROR_MESSAGE); }

    private void addHoverEffect(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(BankAppGUI.SECONDARY_BLUE);
                button.setForeground(Color.WHITE);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(UIManager.getColor("Button.background"));
                button.setForeground(UIManager.getColor("Button.foreground"));
            }
        });
    }
}

// ============================= DOMAIN & STORAGE ============================= //
class Bank {
    private final Storage storage = new Storage();
    private final Map<String, Account> byUsername = new HashMap<>();
    private final Map<String, Account> byAccountNo = new HashMap<>();

    private static final double ANNUAL_RATE = 0.04; // 4% p.a.

    public void bootstrap() {
        storage.ensureFiles();
        List<Account> accounts = storage.loadAccounts();
        for (Account a : accounts) {
            byUsername.put(a.getUsername(), a);
            byAccountNo.put(a.getAccountNumber(), a);
        }
    }

    public void persist() { storage.saveAccounts(new ArrayList<>(byUsername.values())); }

    public boolean userExists(String username) { return byUsername.containsKey(username); }

    public Account createAccount(String username, String fullName, String password, double openingDeposit) {
        if (userExists(username)) throw new IllegalStateException("Username exists");
        String accNo = Ids.newAccountNumber();
        String hash = Crypto.sha256(password);
        Account acc = new Account(accNo, username, hash, fullName, 0.0, LocalDate.now());
        byUsername.put(username, acc);
        byAccountNo.put(accNo, acc);
        if (openingDeposit > 0) deposit(acc, openingDeposit);
        persist();
        return acc;
    }

    public Optional<Account> login(String username, String password) {
        Account a = byUsername.get(username);
        if (a == null) return Optional.empty();
        return a.getPasswordHash().equals(Crypto.sha256(password)) ? Optional.of(a) : Optional.empty();
    }

    public void changePassword(Account acc, String newPassword) { acc.setPasswordHash(Crypto.sha256(newPassword)); persist(); }

    public double refreshBalance(Account acc) { return Account.round2(acc.getBalance()); }

    public void deposit(Account acc, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
        acc.setBalance(Account.round2(acc.getBalance() + amount));
        storage.appendTransaction(Transaction.deposit(acc.getAccountNumber(), amount, acc.getBalance()));
        persist();
    }

    public void withdraw(Account acc, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
        if (acc.getBalance() < amount) throw new IllegalStateException("Insufficient funds");
        acc.setBalance(Account.round2(acc.getBalance() - amount));
        storage.appendTransaction(Transaction.withdraw(acc.getAccountNumber(), amount, acc.getBalance()));
        persist();
    }

    public void transfer(Account from, String toUsername, double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be > 0");
        Account to = byUsername.get(toUsername);
        if (to == null) throw new IllegalArgumentException("Recipient not found");
        if (from.getUsername().equals(toUsername)) throw new IllegalArgumentException("Cannot transfer to self");
        if (from.getBalance() < amount) throw new IllegalStateException("Insufficient funds");

        from.setBalance(Account.round2(from.getBalance() - amount));
        to.setBalance(Account.round2(to.getBalance() + amount));
        storage.appendTransaction(Transaction.transferOut(from.getAccountNumber(), amount, from.getBalance(), to.getAccountNumber()));
        storage.appendTransaction(Transaction.transferIn(to.getAccountNumber(), amount, to.getBalance(), from.getAccountNumber()));
        persist();
    }

    public List<Transaction> getMiniStatement(String accountNumber, int lastN) {
        List<Transaction> all = storage.loadTransactionsFor(accountNumber);
        all.sort(Comparator.comparing(Transaction::getTimestamp));
        return all.subList(Math.max(0, all.size()-lastN), all.size());
    }

    public List<Transaction> getFullStatement(String accountNumber) {
        List<Transaction> all = storage.loadTransactionsFor(accountNumber);
        all.sort(Comparator.comparing(Transaction::getTimestamp));
        return all;
    }

    public double applyInterest(Account acc) {
        LocalDate last = acc.getLastInterestApplied();
        LocalDate today = LocalDate.now();
        if (!today.isAfter(last)) return 0.0;
        long days = Duration.between(last.atStartOfDay(), today.atStartOfDay()).toDays();
        double ratePerDay = ANNUAL_RATE / 365.0;
        double interest = Account.round2(acc.getBalance() * ratePerDay * days);
        if (interest > 0) {
            acc.setBalance(Account.round2(acc.getBalance() + interest));
            acc.setLastInterestApplied(today);
            storage.appendTransaction(Transaction.interest(acc.getAccountNumber(), interest, acc.getBalance(), days));
            persist();
        }
        return interest;
    }
}

class Account {
    private final String accountNumber;
    private final String username;
    private String passwordHash;
    private final String fullName;
    private double balance;
    private LocalDate lastInterestApplied;
    private final LocalDate createdAt;

    public Account(String accountNumber, String username, String passwordHash, String fullName, double balance, LocalDate lastInterestApplied) {
        this.accountNumber = accountNumber;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.balance = balance;
        this.lastInterestApplied = lastInterestApplied == null ? LocalDate.now() : lastInterestApplied;
        this.createdAt = LocalDate.now();
    }

    public String getAccountNumber() { return accountNumber; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public double getBalance() { return balance; }
    public LocalDate getLastInterestApplied() { return lastInterestApplied; }
    public LocalDate getCreatedAt() { return createdAt; }

    public void setPasswordHash(String hash) { this.passwordHash = hash; }
    public void setBalance(double b) { this.balance = round2(b); }
    public void setLastInterestApplied(LocalDate d) { this.lastInterestApplied = d; }

    public static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}

enum TxType { DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT, INTEREST }

class Transaction {
    private final String id;
    private final LocalDateTime timestamp;
    private final String accountNumber;
    private final TxType type;
    private final double amount;
    private final double balanceAfter;
    private final String details;
    private final String relatedAccount;

    public Transaction(String id, LocalDateTime timestamp, String accountNumber, TxType type,
                       double amount, double balanceAfter, String details, String relatedAccount) {
        this.id = id; this.timestamp = timestamp; this.accountNumber = accountNumber; this.type = type;
        this.amount = amount; this.balanceAfter = balanceAfter; this.details = details; this.relatedAccount = relatedAccount;
    }

    public String getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAccountNumber() { return accountNumber; }
    public TxType getType() { return type; }
    public double getAmount() { return amount; }
    public double getBalanceAfter() { return balanceAfter; }
    public String getDetails() { return details; }
    public String getRelatedAccount() { return relatedAccount; }

    public static Transaction deposit(String accNo, double amt, double bal) { return new Transaction(Ids.uuid(), LocalDateTime.now(), accNo, TxType.DEPOSIT, amt, bal, "Cash/Online Deposit", ""); }
    public static Transaction withdraw(String accNo, double amt, double bal) { return new Transaction(Ids.uuid(), LocalDateTime.now(), accNo, TxType.WITHDRAWAL, amt, bal, "Cash Withdrawal", ""); }
    public static Transaction transferOut(String accNo, double amt, double bal, String toAcc) { return new Transaction(Ids.uuid(), LocalDateTime.now(), accNo, TxType.TRANSFER_OUT, amt, bal, "Transfer to "+toAcc, toAcc); }
    public static Transaction transferIn(String accNo, double amt, double bal, String fromAcc) { return new Transaction(Ids.uuid(), LocalDateTime.now(), accNo, TxType.TRANSFER_IN, amt, bal, "Transfer from "+fromAcc, fromAcc); }
    public static Transaction interest(String accNo, double amt, double bal, long days) { return new Transaction(Ids.uuid(), LocalDateTime.now(), accNo, TxType.INTEREST, amt, bal, "Interest for "+days+" day(s)", ""); }
}

class Storage {
    private static final String DATA_DIR = "bank_data";
    private static final String ACCOUNTS_CSV = DATA_DIR + "/accounts.csv";
    private static final String TX_CSV = DATA_DIR + "/transactions.csv";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public void ensureFiles() {
        try {
            Path dir = Paths.get(DATA_DIR);
            if (!Files.exists(dir)) Files.createDirectories(dir);
            Path ac = Paths.get(ACCOUNTS_CSV);
            if (!Files.exists(ac)) Files.write(ac, Collections.singletonList("accountNumber,username,passwordHash,fullName,balance,lastInterestApplied,createdAt"));
            Path tx = Paths.get(TX_CSV);
            if (!Files.exists(tx)) Files.write(tx, Collections.singletonList("id,timestamp,accountNumber,type,amount,balanceAfter,details,relatedAccount"));
        } catch (IOException e) { throw new RuntimeException("Failed to init storage: "+e.getMessage()); }
    }

    public List<Account> loadAccounts() {
        List<Account> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(ACCOUNTS_CSV))) {
            String line; boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] p = CSV.split(line);
                if (p.length < 7) continue;
                String accNo = p[0]; String username = p[1]; String hash = p[2]; String fullName = p[3];
                double balance = Double.parseDouble(p[4]);
                LocalDate lastInt = LocalDate.parse(p[5], DATE);
                LocalDate.parse(p[6], DATE);
                Account a = new Account(accNo, username, hash, fullName, balance, lastInt);
                list.add(a);
            }
        } catch (IOException e) { throw new RuntimeException("Error reading accounts: "+e.getMessage()); }
        return list;
    }

    public void saveAccounts(List<Account> accounts) {
        List<String> lines = new ArrayList<>();
        lines.add("accountNumber,username,passwordHash,fullName,balance,lastInterestApplied,createdAt");
        for (Account a : accounts) {
            lines.add(String.join(",",
                    a.getAccountNumber(), a.getUsername(), a.getPasswordHash(),
                    CSV.escape(a.getFullName()),
                    String.format(Locale.US, "%.2f", a.getBalance()),
                    a.getLastInterestApplied().format(DATE),
                    a.getCreatedAt().format(DATE)
            ));
        }
        try { Files.write(Paths.get(ACCOUNTS_CSV), lines); }
        catch (IOException e) { throw new RuntimeException("Error saving accounts: "+e.getMessage()); }
    }

    public void appendTransaction(Transaction t) {
        String line = String.join(",",
                t.getId(), t.getTimestamp().format(DATETIME), t.getAccountNumber(), t.getType().name(),
                String.format(Locale.US, "%.2f", t.getAmount()),
                String.format(Locale.US, "%.2f", t.getBalanceAfter()),
                CSV.escape(t.getDetails() == null ? "" : t.getDetails()),
                t.getRelatedAccount() == null ? "" : t.getRelatedAccount()
        );
        try (BufferedWriter bw = Files.newBufferedWriter(Paths.get(TX_CSV), StandardOpenOption.APPEND)) {
            bw.write(line); bw.newLine();
        } catch (IOException e) { throw new RuntimeException("Error writing transaction: "+e.getMessage()); }
    }

    public List<Transaction> loadTransactionsFor(String accountNumber) {
        List<Transaction> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(TX_CSV))) {
            String line; boolean header = true;
            while ((line = br.readLine()) != null) {
                if (header) { header = false; continue; }
                if (line.isBlank()) continue;
                String[] p = CSV.split(line);
                if (p.length < 8) continue;
                if (!Objects.equals(p[2], accountNumber)) continue;
                String id = p[0]; LocalDateTime ts = LocalDateTime.parse(p[1], DATETIME);
                TxType type = TxType.valueOf(p[3]);
                double amount = Double.parseDouble(p[4]);
                double balAfter = Double.parseDouble(p[5]);
                String details = CSV.unescape(p[6]);
                String related = p[7];
                list.add(new Transaction(id, ts, accountNumber, type, amount, balAfter, details, related));
            }
        } catch (IOException e) { throw new RuntimeException("Error reading transactions: "+e.getMessage()); }
        return list;
    }
}

class CSV {
    public static String escape(String s) {
        if (s == null) return "";
        boolean need = s.contains(",") || s.contains("\"") || s.contains("\n");
        String out = s.replace("\"", "\"\"");
        return need ? "\"" + out + "\"" : out;
    }
    public static String unescape(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length()-1).replace("\"\"", "\"");
        }
        return s;
    }
    public static String[] split(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i+1 < line.length() && line.charAt(i+1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(ch);
            } else {
                if (ch == '"') inQuotes = true;
                else if (ch == ',') { parts.add(cur.toString()); cur.setLength(0); }
                else cur.append(ch);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }
}

class Ids {
    public static String uuid() { return UUID.randomUUID().toString(); }
    public static String newAccountNumber() {
        String yyyymm = YearMonth.now().toString().replace("-", "");
        int r = 100000 + new Random().nextInt(900000);
        return yyyymm + r;
    }
}

class Crypto {
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { throw new RuntimeException("SHA-256 not available"); }
    }
}

class Validators {
    public static boolean username(String u) { return u != null && u.matches("[A-Za-z0-9]{4,16}"); }
    public static boolean password(String p) { return p != null && p.length() >= 6; }
}