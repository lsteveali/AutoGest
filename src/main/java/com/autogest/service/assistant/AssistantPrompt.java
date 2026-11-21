package com.autogest.service.assistant;

import com.autogest.service.assistant.AssistantCatalog.Destination;
import com.autogest.service.assistant.AssistantCatalog.Filter;
import com.autogest.service.assistant.AssistantIntent.Kind;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lo común a cualquier motor con IA del Asistente: las instrucciones con el
 * catálogo del rol, la forma de la respuesta y su revisión contra el catálogo.
 * No depende de ningún proveedor; cada motor solo hace la llamada.
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public final class AssistantPrompt {

    private AssistantPrompt() {
        // Clase de utilidad: no se instancia.
    }

    /** Par nombre-valor (los mapas no se piden en el esquema). */
    public record Pair(String name, String value) {
    }

    /** Respuesta del modelo; los textos vacíos significan «sin dato». */
    public record Reply(String kind, String destination, String search, List<Pair> filters, String reference,
                        List<Pair> fields, String message) {
    }

    /**
     * Instrucciones con el catálogo del rol. No llevan datos de clientes.
     *
     * @param catalog módulos que el rol puede abrir
     * @return el texto del sistema
     */
    public static String systemPrompt(AssistantCatalog catalog) {
        StringBuilder prompt = new StringBuilder("""
                Eres el Asistente de AutoGest, una aplicación de escritorio para talleres mecánicos.
                Tu único trabajo es convertir el pedido del usuario en una intención para navegar por la
                aplicación. No respondes preguntas generales ni inventas datos: la aplicación abre la
                pantalla, aplica filtros o precarga un formulario, y el usuario revisa y guarda.

                Tipos de intención (campo "kind"):
                - OPEN: abrir un módulo sin filtros.
                - FILTER: abrir la lista de un módulo con búsqueda ("search") o filtros ("filters").
                - RECORD: abrir un registro concreto. "reference" es la placa del vehículo (VEHICLES), el
                  número de la orden (ORDERS), el número de la factura (BILLING) o el nombre del cliente
                  (CUSTOMERS).
                - CREATE: abrir el formulario de creación de un módulo con datos en "fields".
                - HELP: el usuario saluda o pregunta qué puedes hacer.
                - UNKNOWN: no se puede hacer con estos módulos; explica por qué en "message", en español,
                  en una frase.

                Reglas:
                - "destination" es siempre uno de los códigos de la lista de abajo; si el módulo no está,
                  usa UNKNOWN (el rol del usuario no lo puede abrir).
                - En "filters", "name" es la etiqueta del filtro y "value" una de sus opciones, copiada
                  tal cual. No inventes filtros ni opciones.
                - En "fields" (solo para CREATE) usa estos nombres: "placa", "orden", "factura",
                  "cliente" (nombre del cliente), "mensaje" (texto del aviso), y también las etiquetas de
                  los filtros del módulo (por ejemplo "Tipo" o "Canal") con una de sus opciones.
                - Escribe las placas y números tal como los dio el usuario.
                - Deja en "" los textos sin dato y en [] las listas vacías.
                - Responde solo con el JSON pedido.

                Módulos disponibles para este usuario:
                """);
        for (Destination destination : catalog.destinations()) {
            prompt.append("- ").append(destination.code()).append(": ").append(destination.title());
            if (destination.canCreate()) {
                prompt.append(" (tiene formulario de creación)");
            }
            prompt.append('\n');
            for (Filter filter : destination.filters()) {
                prompt.append("    filtro \"").append(filter.label()).append("\": ")
                        .append(String.join(" | ", filter.options())).append('\n');
            }
        }
        return prompt.toString();
    }

    /**
     * Esquema JSON estricto de {@link Reply}: todos los campos obligatorios y
     * sin propiedades extra.
     *
     * @return el esquema como mapa (listo para convertir a JSON)
     */
    public static Map<String, Object> replySchema() {
        Map<String, Object> text = Map.of("type", "string");
        Map<String, Object> pairs = Map.of("type", "array", "items", Map.of(
                "type", "object",
                "properties", Map.of("name", text, "value", text),
                "required", List.of("name", "value"),
                "additionalProperties", false));
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("kind", Map.of("type", "string",
                "enum", List.of("OPEN", "FILTER", "RECORD", "CREATE", "HELP", "UNKNOWN")));
        properties.put("destination", text);
        properties.put("search", text);
        properties.put("filters", pairs);
        properties.put("reference", text);
        properties.put("fields", pairs);
        properties.put("message", text);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.copyOf(properties.keySet()));
        schema.put("additionalProperties", false);
        return schema;
    }

    /**
     * Convierte la respuesta en una intención y descarta lo que no está en el
     * catálogo (filtros u opciones inventadas).
     *
     * @param reply   respuesta del modelo
     * @param catalog módulos que el rol puede abrir
     * @return la intención
     * @throws IllegalStateException si el tipo de intención no existe
     */
    public static AssistantIntent toIntent(Reply reply, AssistantCatalog catalog) {
        Kind kind;
        try {
            kind = Kind.valueOf(text(reply.kind()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("La IA devolvió un tipo de intención desconocido: " + reply.kind(), e);
        }
        if (kind == Kind.HELP) {
            return AssistantIntent.help();
        }
        if (kind == Kind.UNKNOWN) {
            String message = text(reply.message());
            return AssistantIntent.unknown(message.isEmpty() ? null : message);
        }
        String code = text(reply.destination()).toUpperCase(Locale.ROOT);
        Destination destination = catalog.find(code).orElse(null);
        List<Filter> filters = destination == null ? List.of() : destination.filters();
        return new AssistantIntent(kind, code, blankToNull(reply.search()), validFilters(reply.filters(), filters),
                blankToNull(reply.reference()), fields(reply.fields(), filters), null);
    }

    private static Map<String, String> validFilters(List<Pair> pairs, List<Filter> filters) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Pair pair : pairs == null ? List.<Pair>of() : pairs) {
            String option = option(pair, filters);
            if (option != null) {
                result.put(text(pair.name()), option);
            }
        }
        return result;
    }

    private static Map<String, String> fields(List<Pair> pairs, List<Filter> filters) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Pair pair : pairs == null ? List.<Pair>of() : pairs) {
            String name = text(pair.name());
            String value = text(pair.value());
            if (name.isEmpty() || value.isEmpty()) {
                continue;
            }
            boolean isFilter = filters.stream().anyMatch(filter -> filter.label().equals(name));
            if (isFilter) {
                String option = option(pair, filters);
                if (option != null) {
                    result.put(name, option);
                }
            } else {
                result.put(name.toLowerCase(Locale.ROOT), value);
            }
        }
        return result;
    }

    /** La opción del filtro tal como se ve en pantalla, o {@code null} si no existe. */
    private static String option(Pair pair, List<Filter> filters) {
        String name = text(pair.name());
        String value = LocalAssistantInterpreter.normalize(text(pair.value()));
        return filters.stream()
                .filter(filter -> filter.label().equals(name))
                .flatMap(filter -> filter.options().stream())
                .filter(option -> LocalAssistantInterpreter.normalize(option).equals(value))
                .findFirst()
                .orElse(null);
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String blankToNull(String value) {
        String trimmed = text(value);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
