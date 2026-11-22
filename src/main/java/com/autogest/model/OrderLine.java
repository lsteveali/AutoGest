package com.autogest.model;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Línea de una orden de servicio: lo común a un servicio o a un repuesto
 * (cantidad y valor unitario). Obliga a las subclases a calcular su total y a
 * dar su descripción.
 *
 * <p>La orden recorre sus líneas llamando a {@link #calculateTotal()} sin
 * preguntar de qué tipo son. Toda subclase cumple el mismo contrato: el total es
 * no negativo e igual a cantidad por valor unitario, y no lanza excepciones
 * según el tipo.</p>
 *
 * <p>Principles: Polymorphism, OCP, LSP, Information Expert.</p>
 */
public abstract class OrderLine {

    protected int quantity;
    protected BigDecimal unitPrice;

    /**
     * Crea una línea con cantidad 0 y valor unitario cero.
     */
    protected OrderLine() {
        this.unitPrice = BigDecimal.ZERO.setScale(2);
    }

    /**
     * Calcula el total de la línea: cantidad por valor unitario.
     *
     * <p>Principles: Polymorphism, Information Expert.</p>
     *
     * @return total no negativo con escala 2
     */
    public abstract BigDecimal calculateTotal();

    /**
     * Devuelve el nombre que se muestra en la orden.
     *
     * @return nombre del servicio o del repuesto; vacío si no hay referencia
     */
    public abstract String getDescription();

    /**
     * Cálculo común a las subclases: cantidad por valor unitario, con escala 2 y
     * redondeo {@code HALF_UP}.
     *
     * @return importe de la línea
     */
    protected BigDecimal quantityTimesUnitPrice() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Asigna la cantidad.
     *
     * @param quantity cantidad; no puede ser negativa
     * @throws ValidationException si es negativa
     */
    public void setQuantity(int quantity) {
        if (quantity < 0) {
            throw new ValidationException("La cantidad no puede ser negativa");
        }
        this.quantity = quantity;
    }

    /**
     * Asigna el valor unitario, ajustado a escala 2 con redondeo {@code HALF_UP}.
     *
     * @param unitPrice valor unitario; no puede ser nulo ni negativo
     * @throws ValidationException si es nulo o negativo
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null) {
            throw new ValidationException("El precio unitario es obligatorio");
        }
        if (unitPrice.signum() < 0) {
            throw new ValidationException("El precio unitario no puede ser negativo");
        }
        this.unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /** Devuelve la cantidad. */
    public int getQuantity() {
        return quantity;
    }

    /** Devuelve el valor unitario al momento de la orden (escala 2). */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }
}
