package com.autogest.model;

/**
 * Estado guardado de un vehículo. El aviso de mantenimiento próximo NO es un estado: se calcula con {@code Vehicle.isMaintenanceDue} (decisión D-13).
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum VehicleStatus {

    /** Vehículo disponible, sin orden activa. */
    ACTIVE,
    /** Vehículo con una orden de servicio en curso. */
    IN_SERVICE,
    /** Vehículo dado de baja. */
    INACTIVE
}
