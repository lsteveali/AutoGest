package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.Catalogs.FuelType;
import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;
import com.autogest.model.Catalogs.ServiceOfferingStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.model.Vehicle;
import com.autogest.model.Catalogs.VehicleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link ServiceOrderService} (creación, líneas, búsquedas y
 * conteos) con repositorios falsos y el {@link VehicleService} real.
 */
class ServiceOrderServiceTest {

    private FakeServiceOrderRepository repository;
    private VehicleService vehicleService;
    private ServiceOrderService service;
    private Vehicle vehicle;
    private User receptionist;
    private User mechanic;
    private int plateSequence;

    @BeforeEach
    void setUp() {
        FakeCustomerRepository customers = new FakeCustomerRepository();
        vehicleService = new VehicleService(new FakeVehicleRepository(), customers);
        repository = new FakeServiceOrderRepository();
        service = new ServiceOrderService(repository, vehicleService);

        Customer owner = new Customer();
        owner.setDocumentType(DocumentType.CC);
        owner.setDocumentNumber("1234567890");
        owner.setFirstName("Juan");
        owner.setLastName("Pérez");
        customers.create(owner);
        vehicle = registerVehicle(owner);

        receptionist = user(1, Role.RECEPTIONIST);
        mechanic = user(2, Role.MECHANIC);
    }

    // ---------- ayudas ----------

    private Vehicle registerVehicle(Customer owner) {
        Vehicle v = new Vehicle();
        v.setOwner(owner);
        v.setMake("Toyota");
        v.setModel("Corolla");
        v.setYear(Year.now().getValue() - 4);
        v.setLicensePlate("ABC12" + (plateSequence++));
        v.setFuelType(FuelType.GASOLINE);
        v.setMileage(48000);
        return vehicleService.registerVehicle(v);
    }

