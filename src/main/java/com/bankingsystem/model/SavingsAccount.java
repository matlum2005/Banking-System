package com.bankingsystem.model;

import com.bankingsystem.exception.InvalidAccountDataException;
import com.bankingsystem.exception.WithdrawLimitExceededException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Savings account: minimum balance 2000, a per-withdrawal limit, 5% interest rate. */
public class SavingsAccount extends BankAccount {
    public static final BigDecimal INTEREST_RATE = new BigDecimal("0.05");

    private BigDecimal withdrawLimit;

    public SavingsAccount(String number, String holder, LocalDateTime openedAt, BigDecimal withdrawLimit) {
        super(number, holder, openedAt);
        this.withdrawLimit = scale(withdrawLimit);
    }

    @Override
    public AccountType getType() {
        return AccountType.SAVINGS;
    }

    public BigDecimal getWithdrawLimit() {
        return withdrawLimit;
    }

    /** Balance plus one year of interest at {@link #INTEREST_RATE}. */
    public BigDecimal getNetBalance() {
        return getBalance().add(getBalance().multiply(INTEREST_RATE)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getDetail() {
        return withdrawLimit.toPlainString();
    }

    @Override
    public void setDetail(String value) throws InvalidAccountDataException {
        BigDecimal limit;
        try {
            limit = new BigDecimal(requireText(value, "Withdraw limit"));
        } catch (NumberFormatException e) {
            throw new InvalidAccountDataException("Withdraw limit must be a number");
        }
        if (limit.signum() <= 0) {
            throw new InvalidAccountDataException("Withdraw limit must be greater than zero");
        }
        this.withdrawLimit = scale(limit);
    }

    @Override
    protected void checkWithdrawRules(BigDecimal amount) throws WithdrawLimitExceededException {
        if (amount.compareTo(withdrawLimit) > 0) {
            throw new WithdrawLimitExceededException("Maximum withdraw limit exceeded (limit per withdrawal: "
                    + withdrawLimit.toPlainString() + ")");
        }
    }
}
