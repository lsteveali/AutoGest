package com.autogest.controller;

import com.autogest.model.Role;

/**
 * Matriz de acceso por rol (decisión D-11). Decide si un rol puede abrir una
 * pantalla; la usan la barra lateral (para filtrar sus opciones) y el shell
 * (para avisar cuando un rol intenta abrir una vista prohibida).
 *
 * <p>Inicio y Asistente son accesibles para todos. El administrador accede a
 * todo. Recepción ve Clientes, Vehículos, Órdenes y Facturación. Mecánica ve
 * Órdenes y Servicios. Supervisión ve Órdenes, Inventario y Reportes.
 * Configuración (y Usuarios y roles) es solo del administrador. Lo no listado
 * queda denegado por defecto.</p>
 *
 * <p>Principles: Pure Fabrication, SRP, Protected Variations.</p>
 */
public final class Permissions {

    private Permissions() {
        // Clase de decisión: no se instancia.
    }

    /**
     * Indica si un rol puede acceder a una pantalla según D-11.
     *
     * @param role rol del usuario
     * @param view pantalla que se quiere abrir
     * @return {@code true} si el acceso está permitido
     */
    public static boolean canAccess(Role role, View view) {
        if (role == null || view == null) {
            return false;
        }
        // Inicio, Asistente y el perfil propio («Perfil y cuenta») son para todos los roles.
        if (view == View.HOME || view == View.ASSISTANT || view == View.SETTINGS_PROFILE) {
            return true;
        }
        if (role == Role.ADMINISTRATOR) {
            return true;
        }
        View section = view.getSidebarSection();
        return switch (role) {
            case RECEPTIONIST -> section == View.CUSTOMERS || section == View.VEHICLES
                    || section == View.ORDERS || section == View.BILLING;
            case MECHANIC -> section == View.ORDERS || section == View.SERVICES;
            case SUPERVISOR -> section == View.ORDERS || section == View.INVENTORY
                    || section == View.REPORTS;
            case ADMINISTRATOR -> true;
        };
    }
}
