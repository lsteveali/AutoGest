package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.ReportData;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Consultas agregadas de la pantalla Reportes que cruzan facturas, órdenes e
 * historial. Solo lee: no tiene ninguna escritura. Las consultas de un solo
 * módulo están en los DAO de ese módulo (Dev 1, {@code docs/reports-queries.md}).
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion.</p>
 */
public class ReportDao {

    /** Texto con el que {@code ServiceOrder} registra el paso a Lista en el historial. */
    private static final String READY_HISTORY = "% a Listo";

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public ReportDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /**
     * Indicadores del periodo en una sola consulta.
     *
     * @param from primer día
     * @param to   último día
     * @return indicadores
     */
    public ReportData.Kpis kpis(LocalDate from, LocalDate to) {
        String sql = "SELECT"
                + " (SELECT COALESCE(SUM(total), 0) FROM invoices"
                + "   WHERE status <> 'VOIDED' AND issue_date BETWEEN ? AND ?),"
                + " (SELECT COUNT(*) FROM invoices WHERE status <> 'VOIDED' AND issue_date BETWEEN ? AND ?),"
                + " (SELECT COALESCE(SUM(l.quantity), 0) FROM order_lines l"
                + "   JOIN service_orders lo ON lo.id = l.order_id"
                + "   WHERE l.line_type = 'SERVICE' AND lo.status <> 'CANCELLED' AND lo.entry_date BETWEEN ? AND ?),"
                + " COUNT(*),"
                + " COUNT(*) FILTER (WHERE o.status IN ('READY', 'DELIVERED')),"
                + " COUNT(*) FILTER (WHERE o.status = 'CANCELLED'),"
                + " COUNT(DISTINCT v.customer_id) FILTER (WHERE o.status <> 'CANCELLED')"
                + " FROM service_orders o JOIN vehicles v ON v.id = o.vehicle_id"
                + " WHERE o.entry_date BETWEEN ? AND ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < 4; i++) {
                ps.setDate(i * 2 + 1, Date.valueOf(from));
                ps.setDate(i * 2 + 2, Date.valueOf(to));
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new ReportData.Kpis(rs.getBigDecimal(1), rs.getInt(2), rs.getInt(4), rs.getInt(5),
                        rs.getInt(6), rs.getInt(3), rs.getInt(7));
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Facturación del periodo por estado de cobro.
     *
     * @param from primer día
     * @param to   último día
     * @return facturado, pagado, pendiente y vencido
     */
    public ReportData.Billing billing(LocalDate from, LocalDate to) {
        String sql = "SELECT COALESCE(SUM(total) FILTER (WHERE status <> 'VOIDED'), 0),"
                + " COALESCE(SUM(total) FILTER (WHERE status = 'PAID'), 0),"
                + " COALESCE(SUM(total) FILTER (WHERE status = 'PENDING'), 0),"
                + " COALESCE(SUM(total) FILTER (WHERE status = 'OVERDUE'), 0)"
                + " FROM invoices WHERE issue_date BETWEEN ? AND ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new ReportData.Billing(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3),
                        rs.getBigDecimal(4));
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Valor facturado por día de emisión; los días sin facturas no aparecen.
     *
     * @param from primer día
     * @param to   último día
     * @return día → valor, en orden de fecha
     */
    public Map<LocalDate, BigDecimal> revenueByDay(LocalDate from, LocalDate to) {
        String sql = "SELECT issue_date, SUM(total) FROM invoices"
                + " WHERE status <> 'VOIDED' AND issue_date BETWEEN ? AND ? GROUP BY issue_date ORDER BY issue_date";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                Map<LocalDate, BigDecimal> result = new LinkedHashMap<>();
                while (rs.next()) {
                    result.put(rs.getDate(1).toLocalDate(), rs.getBigDecimal(2));
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Valor de los servicios en órdenes no canceladas ingresadas en el periodo.
     *
     * @param from  primer día
     * @param to    último día
     * @param limit máximo de servicios
     * @return servicio → valor, de mayor a menor
     */
    public Map<String, BigDecimal> revenueByService(LocalDate from, LocalDate to, int limit) {
        String sql = "SELECT s.name, SUM(l.quantity * l.unit_price) FROM order_lines l"
                + " JOIN service_orders o ON o.id = l.order_id"
                + " JOIN service_offerings s ON s.id = l.service_offering_id"
                + " WHERE l.line_type = 'SERVICE' AND o.status <> 'CANCELLED' AND o.entry_date BETWEEN ? AND ?"
                + " GROUP BY s.name ORDER BY 2 DESC, 1 LIMIT ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, BigDecimal> result = new LinkedHashMap<>();
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getBigDecimal(2));
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Días promedio entre el ingreso y el primer paso a Lista, según el historial.
     *
     * @param from primer día
     * @param to   último día
     * @return promedio en días, o {@code null} si ninguna orden del periodo llegó a Lista
     */
    public Double averageRepairDays(LocalDate from, LocalDate to) {
        String sql = "SELECT AVG(GREATEST(EXTRACT(EPOCH FROM (h.ready_at - o.entry_date::timestamp)) / 86400, 0))"
                + " FROM service_orders o JOIN (SELECT order_id, MIN(timestamp) AS ready_at FROM order_history"
                + "   WHERE description LIKE ? GROUP BY order_id) h ON h.order_id = o.id"
                + " WHERE o.entry_date BETWEEN ? AND ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, READY_HISTORY);
            ps.setDate(2, Date.valueOf(from));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                double value = rs.getDouble(1);
                return rs.wasNull() ? null : value;
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    /**
     * Porcentaje de los clientes atendidos en el periodo que acumulan dos o más
     * órdenes no canceladas hasta el último día.
     *
     * @param from primer día
     * @param to   último día
     * @return porcentaje de 0 a 100
     */
    public int recurringRate(LocalDate from, LocalDate to) {
        String sql = "WITH served AS (SELECT DISTINCT v.customer_id FROM service_orders o"
                + "   JOIN vehicles v ON v.id = o.vehicle_id"
                + "   WHERE o.status <> 'CANCELLED' AND o.entry_date BETWEEN ? AND ?),"
                + " totals AS (SELECT v.customer_id, COUNT(*) AS orders FROM service_orders o"
                + "   JOIN vehicles v ON v.id = o.vehicle_id"
                + "   WHERE o.status <> 'CANCELLED' AND o.entry_date <= ? GROUP BY v.customer_id)"
                + " SELECT COUNT(*), COUNT(*) FILTER (WHERE t.orders >= 2)"
                + " FROM served s JOIN totals t ON t.customer_id = s.customer_id";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setDate(3, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int served = rs.getInt(1);
                return served == 0 ? 0 : Math.round(rs.getInt(2) * 100f / served);
            }
        } catch (SQLException e) {
            throw failure(e);
        }
    }

    private static DataAccessException failure(SQLException e) {
        return new DataAccessException("No se pudo generar el reporte.", e);
    }
}
