package com.autogest.model;

/**
 * Estado de una cuenta de usuario.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum UserStatus {

    /** El usuario puede iniciar sesión. */
    ACTIVE,
    /** El usuario fue dado de baja (baja lógica). */
    INACTIVE
}