package com.autogest.model;

/**
 * Tipo de transmisión de un vehículo (decisión D-13).
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum Transmission {

    /** Manual. */
    MANUAL,
    /** Automática. */
    AUTOMATIC
}
