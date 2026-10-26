package com.bankingsystem.exception;

/** Thrown when a withdrawal exceeds the per-transaction limit of the account. */
public class WithdrawLimitExceededException extends BankException {
    private static final long serialVersionUID = 2L;

    public WithdrawLimitExceededException(String message) {
        super(message);
    }
}
