package com.autogest.controller.inventory;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Catalogs.MovementType;
import com.autogest.model.Catalogs.StockStatus;

import java.util.List;

/**
 * Utilidades de presentación del módulo Inventario: traduce los estados de
 * stock y los tipos de movimiento a texto en español y entrega las opciones de
 * los filtros. No tiene dependencias de JavaFX salvo el tipo de insignia, así
 * que se prueba sin toolkit.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class InventoryLabels {

    /** Opción «sin filtro» de los desplegables. */
    public static final String ALL = "Todos";

    private InventoryLabels() {
        // Clase de utilidad: no se instancia.
    }

    /** Texto visible del estado de stock. */
    public static String stockText(StockStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case AVAILABLE -> "Disponible";
            case LOW_STOCK -> "Stock bajo";
            case OUT_OF_STOCK -> "Agotado";
        };
    }

    /** Color de la insignia del estado de stock. */
    public static StatusBadge.BadgeType stockType(StockStatus status) {
        if (status == null) {
            return StatusBadge.BadgeType.NEUTRAL;
        }
        return switch (status) {
            case AVAILABLE -> StatusBadge.BadgeType.SUCCESS;
            case LOW_STOCK -> StatusBadge.BadgeType.WARNING;
            case OUT_OF_STOCK -> StatusBadge.BadgeType.DANGER;
        };
    }

    /** Opciones del filtro «Estado de stock» (sin «Todos»). */
    public static List<String> stockOptions() {
        return List.of("Disponible", "Stock bajo", "Agotado");
    }

    /** Convierte el texto del filtro en {@link StockStatus}; «Todos» o vacío = {@code null}. */
    public static StockStatus stockFromText(String text) {
        if (text == null) {
            return null;
        }
        return switch (text) {
            case "Disponible" -> StockStatus.AVAILABLE;
            case "Stock bajo" -> StockStatus.LOW_STOCK;
            case "Agotado" -> StockStatus.OUT_OF_STOCK;
            default -> null;
        };
    }

    /** Texto visible del tipo de movimiento. */
    public static String movementText(MovementType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case ENTRY -> "Entrada";
            case EXIT -> "Salida";
            case ADJUSTMENT -> "Ajuste";
        };
    }

    /** Color de la insignia del tipo de movimiento. */
    public static StatusBadge.BadgeType movementType(MovementType type) {
        if (type == null) {
            return StatusBadge.BadgeType.NEUTRAL;
        }
        return switch (type) {
            case ENTRY -> StatusBadge.BadgeType.SUCCESS;
            case EXIT -> StatusBadge.BadgeType.DANGER;
            case ADJUSTMENT -> StatusBadge.BadgeType.INFO;
        };
    }

    /**
     * Cantidad con signo para la tabla de movimientos: «+5» en entradas,
     * «-3» en salidas y el número tal cual en ajustes.
     */
    public static String signedQuantity(MovementType type, int quantity) {
        if (type == MovementType.ENTRY) {
            return "+" + quantity;
        }
        if (type == MovementType.EXIT) {
            return "-" + quantity;
        }
        return String.valueOf(quantity);
    }

    /** Texto «24 / mín. 10» de la columna de stock. */
    public static String stockSummary(int current, int minimum) {
        return current + " / mín. " + minimum;
    }
}
