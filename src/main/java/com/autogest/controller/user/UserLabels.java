package com.autogest.controller.user;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.User;
import com.autogest.model.Catalogs.UserStatus;

/**
 * Traduce los valores del dominio de usuarios a texto en español para las
 * pantallas (rol, estado e iniciales) y hace la conversión inversa que
 * necesitan los filtros y las listas desplegables. Centraliza estos textos
 * para que el modelo no contenga recursos de presentación y todas las
 * pantallas del módulo muestren lo mismo.
 *
 * <p>Principles: Pure Fabrication, High Cohesion, Protected Variations.</p>
 */
public final class UserLabels {

    private UserLabels() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Devuelve el nombre del rol tal como se muestra en pantalla.
     *
     * @param role rol a traducir; puede ser nulo
     * @return nombre en español (por ejemplo «Administrador») o cadena vacía
     */
    public static String roleText(Role role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case ADMINISTRATOR -> "Administrador";
            case RECEPTIONIST -> "Recepción";
            case MECHANIC -> "Mecánico";
            case SUPERVISOR -> "Supervisor";
        };
    }

    /**
     * Convierte el texto de un rol (el que ven los filtros y listas) de vuelta
     * al valor del dominio.
     *
     * @param text texto mostrado en pantalla; puede ser nulo o «Todos»
     * @return el rol correspondiente o {@code null} si no aplica ninguno
     */
    public static Role roleFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (Role role : Role.values()) {
            if (roleText(role).equalsIgnoreCase(text.trim())) {
                return role;
            }
        }
        return null;
    }

    /**
     * Descripción del alcance de un rol según la matriz de acceso (D-11).
     *
     * @param role rol a describir; puede ser nulo
     * @return descripción en español o cadena vacía
     */
    public static String roleDescription(Role role) {
        if (role == null) {
            return "";
        }
        return switch (role) {
            case ADMINISTRATOR -> "Acceso completo al sistema.";
            case RECEPTIONIST -> "Clientes, vehículos, órdenes y facturación.";
            case MECHANIC -> "Órdenes de servicio, diagnósticos y servicios.";
            case SUPERVISOR -> "Órdenes, inventario y reportes.";
        };
    }

    /**
     * Devuelve el texto del estado de una cuenta.
     *
     * @param status estado a traducir; puede ser nulo
     * @return «Activo», «Inactivo» o cadena vacía
     */
    public static String statusText(UserStatus status) {
        if (status == null) {
            return "";
        }
        return status == UserStatus.ACTIVE ? "Activo" : "Inactivo";
    }

    /**
     * Convierte el texto de un estado de vuelta al valor del dominio.
     *
     * @param text texto mostrado en pantalla; puede ser nulo o «Todos»
     * @return el estado correspondiente o {@code null} si no aplica ninguno
     */
    public static UserStatus statusFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if ("Activo".equalsIgnoreCase(text.trim())) {
            return UserStatus.ACTIVE;
        }
        if ("Inactivo".equalsIgnoreCase(text.trim())) {
            return UserStatus.INACTIVE;
        }
        return null;
    }

    /**
     * Devuelve el tipo de insignia que corresponde al estado de una cuenta.
     *
     * @param status estado a representar
     * @return {@code SUCCESS} para activo, {@code NEUTRAL} para inactivo
     */
    public static StatusBadge.BadgeType statusType(UserStatus status) {
        return status == UserStatus.ACTIVE
                ? StatusBadge.BadgeType.SUCCESS
                : StatusBadge.BadgeType.NEUTRAL;
    }

    /**
     * Calcula las iniciales de un usuario (primera letra del nombre y del
     * apellido) para el avatar de las tablas.
     *
     * @param user usuario; puede ser nulo
     * @return una o dos letras, o «?» si no hay datos
     */
    public static String initials(User user) {
        if (user == null) {
            return "?";
        }
        String initials = (initial(user.getFirstName()) + initial(user.getLastName())).toUpperCase();
        return initials.isBlank() ? "?" : initials;
    }

    /** Primera letra de un texto, o cadena vacía si no hay. */
    private static String initial(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1);
    }
}
