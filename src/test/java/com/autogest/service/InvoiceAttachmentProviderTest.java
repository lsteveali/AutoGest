package com.autogest.service;

import com.autogest.model.Invoice;
import com.autogest.model.Catalogs.InvoiceStatus;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationType;
import com.autogest.model.ServiceOrder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link InvoiceAttachmentProvider} con un servicio de facturas
 * falso (sin base de datos) y el generador real del PDF y la imagen.
 */
class InvoiceAttachmentProviderTest {

    private static final int ORDER_ID = 45;

    @Test
    void billingNoticeOfAnInvoicedOrderCarriesPdfAndPng() {
        List<EmailAttachment> files = provider(invoice(InvoiceStatus.PENDING))
                .attachmentsFor(notice(NotificationType.BILLING, ORDER_ID));

        assertEquals(2, files.size());
        assertEquals("Factura FAC-2026-001.pdf", files.get(0).fileName());
        assertEquals(EmailAttachment.PDF, files.get(0).contentType());
        assertTrue(new String(files.get(0).content(), 0, 5).startsWith("%PDF"));
        assertEquals("Factura FAC-2026-001.png", files.get(1).fileName());
        assertEquals(EmailAttachment.PNG, files.get(1).contentType());
        assertEquals("PNG", new String(files.get(1).content(), 1, 3));
    }

    @Test
    void paidAndOverdueInvoicesAlsoTravel() {
        assertEquals(2, provider(invoice(InvoiceStatus.PAID))
                .attachmentsFor(notice(NotificationType.BILLING, ORDER_ID)).size());
        assertEquals(2, provider(invoice(InvoiceStatus.OVERDUE))
                .attachmentsFor(notice(NotificationType.BILLING, ORDER_ID)).size());
    }

    @Test
    void voidedInvoiceIsNotAttached() {
        assertTrue(provider(invoice(InvoiceStatus.VOIDED))
                .attachmentsFor(notice(NotificationType.BILLING, ORDER_ID)).isEmpty());
    }

    @Test
    void attachmentsTurnedOffInSettingsGoEmpty() {
        FakeInvoiceService service = new FakeInvoiceService(invoice(InvoiceStatus.PENDING));
        InvoiceAttachmentProvider off = new InvoiceAttachmentProvider(() -> service, new InvoicePdfService(), () -> false);

        assertTrue(off.attachmentsFor(notice(NotificationType.BILLING, ORDER_ID)).isEmpty());
    }

    @Test
    void otherNoticesGoWithoutAttachments() {
        InvoiceAttachmentProvider provider = provider(invoice(InvoiceStatus.PENDING));

        assertTrue(provider.attachmentsFor(notice(NotificationType.SERVICE_UPDATE, ORDER_ID)).isEmpty());
        assertTrue(provider.attachmentsFor(notice(NotificationType.BILLING, 0)).isEmpty());
        assertTrue(provider.attachmentsFor(notice(NotificationType.BILLING, 99)).isEmpty());
        assertTrue(provider.attachmentsFor(null).isEmpty());
    }

    // ---------- ayudas ----------

    private static InvoiceAttachmentProvider provider(Invoice invoice) {
        FakeInvoiceService service = new FakeInvoiceService(invoice);
        return new InvoiceAttachmentProvider(() -> service, new InvoicePdfService());
    }

    private static Invoice invoice(InvoiceStatus status) {
        Invoice invoice = InvoicePdfServiceTest.invoice(status, 2);
        invoice.setId(7);
        invoice.getOrder().setId(ORDER_ID);
        return invoice;
    }

    private static Notification notice(NotificationType type, int orderId) {
        Notification notification = new Notification();
        notification.setType(type);
        if (orderId > 0) {
            ServiceOrder order = new ServiceOrder();
            order.setId(orderId);
            notification.setOrder(order);
        }
        return notification;
    }

    /** Servicio de facturas falso: una sola factura, de la orden {@value #ORDER_ID}. */
    private static final class FakeInvoiceService extends InvoiceService {

        private final Invoice invoice;

        FakeInvoiceService(Invoice invoice) {
            super(null, null, null, null);
            this.invoice = invoice;
        }

        @Override
        public Invoice findByOrder(int orderId) {
            return orderId == ORDER_ID ? invoice : null;
        }

        @Override
        public Invoice getDetail(int id) {
            return invoice;
        }
    }
}
