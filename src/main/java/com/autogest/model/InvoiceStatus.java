package com.autogest.model;

/**
 * Estado de una factura.
 *
 * <p>Se guarda en la base de datos como texto con el {@code name()} de la
 * constante. El texto que ve el usuario en pantalla (en español) no vive en el
 * modelo.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public enum InvoiceStatus {

    /** Emitida y aún sin pagar. */
    PENDING,
    /** Pagada. */
    PAID,
    /** Vencida sin pago. */
    OVERDUE,
    /** Anulada (baja lógica). */
    VOIDED
}