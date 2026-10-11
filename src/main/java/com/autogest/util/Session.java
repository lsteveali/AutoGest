package com.autogest.util;

import com.autogest.model.Role;
import com.autogest.model.User;

/**
 * Sesión actual de AutoGest: guarda el usuario que inició sesión para que el
 * shell, la barra lateral y los controladores consulten quién es y qué rol
 * tiene. Es estado global de la aplicación (una sola sesión a la vez) y por eso
 * sus operaciones son estáticas.
 *
 * <p>No conoce reglas de negocio: {@code AuthenticationService} decide cuándo se
 * abre y se cierra. Las decisiones de acceso por rol viven en
 * {@code controller.Permissions}, no aquí.</p>
 *
 * <p>Principles: Pure Fabrication, SRP, High Cohesion, Low Coupling.</p>
 */
public final class Session {

    private static User currentUser;

    private Session() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Abre la sesión con el usuario autenticado.
     *
     * @param user usuario que acaba de iniciar sesión
     */
    public static void start(User user) {
        currentUser = user;
    }

    /**
     * Devuelve el usuario de la sesión actual.
     *
     * @return usuario autenticado o {@code null} si no hay sesión abierta
     */
    public static User getCurrentUser() {
        return currentUser;
    }

    /**
     * Indica si hay una sesión abierta y el usuario tiene el rol indicado.
     *
     * @param role rol a comprobar
     * @return {@code true} si hay sesión y el rol coincide
     */
    public static boolean hasRole(Role role) {
        return currentUser != null && currentUser.hasRole(role);
    }

    /** Cierra la sesión y olvida al usuario actual. */
    public static void close() {
        currentUser = null;
    }
}
