package com.bankingsystem.ui;

import com.bankingsystem.exception.BankException;
import com.bankingsystem.service.AuthService;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.Component;
import java.util.Arrays;

/** Asks for the old password and a new one (twice) and applies the change. */
final class ChangePasswordDialog {
    private ChangePasswordDialog() {
    }

    /** @return true if the password was changed. */
    static boolean show(Component parent, AuthService auth, String user) {
        JPasswordField oldField = new JPasswordField(18);
        JPasswordField newField = new JPasswordField(18);
        JPasswordField repeatField = new JPasswordField(18);
        JPanel form = UiUtils.formPanel();
        UiUtils.addRow(form, 0, "Current password:", oldField);
        UiUtils.addRow(form, 1, "New password:", newField);
        UiUtils.addRow(form, 2, "Repeat new password:", repeatField);

        while (true) {
            int choice = JOptionPane.showConfirmDialog(parent, form, "Change Password",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (choice != JOptionPane.OK_OPTION) {
                return false;
            }
            char[] oldPw = oldField.getPassword();
            char[] newPw = newField.getPassword();
            char[] repeatPw = repeatField.getPassword();
            try {
                if (!Arrays.equals(newPw, repeatPw)) {
                    UiUtils.error(parent, "The two new passwords do not match");
                    continue;
                }
                auth.changePassword(user, oldPw, newPw);
                UiUtils.info(parent, "Password changed.");
                return true;
            } catch (BankException e) {
                UiUtils.error(parent, e.getMessage());
            } finally {
                Arrays.fill(oldPw, '\0');
                Arrays.fill(newPw, '\0');
                Arrays.fill(repeatPw, '\0');
            }
        }
    }
}
