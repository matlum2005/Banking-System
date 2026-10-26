package com.bankingsystem.ui;

import com.bankingsystem.exception.BankException;
import com.bankingsystem.model.BankAccount;
import com.bankingsystem.service.AuthService;
import com.bankingsystem.service.Bank;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** The main screen: searchable account table plus all banking actions. */
public class MainFrame extends JFrame {
    private static final long serialVersionUID = 2L;
    private static final String[] COLUMNS = {"Account No.", "Holder", "Type", "Balance", "Details", "Opened"};

    private final transient Bank bank;
    private final transient AuthService auth;
    private final String user;
    private final transient Runnable onLogout;

    private final AccountTableModel model = new AccountTableModel();
    private final JTable table = new JTable(model);
    private final JTextField searchField = new JTextField(20);
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton editButton = new JButton("Edit");
    private final JButton deleteButton = new JButton("Delete");
    private final JButton historyButton = new JButton("Statement");

    @SuppressWarnings("this-escape")
    public MainFrame(Bank bank, AuthService auth, String user, Runnable onLogout) {
        super("Banking System");
        this.bank = bank;
        this.auth = auth;
        this.user = user;
        this.onLogout = onLogout;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        if (UiUtils.appImage() != null) {
            setIconImage(UiUtils.appImage());
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });

