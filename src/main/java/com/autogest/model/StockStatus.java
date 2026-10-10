package com.autogest.model;

/**
 * Nivel de existencias de un repuesto; se deriva de {@code currentStock} y {@code minimumStock}.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum StockStatus {

    /** Existencias por encima del mínimo. */
    AVAILABLE,
    /** Existencias menores o iguales al mínimo. */
    LOW_STOCK,
    /** Sin existencias. */
    OUT_OF_STOCK
}