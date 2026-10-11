package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.User;
import com.autogest.model.UserStatus;
import com.autogest.repository.UserRepository;
import com.autogest.util.Session;

import java.time.LocalDateTime;

/**
 * Autentica a los usuarios contra la base de datos. Valida las credenciales,
 * actualiza el último acceso, abre la sesión y devuelve el usuario. No conoce
 * SQL ni JavaFX: solo coordina el caso de uso de inicio y cierre de sesión.
 *
 * <p>El mensaje de error es el mismo cuando el correo no existe y cuando la
 * contraseña es incorrecta, para no revelar qué correos están registrados.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP, High Cohesion, Low Coupling.</p>
 */
public class AuthenticationService {

    private final UserRepository repository;

    /**
     * Crea el servicio con el repositorio de usuarios inyectado.
     *
     * @param repository acceso a los usuarios
     */
    public AuthenticationService(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Inicia sesión con el correo y la contraseña ingresados. Si las
     * credenciales son correctas y la cuenta está activa, registra el último
     * acceso, abre la {@link Session} y devuelve el usuario.
     *
     * @param email    correo del usuario
     * @param password contraseña en texto plano escrita en el formulario
     * @return el usuario autenticado
     * @throws ValidationException si faltan campos, las credenciales son
     *         incorrectas o la cuenta está inactiva
     */
    public User login(String email, String password) {
        if (isBlank(email) || isBlank(password)) {
            throw new ValidationException("Debes ingresar el correo y la contraseña.");
        }
        User user = repository.findByEmail(email.trim());
        if (user == null || !user.validatePassword(password)) {
            throw new ValidationException("Correo o contraseña incorrectos");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ValidationException("Tu usuario está inactivo. Contacta al administrador.");
        }
        user.setLastAccess(LocalDateTime.now());
        repository.updateLastAccess(user.getId());
        Session.start(user);
        return user;
    }

    /** Cierra la sesión actual. */
    public void logout() {
        Session.close();
    }

    /** Indica si una cadena es nula o solo espacios. */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
