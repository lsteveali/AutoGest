package com.autogest.controller.components;

import javafx.scene.control.Label;

/**
 * Etiqueta de color para representar el estado de una entidad (Activo,
 * En reparación, Agotado, ...). Centraliza qué clase CSS corresponde a cada
 * tipo de estado para que ninguna pantalla elija colores por su cuenta.
 *
 * <p>Se construye con {@link #of(String, BadgeType)} y devuelve un
 * {@link Label} común, así que se puede usar como celda de tabla o dentro de
 * cualquier contenedor.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class StatusBadge {

    /** Tipos de insignia definidos por el sistema de diseño (sección 3). */
    public enum BadgeType {
        /** Verde: activo, listo, completado, pagada. */
        SUCCESS,
        /** Amarillo: en reparación, mantenimiento próximo, stock bajo. */
        WARNING,
        /** Azul: diagnóstico, en servicio. */
        INFO,
        /** Rojo: cancelado, agotado, anulada. */
        DANGER,
        /** Gris: inactivo o sin estado definido. */
        NEUTRAL
    }

    private StatusBadge() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Crea una insignia con el texto y el tipo de color indicados.
     *
     * @param text texto visible (en español) de la insignia
     * @param type tipo de insignia que define el color
     * @return un {@link Label} con las clases CSS {@code badge} y {@code badge-*}
     */
    public static Label of(String text, BadgeType type) {
        Label badge = new Label(text);
        badge.getStyleClass().addAll("badge", styleClassFor(type));
        return badge;
    }

    /** Traduce el tipo de insignia a su clase CSS (definida en autogest.css). */
    private static String styleClassFor(BadgeType type) {
        return switch (type) {
            case SUCCESS -> "badge-success";
            case WARNING -> "badge-warning";
            case INFO -> "badge-info";
            case DANGER -> "badge-danger";
            case NEUTRAL -> "badge-neutral";
        };
    }
}
