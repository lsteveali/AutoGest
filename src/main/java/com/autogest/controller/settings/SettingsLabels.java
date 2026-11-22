package com.autogest.controller.settings;

import java.util.List;

/**
 * Opciones de los desplegables de «Datos del taller» y «Preferencias del
 * sistema» (textos del mockup). Viven aquí para no repetirlas en los
 * controladores y poder probarlas sin JavaFX.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class SettingsLabels {

    private SettingsLabels() {
        // Clase de utilidad: no se instancia.
    }

    public static List<String> workshopTypes() {
        return List.of("Taller automotriz general", "Taller de motos", "Centro de servicio rápido",
                "Taller de latonería y pintura", "Taller especializado");
    }

    public static List<String> countries() {
        return List.of("Colombia", "Ecuador", "Perú", "Venezuela", "Panamá", "México");
    }

    public static List<String> languages() {
        return List.of("Español", "English");
    }

    public static List<String> timeZones() {
        return List.of("(GMT-05:00) Bogotá, Lima, Quito", "(GMT-04:00) Caracas", "(GMT-06:00) Ciudad de México");
    }

    public static List<String> currencies() {
        return List.of("Peso colombiano (COP)", "Dólar estadounidense (USD)", "Sol peruano (PEN)",
                "Peso mexicano (MXN)");
    }

    public static List<String> dateFormats() {
        return List.of("DD/MM/AAAA", "AAAA-MM-DD", "MM/DD/AAAA");
    }

    public static List<String> timeFormats() {
        return List.of("24 horas", "12 horas");
    }

    public static List<String> decimalSeparators() {
        return List.of("Coma (,)", "Punto (.)");
    }

    public static List<String> sidebarModes() {
        return List.of("Expandido", "Compacto");
    }

    public static List<String> sortOrders() {
        return List.of("Más recientes primero", "Más antiguos primero", "Alfabético");
    }

    /** Opciones de inactividad en minutos. */
    public static List<Integer> inactivityMinutes() {
        return List.of(15, 30, 60, 120);
    }

    /** «30 minutos» / «1 hora» / «2 horas». */
    public static String minutesText(int minutes) {
        if (minutes % 60 == 0) {
            int hours = minutes / 60;
            return hours + (hours == 1 ? " hora" : " horas");
        }
        return minutes + " minutos";
    }
}
