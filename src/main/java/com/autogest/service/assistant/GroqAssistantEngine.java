package com.autogest.service.assistant;

import com.autogest.service.assistant.AssistantPrompt.Reply;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Motor con IA del Asistente sobre la API de Groq (plan gratuito con límites
 * por minuto y por día). Le pide al modelo una intención con un esquema JSON
 * estricto y la revisa con {@link AssistantPrompt#toIntent}.
 *
 * <p>Solo envía lo que escribió el usuario y el catálogo del rol; nunca datos
 * de la base. Si algo falla (sin conexión, límite alcanzado, llave inválida o
 * una respuesta que no encaja) lanza una excepción y {@link AssistantService}
 * responde con las reglas locales.</p>
 *
 * <p>Principles: Adapter, Protected Variations.</p>
 */
public class GroqAssistantEngine implements AssistantService.AiEngine {

    /** Modelo por defecto: admite esquemas JSON estrictos. */
    public static final String DEFAULT_MODEL = "openai/gpt-oss-120b";

    static final URI ENDPOINT = URI.create("https://api.groq.com/openai/v1/chat/completions");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_COMPLETION_TOKENS = 2000;

    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Envía el cuerpo de la petición y devuelve la respuesta (se cambia en las pruebas). */
    @FunctionalInterface
    interface Transport {
        Response send(String body) throws IOException, InterruptedException;
    }

    /** Respuesta HTTP: código y cuerpo. */
    record Response(int status, String body) {
    }

    private final Transport transport;
    private final String model;

    /**
     * @param apiKey llave de la API de Groq
     * @param model  modelo, o {@code null} para {@link #DEFAULT_MODEL}
     */
    public GroqAssistantEngine(String apiKey, String model) {
        this(httpTransport(apiKey), model);
    }

    GroqAssistantEngine(Transport transport, String model) {
        this.transport = transport;
        this.model = model == null || model.isBlank() ? DEFAULT_MODEL : model.trim();
    }

    @Override
    public String name() {
        return "Groq";
    }

    @Override
    public AssistantIntent interpret(String text, AssistantCatalog catalog) {
        Response response;
        try {
            response = transport.send(requestBody(text, catalog));
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo conectar con Groq.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Se interrumpió la consulta a Groq.", e);
        }
        return toIntent(response, catalog);
    }

    /**
     * Cuerpo JSON de la petición: instrucciones, pedido y esquema estricto.
     *
     * @param text    lo que escribió el usuario
     * @param catalog módulos que el rol puede abrir
     * @return el JSON
     */
    String requestBody(String text, AssistantCatalog catalog) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(
                Map.of("role", "system", "content", AssistantPrompt.systemPrompt(catalog)),
                Map.of("role", "user", "content", text)));
        body.put("temperature", 0);
        body.put("max_completion_tokens", MAX_COMPLETION_TOKENS);
        body.put("response_format", Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "intencion_asistente",
                        "strict", true,
                        "schema", AssistantPrompt.replySchema())));
        try {
            return JSON.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo armar la consulta a Groq.", e);
        }
    }

    /**
     * Lee la respuesta de Groq y la convierte en intención.
     *
     * @param response respuesta HTTP
     * @param catalog  módulos que el rol puede abrir
     * @return la intención
     * @throws IllegalStateException si la respuesta es un error o no encaja
     */
    static AssistantIntent toIntent(Response response, AssistantCatalog catalog) {
        if (response.status() == 429) {
            throw new IllegalStateException("Se alcanzó el límite gratuito de Groq; intenta en un momento.");
        }
        if (response.status() < 200 || response.status() >= 300) {
            throw new IllegalStateException("Groq respondió con el código " + response.status() + ".");
        }
        try {
            JsonNode choice = JSON.readTree(response.body()).path("choices").path(0);
            String finish = choice.path("finish_reason").asText("");
            String content = choice.path("message").path("content").asText("");
            if (!"stop".equals(finish) || content.isBlank()) {
                throw new IllegalStateException("Groq no devolvió una intención completa (" + finish + ").");
            }
            return AssistantPrompt.toIntent(JSON.readValue(content, Reply.class), catalog);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("La respuesta de Groq no es el JSON esperado.", e);
        }
    }

    private static Transport httpTransport(String apiKey) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
        return body -> {
            HttpRequest request = HttpRequest.newBuilder(ENDPOINT)
                    .timeout(REQUEST_TIMEOUT)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Response(response.statusCode(), response.body());
        };
    }
}
