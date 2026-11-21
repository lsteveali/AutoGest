package com.autogest.model;

import com.autogest.model.Catalogs.MovementType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Datos de la pantalla Reportes (Sprint 7, Líder), agrupados en registros de
 * solo lectura. No son entidades: se calculan con consultas agregadas.
 *
 * <p>Principles: Information Expert (cada registro calcula sus promedios),
 * High Cohesion.</p>
 */
public final class ReportData {

    private ReportData() {
        // Solo agrupa los registros: no se instancia.
    }

    /**
     * Indicadores de un periodo.
     *
     * @param revenue           valor facturado (facturas no anuladas, por fecha de emisión)
     * @param invoices          facturas no anuladas
     * @param totalOrders       órdenes ingresadas
     * @param completedOrders   órdenes ingresadas que ya están Listas o Entregadas
     * @param cancelledOrders   órdenes ingresadas y canceladas
     * @param servicesPerformed unidades de servicio en órdenes no canceladas
     * @param customersServed   clientes distintos con órdenes no canceladas
     */
    public record Kpis(BigDecimal revenue, int invoices, int totalOrders, int completedOrders,
                       int cancelledOrders, int servicesPerformed, int customersServed) {

        /** Valor promedio por factura en pesos enteros (0 si no hay facturas). */
        public BigDecimal averageTicket() {
            return invoices == 0 ? BigDecimal.ZERO
                    : revenue.divide(BigDecimal.valueOf(invoices), 0, RoundingMode.HALF_UP);
        }

        /** Porcentaje de órdenes canceladas (0 si no hay órdenes). */
        public int cancelledRate() {
            return totalOrders == 0 ? 0 : Math.round(cancelledOrders * 100f / totalOrders);
        }
    }

    /**
     * Facturación del periodo por estado de cobro.
     *
     * @param billed  facturado (sin anuladas)
     * @param paid    pagado
     * @param pending pendiente, dentro del plazo
     * @param overdue vencido
     */
    public record Billing(BigDecimal billed, BigDecimal paid, BigDecimal pending, BigDecimal overdue) {
    }

    /**
     * Punto de la gráfica de ingresos.
     *
     * @param label  día ({@code 05/11}) o mes ({@code nov 2026})
     * @param amount valor facturado
     */
    public record Point(String label, BigDecimal amount) {
    }

    /**
     * Estado actual del inventario.
     *
     * @param parts      repuestos en catálogo
     * @param lowStock   con stock bajo
     * @param outOfStock agotados
     * @param value      valor del stock (existencias por precio unitario)
     */
    public record Inventory(int parts, int lowStock, int outOfStock, BigDecimal value) {
    }

    /**
     * Reporte completo de un periodo.
     *
     * @param from             primer día
     * @param to               último día
     * @param current          indicadores del periodo
     * @param previous         indicadores del periodo anterior de igual duración
     * @param revenue          ingresos por día, o por mes en periodos largos
     * @param orderStatus      órdenes por estado actual (todas)
     * @param topServices      servicios más realizados
     * @param revenueByService ingresos estimados por servicio
     * @param topParts         repuestos más usados
     * @param ordersByMechanic órdenes por mecánico
     * @param movements        unidades de inventario por tipo de movimiento
     * @param inventory        estado actual del inventario
     * @param billing          facturación del periodo
     * @param averageRepairDays días promedio hasta quedar Lista ({@code null} sin datos)
     * @param recurringRate    porcentaje de clientes atendidos con más de una orden
     * @param deliveryRate     porcentaje de notificaciones entregadas
     */
    public record Snapshot(LocalDate from, LocalDate to, Kpis current, Kpis previous, List<Point> revenue,
                           Map<OrderStatus, Integer> orderStatus, Map<String, Integer> topServices,
                           Map<String, BigDecimal> revenueByService, Map<String, Integer> topParts,
                           Map<String, Integer> ordersByMechanic, Map<MovementType, Integer> movements,
                           Inventory inventory, Billing billing, Double averageRepairDays,
                           int recurringRate, int deliveryRate) {
    }
}
