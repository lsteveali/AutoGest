package com.autogest.util;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Personalización de la vitrina del inicio de sesión: qué vehículo gira y de
 * qué color está pintado. Se guarda en cada computador
 * ({@link LoginAppearanceStore}), no en la base de datos, porque se lee antes
 * de iniciar sesión.
 *
 * <p>El color va como texto {@code #RRGGBB}; uno inválido o vacío vuelve al
 * plateado de fábrica, y un vehículo nulo vuelve a la camioneta.</p>
 *
 * @param vehicle vehículo de la vitrina
 * @param color   color de la pintura en formato {@code #RRGGBB}
 */
public record LoginAppearance(Vehicle vehicle, String color) {

    /** Plateado original de la camioneta. */
    public static final String DEFAULT_COLOR = "#C2C8CF";

    /** Va antes de {@link #DEFAULT}, que lo usa al crearse. */
    private static final Pattern HEX = Pattern.compile("#[0-9A-F]{6}");

    /** Lo que muestra el login si nunca se personalizó. */
    public static final LoginAppearance DEFAULT = new LoginAppearance(Vehicle.TRUCK, DEFAULT_COLOR);

    /** Vehículos del catálogo de la vitrina. */
    public enum Vehicle {
        /** Camioneta todoterreno de dos puertas (la original). */
        TRUCK("Camioneta"),
        /** Sedán de cuatro puertas. */
        SEDAN("Sedán"),
        /** Deportivo de dos puertas con alerón. */
        SPORTS("Deportivo");

        private final String label;

        Vehicle(String label) {
            this.label = label;
        }

        /** Nombre para mostrar en pantalla. */
        public String label() {
            return label;
        }
    }

    /**
     * Color predefinido de la paleta.
     *
     * @param name nombre para mostrar
     * @param hex  color en formato {@code #RRGGBB}
     */
    public record Swatch(String name, String hex) {
    }

    /** Colores sugeridos, elegidos para que la pintura se vea bien con la luz de la vitrina. */
    public static final List<Swatch> PALETTE = List.of(
            new Swatch("Plata", DEFAULT_COLOR),
            new Swatch("Blanco perla", "#E9ECEF"),
            new Swatch("Negro", "#2A2D31"),
            new Swatch("Rojo", "#B3261E"),
            new Swatch("Azul", "#1F4E8C"),
            new Swatch("Verde", "#2F6B3A"),
            new Swatch("Caramelo", "#C65C00"),
            new Swatch("Amarillo", "#E0B021"));

    public LoginAppearance {
        vehicle = vehicle == null ? Vehicle.TRUCK : vehicle;
        color = normalizeColor(color);
    }

    /**
     * Lleva un color a {@code #RRGGBB} en mayúsculas. Acepta el numeral
     * opcional; cualquier otra cosa devuelve {@link #DEFAULT_COLOR}.
     *
     * @param color texto del color
     * @return color normalizado
     */
    public static String normalizeColor(String color) {
        if (color == null) {
            return DEFAULT_COLOR;
        }
        String value = color.trim().toUpperCase(Locale.ROOT);
        if (!value.startsWith("#")) {
            value = "#" + value;
        }
        return HEX.matcher(value).matches() ? value : DEFAULT_COLOR;
    }

    /** Copia con otro vehículo. */
    public LoginAppearance withVehicle(Vehicle newVehicle) {
        return new LoginAppearance(newVehicle, color);
    }

    /** Copia con otro color. */
    public LoginAppearance withColor(String newColor) {
        return new LoginAppearance(vehicle, newColor);
    }

    /**
     * Nombre del color si está en la paleta; si no, «Personalizado».
     *
     * @return nombre para mostrar
     */
    public String colorName() {
        return PALETTE.stream()
                .filter(swatch -> swatch.hex().equals(color))
                .map(Swatch::name)
                .findFirst()
                .orElse("Personalizado");
    }
}
