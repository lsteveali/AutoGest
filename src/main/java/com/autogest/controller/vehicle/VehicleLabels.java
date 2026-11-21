package com.autogest.controller.vehicle;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Catalogs.FuelType;
import com.autogest.model.Catalogs.Transmission;
import com.autogest.model.Vehicle;
import com.autogest.model.Catalogs.VehicleStatus;
import com.autogest.model.Catalogs.VehicleType;
import com.autogest.util.Formatter;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Traduce los valores del dominio de vehículos a texto en español para las
 * pantallas (estado, clase, combustible, transmisión, kilometraje y aviso de
 * mantenimiento) y hace la conversión inversa que necesitan los filtros y las
 * listas desplegables. Centraliza estos textos para que el modelo no contenga
 * recursos de presentación y todas las pantallas del módulo muestren lo mismo.
 *
 * <p>Principles: Pure Fabrication, High Cohesion, Protected Variations.</p>
 */
public final class VehicleLabels {

    private static final DecimalFormatSymbols NUMBER_SYMBOLS = numberSymbols();

    private VehicleLabels() {
        // Clase de utilidad: no se instancia.
    }

    // ===== Estado =====

    /**
     * Devuelve el texto del estado de un vehículo.
     *
     * @param status estado a traducir; puede ser nulo
     * @return «Activo», «En servicio», «Inactivo» o cadena vacía
     */
    public static String statusText(VehicleStatus status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case ACTIVE -> "Activo";
            case IN_SERVICE -> "En servicio";
            case INACTIVE -> "Inactivo";
        };
    }

    /**
     * Convierte el texto de un estado de vuelta al valor del dominio.
     *
     * @param text texto mostrado en pantalla; puede ser nulo o «Todos»
     * @return el estado correspondiente o {@code null} si no aplica ninguno
     */
    public static VehicleStatus statusFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (VehicleStatus status : VehicleStatus.values()) {
            if (statusText(status).equalsIgnoreCase(text.trim())) {
                return status;
            }
        }
        return null;
    }

    /**
     * Devuelve el tipo de insignia que corresponde al estado de un vehículo.
     *
     * @param status estado a representar
     * @return {@code SUCCESS} activo, {@code INFO} en servicio, {@code NEUTRAL} inactivo
     */
    public static StatusBadge.BadgeType statusType(VehicleStatus status) {
        if (status == null) {
            return StatusBadge.BadgeType.NEUTRAL;
        }
        return switch (status) {
            case ACTIVE -> StatusBadge.BadgeType.SUCCESS;
            case IN_SERVICE -> StatusBadge.BadgeType.INFO;
            case INACTIVE -> StatusBadge.BadgeType.NEUTRAL;
        };
    }

    // ===== Clase de vehículo =====

    /**
     * Devuelve el nombre de la clase de vehículo tal como se muestra en pantalla.
     *
     * @param type clase a traducir; puede ser nulo
     * @return nombre en español o cadena vacía
     */
    public static String typeText(VehicleType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case CAR -> "Automóvil";
            case VAN -> "Camioneta";
            case MOTORCYCLE -> "Motocicleta";
            case OTHER -> "Otro";
        };
    }

    /**
     * Convierte el texto de una clase de vehículo de vuelta al valor del dominio.
     *
     * @param text texto mostrado en pantalla; puede ser nulo o «Todos»
     * @return la clase correspondiente o {@code null} si no aplica ninguna
     */
    public static VehicleType typeFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (VehicleType type : VehicleType.values()) {
            if (typeText(type).equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return null;
    }

    // ===== Combustible =====

    /**
     * Devuelve el nombre del tipo de combustible.
     *
     * @param fuelType combustible a traducir; puede ser nulo
     * @return nombre en español o cadena vacía
     */
    public static String fuelText(FuelType fuelType) {
        if (fuelType == null) {
            return "";
        }
        return switch (fuelType) {
            case GASOLINE -> "Gasolina";
            case DIESEL -> "Diésel";
            case HYBRID -> "Híbrido";
            case ELECTRIC -> "Eléctrico";
            case GAS -> "Gas";
        };
    }

    /**
     * Convierte el texto de un combustible de vuelta al valor del dominio.
     *
     * @param text texto mostrado en pantalla; puede ser nulo
     * @return el combustible correspondiente o {@code null} si no aplica ninguno
     */
    public static FuelType fuelFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (FuelType fuelType : FuelType.values()) {
            if (fuelText(fuelType).equalsIgnoreCase(text.trim())) {
                return fuelType;
            }
        }
        return null;
    }

    // ===== Transmisión =====

    /**
     * Devuelve el nombre del tipo de transmisión.
     *
     * @param transmission transmisión a traducir; puede ser nula
     * @return «Manual», «Automática» o cadena vacía
     */
    public static String transmissionText(Transmission transmission) {
        if (transmission == null) {
            return "";
        }
        return transmission == Transmission.MANUAL ? "Manual" : "Automática";
    }

    /**
     * Convierte el texto de una transmisión de vuelta al valor del dominio.
     *
     * @param text texto mostrado en pantalla; puede ser nulo
     * @return la transmisión correspondiente o {@code null} si no aplica ninguna
     */
    public static Transmission transmissionFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (Transmission transmission : Transmission.values()) {
            if (transmissionText(transmission).equalsIgnoreCase(text.trim())) {
                return transmission;
            }
        }
        return null;
    }

    // ===== Kilometraje y mantenimiento =====

    /**
     * Da formato al kilometraje con separador de miles (por ejemplo «50.000 km»).
     *
     * @param mileage kilometraje; puede ser nulo
     * @return texto con la unidad «km» o «—» si es nulo
     */
    public static String mileage(Integer mileage) {
        if (mileage == null) {
            return "—";
        }
        return new DecimalFormat("#,##0", NUMBER_SYMBOLS).format(mileage) + " km";
    }

    /**
     * Describe el estado del próximo mantenimiento de un vehículo para hoy.
     *
     * @param vehicle vehículo a evaluar
     * @return «Vencido», «Próximo», «Al día» o «—» si no hay fecha
     */
    public static String maintenanceText(Vehicle vehicle) {
        LocalDate next = vehicle == null ? null : vehicle.getNextMaintenanceDate();
        if (next == null) {
            return "—";
        }
        LocalDate today = LocalDate.now();
        if (next.isBefore(today)) {
            return "Vencido";
        }
        return vehicle.isMaintenanceDue(today) ? "Próximo" : "Al día";
    }

    /**
     * Devuelve el tipo de insignia del aviso de mantenimiento.
     *
     * @param vehicle vehículo a evaluar
     * @return {@code DANGER} vencido, {@code WARNING} próximo, {@code SUCCESS} al día,
     *         {@code NEUTRAL} sin fecha
     */
    public static StatusBadge.BadgeType maintenanceType(Vehicle vehicle) {
        String text = maintenanceText(vehicle);
        return switch (text) {
            case "Vencido" -> StatusBadge.BadgeType.DANGER;
            case "Próximo" -> StatusBadge.BadgeType.WARNING;
            case "Al día" -> StatusBadge.BadgeType.SUCCESS;
            default -> StatusBadge.BadgeType.NEUTRAL;
        };
    }

    /**
     * Describe la fecha del próximo mantenimiento (por ejemplo «Vence el 15/06/2026»).
     *
     * @param vehicle vehículo a evaluar
     * @return texto con la fecha o «Sin programar» si no hay fecha
     */
    public static String nextMaintenanceDateText(Vehicle vehicle) {
        LocalDate next = vehicle == null ? null : vehicle.getNextMaintenanceDate();
        return next == null ? "Sin programar" : "Vence el " + Formatter.date(next);
    }

    // ===== Opciones para listas desplegables =====

    /** @return textos de estado para un filtro o lista desplegable */
    public static List<String> statusOptions() {
        return Arrays.stream(VehicleStatus.values()).map(VehicleLabels::statusText).toList();
    }

    /** @return textos de clase de vehículo para un filtro o lista desplegable */
    public static List<String> typeOptions() {
        return Arrays.stream(VehicleType.values()).map(VehicleLabels::typeText).toList();
    }

    /** @return textos de combustible para una lista desplegable */
    public static List<String> fuelOptions() {
        return Arrays.stream(FuelType.values()).map(VehicleLabels::fuelText).toList();
    }

    /** @return textos de transmisión para una lista desplegable */
    public static List<String> transmissionOptions() {
        return Arrays.stream(Transmission.values()).map(VehicleLabels::transmissionText).toList();
    }

    /** Construye los símbolos numéricos colombianos (miles «.»). */
    private static DecimalFormatSymbols numberSymbols() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));
        symbols.setGroupingSeparator('.');
        return symbols;
    }
}
