package com.bankingsystem.exception;

/** Thrown when bank data cannot be read from or written to disk. */
public class StorageException extends BankException {
    private static final long serialVersionUID = 2L;

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
