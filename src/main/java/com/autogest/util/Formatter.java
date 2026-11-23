package com.autogest.util;

import com.autogest.model.SystemPreferences;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Formato de valores para mostrarlos al usuario, centralizado en un solo lugar
 * para que toda la aplicación use la misma convención. Sigue el formato
 * regional de «Preferencias del sistema» ({@link RegionalFormat}): símbolo de
 * la moneda, separadores, formato de fecha y de hora, y zona horaria. Las de
 * fábrica son las de Colombia: dinero con separador de miles «.» y decimal «,»,
 * y fechas en {@code dd/MM/yyyy}.
 *
 * <p>No formatea lógica de negocio ni cambia valores: solo convierte a texto.
 * Los Services siguen trabajando con {@link BigDecimal} y {@link LocalDate}.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion, Protected Variations.</p>
 */
public final class Formatter {

    /** Formato armado con las últimas preferencias vistas (se rehace cuando cambian). */
    private static volatile Cached cached;

    private record Cached(SystemPreferences source, RegionalFormat format) {
    }

    private Formatter() {
        // Clase de utilidad: no se instancia.
    }

    /** Formato regional vigente según {@link AppPreferences}. */
    static RegionalFormat format() {
        SystemPreferences preferences = AppPreferences.current();
        Cached current = cached;
        if (current == null || current.source() != preferences) {
            current = new Cached(preferences, RegionalFormat.of(preferences));
            cached = current;
        }
        return current.format();
    }

    /**
     * Da formato de moneda a un valor (por ejemplo 120000 -> «$120.000»).
     * Redondea a dos decimales con {@link RoundingMode#HALF_UP}; si el valor no
     * tiene centavos muestra solo los miles, y si los tiene muestra siempre dos
     * decimales (por ejemplo 1234.5 -> «$1.234,50»).
     *
     * @param amount valor monetario; puede ser nulo
     * @return texto con el símbolo de la moneda y los separadores elegidos
     */
    public static String money(BigDecimal amount) {
        RegionalFormat format = format();
        if (amount == null) {
            return format.currencySymbol() + "0";
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        boolean hasDecimals = value.remainder(BigDecimal.ONE).signum() != 0;
        String pattern = hasDecimals ? "#,##0.00" : "#,##0";
        return format.currencySymbol() + new DecimalFormat(pattern, format.symbols()).format(value);
    }

    /**
     * Da formato a un número entero con el separador de miles elegido (por
     * ejemplo el kilometraje: 87300 -> «87.300»).
     *
     * @param value número
     * @return texto con separador de miles
     */
    public static String integer(long value) {
        return new DecimalFormat("#,##0", format().symbols()).format(value);
    }

    /**
     * Da formato de fecha corta (por ejemplo 15/06/2026).
     *
     * @param date fecha; puede ser nula
     * @return fecha en el formato elegido o cadena vacía si es nula
     */
    public static String date(LocalDate date) {
        return date == null ? "" : format().date().format(date);
    }

    /**
     * Da formato de fecha y hora (por ejemplo 15/06/2026 14:30). La hora se
     * guarda en la zona del equipo y se muestra en la zona horaria elegida.
     *
     * @param dateTime fecha y hora; puede ser nula
     * @return fecha y hora en el formato elegido o cadena vacía si es nula
     */
    public static String timestamp(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        RegionalFormat format = format();
        LocalDateTime shown = dateTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(format.zone())
                .toLocalDateTime();
        return format.timestamp().format(shown);
    }
}
