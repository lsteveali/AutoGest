package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.Catalogs.FuelType;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;
import com.autogest.model.OrderHistory;
import com.autogest.model.OrderLine;
import com.autogest.model.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.ServiceOffering;
import com.autogest.model.Catalogs.ServiceOfferingStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.model.Vehicle;
import com.autogest.model.Catalogs.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link OrderWorkflowService} con repositorios falsos y los
 * servicios reales de vehículos, repuestos e inventario: así se comprueba de
 * punta a punta el stock y el estado del vehículo.
 */
class OrderWorkflowServiceTest {

    private RecordingOrderRepository orders;
    private VehicleService vehicleService;
    private PartService partService;
    private FakeInventoryMovementRepository movements;
    private ServiceOrderService orderService;
    private OrderWorkflowService workflow;
    private Vehicle vehicle;
    private User receptionist;
    private User mechanic;
    private Part filter;
    private FakeNotificationRepository notifications;
    private Customer owner;

    private NotificationEventService events;

    @BeforeEach
    void setUp() {
        FakeCustomerRepository customers = new FakeCustomerRepository();
        vehicleService = new VehicleService(new FakeVehicleRepository(), customers);
        FakePartRepository parts = new FakePartRepository();
        partService = new PartService(parts);
        movements = new FakeInventoryMovementRepository();
        InventoryService inventoryService = new InventoryService(movements, partService);
        FakeUserRepository users = new FakeUserRepository();
        UserService userService = new UserService(users);
        orders = new RecordingOrderRepository();
        orderService = new ServiceOrderService(orders, vehicleService);
        notifications = new FakeNotificationRepository();
        // Avisos automáticos (Sprint 6) con el NotificationService real de Dev 1.
        events = new NotificationEventService(
                new NotificationService(notifications, new CustomerService(customers)), vehicleService);
        workflow = new OrderWorkflowService(orders, orderService, vehicleService, inventoryService,
                partService, userService, events);

        owner = new Customer();
        owner.setDocumentType(DocumentType.CC);
        owner.setDocumentNumber("1234567890");
        owner.setFirstName("Juan");
        owner.setLastName("Pérez");
        owner.setEmail("juan@correo.com");
        customers.create(owner);
        Vehicle v = new Vehicle();
        v.setOwner(owner);
        v.setMake("Toyota");
        v.setModel("Corolla");
        v.setYear(Year.now().getValue() - 4);
        v.setLicensePlate("ABC123");
        v.setFuelType(FuelType.GASOLINE);
        v.setMileage(48000);
        vehicle = vehicleService.registerVehicle(v);

        receptionist = users.create(user("Laura", Role.RECEPTIONIST));
        mechanic = users.create(user("Carlos", Role.MECHANIC));

        filter = new Part();
        filter.setCode("REP-001");
        filter.setName("Filtro de aceite");
        filter.setCurrentStock(10);
        filter.setMinimumStock(2);
        filter.setUnitPrice(new BigDecimal("35000"));
        filter = parts.create(filter);
    }

    // ---------- flujo normal ----------

    @Test
    void fullFlowDeductsStockOnRepairAndReleasesVehicleOnDelivery() {
        ServiceOrder order = createOrder(3);

        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        assertEquals(10, stock(), "Diagnóstico no toca el stock");

        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);
        assertEquals(7, stock(), "En reparación descuenta los repuestos");
        assertEquals(1, movements.findByPart(filter.getId()).size());

        workflow.changeStatus(order.getId(), OrderStatus.READY, receptionist);
        assertEquals(VehicleStatus.IN_SERVICE, vehicleStatus());

