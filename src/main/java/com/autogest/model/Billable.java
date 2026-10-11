package com.autogest.model;

import java.math.BigDecimal;

/**
 * Contrato de cualquier elemento que se pueda cobrar: calcula subtotal, impuestos y total.
 *
 * <p>La implementa {@code ServiceOrder}. Es una interfaz pequeña: sus tres métodos
 * los usa toda clase que la implementa.</p>
 *
 * <p>Principles: Interface Segregation (ISP), Polymorphism, OCP.</p>
 */
public interface Billable {

    /**
     * Calcula el valor antes de impuestos.
     *
     * @return subtotal con escala 2
     */
    BigDecimal calculateSubtotal();

    /**
     * Calcula los impuestos sobre el subtotal.
     *
     * @return impuestos con escala 2
     */
    BigDecimal calculateTaxes();

    /**
     * Calcula el valor final a cobrar (subtotal más impuestos).
     *
     * @return total con escala 2
     */
    BigDecimal calculateTotal();
}
