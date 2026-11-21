package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.autogest.exception.ValidationException;
import com.autogest.model.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.FuelType;
import com.autogest.model.MovementType;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;
import com.autogest.model.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.model.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las consultas de apoyo para Reportes (Sprint 7) de órdenes,
 * inventario y notificaciones, con repositorios falsos.
 */
class ReportQueriesTest {

    private static final LocalDate TODAY = LocalDate.now();

    private FakeCustomerRepository customers;
    private VehicleService vehicleService;
    private ServiceOrderService orders;
    private Customer owner;
    private User mechanic;
    private User receptionist;
    private int plate;

    @BeforeEach
    void setUp() {
        customers = new FakeCustomerRepository();
        vehicleService = new VehicleService(new FakeVehicleRepository(), customers);
        orders = new ServiceOrderService(new FakeServiceOrderRepository(), vehicleService);
        owner = new Customer();
        owner.setDocumentType(DocumentType.CC);
        owner.setDocumentNumber("555");
        owner.setFirstName("Juan");
        owner.setLastName("Pérez");
        owner.setEmail("juan@example.com");
        customers.create(owner);
        mechanic = user(2, Role.MECHANIC, "Carlos");
        receptionist = user(1, Role.RECEPTIONIST, "Laura");
    }

    private static User user(int id, Role role, String name) {
        User user = new User();
        user.setId(id);
        user.setFirstName(name);
        user.setLastName("Martínez");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Vehicle newVehicle() {
        Vehicle v = new Vehicle();
        v.setOwner(owner);
        v.setMake("Mazda");
        v.setModel("CX-5");
        v.setYear(Year.now().getValue() - 2);
        v.setLicensePlate("XYZ78" + (plate++));
        v.setFuelType(FuelType.GASOLINE);
        return vehicleService.registerVehicle(v);
    }

    private ServiceOrder order(String service, int parts, User responsible) {
        ServiceOrder order = new ServiceOrder();
        order.setVehicle(newVehicle());
        order.setEntryReason("Revisión");
        order.setResponsible(responsible);
        ServiceOffering offering = new ServiceOffering();
        offering.setId(service.length());
        offering.setName(service);
        offering.setPrice(new BigDecimal("100000"));
        offering.setStatus(ServiceOfferingStatus.ACTIVE);
        order.addLine(orders.buildServiceLine(offering, 1));
        if (parts > 0) {
            Part part = new Part();
            part.setId(1);
            part.setName("Filtro de aceite");
            part.setCurrentStock(50);
            part.setUnitPrice(new BigDecimal("35000"));
            order.addLine(orders.buildPartLine(part, parts));
        }
        return orders.createOrder(order, null, receptionist);
    }

    // ---------- órdenes ----------

    @Test
    void ordersGroupedByStatusIncludeEveryStatus() {
        order("Cambio de aceite", 0, mechanic);
        Map<OrderStatus, Integer> counts = orders.countGroupedByStatus();
        assertEquals(OrderStatus.values().length, counts.size());
        assertEquals(1, counts.get(OrderStatus.RECEIVED));
        assertEquals(0, counts.get(OrderStatus.DELIVERED));
    }

    @Test
    void ordersBetweenAndByMonth() {
        order("Cambio de aceite", 0, mechanic);
        order("Revisión de frenos", 0, null);
        assertEquals(2, orders.countOrdersBetween(TODAY.minusDays(1), TODAY));
        assertEquals(0, orders.countOrdersBetween(TODAY.plusDays(1), TODAY.plusDays(2)));
        assertEquals(Map.of(YearMonth.from(TODAY), 2), orders.countOrdersByMonth(TODAY.withDayOfMonth(1), TODAY));
    }

    @Test
    void topServicesAndPartsAreSortedAndLimited() {
        order("Cambio de aceite", 2, mechanic);
        order("Cambio de aceite", 1, mechanic);
        order("Revisión de frenos", 0, mechanic);
        Map<String, Integer> services = orders.findTopServices(TODAY, TODAY, 1);
        assertEquals(List.of("Cambio de aceite"), List.copyOf(services.keySet()));
        assertEquals(2, services.get("Cambio de aceite"));
        assertEquals(3, orders.findTopParts(TODAY, TODAY, 5).get("Filtro de aceite"));
    }

    @Test
    void ordersByMechanicAndEstimatedRevenue() {
        order("Cambio de aceite", 2, mechanic);
        order("Revisión de frenos", 0, null);
        Map<String, Integer> byMechanic = orders.countOrdersByMechanic(TODAY, TODAY);
        assertEquals(1, byMechanic.get("Carlos Martínez"));
        assertEquals(1, byMechanic.get("Sin asignar"));
        assertEquals(new BigDecimal("270000.00"), orders.estimatedRevenueBetween(TODAY, TODAY));
    }

    @Test
    void periodAndLimitAreValidated() {
        assertThrows(ValidationException.class, () -> orders.countOrdersBetween(null, TODAY));
        assertThrows(ValidationException.class, () -> orders.countOrdersBetween(TODAY, TODAY.minusDays(1)));
        assertThrows(ValidationException.class, () -> orders.findTopServices(TODAY, TODAY, 0));
        assertThrows(ValidationException.class, () -> orders.findTopParts(TODAY, TODAY, 21));
    }

    // ---------- inventario ----------

    @Test
    void movementsSummaryByType() {
        PartService partService = new PartService(new FakePartRepository());
        InventoryService inventory = new InventoryService(new FakeInventoryMovementRepository(), partService);
        Part part = new Part();
        part.setCode("REP-001");
        part.setName("Filtro");
        part.setUnitPrice(BigDecimal.ONE);
        Part saved = partService.registerPart(part);
        inventory.registerEntry(saved.getId(), 10, null, receptionist);
        inventory.registerExit(saved.getId(), 4, null, receptionist);
        Map<MovementType, Integer> summary = inventory.summarizeMovements(TODAY, TODAY);
        assertEquals(10, summary.get(MovementType.ENTRY));
        assertEquals(4, summary.get(MovementType.EXIT));
        assertEquals(0, summary.get(MovementType.ADJUSTMENT));
        assertThrows(ValidationException.class, () -> inventory.summarizeMovements(TODAY, null));
    }

    // ---------- notificaciones ----------

    @Test
    void notificationsByChannelAndDeliveryRate() {
        NotificationService notifications = new NotificationService(new FakeNotificationRepository(),
                new CustomerService(customers));
        notifications.createAndSend(notification(Channel.EMAIL));
        notifications.createAndSend(notification(Channel.SYSTEM));
        notifications.createAndSend(notification(Channel.WHATSAPP));
        assertEquals(1, notifications.countByChannel().get(Channel.WHATSAPP));
        assertEquals(2, notifications.countGroupedByStatus().get(NotificationStatus.SENT));
        assertEquals(67, notifications.deliveryRate());
    }

    @Test
    void deliveryRateIsZeroWithoutAttempts() {
        NotificationService notifications = new NotificationService(new FakeNotificationRepository(),
                new CustomerService(customers));
        assertEquals(0, notifications.deliveryRate());
    }

    private Notification notification(Channel channel) {
        Notification notification = new Notification();
        notification.setCustomer(owner);
        notification.setType(NotificationType.GENERAL);
        notification.setChannel(channel);
        notification.setMessage("Hola");
        return notification;
    }
}
