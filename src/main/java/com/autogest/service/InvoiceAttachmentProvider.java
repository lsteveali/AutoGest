package com.autogest.service;

import com.autogest.model.Invoice;
import com.autogest.model.Catalogs.InvoiceStatus;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationType;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Adjunta la factura en PDF y en PNG a los correos de Facturación: cualquier
 * notificación de tipo {@code BILLING} con una orden que tenga factura
 * vigente (no anulada). Sirve igual para los avisos automáticos («Factura
 * disponible», «Pago recibido», «Factura vencida») y para los que se crean a
 * mano en Notificaciones.
 *
 * <p>Principles: Information Expert, Low Coupling.</p>
 */
public class InvoiceAttachmentProvider implements EmailAttachmentProvider {

    private final Supplier<InvoiceService> invoiceService;
    private final InvoicePdfService pdfService;

    /**
     * @param invoiceService facturas; se pide al usarlo porque se crea después que las notificaciones
     * @param pdfService     generador del PDF y la imagen
     */
    public InvoiceAttachmentProvider(Supplier<InvoiceService> invoiceService, InvoicePdfService pdfService) {
        this(invoiceService, pdfService, () -> true);
    }

    /**
     * @param invoiceService facturas
     * @param pdfService     generador del PDF y la imagen
     * @param enabled        interruptor «Adjuntar la factura» de Configuración → Notificaciones
     */
    public InvoiceAttachmentProvider(Supplier<InvoiceService> invoiceService, InvoicePdfService pdfService,
                                     BooleanSupplier enabled) {
        this.invoiceService = invoiceService;
        this.pdfService = pdfService;
        this.enabled = enabled == null ? () -> true : enabled;
    }

    private final BooleanSupplier enabled;

    @Override
    public List<EmailAttachment> attachmentsFor(Notification notification) {
        if (!enabled.getAsBoolean()) {
            return List.of();
        }
        Invoice invoice = invoiceFor(notification);
        if (invoice == null) {
            return List.of();
        }
        return List.of(
                new EmailAttachment(InvoicePdfService.fileName(invoice), EmailAttachment.PDF,
                        pdfService.render(invoice)),
                new EmailAttachment(InvoicePdfService.pngFileName(invoice), EmailAttachment.PNG,
                        pdfService.renderPng(invoice)));
    }

    /**
     * Factura vigente de la orden de la notificación, con su orden completa.
     *
     * @return la factura, o {@code null} si la notificación no es de
     *         Facturación, no tiene orden o la orden no tiene factura vigente
     */
    public Invoice invoiceFor(Notification notification) {
        if (notification == null || notification.getType() != NotificationType.BILLING
                || notification.getOrder() == null || notification.getOrder().getId() <= 0) {
            return null;
        }
        InvoiceService service = invoiceService.get();
        Invoice invoice = service.findByOrder(notification.getOrder().getId());
        if (invoice == null || invoice.getStatus() == InvoiceStatus.VOIDED) {
            return null;
        }
        return service.getDetail(invoice.getId());
    }
}
