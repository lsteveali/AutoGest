package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;
import com.autogest.model.ServiceOrder;
import com.autogest.repository.NotificationRepository;
import com.autogest.util.Formatter;
import java.util.Map;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Casos de uso de las notificaciones a clientes: crear, programar, enviar,
 * reintentar, eliminar y consultar. Prepara además las notificaciones de
 * facturación (emitida, pagada, vencida) que usa el módulo de Facturación.
 *
 * <p>Decisión D-17: todo el código específico de cada canal (EMAIL, SYSTEM,
 * WHATSAPP) vive en {@link #deliver(Notification)}. Agregar un canal es
 * agregar un caso ahí, sin tocar otras clases.</p>
 *
 * <p>Principles: GRASP Controller, Protected Variations (canales), Creator
 * (crea las notificaciones de factura), SRP, DIP, Low Coupling.</p>
 */
public class NotificationService {

    /** Máximo del asunto (columna {@code subject VARCHAR(150)}). */
    private static final int MAX_SUBJECT = 150;

    private final NotificationRepository repository;
    private final CustomerService customerService;

    /**
     * @param repository      repositorio de notificaciones
     * @param customerService servicio de clientes (valida el destinatario)
     */
    public NotificationService(NotificationRepository repository, CustomerService customerService) {
        this.repository = repository;
        this.customerService = customerService;
    }

    // Envío real de correos (Líder, 16/11/2026). Sin EmailSender el canal EMAIL
    // se comporta como antes (simulado): así funciona en equipos sin configuración.
    private EmailSender emailSender;

    /**
     * Configura el envío real de correos (por ejemplo, SMTP de Gmail).
     *
     * @param emailSender envío de correos, o {@code null} para simularlo
     */
    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Registra una notificación. Si la fecha es futura queda {@code SCHEDULED};
     * si no, {@code PENDING} con la fecha actual.
     *
     * @param notification datos (cliente, tipo, canal, asunto, mensaje, fecha opcional, orden opcional)
     * @return notificación guardada
     * @throws ValidationException si faltan datos o el cliente no acepta promociones
     */
    public Notification createNotification(Notification notification) {
        validate(notification);
        LocalDateTime now = LocalDateTime.now();
        if (notification.getScheduledDate() != null && notification.getScheduledDate().isAfter(now)) {
            notification.setStatus(NotificationStatus.SCHEDULED);
        } else {
            notification.setScheduledDate(now);
            notification.setStatus(NotificationStatus.PENDING);
        }
        return repository.create(notification);
    }

    /**
     * Registra la notificación y la envía de inmediato (si no está programada).
     *
     * @return la notificación con su estado final
     */
    public Notification createAndSend(Notification notification) {
        Notification saved = createNotification(notification);
        if (saved.getStatus() == NotificationStatus.SCHEDULED) {
            return saved;
        }
        return send(saved.getId());
    }

    /**
     * Envía una notificación pendiente o programada por su canal. Queda
     * {@code SENT} o {@code FAILED}.
     *
     * @param id id de la notificación
     * @return la notificación con su nuevo estado
     * @throws ValidationException si ya fue enviada
     */
    public Notification send(int id) {
        Notification notification = getDetail(id);
        if (notification.getStatus() == NotificationStatus.SENT) {
            throw new ValidationException("La notificación ya fue enviada");
        }
        if (notification.getStatus() == NotificationStatus.FAILED) {
            throw new ValidationException("La notificación falló: usa «Reintentar»");
        }
        deliver(notification);
        notification.setScheduledDate(LocalDateTime.now());
        repository.update(notification);
        return notification;
    }

    /**
     * Vuelve a intentar una notificación fallida.
     *
     * @throws ValidationException si la notificación no está en {@code FAILED}
     */
    public Notification retry(int id) {
        Notification notification = getDetail(id);
        if (notification.getStatus() != NotificationStatus.FAILED) {
            throw new ValidationException("Solo se pueden reintentar notificaciones fallidas");
        }
        notification.setStatus(NotificationStatus.PENDING);
        repository.update(notification);
        return send(id);
    }

    /**
     * Envía las notificaciones programadas cuya fecha ya llegó.
     *
     * @return cantidad de notificaciones procesadas
     */
    public int sendDueScheduled() {
        List<Notification> due = repository.findDueScheduled(LocalDateTime.now());
        for (Notification notification : due) {
            send(notification.getId());
        }
        return due.size();
    }

    /** Elimina una notificación. */
    public void deleteNotification(int id) {
        getDetail(id);
        repository.delete(id);
    }

    /**
     * Busca notificaciones; los filtros nulos no se aplican.
     */
    public List<Notification> searchNotifications(String text, NotificationType type, Channel channel,
                                                  NotificationStatus status) {
        return repository.search(text, type, channel, status);
    }

    /** Notificaciones de un cliente. */
    public List<Notification> findByCustomer(int customerId) {
        return repository.findByCustomer(customerId);
    }

    /**
     * Últimas {@code limit} notificaciones (tarjeta «Actividad reciente»).
     */
    public List<Notification> findRecent(int limit) {
        if (limit <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
        return repository.findAll().stream().limit(limit).toList();
    }

    /**
     * Notificación con su cliente.
     *
     * @throws NotFoundException si no existe
     */
    public Notification getDetail(int id) {
        Notification notification = repository.findById(id);
        if (notification == null) {
            throw new NotFoundException("No se encontró la notificación solicitada");
        }
        return notification;
    }

    /** Cantidad de notificaciones en un estado (KPI de la pantalla). */
    public int countByStatus(NotificationStatus status) {
        if (status == null) {
            throw new ValidationException("El estado es obligatorio");
        }
        return repository.countByStatus(status);
    }

    // ---------- consultas para Reportes (Sprint 7) ----------

    /** Notificaciones por canal (todos los canales, con 0 si no hay). */
    public Map<Channel, Integer> countByChannel() {
        return repository.countGroupedByChannel();
    }

    /**
     * Notificaciones por estado (todos los estados, con 0 si no hay).
     *
     * @return estado → cantidad
     */
    public Map<NotificationStatus, Integer> countGroupedByStatus() {
        return repository.countGroupedByStatus();
    }

    /**
     * Porcentaje de notificaciones entregadas sobre las que ya se intentaron
     * (enviadas + fallidas), de 0 a 100. Si no hay intentos devuelve 0.
     */
    public int deliveryRate() {
        Map<NotificationStatus, Integer> counts = repository.countGroupedByStatus();
        int sent = counts.getOrDefault(NotificationStatus.SENT, 0);
        int attempted = sent + counts.getOrDefault(NotificationStatus.FAILED, 0);
        return attempted == 0 ? 0 : Math.round(sent * 100f / attempted);
    }

    // ---------- notificaciones de facturación (Sprint 5) ----------

    /**
     * Prepara (sin guardar) la notificación «Factura disponible».
     *
     * @param invoice factura con su orden, vehículo y propietario
     * @return notificación de tipo {@code BILLING} por correo
     */
    public Notification buildInvoiceIssued(Invoice invoice) {
        Customer customer = requireInvoiceCustomer(invoice);
        String due = invoice.getDueDate() == null ? "" : " Fecha de vencimiento: " + Formatter.date(invoice.getDueDate()) + ".";
        return invoiceNotification(invoice, "Factura " + invoice.getNumber() + " disponible",
                "Hola " + customer.getFirstName() + ", tu factura " + invoice.getNumber() + " por "
                        + Formatter.money(invoice.getTotal()) + orderText(invoice) + " ya está disponible." + due);
    }

    /** Prepara (sin guardar) la notificación «Pago recibido». */
    public Notification buildInvoicePaid(Invoice invoice) {
        Customer customer = requireInvoiceCustomer(invoice);
        return invoiceNotification(invoice, "Pago recibido de la factura " + invoice.getNumber(),
                "Hola " + customer.getFirstName() + ", recibimos el pago de la factura " + invoice.getNumber()
                        + " por " + Formatter.money(invoice.getTotal()) + ". ¡Gracias por confiar en nosotros!");
    }

    /** Prepara (sin guardar) la notificación «Factura vencida». */
    public Notification buildInvoiceOverdue(Invoice invoice) {
        Customer customer = requireInvoiceCustomer(invoice);
        return invoiceNotification(invoice, "Factura " + invoice.getNumber() + " vencida",
                "Hola " + customer.getFirstName() + ", la factura " + invoice.getNumber() + " por "
                        + Formatter.money(invoice.getTotal()) + " venció el "
                        + Formatter.date(invoice.getDueDate()) + ". Por favor comunícate con el taller.");
    }

    /** Crea y envía «Factura disponible» (la llama Facturación al emitir). */
    public Notification notifyInvoiceIssued(Invoice invoice) {
        return createAndSend(buildInvoiceIssued(invoice));
    }

    /** Crea y envía «Pago recibido» (la llama Facturación al marcar pagada). */
    public Notification notifyInvoicePaid(Invoice invoice) {
        return createAndSend(buildInvoicePaid(invoice));
    }

    /** Crea y envía «Factura vencida». */
    public Notification notifyInvoiceOverdue(Invoice invoice) {
        return createAndSend(buildInvoiceOverdue(invoice));
    }

    // ---------- canales (D-17) ----------

    /**
     * Entrega la notificación según su canal. EMAIL requiere correo del
     * cliente; si hay un {@link EmailSender} configurado lo envía de verdad
     * (SMTP) y, si no, lo simula. SYSTEM se registra en la aplicación y
     * WHATSAPP queda preparado pero sin integración, por eso falla.
     */
    private void deliver(Notification notification) {
        Customer customer = notification.getCustomer();
        switch (notification.getChannel()) {
            case SYSTEM -> notification.markAsSent();
            case EMAIL -> {
                if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) {
                    notification.markAsFailed();
                } else if (emailSender == null) {
                    notification.markAsSent();
                } else {
                    try {
                        emailSender.send(customer.getEmail(), customer.getFullName(),
                                notification.getSubject(), notification.getMessage());
                        notification.markAsSent();
                    } catch (RuntimeException e) {
                        // Sin conexión o credenciales inválidas: queda Fallida y se reintenta.
                        notification.markAsFailed();
                    }
                }
            }
            case WHATSAPP -> notification.markAsFailed();
        }
    }

    // ---------- validaciones y ayudas ----------

    private void validate(Notification notification) {
        if (notification == null) {
            throw new ValidationException("La notificación es obligatoria");
        }
        if (notification.getCustomer() == null || notification.getCustomer().getId() <= 0) {
            throw new ValidationException("Selecciona el cliente");
        }
        Customer customer = customerService.getDetail(notification.getCustomer().getId());
        notification.setCustomer(customer);
        if (notification.getType() == null) {
            throw new ValidationException("Selecciona el tipo de notificación");
        }
        if (notification.getChannel() == null) {
            throw new ValidationException("Selecciona el canal");
        }
        if (notification.getType() == NotificationType.PROMOTION && !customer.isAcceptsPromotions()) {
            throw new ValidationException("El cliente no acepta recibir promociones");
        }
        String message = notification.getMessage();
        if (message == null || message.isBlank()) {
            throw new ValidationException("El mensaje es obligatorio");
        }
        notification.setMessage(message.trim());
        String subject = notification.getSubject();
        if (subject != null && subject.trim().length() > MAX_SUBJECT) {
            throw new ValidationException("El asunto no puede superar " + MAX_SUBJECT + " caracteres");
        }
        notification.setSubject(subject == null || subject.isBlank() ? null : subject.trim());
    }

    private Customer requireInvoiceCustomer(Invoice invoice) {
        if (invoice == null || invoice.getOrder() == null || invoice.getOrder().getVehicle() == null
                || invoice.getOrder().getVehicle().getOwner() == null) {
            throw new ValidationException("La factura debe tener orden, vehículo y cliente para notificar");
        }
        return invoice.getOrder().getVehicle().getOwner();
    }

    private Notification invoiceNotification(Invoice invoice, String subject, String message) {
        ServiceOrder order = invoice.getOrder();
        Notification notification = new Notification();
        notification.setType(NotificationType.BILLING);
        notification.setChannel(Channel.EMAIL);
        notification.setCustomer(order.getVehicle().getOwner());
        notification.setOrder(order);
        notification.setSubject(subject);
        notification.setMessage(message);
        return notification;
    }

    private static String orderText(Invoice invoice) {
        String number = invoice.getOrder().getNumber();
        return number == null ? "" : " de la orden #" + number;
    }
}
