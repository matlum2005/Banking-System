package com.bankingsystem.service;

import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.exception.BankException;
import com.bankingsystem.exception.InsufficientFundsException;
import com.bankingsystem.exception.InvalidAccountDataException;
import com.bankingsystem.exception.InvalidAmountException;
import com.bankingsystem.exception.StorageException;
import com.bankingsystem.exception.WithdrawLimitExceededException;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The bank: owns all accounts and enforces the rules. Every change is saved to the
 * {@link AccountStore} straight away, so nothing is lost if the program is closed or crashes.
 * There is no hard cap on the number of accounts (the old version stopped at 100).
 */
public class Bank {
    private static final int FIRST_ACCOUNT_NUMBER = 10001;

    private final Map<String, BankAccount> accounts = new LinkedHashMap<>();
    private final AccountStore store;
    private long nextNumber = FIRST_ACCOUNT_NUMBER;

    /** An in-memory bank (used by tests). */
    public Bank() {
        this.store = null;
    }

    /** Loads an existing bank from {@code store}. */
    public Bank(AccountStore store) throws StorageException {
        this.store = store;
        for (BankAccount account : store.load()) {
            accounts.put(account.getAccountNumber(), account);
            try {
                nextNumber = Math.max(nextNumber, Long.parseLong(account.getAccountNumber()) + 1);
            } catch (NumberFormatException ignored) {
                // non-numeric legacy numbers do not influence the sequence
            }
        }
    }

    // ---- accounts ------------------------------------------------------------------

    public synchronized BankAccount openAccount(AccountType type, String holderName, BigDecimal initialBalance,
                                                String detail) throws BankException {
        if (type == null) {
            throw new InvalidAccountDataException("Account type is required");
        }
        if (initialBalance == null) {
            throw new InvalidAccountDataException("Opening balance is required");
        }
        String number = String.valueOf(nextNumber);
        BankAccount account;
        try {
            account = type.create(number, "", detail == null ? "" : detail, LocalDateTime.now().withNano(0));
        } catch (IllegalArgumentException e) {
            throw new InvalidAccountDataException(type.detailLabel() + " must be a valid number");
        }
        account.setHolderName(holderName);
        account.setDetail(detail);
        account.open(initialBalance);

        accounts.put(number, account);
        nextNumber++;
        persist();
        return account;
    }

    public synchronized BankAccount getAccount(String accountNumber) throws AccountNotFoundException {
        String key = accountNumber == null ? "" : accountNumber.trim();
        if (key.isEmpty()) {
            throw new AccountNotFoundException("Please enter an account number");
        }
        BankAccount account = accounts.get(key);
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + key);
        }
        return account;
    }

    public synchronized void updateAccount(String accountNumber, String holderName, String detail)
            throws BankException {
        BankAccount account = getAccount(accountNumber);
        // validate both first so a bad second field does not leave a half-applied change
        String oldName = account.getHolderName();
        String oldDetail = account.getDetail();
        try {
            account.setHolderName(holderName);
            account.setDetail(detail);
        } catch (InvalidAccountDataException e) {
            account.setHolderName(oldName);
            account.setDetail(oldDetail);
            throw e;
        }
        persist();
    }

    public synchronized BankAccount closeAccount(String accountNumber) throws BankException {
        BankAccount account = getAccount(accountNumber);
        accounts.remove(account.getAccountNumber());
        persist();
        return account;
    }

    // ---- money movement ------------------------------------------------------------

    public synchronized void deposit(String accountNumber, BigDecimal amount) throws BankException {
        getAccount(accountNumber).deposit(amount);
        persist();
    }

    public synchronized void withdraw(String accountNumber, BigDecimal amount) throws BankException {
        getAccount(accountNumber).withdraw(amount);
        persist();
    }

    /** Moves money between two accounts; either both sides change or neither does. */
    public synchronized void transfer(String fromNumber, String toNumber, BigDecimal amount) throws BankException {
        BankAccount from = getAccount(fromNumber);
        BankAccount to = getAccount(toNumber);
        if (from == to) {
            throw new InvalidAccountDataException("Cannot transfer to the same account");
        }
        from.checkCanWithdraw(amount); // validates before anything is changed
        from.debitForTransfer(amount, to.getAccountNumber());
        to.creditFromTransfer(amount, from.getAccountNumber());
        persist();
    }

    // ---- queries -------------------------------------------------------------------

    public synchronized List<BankAccount> getAccounts() {
        return Collections.unmodifiableList(new ArrayList<>(accounts.values()));
    }

    /** Accounts whose number, holder, type or detail contains {@code query} (case-insensitive). */
    public synchronized List<BankAccount> search(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty()) {
            return getAccounts();
        }
        List<BankAccount> result = new ArrayList<>();
        for (BankAccount a : accounts.values()) {
            if (a.getAccountNumber().contains(q)
                    || a.getHolderName().toLowerCase(Locale.ROOT).contains(q)
                    || a.getType().label().toLowerCase(Locale.ROOT).contains(q)
                    || a.getDetail().toLowerCase(Locale.ROOT).contains(q)) {
                result.add(a);
            }
        }
        return result;
    }

    public synchronized int getAccountCount() {
        return accounts.size();
    }

    public synchronized BigDecimal getTotalBalance() {
        BigDecimal total = BankAccount.ZERO;
        for (BankAccount a : accounts.values()) {
            total = total.add(a.getBalance());
        }
        return total;
    }

    // ---- persistence ---------------------------------------------------------------

    private void persist() throws StorageException {
        if (store != null) {
            store.save(accounts.values());
        }
    }
}