        setJMenuBar(buildMenuBar());
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 6, 12));
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTable(), BorderLayout.CENTER);
        root.add(statusLabel, BorderLayout.SOUTH);
        setContentPane(root);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                refresh();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> updateButtons());

        refresh();
        setMinimumSize(new Dimension(900, 460));
        pack();
        setLocationRelativeTo(null);
    }

    // ---- construction --------------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenu file = new JMenu("File");
        JMenuItem changePassword = new JMenuItem("Change password...");
        changePassword.addActionListener(e -> ChangePasswordDialog.show(this, auth, user));
        JMenuItem logout = new JMenuItem("Log out");
        logout.addActionListener(e -> {
            dispose();
            onLogout.run();
        });
        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> exit());
        file.add(changePassword);
        file.add(logout);
        file.addSeparator();
        file.add(exit);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> UiUtils.info(this, "Banking System 2.0.0\nJava " + System.getProperty("java.version")
                + "\nSigned in as: " + user));
        help.add(about);

        JMenuBar bar = new JMenuBar();
        bar.add(file);
        bar.add(help);
        return bar;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 8));

        JPanel titleRow = new JPanel(new BorderLayout(10, 0));
        titleRow.add(new JLabel(UiUtils.scaledIcon(40)), BorderLayout.WEST);
        titleRow.add(UiUtils.heading("Banking System"), BorderLayout.CENTER);
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        searchBox.add(new JLabel("Search:"));
        searchField.setToolTipText("Search by account number, holder name, type or details");
        searchBox.add(searchField);
        titleRow.add(searchBox, BorderLayout.EAST);
        header.add(titleRow, BorderLayout.NORTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton add = new JButton("Add Account");
        JButton deposit = new JButton("Deposit");
        JButton withdraw = new JButton("Withdraw");
        JButton transfer = new JButton("Transfer");
        add.addActionListener(e -> addAccount());
        editButton.addActionListener(e -> editAccount());
        deleteButton.addActionListener(e -> deleteAccount());
        deposit.addActionListener(e -> openTransaction(TransactionDialog.Mode.DEPOSIT));
        withdraw.addActionListener(e -> openTransaction(TransactionDialog.Mode.WITHDRAW));
        transfer.addActionListener(e -> openTransaction(TransactionDialog.Mode.TRANSFER));
        historyButton.addActionListener(e -> showStatement());
        actions.add(add);
        actions.add(editButton);
        actions.add(deleteButton);
        actions.add(Box.createHorizontalStrut(14));
        actions.add(deposit);
        actions.add(withdraw);
        actions.add(transfer);
        actions.add(Box.createHorizontalStrut(14));
        actions.add(historyButton);
        header.add(actions, BorderLayout.SOUTH);
        return header;
    }

    private JScrollPane buildTable() {
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setRowHeight(table.getRowHeight() + 8);
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer money = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                setText(value instanceof BigDecimal b ? UiUtils.money(b) : "");
            }
        };
        money.setHorizontalAlignment(SwingConstants.RIGHT);
        TableCellRenderer date = new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void setValue(Object value) {
                setText(value instanceof LocalDateTime d ? UiUtils.DATE.format(d) : "");
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(money);
        table.getColumnModel().getColumn(5).setCellRenderer(date);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(4).setPreferredWidth(300);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && selectedAccount() != null) {
                    showStatement();
                }
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(980, 340));
        return scroll;
    }

    // ---- actions -------------------------------------------------------------------

    private void addAccount() {
        AccountDialog dialog = new AccountDialog(this, bank, null);
        dialog.setVisible(true);
        refresh();
    }

    private void editAccount() {
        BankAccount account = selectedAccount();
        if (account == null) {
            return;
        }
        new AccountDialog(this, bank, account).setVisible(true);
        refresh();
    }

    private void deleteAccount() {
        BankAccount account = selectedAccount();
        if (account == null) {
            return;
        }
        String message = "Delete account #" + account.getAccountNumber() + " (" + account.getHolderName() + ")?\n"
                + "Remaining balance " + UiUtils.money(account.getBalance())
                + " is assumed to be paid out.\nThis cannot be undone.";
        if (UiUtils.confirm(this, message, "Delete account")) {
            try {
                bank.closeAccount(account.getAccountNumber());
            } catch (BankException e) {
                UiUtils.error(this, e.getMessage());
            }
            refresh();
        }
    }

    private void openTransaction(TransactionDialog.Mode mode) {
        BankAccount selected = selectedAccount();
        new TransactionDialog(this, bank, mode, selected == null ? null : selected.getAccountNumber())
                .setVisible(true);
        refresh();
    }

    private void showStatement() {
        BankAccount account = selectedAccount();
        if (account != null) {
            new HistoryDialog(this, account).setVisible(true);
        }
    }

    private void exit() {
        if (UiUtils.confirm(this, "Exit Banking System?\n(All changes are already saved.)", "Exit")) {
            dispose();
            System.exit(0);
        }
    }

    // ---- table state ---------------------------------------------------------------

    private BankAccount selectedAccount() {
        int viewRow = table.getSelectedRow();
        return viewRow < 0 ? null : model.accountAt(table.convertRowIndexToModel(viewRow));
    }

    private void updateButtons() {
        boolean selected = selectedAccount() != null;
        editButton.setEnabled(selected);
        deleteButton.setEnabled(selected);
        historyButton.setEnabled(selected);
    }

    /** Reloads the table from the bank (keeps the selected account selected if it still exists). */
    private void refresh() {
        BankAccount before = selectedAccount();
        String keep = before == null ? null : before.getAccountNumber();
        model.setAccounts(bank.search(searchField.getText()));
        if (keep != null) {
            for (int i = 0; i < model.getRowCount(); i++) {
                if (model.accountAt(i).getAccountNumber().equals(keep)) {
                    int view = table.convertRowIndexToView(i);
                    table.getSelectionModel().setSelectionInterval(view, view);
                    break;
                }
            }
        }
        statusLabel.setText("Accounts: " + bank.getAccountCount()
                + (searchField.getText().isBlank() ? "" : "  (showing " + model.getRowCount() + ")")
                + "     Total balance: " + UiUtils.money(bank.getTotalBalance())
                + "     Signed in as: " + user);
        updateButtons();
    }

    private static final class AccountTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private transient List<BankAccount> accounts = new ArrayList<>();

        void setAccounts(List<BankAccount> list) {
            this.accounts = new ArrayList<>(list);
            fireTableDataChanged();
        }

        BankAccount accountAt(int row) {
            return accounts.get(row);
        }

        @Override
        public int getRowCount() {
            return accounts.size();
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
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 3 -> BigDecimal.class;
                case 5 -> LocalDateTime.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            BankAccount a = accounts.get(row);
            return switch (column) {
                case 0 -> a.getAccountNumber();
                case 1 -> a.getHolderName();
                case 2 -> a.getType().label();
                case 3 -> a.getBalance();
                case 4 -> a.getType().detailLabel() + ": " + a.getDetail();
                default -> a.getOpenedAt();
            };
        }
    }
}
