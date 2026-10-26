package com.bankingsystem.service;

import com.bankingsystem.exception.StorageException;
import com.bankingsystem.model.BankAccount;

import java.util.Collection;
import java.util.List;

/** Where the bank keeps its data between runs. */
public interface AccountStore {
    List<BankAccount> load() throws StorageException;

    void save(Collection<BankAccount> accounts) throws StorageException;
}
