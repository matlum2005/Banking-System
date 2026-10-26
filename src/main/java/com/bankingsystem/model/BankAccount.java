package com.bankingsystem.model;

import com.bankingsystem.exception.InsufficientFundsException;
import com.bankingsystem.exception.InvalidAccountDataException;
import com.bankingsystem.exception.InvalidAmountException;
import com.bankingsystem.exception.WithdrawLimitExceededException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Base class for every account. Money is always held as {@link BigDecimal} with 2 decimals,
 * so there are no floating-point rounding errors.
 */
public abstract class BankAccount {
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final String accountNumber;
    private final LocalDateTime openedAt;
    private String holderName;
    private BigDecimal balance = ZERO;
    private final List<Transaction> transactions = new ArrayList<>();

    protected BankAccount(String accountNumber, String holderName, LocalDateTime openedAt) {
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
        this.holderName = holderName;
    }

    // ---- type specific -------------------------------------------------------------

    public abstract AccountType getType();

    /** The value of the type-specific field (withdraw limit / licence / institution). */
    public abstract String getDetail();

    /** Changes the type-specific field. */
    public abstract void setDetail(String value) throws InvalidAccountDataException;

    /** Hook for per-transaction rules (e.g. withdraw limit). */
    protected void checkWithdrawRules(BigDecimal amount) throws WithdrawLimitExceededException {
        // no extra rule by default
    }

    public BigDecimal getMinimumBalance() {
        return getType().minimumBalance();
    }

    // ---- operations ----------------------------------------------------------------

    /** Credits the opening balance; must be called once, right after creation. */
    public void open(BigDecimal initialBalance) throws InvalidAccountDataException {
        BigDecimal initial = scale(initialBalance);
        if (initial.compareTo(getMinimumBalance()) < 0) {
            throw new InvalidAccountDataException("Opening balance for a " + getType().label()
                    + " account must be at least " + getMinimumBalance().toPlainString());
        }
        balance = initial;
        record(TransactionType.OPENING, initial, "");
    }

    public void deposit(BigDecimal amount) throws InvalidAmountException {
        BigDecimal value = requirePositive(amount);
        balance = balance.add(value);
        record(TransactionType.DEPOSIT, value, "");
    }

    public void withdraw(BigDecimal amount)
            throws InvalidAmountException, InsufficientFundsException, WithdrawLimitExceededException {
        BigDecimal value = checkCanWithdraw(amount);
        balance = balance.subtract(value);
        record(TransactionType.WITHDRAWAL, value, "");
    }

    /** Validates a withdrawal without changing anything; returns the normalised amount. */
    public BigDecimal checkCanWithdraw(BigDecimal amount)
            throws InvalidAmountException, InsufficientFundsException, WithdrawLimitExceededException {
        BigDecimal value = requirePositive(amount);
        checkWithdrawRules(value);
        if (balance.subtract(value).compareTo(getMinimumBalance()) < 0) {
            throw new InsufficientFundsException("Insufficient balance: this account must keep at least "
                    + getMinimumBalance().toPlainString() + " (available to withdraw: "
                    + getWithdrawableAmount().toPlainString() + ")");
        }
        return value;
    }

    /** Takes money out as part of a transfer; the statement entry names the receiving account. */
    public void debitForTransfer(BigDecimal amount, String toAccount)
            throws InvalidAmountException, InsufficientFundsException, WithdrawLimitExceededException {
        BigDecimal value = checkCanWithdraw(amount);
        balance = balance.subtract(value);
        record(TransactionType.TRANSFER_OUT, value, "To " + toAccount);
    }

    /** Adds money received through a transfer; the statement entry names the sending account. */
    public void creditFromTransfer(BigDecimal amount, String fromAccount) throws InvalidAmountException {
        BigDecimal value = requirePositive(amount);
        balance = balance.add(value);
        record(TransactionType.TRANSFER_IN, value, "From " + fromAccount);
    }

    /** How much can still be taken out before hitting the minimum balance. */
    public BigDecimal getWithdrawableAmount() {
        BigDecimal free = balance.subtract(getMinimumBalance());
        return free.signum() < 0 ? ZERO : free;
    }

    // ---- accessors -----------------------------------------------------------------

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public void setHolderName(String name) throws InvalidAccountDataException {
        this.holderName = requireText(name, "Account holder name");
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getOpenedAt() {
        return openedAt;
    }

    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /** Used by the storage layer to rebuild an account exactly as it was saved. */
    public void restoreState(BigDecimal savedBalance, List<Transaction> savedTransactions) {
        this.balance = scale(savedBalance);
        this.transactions.clear();
        this.transactions.addAll(savedTransactions);
    }

    @Override
    public String toString() {
        return getType().label() + " #" + accountNumber + " (" + holderName + ") balance " + balance.toPlainString();
    }

    // ---- helpers -------------------------------------------------------------------

    private void record(TransactionType type, BigDecimal amount, String note) {
        transactions.add(new Transaction(LocalDateTime.now().withNano(0), type, amount, balance, note));
    }

    public static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    protected static BigDecimal requirePositive(BigDecimal amount) throws InvalidAmountException {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidAmountException("Amount must be greater than zero");
        }
        return scale(amount);
    }

    protected static String requireText(String text, String fieldName) throws InvalidAccountDataException {
        if (text == null || text.isBlank()) {
            throw new InvalidAccountDataException(fieldName + " is required");
        }
        return text.trim();
    }
}
