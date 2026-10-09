package com.autogest.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formato de valores para mostrarlos al usuario, centralizado en un solo lugar
 * para que toda la aplicación use la misma convención colombiana: dinero con
 * separador de miles «.» y decimal «,», y fechas en {@code dd/MM/yyyy}.
 *
 * <p>No formatea lógica de negocio ni cambia valores: solo convierte a texto.
 * Los Services siguen trabajando con {@link BigDecimal} y {@link LocalDate}.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion, Protected Variations.</p>
 */
public final class Formatter {

    private static final DecimalFormatSymbols MONEY_SYMBOLS = moneySymbols();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Formatter() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Da formato de moneda a un valor (por ejemplo 120000 -> «$120.000»).
     * Redondea a dos decimales con {@link RoundingMode#HALF_UP}; si el valor no
     * tiene centavos muestra solo los miles, y si los tiene muestra siempre dos
     * decimales (por ejemplo 1234.5 -> «$1.234,50»).
     *
     * @param amount valor monetario; puede ser nulo
     * @return texto con signo «$» y separadores colombianos
     */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "$0";
        }
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        boolean hasDecimals = value.remainder(BigDecimal.ONE).signum() != 0;
        String pattern = hasDecimals ? "#,##0.00" : "#,##0";
        return "$" + new DecimalFormat(pattern, MONEY_SYMBOLS).format(value);
    }

    /**
     * Da formato de fecha corta (por ejemplo 15/06/2026).
     *
     * @param date fecha; puede ser nula
     * @return fecha en {@code dd/MM/yyyy} o cadena vacía si es nula
     */
    public static String date(LocalDate date) {
        return date == null ? "" : DATE_FORMAT.format(date);
    }

    /**
     * Da formato de fecha y hora (por ejemplo 15/06/2026 14:30).
     *
     * @param dateTime fecha y hora; puede ser nula
     * @return fecha y hora en {@code dd/MM/yyyy HH:mm} o cadena vacía si es nula
     */
    public static String timestamp(LocalDateTime dateTime) {
        return dateTime == null ? "" : TIMESTAMP_FORMAT.format(dateTime);
    }

    /** Construye los símbolos de moneda colombianos (miles «.», decimal «,»). */
    private static DecimalFormatSymbols moneySymbols() {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("es-CO"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return symbols;
    }
}
