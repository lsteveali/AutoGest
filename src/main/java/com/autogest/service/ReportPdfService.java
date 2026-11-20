package com.autogest.service;

import com.autogest.exception.AutoGestException;
import com.autogest.model.MovementType;
import com.autogest.model.OrderStatus;
import com.autogest.model.ReportData;
import com.autogest.util.Formatter;
import org.apache.pdfbox.pdmodel.PDDocument;
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
import java.util.Locale;
import java.util.Map;

/**
 * Genera el PDF de un reporte (A4): encabezado con el periodo y una tabla por
 * sección. Las tablas conservan todos los valores de la pantalla; una sección
 * que no cabe pasa completa a la página siguiente.
 *
 * <p>Principles: Pure Fabrication, SRP.</p>
 */
public class ReportPdfService {

    private static final Color ONYX = new Color(0x11, 0x11, 0x11);
    private static final Color TEXT = new Color(0x11, 0x11, 0x11);
    private static final Color MUTED = new Color(0x6B, 0x64, 0x55);
    private static final Color BORDER = new Color(0xDA, 0xD4, 0xC5);
    private static final Color BAND = new Color(0xF4, 0xF0, 0xE6);

    private static final float MARGIN = 48;
    private static final float WIDTH = PDRectangle.A4.getWidth() - 2 * MARGIN;
    private static final float TOP = PDRectangle.A4.getHeight() - MARGIN;
    /** Alto del logotipo en el encabezado, en puntos (igual que en la factura). */
    private static final float LOGO_HEIGHT = 42;
    private static final float BOTTOM = MARGIN + 34;
    private static final float ROW_HEIGHT = 19;

    /**
     * Genera el PDF y lo guarda.
     *
     * @param report reporte
     * @param target archivo de destino (se reemplaza si existe)
     * @throws AutoGestException si no se puede generar o escribir
     */
    public void export(ReportData.Snapshot report, Path target) {
        byte[] pdf = render(report);
        try {
            Files.write(target, pdf);
        } catch (IOException e) {
            throw new AutoGestException("No se pudo guardar el PDF en " + target.getFileName()
                    + ". Revisa que el archivo no esté abierto en otro programa.", e);
        }
    }

    /** Nombre sugerido: {@code Reporte 2026-11-01 a 2026-11-30.pdf}. */
    public static String fileName(ReportData.Snapshot report) {
        return "Reporte " + report.from() + " a " + report.to() + ".pdf";
    }

    /**
     * Genera el PDF en memoria.
     *
     * @param report reporte
     * @return bytes del PDF
     */
    public byte[] render(ReportData.Snapshot report) {
        try (PDDocument document = new PDDocument()) {
            new Writer(document).write(report);
            document.getDocumentInformation().setTitle("Reporte AutoGest");
            document.getDocumentInformation().setCreator("AutoGest");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new AutoGestException("No se pudo generar el PDF del reporte.", e);
        }
    }

    /** Escribe las secciones y lleva la página y la posición actuales. */
    private static final class Writer {

        private final PDDocument document;
        private final PDFont regular;
        private final PDFont bold;
        private PDPageContentStream stream;
        private float y;

        Writer(PDDocument document) throws IOException {
            this.document = document;
            this.regular = font("Roboto-Regular.ttf");
            this.bold = font("Roboto-Bold.ttf");
        }

        private PDFont font(String file) throws IOException {
            try (InputStream in = ReportPdfService.class.getResourceAsStream("/css/fonts/" + file)) {
                if (in == null) {
                    throw new IOException("No se encontró la fuente " + file);
                }
                return PDType0Font.load(document, in);
            }
        }

