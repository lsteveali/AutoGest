package com.autogest.controller.components;

import com.autogest.util.AppPreferences;

/**
 * Diálogo de confirmación para acciones destructivas (desactivar un cliente,
 * anular una factura, ...). Devuelve {@code true} solo si el usuario acepta.
 *
 * <p>Se muestra con el diseño de AutoGest ({@link AppDialog}); al eliminar, el
 * botón que confirma va en rojo.</p>
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
        return AppDialog.confirm(title, message, "Aceptar", false);
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
        return !AppPreferences.confirmBeforeDelete() || AppDialog.confirm(title, message, "Eliminar", true);
    }
}
