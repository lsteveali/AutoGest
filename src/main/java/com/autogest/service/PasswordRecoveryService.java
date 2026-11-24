package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.model.User;
import com.autogest.repository.UserRepository;
import com.autogest.util.PasswordHasher;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;

/**
 * «¿Olvidaste tu contraseña?»: envía un código de 6 dígitos al correo de la
 * cuenta y, con ese código, deja elegir una contraseña nueva.
 *
 * <p>Reglas:</p>
 * <ul>
 *   <li>El código vence a los {@value #CODE_MINUTES} minutos, sirve una sola vez
 *       y se anula después de {@value #MAX_ATTEMPTS} intentos fallidos.</li>
 *   <li>Solo se guarda el hash del código, en memoria (no en la base de datos).</li>
 *   <li>Hay que esperar {@value #RESEND_SECONDS} segundos para pedir otro.</li>
 *   <li>Si el correo no es de una cuenta activa, la respuesta es la misma
 *       (no se revela qué correos existen) y no se envía nada.</li>
 *   <li>La contraseña nueva cumple las mismas reglas que al crear el usuario.</li>
 * </ul>
 *
 * <p>Principles: Information Expert, SRP, DIP.</p>
 */
public class PasswordRecoveryService {

    static final int CODE_MINUTES = 15;
    static final int RESEND_SECONDS = 60;
    static final int MAX_ATTEMPTS = 5;

    /** Código pendiente de una cuenta. */
    private record Pending(int userId, String codeHash, Instant expires, Instant sentAt, int attempts) {
    }

    private final UserRepository users;
    private final UserService userService;
    private final AccountMailer mailer;
    private final Clock clock;
    private final IntUnaryOperator random;
    private final Map<String, Pending> pending = new ConcurrentHashMap<>();

    /**
     * Crea el servicio con el reloj del sistema y códigos aleatorios seguros.
     *
     * @param users       acceso a los usuarios (buscar por correo)
     * @param userService cambia la contraseña con sus reglas
     * @param mailer      envía el código por correo
     */
    public PasswordRecoveryService(UserRepository users, UserService userService, AccountMailer mailer) {
        this(users, userService, mailer, Clock.systemDefaultZone(), new SecureRandom()::nextInt);
    }

    /**
     * Crea el servicio con reloj y generador dados (para las pruebas).
     *
     * @param random recibe un límite y devuelve un número entre 0 y ese límite (sin incluirlo)
     */
    PasswordRecoveryService(UserRepository users, UserService userService, AccountMailer mailer, Clock clock,
                            IntUnaryOperator random) {
        this.users = users;
        this.userService = userService;
        this.mailer = mailer;
        this.clock = clock;
        this.random = random;
    }

    /**
     * Envía un código al correo, si es de una cuenta activa.
     *
     * @param email correo de la cuenta
     * @throws ValidationException si el correo está vacío o hay que esperar para pedir otro código
     * @throws com.autogest.exception.AutoGestException si no hay correo configurado o el envío falla
     */
    public void requestCode(String email) {
        String key = normalize(email);
        Instant now = clock.instant();
        Pending previous = pending.get(key);
        if (previous != null && previous.sentAt().plusSeconds(RESEND_SECONDS).isAfter(now)) {
            throw new ValidationException("Ya te enviamos un código. Espera un minuto antes de pedir otro.");
        }
        mailer.requireConfigured();
        User user = users.findByEmail(key);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return;
        }
        String code = String.format(Locale.ROOT, "%06d", random.applyAsInt(1_000_000));
        mailer.sendRecoveryCode(user, code, CODE_MINUTES);
        pending.put(key, new Pending(user.getId(), PasswordHasher.hash(code),
                now.plus(Duration.ofMinutes(CODE_MINUTES)), now, 0));
    }

    /**
     * Cambia la contraseña con el código recibido.
     *
     * @param email        correo de la cuenta
     * @param code         código de 6 dígitos
     * @param newPassword  contraseña nueva
     * @param confirmation la contraseña nueva repetida
     * @throws ValidationException si el código no existe, venció, no coincide, se agotaron los
     *                             intentos, las contraseñas no coinciden o no cumplen las reglas
     */
    public void resetPassword(String email, String code, String newPassword, String confirmation) {
        String key = normalize(email);
        Pending entry = pending.get(key);
        if (entry == null) {
            throw new ValidationException("Pide primero un código para ese correo.");
        }
        if (!entry.expires().isAfter(clock.instant())) {
            pending.remove(key);
            throw new ValidationException("El código venció. Pide uno nuevo.");
        }
        String typed = code == null ? "" : code.trim();
        if (!typed.matches("\\d{6}") || !PasswordHasher.verify(typed, entry.codeHash())) {
            int attempts = entry.attempts() + 1;
            if (attempts >= MAX_ATTEMPTS) {
                pending.remove(key);
                throw new ValidationException("Demasiados intentos con un código incorrecto. Pide uno nuevo.");
            }
            pending.put(key, new Pending(entry.userId(), entry.codeHash(), entry.expires(), entry.sentAt(), attempts));
            throw new ValidationException("El código no es correcto.");
        }
        if (newPassword == null || !newPassword.equals(confirmation)) {
            throw new ValidationException("Las contraseñas no coinciden.");
        }
        userService.resetPassword(entry.userId(), newPassword);
        pending.remove(key);
    }

    private static String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Escribe el correo de tu cuenta.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
