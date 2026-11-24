package com.autogest.service;

import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.model.User;
import com.autogest.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** «¿Olvidaste tu contraseña?» con un correo falso: nadie recibe nada. */
class PasswordRecoveryServiceTest {

    private static final String EMAIL = "ana.lopez@autogest.com";

    private final List<String> sent = new ArrayList<>();
    private FakeUserRepository users;
    private UserService userService;
    private MutableClock clock;
    private PasswordRecoveryService recovery;
    private User ana;

    /** Reloj que se puede adelantar. */
    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-23T15:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("America/Bogota");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @BeforeEach
    void setUp() {
        users = new FakeUserRepository();
        userService = new UserService(users);
        User user = new User();
        user.setFirstName("Ana");
        user.setLastName("López");
        user.setEmail(EMAIL);
        user.setRole(Role.RECEPTIONIST);
        ana = userService.createUser(user, "Clave1234");
        clock = new MutableClock();
        AccountMailer mailer = new AccountMailer(() -> (to, name, subject, body) -> sent.add(to + "|" + body));
        recovery = new PasswordRecoveryService(users, userService, mailer, clock, bound -> 42);
    }

    @Test
    void codeArrivesByEmailAndChangesThePassword() {
        recovery.requestCode(" Ana.Lopez@AutoGest.com ");

        assertEquals(1, sent.size());
        assertTrue(sent.get(0).startsWith(EMAIL + "|"));
        assertTrue(sent.get(0).contains("000042"), sent.get(0));

        recovery.resetPassword(EMAIL, "000042", "NuevaClave9", "NuevaClave9");

        assertTrue(PasswordHasher.verify("NuevaClave9", users.findById(ana.getId()).getPasswordHash()));
        // El código solo sirve una vez.
        assertThrows(ValidationException.class,
                () -> recovery.resetPassword(EMAIL, "000042", "OtraClave99", "OtraClave99"));
    }

    @Test
    void unknownOrInactiveAccountsGetNoEmailAndNoHint() {
        recovery.requestCode("nadie@autogest.com");
        userService.changeStatus(ana.getId(), UserStatus.INACTIVE);
        recovery.requestCode(EMAIL);

        assertTrue(sent.isEmpty());
    }

    @Test
    void wrongCodeExpiredCodeAndTooManyAttemptsAreRejected() {
        recovery.requestCode(EMAIL);
        ValidationException wrong = assertThrows(ValidationException.class,
                () -> recovery.resetPassword(EMAIL, "123456", "NuevaClave9", "NuevaClave9"));
        assertEquals("El código no es correcto.", wrong.getMessage());

        for (int i = 0; i < PasswordRecoveryService.MAX_ATTEMPTS - 2; i++) {
            assertThrows(ValidationException.class,
                    () -> recovery.resetPassword(EMAIL, "123456", "NuevaClave9", "NuevaClave9"));
        }
        ValidationException blocked = assertThrows(ValidationException.class,
                () -> recovery.resetPassword(EMAIL, "123456", "NuevaClave9", "NuevaClave9"));
        assertTrue(blocked.getMessage().contains("Demasiados intentos"));

        clock.advance(Duration.ofMinutes(2));
        recovery.requestCode(EMAIL);
        clock.advance(Duration.ofMinutes(PasswordRecoveryService.CODE_MINUTES));
        ValidationException expired = assertThrows(ValidationException.class,
                () -> recovery.resetPassword(EMAIL, "000042", "NuevaClave9", "NuevaClave9"));
        assertTrue(expired.getMessage().contains("venció"));
    }

    @Test
    void newPasswordMustMatchAndFollowTheRules() {
        recovery.requestCode(EMAIL);

        assertThrows(ValidationException.class,
                () -> recovery.resetPassword(EMAIL, "000042", "NuevaClave9", "OtraCosa9"));
        assertThrows(ValidationException.class,
                () -> recovery.resetPassword(EMAIL, "000042", "corta", "corta"));
        // Tras un error de contraseña el código sigue sirviendo.
        recovery.resetPassword(EMAIL, "000042", "NuevaClave9", "NuevaClave9");
    }

    @Test
    void anotherCodeMustWaitAMinute() {
        recovery.requestCode(EMAIL);

        assertThrows(ValidationException.class, () -> recovery.requestCode(EMAIL));
        clock.advance(Duration.ofSeconds(PasswordRecoveryService.RESEND_SECONDS));
        recovery.requestCode(EMAIL);
        assertEquals(2, sent.size());
    }

    @Test
    void withoutEmailConfiguredTheUserIsToldToAskTheAdministrator() {
        PasswordRecoveryService offline = new PasswordRecoveryService(users, userService, new AccountMailer(() -> null),
                clock, bound -> 1);

        AutoGestException error = assertThrows(AutoGestException.class, () -> offline.requestCode(EMAIL));
        assertTrue(error.getMessage().contains("administrador"));
    }
}
