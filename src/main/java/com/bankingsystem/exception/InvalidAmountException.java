package com.bankingsystem.exception;

/** Thrown when an amount is missing, malformed, zero or negative. */
public class InvalidAmountException extends BankException {
    private static final long serialVersionUID = 2L;

    public InvalidAmountException(String message) {
        super(message);
    }
}
