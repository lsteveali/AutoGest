package com.autogest.controller.components;

import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Tooltip;

/**
 * Botón flotante «Asistente AutoGest» presente en todas las pantallas. El
 * controlador del shell lo coloca sobre el área de contenido y le asigna la
 * navegación al módulo Asistente.
 *
 * <p>Es redondo y solo muestra el ícono, para no tapar botones ni filas de
 * las tablas; el nombre queda en la ayuda emergente y para los lectores de
 * pantalla (la barra lateral también tiene el ítem con su nombre).</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class AssistantButton extends Button {

    private static final String NAME = "Asistente AutoGest";

    /** Crea el botón flotante con su ícono; el texto va en la ayuda emergente. */
    public AssistantButton() {
        setText(NAME);
        setGraphic(IconSet.icon(IconSet.SPARKLES));
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        setTooltip(new Tooltip(NAME));
        setAccessibleText(NAME);
        getStyleClass().add("assistant-button");
    }
}
