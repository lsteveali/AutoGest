package com.autogest.model;

/**
 * Clase de vehículo. Valores iniciales (decisión D-13).
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion, Protected Variations.</p>
 */
public enum VehicleType {

    /** Automóvil. */
    CAR,
    /** Camioneta o furgón. */
    VAN,
    /** Motocicleta. */
    MOTORCYCLE,
    /** Otro tipo. */
    OTHER
}
