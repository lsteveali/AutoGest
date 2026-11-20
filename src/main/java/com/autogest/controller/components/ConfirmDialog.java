package com.autogest.controller.components;

import com.autogest.util.AppPreferences;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

/**
 * Diálogo de confirmación para acciones destructivas (desactivar un cliente,
 * anular una factura, ...). Devuelve {@code true} solo si el usuario acepta.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class ConfirmDialog {

    private ConfirmDialog() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Muestra un diálogo de confirmación y espera la respuesta del usuario.
     *
     * @param title   título de la ventana
     * @param message mensaje explicativo (en español)
     * @return {@code true} si el usuario confirma; {@code false} en caso contrario
     */
    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        ButtonType accept = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(accept, cancel);

        return alert.showAndWait().filter(accept::equals).isPresent();
    }

    /**
     * Confirmación para eliminar un registro: respeta la preferencia «Confirmar
     * antes de eliminar registros». Si está apagada, no pregunta y devuelve
     * {@code true}.
     *
     * @param title   título de la ventana
     * @param message mensaje explicativo (en español)
     * @return {@code true} si se puede eliminar
     */
    public static boolean confirmDelete(String title, String message) {
        return !AppPreferences.confirmBeforeDelete() || confirm(title, message);
    }
}
