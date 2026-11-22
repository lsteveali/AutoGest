package com.autogest.service.assistant;

import com.autogest.service.assistant.AssistantCatalog.Destination;
import com.autogest.service.assistant.AssistantCatalog.Filter;
import com.autogest.service.assistant.AssistantIntent.Kind;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del motor local del Asistente con un catálogo como el de la app. */
class LocalAssistantInterpreterTest {

    private final LocalAssistantInterpreter interpreter = new LocalAssistantInterpreter();

    private static final AssistantCatalog ADMIN = new AssistantCatalog(List.of(
            new Destination("HOME", "Inicio", List.of("inicio", "tablero", "dashboard"), List.of(), List.of(), List.of()),
            new Destination("CUSTOMERS", "Clientes", List.of("cliente", "clientes"), List.of("nuevo cliente"),
                    List.of(new Filter("Estado", List.of("Activo", "Inactivo"))), List.of()),
            new Destination("VEHICLES", "Vehículos", List.of("vehiculo", "vehiculos", "carro", "carros"),
                    List.of("nuevo vehiculo", "registrar vehiculo"),
                    List.of(new Filter("Estado", List.of("Activo", "En servicio", "Inactivo")),
                            new Filter("Clase", List.of("Automóvil", "Camioneta", "Motocicleta"))), List.of("Placa")),
            new Destination("ORDERS", "Órdenes de servicio", List.of("orden", "ordenes", "ordenes de servicio"),
                    List.of("nueva orden", "abrir una orden"),
                    List.of(new Filter("Estado", List.of("Recibida", "Diagnóstico", "En reparación", "Lista",
                            "Entregada", "Cancelada"))), List.of()),
            new Destination("BILLING", "Facturación", List.of("factura", "facturas", "facturacion"),
                    List.of("nueva factura", "facturar", "generar factura"),
                    List.of(new Filter("Estado", List.of("Pendiente", "Pagada", "Vencida", "Anulada")),
                            new Filter("Fecha", List.of("Este mes", "Últimos 30 días"))), List.of()),
            new Destination("INVENTORY", "Inventario", List.of("inventario", "repuesto", "repuestos", "stock"),
                    List.of("nuevo repuesto"),
                    List.of(new Filter("Stock", List.of("Disponible", "Stock bajo", "Agotado"))), List.of()),
            new Destination("NOTIFICATIONS", "Notificaciones", List.of("notificacion", "notificaciones", "avisos"),
                    List.of("nueva notificacion", "enviar aviso", "enviar notificacion", "enviar correo"),
                    List.of(new Filter("Tipo", List.of("Actualización de servicio", "Facturación", "Promoción",
                            "General")),
                            new Filter("Estado", List.of("Enviada", "Pendiente", "Fallida"))), List.of()),
            new Destination("REPORTS", "Reportes", List.of("reporte", "reportes", "estadisticas"), List.of(),
                    List.of(), List.of())));

    @Test
    void opensAModuleByItsName() {
        AssistantIntent intent = interpreter.interpret("Llévame a los reportes", ADMIN);

        assertEquals(Kind.OPEN, intent.kind());
        assertEquals("REPORTS", intent.destination());
    }

    @Test
    void filtersByTheOptionsSeenOnScreen() {
        AssistantIntent orders = interpreter.interpret("¿Qué órdenes están en reparación?", ADMIN);
        assertEquals(Kind.FILTER, orders.kind());
        assertEquals("ORDERS", orders.destination());
        assertEquals(Map.of("Estado", "En reparación"), orders.filters());

        assertEquals(Map.of("Estado", "Vencida"), interpreter.interpret("facturas vencidas", ADMIN).filters());
        assertEquals(Map.of("Estado", "Lista"), interpreter.interpret("muéstrame las órdenes listas", ADMIN).filters());
        assertEquals(Map.of("Stock", "Stock bajo"), interpreter.interpret("repuestos con stock bajo", ADMIN).filters());
        assertEquals(Map.of("Estado", "Inactivo"), interpreter.interpret("clientes inactivos", ADMIN).filters());
    }

    @Test
    void severalFiltersAtOnce() {
        AssistantIntent intent = interpreter.interpret("facturas pagadas de este mes", ADMIN);

        assertEquals(Map.of("Estado", "Pagada", "Fecha", "Este mes"), intent.filters());
    }

    @Test
    void plateOpensTheVehicle() {
        AssistantIntent intent = interpreter.interpret("abre el vehículo abc123", ADMIN);
        assertEquals(Kind.RECORD, intent.kind());
        assertEquals("VEHICLES", intent.destination());
        assertEquals("ABC123", intent.reference());

        AssistantIntent loose = interpreter.interpret("ABC-123", ADMIN);
        assertEquals(Kind.RECORD, loose.kind());
        assertEquals("ABC123", loose.reference());
    }

