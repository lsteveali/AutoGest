package com.autogest.service.assistant;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.autogest.service.assistant.AssistantCatalog.Destination;
import com.autogest.service.assistant.AssistantCatalog.Filter;
import com.autogest.service.assistant.AssistantIntent.Kind;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Motor con IA del Asistente: le pide a Claude que convierta un pedido libre
 * en una intención con la misma forma que la del motor local. Claude solo
 * recibe lo que escribió el usuario y el catálogo del rol (módulos, filtros y
 * campos); nunca datos de la base. La respuesta llega como JSON con esquema
 * fijo y se revisa contra el catálogo antes de usarla.
 *
 * <p>Si algo falla (sin conexión, sin crédito, una negativa o una respuesta
 * que no encaja) lanza una excepción y {@link AssistantService} responde con
 * las reglas locales.</p>
 *
 * <p>Principles: Adapter, Protected Variations.</p>
 */
public class ClaudeAssistantEngine implements AssistantService.AiEngine {

    /** Modelo por defecto. */
    public static final String DEFAULT_MODEL = "claude-opus-5-5";

    private static final long MAX_TOKENS = 4000L;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /** Par nombre-valor (los mapas no se piden en el esquema). */
    record Pair(String name, String value) {
    }

    /** Respuesta de Claude; los textos vacíos significan «sin dato». */
    record Reply(String kind, String destination, String search, List<Pair> filters, String reference,
                 List<Pair> fields, String message) {
    }

    private final AnthropicClient client;
    private final String model;

    /**
     * @param apiKey llave de la API de Anthropic
     * @param model  modelo, o {@code null} para {@link #DEFAULT_MODEL}
     */
    public ClaudeAssistantEngine(String apiKey, String model) {
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(TIMEOUT)
                .maxRetries(1)
                .build();
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    @Override
    public AssistantIntent interpret(String text, AssistantCatalog catalog) {
        StructuredMessageCreateParams<Reply> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .system(systemPrompt(catalog))
                .outputConfig(StructuredOutputConfig.<Reply>builder()
                        .format(Reply.class)
                        .effort(OutputConfig.Effort.LOW)
                        .build())
                .addUserMessage(text)
                .build();
        StructuredMessage<Reply> response = client.messages().create(params);
        if (response.stopReason().map(StopReason.REFUSAL::equals).orElse(false)) {
            throw new IllegalStateException("La IA no quiso responder el pedido.");
        }
        Reply reply = response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(block -> block.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("La IA no devolvió una intención."));
        return toIntent(reply, catalog);
    }

    /**
     * Instrucciones con el catálogo del rol. No llevan datos de clientes.
     *
     * @param catalog módulos que el rol puede abrir
     * @return el texto del sistema
     */
    static String systemPrompt(AssistantCatalog catalog) {
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
     * Convierte la respuesta en una intención y descarta lo que no está en el
     * catálogo (filtros u opciones inventadas).
     *
     * @param reply   respuesta de Claude
     * @param catalog módulos que el rol puede abrir
     * @return la intención
     * @throws IllegalStateException si el tipo de intención no existe
     */
    static AssistantIntent toIntent(Reply reply, AssistantCatalog catalog) {
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
