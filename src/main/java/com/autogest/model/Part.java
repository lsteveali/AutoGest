package com.autogest.model;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Repuesto del inventario, con su stock y su precio.
 *
 * <p>La propia clase controla sus existencias: el stock nunca queda negativo y el
 * estado ({@link StockStatus}) se deriva del stock actual y del mínimo, por eso
 * no tiene un {@code setStatus}: se recalcula solo cuando el stock cambia.
 * El precio se guarda como {@link BigDecimal} con escala 2 y redondeo
 * {@code HALF_UP}.</p>
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class Part {

    private int id;
    private String code;
    private String name;
    private String category;
    private int currentStock;
    private int minimumStock;
    private BigDecimal unitPrice;
    private StockStatus status;
    private Supplier supplier;

    /**
     * Crea un repuesto vacío, con {@code id} 0 y sin existencias ({@link StockStatus#OUT_OF_STOCK}).
     */
    public Part() {
        updateStatus();
    }

    /**
     * Indica si las existencias son menores o iguales al stock mínimo.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si el stock actual no supera el mínimo
     */
    public boolean isBelowMinimum() {
        return currentStock <= minimumStock;
    }

    /**
     * Indica si el repuesto no tiene existencias.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si el stock actual es cero
     */
    public boolean isOutOfStock() {
        return currentStock == 0;
    }

    /**
     * Suma existencias (por ejemplo, una entrada de inventario) y recalcula el estado.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param quantity unidades a sumar; debe ser mayor que cero
     * @throws ValidationException si la cantidad es cero o negativa
     */
    public void increaseStock(int quantity) {
        requirePositive(quantity);
        currentStock += quantity;
        updateStatus();
    }

    /**
     * Resta existencias y recalcula el estado. El stock nunca queda negativo.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param quantity unidades a restar; debe ser mayor que cero
     * @throws ValidationException si la cantidad es cero o negativa, o si no hay
     *                             existencias suficientes
     */
    public void decreaseStock(int quantity) {
        requirePositive(quantity);
        if (quantity > currentStock) {
            throw new ValidationException("Stock insuficiente: solo hay " + currentStock + " unidades");
        }
        currentStock -= quantity;
        updateStatus();
    }

    /**
     * Asigna las existencias actuales y recalcula el estado.
     *
     * @param currentStock existencias; no puede ser negativo
     * @throws ValidationException si es negativo
     */
    public void setCurrentStock(int currentStock) {
        if (currentStock < 0) {
            throw new ValidationException("El stock no puede ser negativo");
        }
        this.currentStock = currentStock;
        updateStatus();
    }

    /**
     * Asigna el stock mínimo y recalcula el estado.
     *
     * @param minimumStock umbral de alerta
     */
    public void setMinimumStock(int minimumStock) {
        this.minimumStock = minimumStock;
        updateStatus();
    }

    /**
     * Asigna el precio unitario, ajustado a escala 2 con redondeo {@code HALF_UP}.
     *
     * @param unitPrice precio; puede ser {@code null}
     */
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice == null ? null : unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /** Valida que una cantidad de movimiento sea mayor que cero. */
    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
    }

    /** Deriva el estado: 0 = agotado, menor o igual al mínimo = stock bajo, otro caso = disponible. */
    private void updateStatus() {
        if (isOutOfStock()) {
            status = StockStatus.OUT_OF_STOCK;
        } else if (isBelowMinimum()) {
            status = StockStatus.LOW_STOCK;
        } else {
            status = StockStatus.AVAILABLE;
        }
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el código del repuesto, por ejemplo REP-001. */
    public String getCode() {
        return code;
    }

    /** Asigna el código del repuesto, por ejemplo REP-001. */
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

    /** Devuelve la categoría. */
    public String getCategory() {
        return category;
    }

    /** Asigna la categoría. */
    public void setCategory(String category) {
        this.category = category;
    }

    /** Devuelve las existencias actuales. */
    public int getCurrentStock() {
        return currentStock;
    }

    /** Devuelve el stock mínimo, que es el umbral de alerta. */
    public int getMinimumStock() {
        return minimumStock;
    }

    /** Devuelve el precio unitario de venta (escala 2). */
    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    /** Devuelve el nivel de existencias, derivado del stock actual y el mínimo. */
    public StockStatus getStatus() {
        return status;
    }

    /** Devuelve el proveedor habitual. */
    public Supplier getSupplier() {
        return supplier;
    }

    /** Asigna el proveedor habitual. */
    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }
}
