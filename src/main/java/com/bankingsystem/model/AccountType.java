package com.bankingsystem.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** The kinds of account the bank offers, with their minimum balance and a factory. */
public enum AccountType {
    SAVINGS("Savings", "2000", "Maximum withdraw limit") {
        @Override
        public BankAccount create(String number, String holder, String detail, LocalDateTime openedAt) {
            return new SavingsAccount(number, holder, openedAt, parseLimit(detail));
        }
    },
    CURRENT("Current", "5000", "Trade licence number") {
        @Override
        public BankAccount create(String number, String holder, String detail, LocalDateTime openedAt) {
            return new CurrentAccount(number, holder, openedAt, detail);
        }
    },
    STUDENT("Student", "100", "Institution name") {
        @Override
        public BankAccount create(String number, String holder, String detail, LocalDateTime openedAt) {
            return new StudentAccount(number, holder, openedAt, detail);
        }
    };

    private final String label;
    private final BigDecimal minimumBalance;
    private final String detailLabel;

    AccountType(String label, String minimumBalance, String detailLabel) {
        this.label = label;
        this.minimumBalance = new BigDecimal(minimumBalance).setScale(2);
        this.detailLabel = detailLabel;
    }

    /** Builds an account of this type; {@code detail} is the type-specific extra field. */
    public abstract BankAccount create(String number, String holder, String detail, LocalDateTime openedAt);

    public String label() {
        return label;
    }

    public BigDecimal minimumBalance() {
        return minimumBalance;
    }

    /** Label of the type-specific extra field (limit, licence, institution). */
    public String detailLabel() {
        return detailLabel;
    }

    @Override
    public String toString() {
        return label;
    }

    private static BigDecimal parseLimit(String detail) {
        try {
            return new BigDecimal(detail.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Withdraw limit must be a number: " + detail, e);
        }
    }
}
