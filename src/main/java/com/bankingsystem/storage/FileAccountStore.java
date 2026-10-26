package com.bankingsystem.storage;

import com.bankingsystem.exception.StorageException;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.BankAccount;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.TransactionType;
import com.bankingsystem.service.AccountStore;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores accounts and their statements in a plain, human-readable tab-separated text file.
 * <p>
 * This replaces Java object serialization: the file is safe to load (no arbitrary object
 * deserialization), survives code changes, and can be opened in any text editor.
 * Writes go to a temporary file first and then replace the real one, and the previous
 * version is kept as {@code <file>.bak}, so a crash mid-write cannot destroy the data.
 */
public class FileAccountStore implements AccountStore {
    private static final String HEADER = "#BANKING-SYSTEM-DATA\t2";

    private final Path file;

    public FileAccountStore(Path file) {
        this.file = file;
    }

    public Path getFile() {
        return file;
    }

    @Override
    public List<BankAccount> load() throws StorageException {
        if (!Files.exists(file)) {
            return new ArrayList<>();
        }
        Map<String, BankAccount> accounts = new LinkedHashMap<>();
        Map<String, List<Transaction>> statements = new LinkedHashMap<>();
        try (BufferedReader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line = in.readLine();
            if (line == null || !line.startsWith("#BANKING-SYSTEM-DATA")) {
                throw new StorageException("Not a banking data file: " + file, null);
            }
            int lineNo = 1;
            while ((line = in.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split("\t", -1);
                try {
                    switch (f[0]) {
                        case "A" -> readAccount(f, accounts, statements);
                        case "T" -> readTransaction(f, statements);
                        default -> throw new IllegalArgumentException("unknown record '" + f[0] + "'");
                    }
                } catch (RuntimeException e) {
                    throw new StorageException("Corrupt data file " + file + " at line " + lineNo
                            + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new StorageException("Could not read " + file + ": " + e.getMessage(), e);
        }
        for (Map.Entry<String, BankAccount> e : accounts.entrySet()) {
            BankAccount a = e.getValue();
            a.restoreState(a.getBalance(), statements.getOrDefault(e.getKey(), List.of()));
        }
        return new ArrayList<>(accounts.values());
    }

    private void readAccount(String[] f, Map<String, BankAccount> accounts,
                             Map<String, List<Transaction>> statements) {
        if (f.length != 7) {
            throw new IllegalArgumentException("expected 7 fields but found " + f.length);
        }
        String number = unescape(f[1]);
        AccountType type = AccountType.valueOf(f[2]);
        BankAccount account = type.create(number, unescape(f[3]), unescape(f[4]), LocalDateTime.parse(f[6]));
        // keep the stored balance until the statement has been attached
        account.restoreState(new BigDecimal(f[5]), List.of());
        accounts.put(number, account);
        statements.put(number, new ArrayList<>());
    }

    private void readTransaction(String[] f, Map<String, List<Transaction>> statements) {
        if (f.length != 7) {
            throw new IllegalArgumentException("expected 7 fields but found " + f.length);
        }
        List<Transaction> list = statements.get(unescape(f[1]));
        if (list == null) {
            throw new IllegalArgumentException("transaction for unknown account " + unescape(f[1]));
        }
        list.add(new Transaction(LocalDateTime.parse(f[2]), TransactionType.valueOf(f[3]),
                new BigDecimal(f[4]), new BigDecimal(f[5]), unescape(f[6])));
    }

    @Override
    public synchronized void save(Collection<BankAccount> accounts) throws StorageException {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            try (BufferedWriter out = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                out.write(HEADER);
                out.newLine();
                for (BankAccount a : accounts) {
                    out.write(String.join("\t", "A", escape(a.getAccountNumber()), a.getType().name(),
                            escape(a.getHolderName()), escape(a.getDetail()), a.getBalance().toPlainString(),
                            a.getOpenedAt().toString()));
                    out.newLine();
                    for (Transaction t : a.getTransactions()) {
                        out.write(String.join("\t", "T", escape(a.getAccountNumber()), t.time().toString(),
                                t.type().name(), t.amount().toPlainString(), t.balanceAfter().toPlainString(),
                                escape(t.note())));
                        out.newLine();
                    }
                }
            }
            if (Files.exists(file)) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Could not save data to " + file + ": " + e.getMessage(), e);
        }
    }

    // ---- escaping so tabs / new lines inside names cannot break the format ----------

    static String escape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\t' -> sb.append("\\t");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    static String unescape(String s) {
        if (s.indexOf('\\') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 't' -> sb.append('\t');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
