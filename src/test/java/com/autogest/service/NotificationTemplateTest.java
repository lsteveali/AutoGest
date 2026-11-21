package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.Catalogs.InvoiceStatus;
import com.autogest.model.NotificationType;
import com.autogest.model.ServiceOrder;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link NotificationTemplate}: mensaje predeterminado por tipo,
 * valores de los campos y que ningún texto quede con llaves o huecos.
 */
class NotificationTemplateTest {

    @Test
    void eachTypeHasItsDefaultMessage() {
        assertEquals(NotificationTemplate.SERVICE_UPDATE,
                NotificationTemplate.forManual(NotificationType.SERVICE_UPDATE, null));
        assertEquals(NotificationTemplate.MAINTENANCE_INVITE,
                NotificationTemplate.forManual(NotificationType.MAINTENANCE_REMINDER, null));
        assertEquals(NotificationTemplate.PROMOTION, NotificationTemplate.forManual(NotificationType.PROMOTION, null));
        assertEquals(NotificationTemplate.GENERAL, NotificationTemplate.forManual(NotificationType.GENERAL, null));
        assertEquals(NotificationTemplate.GENERAL, NotificationTemplate.forManual(null, null));
        for (NotificationType type : NotificationType.values()) {
            assertEquals(type, NotificationTemplate.forManual(type, null).getType());
        }
    }

    @Test
    void billingMessageDependsOnTheInvoiceStatus() {
        assertEquals(NotificationTemplate.INVOICE_AVAILABLE,
                NotificationTemplate.forManual(NotificationType.BILLING, null));
        assertEquals(NotificationTemplate.INVOICE_AVAILABLE,
                NotificationTemplate.forManual(NotificationType.BILLING, invoice(InvoiceStatus.PENDING)));
        assertEquals(NotificationTemplate.INVOICE_PAID,
                NotificationTemplate.forManual(NotificationType.BILLING, invoice(InvoiceStatus.PAID)));
        assertEquals(NotificationTemplate.INVOICE_OVERDUE,
                NotificationTemplate.forManual(NotificationType.BILLING, invoice(InvoiceStatus.OVERDUE)));
    }

    @Test
    void invoiceMessageHasNumberTotalVehicleOrderAndDueDate() {
        Invoice invoice = invoice(InvoiceStatus.PENDING);
        Map<String, String> values = NotificationTemplate.valuesFor(
                invoice.getOrder().getVehicle().getOwner(), invoice.getOrder(), invoice);

        String message = NotificationTemplate.INVOICE_AVAILABLE.message(values);

        assertEquals("Factura FAC-2026-001 disponible", NotificationTemplate.INVOICE_AVAILABLE.subject(values));
        assertTrue(message.startsWith("Hola Carlos, te enviamos la factura FAC-2026-001 por "), message);
        assertTrue(message.contains("Toyota Corolla (ABC123), orden #0045"), message);
        assertTrue(message.contains("adjunta en PDF y como imagen"), message);
        assertTrue(message.contains(com.autogest.util.Formatter.date(invoice.getDueDate())), message);
        assertTrue(message.contains(com.autogest.util.Formatter.money(invoice.getTotal())), message);
    }

    @Test
    void withoutOrderTheVehicleReadsNaturally() {
        Customer ana = new Customer();
        ana.setFirstName("Ana");
        Map<String, String> values = NotificationTemplate.valuesFor(ana, null, null);

        String subject = NotificationTemplate.MAINTENANCE_INVITE.subject(values);
        String message = NotificationTemplate.MAINTENANCE_INVITE.message(values);

        assertEquals("Es momento del mantenimiento de tu vehículo", subject);
        assertTrue(message.contains("de tu vehículo."), message);
        assertFalse(message.contains("()"), message);
    }

    @Test
    void everyTemplateFillsWithoutBracesOrDoubleSpaces() {
        Invoice invoice = invoice(InvoiceStatus.PENDING);
        Map<String, String> full = NotificationTemplate.valuesFor(
                invoice.getOrder().getVehicle().getOwner(), invoice.getOrder(), invoice);
        full.put("fecha", "30/11/2026");
        for (NotificationTemplate template : NotificationTemplate.values()) {
            for (String text : new String[] {template.subject(full), template.message(full)}) {
                assertFalse(text.contains("{") || text.contains("}"), template + ": " + text);
                assertFalse(text.contains("  "), template + ": " + text);
                assertFalse(text.isBlank(), template.name());
            }
            assertTrue(template.message(full).startsWith("Hola Carlos"), template.name());
        }
    }

    @Test
    void unknownFieldIsRejected() {
        assertThrows(ValidationException.class, () -> NotificationTemplate.fill("Hola {nombre}", Map.of()));
    }

    private static Invoice invoice(InvoiceStatus status) {
        Invoice invoice = InvoicePdfServiceTest.invoice(status, 2);
        ServiceOrder order = invoice.getOrder();
        assertTrue(order.getVehicle().getOwner() != null);
        return invoice;
    }
}
