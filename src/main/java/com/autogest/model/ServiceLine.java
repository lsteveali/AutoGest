package com.autogest.model;

import java.math.BigDecimal;

/**
 * Línea de la orden que corresponde a un servicio del catálogo.
 *
 * <p>Principles: Polymorphism, LSP, OCP.</p>
 */
public class ServiceLine extends OrderLine {

    private ServiceOffering serviceOffering;

    /**
     * Crea una línea de servicio vacía.
     */
    public ServiceLine() {
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
     * Devuelve el nombre del servicio.
     *
     * @return nombre del servicio; vacío si no hay servicio o no tiene nombre
     */
    @Override
    public String getDescription() {
        if (serviceOffering == null || serviceOffering.getName() == null) {
            return "";
        }
        return serviceOffering.getName();
    }

    /** Devuelve el servicio aplicado. */
    public ServiceOffering getServiceOffering() {
        return serviceOffering;
    }

    /** Asigna el servicio aplicado. */
    public void setServiceOffering(ServiceOffering serviceOffering) {
        this.serviceOffering = serviceOffering;
    }
}
