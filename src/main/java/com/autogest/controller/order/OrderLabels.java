package com.autogest.controller.order;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Vehicle;

import java.util.List;

/**
 * Utilidades de presentación de las órdenes de servicio: estados en español,
 * colores de insignia (guía §3) y textos de las columnas de la tabla.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class OrderLabels {

    /** Opción «sin filtro» de los desplegables. */
    public static final String ALL = "Todos";

    private OrderLabels() {
        // Clase de utilidad: no se instancia.
    }

    /** Texto visible del estado. */
    public static String statusText(OrderStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case RECEIVED -> "Recibido";
            case DIAGNOSIS -> "Diagnóstico";
            case IN_REPAIR -> "En reparación";
            case READY -> "Listo";
            case DELIVERED -> "Entregado";
            case CANCELLED -> "Cancelado";
        };
    }

    /** Color de la insignia: azul diagnóstico, amarillo reparación, verde listo/entregado, rojo cancelado. */
    public static StatusBadge.BadgeType statusType(OrderStatus status) {
        if (status == null) {
            return StatusBadge.BadgeType.NEUTRAL;
        }
        return switch (status) {
            case RECEIVED -> StatusBadge.BadgeType.NEUTRAL;
            case DIAGNOSIS -> StatusBadge.BadgeType.INFO;
            case IN_REPAIR -> StatusBadge.BadgeType.WARNING;
            case READY, DELIVERED -> StatusBadge.BadgeType.SUCCESS;
            case CANCELLED -> StatusBadge.BadgeType.DANGER;
        };
    }

    /** Opciones del filtro de estado (sin «Todos»), en el orden del flujo. */
    public static List<String> statusOptions() {
        return List.of(OrderStatus.values()).stream().map(OrderLabels::statusText).toList();
    }

    /** Estado a partir del texto del filtro; «Todos» o desconocido = {@code null}. */
    public static OrderStatus statusFromText(String text) {
        for (OrderStatus status : OrderStatus.values()) {
            if (statusText(status).equals(text)) {
                return status;
            }
        }
        return null;
    }

    /** Número visible de la orden: «#0145». */
    public static String number(ServiceOrder order) {
        return order.getNumber() == null ? "-" : "#" + order.getNumber();
    }

    /** Nombre del cliente dueño del vehículo de la orden. */
    public static String customer(ServiceOrder order) {
        Vehicle vehicle = order.getVehicle();
        if (vehicle == null || vehicle.getOwner() == null || vehicle.getOwner().getFirstName() == null) {
            return "-";
        }
        return vehicle.getOwner().getFullName();
    }

    /** «Toyota Corolla» del vehículo de la orden. */
    public static String vehicle(ServiceOrder order) {
        Vehicle vehicle = order.getVehicle();
        if (vehicle == null || vehicle.getMake() == null) {
            return "-";
        }
        return vehicle.getMake() + " " + (vehicle.getModel() == null ? "" : vehicle.getModel());
    }

    /** Placa del vehículo de la orden. */
    public static String plate(ServiceOrder order) {
        Vehicle vehicle = order.getVehicle();
        return vehicle == null || vehicle.getLicensePlate() == null ? "-" : vehicle.getLicensePlate();
    }

    /** Nombre del responsable o «Sin asignar». */
    public static String responsible(ServiceOrder order) {
        User user = order.getResponsible();
        return user == null || user.getFirstName() == null ? "Sin asignar" : user.getFullName();
    }

    /** «Toyota Corolla · ABC-123 · 2022 · 48.230 km» para el desplegable de vehículos. */
    public static String vehicleOption(Vehicle vehicle) {
        return vehicle.getMake() + " " + vehicle.getModel() + " · " + vehicle.getLicensePlate()
                + " · " + vehicle.getYear() + " · " + String.format(java.util.Locale.US, "%,d", vehicle.getMileage()).replace(',', '.')
                + " km";
    }
}
