package com.autogest.model;

import com.autogest.model.Catalogs.Role;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.util.PasswordHasher;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas propias de {@link User}.
 */
class UserTest {

    @Test
    void getFullNameJoinsFirstAndLastName() {
        User user = new User();
        user.setFirstName("Carlos");
        user.setLastName("Martínez");

        assertEquals("Carlos Martínez", user.getFullName());
    }

    @Test
    void hasRoleMatchesOnlyTheAssignedRole() {
        User user = new User();
        user.setRole(Role.MECHANIC);

        assertTrue(user.hasRole(Role.MECHANIC));
        assertFalse(user.hasRole(Role.ADMINISTRATOR));
    }

    @Test
    void validatePasswordAcceptsOnlyTheRightPassword() {
        User user = new User();
        user.setPasswordHash(PasswordHasher.hash("Secreta123"));

        assertTrue(user.validatePassword("Secreta123"));
        assertFalse(user.validatePassword("otraClave"));
    }
}
