package com.autogest.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del cifrado y la verificación de contraseñas (PBKDF2, D-07).
 */
class PasswordHasherTest {

    private static final String PASSWORD = "admin";

    @Test
    void hashUsesIterationsSaltAndHashFormat() {
        String hash = PasswordHasher.hash(PASSWORD);
        String[] parts = hash.split(":");
        assertEquals(3, parts.length);
        assertEquals("600000", parts[0]);
    }

    @Test
    void verifyAcceptsCorrectPassword() {
        String hash = PasswordHasher.hash(PASSWORD);
        assertTrue(PasswordHasher.verify(PASSWORD, hash));
    }

    @Test
    void verifyRejectsWrongPassword() {
        String hash = PasswordHasher.hash(PASSWORD);
        assertFalse(PasswordHasher.verify("admin2", hash));
    }

    @Test
    void verifyRejectsMalformedHash() {
        assertFalse(PasswordHasher.verify(PASSWORD, "hash-corrupto"));
        assertFalse(PasswordHasher.verify(PASSWORD, null));
    }

    @Test
    void hashesUseDifferentRandomSalt() {
        assertNotEquals(PasswordHasher.hash(PASSWORD), PasswordHasher.hash(PASSWORD));
    }

    @Test
    void hashRejectsEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> PasswordHasher.hash(""));
    }
}
