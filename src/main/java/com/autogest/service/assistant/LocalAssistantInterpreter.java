package com.autogest.service.assistant;

import com.autogest.service.assistant.AssistantCatalog.Destination;
import com.autogest.service.assistant.AssistantCatalog.Filter;
import com.autogest.service.assistant.AssistantIntent.Kind;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor local del Asistente: entiende pedidos frecuentes en español con
 * reglas, sin internet y sin enviar datos afuera. Reconoce el módulo por sus
 * palabras, los filtros por las opciones que se ven en pantalla («En
 * reparación», «Vencida», «Stock bajo»…), y referencias concretas: placas,
 * números de orden y de factura, nombres con mayúscula y textos entre
 * comillas.
 *
 * <p>Ejemplos: «órdenes en reparación», «facturas vencidas», «abre el
 * vehículo ABC123», «nueva orden para ABC123», «clientes inactivos»,
 * «nueva notificación de promoción para Ana Gómez».</p>
 *
 * <p>Solo interpreta: no consulta la base de datos ni navega.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations (el vocabulario sale
 * del catálogo, no está escrito aquí).</p>
 */
public class LocalAssistantInterpreter {

    /** Códigos de los módulos a los que llevan las referencias sueltas. */
    public static final String VEHICLES = "VEHICLES";
    public static final String ORDERS = "ORDERS";
    public static final String BILLING = "BILLING";
    public static final String CUSTOMERS = "CUSTOMERS";

    /** Claves de {@link AssistantIntent#fields()} que no son etiquetas de pantalla. */
    public static final String FIELD_PLATE = "placa";
    public static final String FIELD_ORDER = "orden";
    public static final String FIELD_INVOICE = "factura";
    public static final String FIELD_CUSTOMER = "cliente";
    public static final String FIELD_MESSAGE = "mensaje";

    private static final Pattern HELP = Pattern.compile(
            "^(hola|buenas?|buenos dias|buenas tardes|buenas noches|ayuda|ayudame|que puedes hacer|"
                    + "que sabes hacer|como funcionas|como te uso)\\b.*");
    private static final Pattern CREATE = Pattern.compile(
            "(?<![a-z0-9])(nuev[oa]s?|crea|crear|creame|registra|registrar|agrega|agregar|anade|anadir|"
                    + "haz|hacer|prepara|preparar|genera|generar)(?![a-z0-9])");
    private static final Pattern PLATE = Pattern.compile("(?<![A-Za-z0-9])([A-Za-z]{3})(-|\\s)?(\\d{2,3}[A-Za-z]?)(?![A-Za-z0-9])");
    private static final Pattern ORDER_NUMBER = Pattern.compile(
            "(?:orden(?:es)?|#)\\s*(?:(?:n|no|nro|numero)\\.?\\s*)?#?\\s*(\\d{1,6})(?![0-9])");
    private static final Pattern INVOICE_NUMBER = Pattern.compile("(?<![A-Za-z0-9])([A-Za-z]{1,6})-(\\d{4})-(\\d{1,6})(?![0-9])");
    private static final Pattern QUOTED = Pattern.compile("[\"«“']([^\"»”']{2,60})[\"»”']");
    private static final Pattern NAME = Pattern.compile(
            "(?:\\bde\\b|\\bdel cliente\\b|\\bcliente\\b|\\bpara\\b|\\ba nombre de\\b|\\bllamad[oa]\\b)\\s+"
                    + "((?:[A-ZÁÉÍÓÚÑ][a-záéíóúñü]+)(?:\\s+(?:de\\s+)?[A-ZÁÉÍÓÚÑ][a-záéíóúñü]+){0,3})");
    private static final Pattern MESSAGE = Pattern.compile(
            "(?:que diga|con el mensaje|mensaje|diciendo|diciéndole|diciendole)\\s*:?\\s*[\"«“']?(.{3,300}?)[\"»”']?\\s*$",
            Pattern.CASE_INSENSITIVE);

