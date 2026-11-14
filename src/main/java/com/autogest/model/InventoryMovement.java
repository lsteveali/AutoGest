package com.autogest.model;

import java.time.LocalDateTime;

/**
 * Registro de una entrada, salida o ajuste de stock de un repuesto, para poder
 * consultar el historial y saber quién lo hizo.
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class InventoryMovement {

    private int id;
    private MovementType type;
    private int quantity;
    private LocalDateTime timestamp;
    private String reason;
    private Part part;
    private User user;

    /**
     * Crea un movimiento vacío.
     */
    public InventoryMovement() {
    }

    /**
     * Indica si el movimiento es una entrada de stock. Un ajuste no cuenta como
     * entrada, porque con la cantidad sola no se sabe si suma o resta.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si el tipo es {@code ENTRY}
     */
    public boolean isEntry() {
        return type == MovementType.ENTRY;
    }

    /** Devuelve el id del movimiento (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el id del movimiento. */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el tipo de movimiento. */
    public MovementType getType() {
        return type;
    }

    /** Asigna el tipo de movimiento. */
    public void setType(MovementType type) {
        this.type = type;
    }

    /** Devuelve las unidades movidas. */
    public int getQuantity() {
        return quantity;
    }

    /** Asigna las unidades movidas. */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    /** Devuelve la fecha y hora del movimiento. */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** Asigna la fecha y hora del movimiento. */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /** Devuelve la razón del movimiento. */
    public String getReason() {
        return reason;
    }

    /** Asigna la razón del movimiento. */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /** Devuelve el repuesto afectado. */
    public Part getPart() {
        return part;
    }

    /** Asigna el repuesto afectado. */
    public void setPart(Part part) {
        this.part = part;
    }

    /** Devuelve el usuario que registró el movimiento. */
    public User getUser() {
        return user;
    }

    /** Asigna el usuario que registró el movimiento. */
    public void setUser(User user) {
        this.user = user;
    }
}
