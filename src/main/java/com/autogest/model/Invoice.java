package com.autogest.model;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Documento de cobro generado a partir de una orden de servicio.
 *
 * <p>Calcula su propio total (subtotal + impuestos - descuento), sabe si está
 * vencida y controla sus cambios de estado: pagar o anular. No se borra: se
 * anula (baja lógica). Los importes son {@link BigDecimal} con escala 2 y
 * redondeo {@code HALF_UP}. Mientras {@code PaymentMethod} siga pendiente, el
 * método de pago es un {@code String}.</p>
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class Invoice {

    private int id;
    private String number;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal taxes;
    private BigDecimal discount;
    private BigDecimal total;
    private InvoiceStatus status;
    private String paymentMethod;
    private ServiceOrder order;

    /**
     * Crea una factura vacía, con {@code id} 0, estado {@link InvoiceStatus#PENDING} e importes en cero.
     */
    public Invoice() {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        this.subtotal = zero;
        this.taxes = zero;
        this.discount = zero;
        this.total = zero;
        this.status = InvoiceStatus.PENDING;
    }

    /**
     * Calcula el total: subtotal más impuestos menos descuento. Solo calcula y
     * devuelve; quien guarda el valor usa {@link #setTotal(BigDecimal)}.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return total con escala 2
     * @throws ValidationException si el descuento es mayor que el subtotal más los impuestos
     */
    public BigDecimal calculateTotal() {
        BigDecimal result = subtotal.add(taxes).subtract(discount);
        if (result.signum() < 0) {
            throw new ValidationException("El descuento no puede ser mayor que el subtotal más los impuestos");
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Marca la factura como pagada y guarda el método de pago. Se puede pagar
     * desde {@code PENDING} u {@code OVERDUE}.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param method método de pago (texto mientras {@code PaymentMethod} esté pendiente)
     * @throws ValidationException si ya está pagada o anulada, o si el método es nulo o vacío
     */
    public void markAsPaid(String method) {
        if (status == InvoiceStatus.PAID) {
            throw new ValidationException("La factura ya está pagada");
        }
        if (status == InvoiceStatus.VOIDED) {
            throw new ValidationException("No se puede pagar una factura anulada");
        }
        if (method == null || method.isBlank()) {
            throw new ValidationException("El método de pago es obligatorio");
        }
        this.paymentMethod = method.trim();
        this.status = InvoiceStatus.PAID;
    }

    /**
     * Anula la factura (baja lógica). Se puede anular desde {@code PENDING} u
     * {@code OVERDUE}.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @throws ValidationException si ya está pagada o ya está anulada
     */
    public void voidInvoice() {
        if (status == InvoiceStatus.PAID) {
            throw new ValidationException("No se puede anular una factura pagada");
        }
        if (status == InvoiceStatus.VOIDED) {
            throw new ValidationException("La factura ya está anulada");
        }
        this.status = InvoiceStatus.VOIDED;
    }

    /**
     * Indica si la factura está vencida usando la fecha actual.
     *
     * @return {@code true} si está pendiente y su fecha de vencimiento ya pasó
     * @see #isOverdue(LocalDate)
     */
    public boolean isOverdue() {
        return isOverdue(LocalDate.now());
    }

    /**
     * Indica si la factura está pendiente y su fecha de vencimiento es anterior a
     * la fecha dada (si vence ese mismo día, todavía no está vencida). Detecta las
     * pendientes que deben pasar a vencidas; una factura ya marcada {@code OVERDUE},
     * pagada o anulada devuelve {@code false}.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param today fecha de referencia (se recibe para poder probar con fechas fijas)
     * @return {@code true} si hay que marcarla como vencida
     */
    public boolean isOverdue(LocalDate today) {
        return status == InvoiceStatus.PENDING && dueDate != null && dueDate.isBefore(today);
    }

    /** Valida un importe (no nulo, no negativo) y lo ajusta a escala 2 con redondeo HALF_UP. */
    private static BigDecimal toMoney(BigDecimal value, String requiredMessage, String negativeMessage) {
        if (value == null) {
            throw new ValidationException(requiredMessage);
        }
        if (value.signum() < 0) {
            throw new ValidationException(negativeMessage);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Asigna el subtotal, ajustado a escala 2 con redondeo {@code HALF_UP}.
     *
     * @param subtotal importe; no puede ser nulo ni negativo
     * @throws ValidationException si es nulo o negativo
     */
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = toMoney(subtotal, "El subtotal es obligatorio", "El subtotal no puede ser negativo");
    }

    /**
     * Asigna los impuestos, ajustado a escala 2 con redondeo {@code HALF_UP}.
     *
     * @param taxes importe; no puede ser nulo ni negativo
     * @throws ValidationException si es nulo o negativo
     */
    public void setTaxes(BigDecimal taxes) {
        this.taxes = toMoney(taxes, "Los impuestos son obligatorios", "Los impuestos no pueden ser negativos");
    }

    /**
     * Asigna el descuento, ajustado a escala 2 con redondeo {@code HALF_UP}.
     *
     * @param discount importe; no puede ser nulo ni negativo
     * @throws ValidationException si es nulo o negativo
     */
    public void setDiscount(BigDecimal discount) {
        this.discount = toMoney(discount, "El descuento es obligatorio", "El descuento no puede ser negativo");
    }

    /**
     * Asigna el total, ajustado a escala 2 con redondeo {@code HALF_UP}.
     *
     * @param total importe; no puede ser nulo ni negativo
     * @throws ValidationException si es nulo o negativo
     */
    public void setTotal(BigDecimal total) {
        this.total = toMoney(total, "El total es obligatorio", "El total no puede ser negativo");
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el número de factura, por ejemplo FAC-00129. */
    public String getNumber() {
        return number;
    }

    /** Asigna el número de factura, por ejemplo FAC-00129. */
    public void setNumber(String number) {
        this.number = number;
    }

    /** Devuelve la fecha de emisión. */
    public LocalDate getIssueDate() {
        return issueDate;
    }

    /** Asigna la fecha de emisión. */
    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    /** Devuelve la fecha límite de pago. */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /** Asigna la fecha límite de pago. */
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /** Devuelve el subtotal (escala 2). */
    public BigDecimal getSubtotal() {
        return subtotal;
    }

    /** Devuelve los impuestos (escala 2). */
    public BigDecimal getTaxes() {
        return taxes;
    }

    /** Devuelve el descuento (escala 2). */
    public BigDecimal getDiscount() {
        return discount;
    }

    /** Devuelve el total (escala 2). */
    public BigDecimal getTotal() {
        return total;
    }

    /** Devuelve el estado de la factura. */
    public InvoiceStatus getStatus() {
        return status;
    }

    /** Asigna el estado de la factura. */
    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    /** Devuelve el método de pago registrado (texto mientras PaymentMethod esté pendiente). */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /** Asigna el método de pago registrado (texto mientras PaymentMethod esté pendiente). */
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /** Devuelve la orden de origen. */
    public ServiceOrder getOrder() {
        return order;
    }

    /** Asigna la orden de origen. */
    public void setOrder(ServiceOrder order) {
        this.order = order;
    }
}
