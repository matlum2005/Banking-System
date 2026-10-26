package com.bankingsystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bankingsystem.exception.InvalidAccountDataException;
import com.bankingsystem.service.AuthService;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthServiceTest {
    @TempDir
    Path dir;

    @Test
    void defaultAdminCanLogInAndWrongCredentialsCannot() throws Exception {
        AuthService auth = new AuthService(dir.resolve("users.properties"));
        assertTrue(auth.authenticate("admin", "admin".toCharArray()));
        assertFalse(auth.authenticate("admin", "wrong".toCharArray()));
        assertFalse(auth.authenticate("admin", new char[0]));
        assertFalse(auth.authenticate("nobody", "admin".toCharArray()));
        assertFalse(auth.authenticate(null, null));
        assertTrue(auth.usesDefaultPassword("admin"));
    }

    @Test
    void passwordFileDoesNotContainThePasswordInPlainText() throws Exception {
        Path file = dir.resolve("users.properties");
        new AuthService(file);
        String content = Files.readString(file);
        assertTrue(content.contains("admin="));
        assertFalse(content.contains("admin=admin"));
    }

    @Test
    void changedPasswordWorksAfterRestart() throws Exception {
        Path file = dir.resolve("users.properties");
        AuthService auth = new AuthService(file);
        auth.changePassword("admin", "admin".toCharArray(), "s3cret-pw".toCharArray());
        assertFalse(auth.usesDefaultPassword("admin"));

        AuthService restarted = new AuthService(file);
        assertTrue(restarted.authenticate("admin", "s3cret-pw".toCharArray()));
        assertFalse(restarted.authenticate("admin", "admin".toCharArray()));
    }

    @Test
    void changePasswordRules() throws Exception {
        AuthService auth = new AuthService(dir.resolve("users.properties"));
        assertThrows(InvalidAccountDataException.class,
                () -> auth.changePassword("admin", "bad".toCharArray(), "longenough".toCharArray()));
        assertThrows(InvalidAccountDataException.class,
                () -> auth.changePassword("admin", "admin".toCharArray(), "abc".toCharArray()));
        assertThrows(InvalidAccountDataException.class,
                () -> auth.changePassword("admin", "admin".toCharArray(), "admin".toCharArray()));
        assertTrue(auth.authenticate("admin", "admin".toCharArray()));
    }
}