    @Test
    void orderAndInvoiceNumbersOpenTheRecord() {
        AssistantIntent order = interpreter.interpret("muéstrame la orden #0045", ADMIN);
        assertEquals(Kind.RECORD, order.kind());
        assertEquals("ORDERS", order.destination());
        assertEquals("0045", order.reference());

        AssistantIntent invoice = interpreter.interpret("abre la FAC-2026-001", ADMIN);
        assertEquals(Kind.RECORD, invoice.kind());
        assertEquals("BILLING", invoice.destination());
        assertEquals("FAC-2026-001", invoice.reference());
    }

    @Test
    void ordersOfAPlateAreAFilteredList() {
        AssistantIntent intent = interpreter.interpret("órdenes en reparación del ABC123", ADMIN);

        assertEquals(Kind.FILTER, intent.kind());
        assertEquals("ABC123", intent.search());
        assertEquals(Map.of("Estado", "En reparación"), intent.filters());
    }

    @Test
    void createRequestsCarryTheirData() {
        AssistantIntent order = interpreter.interpret("Nueva orden para el ABC123", ADMIN);
        assertEquals(Kind.CREATE, order.kind());
        assertEquals("ORDERS", order.destination());
        assertEquals("ABC123", order.fields().get(LocalAssistantInterpreter.FIELD_PLATE));

        AssistantIntent invoice = interpreter.interpret("facturar la orden 45", ADMIN);
        assertEquals(Kind.CREATE, invoice.kind());
        assertEquals("BILLING", invoice.destination());
        assertEquals("45", invoice.fields().get(LocalAssistantInterpreter.FIELD_ORDER));

        AssistantIntent notice = interpreter.interpret(
                "Enviar aviso de promoción para Ana Gómez con el mensaje \"20 % en cambio de aceite\"", ADMIN);
        assertEquals("NOTIFICATIONS", notice.destination());
        assertEquals("Promoción", notice.fields().get("Tipo"));
        assertEquals("Ana Gómez", notice.fields().get(LocalAssistantInterpreter.FIELD_CUSTOMER));
        assertEquals("20 % en cambio de aceite", notice.fields().get(LocalAssistantInterpreter.FIELD_MESSAGE));
    }

    @Test
    void verbPlusModuleAlsoCreates() {
        AssistantIntent intent = interpreter.interpret("quiero registrar un cliente", ADMIN);

        assertEquals(Kind.CREATE, intent.kind());
        assertEquals("CUSTOMERS", intent.destination());
    }

    @Test
    void customerNameOpensTheCustomer() {
        AssistantIntent intent = interpreter.interpret("busca el cliente Juan Pérez", ADMIN);

        assertEquals(Kind.RECORD, intent.kind());
        assertEquals("CUSTOMERS", intent.destination());
        assertEquals("Juan Pérez", intent.reference());
    }

    @Test
    void helpAndUnknown() {
        assertEquals(Kind.HELP, interpreter.interpret("hola", ADMIN).kind());
        assertEquals(Kind.HELP, interpreter.interpret("  ", ADMIN).kind());
        assertEquals(Kind.UNKNOWN, interpreter.interpret("¿va a llover mañana?", ADMIN).kind());
    }

    @Test
    void modulesOutsideTheCatalogAreNeverProposed() {
        AssistantCatalog mechanic = new AssistantCatalog(List.of(ADMIN.find("ORDERS").orElseThrow()));

        assertEquals(Kind.UNKNOWN, mechanic.find("BILLING").isPresent() ? null
                : interpreter.interpret("facturas vencidas", mechanic).kind());
        assertEquals(Kind.UNKNOWN, interpreter.interpret("abre el ABC123", mechanic).kind());
    }

    @Test
    void wordsThatLookLikePlatesAreIgnored() {
        AssistantIntent intent = interpreter.interpret("órdenes con 120 km", ADMIN);

        assertNull(intent.reference());
        assertTrue(intent.kind() == Kind.OPEN || intent.kind() == Kind.FILTER);
    }

    @Test
    void normalizeRemovesAccentsAndSigns() {
        assertEquals("ordenes en reparacion", LocalAssistantInterpreter.normalize("¡Órdenes EN reparación!"));
        assertTrue(LocalAssistantInterpreter.containsWord("facturas vencidas", "vencidas"));
        assertTrue(!LocalAssistantInterpreter.containsWord("facturacion", "factura"));
    }
}
