package com.autogest.model;

import com.autogest.util.Formatter;
import java.time.LocalDateTime;

/**
 * Entrada del historial de una orden de servicio: qué ocurrió, cuándo y quién lo
 * hizo. Garantiza la trazabilidad de la orden.
 *
 * <p>Las crea {@code ServiceOrder.recordHistory}, que es la dueña del historial.</p>
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class OrderHistory {

    private LocalDateTime timestamp;
    private String description;
    private User user;

    /**
     * Crea una entrada de historial vacía.
     */
    public OrderHistory() {
    }

    /**
     * Devuelve el texto listo para mostrar, con el formato
     * {@code dd/MM/yyyy HH:mm - Nombre del usuario: descripción}. Si no hay usuario,
     * omite su nombre.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return línea de historial para la pantalla
     */
    public String getDisplayLine() {
        String when = Formatter.timestamp(timestamp);
        if (user == null) {
            return when + " - " + description;
        }
        return when + " - " + user.getFullName() + ": " + description;
    }

    /** Devuelve la fecha y hora del cambio. */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /** Asigna la fecha y hora del cambio. */
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    /** Devuelve lo que ocurrió. */
    public String getDescription() {
        return description;
    }

    /** Asigna lo que ocurrió. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Devuelve el usuario que realizó el cambio. */
    public User getUser() {
        return user;
    }

    /** Asigna el usuario que realizó el cambio. */
    public void setUser(User user) {
        this.user = user;
    }
}
