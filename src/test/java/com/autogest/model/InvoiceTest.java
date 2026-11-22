package com.autogest.model;

import com.autogest.model.Catalogs.InvoiceStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas de {@link Invoice}: total, vencimiento y cambios de estado.
 */
class InvoiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);

    private Invoice invoice(String subtotal, String taxes, String discount) {
        Invoice invoice = new Invoice();
        invoice.setSubtotal(new BigDecimal(subtotal));
        invoice.setTaxes(new BigDecimal(taxes));
        invoice.setDiscount(new BigDecimal(discount));
        return invoice;
    }

    private Invoice invoiceDueOn(LocalDate dueDate, InvoiceStatus status) {
        Invoice invoice = new Invoice();
        invoice.setDueDate(dueDate);
        invoice.setStatus(status);
        return invoice;
    }

    // ---------- valores iniciales ----------

    @Test
    void newInvoiceIsPendingWithZeroAmounts() {
        Invoice invoice = new Invoice();

        assertEquals(InvoiceStatus.PENDING, invoice.getStatus());
        assertEquals(new BigDecimal("0.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("0.00"), invoice.getTaxes());
        assertEquals(new BigDecimal("0.00"), invoice.getDiscount());
        assertEquals(new BigDecimal("0.00"), invoice.getTotal());
        assertEquals(new BigDecimal("0.00"), invoice.calculateTotal());
    }

    // ---------- total ----------

    @Test
    void totalIsSubtotalPlusTaxesMinusDiscount() {
        Invoice invoice = invoice("270000", "51300", "20000");

        assertEquals(new BigDecimal("301300.00"), invoice.calculateTotal());
    }

    @Test
    void totalWithZeroTaxesAndZeroDiscountEqualsSubtotal() {
        assertEquals(new BigDecimal("270000.00"), invoice("270000", "0", "0").calculateTotal());
    }

    @Test
    void calculateTotalDoesNotChangeTheStoredTotal() {
        Invoice invoice = invoice("100000", "0", "0");

        invoice.calculateTotal();
        assertEquals(new BigDecimal("0.00"), invoice.getTotal());

        invoice.setTotal(invoice.calculateTotal());
        assertEquals(new BigDecimal("100000.00"), invoice.getTotal());
    }

    @Test
    void discountEqualToSubtotalGivesZeroButGreaterIsRejected() {
        assertEquals(new BigDecimal("0.00"), invoice("50000", "0", "50000").calculateTotal());

        Invoice tooMuch = invoice("50000", "0", "50000.01");
        ValidationException error = assertThrows(ValidationException.class, tooMuch::calculateTotal);
        assertEquals("El descuento no puede ser mayor que el subtotal más los impuestos", error.getMessage());
    }

    @Test
    void amountsUseScaleTwoWithHalfUpRounding() {
        Invoice invoice = new Invoice();

        invoice.setSubtotal(new BigDecimal("1000"));
        assertEquals(new BigDecimal("1000.00"), invoice.getSubtotal());

        invoice.setTaxes(new BigDecimal("10.005"));
        assertEquals(new BigDecimal("10.01"), invoice.getTaxes());
    }

    @Test
    void amountSettersRejectNullAndNegativeValues() {
        Invoice invoice = new Invoice();

        assertThrows(ValidationException.class, () -> invoice.setSubtotal(null));
        assertThrows(ValidationException.class, () -> invoice.setSubtotal(new BigDecimal("-1")));
        assertThrows(ValidationException.class, () -> invoice.setTaxes(null));
        assertThrows(ValidationException.class, () -> invoice.setTaxes(new BigDecimal("-1")));
        assertThrows(ValidationException.class, () -> invoice.setDiscount(null));
        assertThrows(ValidationException.class, () -> invoice.setDiscount(new BigDecimal("-1")));
        assertThrows(ValidationException.class, () -> invoice.setTotal(null));
        assertThrows(ValidationException.class, () -> invoice.setTotal(new BigDecimal("-1")));
        assertEquals(new BigDecimal("0.00"), invoice.getSubtotal());
    }

    // ---------- vencimiento ----------

    @Test
    void pendingInvoiceIsOverdueOnlyAfterTheDueDate() {
        assertTrue(invoiceDueOn(TODAY.minusDays(1), InvoiceStatus.PENDING).isOverdue(TODAY));
        assertFalse(invoiceDueOn(TODAY, InvoiceStatus.PENDING).isOverdue(TODAY));
        assertFalse(invoiceDueOn(TODAY.plusDays(1), InvoiceStatus.PENDING).isOverdue(TODAY));
    }

    @Test
    void onlyPendingInvoicesAreDetectedAsOverdue() {
        LocalDate longAgo = TODAY.minusDays(60);

        assertFalse(invoiceDueOn(longAgo, InvoiceStatus.PAID).isOverdue(TODAY));
        assertFalse(invoiceDueOn(longAgo, InvoiceStatus.VOIDED).isOverdue(TODAY));
        assertFalse(invoiceDueOn(longAgo, InvoiceStatus.OVERDUE).isOverdue(TODAY));
    }

    @Test
    void invoiceWithoutDueDateIsNeverOverdue() {
        assertFalse(invoiceDueOn(null, InvoiceStatus.PENDING).isOverdue(TODAY));
    }

    @Test
    void isOverdueWithoutArgumentUsesTheCurrentDate() {
        assertTrue(invoiceDueOn(LocalDate.now().minusDays(1), InvoiceStatus.PENDING).isOverdue());
        assertFalse(invoiceDueOn(LocalDate.now().plusDays(1), InvoiceStatus.PENDING).isOverdue());
    }

    // ---------- pagar ----------

    @Test
    void markAsPaidFromPendingAndOverdue() {
        Invoice pending = new Invoice();
        pending.markAsPaid("Efectivo");
        assertEquals(InvoiceStatus.PAID, pending.getStatus());
        assertEquals("Efectivo", pending.getPaymentMethod());

        Invoice overdue = new Invoice();
        overdue.setStatus(InvoiceStatus.OVERDUE);
        overdue.markAsPaid("Transferencia");
        assertEquals(InvoiceStatus.PAID, overdue.getStatus());
        assertEquals("Transferencia", overdue.getPaymentMethod());
    }

    @Test
    void markAsPaidRejectsAlreadyPaidAndVoidedInvoices() {
        Invoice paid = new Invoice();
        paid.markAsPaid("Efectivo");
        ValidationException alreadyPaid = assertThrows(ValidationException.class, () -> paid.markAsPaid("Tarjeta"));
        assertEquals("La factura ya está pagada", alreadyPaid.getMessage());
        assertEquals("Efectivo", paid.getPaymentMethod());

        Invoice voided = new Invoice();
        voided.voidInvoice();
        ValidationException voidedError = assertThrows(ValidationException.class, () -> voided.markAsPaid("Efectivo"));
        assertEquals("No se puede pagar una factura anulada", voidedError.getMessage());
        assertEquals(InvoiceStatus.VOIDED, voided.getStatus());
    }

    @Test
    void markAsPaidRequiresAPaymentMethod() {
        Invoice invoice = new Invoice();

        assertThrows(ValidationException.class, () -> invoice.markAsPaid(null));
        assertThrows(ValidationException.class, () -> invoice.markAsPaid("   "));
        assertEquals(InvoiceStatus.PENDING, invoice.getStatus());
    }

    // ---------- anular ----------

    @Test
    void voidInvoiceFromPendingAndOverdue() {
        Invoice pending = new Invoice();
        pending.voidInvoice();
        assertEquals(InvoiceStatus.VOIDED, pending.getStatus());

        Invoice overdue = new Invoice();
        overdue.setStatus(InvoiceStatus.OVERDUE);
        overdue.voidInvoice();
        assertEquals(InvoiceStatus.VOIDED, overdue.getStatus());
    }

    @Test
    void voidInvoiceRejectsPaidAndAlreadyVoidedInvoices() {
        Invoice paid = new Invoice();
        paid.markAsPaid("Efectivo");
        ValidationException paidError = assertThrows(ValidationException.class, paid::voidInvoice);
        assertEquals("No se puede anular una factura pagada", paidError.getMessage());
        assertEquals(InvoiceStatus.PAID, paid.getStatus());

        Invoice voided = new Invoice();
        voided.voidInvoice();
        ValidationException voidedError = assertThrows(ValidationException.class, voided::voidInvoice);
        assertEquals("La factura ya está anulada", voidedError.getMessage());
    }
}
