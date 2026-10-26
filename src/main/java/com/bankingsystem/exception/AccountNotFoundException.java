package com.bankingsystem.exception;

/** Thrown when no account exists for the given account number. */
public class AccountNotFoundException extends BankException {
    private static final long serialVersionUID = 2L;

    public AccountNotFoundException(String message) {
        super(message);
    }
}
