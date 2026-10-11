package com.autogest.model;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Servicio del catálogo del taller, que alimenta las órdenes de servicio.
 *
 * <p>Guarda los repuestos que suele requerir. La lista se modifica solo con
 * {@link #addPart(Part)}: {@link #getParts()} devuelve una vista de solo lectura.
 * El precio usa escala 2 con redondeo {@code HALF_UP}.</p>
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class ServiceOffering {

    private int id;
    private String code;
    private String name;
    private String category;
    private String description;
    private double durationHours;
    private BigDecimal price;
    private ServiceOfferingStatus status;
    private List<Part> parts;

    /**
     * Crea un servicio vacío, con {@code id} 0, estado {@link ServiceOfferingStatus#ACTIVE} y sin repuestos.
     */
    public ServiceOffering() {
        this.status = ServiceOfferingStatus.ACTIVE;
        this.parts = new ArrayList<>();
    }

    /**
     * Indica si el servicio se puede agregar a nuevas órdenes.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si el estado es {@code ACTIVE}
     */
    public boolean isActive() {
        return status == ServiceOfferingStatus.ACTIVE;
    }

    /**
     * Asocia un repuesto al servicio.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param part repuesto a asociar
     * @throws ValidationException si el repuesto es {@code null}
     */
    public void addPart(Part part) {
        if (part == null) {
            throw new ValidationException("El repuesto es obligatorio");
        }
        parts.add(part);
    }

    /** Devuelve los repuestos asociados como una vista de solo lectura. */
    public List<Part> getParts() {
        return Collections.unmodifiableList(parts);
    }

    /** Reemplaza los repuestos asociados por una copia de la lista recibida ({@code null} deja la lista vacía). */
    public void setParts(List<Part> parts) {
        this.parts = parts == null ? new ArrayList<>() : new ArrayList<>(parts);
    }

    /**
     * Asigna el precio base, ajustado a escala 2 con redondeo {@code HALF_UP}.
     *
     * @param price precio; puede ser {@code null}
     */
    public void setPrice(BigDecimal price) {
        this.price = price == null ? null : price.setScale(2, RoundingMode.HALF_UP);
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el código del servicio. */
    public String getCode() {
        return code;
    }

    /** Asigna el código del servicio. */
    public void setCode(String code) {
        this.code = code;
    }

    /** Devuelve el nombre. */
    public String getName() {
        return name;
    }

    /** Asigna el nombre. */
    public void setName(String name) {
        this.name = name;
    }

    /** Devuelve la categoría, por ejemplo Mantenimiento o Frenos. */
    public String getCategory() {
        return category;
    }

    /** Asigna la categoría, por ejemplo Mantenimiento o Frenos. */
    public void setCategory(String category) {
        this.category = category;
    }

    /** Devuelve la descripción. */
    public String getDescription() {
        return description;
    }

    /** Asigna la descripción. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Devuelve la duración estimada en horas. */
    public double getDurationHours() {
        return durationHours;
    }

    /** Asigna la duración estimada en horas. */
    public void setDurationHours(double durationHours) {
        this.durationHours = durationHours;
    }

    /** Devuelve el precio base (escala 2). */
    public BigDecimal getPrice() {
        return price;
    }

    /** Devuelve el estado del servicio. */
    public ServiceOfferingStatus getStatus() {
        return status;
    }

    /** Asigna el estado del servicio. */
    public void setStatus(ServiceOfferingStatus status) {
        this.status = status;
    }
}
