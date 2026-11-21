package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.Catalogs.OrderStatus;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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

    /** Alto en píxeles de una página A4 a 150 ppp (PDFBox redondea hacia abajo). */
    private static final int A4_PNG_HEIGHT = 1753;

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

    // ---------- datos del taller y nota ----------

    @Test
    void workshopDataReplacesTheSlogan() throws IOException {
        com.autogest.model.WorkshopProfile profile = new com.autogest.model.WorkshopProfile();
        profile.setName("Taller El Bulldog");
        profile.setNit("901.456.789-1");
        profile.setAddress("Carrera 15 # 25-40");
        profile.setCity("Valledupar");
        profile.setPhone("605 555 1234");
        profile.setEmail("contacto@bulldog.com");
        InvoicePdfService withWorkshop = new InvoicePdfService();
        withWorkshop.setDocumentSources(() -> profile, null);

        String text = text(withWorkshop.render(invoice(InvoiceStatus.PENDING, 2)));

        assertTrue(text.contains("Taller El Bulldog"), text);
        assertTrue(text.contains("NIT 901.456.789-1 · Carrera 15 # 25-40, Valledupar"), text);
        assertTrue(text.contains("Tel. 605 555 1234 · contacto@bulldog.com"), text);
        assertFalse(text.contains("Gestión integral para talleres automotrices"), text);
    }

    @Test
    void withoutWorkshopDataTheSloganStays() throws IOException {
        InvoicePdfService failing = new InvoicePdfService();
        failing.setDocumentSources(() -> {
            throw new com.autogest.exception.DataAccessException("sin conexión");
        }, null);

        assertTrue(text(failing.render(invoice(InvoiceStatus.PENDING, 2)))
                .contains("Gestión integral para talleres automotrices"));
    }

    @Test
    void configuredNoteIsPrinted() throws IOException {
        com.autogest.model.BillingSettings billing = com.autogest.model.BillingSettings.defaults();
        billing.setInvoiceNote("Transferencias a la cuenta Bancolombia 123-456789-00.\nGracias por su pago.");
        InvoicePdfService withNote = new InvoicePdfService();
        withNote.setDocumentSources(null, () -> billing);

        String text = text(withNote.render(invoice(InvoiceStatus.PENDING, 2)));

        assertTrue(text.contains("NOTA"), text);
        assertTrue(text.contains("Bancolombia 123-456789-00."), text);
        assertTrue(text.contains("Gracias por su pago."), text);
        assertFalse(text(service.render(invoice(InvoiceStatus.PENDING, 2))).contains("NOTA"));
    }

    // ---------- PNG ----------

    @Test
    void pngIsAnA4PageAt150Dpi() throws IOException {
        byte[] png = service.renderPng(invoice(InvoiceStatus.PENDING, 4));

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        // A4 (595 × 842 pt) a 150 ppp.
        assertEquals(1240, image.getWidth());
        assertEquals(A4_PNG_HEIGHT, image.getHeight());
        // El encabezado de la factura no es una página en blanco.
        assertTrue(hasDarkPixels(image), "La imagen tiene contenido");
    }

    @Test
    void longInvoicePngStacksAllPages() throws IOException {
        Invoice invoice = invoice(InvoiceStatus.PENDING, 60);
        int pages;
        try (PDDocument document = Loader.loadPDF(service.render(invoice))) {
            pages = document.getNumberOfPages();
        }

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(service.renderPng(invoice)));

        assertEquals(A4_PNG_HEIGHT * pages, image.getHeight());
    }

    @Test
    void exportPngWritesTheFile(@TempDir Path folder) throws IOException {
        Invoice invoice = invoice(InvoiceStatus.PAID, 2);
        Path target = folder.resolve(InvoicePdfService.pngFileName(invoice));

        service.exportPng(invoice, target);

        assertEquals("Factura FAC-2026-001.png", target.getFileName().toString());
        byte[] bytes = Files.readAllBytes(target);
        assertEquals((byte) 0x89, bytes[0]);
        assertEquals("PNG", new String(bytes, 1, 3));
    }

    @Test
    void invoiceWithoutOrderIsRejected() {
        Invoice invoice = new Invoice();
        invoice.setNumber("FAC-2026-009");

        assertThrows(ValidationException.class, () -> service.render(invoice));
    }

    // ---------- ayudas ----------

    private static boolean hasDarkPixels(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y += 4) {
            for (int x = 0; x < image.getWidth(); x += 4) {
                if ((image.getRGB(x, y) & 0xFF) < 100) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    /**
     * Factura con descuento de $10.000 y {@code lines} conceptos (los dos
     * primeros suman $480.000 si son 4). La usan también las pruebas de los adjuntos.
     */
    static Invoice invoice(InvoiceStatus status, int lines) {
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
