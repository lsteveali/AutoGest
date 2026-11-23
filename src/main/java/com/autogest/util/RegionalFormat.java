package com.autogest.util;

import com.autogest.model.SystemPreferences;

import java.text.DecimalFormatSymbols;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formato regional elegido en «Preferencias del sistema»: símbolo de la
 * moneda, separadores de miles y decimales, formato de fecha y de hora, y zona
 * horaria en que se muestran las fechas con hora. Traduce los textos de las
 * opciones (por ejemplo «DD/MM/AAAA» o «Punto (.)») a objetos de Java; una
 * opción desconocida usa el formato de Colombia.
 *
 * @param currencySymbol símbolo que antecede los valores, por ejemplo «$» o «US$»
 * @param symbols        separadores de miles y decimales
 * @param date           formato de fecha
 * @param timestamp      formato de fecha y hora
 * @param zone           zona horaria en que se muestran las fechas con hora
 */
public record RegionalFormat(String currencySymbol, DecimalFormatSymbols symbols, DateTimeFormatter date,
                             DateTimeFormatter timestamp, ZoneId zone) {

    private static final Locale SPANISH = Locale.forLanguageTag("es-CO");

    /**
     * Arma el formato a partir de las preferencias.
     *
     * @param preferences preferencias del sistema ({@code null} = las de fábrica)
     * @return formato listo para usar
     */
    public static RegionalFormat of(SystemPreferences preferences) {
        SystemPreferences p = preferences == null ? SystemPreferences.defaults() : preferences;
        String datePattern = datePattern(p.getDateFormat());
        String timePattern = "12 horas".equals(p.getTimeFormat()) ? "h:mm a" : "HH:mm";
        return new RegionalFormat(currencySymbol(p.getCurrency()), symbols(p.getDecimalSeparator()),
                DateTimeFormatter.ofPattern(datePattern, SPANISH),
                DateTimeFormatter.ofPattern(datePattern + " " + timePattern, SPANISH),
                zone(p.getTimeZone()));
    }

    /** Símbolo de la moneda según la opción de «Moneda». */
    static String currencySymbol(String currency) {
        if (currency == null) {
            return "$";
        }
        if (currency.contains("USD")) {
            return "US$";
        }
        if (currency.contains("PEN")) {
            return "S/";
        }
        if (currency.contains("MXN")) {
            return "MX$";
        }
        return "$";
    }

    /** Separadores: con «Punto (.)» los miles van con coma; si no, como en Colombia. */
    static DecimalFormatSymbols symbols(String decimalSeparator) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(SPANISH);
        boolean point = decimalSeparator != null && decimalSeparator.startsWith("Punto");
        symbols.setGroupingSeparator(point ? ',' : '.');
        symbols.setDecimalSeparator(point ? '.' : ',');
        return symbols;
    }

    /** Patrón de fecha según la opción de «Formato de fecha». */
    static String datePattern(String dateFormat) {
        if ("AAAA-MM-DD".equals(dateFormat)) {
            return "yyyy-MM-dd";
        }
        if ("MM/DD/AAAA".equals(dateFormat)) {
            return "MM/dd/yyyy";
        }
        return "dd/MM/yyyy";
    }

    /** Zona horaria según la opción de «Zona horaria». */
    static ZoneId zone(String timeZone) {
        if (timeZone == null) {
            return ZoneId.of("America/Bogota");
        }
        if (timeZone.contains("Caracas")) {
            return ZoneId.of("America/Caracas");
        }
        if (timeZone.contains("México")) {
            return ZoneId.of("America/Mexico_City");
        }
        return ZoneId.of("America/Bogota");
    }
}
