package com.bankingsystem.exception;

/** Thrown when account details (name, opening balance, limits...) are invalid. */
public class InvalidAccountDataException extends BankException {
    private static final long serialVersionUID = 2L;

    public InvalidAccountDataException(String message) {
        super(message);
    }
}
