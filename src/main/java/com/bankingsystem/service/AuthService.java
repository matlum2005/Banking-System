package com.bankingsystem.service;

import com.bankingsystem.exception.BankException;
import com.bankingsystem.exception.InvalidAccountDataException;
import com.bankingsystem.exception.StorageException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

/**
 * Login handling. Passwords are never stored or compared in plain text: each user has a random
 * salt and a PBKDF2-HMAC-SHA256 hash in the users file. The first run creates the user
 * {@code admin} with password {@code admin}; the app asks to change it right after the first login.
 */
public class AuthService {
    public static final String DEFAULT_USER = "admin";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    public static final int MIN_PASSWORD_LENGTH = 6;

    private final Path file;
    private final SecureRandom random = new SecureRandom();
    private final Properties users = new Properties();

    public AuthService(Path file) throws StorageException {
        this.file = file;
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                users.load(in);
            } catch (IOException e) {
                throw new StorageException("Could not read " + file + ": " + e.getMessage(), e);
            }
        }
        if (users.isEmpty()) {
            users.setProperty(DEFAULT_USER, hash(DEFAULT_PASSWORD.toCharArray()));
            store();
        }
    }

    public synchronized boolean authenticate(String username, char[] password) {
        String entry = username == null ? null : users.getProperty(username.trim());
        // verify against a dummy hash for unknown users so timing does not reveal valid usernames
        boolean ok = verify(password, entry != null ? entry : hash(new char[0]));
        return entry != null && ok;
    }

    /** True while the user still has the factory password. */
    public synchronized boolean usesDefaultPassword(String username) {
        return DEFAULT_USER.equals(username) && authenticate(username, DEFAULT_PASSWORD.toCharArray());
    }

    public synchronized void changePassword(String username, char[] oldPassword, char[] newPassword)
            throws BankException {
        if (!authenticate(username, oldPassword)) {
            throw new InvalidAccountDataException("Current password is wrong");
        }
        if (newPassword == null || newPassword.length < MIN_PASSWORD_LENGTH) {
            throw new InvalidAccountDataException("New password must be at least " + MIN_PASSWORD_LENGTH
                    + " characters");
        }
        if (Arrays.equals(oldPassword, newPassword)) {
            throw new InvalidAccountDataException("New password must be different from the current one");
        }
        users.setProperty(username.trim(), hash(newPassword));
        store();
    }

    // ---- internals -----------------------------------------------------------------

    private String hash(char[] password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
                + Base64.getEncoder().encodeToString(pbkdf2(password, salt, ITERATIONS));
    }

    private boolean verify(char[] password, String stored) {
        try {
            String[] p = stored.split(":");
            int iterations = Integer.parseInt(p[0]);
            byte[] salt = Base64.getDecoder().decode(p[1]);
            byte[] expected = Base64.getDecoder().decode(p[2]);
            return MessageDigest.isEqual(expected, pbkdf2(password == null ? new char[0] : password, salt, iterations));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 not available", e);
        }
    }

    private void store() throws StorageException {
        try {
            Path dir = file.toAbsolutePath().getParent();
            if (dir != null) {
                Files.createDirectories(dir);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                users.store(out, "Banking System users (salted PBKDF2 hashes - do not edit)");
            }
        } catch (IOException e) {
            throw new StorageException("Could not save " + file + ": " + e.getMessage(), e);
        }
    }
}
