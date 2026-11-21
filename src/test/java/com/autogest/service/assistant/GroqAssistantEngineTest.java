package com.autogest.service.assistant;

import com.autogest.service.assistant.AssistantIntent.Kind;
import com.autogest.service.assistant.GroqAssistantEngine.Response;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del motor de Groq con un transporte falso: nunca sale a internet. */
class GroqAssistantEngineTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    /** Respuesta de chat de Groq con el contenido dado. */
    private static Response ok(String content, String finish) throws IOException {
        String body = JSON.writeValueAsString(Map.of("choices", java.util.List.of(Map.of(
                "index", 0,
                "message", Map.of("role", "assistant", "content", content),
                "finish_reason", finish))));
        return new Response(200, body);
    }

    private static final String FILTER_REPLY = """
            {"kind":"FILTER","destination":"ORDERS","search":"","filters":[{"name":"Estado","value":"En reparación"}],
             "reference":"","fields":[],"message":""}""";

    @Test
    void requestCarriesTheModelTheCatalogAndAStrictSchema() throws IOException {
        AtomicReference<String> sent = new AtomicReference<>();
        GroqAssistantEngine engine = new GroqAssistantEngine(body -> {
            sent.set(body);
            return ok(FILTER_REPLY, "stop");
        }, null);

        engine.interpret("órdenes en reparación", AssistantPromptTest.CATALOG);

        JsonNode request = JSON.readTree(sent.get());
        assertEquals(GroqAssistantEngine.DEFAULT_MODEL, request.path("model").asText());
        assertEquals("system", request.path("messages").path(0).path("role").asText());
        assertTrue(request.path("messages").path(0).path("content").asText().contains("- ORDERS:"));
        assertEquals("órdenes en reparación", request.path("messages").path(1).path("content").asText());
        JsonNode format = request.path("response_format");
        assertEquals("json_schema", format.path("type").asText());
        assertTrue(format.path("json_schema").path("strict").asBoolean());
    }

    @Test
    void replyBecomesAnIntent() {
        GroqAssistantEngine engine = new GroqAssistantEngine(body -> ok(FILTER_REPLY, "stop"), "otro/modelo");

        AssistantIntent intent = engine.interpret("órdenes en reparación", AssistantPromptTest.CATALOG);

        assertEquals(Kind.FILTER, intent.kind());
        assertEquals(Map.of("Estado", "En reparación"), intent.filters());
    }

    @Test
    void rateLimitFallsBackToTheLocalRules() {
        GroqAssistantEngine engine = new GroqAssistantEngine(body -> new Response(429, "{}"), null);
        AssistantService service = new AssistantService(engine);

        AssistantService.Result result = service.interpret("abre orden", AssistantPromptTest.CATALOG);

        assertFalse(result.byAi());
        assertTrue(result.note() != null && result.note().contains("reglas locales"), String.valueOf(result.note()));
        assertEquals("ORDERS", result.intent().destination(), "Las reglas locales igual entienden el pedido");
    }

    @Test
    void connectionErrorAndCutAnswersAreErrors() {
        GroqAssistantEngine offline = new GroqAssistantEngine(body -> {
            throw new IOException("sin red");
        }, null);
        GroqAssistantEngine cut = new GroqAssistantEngine(body -> ok("{\"kind\":", "length"), null);
        GroqAssistantEngine notJson = new GroqAssistantEngine(body -> ok("hola", "stop"), null);

        assertThrows(IllegalStateException.class, () -> offline.interpret("x", AssistantPromptTest.CATALOG));
        assertThrows(IllegalStateException.class, () -> cut.interpret("x", AssistantPromptTest.CATALOG));
        assertThrows(IllegalStateException.class, () -> notJson.interpret("x", AssistantPromptTest.CATALOG));
    }

    @Test
    void panelShowsTheEngineName() {
        AssistantService service = new AssistantService(new GroqAssistantEngine(body -> ok(FILTER_REPLY, "stop"),
                null));

        assertEquals("Groq", service.aiName());
    }
}
