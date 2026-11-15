package com.autogest.model;

import java.math.BigDecimal;

/**
 * Indicadores de la pantalla de Facturación, calculados en una sola consulta.
 * Las facturas anuladas cuentan en {@code total} pero no suman dinero.
 *
 * @param total         facturas generadas (todas, incluidas las anuladas)
 * @param paid          facturas pagadas
 * @param pending       facturas pendientes (sin vencer)
 * @param overdue       facturas vencidas
 * @param billedAmount  valor facturado (sin las anuladas)
 * @param paidAmount    valor de las pagadas
 * @param pendingAmount valor por cobrar (pendientes y vencidas)
 */
public record InvoiceSummary(int total, int paid, int pending, int overdue,
                             BigDecimal billedAmount, BigDecimal paidAmount, BigDecimal pendingAmount) {
}
