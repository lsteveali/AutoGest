package com.autogest.controller.catalog;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.ServiceOfferingStatus;
import com.autogest.util.Formatter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

/**
 * Utilidades de presentación del catálogo de servicios: convierte los valores
 * del dominio a texto en español y aporta las opciones de los filtros.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class ServiceLabels {

    private ServiceLabels() {
        // Clase de utilidad: no se instancia.
    }

    /** Texto visible para el estado del servicio. */
    public static String statusText(ServiceOfferingStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case ACTIVE -> "Activo";
            case INACTIVE -> "Inactivo";
        };
    }

    /** Tipo de insignia para el estado. */
    public static StatusBadge.BadgeType statusType(ServiceOfferingStatus status) {
        if (status == null) {
            return StatusBadge.BadgeType.NEUTRAL;
        }
        return switch (status) {
            case ACTIVE -> StatusBadge.BadgeType.SUCCESS;
            case INACTIVE -> StatusBadge.BadgeType.NEUTRAL;
        };
    }

    /** Opciones del filtro de estado. */
    public static List<String> statusOptions() {
        return List.of("Activo", "Inactivo");
    }

    /** Convierte el texto del filtro a {@link ServiceOfferingStatus} (vacío = sin filtro). */
    public static ServiceOfferingStatus statusFromText(String value) {
        if (value == null || value.isBlank() || "Todos".equals(value)) {
            return null;
        }
        if ("Activo".equals(value)) {
            return ServiceOfferingStatus.ACTIVE;
        }
        if ("Inactivo".equals(value)) {
            return ServiceOfferingStatus.INACTIVE;
        }
        return null;
    }

    /** Formatea el precio con la utilidad {@link Formatter} (o «-» si es nulo). */
    public static String price(BigDecimal price) {
        if (price == null) {
            return "-";
        }
        return Formatter.money(price);
    }

    /** Formatea la duración estimada en horas. */
    public static String duration(double hours) {
        if (hours <= 0) {
            return "0 h";
        }
        if (Math.abs(hours - Math.round(hours)) < 0.0001) {
            return (long) Math.round(hours) + " h";
        }
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f h", hours);
    }
}
