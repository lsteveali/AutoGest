package com.autogest.model;

/**
 * Motivo o categoría de una notificación.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum NotificationType {

    /** Actualización del estado de un servicio u orden. */
    SERVICE_UPDATE,
    /** Recordatorio de mantenimiento. */
    MAINTENANCE_REMINDER,
    /** Relacionada con facturación. */
    BILLING,
    /** Promoción. */
    PROMOTION,
    /** Mensaje general. */
    GENERAL
}