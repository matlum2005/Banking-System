package com.bankingsystem.ui;

import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.exception.BankException;
import com.bankingsystem.model.BankAccount;
import com.bankingsystem.service.Bank;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.math.BigDecimal;

/** Deposit / withdraw / transfer dialog. */
class TransactionDialog extends JDialog {
    private static final long serialVersionUID = 2L;

    enum Mode {
        DEPOSIT("Deposit To Account", "Deposit", "Account number:"),
        WITHDRAW("Withdraw From Account", "Withdraw", "Account number:"),
        TRANSFER("Transfer Money", "Transfer", "From account:");

        private final String title;
        private final String button;
        private final String accountLabel;

        Mode(String title, String button, String accountLabel) {
            this.title = title;
            this.button = button;
            this.accountLabel = accountLabel;
        }
    }

    private final transient Bank bank;
    private final Mode mode;
    private final JTextField accountField = new JTextField(18);
    private final JTextField toField = new JTextField(18);
    private final JTextField amountField = new JTextField(18);
    private final JLabel accountInfo = new JLabel(" ");
    private final JLabel toInfo = new JLabel(" ");
    private boolean done;

    TransactionDialog(Frame owner, Bank bank, Mode mode, String presetAccount) {
        super(owner, mode.title, true);
        this.bank = bank;
        this.mode = mode;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = UiUtils.formPanel();
        int row = 0;
        UiUtils.addRow(form, row++, mode.accountLabel, accountField);
        UiUtils.addRow(form, row++, "", accountInfo);
        if (mode == Mode.TRANSFER) {
            UiUtils.addRow(form, row++, "To account:", toField);
            UiUtils.addRow(form, row++, "", toInfo);
        }
        UiUtils.addRow(form, row, "Amount:", amountField);
        accountInfo.setForeground(Color.GRAY);
        toInfo.setForeground(Color.GRAY);

        JButton ok = new JButton(mode.button);
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> submit());
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(cancel);
        buttons.add(ok);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));
        root.add(UiUtils.heading(mode.title), BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(ok);
        UiUtils.closeOnEscape(this);

        accountField.getDocument().addDocumentListener(new Refresh(accountField, accountInfo));
        toField.getDocument().addDocumentListener(new Refresh(toField, toInfo));
        if (presetAccount != null) {
            accountField.setText(presetAccount);
        }
        pack();
        setLocationRelativeTo(owner);
    }

    boolean wasCompleted() {
        return done;
    }

    private void submit() {
        try {
            BankAccount account = bank.getAccount(accountField.getText());
            BigDecimal amount = UiUtils.parseMoney(amountField.getText());
            String amountText = UiUtils.money(amount);
            String question;
            switch (mode) {
                case DEPOSIT -> question = "Deposit " + amountText + " to #" + account.getAccountNumber()
                        + " (" + account.getHolderName() + ")?";
                case WITHDRAW -> question = "Withdraw " + amountText + " from #" + account.getAccountNumber()
                        + " (" + account.getHolderName() + ")?";
                default -> {
                    BankAccount to = bank.getAccount(toField.getText());
                    question = "Transfer " + amountText + " from #" + account.getAccountNumber()
                            + " (" + account.getHolderName() + ") to #" + to.getAccountNumber()
                            + " (" + to.getHolderName() + ")?";
                }
            }
            if (!UiUtils.confirm(this, question, "Confirm")) {
                return;
            }
            switch (mode) {
                case DEPOSIT -> bank.deposit(account.getAccountNumber(), amount);
                case WITHDRAW -> bank.withdraw(account.getAccountNumber(), amount);
                default -> bank.transfer(account.getAccountNumber(), toField.getText(), amount);
            }
            UiUtils.info(this, mode.button + " successful.\nNew balance of #" + account.getAccountNumber()
                    + ": " + UiUtils.money(account.getBalance()));
            done = true;
            dispose();
        } catch (BankException e) {
            UiUtils.error(this, e.getMessage());
        }
    }

    /** Shows "Name - Type - balance" under an account number field as the user types. */
    private final class Refresh implements DocumentListener {
        private final JTextField field;
        private final JLabel target;

        Refresh(JTextField field, JLabel target) {
            this.field = field;
            this.target = target;
        }

        private void update() {
            String text = field.getText().trim();
            if (text.isEmpty()) {
                target.setText(" ");
                return;
            }
            try {
                BankAccount a = bank.getAccount(text);
                target.setText(a.getHolderName() + " - " + a.getType().label() + " - balance "
                        + UiUtils.money(a.getBalance()));
            } catch (AccountNotFoundException e) {
                target.setText("No account with this number");
            }
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update();
        }
    }
}
