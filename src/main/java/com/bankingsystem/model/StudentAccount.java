package com.bankingsystem.model;

import com.bankingsystem.exception.InvalidAccountDataException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Student account: a savings account with a low minimum balance (100) and an institution. */
public class StudentAccount extends SavingsAccount {
    public static final BigDecimal DEFAULT_WITHDRAW_LIMIT = new BigDecimal("20000");

    private String institutionName;

    public StudentAccount(String number, String holder, LocalDateTime openedAt, String institutionName) {
        super(number, holder, openedAt, DEFAULT_WITHDRAW_LIMIT);
        this.institutionName = institutionName;
    }

    @Override
    public AccountType getType() {
        return AccountType.STUDENT;
    }

    @Override
    public String getDetail() {
        return institutionName;
    }

    @Override
    public void setDetail(String value) throws InvalidAccountDataException {
        this.institutionName = requireText(value, "Institution name");
    }
}
