package com.bankingsystem.exception;

/** Base class for all business-rule failures in the banking system. */
public class BankException extends Exception {
    private static final long serialVersionUID = 2L;

    public BankException(String message) {
        super(message);
    }

    public BankException(String message, Throwable cause) {
        super(message, cause);
    }
}
