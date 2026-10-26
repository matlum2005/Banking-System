package com.bankingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bankingsystem.exception.InsufficientFundsException;
import com.bankingsystem.exception.InvalidAccountDataException;
import com.bankingsystem.exception.InvalidAmountException;
import com.bankingsystem.exception.WithdrawLimitExceededException;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAccount;
import com.bankingsystem.model.SavingsAccount;
import com.bankingsystem.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AccountRulesTest {
    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BankAccount open(AccountType type, String detail, String initial) throws Exception {
        BankAccount a = type.create("1", "Test", detail, LocalDateTime.now());
        a.open(bd(initial));
        return a;
    }

    @Test
    void minimumOpeningBalancePerType() {
        assertThrows(InvalidAccountDataException.class, () -> open(AccountType.SAVINGS, "500", "1999.99"));
        assertThrows(InvalidAccountDataException.class, () -> open(AccountType.CURRENT, "L-1", "4999"));
        assertThrows(InvalidAccountDataException.class, () -> open(AccountType.STUDENT, "MIT", "99"));
    }

    @Test
    void studentAccountCanOpenWith100() throws Exception {
        // regression: the old code forced student accounts to the savings minimum of 2000
        BankAccount a = open(AccountType.STUDENT, "MIT", "100");
        assertEquals(bd("100.00"), a.getBalance());
    }

    @Test
    void depositAndWithdrawKeepExactDecimals() throws Exception {
        BankAccount a = open(AccountType.CURRENT, "L-1", "5000");
        a.deposit(bd("0.10"));
        a.deposit(bd("0.20"));
        assertEquals(bd("5000.30"), a.getBalance()); // would be 5000.300000000001-ish with double
    }

    @Test
    void zeroAndNegativeAmountsAreRejected() throws Exception {
        BankAccount a = open(AccountType.SAVINGS, "1000", "5000");
        assertThrows(InvalidAmountException.class, () -> a.deposit(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> a.deposit(bd("-5")));
        assertThrows(InvalidAmountException.class, () -> a.withdraw(BigDecimal.ZERO));
        assertThrows(InvalidAmountException.class, () -> a.withdraw(bd("-5")));
    }

    @Test
    void withdrawMayTouchMinimumBalanceButNotGoBelow() throws Exception {
        BankAccount a = open(AccountType.SAVINGS, "10000", "5000");
        a.withdraw(bd("3000")); // leaves exactly 2000 = allowed
        assertEquals(bd("2000.00"), a.getBalance());
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(bd("0.01")));
        assertEquals(bd("2000.00"), a.getBalance());
    }

    @Test
    void savingsWithdrawLimitIsInclusive() throws Exception {
        BankAccount a = open(AccountType.SAVINGS, "1000", "9000");
        a.withdraw(bd("1000")); // exactly the limit is allowed
        assertThrows(WithdrawLimitExceededException.class, () -> a.withdraw(bd("1000.01")));
    }

    @Test
    void studentAccountUsesDefaultLimit() throws Exception {
        BankAccount a = open(AccountType.STUDENT, "MIT", "50000");
        a.withdraw(bd("20000"));
        assertThrows(WithdrawLimitExceededException.class, () -> a.withdraw(bd("20000.01")));
    }

    @Test
    void failedWithdrawalChangesNothing() throws Exception {
        BankAccount a = open(AccountType.CURRENT, "L-1", "6000");
        int before = a.getTransactions().size();
        assertThrows(InsufficientFundsException.class, () -> a.withdraw(bd("1001")));
        assertEquals(bd("6000.00"), a.getBalance());
        assertEquals(before, a.getTransactions().size());
    }

    @Test
    void statementRecordsEveryMovement() throws Exception {
        BankAccount a = open(AccountType.CURRENT, "L-1", "6000");
        a.deposit(bd("100"));
        a.withdraw(bd("50"));
        var tx = a.getTransactions();
        assertEquals(3, tx.size());
        assertEquals(TransactionType.OPENING, tx.get(0).type());
        assertEquals(TransactionType.DEPOSIT, tx.get(1).type());
        assertEquals(TransactionType.WITHDRAWAL, tx.get(2).type());
        assertEquals(bd("6050.00"), tx.get(2).balanceAfter());
    }

    @Test
    void netBalanceAddsFivePercent() throws Exception {
        SavingsAccount a = (SavingsAccount) open(AccountType.SAVINGS, "1000", "2000");
        assertEquals(bd("2100.00"), a.getNetBalance());
    }

    @Test
    void detailValidation() throws Exception {
        BankAccount savings = open(AccountType.SAVINGS, "1000", "2000");
        assertThrows(InvalidAccountDataException.class, () -> savings.setDetail("abc"));
        assertThrows(InvalidAccountDataException.class, () -> savings.setDetail("0"));
        savings.setDetail("2500");
        assertEquals("2500.00", savings.getDetail());
        BankAccount current = open(AccountType.CURRENT, "L-1", "5000");
        assertThrows(InvalidAccountDataException.class, () -> current.setDetail("  "));
        assertThrows(InvalidAccountDataException.class, () -> current.setHolderName(""));
    }
}
