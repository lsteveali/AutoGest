package com.autogest.service.assistant;

import com.autogest.service.assistant.AssistantCatalog.Destination;
import com.autogest.service.assistant.AssistantCatalog.Filter;
import com.autogest.service.assistant.AssistantIntent.Kind;
import com.autogest.service.assistant.ClaudeAssistantEngine.Pair;
import com.autogest.service.assistant.ClaudeAssistantEngine.Reply;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del motor con IA sin llamar a la API: solo el texto que se envía y
 * cómo se revisa la respuesta contra el catálogo.
 */
class ClaudeAssistantEngineTest {

    private static final AssistantCatalog CATALOG = new AssistantCatalog(List.of(
            new Destination("ORDERS", "Órdenes de servicio", List.of("orden"), List.of("nueva orden"),
                    List.of(new Filter("Estado", List.of("Recibida", "En reparación", "Lista"))), List.of()),
            new Destination("NOTIFICATIONS", "Notificaciones", List.of("aviso"), List.of("nuevo aviso"),
                    List.of(new Filter("Tipo", List.of("General", "Promoción"))), List.of())));

    private static Reply reply(String kind, String destination, List<Pair> filters, List<Pair> fields) {
        return new Reply(kind, destination, "", filters, "", fields, "");
    }

    @Test
    void promptListsTheRoleCatalogAndNoData() {
        String prompt = ClaudeAssistantEngine.systemPrompt(CATALOG);

        assertTrue(prompt.contains("- ORDERS: Órdenes de servicio (tiene formulario de creación)"), prompt);
        assertTrue(prompt.contains("filtro \"Estado\": Recibida | En reparación | Lista"), prompt);
        assertFalse(prompt.contains("- BILLING:"), "Un módulo que el rol no ve no aparece");
    }

    @Test
    void filterOptionIsCopiedAsShownOnScreen() {
        AssistantIntent intent = ClaudeAssistantEngine.toIntent(
                reply("filter", "orders", List.of(new Pair("Estado", "en reparacion")), List.of()), CATALOG);

        assertEquals(Kind.FILTER, intent.kind());
        assertEquals("ORDERS", intent.destination());
        assertEquals(Map.of("Estado", "En reparación"), intent.filters());
        assertNull(intent.search(), "Un texto vacío es «sin dato»");
    }

    @Test
    void inventedFiltersAndOptionsAreDropped() {
        AssistantIntent intent = ClaudeAssistantEngine.toIntent(reply("FILTER", "ORDERS",
                List.of(new Pair("Estado", "Perdida"), new Pair("Color", "Rojo")), List.of()), CATALOG);

        assertTrue(intent.filters().isEmpty(), intent.filters().toString());
    }

    @Test
    void createKeepsDataFieldsAndValidTypes() {
        AssistantIntent intent = ClaudeAssistantEngine.toIntent(reply("CREATE", "NOTIFICATIONS", List.of(),
                List.of(new Pair("Tipo", "promocion"), new Pair("Cliente", "Ana Gómez"),
                        new Pair("mensaje", "20 % en aceite"), new Pair("placa", ""))), CATALOG);

        assertEquals(Map.of("Tipo", "Promoción", "cliente", "Ana Gómez", "mensaje", "20 % en aceite"),
                intent.fields());
    }

    @Test
    void unknownKindFailsSoTheLocalRulesAnswer() {
        assertThrows(IllegalStateException.class,
                () -> ClaudeAssistantEngine.toIntent(reply("DELETE", "ORDERS", List.of(), List.of()), CATALOG));
    }

    @Test
    void unknownCarriesTheExplanation() {
        AssistantIntent intent = ClaudeAssistantEngine.toIntent(
                new Reply("UNKNOWN", "", "", List.of(), "", List.of(), "Eso no está en AutoGest."), CATALOG);

        assertEquals(Kind.UNKNOWN, intent.kind());
        assertEquals("Eso no está en AutoGest.", intent.message());
    }

    @Test
    void destinationOutsideTheCatalogIsRejectedByTheService() {
        AssistantService service = new AssistantService((text, catalog) ->
                ClaudeAssistantEngine.toIntent(reply("OPEN", "BILLING", List.of(), List.of()), catalog));

        AssistantService.Result result = service.interpret("abre facturación", CATALOG);

        assertFalse(result.byAi(), "La IA no puede abrir un módulo que el rol no ve");
    }
}
