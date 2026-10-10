package com.autogest.model;

/**
 * Estado de una orden de servicio. Las constantes están en el orden del flujo; las transiciones válidas las define {@code ServiceOrder.canChangeTo} (decisión D-04).
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum OrderStatus {

    /** Vehículo recibido en el taller. */
    RECEIVED,
    /** En diagnóstico. */
    DIAGNOSIS,
    /** En reparación. */
    IN_REPAIR,
    /** Listo para entregar. */
    READY,
    /** Entregado al cliente. */
    DELIVERED,
    /** Orden cancelada. */
    CANCELLED
}
