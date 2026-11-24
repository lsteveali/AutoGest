package com.autogest.service;

import com.autogest.exception.AutoGestException;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.User;

import java.util.function.Supplier;

/**
 * Correos de las cuentas de usuario (administradores, recepción, mecánicos y
 * supervisión): la bienvenida al crear la cuenta y el código para recuperar la
 * contraseña. Usa la cuenta de Gmail configurada en el equipo
 * ({@link EmailSender}); sin cuenta, no envía nada.
 *
 * <p>Nunca manda contraseñas: la bienvenida indica con qué correo entrar y que
 * la contraseña la entrega el administrador o se recupera desde el inicio de
 * sesión.</p>
 *
 * <p>Principles: Pure Fabrication, SRP, DIP.</p>
 */
public class AccountMailer {

    private final Supplier<EmailSender> sender;

    /**
     * Crea el remitente de correos de cuentas.
     *
     * @param sender quien envía los correos; puede devolver {@code null} si no hay cuenta configurada
     */
    public AccountMailer(Supplier<EmailSender> sender) {
        this.sender = sender == null ? () -> null : sender;
    }

    /** Hay una cuenta de correo configurada en este equipo. */
    public boolean isConfigured() {
        return sender.get() != null;
    }

    /**
     * Envía la bienvenida a un usuario recién creado.
     *
     * @param user usuario guardado (con nombre, correo y rol)
     * @throws AutoGestException si no hay cuenta de correo o el envío falla
     */
    public void sendWelcome(User user) {
        require().send(user.getEmail(), user.getFullName(), welcomeSubject(), welcomeBody(user));
    }

    /**
     * Envía el código para recuperar la contraseña.
     *
     * @param user    dueño de la cuenta
     * @param code    código de 6 dígitos
     * @param minutes minutos que dura el código
     * @throws AutoGestException si no hay cuenta de correo o el envío falla
     */
    public void sendRecoveryCode(User user, String code, int minutes) {
        require().send(user.getEmail(), user.getFullName(), recoverySubject(), recoveryBody(user, code, minutes));
    }

    /**
     * Comprueba que haya una cuenta de correo configurada.
     *
     * @throws AutoGestException si no la hay, con un mensaje para el usuario
     */
    public void requireConfigured() {
        require();
    }

    private EmailSender require() {
        EmailSender current = sender.get();
        if (current == null) {
            throw new AutoGestException("El envío de correos no está configurado en este equipo. "
                    + "Pide al administrador que restablezca tu contraseña.");
        }
        return current;
    }

    static String welcomeSubject() {
        return "¡Te registraste con éxito en " + workshop() + "!";
    }

    static String welcomeBody(User user) {
        return "Hola " + user.getFirstName() + ", ¡bienvenido a " + workshop() + "!\n\n"
                + "Tu cuenta de AutoGest quedó creada con el rol " + roleText(user.getRole()) + ".\n"
                + "Para entrar usa este correo: " + user.getEmail() + "\n\n"
                + "La contraseña te la entrega el administrador. Si la olvidas, en la pantalla de inicio de "
                + "sesión pulsa «¿Olvidaste tu contraseña?» y te enviaremos un código para cambiarla.\n\n"
                + "Por seguridad, nunca te enviaremos tu contraseña por correo.";
    }

    static String recoverySubject() {
        return "Código para recuperar tu contraseña de AutoGest";
    }

    static String recoveryBody(User user, String code, int minutes) {
        return "Hola " + user.getFirstName() + ",\n\n"
                + "Pediste cambiar la contraseña de tu cuenta de AutoGest en " + workshop() + ". "
                + "Tu código es:\n\n"
                + "    " + code + "\n\n"
                + "Escríbelo en la pantalla de recuperación junto con tu nueva contraseña. "
                + "Vence en " + minutes + " minutos y solo sirve una vez.\n\n"
                + "Si no fuiste tú, ignora este correo: tu contraseña sigue igual.";
    }

    private static String workshop() {
        return NotificationTemplate.baseValues().get("taller");
    }

    private static String roleText(Role role) {
        if (role == null) {
            return "sin rol";
        }
        return switch (role) {
            case ADMINISTRATOR -> "Administrador";
            case RECEPTIONIST -> "Recepcionista";
            case MECHANIC -> "Mecánico";
            case SUPERVISOR -> "Supervisor";
        };
    }
}
