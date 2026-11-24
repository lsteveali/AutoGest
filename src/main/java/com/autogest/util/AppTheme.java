package com.autogest.util;

/**
 * Tema visual de AutoGest en este equipo: claro (fondo Ivory, texto Steel y el
 * taller de día) u oscuro (fondo Steel, texto Ivory y el taller de noche). Se
 * aplica a toda la aplicación, login incluido, con {@link ThemeManager} y se
 * guarda con {@link LoginAppearanceStore}.
 */
public enum AppTheme {

    /** Fondo Ivory, texto Steel y el taller de día. */
    LIGHT("Claro"),
    /** Fondo Steel, texto Ivory y el taller de noche. */
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

    /** El otro tema (lo usa el botón de la barra superior). */
    public AppTheme toggled() {
        return this == DARK ? LIGHT : DARK;
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
