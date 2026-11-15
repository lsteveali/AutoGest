package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.DocumentType;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link InvoicePdfService}: se genera el PDF en memoria y se lee
 * su texto con PDFBox para comprobar el contenido.
 */
class InvoicePdfServiceTest {

    private final InvoicePdfService service = new InvoicePdfService();

    @Test
    void pdfHasNumberCustomerConceptsAndTotals() throws IOException {
        Invoice invoice = invoice(InvoiceStatus.PENDING, 4);

        String text = text(service.render(invoice));

        assertTrue(text.contains("FAC-2026-001"), text);
        assertTrue(text.contains("Carlos Rodríguez"), text);
        assertTrue(text.contains("CC 1.098.456.789"), text);
        assertTrue(text.contains("300 555 1234"), text);
        assertTrue(text.contains("Toyota Corolla · ABC123"), text);
        assertTrue(text.contains("#0045"), text);
        assertTrue(text.contains("Cambio de aceite"), text);
        assertTrue(text.contains("Repuesto"), text);
        assertTrue(text.contains("$470.000"), "Total con el descuento: " + text);
        assertTrue(text.contains("-$10.000"), text);
        assertTrue(text.contains("IVA (0 %)"), text);
        assertTrue(text.contains("Pendiente de pago hasta el"), text);
        assertFalse(text.contains("ANULADA"), text);
    }

    @Test
    void paidInvoiceShowsPaymentMethod() throws IOException {
        Invoice invoice = invoice(InvoiceStatus.PAID, 2);
        invoice.setPaymentMethod("TRANSFER");

        assertTrue(text(service.render(invoice)).contains("Pagada con Transferencia"));
    }

    @Test
    void voidedInvoiceIsMarked() throws IOException {
        String text = text(service.render(invoice(InvoiceStatus.VOIDED, 2)));

        assertTrue(text.contains("ANULADA"), text);
        assertTrue(text.contains("no se cobra"), text);
    }

    @Test
    void manyConceptsContinueOnAnotherPage() throws IOException {
        byte[] pdf = service.render(invoice(InvoiceStatus.PENDING, 60));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Página 2 de " + document.getNumberOfPages()), text);
            assertTrue(text.contains("Concepto 60"), "Todos los conceptos quedan en el PDF");
        }
    }

    @Test
    void charactersMissingFromTheFontDoNotBreakThePdf() throws IOException {
        Invoice invoice = invoice(InvoiceStatus.PENDING, 1);
        invoice.getOrder().getVehicle().getOwner().setFirstName("Carlos 🚗");

        assertTrue(text(service.render(invoice)).contains("Carlos ?"));
    }

    @Test
    void exportWritesTheFile(@TempDir Path folder) throws IOException {
        Invoice invoice = invoice(InvoiceStatus.PENDING, 2);
        Path target = folder.resolve(InvoicePdfService.fileName(invoice));

        service.export(invoice, target);

        assertEquals("Factura FAC-2026-001.pdf", target.getFileName().toString());
        assertTrue(Files.size(target) > 1000);
        assertTrue(new String(Files.readAllBytes(target), 0, 5).startsWith("%PDF"));
    }

    @Test
    void invoiceWithoutOrderIsRejected() {
        Invoice invoice = new Invoice();
        invoice.setNumber("FAC-2026-009");

        assertThrows(ValidationException.class, () -> service.render(invoice));
    }

    // ---------- ayudas ----------

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /** Factura con descuento de $10.000 y {@code lines} conceptos (los dos primeros suman $480.000 si son 4). */
    private static Invoice invoice(InvoiceStatus status, int lines) {
        Customer owner = new Customer();
        owner.setFirstName("Carlos");
        owner.setLastName("Rodríguez");
        owner.setDocumentType(DocumentType.CC);
        owner.setDocumentNumber("1098456789");
        owner.setPhone("3005551234");
        owner.setEmail("carlos@correo.com");
        Vehicle vehicle = new Vehicle();
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setLicensePlate("ABC123");
        vehicle.setOwner(owner);
        ServiceOrder order = new ServiceOrder();
        order.setNumber("0045");
        order.setStatus(OrderStatus.DELIVERED);
        order.setVehicle(vehicle);
        String[] names = {"Cambio de aceite", "Revisión de frenos", "Filtro de aceite", "Pastillas de freno"};
        String[] prices = {"120000", "180000", "35000", "145000"};
        for (int i = 0; i < lines; i++) {
            String name = lines <= 4 ? names[i] : "Concepto " + (i + 1);
            BigDecimal price = new BigDecimal(lines <= 4 ? prices[i] : "10000");
            if (i % 2 == 0) {
                ServiceOffering offering = new ServiceOffering();
                offering.setName(name);
                ServiceLine line = new ServiceLine();
                line.setServiceOffering(offering);
                line.setQuantity(1);
                line.setUnitPrice(price);
                order.addLine(line);
            } else {
                Part part = new Part();
                part.setName(name);
                PartLine line = new PartLine();
                line.setPart(part);
                line.setQuantity(1);
                line.setUnitPrice(price);
                order.addLine(line);
            }
        }
        Invoice invoice = InvoiceService.buildDraft(order, new BigDecimal("10000"), LocalDate.now().plusDays(14));
        invoice.setNumber("FAC-2026-001");
        invoice.setStatus(status);
        return invoice;
    }
}