        void write(ReportData.Snapshot r) throws IOException {
            newPage();
            header(r);
            ReportData.Kpis now = r.current();
            ReportData.Kpis before = r.previous();
            section("Indicadores del periodo", List.of(
                    row("Ingresos", Formatter.money(now.revenue()),
                            ReportService.changeText(now.revenue(), before.revenue())),
                    row("Órdenes completadas", now.completedOrders(), now.completedOrders(), before.completedOrders()),
                    row("Servicios realizados", now.servicesPerformed(), now.servicesPerformed(),
                            before.servicesPerformed()),
                    row("Clientes atendidos", now.customersServed(), now.customersServed(), before.customersServed())));
            section("Indicadores del taller", List.of(
                    row("Tiempo promedio de reparación", repairDays(r.averageRepairDays()), ""),
                    row("Ticket promedio", Formatter.money(now.averageTicket()), ""),
                    row("Clientes recurrentes", r.recurringRate() + " %", ""),
                    row("Órdenes canceladas", now.cancelledRate() + " %", "")));
            ReportData.Billing billing = r.billing();
            section("Facturación", List.of(row("Facturado", Formatter.money(billing.billed()), ""),
                    row("Pagado", Formatter.money(billing.paid()), ""),
                    row("Pendiente", Formatter.money(billing.pending()), ""),
                    row("Vencido", Formatter.money(billing.overdue()), "")));
            List<String[]> status = new ArrayList<>();
            for (OrderStatus value : OrderStatus.values()) {
                status.add(row(statusText(value), String.valueOf(r.orderStatus().getOrDefault(value, 0)), ""));
            }
            section("Estado actual de las órdenes", status);
            section("Servicios más realizados", counts(r.topServices()));
            List<String[]> byService = new ArrayList<>();
            r.revenueByService().forEach((name, value) -> byService.add(row(name, Formatter.money(value), "")));
            section("Ingresos por servicio", byService);
            section("Repuestos más usados", counts(r.topParts()));
            section("Órdenes por mecánico", counts(r.ordersByMechanic()));
            ReportData.Inventory inventory = r.inventory();
            List<String[]> stock = new ArrayList<>(List.of(row("Repuestos en catálogo",
                            String.valueOf(inventory.parts()), ""),
                    row("Stock bajo", String.valueOf(inventory.lowStock()), ""),
                    row("Agotados", String.valueOf(inventory.outOfStock()), ""),
                    row("Valor del inventario", Formatter.money(inventory.value()), "")));
            for (MovementType type : MovementType.values()) {
                stock.add(row(movementText(type), r.movements().getOrDefault(type, 0) + " unidades", ""));
            }
            section("Inventario", stock);
            stream.close();
            footers();
        }

        private void header(ReportData.Snapshot r) throws IOException {
            // Logotipo completo a la izquierda; título y periodo a la derecha, como en la factura.
            try (InputStream in = ReportPdfService.class.getResourceAsStream(InvoicePdfService.LOGO)) {
                if (in != null) {
                    PDImageXObject logo = PDImageXObject.createFromByteArray(document, in.readAllBytes(), "logo");
                    float width = LOGO_HEIGHT * logo.getWidth() / logo.getHeight();
                    stream.drawImage(logo, MARGIN, TOP - LOGO_HEIGHT, width, LOGO_HEIGHT);
                } else {
                    text(bold, 20, ONYX, MARGIN, TOP - 22, "AutoGest");
                }
            }
            textRight(bold, 18, ONYX, MARGIN + WIDTH, TOP - 18, "Reporte del taller");
            textRight(regular, 10, MUTED, MARGIN + WIDTH, TOP - 34,
                    "Periodo: " + Formatter.date(r.from()) + " a " + Formatter.date(r.to()));
            y = TOP - LOGO_HEIGHT - 16;
            line(y);
            y -= 22;
        }

