package com.autogest.controller.components;

import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

/**
 * Pintura de los vehículos de la vitrina a partir de un solo color. Arma el
 * degradado que imita el reflejo del entorno (cielo claro arriba, línea del
 * horizonte más oscura a media puerta y piso abajo) con las mismas
 * proporciones del plateado original, así cualquier color se ve metalizado.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
final class VehiclePaint {

    /** Posiciones del degradado (coordenada V de la textura). */
    static final double[] STOPS = {0.0, 0.25, 0.47, 0.50, 0.53, 0.75, 1.0};

    private VehiclePaint() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Tonos del degradado para cada posición de {@link #STOPS}: dos reflejos
     * del cielo (mezcla con blanco), el color base, el horizonte oscuro y la
     * subida hacia el piso.
     *
     * @param base color de la pintura
     * @return siete tonos, del cielo al piso
     */
    static Color[] tones(Color base) {
        return new Color[]{
                base.interpolate(Color.WHITE, 0.28),
                base.interpolate(Color.WHITE, 0.18),
                base,
                darker(base, 0.72),
                darker(base, 0.82),
                darker(base, 0.91),
                darker(base, 0.49)};
    }

    /**
     * Material de la pintura con su brillo.
     *
     * @param base color de la pintura
     * @return material listo para la malla de la carrocería
     */
    static PhongMaterial material(Color base) {
        Color[] tones = tones(base);
        Object[] stops = new Object[2 * STOPS.length];
        for (int i = 0; i < STOPS.length; i++) {
            stops[2 * i] = STOPS[i];
            stops[2 * i + 1] = tones[i];
        }
        return MeshKit.material(MeshKit.gradient(stops), 0.55, 36);
    }

    private static Color darker(Color color, double factor) {
        return Color.color(color.getRed() * factor, color.getGreen() * factor, color.getBlue() * factor);
    }
}
