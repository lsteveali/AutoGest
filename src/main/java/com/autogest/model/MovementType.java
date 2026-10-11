package com.autogest.model;

/**
 * Tipo de movimiento de inventario.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum MovementType {

    /** Entrada: aumenta el stock. */
    ENTRY,
    /** Salida: disminuye el stock. */
    EXIT,
    /** Ajuste manual de existencias. */
    ADJUSTMENT
}