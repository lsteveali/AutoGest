package com.autogest.controller.settings;

import com.autogest.controller.components.IconSet;
import com.autogest.exception.AutoGestException;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Piezas comunes de las secciones de Configuración del Líder que tienen
 * formulario (Facturación y Notificaciones): tarjeta con ícono, campo con
 * etiqueta y ayuda, mensaje de resultado y barra de botones.
 *
 * <p>Principles: Pure Fabrication, DRY.</p>
 */
final class SettingsForm {

    private SettingsForm() {
        // Clase de utilidad: no se instancia.
    }

    /** Tarjeta con ícono y título, como Seguridad y Copias de seguridad. */
    static VBox card(String title, String icon, Node... body) {
        Node iconNode = IconSet.icon(icon);
        iconNode.getStyleClass().add("security-card-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        HBox titleRow = new HBox(10, iconNode, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(16, titleRow);
        card.getChildren().addAll(body);
        card.getStyleClass().add("card");
        return card;
    }

    /**
     * Campo con su etiqueta encima y, si se da, una ayuda debajo.
     *
     * @param label texto de la etiqueta
     * @param field control
     * @param hint  ayuda (puede ser {@code null})
     */
    static VBox field(String label, Node field, String hint) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        labelNode.setLabelFor(field);
        VBox box = new VBox(6, labelNode, field);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
        if (field instanceof TextInputControl input) {
            input.getStyleClass().add("input");
        }
        if (hint != null) {
            box.getChildren().add(note(hint));
        }
        return box;
    }

    /** Texto de ayuda en gris. */
    static Label note(String text) {
        Label note = new Label(text);
        note.setWrapText(true);
        note.getStyleClass().add("settings-note");
        return note;
    }

    /** Dos columnas iguales. */
    static GridPane columns(Node left, Node right) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setFillWidth(true);
            grid.getColumnConstraints().add(column);
        }
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        GridPane.setValignment(left, javafx.geometry.VPos.TOP);
        GridPane.setValignment(right, javafx.geometry.VPos.TOP);
        return grid;
    }

    /** Etiqueta para el resultado de guardar (éxito o error). */
    static Label message() {
        Label message = new Label();
        message.setWrapText(true);
        message.getStyleClass().add("form-message");
        message.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(message, Priority.ALWAYS);
        return message;
    }

    /** Muestra un resultado en la etiqueta de mensaje. */
    static void show(Label message, String text, boolean error) {
        message.setText(text);
        message.getStyleClass().removeAll("form-message-error", "form-message-success");
        message.getStyleClass().add(error ? "form-message-error" : "form-message-success");
    }

    /** Texto para el usuario de un error al leer o guardar. */
    static String errorText(RuntimeException error) {
        if (error instanceof AutoGestException && error.getMessage() != null) {
            return error.getMessage();
        }
        return "No se pudo completar la operación. Revisa la conexión e inténtalo de nuevo.";
    }

    /** Barra inferior: mensaje a la izquierda y botones a la derecha. */
    static HBox actions(Label message, Button... buttons) {
        HBox bar = new HBox(12, message);
        bar.getChildren().addAll(buttons);
        bar.setAlignment(Pos.CENTER_RIGHT);
        for (Button button : buttons) {
            button.setMinWidth(Region.USE_PREF_SIZE);
        }
        return bar;
    }

    /** Botón con su estilo. */
    static Button button(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        return button;
    }
}
