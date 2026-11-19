package com.autogest.repository;

import com.autogest.dao.ReportDao;
import com.autogest.model.ReportData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Acceso a las consultas agregadas de Reportes; aísla al Service del SQL.
 *
 * <p>Principles: Indirection, Pure Fabrication, Low Coupling, DIP.</p>
 */
public class ReportRepository {

    private final ReportDao dao;

    /**
     * @param dao consultas agregadas
     */
    public ReportRepository(ReportDao dao) {
        this.dao = dao;
    }

    /** Indicadores del periodo. */
    public ReportData.Kpis kpis(LocalDate from, LocalDate to) {
        return dao.kpis(from, to);
    }

    /** Facturación del periodo por estado de cobro. */
    public ReportData.Billing billing(LocalDate from, LocalDate to) {
        return dao.billing(from, to);
    }

    /** Valor facturado por día de emisión. */
    public Map<LocalDate, BigDecimal> revenueByDay(LocalDate from, LocalDate to) {
        return dao.revenueByDay(from, to);
    }

    /** Valor de los servicios del periodo, de mayor a menor. */
    public Map<String, BigDecimal> revenueByService(LocalDate from, LocalDate to, int limit) {
        return dao.revenueByService(from, to, limit);
    }

    /** Días promedio hasta quedar Lista, o {@code null} sin datos. */
    public Double averageRepairDays(LocalDate from, LocalDate to) {
        return dao.averageRepairDays(from, to);
    }

    /** Porcentaje de clientes atendidos que acumulan dos o más órdenes. */
    public int recurringRate(LocalDate from, LocalDate to) {
        return dao.recurringRate(from, to);
    }
}
