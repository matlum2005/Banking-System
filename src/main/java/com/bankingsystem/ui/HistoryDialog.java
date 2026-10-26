package com.bankingsystem.ui;

import com.bankingsystem.model.BankAccount;
import com.bankingsystem.model.SavingsAccount;
import com.bankingsystem.model.Transaction;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Balance enquiry plus the full statement of one account (newest first). */
class HistoryDialog extends JDialog {
    private static final long serialVersionUID = 2L;
    private static final String[] COLUMNS = {"Date / time", "Type", "Amount", "Balance after", "Note"};

    HistoryDialog(Frame owner, BankAccount account) {
        super(owner, "Account #" + account.getAccountNumber() + " - Statement", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel info = new JPanel(new GridLayout(0, 2, 16, 4));
        info.setBorder(BorderFactory.createEmptyBorder(8, 4, 12, 4));
        add2(info, "Account number", account.getAccountNumber());
        add2(info, "Holder", account.getHolderName());
        add2(info, "Type", account.getType().label());
        add2(info, account.getType().detailLabel(), account.getDetail());
        add2(info, "Opened", UiUtils.DATE.format(account.getOpenedAt()));
        add2(info, "Current balance", UiUtils.money(account.getBalance()));
        add2(info, "Minimum balance", UiUtils.money(account.getMinimumBalance()));
        add2(info, "Available to withdraw", UiUtils.money(account.getWithdrawableAmount()));
        if (account instanceof SavingsAccount savings) {
            add2(info, "Balance with 5% interest", UiUtils.money(savings.getNetBalance()));
        }

        List<Transaction> rows = new ArrayList<>(account.getTransactions());
        Collections.reverse(rows);
        JTable table = new JTable(new StatementModel(rows));
        table.setFillsViewportHeight(true);
        table.setRowHeight(table.getRowHeight() + 6);
        table.getColumnModel().getColumn(0).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setPreferredWidth(130);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(2).setCellRenderer(right);
        table.getColumnModel().getColumn(3).setCellRenderer(right);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        buttons.add(close);

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        JPanel north = new JPanel(new BorderLayout());
        north.add(UiUtils.heading("Account Statement"), BorderLayout.NORTH);
        north.add(info, BorderLayout.CENTER);
        root.add(north, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(640, 260));
        root.add(scroll, BorderLayout.CENTER);
        root.add(buttons, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(close);
        UiUtils.closeOnEscape(this);
        pack();
        setLocationRelativeTo(owner);
    }

    private static void add2(JPanel panel, String label, String value) {
        JLabel l = new JLabel(label + ":");
        l.setForeground(java.awt.Color.GRAY);
        panel.add(l);
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(java.awt.Font.BOLD));
        panel.add(v);
    }

    private static final class StatementModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final transient List<Transaction> rows;

        StatementModel(List<Transaction> rows) {
            this.rows = rows;
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Transaction t = rows.get(row);
            return switch (column) {
                case 0 -> UiUtils.DATE_TIME.format(t.time());
                case 1 -> t.type().label();
                case 2 -> sign(t) + UiUtils.money(t.amount());
                case 3 -> UiUtils.money(t.balanceAfter());
                default -> t.note();
            };
        }

        private static String sign(Transaction t) {
            return switch (t.type()) {
                case WITHDRAWAL, TRANSFER_OUT -> "- ";
                default -> "+ ";
            };
        }
    }
}
