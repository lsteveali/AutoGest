package com.autogest.service;

import com.autogest.model.Part;
import com.autogest.model.ReportData;
import com.autogest.repository.ReportRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Arma el reporte de un periodo (Sprint 7, Líder) con las consultas de
 * {@link ReportRepository} y las de apoyo de Dev 1 en órdenes, inventario y
 * notificaciones ({@code docs/reports-queries.md}).
 *
 * <p><b>Criterios:</b> los ingresos salen de las facturas no anuladas, por
 * fecha de emisión, porque {@code invoices} no guarda la fecha de pago. Cada
 * indicador se compara con el periodo anterior de igual duración. Los periodos
 * de más de {@value #MAX_DAILY_POINTS} días se grafican por mes.</p>
 *
 * <p>Solo lee: generar un reporte no modifica ningún dato.</p>
 *
 * <p>Principles: GRASP Controller (de caso de uso), Indirection, SRP, DIP.</p>
 */
public class ReportService {

    /** Periodos rápidos de la pantalla. */
    public enum Period {
        TODAY, THIS_WEEK, THIS_MONTH, LAST_3_MONTHS, THIS_YEAR;

        /** Primer día del periodo. */
        public LocalDate start(LocalDate today) {
            return switch (this) {
                case TODAY -> today;
                case THIS_WEEK -> today.with(DayOfWeek.MONDAY);
                case THIS_MONTH -> today.withDayOfMonth(1);
                case LAST_3_MONTHS -> today.minusMonths(2).withDayOfMonth(1);
                case THIS_YEAR -> today.withDayOfYear(1);
            };
        }
    }

    /** Días máximos que se grafican uno por uno. */
    static final int MAX_DAILY_POINTS = 62;

    /** Filas de cada ranking. */
    static final int TOP_LIMIT = 5;

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MONTH_LABEL =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("es-CO"));

    private final ReportRepository repository;
    private final ServiceOrderService orderService;
    private final InventoryService inventoryService;
    private final PartService partService;
    private final NotificationService notificationService;

    /**
     * @param repository          consultas agregadas de Reportes
     * @param orderService        consultas de órdenes (Dev 1)
     * @param inventoryService    movimientos de inventario (Dev 1)
     * @param partService         estado de los repuestos
     * @param notificationService tasa de entrega de notificaciones (Dev 1)
     */
    public ReportService(ReportRepository repository, ServiceOrderService orderService,
                         InventoryService inventoryService, PartService partService,
                         NotificationService notificationService) {
        this.repository = repository;
        this.orderService = orderService;
        this.inventoryService = inventoryService;
        this.partService = partService;
        this.notificationService = notificationService;
    }

    /**
     * Genera el reporte del periodo.
     *
     * @param from primer día
     * @param to   último día
     * @return reporte completo
     * @throws com.autogest.exception.ValidationException si faltan las fechas o la inicial es posterior
     */
    public ReportData.Snapshot build(LocalDate from, LocalDate to) {
        ServiceOrderService.validatePeriod(from, to);
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate previousTo = from.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        return new ReportData.Snapshot(from, to,
                repository.kpis(from, to),
                repository.kpis(previousFrom, previousTo),
                revenueSeries(from, to, repository.revenueByDay(from, to)),
                orderService.countGroupedByStatus(),
                orderService.findTopServices(from, to, TOP_LIMIT),
                repository.revenueByService(from, to, TOP_LIMIT),
                orderService.findTopParts(from, to, TOP_LIMIT),
                orderService.countOrdersByMechanic(from, to),
                inventoryService.summarizeMovements(from, to),
                inventory(),
                repository.billing(from, to),
                repository.averageRepairDays(from, to),
                repository.recurringRate(from, to),
                notificationService.deliveryRate());
    }

    /**
     * Variación porcentual frente al periodo anterior.
     *
     * @param current  valor actual
     * @param previous valor anterior
     * @return porcentaje redondeado, o {@code null} si el anterior es 0 y no se puede comparar
     */
    public static Integer changePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 0, RoundingMode.HALF_UP).intValue();
    }

    /**
     * Texto de la comparación con el periodo anterior (pantalla y PDF).
     *
     * @param current  valor actual
     * @param previous valor anterior
     * @return por ejemplo «+14 % vs. periodo anterior»
     */
    public static String changeText(BigDecimal current, BigDecimal previous) {
        Integer percent = changePercent(current, previous);
        if (percent == null) {
            return current.signum() == 0 ? "Sin movimiento" : "Sin datos del periodo anterior";
        }
        if (percent == 0) {
            return "Igual al periodo anterior";
        }
        return (percent > 0 ? "+" : "") + percent + " % vs. periodo anterior";
    }

    /**
     * Completa la serie de ingresos: incluye los días sin facturas (en 0) y
     * agrupa por mes los periodos largos.
     */
    static List<ReportData.Point> revenueSeries(LocalDate from, LocalDate to, Map<LocalDate, BigDecimal> byDay) {
        List<ReportData.Point> points = new ArrayList<>();
        if (ChronoUnit.DAYS.between(from, to) + 1 <= MAX_DAILY_POINTS) {
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                points.add(new ReportData.Point(day.format(DAY_LABEL), byDay.getOrDefault(day, BigDecimal.ZERO)));
            }
            return points;
        }
        Map<YearMonth, BigDecimal> byMonth = new LinkedHashMap<>();
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            byMonth.put(month, BigDecimal.ZERO);
        }
        byDay.forEach((day, amount) -> byMonth.merge(YearMonth.from(day), amount, BigDecimal::add));
        byMonth.forEach((month, amount) -> points.add(new ReportData.Point(month.format(MONTH_LABEL), amount)));
        return points;
    }

    private ReportData.Inventory inventory() {
        BigDecimal value = BigDecimal.ZERO;
        for (Part part : partService.findAll()) {
            value = value.add(part.getUnitPrice().multiply(BigDecimal.valueOf(part.getCurrentStock())));
        }
        return new ReportData.Inventory(partService.countParts(), partService.countLowStock(),
                partService.countOutOfStock(), value);
    }
}
