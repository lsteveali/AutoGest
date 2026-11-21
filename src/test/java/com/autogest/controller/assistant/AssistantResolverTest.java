package com.autogest.controller.assistant;

import com.autogest.controller.View;
import com.autogest.controller.assistant.AssistantAction.Plan;
import com.autogest.model.Catalogs.Role;
import com.autogest.service.assistant.AssistantCatalog;
import com.autogest.service.assistant.AssistantIntent;
import com.autogest.service.assistant.LocalAssistantInterpreter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del camino completo sin interfaz ni base de datos: el motor local
 * interpreta con el catálogo real de cada rol y el resolutor arma las acciones
 * con búsquedas en memoria.
 */
class AssistantResolverTest {

    /** Búsquedas en memoria: un vehículo ABC123 (id 7, dueña Ana id 3), la orden 0045 (id 12), FAC-2026-001 (id 5). */
    private static final AssistantLookup LOOKUP = new AssistantLookup() {
        @Override
        public Optional<Integer> vehicleByPlate(String plate) {
            return "ABC123".equals(ServiceAssistantLookup.plateKey(plate)) ? Optional.of(7) : Optional.empty();
        }

        @Override
        public Optional<Integer> ownerOfPlate(String plate) {
            return vehicleByPlate(plate).map(id -> 3);
        }

        @Override
        public Optional<Integer> orderByNumber(String number) {
            return "45".equals(ServiceAssistantLookup.withoutLeadingZeros(number)) ? Optional.of(12) : Optional.empty();
        }

        @Override
        public Optional<Integer> invoiceByNumber(String number) {
            return "FAC-2026-001".equalsIgnoreCase(number) ? Optional.of(5) : Optional.empty();
        }

        @Override
        public List<Match> customersByName(String name) {
            if (name.toLowerCase().contains("ana")) {
                return List.of(new Match(3, "Ana Gómez"));
            }
            if (name.toLowerCase().contains("juan")) {
                return List.of(new Match(1, "Juan Pérez"), new Match(2, "Juan Ruiz"));
            }
            return List.of();
        }
    };

    private final LocalAssistantInterpreter interpreter = new LocalAssistantInterpreter();
    private final AssistantResolver resolver = new AssistantResolver(LOOKUP);

    private Plan ask(String text, Role role) {
        AssistantCatalog catalog = AssistantDestinations.catalogFor(role);
        AssistantIntent intent = interpreter.interpret(text, catalog);
        return resolver.resolve(intent, role);
    }

    @Test
    void filteredListUsesTheRealOptions() {
        Plan plan = ask("órdenes en reparación", Role.ADMINISTRATOR);

        AssistantAction action = plan.actions().get(0);
        assertEquals(View.ORDERS, action.view());
        assertEquals(1, action.filters().size());
        assertTrue(plan.message().startsWith("Abriendo Órdenes de servicio con Estado: "), plan.message());
    }

    @Test
    void plateOpensTheVehicleDetail() {
        AssistantAction action = ask("abre el ABC123", Role.ADMINISTRATOR).actions().get(0);

        assertEquals(View.VEHICLE_DETAIL, action.view());
        assertEquals(7, action.parameter());
    }

    @Test
    void unknownReferenceFallsBackToASearch() {
        Plan plan = ask("abre la orden 999", Role.ADMINISTRATOR);

        AssistantAction action = plan.actions().get(0);
        assertEquals(View.ORDERS, action.view());
        assertEquals("999", action.search());
        assertTrue(plan.message().startsWith("No encontré la orden #999"), plan.message());
    }

    @Test
    void newOrderForAPlateIsPrefilledButNotSaved() {
        Plan plan = ask("nueva orden para el ABC123", Role.RECEPTIONIST);

        AssistantAction action = plan.actions().get(0);
        assertEquals(View.ORDER_NEW, action.view());
        assertEquals(7, action.parameter());
        assertTrue(plan.message().contains("no se guarda nada hasta que lo confirmes"), plan.message());
    }

    @Test
    void newOrderForAnUnknownPlateOffersToRegisterTheVehicle() {
        Plan plan = ask("nueva orden para XYZ987", Role.ADMINISTRATOR);

        assertNull(plan.actions().get(0).parameter());
        AssistantAction register = plan.actions().get(1);
        assertEquals(View.VEHICLE_NEW, register.view());
        assertEquals(Map.of("Placa", "XYZ987"), register.fields());
    }

    @Test
    void invoiceForAnOrderNumberOpensTheNewInvoice() {
        AssistantAction action = ask("facturar la orden 0045", Role.ADMINISTRATOR).actions().get(0);

        assertEquals(View.INVOICE_NEW, action.view());
        assertEquals(12, action.parameter());
    }

    @Test
    void noticeForACustomerCarriesTypeAndMessage() {
        AssistantAction action = ask("enviar aviso de promoción para Ana Gómez que diga \"20 % en aceite\"",
                Role.ADMINISTRATOR).actions().get(0);

        assertEquals(View.NOTIFICATION_NEW, action.view());
        assertEquals(3, action.parameter());
        assertEquals("20 % en aceite", action.fields().get("Mensaje"));
        assertTrue(action.fields().containsKey("Tipo"));
    }

    @Test
    void severalCustomersShowTheList() {
        Plan plan = ask("busca el cliente Juan", Role.ADMINISTRATOR);

        AssistantAction action = plan.actions().get(0);
        assertEquals(View.CUSTOMERS, action.view());
        assertEquals("Juan", action.search());
    }

    @Test
    void mechanicNeverGetsBillingOrCustomers() {
        assertFalse(AssistantDestinations.catalogFor(Role.MECHANIC).find("BILLING").isPresent());
        assertFalse(AssistantDestinations.catalogFor(Role.MECHANIC).find("CUSTOMERS").isPresent());

        Plan plan = ask("facturas vencidas", Role.MECHANIC);

        assertTrue(plan.actions().isEmpty());
    }

    @Test
    void forbiddenActionsAreRemovedEvenIfAnEngineProposesThem() {
        AssistantIntent intent = new AssistantIntent(AssistantIntent.Kind.OPEN, "BILLING", null, Map.of(), null,
                Map.of(), null);

        Plan plan = resolver.resolve(intent, Role.MECHANIC);

        assertTrue(plan.actions().isEmpty());
        assertTrue(plan.message().contains("no tiene acceso"), plan.message());
    }

    @Test
    void helpListsExamples() {
        Plan plan = ask("hola", Role.SUPERVISOR);

        assertTrue(plan.actions().isEmpty());
        assertTrue(plan.message().contains("órdenes en reparación"), plan.message());
    }
}
