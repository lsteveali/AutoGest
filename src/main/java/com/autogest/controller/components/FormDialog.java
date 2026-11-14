package com.autogest.controller.components;

import com.autogest.exception.AutoGestException;
import com.autogest.util.Background;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.function.Supplier;

/**
 * Diálogo de formulario con el aspecto de la app (encabezado propio, botones
 * «Cancelar» y acción principal). Al pulsar la acción, la ejecuta en segundo
 * plano; si falla, muestra el error junto a los botones y no se cierra, así lo
 * escrito no se pierde. Devuelve {@code true} si la acción terminó bien.
 *
 * <p>Uso: {@code new FormDialog("Registrar diagnóstico", "…", contenido,
 * "Guardar", () -> servicio.guardar(…)).showAndWait()}.</p>
 *
 * <p>Principles: Pure Fabrication, DRY (mismo comportamiento que el
 * formulario de usuario).</p>
 */
public class FormDialog extends Dialog<Boolean> {

    private static final String STYLESHEET = "/css/autogest.css";

    private final Label error = new Label();

    /**
     * @param title      título del diálogo
     * @param subtitle   texto de ayuda bajo el título (puede ser {@code null})
     * @param content    campos del formulario
     * @param actionText texto del botón principal
     * @param action     trabajo a ejecutar; lanza una excepción con mensaje si los datos no son válidos
     */
    public FormDialog(String title, String subtitle, Node content, String actionText, Supplier<?> action) {
        setTitle(title);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        VBox header = new VBox(4, titleLabel);
        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("dialog-subtitle");
            subtitleLabel.setWrapText(true);
            header.getChildren().add(subtitleLabel);
        }
        error.getStyleClass().addAll("form-message", "form-message-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox body = new VBox(16, header, content, error);
        body.getStyleClass().add("dialog-content");

        DialogPane pane = getDialogPane();
        pane.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        pane.getStyleClass().add("app-dialog");
        pane.setPrefWidth(500);
        pane.setContent(body);

        ButtonType actionType = new ButtonType(actionText, ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, actionType);
        Button primary = (Button) pane.lookupButton(actionType);
        primary.getStyleClass().add("btn-primary");
        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancel.setText("Cancelar");
        cancel.getStyleClass().add("btn-secondary");
        ButtonBar bar = (ButtonBar) pane.lookup(".button-bar");
        if (bar != null) {
            bar.setButtonOrder(ButtonBar.BUTTON_ORDER_MAC_OS);
        }

        primary.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            primary.setDisable(true);
            cancel.setDisable(true);
            error.setVisible(false);
            error.setManaged(false);
            Background.run(action::get,
                    result -> {
                        setResult(Boolean.TRUE);
                        close();
                    },
                    failure -> {
                        primary.setDisable(false);
                        cancel.setDisable(false);
                        error.setText(failure instanceof AutoGestException && failure.getMessage() != null
                                ? failure.getMessage()
                                : "No se pudo completar. Revisa la conexión e inténtalo de nuevo.");
                        error.setVisible(true);
                        error.setManaged(true);
                        pane.getScene().getWindow().sizeToScene();
                    });
        });
        setResultConverter(button -> Boolean.FALSE);
    }

    /**
     * Muestra el diálogo y espera.
     *
     * @return {@code true} si la acción se completó
     */
    public boolean open() {
        return showAndWait().orElse(Boolean.FALSE);
    }
}
