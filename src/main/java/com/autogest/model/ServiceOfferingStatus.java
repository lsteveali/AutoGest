package com.autogest.model;

/**
 * Estado de un servicio del catálogo.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum ServiceOfferingStatus {

    /** El servicio se puede agregar a nuevas órdenes. */
    ACTIVE,
    /** El servicio ya no se ofrece. */
    INACTIVE
}
