package com.bankingsystem.exception;

/** Thrown when a withdrawal would take the balance below the account's minimum balance. */
public class InsufficientFundsException extends BankException {
    private static final long serialVersionUID = 2L;

    public InsufficientFundsException(String message) {
        super(message);
    }
}
