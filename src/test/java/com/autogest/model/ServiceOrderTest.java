package com.autogest.model;

import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.Catalogs.Role;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas de {@link ServiceOrder}: costos, transiciones de estado e historial.
 */
class ServiceOrderTest {

    private static final List<OrderStatus> FLOW = List.of(
            OrderStatus.RECEIVED, OrderStatus.DIAGNOSIS, OrderStatus.IN_REPAIR,
            OrderStatus.READY, OrderStatus.DELIVERED);

    private User mechanic() {
        User user = new User();
        user.setFirstName("Carlos");
        user.setLastName("Martínez");
        user.setRole(Role.MECHANIC);
        return user;
    }

    private ServiceLine serviceLine(String name, int quantity, String price) {
        ServiceOffering offering = new ServiceOffering();
        offering.setName(name);
        ServiceLine line = new ServiceLine();
        line.setServiceOffering(offering);
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(price));
        return line;
    }

    private PartLine partLine(String name, int quantity, String price) {
        Part part = new Part();
        part.setName(name);
        PartLine line = new PartLine();
        line.setPart(part);
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(price));
        return line;
    }

    private ServiceOrder orderIn(OrderStatus status) {
        ServiceOrder order = new ServiceOrder();
        order.setStatus(status);
        return order;
    }

    // ---------- costos ----------

    @Test
    void subtotalAddsServicesAndPartsTogether() {
        ServiceOrder order = new ServiceOrder();
        order.addLine(serviceLine("Cambio de aceite", 1, "120000"));
        order.addLine(serviceLine("Revisión general", 1, "80000"));
        order.addLine(serviceLine("Revisión de frenos", 1, "35000"));
        order.addLine(partLine("Filtro de aceite", 1, "35000"));

        assertEquals(new BigDecimal("270000.00"), order.calculateSubtotal());
    }

    @Test
    void subtotalRespectsQuantities() {
        ServiceOrder order = new ServiceOrder();
        order.addLine(partLine("Bujías", 4, "28000"));
        order.addLine(serviceLine("Alineación", 1, "90000"));

        assertEquals(new BigDecimal("202000.00"), order.calculateSubtotal());
    }

    @Test
    void emptyOrderHasZeroSubtotalAndTotal() {
        ServiceOrder order = new ServiceOrder();

        assertEquals(new BigDecimal("0.00"), order.calculateSubtotal());
        assertEquals(new BigDecimal("0.00"), order.calculateTotal());
    }

    @Test
    void taxesAreZeroAndTotalEqualsSubtotal() {
        ServiceOrder order = new ServiceOrder();
        order.addLine(serviceLine("Cambio de aceite", 1, "120000"));
        order.addLine(partLine("Filtro de aceite", 1, "35000"));

        assertEquals(new BigDecimal("0.00"), order.calculateTaxes());
        assertEquals(order.calculateSubtotal(), order.calculateTotal());
        assertEquals(new BigDecimal("155000.00"), order.calculateTotal());
    }

    @Test
    void orderCanBeUsedAsBillable() {
        Billable billable = new ServiceOrder();

        assertEquals(new BigDecimal("0.00"), billable.calculateTotal());
    }

    // ---------- líneas ----------

    @Test
    void addLineRejectsNull() {
        assertThrows(ValidationException.class, () -> new ServiceOrder().addLine(null));
    }

    @Test
    void linesCannotBeModifiedFromOutside() {
        ServiceOrder order = new ServiceOrder();

        assertThrows(UnsupportedOperationException.class, () -> order.getLines().add(new ServiceLine()));
    }

    // ---------- transiciones ----------

    @Test
    void newOrderStartsReceived() {
        assertEquals(OrderStatus.RECEIVED, new ServiceOrder().getStatus());
    }

    @Test
    void validTransitionsAdvanceOneStepAtATime() {
        for (int i = 0; i < FLOW.size() - 1; i++) {
            assertTrue(orderIn(FLOW.get(i)).canChangeTo(FLOW.get(i + 1)),
                    FLOW.get(i) + " -> " + FLOW.get(i + 1));
        }
    }

    @Test
    void skippingStepsIsInvalid() {
        assertFalse(orderIn(OrderStatus.RECEIVED).canChangeTo(OrderStatus.IN_REPAIR));
        assertFalse(orderIn(OrderStatus.RECEIVED).canChangeTo(OrderStatus.READY));
        assertFalse(orderIn(OrderStatus.DIAGNOSIS).canChangeTo(OrderStatus.DELIVERED));
    }

    @Test
    void goingBackwardsOrStayingIsInvalid() {
        assertFalse(orderIn(OrderStatus.DIAGNOSIS).canChangeTo(OrderStatus.RECEIVED));
        assertFalse(orderIn(OrderStatus.READY).canChangeTo(OrderStatus.IN_REPAIR));
        for (OrderStatus status : FLOW) {
            assertFalse(orderIn(status).canChangeTo(status), "mismo estado: " + status);
        }
    }

    @Test
    void cancellationIsAllowedFromEveryStatusExceptDeliveredAndCancelled() {
        assertTrue(orderIn(OrderStatus.RECEIVED).canChangeTo(OrderStatus.CANCELLED));
        assertTrue(orderIn(OrderStatus.DIAGNOSIS).canChangeTo(OrderStatus.CANCELLED));
        assertTrue(orderIn(OrderStatus.IN_REPAIR).canChangeTo(OrderStatus.CANCELLED));
        assertTrue(orderIn(OrderStatus.READY).canChangeTo(OrderStatus.CANCELLED));
        assertFalse(orderIn(OrderStatus.DELIVERED).canChangeTo(OrderStatus.CANCELLED));
        assertFalse(orderIn(OrderStatus.CANCELLED).canChangeTo(OrderStatus.CANCELLED));
    }

    @Test
    void deliveredAndCancelledOrdersCannotChangeToAnyStatus() {
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(orderIn(OrderStatus.DELIVERED).canChangeTo(target), "DELIVERED -> " + target);
            assertFalse(orderIn(OrderStatus.CANCELLED).canChangeTo(target), "CANCELLED -> " + target);
        }
    }

    @Test
    void canChangeToNullIsFalse() {
        assertFalse(new ServiceOrder().canChangeTo(null));
    }

    // ---------- cambio de estado e historial ----------

    @Test
    void changeStatusUpdatesStatusAndRecordsOneHistoryEntry() {
        ServiceOrder order = new ServiceOrder();
        User user = mechanic();

        order.changeStatus(OrderStatus.DIAGNOSIS, user);

        assertEquals(OrderStatus.DIAGNOSIS, order.getStatus());
        assertEquals(1, order.getHistory().size());
        OrderHistory entry = order.getHistory().get(0);
        assertEquals("Estado cambiado de Recibido a Diagnóstico", entry.getDescription());
        assertSame(user, entry.getUser());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void changeStatusCanWalkTheWholeFlowAndLeavesOneEntryPerStep() {
        ServiceOrder order = new ServiceOrder();
        User user = mechanic();

        for (int i = 1; i < FLOW.size(); i++) {
            order.changeStatus(FLOW.get(i), user);
        }

        assertEquals(OrderStatus.DELIVERED, order.getStatus());
        assertEquals(4, order.getHistory().size());
    }

    @Test
    void changeStatusToCancelledRecordsHistory() {
        ServiceOrder order = orderIn(OrderStatus.IN_REPAIR);

        order.changeStatus(OrderStatus.CANCELLED, mechanic());

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals("Estado cambiado de En reparación a Cancelado", order.getHistory().get(0).getDescription());
    }

    @Test
    void invalidTransitionThrowsAndChangesNothing() {
        ServiceOrder order = new ServiceOrder();

        ValidationException error = assertThrows(ValidationException.class,
                () -> order.changeStatus(OrderStatus.READY, mechanic()));

        assertEquals("No se puede cambiar el estado de Recibido a Listo", error.getMessage());
        assertEquals(OrderStatus.RECEIVED, order.getStatus());
        assertTrue(order.getHistory().isEmpty());
    }

    @Test
    void changeStatusRequiresStatusAndUser() {
        ServiceOrder order = new ServiceOrder();

        assertThrows(ValidationException.class, () -> order.changeStatus(null, mechanic()));
        assertThrows(ValidationException.class, () -> order.changeStatus(OrderStatus.DIAGNOSIS, null));
        assertEquals(OrderStatus.RECEIVED, order.getStatus());
    }

    @Test
    void recordHistoryCreatesEntryWithCurrentTimeAndUser() {
        ServiceOrder order = new ServiceOrder();
        User user = mechanic();
        LocalDateTime before = LocalDateTime.now();

        order.recordHistory("Orden creada", user);

        LocalDateTime after = LocalDateTime.now();
        OrderHistory entry = order.getHistory().get(0);
        assertEquals("Orden creada", entry.getDescription());
        assertSame(user, entry.getUser());
        assertFalse(entry.getTimestamp().isBefore(before));
        assertFalse(entry.getTimestamp().isAfter(after));
    }

    @Test
    void recordHistoryRejectsBlankTextAndMissingUser() {
        ServiceOrder order = new ServiceOrder();

        assertThrows(ValidationException.class, () -> order.recordHistory("  ", mechanic()));
        assertThrows(ValidationException.class, () -> order.recordHistory(null, mechanic()));
        assertThrows(ValidationException.class, () -> order.recordHistory("Orden creada", null));
        assertTrue(order.getHistory().isEmpty());
    }

    @Test
    void historyCannotBeModifiedFromOutside() {
        ServiceOrder order = new ServiceOrder();

        assertThrows(UnsupportedOperationException.class, () -> order.getHistory().add(new OrderHistory()));
    }
}
