package com.autogest.model;

import java.math.BigDecimal;

/**
 * Línea de la orden que corresponde a un repuesto del inventario.
 *
 * <p>Principles: Polymorphism, LSP, OCP.</p>
 */
public class PartLine extends OrderLine {

    private Part part;

    /**
     * Crea una línea de repuesto vacía.
     */
    public PartLine() {
    }

    /**
     * Calcula el total de la línea: cantidad por valor unitario.
     *
     * <p>Principles: Polymorphism, LSP.</p>
     *
     * @return total no negativo con escala 2
     */
    @Override
    public BigDecimal calculateTotal() {
        return quantityTimesUnitPrice();
    }

    /**
     * Devuelve el nombre del repuesto.
     *
     * @return nombre del repuesto; vacío si no hay repuesto o no tiene nombre
     */
    @Override
    public String getDescription() {
        if (part == null || part.getName() == null) {
            return "";
        }
        return part.getName();
    }

    /** Devuelve el repuesto utilizado. */
    public Part getPart() {
        return part;
    }

    /** Asigna el repuesto utilizado. */
    public void setPart(Part part) {
        this.part = part;
    }
}
