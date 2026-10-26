package com.bankingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bankingsystem.exception.AccountNotFoundException;
import com.bankingsystem.exception.InsufficientFundsException;
import com.bankingsystem.exception.InvalidAccountDataException;
import com.bankingsystem.exception.InvalidAmountException;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAccount;
import com.bankingsystem.model.CurrentAccount;
import com.bankingsystem.model.StudentAccount;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.service.Bank;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankTest {
    private Bank bank;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @BeforeEach
    void setUp() {
        bank = new Bank();
    }

    @Test
    void studentOpensAsStudentAccountNotCurrent() throws Exception {
        // regression: the old "Add Student Account" screen created CurrentAccounts (and created them twice)
        BankAccount a = bank.openAccount(AccountType.STUDENT, "Sara", bd("100"), "MIT");
        assertTrue(a instanceof StudentAccount);
        assertEquals(1, bank.getAccountCount());
    }

    @Test
    void accountNumbersAreUniqueAndThereIsNoHundredAccountLimit() throws Exception {
        Set<String> numbers = new HashSet<>();
        for (int i = 0; i < 250; i++) {
            numbers.add(bank.openAccount(AccountType.CURRENT, "Holder " + i, bd("5000"), "L" + i).getAccountNumber());
        }
        assertEquals(250, numbers.size());
        assertEquals(250, bank.getAccountCount());
    }

    @Test
    void invalidOpeningDataLeavesNoTraceInTheBank() {
        assertThrows(InvalidAccountDataException.class,
                () -> bank.openAccount(AccountType.SAVINGS, "  ", bd("5000"), "1000"));
        assertThrows(InvalidAccountDataException.class,
                () -> bank.openAccount(AccountType.SAVINGS, "Ali", bd("100"), "1000"));
        assertThrows(InvalidAccountDataException.class,
                () -> bank.openAccount(AccountType.SAVINGS, "Ali", bd("5000"), "not-a-number"));
        assertThrows(InvalidAccountDataException.class,
                () -> bank.openAccount(AccountType.CURRENT, "Ali", bd("5000"), ""));
        assertEquals(0, bank.getAccountCount());
    }

    @Test
    void depositWithdrawAndUnknownAccount() throws Exception {
        BankAccount a = bank.openAccount(AccountType.CURRENT, "Ali", bd("5000"), "L1");
        bank.deposit(a.getAccountNumber(), bd("250.50"));
        bank.withdraw(a.getAccountNumber(), bd("100"));
        assertEquals(bd("5150.50"), a.getBalance());
        assertThrows(AccountNotFoundException.class, () -> bank.deposit("99999", bd("1")));
        assertThrows(AccountNotFoundException.class, () -> bank.withdraw("", bd("1")));
        assertThrows(InvalidAmountException.class, () -> bank.deposit(a.getAccountNumber(), bd("0")));
    }

    @Test
    void transferMovesMoneyAndLogsBothSides() throws Exception {
        BankAccount from = bank.openAccount(AccountType.CURRENT, "A", bd("10000"), "L1");
        BankAccount to = bank.openAccount(AccountType.STUDENT, "B", bd("500"), "MIT");
        bank.transfer(from.getAccountNumber(), to.getAccountNumber(), bd("1500"));
        assertEquals(bd("8500.00"), from.getBalance());
        assertEquals(bd("2000.00"), to.getBalance());
        var out = from.getTransactions().get(from.getTransactions().size() - 1);
        var in = to.getTransactions().get(to.getTransactions().size() - 1);
        assertEquals(TransactionType.TRANSFER_OUT, out.type());
        assertEquals(TransactionType.TRANSFER_IN, in.type());
        assertEquals("To " + to.getAccountNumber(), out.note());
        assertEquals("From " + from.getAccountNumber(), in.note());
        assertEquals(bd("10500.00"), bank.getTotalBalance());
    }

    @Test
    void failedTransferChangesNeitherAccount() throws Exception {
        BankAccount from = bank.openAccount(AccountType.CURRENT, "A", bd("5000"), "L1");
        BankAccount to = bank.openAccount(AccountType.CURRENT, "B", bd("5000"), "L2");
        assertThrows(InsufficientFundsException.class,
                () -> bank.transfer(from.getAccountNumber(), to.getAccountNumber(), bd("1")));
        assertThrows(AccountNotFoundException.class,
                () -> bank.transfer(from.getAccountNumber(), "00000", bd("1")));
        assertThrows(InvalidAccountDataException.class,
                () -> bank.transfer(from.getAccountNumber(), from.getAccountNumber(), bd("1")));
        assertEquals(bd("5000.00"), from.getBalance());
        assertEquals(bd("5000.00"), to.getBalance());
        assertEquals(1, from.getTransactions().size());
        assertEquals(1, to.getTransactions().size());
    }

    @Test
    void closeAccountRemovesIt() throws Exception {
        BankAccount a = bank.openAccount(AccountType.CURRENT, "A", bd("5000"), "L1");
        bank.closeAccount(a.getAccountNumber());
        assertEquals(0, bank.getAccountCount());
        assertThrows(AccountNotFoundException.class, () -> bank.getAccount(a.getAccountNumber()));
    }

    @Test
    void closedAccountNumberIsNotReused() throws Exception {
        BankAccount first = bank.openAccount(AccountType.CURRENT, "A", bd("5000"), "L1");
        bank.closeAccount(first.getAccountNumber());
        BankAccount second = bank.openAccount(AccountType.CURRENT, "B", bd("5000"), "L2");
        assertNotEquals(first.getAccountNumber(), second.getAccountNumber());
    }

    @Test
    void updateChangesNameAndDetailAndRollsBackOnError() throws Exception {
        BankAccount a = bank.openAccount(AccountType.CURRENT, "Old Name", bd("5000"), "L1");
        bank.updateAccount(a.getAccountNumber(), "New Name", "L2");
        assertEquals("New Name", a.getHolderName());
        assertEquals("L2", ((CurrentAccount) a).getDetail());

        assertThrows(InvalidAccountDataException.class, () -> bank.updateAccount(a.getAccountNumber(), "Other", " "));
        assertEquals("New Name", a.getHolderName()); // name must not be half-applied
        assertEquals("L2", a.getDetail());
    }

    @Test
    void searchMatchesNumberNameTypeAndDetailIgnoringCase() throws Exception {
        BankAccount a = bank.openAccount(AccountType.CURRENT, "Samira Khan", bd("5000"), "TL-77");
        bank.openAccount(AccountType.STUDENT, "Bilal", bd("500"), "Oxford");
        assertEquals(1, bank.search("samira").size());
        assertEquals(1, bank.search(a.getAccountNumber()).size());
        assertEquals(1, bank.search("student").size());
        assertEquals(1, bank.search("oxford").size());
        assertEquals(1, bank.search("tl-77").size());
        assertEquals(2, bank.search("  ").size());
        assertEquals(0, bank.search("zzz").size());
    }
}