    /** Palabras de tres letras que no son el inicio de una placa («con 120 km»). */
    private static final Set<String> NOT_PLATES = Set.of("CON", "DEL", "LAS", "LOS", "POR", "UNA", "UNO", "QUE",
            "SIN", "MAS", "HAY", "VER", "MIS", "SUS", "LES", "DOS", "SEIS", "PAR", "DIA", "MES", "ANO", "FAC");

    /**
     * Interpreta un pedido.
     *
     * @param text    lo que escribió el usuario
     * @param catalog módulos que el rol puede abrir
     * @return la intención; {@code UNKNOWN} si no se entendió
     */
    public AssistantIntent interpret(String text, AssistantCatalog catalog) {
        String original = text == null ? "" : text.trim();
        String normalized = normalize(original);
        if (normalized.isEmpty() || HELP.matcher(normalized).matches()) {
            return AssistantIntent.help();
        }

        String plate = findPlate(original);
        String orderNumber = find(ORDER_NUMBER, normalized);
        String invoiceNumber = findInvoice(original);
        String quoted = find(QUOTED, original);
        String name = find(NAME, original);
        String message = find(MESSAGE, original);

        Destination createTarget = findCreateTarget(normalized, catalog);
        Destination destination = createTarget != null ? createTarget : findDestination(normalized, catalog);

        if (createTarget != null) {
            Map<String, String> fields = new LinkedHashMap<>(matchFilters(normalized, createTarget));
            putIfPresent(fields, FIELD_PLATE, plate);
            putIfPresent(fields, FIELD_ORDER, orderNumber);
            putIfPresent(fields, FIELD_INVOICE, invoiceNumber);
            putIfPresent(fields, FIELD_CUSTOMER, name != null ? name : quoted);
            putIfPresent(fields, FIELD_MESSAGE, message);
            return new AssistantIntent(Kind.CREATE, createTarget.code(), null, Map.of(), null, fields, null);
        }

        if (destination == null) {
            // Sin módulo: una referencia suelta dice a dónde ir.
            if (invoiceNumber != null && catalog.find(BILLING).isPresent()) {
                return record(BILLING, invoiceNumber);
            }
            if (orderNumber != null && catalog.find(ORDERS).isPresent()) {
                return record(ORDERS, orderNumber);
            }
            if (plate != null && catalog.find(VEHICLES).isPresent()) {
                return record(VEHICLES, plate);
            }
            return AssistantIntent.unknown(null);
        }

        Map<String, String> filters = matchFilters(normalized, destination);
        String code = destination.code();
        String reference = switch (code) {
            case VEHICLES -> plate;
            case ORDERS -> orderNumber;
            case BILLING -> invoiceNumber;
            case CUSTOMERS -> name != null ? name : quoted;
            default -> null;
        };
        if (reference != null && filters.isEmpty()) {
            return record(code, reference);
        }
        String search = quoted != null ? quoted : name != null ? name : plate;
        if (!filters.isEmpty() || search != null) {
            return new AssistantIntent(Kind.FILTER, code, search, filters, null, Map.of(), null);
        }
        return new AssistantIntent(Kind.OPEN, code, null, Map.of(), null, Map.of(), null);
    }

    // ---------- módulos ----------

    /** Módulo cuyo pedido de creación aparece en el texto (palabras propias o verbo + nombre del módulo). */
    private static Destination findCreateTarget(String text, AssistantCatalog catalog) {
        Destination best = null;
        int bestLength = 0;
        for (Destination destination : catalog.destinations()) {
            for (String words : destination.createWords()) {
                if (containsWord(text, words) && words.length() > bestLength) {
                    best = destination;
                    bestLength = words.length();
                }
            }
        }
        if (best != null) {
            return best;
        }
        if (!CREATE.matcher(text).find()) {
            return null;
        }
        Destination destination = findDestination(text, catalog);
        return destination != null && destination.canCreate() ? destination : null;
    }

