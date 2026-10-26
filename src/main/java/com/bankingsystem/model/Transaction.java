package com.bankingsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/** One immutable entry in an account's statement. */
public record Transaction(LocalDateTime time, TransactionType type, BigDecimal amount,
                          BigDecimal balanceAfter, String note) {
    public Transaction {
        Objects.requireNonNull(time, "time");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(balanceAfter, "balanceAfter");
        note = note == null ? "" : note;
    }
}