        ServiceOrder delivered = workflow.changeStatus(order.getId(), OrderStatus.DELIVERED, receptionist);
        assertEquals(OrderStatus.DELIVERED, delivered.getStatus());
        assertEquals(7, stock(), "Entregar no vuelve a mover el stock");
        assertEquals(VehicleStatus.ACTIVE, vehicleStatus(), "Al entregar, el vehículo vuelve a Activo");
        assertTrue(historyTexts(delivered).stream().anyMatch(t -> t.contains("descontados del inventario")));
    }

    @Test
    void changesArePersistedWithTheirHistory() {
        ServiceOrder order = createOrder(1);
        int before = orders.historyWrites;

        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);

        assertEquals(OrderStatus.DIAGNOSIS, orders.findById(order.getId()).getStatus());
        assertEquals(before + 1, orders.historyWrites, "Una entrada nueva en el historial, no todo de nuevo");
    }

    @Test
    void repairFailsWithoutStockAndChangesNothing() {
        ServiceOrder order = createOrder(3);
        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        partService.decreaseStock(filter.getId(), 9);

        String message = assertThrows(ValidationException.class,
                () -> workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist)).getMessage();

        assertTrue(message.contains("Filtro de aceite"), message);
        assertEquals(OrderStatus.DIAGNOSIS, orders.findById(order.getId()).getStatus());
        assertEquals(1, stock());
        assertTrue(movements.findByPart(filter.getId()).isEmpty());
    }

    // ---------- cancelación ----------

    @Test
    void cancellingAfterRepairReturnsStockAndReleasesVehicle() {
        ServiceOrder order = createOrder(4);
        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);
        assertEquals(6, stock());

        ServiceOrder cancelled = workflow.changeStatus(order.getId(), OrderStatus.CANCELLED, receptionist);

        assertEquals(10, stock(), "Los repuestos vuelven al inventario");
        assertEquals(VehicleStatus.ACTIVE, vehicleStatus());
        assertTrue(historyTexts(cancelled).stream().anyMatch(t -> t.contains("devueltos al inventario")));
    }

    @Test
    void cancellingBeforeRepairDoesNotTouchStock() {
        ServiceOrder order = createOrder(4);

        workflow.changeStatus(order.getId(), OrderStatus.CANCELLED, receptionist);

        assertEquals(10, stock());
        assertTrue(movements.findByPart(filter.getId()).isEmpty());
        assertEquals(VehicleStatus.ACTIVE, vehicleStatus());
    }

    @Test
    void invalidTransitionsAreRejected() {
        ServiceOrder order = createOrder(1);

        assertThrows(ValidationException.class,
                () -> workflow.changeStatus(order.getId(), OrderStatus.READY, receptionist));
        workflow.changeStatus(order.getId(), OrderStatus.CANCELLED, receptionist);
        assertThrows(ValidationException.class,
                () -> workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist));
        assertThrows(ValidationException.class,
                () -> workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, null));
    }

    // ---------- líneas ----------

    @Test
    void partAddedDuringRepairIsDeductedImmediately() {
        ServiceOrder order = createOrder(0);
        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);

        workflow.addPart(order.getId(), filter, 2, receptionist);

        assertEquals(8, stock());
        assertEquals(1, orders.lineWrites);
    }

    @Test
    void partAddedBeforeRepairIsDeductedLaterWithTheRest() {
        ServiceOrder order = createOrder(1);
        workflow.addPart(order.getId(), filter, 2, receptionist);
        assertEquals(10, stock(), "Antes de En reparación no se descuenta");

        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);

        assertEquals(7, stock(), "Se descuentan las 3 unidades de las dos líneas");
    }

    @Test
    void linesCannotBeAddedOnceReady() {
        ServiceOrder order = createOrder(0);
        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.READY, receptionist);

        assertThrows(ValidationException.class,
                () -> workflow.addService(order.getId(), offering(), 1, receptionist));
        assertThrows(ValidationException.class,
                () -> workflow.addPart(order.getId(), filter, 1, receptionist));
    }

    @Test
    void addServiceUsesCatalogPrice() {
        ServiceOrder order = createOrder(0);

        ServiceOrder updated = workflow.addService(order.getId(), offering(), 2, receptionist);

        assertEquals(0, new BigDecimal("240000").compareTo(updated.calculateSubtotal()));
    }

    // ---------- diagnóstico, responsable y notas ----------

    @Test
    void diagnosisIsSavedAndRecorded() {
        ServiceOrder order = createOrder(0);

        assertThrows(ValidationException.class,
                () -> workflow.saveDiagnosis(order.getId(), "  ", null, mechanic));
        ServiceOrder updated = workflow.saveDiagnosis(order.getId(), "Pastillas gastadas", "Cambiar", mechanic);

        assertEquals("Pastillas gastadas", updated.getDiagnosis().getDescription());
        assertTrue(historyTexts(updated).contains("Diagnóstico registrado"));
        assertEquals(1, orders.diagnosisWrites);
    }

    @Test
    void responsibleMustBeAnActiveMechanic() {
        ServiceOrder order = createOrder(0);

        assertThrows(ValidationException.class,
                () -> workflow.assignResponsible(order.getId(), receptionist, receptionist));
        ServiceOrder updated = workflow.assignResponsible(order.getId(), mechanic, receptionist);

        assertEquals(mechanic.getId(), updated.getResponsible().getId());
    }

    @Test
    void notesGoToHistory() {
        ServiceOrder order = createOrder(0);

        assertThrows(ValidationException.class, () -> workflow.addNote(order.getId(), "", receptionist));
        ServiceOrder updated = workflow.addNote(order.getId(), "Cliente pide revisar frenos", receptionist);

        assertTrue(historyTexts(updated).contains("Nota: Cliente pide revisar frenos"));
    }

    @Test
    void nextStatusFollowsTheFlow() {
        assertEquals(OrderStatus.DIAGNOSIS, OrderWorkflowService.nextStatus(OrderStatus.RECEIVED));
        assertEquals(OrderStatus.DELIVERED, OrderWorkflowService.nextStatus(OrderStatus.READY));
        assertNull(OrderWorkflowService.nextStatus(OrderStatus.DELIVERED));
        assertNull(OrderWorkflowService.nextStatus(OrderStatus.CANCELLED));
    }

    // ---------- ayudas ----------

    /** Crea una orden con una línea de repuesto de {@code partUnits} unidades (0 = sin repuestos). */
    // ---------- aviso «vehículo listo» (Sprint 6) ----------

    @Test
    void readyOrderNotifiesTheCustomerOnce() {
        ServiceOrder order = createOrder(0);
        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);
        assertTrue(notifications.findAll().isEmpty(), "Ningún aviso antes de Lista");

        ServiceOrder ready = workflow.changeStatus(order.getId(), OrderStatus.READY, receptionist);

        assertEquals(1, notifications.findAll().size());
        Notification sent = notifications.findAll().get(0);
        assertEquals(NotificationType.SERVICE_UPDATE, sent.getType());
        assertEquals(NotificationStatus.SENT, sent.getStatus());
        assertEquals("Tu Toyota Corolla (ABC123) está listo para entregar", sent.getSubject());
        assertTrue(sent.getMessage().startsWith("Hola Juan, terminamos el servicio de tu Toyota Corolla"),
                sent.getMessage());
        assertTrue(historyTexts(ready).contains("Se avisó al cliente que el vehículo está listo"));

        workflow.changeStatus(order.getId(), OrderStatus.DELIVERED, receptionist);
        assertEquals(1, notifications.findAll().size(), "Entregar no envía otro aviso");
    }

    @Test
    void readyWithoutCustomerEmailStillChangesStatus() {
        owner.setEmail(null);
        ServiceOrder order = createOrder(0);
        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);

        ServiceOrder ready = workflow.changeStatus(order.getId(), OrderStatus.READY, receptionist);

        assertEquals(OrderStatus.READY, ready.getStatus());
        assertEquals(NotificationStatus.FAILED, notifications.findAll().get(0).getStatus());
        assertTrue(historyTexts(ready).stream().anyMatch(t -> t.startsWith("No se pudo avisar al cliente")));
    }

    @Test
    void readyWithTheNoticeTurnedOffSendsNothing() {
        com.autogest.model.NotificationSettings off = com.autogest.model.NotificationSettings.defaults();
        off.setVehicleReadyEnabled(false);
        events.setSettings(() -> off);
        ServiceOrder order = createOrder(0);
        workflow.changeStatus(order.getId(), OrderStatus.DIAGNOSIS, receptionist);
        workflow.changeStatus(order.getId(), OrderStatus.IN_REPAIR, receptionist);

        ServiceOrder ready = workflow.changeStatus(order.getId(), OrderStatus.READY, receptionist);

        assertEquals(OrderStatus.READY, ready.getStatus());
        assertTrue(notifications.findAll().isEmpty(), "Con el aviso apagado no se envía nada");
        assertTrue(historyTexts(ready).stream().noneMatch(t -> t.contains("avis")), historyTexts(ready).toString());
    }

    private ServiceOrder createOrder(int partUnits) {
        ServiceOrder order = new ServiceOrder();
        order.setVehicle(vehicle);
        order.setEntryReason("Mantenimiento preventivo");
        if (partUnits > 0) {
            order.addLine(orderService.buildPartLine(partService.getDetail(filter.getId()), partUnits));
        }
        return orderService.createOrder(order, null, receptionist);
    }

    private int stock() {
        return partService.getDetail(filter.getId()).getCurrentStock();
    }

    private VehicleStatus vehicleStatus() {
        return vehicleService.getDetail(vehicle.getId()).getStatus();
    }

    private static List<String> historyTexts(ServiceOrder order) {
        List<String> texts = new ArrayList<>();
        for (OrderHistory entry : order.getHistory()) {
            texts.add(entry.getDescription());
        }
        return texts;
    }

    private static ServiceOffering offering() {
        ServiceOffering offering = new ServiceOffering();
        offering.setId(1);
        offering.setName("Cambio de aceite");
        offering.setPrice(new BigDecimal("120000"));
        offering.setStatus(ServiceOfferingStatus.ACTIVE);
        return offering;
    }

    private static User user(String name, Role role) {
        User user = new User();
        user.setFirstName(name);
        user.setLastName("Martínez");
        user.setEmail(name.toLowerCase() + "@autogest.com");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    /**
     * Repositorio falso de Dev 1 más las escrituras que usa el seguimiento
     * (líneas, historial y diagnóstico). Solo las cuenta: el objeto en memoria
     * ya refleja el cambio.
     */
    private static final class RecordingOrderRepository extends FakeServiceOrderRepository {

        int lineWrites;
        int historyWrites;
        int diagnosisWrites;

        @Override
        public void addLine(int orderId, OrderLine line) {
            lineWrites++;
        }

        @Override
        public void addHistory(int orderId, OrderHistory entry) {
            historyWrites++;
        }

        @Override
        public void saveDiagnosis(int orderId, com.autogest.model.Diagnosis diagnosis) {
            diagnosisWrites++;
        }
    }
}
