package com.bankingsystem;

import com.bankingsystem.exception.StorageException;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.Bank;
import com.bankingsystem.storage.FileAccountStore;
import com.bankingsystem.ui.LoginFrame;
import com.bankingsystem.ui.MainFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.nio.file.Path;

/**
 * Application entry point. Data lives in the folder given by {@code -Dbank.data.dir=...}
 * (default: {@code ./data}).
 */
public final class App {
    private final Bank bank;
    private final AuthService auth;

    private App(Bank bank, AuthService auth) {
        this.bank = bank;
        this.auth = auth;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::start);
    }

    private static void start() {
        useSystemLookAndFeel();
        Path dir = Path.of(System.getProperty("bank.data.dir", "data"));
        try {
            AuthService auth = new AuthService(dir.resolve("users.properties"));
            Bank bank = new Bank(new FileAccountStore(dir.resolve("bank.tsv")));
            new App(bank, auth).showLogin();
        } catch (StorageException e) {
            // Never continue with an empty bank here: the next save would overwrite the real data.
            JOptionPane.showMessageDialog(null, e.getMessage()
                    + "\n\nThe data was NOT changed. Fix or restore the file (a backup 'bank.tsv.bak' is kept"
                    + " next to it) and start again.", "Cannot start Banking System", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private void showLogin() {
        new LoginFrame(auth, this::showMain).setVisible(true);
    }

    private void showMain(String user) {
        new MainFrame(bank, auth, user, this::showLogin).setVisible(true);
        if (auth.usesDefaultPassword(user)) {
            JOptionPane.showMessageDialog(null, "You are still using the default password.\n"
                    + "Please change it now: File > Change password...", "Security", JOptionPane.WARNING_MESSAGE);
        }
    }

    private static void useSystemLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        return;
                    }
                }
            } catch (Exception ignored) {
                // fall back to the default Swing look
            }
        }
    }
}
