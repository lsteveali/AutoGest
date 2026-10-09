package com.autogest.controller.components;

import javafx.scene.control.Button;

/**
 * Botón flotante «Asistente AutoGest» presente en todas las pantallas. El
 * controlador del shell lo coloca sobre el área de contenido y le asigna la
 * navegación al módulo Asistente.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class AssistantButton extends Button {

    /** Crea el botón flotante con su ícono y texto en español. */
    public AssistantButton() {
        setText("Asistente AutoGest");
        setGraphic(IconSet.icon(IconSet.SPARKLES));
        setGraphicTextGap(8);
        getStyleClass().add("assistant-button");
    }
}
