package com.autogest.service;

import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.OrderLine;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import com.autogest.util.Formatter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera el PDF de una factura (tamaño carta A4): encabezado con el logo y el
 * número, cliente y datos de la factura, conceptos, totales y estado del pago.
 * Una factura anulada lleva la marca «ANULADA». Si los conceptos no caben, la
 * tabla sigue en otra página con su encabezado.
 *
 * <p>Usa las fuentes Roboto del proyecto incrustadas (D-18) y los colores del
 * sistema de diseño. Necesita la factura con su orden completa (cliente,
 * vehículo y líneas), como la devuelve {@link InvoiceService#getDetail(int)}.</p>
 *
 * <p>Los datos del taller (nombre, NIT, dirección) se agregarán cuando exista
 * «Datos del taller» en Configuración (Dev 1, Sprint 8).</p>
 *
 * <p>Principles: Pure Fabrication, SRP, High Cohesion.</p>
 */
public class InvoicePdfService {

    private static final Color NAVY = new Color(0x10, 0x1E, 0x39);
    private static final Color PRIMARY = new Color(0x2F, 0x6D, 0xB5);
    private static final Color TEXT = new Color(0x1E, 0x2A, 0x3A);
    private static final Color MUTED = new Color(0x64, 0x74, 0x8B);
    private static final Color BORDER = new Color(0xDC, 0xE3, 0xEA);
    private static final Color BAND = new Color(0xF0, 0xF4, 0xF7);
    private static final Color DANGER = new Color(0xD6, 0x45, 0x45);
    private static final Color SUCCESS = new Color(0x1E, 0x9E, 0x5A);
    private static final Color WARNING = new Color(0xB7, 0x7E, 0x0B);

    private static final float MARGIN = 48;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final float FOOTER_LIMIT = MARGIN + 40;
    private static final float ROW_LINE = 13;

    /** Columnas de la tabla: tipo, descripción, cantidad, valor unitario y subtotal (bordes derechos). */
    private static final float COL_TYPE = MARGIN + 10;
    private static final float COL_DESC = MARGIN + 82;
    private static final float COL_QTY_RIGHT = MARGIN + 330;
    private static final float COL_PRICE_RIGHT = MARGIN + 425;
    private static final float COL_TOTAL_RIGHT = MARGIN + CONTENT_WIDTH - 10;
    private static final float DESC_WIDTH = COL_QTY_RIGHT - 50 - COL_DESC;

    /**
     * Genera el PDF y lo guarda en el archivo indicado.
     *
     * @param invoice factura con su orden completa
     * @param target  archivo de destino (se reemplaza si existe)
     * @throws AutoGestException si no se puede generar o escribir el archivo
     */
    public void export(Invoice invoice, Path target) {
        byte[] pdf = render(invoice);
        try {
            Files.write(target, pdf);
        } catch (IOException e) {
            throw new AutoGestException("No se pudo guardar el PDF en " + target.getFileName()
                    + ". Revisa que el archivo no esté abierto en otro programa.", e);
        }
    }

    /**
     * Nombre de archivo sugerido: {@code Factura FAC-2026-001.pdf}.
     *
     * @param invoice factura
     * @return nombre sin caracteres que Windows no permite
     */
    public static String fileName(Invoice invoice) {
        String number = invoice.getNumber() == null ? "sin-numero" : invoice.getNumber();
        return "Factura " + number.replaceAll("[\\\\/:*?\"<>|]", "-") + ".pdf";
    }

    /**
     * Genera el PDF en memoria.
     *
     * @param invoice factura con su orden completa
     * @return bytes del PDF
     * @throws ValidationException si la factura no tiene número u orden
     * @throws AutoGestException   si falla la generación
     */
    public byte[] render(Invoice invoice) {
        if (invoice == null || invoice.getNumber() == null || invoice.getOrder() == null) {
            throw new ValidationException("La factura no tiene los datos necesarios para el PDF");
        }
        try (PDDocument document = new PDDocument()) {
            Writer writer = new Writer(document, invoice);
            writer.write();
            PDDocumentInformation info = document.getDocumentInformation();
            info.setTitle("Factura " + invoice.getNumber());
            info.setCreator("AutoGest");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AutoGestException("No se pudo generar el PDF de la factura.", e);
        }
    }

    /** Escribe una factura en un documento; guarda la página y la posición actuales. */
    private static final class Writer {

        private final PDDocument document;
        private final Invoice invoice;
        private final PDFont regular;
        private final PDFont medium;
        private final PDFont bold;
        private final PDImageXObject logo;
        private final List<PDPageContentStream> streams = new ArrayList<>();
        private PDPageContentStream cs;
        private float y;

        Writer(PDDocument document, Invoice invoice) throws IOException {
            this.document = document;
            this.invoice = invoice;
            this.regular = font(document, "Roboto-Regular.ttf");
            this.medium = font(document, "Roboto-Medium.ttf");
            this.bold = font(document, "Roboto-Bold.ttf");
            this.logo = image(document, "/images/logo-gear-64.png");
        }

        void write() throws IOException {
            newPage();
            header();
            parties();
            concepts();
            totals();
            payment();
            for (PDPageContentStream stream : streams) {
                stream.close();
            }
            footers();
        }

        // ---------- secciones ----------

        private void header() throws IOException {
            float top = PAGE_HEIGHT - MARGIN;
            if (logo != null) {
                cs.drawImage(logo, MARGIN, top - 34, 34, 34);
            }
            text(bold, 20, NAVY, MARGIN + 44, top - 22, "AutoGest");
            text(regular, 9, MUTED, MARGIN + 44, top - 35, "Gestión integral para talleres automotrices");

            float right = MARGIN + CONTENT_WIDTH;
            textRight(medium, 10, MUTED, right, top - 10, "FACTURA");
            textRight(bold, 18, NAVY, right, top - 30, invoice.getNumber());
            String status = statusText(invoice.getStatus());
            textRight(medium, 10, statusColor(invoice.getStatus()), right, top - 45, status);

            y = top - 62;
            line(MARGIN, y, MARGIN + CONTENT_WIDTH, y, BORDER, 1);
            if (invoice.getStatus() == InvoiceStatus.VOIDED) {
                y -= 26;
                text(bold, 16, DANGER, MARGIN, y, "ANULADA");
                text(regular, 9, MUTED, MARGIN + 82, y + 1, "Esta factura fue anulada y no tiene validez de cobro.");
                y -= 10;
            }
            y -= 24;
        }

        private void parties() throws IOException {
            ServiceOrder order = invoice.getOrder();
            Vehicle vehicle = order.getVehicle();
            Customer owner = vehicle == null ? null : vehicle.getOwner();

            float left = MARGIN;
            float right = MARGIN + CONTENT_WIDTH / 2 + 10;
            float start = y;
            text(medium, 9, MUTED, left, y, "FACTURAR A");
            text(medium, 9, MUTED, right, y, "DATOS DE LA FACTURA");
            y -= 16;

            List<String> customer = new ArrayList<>();
            if (owner == null || owner.getFirstName() == null) {
                customer.add("Sin cliente");
            } else {
                customer.add(owner.getFullName());
                addIfPresent(customer, documentText(owner));
                addIfPresent(customer, phoneText(owner.getPhone()));
                addIfPresent(customer, owner.getEmail());
                addIfPresent(customer, owner.getAddress());
            }
            float leftY = y;
            for (int i = 0; i < customer.size(); i++) {
                text(i == 0 ? bold : regular, i == 0 ? 11 : 9.5f, i == 0 ? TEXT : MUTED, left, leftY,
                        fit(i == 0 ? bold : regular, i == 0 ? 11 : 9.5f, customer.get(i), CONTENT_WIDTH / 2 - 20));
                leftY -= i == 0 ? 15 : 13;
            }

            float rightY = y;
            rightY = detailRow(right, rightY, "Emisión", Formatter.date(invoice.getIssueDate()));
            rightY = detailRow(right, rightY, "Vencimiento",
                    invoice.getDueDate() == null ? "-" : Formatter.date(invoice.getDueDate()));
            rightY = detailRow(right, rightY, "Orden de servicio",
                    order.getNumber() == null ? "-" : "#" + order.getNumber());
            if (vehicle != null) {
                String name = (vehicle.getMake() == null ? "" : vehicle.getMake()) + " "
                        + (vehicle.getModel() == null ? "" : vehicle.getModel());
                rightY = detailRow(right, rightY, "Vehículo", name.trim()
                        + (vehicle.getLicensePlate() == null ? "" : " · " + vehicle.getLicensePlate()));
            }
            y = Math.min(leftY, rightY) - 14;
            if (start - y < 60) {
                y = start - 60;
            }
        }

        private float detailRow(float x, float rowY, String label, String value) throws IOException {
            text(regular, 9.5f, MUTED, x, rowY, label);
            textRight(medium, 9.5f, TEXT, MARGIN + CONTENT_WIDTH, rowY,
                    fit(medium, 9.5f, value, CONTENT_WIDTH / 2 - 110));
            return rowY - 14;
        }

        private void concepts() throws IOException {
            tableHeader();
            List<OrderLine> lines = invoice.getOrder().getLines();
            if (lines.isEmpty()) {
                text(regular, 9.5f, MUTED, COL_TYPE, y - 14, "La orden no tiene conceptos.");
                y -= 26;
                return;
            }
            for (OrderLine line : lines) {
                List<String> description = wrap(regular, 9.5f, line.getDescription(), DESC_WIDTH);
                float height = Math.max(1, description.size()) * ROW_LINE + 10;
                if (y - height < FOOTER_LIMIT + 20) {
                    newPage();
                    y = PAGE_HEIGHT - MARGIN;
                    tableHeader();
                }
                float base = y - 15;
                text(regular, 9.5f, TEXT, COL_TYPE, base, line instanceof PartLine ? "Repuesto" : "Servicio");
                for (int i = 0; i < description.size(); i++) {
                    text(regular, 9.5f, TEXT, COL_DESC, base - i * ROW_LINE, description.get(i));
                }
                textRight(regular, 9.5f, TEXT, COL_QTY_RIGHT, base, String.valueOf(line.getQuantity()));
                textRight(regular, 9.5f, TEXT, COL_PRICE_RIGHT, base, Formatter.money(line.getUnitPrice()));
                textRight(medium, 9.5f, TEXT, COL_TOTAL_RIGHT, base, Formatter.money(line.calculateTotal()));
                y -= height;
                line(MARGIN, y, MARGIN + CONTENT_WIDTH, y, BORDER, 0.6f);
            }
        }

        private void tableHeader() throws IOException {
            rect(MARGIN, y - 22, CONTENT_WIDTH, 22, BAND);
            float base = y - 14.5f;
            text(medium, 8.5f, MUTED, COL_TYPE, base, "TIPO");
            text(medium, 8.5f, MUTED, COL_DESC, base, "DESCRIPCIÓN");
            textRight(medium, 8.5f, MUTED, COL_QTY_RIGHT, base, "CANT.");
            textRight(medium, 8.5f, MUTED, COL_PRICE_RIGHT, base, "VALOR UNITARIO");
            textRight(medium, 8.5f, MUTED, COL_TOTAL_RIGHT, base, "SUBTOTAL");
            y -= 22;
        }

        private void totals() throws IOException {
            if (y - 110 < FOOTER_LIMIT) {
                newPage();
                y = PAGE_HEIGHT - MARGIN;
            }
            float labelX = MARGIN + CONTENT_WIDTH - 230;
            float right = COL_TOTAL_RIGHT;
            y -= 22;
            totalRow(labelX, right, "Subtotal", Formatter.money(invoice.getSubtotal()));
            totalRow(labelX, right, "Descuento", invoice.getDiscount().signum() == 0
                    ? Formatter.money(BigDecimal.ZERO) : "-" + Formatter.money(invoice.getDiscount()));
            totalRow(labelX, right, "IVA (" + taxRateText() + ")", Formatter.money(invoice.getTaxes()));
            line(labelX, y + 6, MARGIN + CONTENT_WIDTH, y + 6, BORDER, 1);
            y -= 12;
            text(bold, 13, TEXT, labelX, y, "Total");
            textRight(bold, 15, PRIMARY, right, y, Formatter.money(invoice.getTotal()));
            y -= 30;
        }

        private void totalRow(float labelX, float right, String label, String value) throws IOException {
            text(regular, 10, MUTED, labelX, y, label);
            textRight(regular, 10, TEXT, right, y, value);
            y -= 17;
        }

        private void payment() throws IOException {
            if (y - 60 < FOOTER_LIMIT) {
                newPage();
                y = PAGE_HEIGHT - MARGIN;
            }
            float boxHeight = 46;
            rect(MARGIN, y - boxHeight, CONTENT_WIDTH, boxHeight, BAND);
            text(medium, 9, MUTED, MARGIN + 14, y - 17, "PAGO");
            String detail = switch (invoice.getStatus()) {
                case PAID -> "Pagada con " + InvoiceService.methodText(invoice.getPaymentMethod())
                        + ". Saldo pendiente: " + Formatter.money(BigDecimal.ZERO) + ".";
                case PENDING -> "Pendiente de pago" + (invoice.getDueDate() == null ? ""
                        : " hasta el " + Formatter.date(invoice.getDueDate())) + ". Saldo: "
                        + Formatter.money(invoice.getTotal()) + ".";
                case OVERDUE -> "Vencida" + (invoice.getDueDate() == null ? ""
                        : " desde el " + Formatter.date(invoice.getDueDate())) + ". Saldo: "
                        + Formatter.money(invoice.getTotal()) + ".";
                case VOIDED -> "Factura anulada: no se cobra.";
            };
            text(medium, 10.5f, statusColor(invoice.getStatus()), MARGIN + 14, y - 33,
                    fit(medium, 10.5f, detail, CONTENT_WIDTH - 28));
            y -= boxHeight + 16;
        }

        private void footers() throws IOException {
            int pages = document.getNumberOfPages();
            String generated = "Generada con AutoGest el " + Formatter.timestamp(LocalDateTime.now());
            for (int i = 0; i < pages; i++) {
                try (PDPageContentStream footer = new PDPageContentStream(document, document.getPage(i),
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    PDPageContentStream previous = cs;
                    cs = footer;
                    line(MARGIN, MARGIN + 18, MARGIN + CONTENT_WIDTH, MARGIN + 18, BORDER, 0.6f);
                    text(regular, 8, MUTED, MARGIN, MARGIN + 6, generated);
                    textRight(regular, 8, MUTED, MARGIN + CONTENT_WIDTH, MARGIN + 6,
                            invoice.getNumber() + " · Página " + (i + 1) + " de " + pages);
                    cs = previous;
                }
            }
        }

        // ---------- dibujo ----------

        private void newPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            cs = new PDPageContentStream(document, page);
            streams.add(cs);
        }

        private void text(PDFont font, float size, Color color, float x, float baseline, String value)
                throws IOException {
            if (value == null || value.isEmpty()) {
                return;
            }
            cs.beginText();
            cs.setFont(font, size);
            cs.setNonStrokingColor(color);
            cs.newLineAtOffset(x, baseline);
            cs.showText(safe(font, value));
            cs.endText();
        }

        private void textRight(PDFont font, float size, Color color, float right, float baseline, String value)
                throws IOException {
            if (value == null || value.isEmpty()) {
                return;
            }
            String clean = safe(font, value);
            text(font, size, color, right - width(font, size, clean), baseline, clean);
        }

        private void line(float x1, float y1, float x2, float y2, Color color, float width) throws IOException {
            cs.setStrokingColor(color);
            cs.setLineWidth(width);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        private void rect(float x, float rectY, float w, float h, Color color) throws IOException {
            cs.setNonStrokingColor(color);
            cs.addRect(x, rectY, w, h);
            cs.fill();
        }

        // ---------- texto ----------

        /** Parte el texto en renglones que caben en el ancho dado. */
        private static List<String> wrap(PDFont font, float size, String value, float maxWidth) throws IOException {
            List<String> lines = new ArrayList<>();
            String clean = safe(font, value == null ? "" : value.trim());
            StringBuilder current = new StringBuilder();
            for (String word : clean.split("\\s+")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (width(font, size, candidate) <= maxWidth || current.isEmpty()) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    lines.add(current.toString());
                    current.setLength(0);
                    current.append(word);
                }
            }
            if (!current.isEmpty()) {
                lines.add(current.toString());
            }
            return lines.isEmpty() ? List.of("") : lines;
        }

        /** Recorta el texto con «…» si no cabe en el ancho dado. */
        private static String fit(PDFont font, float size, String value, float maxWidth) throws IOException {
            String clean = safe(font, value == null ? "" : value);
            if (width(font, size, clean) <= maxWidth) {
                return clean;
            }
            String text = clean;
            while (!text.isEmpty() && width(font, size, text + "…") > maxWidth) {
                text = text.substring(0, text.length() - 1);
            }
            return text.trim() + "…";
        }

        private static float width(PDFont font, float size, String value) throws IOException {
            return font.getStringWidth(value) / 1000 * size;
        }

        /** Cambia por «?» los caracteres que la fuente no tiene (emoji, por ejemplo). */
        private static String safe(PDFont font, String value) {
            StringBuilder out = new StringBuilder(value.length());
            value.codePoints().forEach(cp -> {
                String ch = new String(Character.toChars(cp));
                if (cp == '\n' || cp == '\r' || cp == '\t') {
                    out.append(' ');
                    return;
                }
                try {
                    font.encode(ch);
                    out.append(ch);
                } catch (IOException | IllegalArgumentException e) {
                    out.append('?');
                }
            });
            return out.toString();
        }
    }

    // ---------- ayudas ----------

    private static PDFont font(PDDocument document, String file) throws IOException {
        try (InputStream in = InvoicePdfService.class.getResourceAsStream("/css/fonts/" + file)) {
            if (in == null) {
                throw new IOException("No se encontró la fuente " + file);
            }
            return PDType0Font.load(document, in);
        }
    }

    private static PDImageXObject image(PDDocument document, String resource) throws IOException {
        try (InputStream in = InvoicePdfService.class.getResourceAsStream(resource)) {
            return in == null ? null : PDImageXObject.createFromByteArray(document, in.readAllBytes(), "logo");
        }
    }

    private static void addIfPresent(List<String> list, String value) {
        if (value != null && !value.isBlank()) {
            list.add(value.trim());
        }
    }

    private static String documentText(Customer customer) {
        if (customer.getDocumentType() == null || customer.getDocumentNumber() == null) {
            return null;
        }
        String number = customer.getDocumentNumber();
        if (number.matches("\\d{1,15}")) {
            number = String.format(java.util.Locale.US, "%,d", Long.parseLong(number)).replace(',', '.');
        }
        return customer.getDocumentType().name() + " " + number;
    }

    /** Celular de 10 cifras como en la app: {@code 3005551234} pasa a {@code 300 555 1234}. */
    private static String phoneText(String phone) {
        if (phone != null && phone.matches("\\d{10}")) {
            return phone.substring(0, 3) + " " + phone.substring(3, 6) + " " + phone.substring(6);
        }
        return phone;
    }

    private static String statusText(InvoiceStatus status) {
        return switch (status) {
            case PENDING -> "Pendiente";
            case PAID -> "Pagada";
            case OVERDUE -> "Vencida";
            case VOIDED -> "Anulada";
        };
    }

    private static Color statusColor(InvoiceStatus status) {
        return switch (status) {
            case PAID -> SUCCESS;
            case PENDING -> WARNING;
            case OVERDUE, VOIDED -> DANGER;
        };
    }

    private static String taxRateText() {
        return InvoiceService.TAX_RATE.movePointRight(2).stripTrailingZeros().toPlainString() + " %";
    }
}
