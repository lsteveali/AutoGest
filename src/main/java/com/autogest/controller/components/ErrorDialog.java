package com.autogest.controller.components;

/**
 * Diálogo para mostrar errores al usuario sin exponer trazas técnicas. Los
 * controladores lo usan en sus bloques {@code catch} con el mensaje que lanzó
 * el Service.
 *
 * <p>Se muestra con el diseño de AutoGest ({@link AppDialog}).</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class ErrorDialog {

    private ErrorDialog() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Muestra un error con el título por defecto «Error».
     *
     * @param message mensaje del error (en español)
     */
    public static void show(String message) {
        show("Error", message);
    }

    /**
     * Muestra un error con un título personalizado.
     *
     * @param title   título de la ventana
     * @param message mensaje del error (en español)
     */
    public static void show(String title, String message) {
        AppDialog.error(title, message);
    }
}
