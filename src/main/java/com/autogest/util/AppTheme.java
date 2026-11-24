package com.autogest.util;

/**
 * Tema visual de AutoGest en este equipo. Por ahora cambia el inicio de
 * sesión: el taller cálido con el formulario claro, o el taller azul nocturno
 * con el formulario oscuro. Se guarda con {@link LoginAppearanceStore}.
 */
public enum AppTheme {

    /** Taller cálido y formulario claro (el de siempre). */
    LIGHT("Claro"),
    /** Taller azul nocturno y formulario oscuro. */
    DARK("Oscuro");

    private final String label;

    AppTheme(String label) {
        this.label = label;
    }

    /** Nombre para mostrar en pantalla. */
    public String label() {
        return label;
    }

    /** Clase CSS que se pone en la raíz de la pantalla para este tema. */
    public String styleClass() {
        return this == DARK ? "theme-dark" : "theme-light";
    }

    /**
     * Tema guardado como texto; uno desconocido o vacío es el claro.
     *
     * @param name nombre de la constante
     * @return el tema
     */
    public static AppTheme parse(String name) {
        if (name == null) {
            return LIGHT;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException unknown) {
            return LIGHT;
        }
    }
}
