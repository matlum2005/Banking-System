package com.bankingsystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bankingsystem.exception.StorageException;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAccount;
import com.bankingsystem.service.Bank;
import com.bankingsystem.storage.FileAccountStore;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileAccountStoreTest {
    @TempDir
    Path dir;

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    @Test
    void everyChangeIsSavedImmediatelyAndReloadsIdentically() throws Exception {
        Path file = dir.resolve("data").resolve("bank.tsv");
        Bank bank = new Bank(new FileAccountStore(file));
        BankAccount a = bank.openAccount(AccountType.SAVINGS, "Soad", bd("6000"), "5000");
        BankAccount b = bank.openAccount(AccountType.CURRENT, "Samira", bd("10000"), "TL-1");
        bank.deposit(a.getAccountNumber(), bd("250.75"));
        bank.transfer(b.getAccountNumber(), a.getAccountNumber(), bd("1000"));

        // "restart": a brand new Bank reading the same file, with no explicit save call in between
        Bank reloaded = new Bank(new FileAccountStore(file));
        assertEquals(2, reloaded.getAccountCount());
        BankAccount a2 = reloaded.getAccount(a.getAccountNumber());
        BankAccount b2 = reloaded.getAccount(b.getAccountNumber());
        assertEquals(bd("7250.75"), a2.getBalance());
        assertEquals(bd("9000.00"), b2.getBalance());
        assertEquals(AccountType.SAVINGS, a2.getType());
        assertEquals("5000.00", a2.getDetail());
        assertEquals("TL-1", b2.getDetail());
        assertEquals(a.getTransactions(), a2.getTransactions());
        assertEquals(b.getTransactions(), b2.getTransactions());
        assertEquals(a.getOpenedAt(), a2.getOpenedAt());
    }

    @Test
    void accountNumberSequenceContinuesAfterRestart() throws Exception {
        Path file = dir.resolve("bank.tsv");
        Bank bank = new Bank(new FileAccountStore(file));
        BankAccount first = bank.openAccount(AccountType.CURRENT, "A", bd("5000"), "L1");
        Bank reloaded = new Bank(new FileAccountStore(file));
        BankAccount second = reloaded.openAccount(AccountType.CURRENT, "B", bd("5000"), "L2");
        assertEquals(Long.parseLong(first.getAccountNumber()) + 1, Long.parseLong(second.getAccountNumber()));
    }

    @Test
    void tabsNewlinesAndBackslashesInTextSurvive() throws Exception {
        Path file = dir.resolve("bank.tsv");
        Bank bank = new Bank(new FileAccountStore(file));
        String nasty = "Name\twith\ttabs\nand\\newline";
        BankAccount a = bank.openAccount(AccountType.STUDENT, nasty, bd("100"), "Uni\\Campus\tA");
        Bank reloaded = new Bank(new FileAccountStore(file));
        BankAccount a2 = reloaded.getAccount(a.getAccountNumber());
        assertEquals(nasty, a2.getHolderName());
        assertEquals("Uni\\Campus\tA", a2.getDetail());
    }

    @Test
    void closedAccountsStayClosedAfterRestart() throws Exception {
        Path file = dir.resolve("bank.tsv");
        Bank bank = new Bank(new FileAccountStore(file));
        BankAccount a = bank.openAccount(AccountType.CURRENT, "A", bd("5000"), "L1");
        bank.openAccount(AccountType.CURRENT, "B", bd("5000"), "L2");
        bank.closeAccount(a.getAccountNumber());
        assertEquals(1, new Bank(new FileAccountStore(file)).getAccountCount());
    }

    @Test
    void missingFileMeansEmptyBank() throws Exception {
        assertEquals(0, new Bank(new FileAccountStore(dir.resolve("nope.tsv"))).getAccountCount());
    }

    @Test
    void corruptFileIsReportedAndNeverOverwrittenWithAnEmptyBank() throws Exception {
        // regression: the old FileIO swallowed every error and silently started with an empty bank
        Path file = dir.resolve("bank.tsv");
        Files.writeString(file, "#BANKING-SYSTEM-DATA\t2\nA\t1\tNOT_A_TYPE\tx\ty\t10\t2025-01-01T00:00:00\n",
                StandardCharsets.UTF_8);
        String before = Files.readString(file);
        assertThrows(StorageException.class, () -> new Bank(new FileAccountStore(file)));
        assertEquals(before, Files.readString(file));

        Path garbage = dir.resolve("garbage.tsv");
        Files.writeString(garbage, "this is not a bank file");
        assertThrows(StorageException.class, () -> new Bank(new FileAccountStore(garbage)));
    }

    @Test
    void previousVersionIsKeptAsBackup() throws Exception {
        Path file = dir.resolve("bank.tsv");
        Bank bank = new Bank(new FileAccountStore(file));
        bank.openAccount(AccountType.CURRENT, "A", bd("5000"), "L1");
        bank.openAccount(AccountType.CURRENT, "B", bd("5000"), "L2");
        Path backup = dir.resolve("bank.tsv.bak");
        assertTrue(Files.exists(backup));
        assertEquals(1, new Bank(new FileAccountStore(backup)).getAccountCount());
        assertTrue(Files.notExists(dir.resolve("bank.tsv.tmp")));
    }
}
