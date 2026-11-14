package com.autogest.controller.inventory;

import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.exception.ValidationException;
import com.autogest.model.MovementType;
import com.autogest.model.Part;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.net.URL;
import java.util.List;
import java.util.Optional;

/**
 * Diálogo para registrar un movimiento de inventario (entrada, salida o
 * ajuste). Solo recoge los datos; quien lo abre llama a
 * {@code InventoryService}, que es quien valida y guarda.
 *
 * <p>Principles: GRASP Controller, SRP, High Cohesion.</p>
 */
public final class MovementDialog {

    /**
     * Datos capturados en el diálogo.
     *
     * @param part     repuesto elegido
     * @param quantity cantidad (o stock real si es un ajuste)
     * @param reason   motivo escrito por el usuario (puede ser {@code null})
     */
    public record Request(Part part, int quantity, String reason) {
    }

    private MovementDialog() {
        // Se usa a través de show(...).
    }

    /**
     * Abre el diálogo.
     *
     * @param type  tipo de movimiento
     * @param fixed repuesto ya elegido (desde el detalle) o {@code null} para elegirlo
     * @param parts repuestos para elegir cuando {@code fixed} es {@code null}
     * @return los datos si el usuario confirma con valores válidos
     */
    public static Optional<Request> show(MovementType type, Part fixed, List<Part> parts) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title(type));
        dialog.setHeaderText(null);
        URL css = MovementDialog.class.getResource("/css/autogest.css");
        if (css != null) {
            dialog.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
        ButtonType confirm = new ButtonType(confirmText(type), ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, confirm);

        ComboBox<Part> partBox = FormLayout.combo("Seleccionar repuesto",
                part -> part.getCode() + " · " + part.getName() + " (stock " + part.getCurrentStock() + ")");
        if (fixed != null) {
            partBox.getItems().setAll(fixed);
            partBox.setValue(fixed);
            partBox.setDisable(true);
        } else {
            partBox.getItems().setAll(parts);
        }
        TextField quantityField = FormLayout.textField(type == MovementType.ADJUSTMENT ? "Ej. 12" : "Ej. 5");
        TextArea reasonArea = FormLayout.textArea(reasonPrompt(type), 3);

        GridPane grid = FormLayout.grid(1);
        grid.setPrefWidth(420);
        FormLayout.row(grid, 0, FormLayout.labeled("Repuesto *", partBox));
        FormLayout.row(grid, 1, FormLayout.labeled(type == MovementType.ADJUSTMENT
                ? "Stock real contado *" : "Cantidad *", quantityField));
        FormLayout.row(grid, 2, FormLayout.labeled(type == MovementType.ADJUSTMENT
                ? "Motivo del ajuste *" : "Motivo", reasonArea));
        if (type == MovementType.ADJUSTMENT) {
            Label hint = new Label("El sistema guarda la diferencia con el stock actual.");
            hint.getStyleClass().add("page-subtitle");
            FormLayout.row(grid, 3, hint);
        }
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get() != confirm) {
            return Optional.empty();
        }
        try {
            if (partBox.getValue() == null) {
                throw new ValidationException("Selecciona un repuesto");
            }
            int quantity = FormLayout.parseInt(quantityField.getText(),
                    type == MovementType.ADJUSTMENT ? "El stock real" : "La cantidad", true);
            return Optional.of(new Request(partBox.getValue(), quantity, FormLayout.text(reasonArea)));
        } catch (ValidationException e) {
            ErrorDialog.show("Revisa el movimiento", e.getMessage());
            return Optional.empty();
        }
    }

    private static String title(MovementType type) {
        return switch (type) {
            case ENTRY -> "Registrar entrada de stock";
            case EXIT -> "Registrar salida de stock";
            case ADJUSTMENT -> "Ajustar stock";
        };
    }

    private static String confirmText(MovementType type) {
        return switch (type) {
            case ENTRY -> "Registrar entrada";
            case EXIT -> "Registrar salida";
            case ADJUSTMENT -> "Ajustar";
        };
    }

    private static String reasonPrompt(MovementType type) {
        return switch (type) {
            case ENTRY -> "Ej. Compra a AutoPartes Colombia, factura 123";
            case EXIT -> "Ej. Consumo interno, pieza dañada...";
            case ADJUSTMENT -> "Ej. Conteo físico mensual";
        };
    }
}
