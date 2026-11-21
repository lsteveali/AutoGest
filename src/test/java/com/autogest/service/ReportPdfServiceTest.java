package com.autogest.service;

import com.autogest.model.Catalogs.MovementType;
import com.autogest.model.OrderStatus;
import com.autogest.model.ReportData;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link ReportPdfService}: se lee el texto del PDF generado. */
class ReportPdfServiceTest {

    private final ReportPdfService service = new ReportPdfService();

    @Test
    void pdfHasPeriodIndicatorsAndRankings() throws IOException {
        String text = text(service.render(report(Map.of("Cambio de aceite", 7))));

        assertTrue(text.contains("Periodo: 01/11/2026 a 30/11/2026"), text);
        assertTrue(text.contains("$240.000"), text);
        assertTrue(text.contains("+20 % vs. periodo anterior"), text);
        assertTrue(text.contains("Cambio de aceite"), text);
        assertTrue(text.contains("En reparación"), text);
        assertTrue(text.contains("2,5 días"), text);
        assertTrue(text.contains("Entradas"), text);
    }

    @Test
    void emptySectionsSayThereIsNoData() throws IOException {
        assertTrue(text(service.render(report(Map.of()))).contains("Sin datos en este periodo"));
    }

    @Test
    void longReportsContinueOnAnotherPage() throws IOException {
        Map<String, Integer> many = new LinkedHashMap<>();
        for (int i = 1; i <= 45; i++) {
            many.put("Servicio " + i, i);
        }

        try (PDDocument document = Loader.loadPDF(service.render(report(many)))) {
            assertTrue(document.getNumberOfPages() > 1);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Página 2 de " + document.getNumberOfPages()), text);
        }
    }

    @Test
    void exportWritesTheFile(@TempDir Path folder) throws IOException {
        ReportData.Snapshot report = report(Map.of());
        Path target = folder.resolve(ReportPdfService.fileName(report));

        service.export(report, target);

        assertEquals("Reporte 2026-11-01 a 2026-11-30.pdf", target.getFileName().toString());
        assertTrue(Files.size(target) > 1000);
    }

    @Test
    void headerShowsTheWorkshopNameAndNit() throws IOException {
        com.autogest.model.WorkshopProfile profile = new com.autogest.model.WorkshopProfile();
        profile.setName("Taller El Bulldog");
        profile.setNit("901.456.789-1");
        ReportPdfService withWorkshop = new ReportPdfService();
        withWorkshop.setWorkshop(() -> profile);

        assertTrue(text(withWorkshop.render(report(Map.of()))).contains("Taller El Bulldog · NIT 901.456.789-1"));
        assertFalse(text(service.render(report(Map.of()))).contains("NIT"));
    }

    private static String text(byte[] pdf) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static ReportData.Snapshot report(Map<String, Integer> topServices) {
        ReportData.Kpis now = new ReportData.Kpis(new BigDecimal("240000"), 2, 4, 3, 1, 6, 3);
        ReportData.Kpis before = new ReportData.Kpis(new BigDecimal("200000"), 1, 2, 1, 0, 2, 1);
        return new ReportData.Snapshot(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), now, before,
                List.of(new ReportData.Point("01/11", BigDecimal.TEN)),
                Map.of(OrderStatus.IN_REPAIR, 2), topServices, Map.of(), Map.of(), Map.of(),
                Map.of(MovementType.ENTRY, 8),
                new ReportData.Inventory(4, 1, 0, new BigDecimal("500000")),
                new ReportData.Billing(new BigDecimal("240000"), new BigDecimal("100000"),
                        new BigDecimal("140000"), BigDecimal.ZERO),
                2.5, 50, 100);
    }
}
