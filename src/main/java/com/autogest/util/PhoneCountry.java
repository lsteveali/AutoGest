package com.autogest.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

/**
 * País del teléfono: su indicativo y las reglas del número nacional (con qué
 * dígito empieza y cuántos dígitos tiene). Los teléfonos se guardan como
 * {@code +<indicativo><número>} sin espacios, por ejemplo {@code +573001234567}.
 *
 * <p>Principles: Information Expert, Open/Closed (un país nuevo es una constante).</p>
 */
public enum PhoneCountry {

    /** Celular: 3 + 9 dígitos; fijo: 60 + 8 dígitos. */
    COLOMBIA("Colombia", "57", "3[0-9]{9}|60[0-9]{8}", 10,
            "empieza por 3 (celular) o por 60 (fijo) y tiene 10 dígitos", "300 123 4567"),
    /** Celular: 4 + 9 dígitos; fijo: 2 + 9 dígitos. */
    VENEZUELA("Venezuela", "58", "[24][0-9]{9}", 10,
            "empieza por 4 (celular) o por 2 (fijo) y tiene 10 dígitos", "412 123 4567"),
    /** Celular: 9 + 8 dígitos; fijo: 2 a 7 + 7 dígitos. */
    ECUADOR("Ecuador", "593", "9[0-9]{8}|[2-7][0-9]{7}", 9,
            "empieza por 9 y tiene 9 dígitos (celular), o empieza de 2 a 7 y tiene 8 (fijo)", "99 123 4567"),
    /** Celular: 9 + 8 dígitos; fijo: 1 o 4 a 8 + 7 dígitos. */
    PERU("Perú", "51", "9[0-9]{8}|[14-8][0-9]{7}", 9,
            "empieza por 9 y tiene 9 dígitos (celular), u 8 dígitos si es fijo", "912 345 678"),
    /** Celular: 6 + 7 dígitos; fijo: 2 a 9 + 6 dígitos. */
    PANAMA("Panamá", "507", "6[0-9]{7}|[2-9][0-9]{6}", 8,
            "empieza por 6 y tiene 8 dígitos (celular), o tiene 7 dígitos si es fijo", "6123 4567"),
    /** 10 dígitos que no empiezan por 0. */
    MEXICO("México", "52", "[1-9][0-9]{9}", 10,
            "tiene 10 dígitos y no empieza por 0", "55 1234 5678"),
    /** 9 dígitos que empiezan de 2 a 9 (celular: 9). */
    CHILE("Chile", "56", "[2-9][0-9]{8}", 9,
            "empieza por 9 (celular) o de 2 a 8 (fijo) y tiene 9 dígitos", "9 1234 5678"),
    /** Celular: 9 + 10 dígitos; fijo: 10 dígitos que no empiezan por 0. */
    ARGENTINA("Argentina", "54", "9[1-9][0-9]{9}|[1-8][0-9]{9}", 11,
            "tiene 10 dígitos (fijo) o 9 más 10 dígitos (celular)", "9 11 2345 6789"),
    /** Código de área de 2 dígitos + celular (9 + 8) o fijo (2 a 5 + 7). */
    BRASIL("Brasil", "55", "[1-9]{2}(9[0-9]{8}|[2-5][0-9]{7})", 11,
            "tiene el código de área de 2 dígitos y 9 dígitos (celular) u 8 (fijo)", "11 91234 5678"),
    /** 10 dígitos: área y central no empiezan por 0 ni 1. */
    ESTADOS_UNIDOS("Estados Unidos", "1", "[2-9][0-9]{2}[2-9][0-9]{6}", 10,
            "tiene 10 dígitos y no empieza por 0 ni por 1", "305 555 1234"),
    /** 9 dígitos: celular 6 o 7, fijo 8 o 9. */
    ESPANA("España", "34", "[6-9][0-9]{8}", 9,
            "empieza por 6 o 7 (celular), u 8 o 9 (fijo) y tiene 9 dígitos", "612 34 56 78");

    /** País que se propone al escribir un teléfono nuevo. */
    public static final PhoneCountry DEFAULT = COLOMBIA;

    private final String label;
    private final String dialCode;
    private final Pattern national;
    private final int maxDigits;
    private final String rule;
    private final String example;

    PhoneCountry(String label, String dialCode, String national, int maxDigits, String rule, String example) {
        this.label = label;
        this.dialCode = dialCode;
        this.national = Pattern.compile(national);
        this.maxDigits = maxDigits;
        this.rule = rule;
        this.example = example;
    }

    /** Nombre del país en español. */
    public String label() {
        return label;
    }

    /** Indicativo sin el signo más (por ejemplo «57»). */
    public String dialCode() {
        return dialCode;
    }

    /** Máximo de dígitos del número nacional (sirve para limitar el campo). */
    public int maxDigits() {
        return maxDigits;
    }

    /** Ejemplo de número nacional para el texto de ayuda del campo. */
    public String example() {
        return example;
    }

    /** Regla del número en palabras, para los mensajes de error. */
    public String rule() {
        return rule;
    }

    /**
     * Indica si el número nacional (solo dígitos) cumple las reglas del país.
     *
     * @param digits número sin indicativo ni espacios
     * @return {@code true} si es válido
     */
    public boolean accepts(String digits) {
        return digits != null && national.matcher(digits).matches();
    }

    /** Texto del selector: «Colombia +57». */
    @Override
    public String toString() {
        return label + " +" + dialCode;
    }

    /**
     * País de un teléfono guardado ({@code +<indicativo><número>}). Un número
     * sin indicativo es de Colombia (así se guardaban antes).
     *
     * @param stored teléfono guardado
     * @return el país, o {@link #DEFAULT} si no se reconoce
     */
    public static PhoneCountry of(String stored) {
        String value = stored == null ? "" : stored.replaceAll("[\\s()-]", "");
        if (!value.startsWith("+")) {
            return DEFAULT;
        }
        return Arrays.stream(values())
                .sorted(Comparator.comparingInt((PhoneCountry c) -> c.dialCode.length()).reversed())
                .filter(c -> value.startsWith("+" + c.dialCode))
                .findFirst()
                .orElse(DEFAULT);
    }

    /**
     * Número nacional de un teléfono guardado: sin el indicativo, solo dígitos.
     *
     * @param stored teléfono guardado
     * @return los dígitos del número nacional ("" si no hay)
     */
    public static String nationalPart(String stored) {
        String value = stored == null ? "" : stored.replaceAll("[\\s()-]", "");
        if (value.startsWith("+")) {
            PhoneCountry country = of(value);
            if (value.startsWith("+" + country.dialCode)) {
                value = value.substring(country.dialCode.length() + 1);
            }
        }
        return value.replaceAll("\\D", "");
    }
}
