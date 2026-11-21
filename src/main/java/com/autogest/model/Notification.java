package com.autogest.model;

import com.autogest.model.Catalogs.NotificationType;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.model.Catalogs.Channel;
import com.autogest.exception.ValidationException;
import java.time.LocalDateTime;

/**
 * Mensaje para un cliente, generado por un evento del sistema o creado a mano.
 *
 * <p>Controla su propio estado de envío: pendiente, programada, enviada o fallida.
 * El código específico de cada canal vive en {@code NotificationService}, no aquí.</p>
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class Notification {

    private int id;
    private NotificationType type;
    private Channel channel;
    private String subject;
    private String message;
    private NotificationStatus status;
    private LocalDateTime scheduledDate;
    private ServiceOrder order;
    private Customer customer;

    /**
     * Crea una notificación vacía, con {@code id} 0 y estado {@link NotificationStatus#PENDING}.
     */
    public Notification() {
        this.status = NotificationStatus.PENDING;
    }

    /**
     * Indica si la notificación aún no se envía: está pendiente o programada.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si el estado es {@code PENDING} o {@code SCHEDULED}
     */
    public boolean isPending() {
        return status == NotificationStatus.PENDING || status == NotificationStatus.SCHEDULED;
    }

    /**
     * Marca la notificación como enviada. Se puede desde {@code PENDING},
     * {@code SCHEDULED} o {@code FAILED} (un reintento exitoso).
     *
     * <p>Principles: Information Expert.</p>
     *
     * @throws ValidationException si ya fue enviada
     */
    public void markAsSent() {
        if (status == NotificationStatus.SENT) {
            throw new ValidationException("La notificación ya fue enviada");
        }
        this.status = NotificationStatus.SENT;
    }

    /**
     * Marca el envío como fallido. Solo se puede desde {@code PENDING} o
     * {@code SCHEDULED}.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @throws ValidationException si ya fue enviada o ya está marcada como fallida
     */
    public void markAsFailed() {
        if (status == NotificationStatus.SENT) {
            throw new ValidationException("Una notificación enviada no puede marcarse como fallida");
        }
        if (status == NotificationStatus.FAILED) {
            throw new ValidationException("La notificación ya está marcada como fallida");
        }
        this.status = NotificationStatus.FAILED;
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el tipo de notificación. */
    public NotificationType getType() {
        return type;
    }

    /** Asigna el tipo de notificación. */
    public void setType(NotificationType type) {
        this.type = type;
    }

    /** Devuelve el canal de envío. */
    public Channel getChannel() {
        return channel;
    }

    /** Asigna el canal de envío. */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /** Devuelve el asunto. */
    public String getSubject() {
        return subject;
    }

    /** Asigna el asunto. */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /** Devuelve el contenido del mensaje. */
    public String getMessage() {
        return message;
    }

    /** Asigna el contenido del mensaje. */
    public void setMessage(String message) {
        this.message = message;
    }

    /** Devuelve el estado de envío. */
    public NotificationStatus getStatus() {
        return status;
    }

    /** Asigna el estado de envío. */
    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    /** Devuelve la fecha y hora de envío programado. */
    public LocalDateTime getScheduledDate() {
        return scheduledDate;
    }

    /** Asigna la fecha y hora de envío programado. */
    public void setScheduledDate(LocalDateTime scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    /** Devuelve la orden relacionada (opcional). */
    public ServiceOrder getOrder() {
        return order;
    }

    /** Asigna la orden relacionada (opcional). */
    public void setOrder(ServiceOrder order) {
        this.order = order;
    }

    /** Devuelve el cliente destinatario. */
    public Customer getCustomer() {
        return customer;
    }

    /** Asigna el cliente destinatario. */
    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
}
