package com.autogest.model;

/**
 * Estado de un cliente del taller.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum CustomerStatus {

    /** El cliente puede recibir nuevas órdenes. */
    ACTIVE,
    /** El cliente fue dado de baja (baja lógica); conserva su historial. */
    INACTIVE
}