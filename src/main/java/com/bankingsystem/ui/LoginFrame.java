package com.bankingsystem.ui;

import com.bankingsystem.service.AuthService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.function.Consumer;

/** Login screen. Three wrong attempts in a row lock the form for a few seconds. */
public class LoginFrame extends JFrame {
    private static final long serialVersionUID = 2L;
    private static final int MAX_ATTEMPTS = 3;
    private static final int LOCK_SECONDS = 10;

    private final transient AuthService auth;
    private final transient Consumer<String> onSuccess;
    private final JTextField userField = new JTextField(AuthService.DEFAULT_USER, 16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton loginButton = new JButton("Login");
    private final JLabel messageLabel = new JLabel(" ", SwingConstants.CENTER);
    private int failedAttempts;

    @SuppressWarnings("this-escape")
    public LoginFrame(AuthService auth, Consumer<String> onSuccess) {
        super("Banking System - Login");
        this.auth = auth;
        this.onSuccess = onSuccess;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        if (UiUtils.appImage() != null) {
            setIconImage(UiUtils.appImage());
        }

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        JPanel top = new JPanel(new BorderLayout(0, 6));
        JLabel logo = new JLabel(UiUtils.scaledIcon(64), SwingConstants.CENTER);
        JLabel title = UiUtils.heading("Banking System");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(logo, BorderLayout.NORTH);
        top.add(title, BorderLayout.CENTER);
        top.add(new JLabel("Please sign in to continue", SwingConstants.CENTER), BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        JPanel form = UiUtils.formPanel();
        UiUtils.addRow(form, 0, "Username:", userField);
        UiUtils.addRow(form, 1, "Password:", passwordField);
        root.add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        messageLabel.setForeground(new Color(0xB00020));
        bottom.add(messageLabel, BorderLayout.NORTH);
        bottom.add(loginButton, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(loginButton); // Enter key logs in
        loginButton.addActionListener(e -> attemptLogin());
        setMinimumSize(new Dimension(380, 0));
        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            passwordField.requestFocusInWindow();
        }
    }

    private void attemptLogin() {
        String user = userField.getText().trim();
        char[] password = passwordField.getPassword();
        try {
            if (auth.authenticate(user, password)) {
                failedAttempts = 0;
                dispose();
                onSuccess.accept(user);
                return;
            }
        } finally {
            Arrays.fill(password, '\0');
        }
        passwordField.setText("");
        failedAttempts++;
        if (failedAttempts >= MAX_ATTEMPTS) {
            lockTemporarily();
        } else {
            messageLabel.setText("Wrong username or password (" + (MAX_ATTEMPTS - failedAttempts)
                    + " attempt(s) left)");
        }
    }

    private void lockTemporarily() {
        loginButton.setEnabled(false);
        userField.setEnabled(false);
        passwordField.setEnabled(false);
        int[] remaining = {LOCK_SECONDS};
        messageLabel.setText("Too many failed attempts. Try again in " + remaining[0] + "s");
        Timer timer = new Timer(1000, null);
        timer.addActionListener(e -> {
            remaining[0]--;
            if (remaining[0] <= 0) {
                timer.stop();
                failedAttempts = 0;
                loginButton.setEnabled(true);
                userField.setEnabled(true);
                passwordField.setEnabled(true);
                messageLabel.setText(" ");
                passwordField.requestFocusInWindow();
            } else {
                messageLabel.setText("Too many failed attempts. Try again in " + remaining[0] + "s");
            }
        });
        timer.start();
    }
}
