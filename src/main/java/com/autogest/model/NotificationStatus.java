package com.autogest.model;

/**
 * Estado de envío de una notificación.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum NotificationStatus {

    /** Enviada. */
    SENT,
    /** Pendiente de envío. */
    PENDING,
    /** Programada para una fecha futura. */
    SCHEDULED,
    /** El envío falló. */
    FAILED
}

