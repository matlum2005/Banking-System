package com.bankingsystem.model;

public enum TransactionType {
    OPENING("Opening deposit"),
    DEPOSIT("Deposit"),
    WITHDRAWAL("Withdrawal"),
    TRANSFER_IN("Transfer in"),
    TRANSFER_OUT("Transfer out");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
