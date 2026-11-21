package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Diagnosis;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.OrderHistory;
import com.autogest.model.OrderLine;
import com.autogest.model.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.model.VehicleStatus;
import com.autogest.repository.ServiceOrderRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Seguimiento de una orden de servicio ya creada (Sprint 4, parte del Líder):
 * cambio de estado, historial, diagnóstico, responsable, líneas agregadas
 * después de crearla y la integración con el inventario.
 *
 * <p>Se separa de {@link ServiceOrderService} (creación y consultas, Dev 1)
 * como sugiere la guía: «si {@code ServiceOrderService} crece demasiado, se
 * separan costos o estados en otra clase de service» (SRP).</p>
 *
 * <p><b>Regla de stock:</b> mientras la orden está En reparación, Lista o
 * Entregada, todos sus repuestos ya están descontados del inventario. Por eso:
 * al pasar a En reparación se descuentan todos; un repuesto agregado en esos
 * estados se descuenta en el momento; y al cancelar una orden que ya los había
 * descontado se devuelven. Al entregar o cancelar, el vehículo vuelve a Activo.</p>
 *
 * <p>Principles: GRASP Controller (de caso de uso), Information Expert (las
 * transiciones las decide {@link ServiceOrder#canChangeTo}), SRP, DIP.</p>
 */
public class OrderWorkflowService {

    /** Estados en los que los repuestos de la orden ya salieron del inventario. */
    static final Set<OrderStatus> STOCK_DEDUCTED =
            EnumSet.of(OrderStatus.IN_REPAIR, OrderStatus.READY, OrderStatus.DELIVERED);

    /** Estados en los que todavía se pueden agregar servicios y repuestos. */
    static final Set<OrderStatus> EDITABLE_LINES =
            EnumSet.of(OrderStatus.RECEIVED, OrderStatus.DIAGNOSIS, OrderStatus.IN_REPAIR);

    private static final int MAX_NOTE = 500;

    private final ServiceOrderRepository repository;
    private final ServiceOrderService orderService;
    private final VehicleService vehicleService;
    private final InventoryService inventoryService;
    private final PartService partService;
    private final UserService userService;
    private NotificationEventService events;

    /**
     * @param repository       repositorio de órdenes
     * @param orderService     creación y consultas de órdenes (arma las líneas)
     * @param vehicleService   estado del vehículo
     * @param inventoryService entradas y salidas de stock
     * @param partService      stock actual de cada repuesto
     * @param userService      mecánicos activos (responsable)
     */
    public OrderWorkflowService(ServiceOrderRepository repository, ServiceOrderService orderService,
                                VehicleService vehicleService, InventoryService inventoryService,
                                PartService partService, UserService userService) {
        this.repository = repository;
        this.orderService = orderService;
        this.vehicleService = vehicleService;
        this.inventoryService = inventoryService;
        this.partService = partService;
        this.userService = userService;
    }

    /**
     * Igual que el constructor anterior, más los avisos automáticos al cliente
     * (Sprint 6): al pasar a Lista se le avisa que el vehículo está listo.
     *
     * @param events avisos automáticos ({@code null} = sin avisos)
     */
    public OrderWorkflowService(ServiceOrderRepository repository, ServiceOrderService orderService,
                                VehicleService vehicleService, InventoryService inventoryService,
                                PartService partService, UserService userService, NotificationEventService events) {
        this(repository, orderService, vehicleService, inventoryService, partService, userService);
        this.events = events;
    }

    // ---------- estado ----------

    /**
     * Pasa la orden a otro estado (un paso adelante o Cancelada, D-04),
     * mueve el stock según la regla de la clase, deja constancia en el
     * historial y libera el vehículo al entregar o cancelar.
     *
     * @param orderId   id de la orden
     * @param newStatus estado destino
     * @param user      usuario que hace el cambio
     * @return la orden actualizada
     * @throws ValidationException si la transición no es válida o no alcanza el stock
     */
    public ServiceOrder changeStatus(int orderId, OrderStatus newStatus, User user) {
        requireUser(user);
        ServiceOrder order = orderService.getDetail(orderId);
        OrderStatus previous = order.getStatus();
        if (!order.canChangeTo(newStatus)) {
            // Mensaje del modelo (estados en español) sin modificar la orden.
            order.changeStatus(newStatus, user);
        }
        int historyBefore = order.getHistory().size();

        List<PartLine> parts = partLines(order);
        boolean deductNow = !STOCK_DEDUCTED.contains(previous) && STOCK_DEDUCTED.contains(newStatus);
        boolean returnNow = STOCK_DEDUCTED.contains(previous) && newStatus == OrderStatus.CANCELLED;
        if (deductNow && !parts.isEmpty()) {
            deductStock(order, parts, user);
        }
        if (returnNow && !parts.isEmpty()) {
            returnStock(order, parts, user);
        }

        order.changeStatus(newStatus, user);
        if (deductNow && !parts.isEmpty()) {
            order.recordHistory(unitsText(parts, "descontado") + " del inventario", user);
        }
        if (returnNow && !parts.isEmpty()) {
            order.recordHistory(unitsText(parts, "devuelto") + " al inventario", user);
        }
        repository.update(order);
        persistNewHistory(order, historyBefore);

        if (newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED) {
            releaseVehicle(order);
        }
        if (newStatus == OrderStatus.READY) {
            notifyVehicleReady(order, user);
        }
        return order;
    }

    /**
     * Avisa al cliente que el vehículo está listo y deja constancia en el
     * historial. Un aviso que no sale no deshace el cambio de estado.
     */
    private void notifyVehicleReady(ServiceOrder order, User user) {
        // Sin servicio de avisos, o con el aviso apagado en Configuración → Notificaciones.
        if (events == null || !events.isVehicleReadyEnabled()) {
            return;
        }
        String text;
        try {
            Notification sent = events.notifyVehicleReady(order);
            text = sent.getStatus() == NotificationStatus.SENT
                    ? "Se avisó al cliente que el vehículo está listo"
                    : "No se pudo avisar al cliente que el vehículo está listo (revisa su correo en Notificaciones)";
        } catch (RuntimeException e) {
            text = "No se pudo avisar al cliente que el vehículo está listo";
        }
        try {
            record(order, text, user);
        } catch (RuntimeException ignored) {
            // El historial del aviso es secundario: el cambio de estado ya se guardó.
        }
    }

    /**
     * Siguiente estado del flujo normal, o {@code null} si la orden ya está
     * entregada o cancelada.
     */
    public static OrderStatus nextStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case RECEIVED -> OrderStatus.DIAGNOSIS;
            case DIAGNOSIS -> OrderStatus.IN_REPAIR;
            case IN_REPAIR -> OrderStatus.READY;
            case READY -> OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> null;
        };
    }

    // ---------- diagnóstico, responsable y notas ----------

    /**
     * Registra o reemplaza el diagnóstico. Solo con la orden abierta.
     *
     * @throws ValidationException si falta la descripción o la orden está cerrada
     */
    public ServiceOrder saveDiagnosis(int orderId, String description, String recommendations, User user) {
        requireUser(user);
        ServiceOrder order = orderService.getDetail(orderId);
        requireOpen(order);
        if (isBlank(description)) {
            throw new ValidationException("Describe el diagnóstico");
        }
        boolean existed = order.getDiagnosis() != null;
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setDescription(description.trim());
        diagnosis.setRecommendations(isBlank(recommendations) ? null : recommendations.trim());
        diagnosis.setDate(LocalDate.now());
        repository.saveDiagnosis(orderId, diagnosis);
        order.setDiagnosis(diagnosis);
        record(order, existed ? "Diagnóstico actualizado" : "Diagnóstico registrado", user);
        return order;
    }

    /**
     * Asigna (o quita, con {@code null}) el mecánico responsable.
     *
     * @throws ValidationException si no es un mecánico activo o la orden está cerrada
     */
    public ServiceOrder assignResponsible(int orderId, User mechanic, User user) {
        requireUser(user);
        ServiceOrder order = orderService.getDetail(orderId);
        requireOpen(order);
        if (mechanic != null) {
            boolean active = userService.findActiveMechanics().stream().anyMatch(m -> m.getId() == mechanic.getId());
            if (!mechanic.hasRole(Role.MECHANIC) || mechanic.getStatus() == UserStatus.INACTIVE || !active) {
                throw new ValidationException("El responsable debe ser un mecánico activo");
            }
        }
        order.setResponsible(mechanic);
        repository.update(order);
        record(order, mechanic == null ? "Responsable retirado"
                : "Responsable asignado: " + mechanic.getFullName(), user);
        return order;
    }

    /**
     * Agrega una nota libre al historial (por ejemplo «Cliente pide revisar frenos»).
     *
     * @throws ValidationException si la nota está vacía o es muy larga
     */
    public ServiceOrder addNote(int orderId, String note, User user) {
        requireUser(user);
        if (isBlank(note)) {
            throw new ValidationException("Escribe la nota");
        }
        if (note.trim().length() > MAX_NOTE) {
            throw new ValidationException("La nota no puede superar " + MAX_NOTE + " caracteres");
        }
        ServiceOrder order = orderService.getDetail(orderId);
        record(order, "Nota: " + note.trim(), user);
        return order;
    }

    // ---------- líneas ----------

    /**
     * Agrega un servicio del catálogo a una orden abierta (Recibida,
     * Diagnóstico o En reparación).
     */
    public ServiceOrder addService(int orderId, ServiceOffering offering, int quantity, User user) {
        requireUser(user);
        ServiceOrder order = orderService.getDetail(orderId);
        requireEditableLines(order);
        OrderLine line = orderService.buildServiceLine(offering, quantity);
        repository.addLine(orderId, line);
        order.addLine(line);
        record(order, "Servicio agregado: " + line.getDescription() + " × " + quantity, user);
        return order;
    }

    /**
     * Agrega un repuesto a una orden abierta. Si la orden ya está En
     * reparación, el repuesto se descuenta del inventario en el momento.
     */
    public ServiceOrder addPart(int orderId, Part part, int quantity, User user) {
        requireUser(user);
        ServiceOrder order = orderService.getDetail(orderId);
        requireEditableLines(order);
        Part fresh = part == null ? null : partService.getDetail(part.getId());
        PartLine line = orderService.buildPartLine(fresh, quantity);
        boolean deduct = STOCK_DEDUCTED.contains(order.getStatus());
        if (deduct) {
            inventoryService.registerExit(fresh.getId(), quantity, reasonFor(order), user);
        }
        repository.addLine(orderId, line);
        order.addLine(line);
        record(order, "Repuesto agregado: " + line.getDescription() + " × " + quantity
                + (deduct ? " (descontado del inventario)" : ""), user);
        return order;
    }

    // ---------- stock ----------

    /**
     * Descuenta todos los repuestos. Primero comprueba que alcance el stock de
     * cada uno (sumando líneas del mismo repuesto); si una salida falla a
     * mitad, devuelve las ya hechas para no dejar el inventario a medias.
     */
    private void deductStock(ServiceOrder order, List<PartLine> parts, User user) {
        Map<Integer, Integer> needed = new LinkedHashMap<>();
        Map<Integer, Part> byId = new LinkedHashMap<>();
        for (PartLine line : parts) {
            needed.merge(line.getPart().getId(), line.getQuantity(), Integer::sum);
            byId.put(line.getPart().getId(), line.getPart());
        }
        List<String> missing = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : needed.entrySet()) {
            Part current = partService.getDetail(entry.getKey());
            if (current.getCurrentStock() < entry.getValue()) {
                missing.add(current.getName() + " (hay " + current.getCurrentStock() + ", se necesitan "
                        + entry.getValue() + ")");
            }
        }
        if (!missing.isEmpty()) {
            throw new ValidationException("No alcanza el stock para pasar a En reparación: "
                    + String.join("; ", missing) + ". Registra una entrada en Inventario y vuelve a intentarlo.");
        }
        String reason = reasonFor(order);
        List<PartLine> done = new ArrayList<>();
        try {
            for (PartLine line : parts) {
                inventoryService.registerExit(line.getPart().getId(), line.getQuantity(), reason, user);
                done.add(line);
            }
        } catch (RuntimeException e) {
            for (PartLine line : done) {
                inventoryService.registerEntry(line.getPart().getId(), line.getQuantity(),
                        "Reversión: " + reason, user);
            }
            throw e;
        }
    }

    private void returnStock(ServiceOrder order, List<PartLine> parts, User user) {
        String reason = "Devolución por cancelación de la orden #" + order.getNumber();
        for (PartLine line : parts) {
            inventoryService.registerEntry(line.getPart().getId(), line.getQuantity(), reason, user);
        }
    }

    private void releaseVehicle(ServiceOrder order) {
        if (order.getVehicle() == null) {
            return;
        }
        int vehicleId = order.getVehicle().getId();
        if (vehicleService.getDetail(vehicleId).getStatus() == VehicleStatus.IN_SERVICE) {
            vehicleService.changeStatus(vehicleId, VehicleStatus.ACTIVE);
        }
    }

    // ---------- ayudas ----------

    private void record(ServiceOrder order, String text, User user) {
        int before = order.getHistory().size();
        order.recordHistory(text, user);
        persistNewHistory(order, before);
    }

    private void persistNewHistory(ServiceOrder order, int from) {
        List<OrderHistory> history = order.getHistory();
        for (int i = from; i < history.size(); i++) {
            repository.addHistory(order.getId(), history.get(i));
        }
    }

    private static List<PartLine> partLines(ServiceOrder order) {
        List<PartLine> parts = new ArrayList<>();
        for (OrderLine line : order.getLines()) {
            if (line instanceof PartLine part && part.getPart() != null) {
                parts.add(part);
            }
        }
        return parts;
    }

    /** «1 repuesto descontado» o «3 repuestos descontados» (concordancia en número). */
    private static String unitsText(List<PartLine> parts, String participle) {
        int units = parts.stream().mapToInt(OrderLine::getQuantity).sum();
        return units == 1 ? "1 repuesto " + participle : units + " repuestos " + participle + "s";
    }

    private static String reasonFor(ServiceOrder order) {
        return "Orden de servicio #" + order.getNumber();
    }

    private static void requireOpen(ServiceOrder order) {
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new ValidationException("La orden está cerrada");
        }
    }

    private static void requireEditableLines(ServiceOrder order) {
        if (!EDITABLE_LINES.contains(order.getStatus())) {
            throw new ValidationException("Ya no se pueden agregar servicios ni repuestos: la orden está "
                    + "lista, entregada o cancelada");
        }
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new ValidationException("Debe haber un usuario en sesión");
        }
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
