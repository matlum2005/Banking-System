package com.bankingsystem.ui;

import com.bankingsystem.exception.InvalidAmountException;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Small shared helpers for the Swing screens. */
final class UiUtils {
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");

    private UiUtils() {
    }

    // ---- money ---------------------------------------------------------------------

    /** 1234.5 -> "1,234.50" */
    static String money(BigDecimal value) {
        return String.format(Locale.US, "%,.2f", value);
    }

    /** Parses what the user typed ("1,500", "250.75"); rejects blanks, text, zero, negatives. */
    static BigDecimal parseMoney(String text) throws InvalidAmountException {
        if (text == null || text.isBlank()) {
            throw new InvalidAmountException("Please enter an amount");
        }
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim().replace(",", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("'" + text.trim() + "' is not a valid amount");
        }
        if (value.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        if (value.stripTrailingZeros().scale() > 2) {
            throw new InvalidAmountException("Amount can have at most 2 decimal places");
        }
        if (value.compareTo(MAX_AMOUNT) > 0) {
            throw new InvalidAmountException("Amount is too large");
        }
        return value.setScale(2);
    }

    // ---- dialogs -------------------------------------------------------------------

    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Banking System", JOptionPane.INFORMATION_MESSAGE);
    }

    static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Lets the Escape key close a dialog. */
    static void closeOnEscape(JDialog dialog) {
        String key = "close-on-escape";
        dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), key);
        dialog.getRootPane().getActionMap().put(key, new javax.swing.AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
    }

    // ---- layout helpers ------------------------------------------------------------

    static JLabel heading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 18f));
        return label;
    }

    /** Adds "label  field" as one row of a GridBagLayout form. */
    static void addRow(JPanel form, int row, String label, JComponent field) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.anchor = GridBagConstraints.WEST;
        l.insets = new Insets(5, 0, 5, 12);
        form.add(new JLabel(label), l);

        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 1;
        f.gridy = row;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        f.insets = new Insets(5, 0, 5, 0);
        form.add(field, f);
    }

    static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return panel;
    }

    // ---- icon ----------------------------------------------------------------------

    static Image appImage() {
        URL url = UiUtils.class.getResource("/img/bank.png");
        return url == null ? null : new ImageIcon(url).getImage();
    }

    static ImageIcon scaledIcon(int size) {
        Image image = appImage();
        return image == null ? null : new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }
}