    private static User user(int id, Role role) {
        User user = new User();
        user.setId(id);
        user.setFirstName(role == Role.MECHANIC ? "Carlos" : "Laura");
        user.setLastName("Martínez");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private ServiceOrder draft() {
        ServiceOrder order = new ServiceOrder();
        order.setVehicle(vehicle);
        order.setEntryReason("  Mantenimiento preventivo ");
        order.setResponsible(mechanic);
        return order;
    }

    private static ServiceOffering offering(String name, String price, ServiceOfferingStatus status) {
        ServiceOffering offering = new ServiceOffering();
        offering.setId(name.length());
        offering.setName(name);
        offering.setPrice(new BigDecimal(price));
        offering.setStatus(status);
        return offering;
    }

    private static Part part(int stock) {
        Part part = new Part();
        part.setId(9);
        part.setName("Filtro de aceite");
        part.setCurrentStock(stock);
        part.setUnitPrice(new BigDecimal("35000"));
        return part;
    }

    private String errorOf(ServiceOrder order) {
        return assertThrows(ValidationException.class,
                () -> service.createOrder(order, null, receptionist)).getMessage();
    }

    // ---------- creación ----------

    @Test
    void createOrderStartsReceivedWithNumberHistoryAndToday() {
        ServiceOrder saved = service.createOrder(draft(), null, receptionist);
        assertTrue(saved.getId() > 0);
        assertEquals(OrderStatus.RECEIVED, saved.getStatus());
        assertEquals("0001", saved.getNumber());
        assertEquals(LocalDate.now(), saved.getEntryDate());
        assertEquals("Mantenimiento preventivo", saved.getEntryReason());
        assertEquals(1, saved.getHistory().size());
        assertEquals(receptionist, saved.getHistory().get(0).getUser());
    }

    @Test
    void createOrderPutsVehicleInService() {
        service.createOrder(draft(), null, receptionist);
        assertEquals(VehicleStatus.IN_SERVICE, vehicleService.getDetail(vehicle.getId()).getStatus());
    }

    @Test
    void createOrderUpdatesHigherMileage() {
        service.createOrder(draft(), 48230, receptionist);
        assertEquals(48230, vehicleService.getDetail(vehicle.getId()).getMileage());
    }

    @Test
    void createOrderRejectsLowerMileage() {
        assertThrows(ValidationException.class, () -> service.createOrder(draft(), 100, receptionist));
    }

    @Test
    void createOrderRequiresVehicleReasonAndUser() {
        ServiceOrder noVehicle = draft();
        noVehicle.setVehicle(null);
        assertEquals("Selecciona el vehículo", errorOf(noVehicle));

        ServiceOrder noReason = draft();
        noReason.setEntryReason("  ");
        assertEquals("El motivo de ingreso es obligatorio", errorOf(noReason));

        assertThrows(ValidationException.class, () -> service.createOrder(draft(), null, null));
    }

    @Test
    void createOrderRejectsUnknownVehicle() {
        ServiceOrder order = draft();
        Vehicle ghost = new Vehicle();
        ghost.setId(99);
        order.setVehicle(ghost);
        assertThrows(NotFoundException.class, () -> service.createOrder(order, null, receptionist));
    }

    @Test
    void createOrderRejectsInactiveVehicle() {
        vehicleService.deactivateVehicle(vehicle.getId());
        assertEquals("El vehículo está dado de baja", errorOf(draft()));
    }

    @Test
    void createOrderRejectsSecondOpenOrderForSameVehicle() {
        service.createOrder(draft(), null, receptionist);
        assertEquals("El vehículo ya tiene una orden de servicio abierta", errorOf(draft()));
    }

    @Test
    void createOrderValidatesDates() {
        ServiceOrder future = draft();
        future.setEntryDate(LocalDate.now().plusDays(1));
        assertEquals("La fecha de ingreso no puede ser futura", errorOf(future));

        ServiceOrder delivery = draft();
        delivery.setEntryDate(LocalDate.now());
        delivery.setEstimatedDeliveryDate(LocalDate.now().minusDays(1));
        assertEquals("La entrega estimada no puede ser anterior a la fecha de ingreso", errorOf(delivery));
    }

    @Test
    void createOrderRequiresActiveMechanicAsResponsible() {
        ServiceOrder notMechanic = draft();
        notMechanic.setResponsible(receptionist);
        assertEquals("El responsable de la orden debe ser un mecánico", errorOf(notMechanic));

        ServiceOrder inactive = draft();
        User retired = user(3, Role.MECHANIC);
        retired.setStatus(UserStatus.INACTIVE);
        inactive.setResponsible(retired);
        assertEquals("El mecánico seleccionado está inactivo", errorOf(inactive));
    }

    @Test
    void createOrderAllowsNoResponsible() {
        ServiceOrder order = draft();
        order.setResponsible(null);
        assertEquals(OrderStatus.RECEIVED, service.createOrder(order, null, receptionist).getStatus());
    }

    @Test
    void createOrderKeepsLinesAndTotal() {
        ServiceOrder order = draft();
        order.addLine(service.buildServiceLine(offering("Cambio de aceite", "120000", ServiceOfferingStatus.ACTIVE), 1));
        order.addLine(service.buildPartLine(part(10), 2));
        ServiceOrder saved = service.createOrder(order, null, receptionist);
        assertEquals(new BigDecimal("190000.00"), saved.calculateTotal());
    }

    // ---------- líneas ----------

    @Test
    void buildServiceLineUsesCatalogPrice() {
        ServiceLine line = service.buildServiceLine(offering("Revisión", "80000", ServiceOfferingStatus.ACTIVE), 1);
        assertEquals(new BigDecimal("80000.00"), line.getUnitPrice());
        assertEquals("Revisión", line.getDescription());
    }

    @Test
    void buildServiceLineRejectsInactiveOrMissingService() {
        assertThrows(ValidationException.class, () -> service.buildServiceLine(
                offering("Viejo", "1", ServiceOfferingStatus.INACTIVE), 1));
        assertThrows(ValidationException.class, () -> service.buildServiceLine(null, 1));
    }

    @Test
    void buildPartLineChecksStockAndQuantity() {
        PartLine line = service.buildPartLine(part(3), 3);
        assertEquals(new BigDecimal("105000.00"), line.calculateTotal());
        assertThrows(ValidationException.class, () -> service.buildPartLine(part(3), 4));
        assertThrows(ValidationException.class, () -> service.buildPartLine(part(3), 0));
    }

    // ---------- consultas ----------

    @Test
    void searchByPlateCustomerAndNumber() {
        service.createOrder(draft(), null, receptionist);
        assertEquals(1, service.searchOrders(vehicle.getLicensePlate(), null, 0).size());
        assertEquals(1, service.searchOrders("juan", null, 0).size());
        assertEquals(1, service.searchOrders("#0001", null, 0).size());
        assertEquals(0, service.searchOrders("zzz", null, 0).size());
    }

    @Test
    void searchByStatusAndResponsible() {
        service.createOrder(draft(), null, receptionist);
        assertEquals(1, service.searchOrders(null, OrderStatus.RECEIVED, mechanic.getId()).size());
        assertEquals(0, service.searchOrders(null, OrderStatus.READY, 0).size());
        assertEquals(0, service.searchOrders(null, null, 77).size());
    }

    @Test
    void countsActiveAndByStatus() {
        service.createOrder(draft(), null, receptionist);
        vehicle = registerVehicle(vehicle.getOwner());
        ServiceOrder second = service.createOrder(draft(), null, receptionist);
        second.setStatus(OrderStatus.DELIVERED);
        assertEquals(1, service.countActive());
        assertEquals(1, service.countByStatus(OrderStatus.RECEIVED));
        assertThrows(ValidationException.class, () -> service.countByStatus(null));
    }

    @Test
    void findByVehicleAndCustomer() {
        ServiceOrder saved = service.createOrder(draft(), null, receptionist);
        List<ServiceOrder> byVehicle = service.findByVehicle(vehicle.getId());
        assertEquals(saved.getId(), byVehicle.get(0).getId());
        assertEquals(1, service.findByCustomer(vehicle.getOwner().getId()).size());
    }

    @Test
    void getDetailOfUnknownOrderThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> service.getDetail(5));
    }

    @Test
    void formatNumberPadsToFourDigits() {
        assertEquals("0145", ServiceOrderService.formatNumber(145));
        assertEquals("12345", ServiceOrderService.formatNumber(12345));
    }
}
