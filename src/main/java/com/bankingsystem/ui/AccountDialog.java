package com.bankingsystem.ui;

import com.bankingsystem.exception.BankException;
import com.bankingsystem.exception.InvalidAmountException;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAccount;
import com.bankingsystem.service.Bank;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;

/** One dialog for opening a new account (any type) or editing an existing one. */
class AccountDialog extends JDialog {
    private static final long serialVersionUID = 2L;

    private final transient Bank bank;
    private final transient BankAccount existing; // null = "add" mode
    private final JComboBox<AccountType> typeBox = new JComboBox<>(AccountType.values());
    private final JTextField nameField = new JTextField(22);
    private final JTextField balanceField = new JTextField(22);
    private final JTextField detailField = new JTextField(22);
    private final JLabel detailLabel = new JLabel();
    private final JLabel hintLabel = new JLabel(" ");
    private boolean saved;

    AccountDialog(Frame owner, Bank bank, BankAccount existing) {
        super(owner, existing == null ? "Add Account" : "Edit Account #" + existing.getAccountNumber(), true);
        this.bank = bank;
        this.existing = existing;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = UiUtils.formPanel();
        UiUtils.addRow(form, 0, "Account type:", typeBox);
        UiUtils.addRow(form, 1, "Holder name:", nameField);
        if (existing == null) {
            UiUtils.addRow(form, 2, "Opening balance:", balanceField);
        }
        addDetailRow(form, 3);
        GridBagConstraints hint = new GridBagConstraints();
        hint.gridx = 1;
        hint.gridy = 4;
        hint.anchor = GridBagConstraints.WEST;
        hint.insets = new Insets(0, 0, 4, 0);
        hintLabel.setForeground(java.awt.Color.GRAY);
        form.add(hintLabel, hint);

        JButton ok = new JButton(existing == null ? "Add Account" : "Save Changes");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(cancel);
        buttons.add(ok);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        root.add(UiUtils.heading(getTitle()), BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(ok);
        UiUtils.closeOnEscape(this);

        typeBox.addActionListener(e -> updateTypeHints());
        if (existing != null) {
            typeBox.setSelectedItem(existing.getType());
            typeBox.setEnabled(false);
            nameField.setText(existing.getHolderName());
            detailField.setText(existing.getDetail());
        }
        updateTypeHints();
        pack();
        setLocationRelativeTo(owner);
    }

    boolean wasSaved() {
        return saved;
    }

    private void addDetailRow(JPanel form, int row) {
        GridBagConstraints l = new GridBagConstraints();
        l.gridx = 0;
        l.gridy = row;
        l.anchor = GridBagConstraints.WEST;
        l.insets = new Insets(5, 0, 5, 12);
        form.add(detailLabel, l);
        GridBagConstraints f = new GridBagConstraints();
        f.gridx = 1;
        f.gridy = row;
        f.weightx = 1;
        f.fill = GridBagConstraints.HORIZONTAL;
        f.insets = new Insets(5, 0, 5, 0);
        form.add(detailField, f);
    }

    private void updateTypeHints() {
        AccountType type = (AccountType) typeBox.getSelectedItem();
        if (type == null) {
            return;
        }
        detailLabel.setText(type.detailLabel() + ":");
        hintLabel.setText("Minimum balance: " + UiUtils.money(type.minimumBalance()));
    }

    private void submit() {
        AccountType type = (AccountType) typeBox.getSelectedItem();
        try {
            if (existing == null) {
                BigDecimal opening = UiUtils.parseMoney(balanceField.getText());
                BankAccount created = bank.openAccount(type, nameField.getText(), opening, detailField.getText());
                UiUtils.info(this, "Account created.\nAccount number: " + created.getAccountNumber());
            } else {
                bank.updateAccount(existing.getAccountNumber(), nameField.getText(), detailField.getText());
                UiUtils.info(this, "Account updated.");
            }
            saved = true;
            dispose();
        } catch (InvalidAmountException e) {
            UiUtils.error(this, "Opening balance: " + e.getMessage());
        } catch (BankException e) {
            UiUtils.error(this, e.getMessage());
        }
    }
}
