package com.autogest.model;
/**
 * Perfil de permisos de un {@code User}. No existen subclases de {@code User} por rol: el rol es un atributo.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion, Low Coupling.</p>
 */
public enum Role {

    /** Acceso completo al sistema. */
    ADMINISTRATOR,
    /** Clientes, vehículos, órdenes y facturación. */
    RECEPTIONIST,
    /** Órdenes de servicio, diagnósticos y servicios; puede ser responsable de una orden. */
    MECHANIC,
    /** Órdenes, inventario y reportes. */
    SUPERVISOR
}