    /** Módulo con la palabra más larga que aparece en el texto. */
    static Destination findDestination(String text, AssistantCatalog catalog) {
        Destination best = null;
        int bestLength = 0;
        for (Destination destination : catalog.destinations()) {
            for (String keyword : destination.keywords()) {
                if (keyword.length() > bestLength && containsWord(text, keyword)) {
                    best = destination;
                    bestLength = keyword.length();
                }
            }
        }
        return best;
    }

    // ---------- filtros ----------

    /** Opciones de filtro que aparecen en el texto (singular o plural), la más larga por filtro. */
    static Map<String, String> matchFilters(String text, Destination destination) {
        Map<String, String> found = new LinkedHashMap<>();
        for (Filter filter : destination.filters()) {
            String bestOption = null;
            int bestLength = 0;
            for (String option : filter.options()) {
                for (String form : forms(normalize(option))) {
                    if (form.length() > bestLength && containsWord(text, form)) {
                        bestOption = option;
                        bestLength = form.length();
                    }
                }
            }
            if (bestOption != null) {
                found.put(filter.label(), bestOption);
            }
        }
        return found;
    }

    /** Formas de una opción: tal cual y en plural («lista» → «listas», «pendiente» → «pendientes»). */
    static List<String> forms(String option) {
        List<String> forms = new ArrayList<>();
        if (option.isEmpty()) {
            return forms;
        }
        forms.add(option);
        String[] words = option.split(" ");
        String first = words[0];
        String plural = first.matches(".*[aeiou]$") ? first + "s" : first + "es";
        words[0] = plural;
        forms.add(String.join(" ", words));
        if (words.length > 1) {
            // «en reparacion», «stock bajo»: el plural va en la última palabra.
            String[] tail = option.split(" ");
            String last = tail[tail.length - 1];
            tail[tail.length - 1] = last.matches(".*[aeiou]$") ? last + "s" : last + "es";
            forms.add(String.join(" ", tail));
        }
        return forms;
    }

    // ---------- referencias ----------

    private static String findPlate(String text) {
        Matcher matcher = PLATE.matcher(text);
        while (matcher.find()) {
            String letters = matcher.group(1);
            String separator = matcher.group(2);
            if (NOT_PLATES.contains(letters.toUpperCase(Locale.ROOT))) {
                continue;
            }
            // Con espacio en medio solo cuenta si las letras van en mayúscula («ABC 123»).
            if (" ".equals(separator) && !letters.equals(letters.toUpperCase(Locale.ROOT))) {
                continue;
            }
            return (letters + matcher.group(3)).toUpperCase(Locale.ROOT);
        }
        return null;
    }

    private static String findInvoice(String text) {
        Matcher matcher = INVOICE_NUMBER.matcher(text);
        return matcher.find()
                ? (matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3)).toUpperCase(Locale.ROOT)
                : null;
    }

    private static String find(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private static AssistantIntent record(String code, String reference) {
        return new AssistantIntent(Kind.RECORD, code, null, Map.of(), reference, Map.of(), null);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value.trim());
        }
    }

    // ---------- texto ----------

    /** Minúsculas, sin tildes, sin signos (salvo # y -), espacios simples. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String noMarks = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noMarks.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9#\\-\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** La palabra o frase aparece completa (no como parte de otra). */
    static boolean containsWord(String text, String words) {
        if (words.isEmpty()) {
            return false;
        }
        int from = 0;
        while (true) {
            int index = text.indexOf(words, from);
            if (index < 0) {
                return false;
            }
            int end = index + words.length();
            boolean startOk = index == 0 || !Character.isLetterOrDigit(text.charAt(index - 1));
            boolean endOk = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (startOk && endOk) {
                return true;
            }
            from = index + 1;
        }
    }
}
