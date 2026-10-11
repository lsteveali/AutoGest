package com.autogest.model;

/**
 * Canal por el que se envía una notificación. {@code WHATSAPP} solo queda preparado, sin integración real. El código específico de cada canal vive en {@code NotificationService}.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion, Protected Variations.</p>
 */
public enum Channel {

    /** Correo electrónico. */
    EMAIL,
    /** Mensaje interno del sistema. */
    SYSTEM,
    /** WhatsApp (solo preparado). */
    WHATSAPP
}