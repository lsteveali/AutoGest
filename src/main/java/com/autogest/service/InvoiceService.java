package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.InvoiceSummary;
import com.autogest.model.OrderHistory;
import com.autogest.model.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.repository.InvoiceRepository;
import com.autogest.repository.ServiceOrderRepository;
import com.autogest.util.Formatter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Facturación (Sprint 5, Líder): genera la factura de una orden, la consulta,
 * la marca como pagada o la anula, y pasa a vencidas las pendientes.
 *
 * <p><b>Reglas:</b></p>
 * <ul>
 *   <li>Solo se facturan órdenes Listas o Entregadas, con al menos un servicio
 *       o repuesto. Una orden tiene 0..1 factura: si ya tiene una vigente no se
 *       genera otra. Si su factura está anulada, se vuelve a emitir sobre la
 *       misma fila con un número nuevo (la tabla exige un {@code order_id}
 *       único).</li>
 *   <li>Los conceptos son las líneas de la orden; el subtotal sale de
 *       {@link ServiceOrder#calculateSubtotal()}. El IVA va en 0 %
 *       ({@link #TAX_RATE}, decisión D-05) hasta que exista la configuración
 *       de facturación.</li>
 *   <li>El descuento es un valor sobre el total y no puede superar subtotal más
 *       impuestos ({@link Invoice#calculateTotal()}).</li>
 *   <li>Cada factura generada, pagada o anulada deja una entrada en el
 *       historial de su orden.</li>
 * </ul>
 *
 * <p><b>Avisos al cliente</b> ({@link NotificationService} de Dev 1): al
 * generar una factura pendiente se envía «Factura disponible»; al pagarla
 * (también si se paga al generarla) «Pago recibido»; y cuando una pendiente
 * pasa a vencida, «Factura vencida». Un aviso que falle no deshace la
 * factura: la notificación queda como fallida (por ejemplo, si el cliente no
 * tiene correo) y se reintenta desde Notificaciones.</p>
 *
 * <p>Principles: GRASP Controller (de caso de uso), Information Expert (los
 * importes y estados los decide {@link Invoice}), SRP, DIP.</p>
 */
public class InvoiceService {

    /** Tasa de IVA: 0 % hasta que Configuración la defina (D-05). */
    public static final BigDecimal TAX_RATE = BigDecimal.ZERO;

    /** Días de plazo de pago por defecto (mockup: emisión 02/10, vencimiento 16/10). */
    public static final int DEFAULT_DUE_DAYS = 14;

    /** Métodos de pago aceptados (se guardan con este código). */
    public static final List<String> PAYMENT_METHODS = List.of("CASH", "CARD", "TRANSFER", "OTHER");

    private final InvoiceRepository repository;
    private final ServiceOrderService orderService;
    private final ServiceOrderRepository orderRepository;
    private final NotificationService notificationService;

    /**
     * @param repository          facturas
     * @param orderService        consulta de órdenes (detalle completo y búsqueda)
     * @param orderRepository     historial de la orden
     * @param notificationService avisos al cliente (Dev 1)
     */
    public InvoiceService(InvoiceRepository repository, ServiceOrderService orderService,
                          ServiceOrderRepository orderRepository, NotificationService notificationService) {
        this.repository = repository;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    // ---------- generar ----------

    /**
     * Indica si una orden en ese estado se puede facturar (Lista o Entregada).
     *
     * @param status estado de la orden
     * @return {@code true} si se puede facturar
     */
    public static boolean canInvoice(OrderStatus status) {
        return status == OrderStatus.READY || status == OrderStatus.DELIVERED;
    }

    /**
     * Órdenes que se pueden facturar: Listas o Entregadas y sin factura
     * vigente, de la más reciente a la más antigua.
     *
     * @return órdenes completas (con sus líneas)
     */
    public List<ServiceOrder> findBillableOrders() {
        Set<Integer> invoiced = repository.findInvoicedOrderIds();
        List<ServiceOrder> orders = new ArrayList<>();
        orders.addAll(orderService.searchOrders("", OrderStatus.READY, 0));
        orders.addAll(orderService.searchOrders("", OrderStatus.DELIVERED, 0));
        orders.removeIf(order -> invoiced.contains(order.getId()) || order.getLines().isEmpty());
        orders.sort(Comparator.comparing(ServiceOrder::getEntryDate,
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(ServiceOrder::getId,
                Comparator.reverseOrder()));
        return orders;
    }

    /**
     * Arma la factura de una orden sin guardarla (la pantalla la usa para
     * mostrar el resumen mientras se escribe el descuento).
     *
     * @param order    orden completa
     * @param discount descuento ({@code null} = 0)
     * @param dueDate  vencimiento ({@code null} = hoy + {@value #DEFAULT_DUE_DAYS} días)
     * @return factura pendiente, sin número ni id
     * @throws ValidationException si el descuento es negativo o mayor que el total
     */
    public static Invoice buildDraft(ServiceOrder order, BigDecimal discount, LocalDate dueDate) {
        if (order == null) {
            throw new ValidationException("Selecciona la orden de servicio");
        }
        LocalDate today = LocalDate.now();
        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setIssueDate(today);
        invoice.setDueDate(dueDate == null ? today.plusDays(DEFAULT_DUE_DAYS) : dueDate);
        BigDecimal subtotal = order.calculateSubtotal();
        invoice.setSubtotal(subtotal);
        invoice.setTaxes(subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP));
        invoice.setDiscount(discount == null ? BigDecimal.ZERO : discount);
        invoice.setTotal(invoice.calculateTotal());
        return invoice;
    }

    /**
     * Genera y guarda la factura de una orden.
     *
     * @param orderId  id de la orden
     * @param discount descuento ({@code null} = 0)
     * @param dueDate  vencimiento ({@code null} = hoy + {@value #DEFAULT_DUE_DAYS} días)
     * @param paidWith método de pago si se registra el pago de una vez; {@code null} la deja pendiente
     * @param user     usuario que la genera
     * @return la factura guardada, con su orden completa
     * @throws ValidationException si la orden no se puede facturar o los datos no son válidos
     */
    public Invoice createInvoice(int orderId, BigDecimal discount, LocalDate dueDate, String paidWith, User user) {
        requireUser(user);
        ServiceOrder order = orderService.getDetail(orderId);
        if (!canInvoice(order.getStatus())) {
            throw new ValidationException("Solo se pueden facturar órdenes Listas o Entregadas");
        }
        if (order.getLines().isEmpty()) {
            throw new ValidationException("La orden no tiene servicios ni repuestos para facturar");
        }
        Invoice existing = repository.findByOrder(orderId);
        if (existing != null && existing.getStatus() != InvoiceStatus.VOIDED) {
            throw new ValidationException("La orden #" + order.getNumber() + " ya tiene la factura "
                    + existing.getNumber());
        }
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new ValidationException("El vencimiento no puede ser anterior a hoy");
        }

        Invoice invoice = buildDraft(order, discount, dueDate);
        invoice.setNumber(formatNumber(LocalDate.now().getYear(), repository.nextSequence()));
        if (paidWith != null) {
            invoice.markAsPaid(requireMethod(paidWith));
        }
        if (existing == null) {
            repository.create(invoice);
        } else {
            invoice.setId(existing.getId());
            repository.update(invoice);
        }

        String text = "Factura " + invoice.getNumber() + " generada por " + Formatter.money(invoice.getTotal())
                + (invoice.getStatus() == InvoiceStatus.PAID
                ? " y pagada con " + methodText(invoice.getPaymentMethod()) : "");
        record(orderId, text, user);
        // Si se paga al generarla, un solo aviso: «Pago recibido».
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            notifyCustomer(() -> notificationService.notifyInvoicePaid(invoice));
        } else {
            notifyCustomer(() -> notificationService.notifyInvoiceIssued(invoice));
        }
        return invoice;
    }

    /**
     * Número visible de la factura: {@code FAC-2026-001}.
     *
     * @param year     año de emisión
     * @param sequence consecutivo
     * @return número con el consecutivo de al menos tres cifras
     */
    public static String formatNumber(int year, int sequence) {
        return String.format("FAC-%d-%03d", year, sequence);
    }

    // ---------- consultar ----------

    /**
     * Factura con su orden completa (cliente, vehículo, líneas e historial).
     *
     * @param id id de la factura
     * @return la factura
     * @throws NotFoundException si no existe
     */
    public Invoice getDetail(int id) {
        Invoice invoice = requireInvoice(id);
        invoice.setOrder(orderService.getDetail(invoice.getOrder().getId()));
        return invoice;
    }

    /**
     * Factura de una orden, con la orden resumida.
     *
     * @param orderId id de la orden
     * @return la factura, o {@code null} si la orden no tiene
     */
    public Invoice findByOrder(int orderId) {
        return repository.findByOrder(orderId);
    }

    /**
     * Busca facturas por texto, estado, fecha de emisión desde y método.
     *
     * @param text          número de factura u orden, placa, vehículo o cliente
     * @param status        estado ({@code null} = todos)
     * @param issuedFrom    emitidas desde ({@code null} = todas)
     * @param paymentMethod código del método ({@code null} = todos)
     * @return facturas de la más reciente a la más antigua
     */
    public List<Invoice> searchInvoices(String text, InvoiceStatus status, LocalDate issuedFrom,
                                        String paymentMethod) {
        return repository.search(text, status, issuedFrom, paymentMethod);
    }

    /**
     * Indicadores de Facturación.
     *
     * @return conteos y valores
     */
    public InvoiceSummary getSummary() {
        return repository.summary();
    }

    /**
     * Indicadores de las facturas emitidas en el mes de la fecha dada (Dashboard).
     *
     * @param today fecha de referencia
     * @return conteos y valores desde el día 1 de ese mes
     */
    public InvoiceSummary getMonthSummary(LocalDate today) {
        return repository.summary(today.withDayOfMonth(1));
    }

    /**
     * Pasa a vencidas las facturas pendientes cuyo vencimiento ya pasó.
     *
     * @return ids de las que acaban de vencer
     */
    public List<Integer> refreshOverdue() {
        return refreshOverdue(LocalDate.now());
    }

    /**
     * Pasa a vencidas las facturas pendientes con vencimiento anterior a la
     * fecha y avisa a cada cliente una sola vez (solo las que acaban de vencer).
     *
     * @param today fecha de referencia (se recibe para probar con fechas fijas)
     * @return ids de las que acaban de vencer
     */
    public List<Integer> refreshOverdue(LocalDate today) {
        List<Integer> changed = repository.markOverdue(today);
        for (int id : changed) {
            notifyCustomer(() -> {
                Invoice invoice = repository.findById(id);
                return invoice == null ? null : notificationService.notifyInvoiceOverdue(invoice);
            });
        }
        return changed;
    }

    // ---------- pagar y anular ----------

    /**
     * Registra el pago de la factura (desde Pendiente o Vencida).
     *
     * @param id     id de la factura
     * @param method código del método de pago
     * @param user   usuario que registra el pago
     * @return la factura pagada
     * @throws ValidationException si ya está pagada o anulada, o el método no es válido
     */
    public Invoice markAsPaid(int id, String method, User user) {
        requireUser(user);
        Invoice invoice = requireInvoice(id);
        invoice.markAsPaid(requireMethod(method));
        repository.update(invoice);
        record(invoice.getOrder().getId(), "Factura " + invoice.getNumber() + " pagada con "
                + methodText(invoice.getPaymentMethod()), user);
        notifyCustomer(() -> notificationService.notifyInvoicePaid(invoice));
        return invoice;
    }

    /**
     * Anula la factura (baja lógica). La orden queda libre para volver a
     * facturarse.
     *
     * @param id   id de la factura
     * @param user usuario que la anula
     * @return la factura anulada
     * @throws ValidationException si está pagada o ya anulada
     */
    public Invoice voidInvoice(int id, User user) {
        requireUser(user);
        Invoice invoice = requireInvoice(id);
        invoice.voidInvoice();
        repository.update(invoice);
        record(invoice.getOrder().getId(), "Factura " + invoice.getNumber() + " anulada", user);
        return invoice;
    }

    /**
     * Nombre en español de un código de método de pago (para el historial).
     *
     * @param code código guardado
     * @return nombre visible, o el mismo texto si no es un código conocido
     */
    public static String methodText(String code) {
        if (code == null) {
            return "-";
        }
        return switch (code) {
            case "CASH" -> "Efectivo";
            case "CARD" -> "Tarjeta";
            case "TRANSFER" -> "Transferencia";
            case "OTHER" -> "Otro";
            default -> code;
        };
    }

    // ---------- ayudas ----------

    private Invoice requireInvoice(int id) {
        Invoice invoice = repository.findById(id);
        if (invoice == null) {
            throw new NotFoundException("No se encontró la factura solicitada");
        }
        return invoice;
    }

    private static String requireMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new ValidationException("El método de pago es obligatorio");
        }
        if (!PAYMENT_METHODS.contains(method)) {
            throw new ValidationException("Método de pago no válido");
        }
        return method;
    }

    /**
     * Envía un aviso al cliente sin afectar la factura: si falla (cliente sin
     * datos, error de conexión) la factura ya quedó guardada y el error no se
     * propaga. Una notificación sin correo queda «Fallida» en Notificaciones.
     */
    private void notifyCustomer(java.util.function.Supplier<?> send) {
        if (notificationService == null) {
            return;
        }
        try {
            send.get();
        } catch (RuntimeException ignored) {
            // El aviso es secundario: la operación de facturación ya se completó.
        }
    }

    private void record(int orderId, String text, User user) {
        OrderHistory entry = new OrderHistory();
        entry.setTimestamp(LocalDateTime.now());
        entry.setDescription(text);
        entry.setUser(user);
        orderRepository.addHistory(orderId, entry);
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new ValidationException("No hay un usuario con sesión iniciada");
        }
    }
}
