package com.bankingsystem.model;

import com.bankingsystem.exception.InvalidAccountDataException;

import java.time.LocalDateTime;

/** Current (business) account: minimum balance 5000 and a trade licence number. */
public class CurrentAccount extends BankAccount {
    private String tradeLicenseNumber;

    public CurrentAccount(String number, String holder, LocalDateTime openedAt, String tradeLicenseNumber) {
        super(number, holder, openedAt);
        this.tradeLicenseNumber = tradeLicenseNumber;
    }

    @Override
    public AccountType getType() {
        return AccountType.CURRENT;
    }

    @Override
    public String getDetail() {
        return tradeLicenseNumber;
    }

    @Override
    public void setDetail(String value) throws InvalidAccountDataException {
        this.tradeLicenseNumber = requireText(value, "Trade licence number");
    }
}