        /** Título y filas; la sección completa pasa a otra página si no cabe. */
        private void section(String title, List<String[]> rows) throws IOException {
            List<String[]> shown = rows.isEmpty() ? List.<String[]>of(row("Sin datos en este periodo", "", "")) : rows;
            if (y - 26 - shown.size() * ROW_HEIGHT < BOTTOM) {
                stream.close();
                newPage();
                y = TOP;
            }
            stream.setNonStrokingColor(BAND);
            stream.addRect(MARGIN, y - 18, WIDTH, 20);
            stream.fill();
            text(bold, 10, ONYX, MARGIN + 8, y - 12, title);
            y -= 26;
            for (String[] row : shown) {
                text(regular, 10, TEXT, MARGIN + 8, y - 11, row[0]);
                textRight(bold, 10, TEXT, MARGIN + WIDTH - 170, y - 11, row[1]);
                textRight(regular, 9, MUTED, MARGIN + WIDTH - 8, y - 11, row[2]);
                y -= ROW_HEIGHT;
                line(y);
            }
            y -= 16;
        }

        private void footers() throws IOException {
            int pages = document.getNumberOfPages();
            String generated = "Generado con AutoGest el " + Formatter.timestamp(LocalDateTime.now());
            for (int i = 0; i < pages; i++) {
                try (PDPageContentStream footer = new PDPageContentStream(document, document.getPage(i),
                        PDPageContentStream.AppendMode.APPEND, true)) {
                    stream = footer;
                    text(regular, 8, MUTED, MARGIN, MARGIN + 6, generated);
                    textRight(regular, 8, MUTED, MARGIN + WIDTH, MARGIN + 6, "Página " + (i + 1) + " de " + pages);
                }
            }
        }

        private void newPage() throws IOException {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
        }

        private void text(PDFont font, float size, Color color, float x, float baseline, String value)
                throws IOException {
            if (value == null || value.isEmpty()) {
                return;
            }
            stream.beginText();
            stream.setFont(font, size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(safe(font, value));
            stream.endText();
        }

        private void textRight(PDFont font, float size, Color color, float right, float baseline, String value)
                throws IOException {
            if (value == null || value.isEmpty()) {
                return;
            }
            String clean = safe(font, value);
            text(font, size, color, right - font.getStringWidth(clean) / 1000 * size, baseline, clean);
        }

        private void line(float lineY) throws IOException {
            stream.setStrokingColor(BORDER);
            stream.setLineWidth(0.6f);
            stream.moveTo(MARGIN, lineY);
            stream.lineTo(MARGIN + WIDTH, lineY);
            stream.stroke();
        }

        /** Cambia por «?» lo que la fuente no tiene (emoji en un nombre, por ejemplo). */
        private static String safe(PDFont font, String value) {
            StringBuilder out = new StringBuilder(value.length());
            value.codePoints().forEach(cp -> {
                String ch = new String(Character.toChars(cp));
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

    // ---------- textos ----------

    private static String[] row(String name, String value, String detail) {
        return new String[]{name, value, detail};
    }

    private static String[] row(String name, int value, int current, int previous) {
        return row(name, String.valueOf(value),
                ReportService.changeText(BigDecimal.valueOf(current), BigDecimal.valueOf(previous)));
    }

    private static List<String[]> counts(Map<String, Integer> values) {
        List<String[]> rows = new ArrayList<>();
        values.forEach((name, value) -> rows.add(row(name, String.valueOf(value), "")));
        return rows;
    }

    private static String repairDays(Double days) {
        return days == null ? "Sin datos" : String.format(Locale.forLanguageTag("es-CO"), "%.1f días", days);
    }

    private static String statusText(OrderStatus status) {
        return switch (status) {
            case RECEIVED -> "Recibido";
            case DIAGNOSIS -> "Diagnóstico";
            case IN_REPAIR -> "En reparación";
            case READY -> "Listo";
            case DELIVERED -> "Entregado";
            case CANCELLED -> "Cancelado";
        };
    }

    private static String movementText(MovementType type) {
        return switch (type) {
            case ENTRY -> "Entradas";
            case EXIT -> "Salidas";
            case ADJUSTMENT -> "Ajustes";
        };
    }
}
