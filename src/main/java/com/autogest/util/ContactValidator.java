package com.autogest.util;

import com.autogest.exception.ValidationException;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reglas compartidas para los datos de contacto que se escriben a mano:
 * correos con formato estricto (y aviso de errores de escritura comunes en el
 * dominio), teléfonos con indicativo según las reglas de cada país
 * ({@link PhoneCountry}) y nombres o apellidos solo con letras.
 *
 * <p>Lo usan los servicios (clientes, usuarios, proveedores y datos del
 * taller) para que la regla sea la misma en toda la aplicación.</p>
 *
 * <p>Principles: Pure Fabrication, DRY, SRP.</p>
 */
public final class ContactValidator {

    /** Parte local sin puntos al inicio o al final; dominio con etiquetas y extensión de letras. */
    private static final Pattern EMAIL = Pattern.compile(
            "^[a-z0-9](?:[a-z0-9._%+-]{0,62}[a-z0-9])?"
                    + "@(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,24}$");

    /** Letras (con tildes y ñ) y un espacio entre palabras. */
    private static final Pattern NAME = Pattern.compile("^\\p{L}+(?: \\p{L}+)*$");

    /** Dominios mal escritos que se ven a menudo y el dominio que se quiso escribir. */
    private static final Map<String, String> DOMAIN_TYPOS = Map.ofEntries(
            Map.entry("gmial.com", "gmail.com"), Map.entry("gmai.com", "gmail.com"),
            Map.entry("gamil.com", "gmail.com"), Map.entry("gmal.com", "gmail.com"),
            Map.entry("gmaill.com", "gmail.com"), Map.entry("gnail.com", "gmail.com"),
            Map.entry("gmail.co", "gmail.com"), Map.entry("gmail.con", "gmail.com"),
            Map.entry("gmail.cm", "gmail.com"), Map.entry("gmail.om", "gmail.com"),
            Map.entry("hotmial.com", "hotmail.com"), Map.entry("hotmai.com", "hotmail.com"),
            Map.entry("hotmal.com", "hotmail.com"), Map.entry("homtail.com", "hotmail.com"),
            Map.entry("hotmail.co", "hotmail.com"), Map.entry("hotmail.con", "hotmail.com"),
            Map.entry("outlok.com", "outlook.com"), Map.entry("outllok.com", "outlook.com"),
            Map.entry("otulook.com", "outlook.com"), Map.entry("outloo.com", "outlook.com"),
            Map.entry("outlook.co", "outlook.com"), Map.entry("outlook.con", "outlook.com"),
            Map.entry("yaho.com", "yahoo.com"), Map.entry("yahooo.com", "yahoo.com"),
            Map.entry("yahoo.co", "yahoo.com"), Map.entry("yahoo.con", "yahoo.com"),
            Map.entry("iclod.com", "icloud.com"), Map.entry("icloud.co", "icloud.com"),
            Map.entry("icloud.con", "icloud.com"));

    private ContactValidator() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Revisa un correo y lo devuelve recortado y en minúsculas.
     *
     * @param email correo escrito
     * @return el correo listo para guardar
     * @throws ValidationException si está vacío, no tiene formato válido o el
     *                             dominio parece mal escrito (propone el correcto)
     */
    public static String email(String email) {
        String value = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (value.isEmpty()) {
            throw new ValidationException("El correo electrónico es obligatorio.");
        }
        if (!EMAIL.matcher(value).matches() || value.contains("..")) {
            throw new ValidationException("Ingresa un correo válido, por ejemplo nombre@gmail.com.");
        }
        String domain = value.substring(value.indexOf('@') + 1);
        String fixed = DOMAIN_TYPOS.get(domain);
        if (fixed != null) {
            throw new ValidationException("Revisa el correo: ¿quisiste decir "
                    + value.substring(0, value.indexOf('@') + 1) + fixed + "?");
        }
        return value;
    }

    /**
     * Como {@link #email(String)}, pero un correo vacío es válido (campo opcional).
     *
     * @param email correo escrito (puede estar vacío)
     * @return el correo listo para guardar, o {@code null} si está vacío
     */
    public static String optionalEmail(String email) {
        return email == null || email.isBlank() ? null : email(email);
    }

    /**
     * Revisa un teléfono guardado o escrito ({@code +<indicativo><número>}; sin
     * indicativo se toma como de Colombia) y lo devuelve sin espacios.
     *
     * @param phone teléfono
     * @return el teléfono listo para guardar, por ejemplo {@code +573001234567}
     * @throws ValidationException si está vacío o el número no cumple las reglas del país
     */
    public static String phone(String phone) {
        String raw = phone == null ? "" : phone.trim();
        if (raw.isEmpty() || PhoneCountry.nationalPart(raw).isEmpty()) {
            throw new ValidationException("El teléfono es obligatorio.");
        }
        if (raw.replaceAll("[\\s()+-]", "").matches(".*\\D.*")) {
            throw new ValidationException("El teléfono solo puede tener números.");
        }
        PhoneCountry country = PhoneCountry.of(raw);
        String digits = PhoneCountry.nationalPart(raw);
        if (!country.accepts(digits)) {
            throw new ValidationException("El teléfono de " + country.label() + " " + country.rule() + ".");
        }
        return "+" + country.dialCode() + digits;
    }

    /**
     * Como {@link #phone(String)}, pero un teléfono vacío es válido (campo opcional).
     *
     * @param phone teléfono (puede estar vacío)
     * @return el teléfono listo para guardar, o {@code null} si está vacío
     */
    public static String optionalPhone(String phone) {
        return phone == null || PhoneCountry.nationalPart(phone).isEmpty() ? null : phone(phone);
    }

    /**
     * Revisa un nombre o apellido: obligatorio, solo letras (con tildes y ñ) y
     * un espacio entre palabras. Lo devuelve sin espacios repetidos.
     *
     * @param value nombre escrito
     * @param field «Los nombres» o «Los apellidos», para el mensaje
     * @return el nombre listo para guardar
     * @throws ValidationException si está vacío o tiene números o símbolos
     */
    public static String name(String value, String field) {
        String clean = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            throw new ValidationException(field + " son obligatorios.");
        }
        if (!NAME.matcher(clean).matches()) {
            throw new ValidationException(field + " solo pueden tener letras.");
        }
        return clean;
    }

    /**
     * Indica si un carácter puede ir en un nombre (para filtrar lo que se escribe).
     *
     * @param c carácter
     * @return {@code true} si es una letra o un espacio
     */
    public static boolean isNameCharacter(char c) {
        return Character.isLetter(c) || c == ' ';
    }
}
