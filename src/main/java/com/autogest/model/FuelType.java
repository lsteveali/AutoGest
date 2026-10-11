package com.autogest.model;

/**
 * Tipo de combustible de un vehículo. Valores iniciales, ampliables (decisión D-06).
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion, Protected Variations.</p>
 */
public enum FuelType {

    /** Gasolina. */
    GASOLINE,
    /** Diésel. */
    DIESEL,
    /** Híbrido. */
    HYBRID,
    /** Eléctrico. */
    ELECTRIC,
    /** Gas. */
    GAS
}