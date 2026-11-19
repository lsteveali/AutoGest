package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Part;
import com.autogest.model.ReportData;
import com.autogest.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de {@link ReportService} con un repositorio falso de consultas y los
 * servicios reales de órdenes, inventario y notificaciones sobre repositorios falsos.
 */
class ReportServiceTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    private RecordingReportRepository reports;
    private FakePartRepository parts;
    private ReportService service;

    @BeforeEach
    void setUp() {
        reports = new RecordingReportRepository();
        parts = new FakePartRepository();
        PartService partService = new PartService(parts);
        service = new ReportService(reports, new ServiceOrderService(new FakeServiceOrderRepository(), null),
                new InventoryService(new FakeInventoryMovementRepository(), partService), partService,
                new NotificationService(new FakeNotificationRepository(),
                        new CustomerService(new FakeCustomerRepository())));
    }

    // ---------- periodo ----------

    @Test
    void comparesWithThePreviousPeriodOfTheSameLength() {
        ReportData.Snapshot report = service.build(NOV_1, NOV_30);

        assertEquals(List.of(NOV_1 + ".." + NOV_30, LocalDate.of(2026, 10, 2) + ".." + LocalDate.of(2026, 10, 31)),
                reports.kpiPeriods);
        assertEquals(NOV_1, report.from());
        assertEquals(new BigDecimal("500000"), report.current().revenue());
        assertEquals(new BigDecimal("500000"), report.previous().revenue());
    }

    @Test
    void oneDayPeriodComparesWithTheDayBefore() {
        service.build(NOV_30, NOV_30);

        assertEquals(LocalDate.of(2026, 11, 29) + ".." + LocalDate.of(2026, 11, 29), reports.kpiPeriods.get(1));
    }

    @Test
    void periodIsValidated() {
        assertThrows(ValidationException.class, () -> service.build(null, NOV_30));
        assertThrows(ValidationException.class, () -> service.build(NOV_30, NOV_1));
    }

    @Test
    void quickPeriodsStartOnTheRightDay() {
        LocalDate wednesday = LocalDate.of(2026, 11, 18);

        assertEquals(wednesday, ReportService.Period.TODAY.start(wednesday));
        assertEquals(LocalDate.of(2026, 11, 16), ReportService.Period.THIS_WEEK.start(wednesday));
        assertEquals(NOV_1, ReportService.Period.THIS_MONTH.start(wednesday));
        assertEquals(LocalDate.of(2026, 9, 1), ReportService.Period.LAST_3_MONTHS.start(wednesday));
        assertEquals(LocalDate.of(2026, 1, 1), ReportService.Period.THIS_YEAR.start(wednesday));
    }

    // ---------- serie de ingresos ----------

    @Test
    void shortPeriodsHaveOnePointPerDayIncludingDaysWithoutInvoices() {
        Map<LocalDate, BigDecimal> byDay = Map.of(LocalDate.of(2026, 11, 2), new BigDecimal("100"));

        List<ReportData.Point> points = ReportService.revenueSeries(NOV_1, LocalDate.of(2026, 11, 3), byDay);

        assertEquals(List.of("01/11", "02/11", "03/11"), points.stream().map(ReportData.Point::label).toList());
        assertEquals(BigDecimal.ZERO, points.get(0).amount());
        assertEquals(new BigDecimal("100"), points.get(1).amount());
    }

    @Test
    void longPeriodsAreGroupedByMonth() {
        Map<LocalDate, BigDecimal> byDay = new LinkedHashMap<>();
        byDay.put(LocalDate.of(2026, 1, 5), new BigDecimal("100"));
        byDay.put(LocalDate.of(2026, 1, 20), new BigDecimal("50"));
        byDay.put(LocalDate.of(2026, 3, 1), new BigDecimal("70"));

        List<ReportData.Point> points = ReportService.revenueSeries(LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31), byDay);

        assertEquals(3, points.size(), "Enero, febrero (en 0) y marzo");
        assertEquals(new BigDecimal("150"), points.get(0).amount());
        assertEquals(BigDecimal.ZERO, points.get(1).amount());
        assertEquals(new BigDecimal("70"), points.get(2).amount());
    }

    // ---------- indicadores ----------

    @Test
    void changeIsComparedAgainstThePreviousValue() {
        assertEquals(25, ReportService.changePercent(new BigDecimal("125"), new BigDecimal("100")));
        assertEquals(-50, ReportService.changePercent(new BigDecimal("50"), new BigDecimal("100")));
        assertEquals(0, ReportService.changePercent(BigDecimal.TEN, BigDecimal.TEN));
        assertNull(ReportService.changePercent(BigDecimal.TEN, BigDecimal.ZERO), "Sin base no hay comparación");
    }

    @Test
    void kpisCalculateAverageTicketAndCancelledRate() {
        ReportData.Kpis kpis = new ReportData.Kpis(new BigDecimal("300000"), 4, 8, 5, 2, 10, 6);

        assertEquals(new BigDecimal("75000"), kpis.averageTicket());
        assertEquals(25, kpis.cancelledRate());
    }

    @Test
    void averageTicketIsRoundedToWholePesos() {
        ReportData.Kpis kpis = new ReportData.Kpis(new BigDecimal("4105000"), 14, 1, 0, 0, 0, 0);

        assertEquals(new BigDecimal("293214"), kpis.averageTicket());
    }

    @Test
    void changeTextDescribesTheComparison() {
        assertEquals("+14 % vs. periodo anterior", ReportService.changeText(new BigDecimal("114"), new BigDecimal("100")));
        assertEquals("-8 % vs. periodo anterior", ReportService.changeText(new BigDecimal("92"), new BigDecimal("100")));
        assertEquals("Igual al periodo anterior", ReportService.changeText(BigDecimal.TEN, BigDecimal.TEN));
        assertEquals("Sin datos del periodo anterior", ReportService.changeText(BigDecimal.TEN, BigDecimal.ZERO));
        assertEquals("Sin movimiento", ReportService.changeText(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void emptyPeriodDoesNotDivideByZero() {
        ReportData.Kpis empty = new ReportData.Kpis(BigDecimal.ZERO, 0, 0, 0, 0, 0, 0);

        assertEquals(BigDecimal.ZERO, empty.averageTicket());
        assertEquals(0, empty.cancelledRate());
    }

    @Test
    void inventoryValueAddsStockTimesPrice() {
        parts.create(part("REP-001", 3, "10000"));
        parts.create(part("REP-002", 2, "5000"));

        ReportData.Inventory inventory = service.build(NOV_1, NOV_30).inventory();

        assertEquals(2, inventory.parts());
        assertEquals(0, new BigDecimal("40000").compareTo(inventory.value()));
    }

    private static Part part(String code, int stock, String price) {
        Part part = new Part();
        part.setCode(code);
        part.setName("Repuesto " + code);
        part.setCurrentStock(stock);
        part.setMinimumStock(1);
        part.setUnitPrice(new BigDecimal(price));
        return part;
    }

    /** Consultas agregadas fijas que además recuerdan los periodos pedidos. */
    private static final class RecordingReportRepository extends ReportRepository {

        final List<String> kpiPeriods = new ArrayList<>();

        RecordingReportRepository() {
            super(null);
        }

        @Override
        public ReportData.Kpis kpis(LocalDate from, LocalDate to) {
            kpiPeriods.add(from + ".." + to);
            return new ReportData.Kpis(new BigDecimal("500000"), 2, 4, 2, 1, 6, 3);
        }

        @Override
        public ReportData.Billing billing(LocalDate from, LocalDate to) {
            return new ReportData.Billing(BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);
        }

        @Override
        public Map<LocalDate, BigDecimal> revenueByDay(LocalDate from, LocalDate to) {
            return Map.of();
        }

        @Override
        public Map<String, BigDecimal> revenueByService(LocalDate from, LocalDate to, int limit) {
            return Map.of();
        }

        @Override
        public Double averageRepairDays(LocalDate from, LocalDate to) {
            return null;
        }

        @Override
        public int recurringRate(LocalDate from, LocalDate to) {
            return 0;
        }
    }
}
