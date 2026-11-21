package com.autogest.service.assistant;

import com.autogest.service.assistant.AssistantCatalog.Destination;
import com.autogest.service.assistant.AssistantIntent.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link AssistantService}: motor con IA primero y el local como respaldo. */
class AssistantServiceTest {

    private static final AssistantCatalog CATALOG = new AssistantCatalog(List.of(
            new Destination("ORDERS", "Órdenes de servicio", List.of("orden", "ordenes"), List.of("nueva orden"),
                    List.of(), List.of()),
            new Destination("REPORTS", "Reportes", List.of("reportes"), List.of(), List.of(), List.of())));

    @Test
    void withoutAiTheLocalEngineAnswers() {
        AssistantService service = new AssistantService(null);

        AssistantService.Result result = service.interpret("reportes", CATALOG);

        assertFalse(service.aiEnabled());
        assertFalse(result.byAi());
        assertEquals("REPORTS", result.intent().destination());
        assertNull(result.note());
    }

    @Test
    void aiAnswerIsUsedWhenItIsValid() {
        AssistantService service = new AssistantService((text, catalog) ->
                new AssistantIntent(Kind.OPEN, "ORDERS", null, Map.of(), null, Map.of(), "Listo"));

        AssistantService.Result result = service.interpret("llévame a lo de los carros en el taller", CATALOG);

        assertTrue(result.byAi());
        assertEquals("ORDERS", result.intent().destination());
    }

    @Test
    void aiCannotProposeModulesOutsideTheCatalog() {
        AssistantService service = new AssistantService((text, catalog) ->
                new AssistantIntent(Kind.OPEN, "BILLING", null, Map.of(), null, Map.of(), null));

        AssistantService.Result result = service.interpret("reportes", CATALOG);

        assertFalse(result.byAi());
        assertEquals("REPORTS", result.intent().destination());
    }

    @Test
    void aiFailureFallsBackToTheLocalEngineWithANote() {
        AssistantService service = new AssistantService((text, catalog) -> {
            throw new IllegalStateException("sin conexión");
        });

        AssistantService.Result result = service.interpret("reportes", CATALOG);

        assertFalse(result.byAi());
        assertEquals("REPORTS", result.intent().destination());
        assertNotNull(result.note());
    }
}
