package com.autogest.config;

import com.autogest.service.assistant.ClaudeAssistantEngine;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Pruebas de la configuración del motor con IA (no llama a la API). */
class AssistantConfigTest {

    @Test
    void withoutKeyThereIsNoAiEngine() {
        Properties properties = new Properties();
        properties.setProperty(AssistantConfig.API_KEY, "   ");

        assertNull(AssistantConfig.fromProperties(properties));
        assertNull(AssistantConfig.fromProperties(new Properties()));
    }

    @Test
    void withKeyTheClaudeEngineIsUsed() {
        Properties properties = new Properties();
        properties.setProperty(AssistantConfig.API_KEY, "sk-ant-prueba");

        assertInstanceOf(ClaudeAssistantEngine.class, AssistantConfig.fromProperties(properties));
    }
}
