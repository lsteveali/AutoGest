package com.autogest.service;

import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.InvoiceSummary;
import com.autogest.repository.InvoiceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

/**
 * Repositorio falso de facturas, en memoria. Reproduce la búsqueda, los
 * indicadores y el paso a vencidas del {@code InvoiceDao}.
 */
class FakeInvoiceRepository extends InvoiceRepository {

    private final Map<Integer, Invoice> invoices = new LinkedHashMap<>();
    private int sequence;
    int updates;

    FakeInvoiceRepository() {
        super(null);
    }

    @Override
    public Invoice create(Invoice invoice) {
        invoice.setId(++sequence);
        invoices.put(invoice.getId(), invoice);
        return invoice;
    }

    @Override
    public Invoice findById(int id) {
        return invoices.get(id);
    }

    @Override
    public List<Invoice> findAll() {
        return newestFirst(new ArrayList<>(invoices.values()));
    }

    @Override
    public boolean update(Invoice invoice) {
        updates++;
        return invoices.replace(invoice.getId(), invoice) != null;
    }

    @Override
    public Invoice findByOrder(int orderId) {
        return invoices.values().stream().filter(i -> i.getOrder().getId() == orderId).findFirst().orElse(null);
    }

    @Override
    public List<Invoice> search(String text, InvoiceStatus status, LocalDate issuedFrom, String paymentMethod) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        List<Invoice> result = new ArrayList<>();
        for (Invoice invoice : invoices.values()) {
            boolean matchesText = needle.isEmpty() || contains(invoice.getNumber(), needle)
                    || contains(invoice.getOrder().getVehicle().getLicensePlate(), needle)
                    || contains(invoice.getOrder().getVehicle().getOwner().getFullName(), needle);
            boolean matchesStatus = status == null || invoice.getStatus() == status;
            boolean matchesDate = issuedFrom == null || !invoice.getIssueDate().isBefore(issuedFrom);
            boolean matchesMethod = paymentMethod == null || paymentMethod.equals(invoice.getPaymentMethod());
            if (matchesText && matchesStatus && matchesDate && matchesMethod) {
                result.add(invoice);
            }
        }
        return newestFirst(result);
    }

    @Override
    public InvoiceSummary summary() {
        return new InvoiceSummary(invoices.size(), count(i -> i.getStatus() == InvoiceStatus.PAID),
                count(i -> i.getStatus() == InvoiceStatus.PENDING), count(i -> i.getStatus() == InvoiceStatus.OVERDUE),
                sum(i -> i.getStatus() != InvoiceStatus.VOIDED), sum(i -> i.getStatus() == InvoiceStatus.PAID),
                sum(i -> i.getStatus() == InvoiceStatus.PENDING || i.getStatus() == InvoiceStatus.OVERDUE));
    }

    @Override
    public List<Integer> markOverdue(LocalDate today) {
        List<Integer> ids = new ArrayList<>();
        for (Invoice invoice : invoices.values()) {
            if (invoice.isOverdue(today)) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                ids.add(invoice.getId());
            }
        }
        return ids;
    }

    @Override
    public Set<Integer> findInvoicedOrderIds() {
        Set<Integer> ids = new TreeSet<>();
        invoices.values().stream().filter(i -> i.getStatus() != InvoiceStatus.VOIDED)
                .forEach(i -> ids.add(i.getOrder().getId()));
        return ids;
    }

    @Override
    public int nextSequence() {
        return sequence + 1;
    }

    private int count(Predicate<Invoice> filter) {
        return (int) invoices.values().stream().filter(filter).count();
    }

    private BigDecimal sum(Predicate<Invoice> filter) {
        return invoices.values().stream().filter(filter).map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<Invoice> newestFirst(List<Invoice> list) {
        List<Invoice> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparing(Invoice::getId).reversed());
        return copy;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
