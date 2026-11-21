package com.autogest.config;

import com.autogest.service.assistant.AssistantService;
import com.autogest.service.assistant.ClaudeAssistantEngine;

import java.util.Properties;

/**
 * Arma el motor con IA del Asistente a partir de
 * {@code application.local.properties}. Sin llave devuelve {@code null} y el
 * Asistente usa solo las reglas locales: así funciona igual en un equipo sin
 * cuenta de Anthropic ni internet.
 *
 * <p>Claves:</p>
 * <ul>
 *   <li>{@code assistant.anthropic.api-key}: llave de la API de Anthropic (opcional).</li>
 *   <li>{@code assistant.anthropic.model}: modelo ({@code claude-opus-5-5}).</li>
 * </ul>
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public final class AssistantConfig {

    static final String API_KEY = "assistant.anthropic.api-key";
    static final String MODEL = "assistant.anthropic.model";

    private AssistantConfig() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Motor con IA según el archivo local.
     *
     * @return el motor, o {@code null} si no hay llave configurada
     */
    static AssistantService.AiEngine fromLocalFile() {
        return fromProperties(DatabaseConnection.loadProperties());
    }

    /**
     * Motor con IA según las propiedades dadas.
     *
     * @param properties propiedades con las claves {@code assistant.*}
     * @return el motor, o {@code null} si falta la llave
     */
    public static AssistantService.AiEngine fromProperties(Properties properties) {
        String key = value(properties, API_KEY);
        if (key == null) {
            return null;
        }
        return new ClaudeAssistantEngine(key, value(properties, MODEL));
    }

    private static String value(Properties properties, String key) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
