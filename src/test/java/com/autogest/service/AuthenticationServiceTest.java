package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.User;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.util.PasswordHasher;
import com.autogest.util.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link AuthenticationService} con un repositorio falso, sin base
 * de datos.
 */
class AuthenticationServiceTest {

    private static final String EMAIL = "jose.garcia@autogest.com";
    private static final String PASSWORD = "admin";

    private FakeUserRepository repository;
    private AuthenticationService service;
    private User active;

    @BeforeEach
    void setUp() {
        repository = new FakeUserRepository();
        service = new AuthenticationService(repository);
        active = new User();
        active.setFirstName("José");
        active.setLastName("García");
        active.setEmail(EMAIL);
        active.setUsername("jose.garcia");
        active.setRole(Role.ADMINISTRATOR);
        active.setPasswordHash(PasswordHasher.hash(PASSWORD));
        repository.create(active);
    }

    @AfterEach
    void tearDown() {
        Session.close();
    }

    @Test
    void loginWithValidCredentialsStartsSessionAndUpdatesLastAccess() {
        User user = service.login(EMAIL, PASSWORD);

        assertSame(active, user);
        assertSame(active, Session.getCurrentUser());
        assertTrue(repository.wasLastAccessUpdated(active.getId()));
        assertNotNull(user.getLastAccess());
    }

    @Test
    void loginIgnoresEmailCase() {
        assertNotNull(service.login("JOSE.GARCIA@AUTOGEST.COM", PASSWORD));
    }

    @Test
    void loginWithUnknownEmailUsesGenericMessage() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.login("nadie@autogest.com", PASSWORD));
        assertEquals("Correo o contraseña incorrectos", exception.getMessage());
        assertNull(Session.getCurrentUser());
    }

    @Test
    void loginWithWrongPasswordUsesGenericMessage() {
        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.login(EMAIL, "OtraClave2026"));
        assertEquals("Correo o contraseña incorrectos", exception.getMessage());
        assertNull(Session.getCurrentUser());
    }

    @Test
    void loginWithBlankFieldsFails() {
        assertThrows(ValidationException.class, () -> service.login("  ", PASSWORD));
        assertThrows(ValidationException.class, () -> service.login(EMAIL, ""));
    }

    @Test
    void loginWithInactiveUserFails() {
        active.setStatus(UserStatus.INACTIVE);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> service.login(EMAIL, PASSWORD));
        assertEquals("Tu usuario está inactivo. Contacta al administrador.", exception.getMessage());
        assertNull(Session.getCurrentUser());
    }

    @Test
    void logoutClosesSession() {
        service.login(EMAIL, PASSWORD);
        service.logout();

        assertNull(Session.getCurrentUser());
    }
}
