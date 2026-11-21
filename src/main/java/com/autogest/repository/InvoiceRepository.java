package com.autogest.repository;

import com.autogest.dao.InvoiceDao;
import com.autogest.model.Invoice;
import com.autogest.model.Catalogs.InvoiceStatus;
import com.autogest.model.InvoiceSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Organiza el acceso a las facturas y aísla al Service del SQL. Las facturas
 * llegan con su orden «resumida» (número, estado, vehículo y cliente); la
 * orden completa la pide el Service a {@code ServiceOrderService}.
 *
 * <p>Principles: Indirection, Pure Fabrication, Low Coupling, DIP.</p>
 */
public class InvoiceRepository {

    private final InvoiceDao dao;

    /**
     * @param dao acceso SQL a la tabla {@code invoices}
     */
    public InvoiceRepository(InvoiceDao dao) {
        this.dao = dao;
    }

    /** Guarda una factura nueva y le asigna su id. */
    public Invoice create(Invoice invoice) {
        return dao.create(invoice);
    }

    /** Factura por id, o {@code null} si no existe. */
    public Invoice findById(int id) {
        return dao.findById(id);
    }

    /** Todas las facturas, de la más reciente a la más antigua. */
    public List<Invoice> findAll() {
        return dao.findAll();
    }

    /** Guarda todos los campos de la factura. */
    public boolean update(Invoice invoice) {
        return dao.update(invoice);
    }

    /** Factura de una orden, o {@code null} si no tiene. */
    public Invoice findByOrder(int orderId) {
        return dao.findByOrder(orderId);
    }

    /** Búsqueda por texto, estado, fecha de emisión desde y método de pago. */
    public List<Invoice> search(String text, InvoiceStatus status, LocalDate issuedFrom, String paymentMethod) {
        return dao.search(text, status, issuedFrom, paymentMethod);
    }

    /** Conteos y valores para los indicadores. */
    public InvoiceSummary summary() {
        return dao.summary();
    }

    /** Conteos y valores de las facturas emitidas desde una fecha ({@code null} = todas). */
    public InvoiceSummary summary(LocalDate issuedFrom) {
        return dao.summary(issuedFrom);
    }

    /** Pasa a vencidas las pendientes con vencimiento anterior a {@code today}; devuelve sus ids. */
    public List<Integer> markOverdue(LocalDate today) {
        return dao.markOverdue(today);
    }

    /** Ids de las órdenes con factura vigente (no anulada). */
    public Set<Integer> findInvoicedOrderIds() {
        return dao.findInvoicedOrderIds();
    }

    /** Siguiente consecutivo para el número de factura. */
    public int nextSequence() {
        return dao.nextSequence();
    }
}
