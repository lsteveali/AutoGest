package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.DocumentType;
import com.autogest.model.FuelType;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de las plantillas ({@link NotificationTemplate}) y de los avisos
 * automáticos ({@link NotificationEventService}) con el
 * {@link NotificationService} real de Dev 1 sobre repositorios falsos.
 */
class NotificationEventServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 16);

    private FakeCustomerRepository customers;
    private FakeNotificationRepository notifications;
    private VehicleService vehicleService;
    private NotificationEventService events;
    private int plateSequence;

    @BeforeEach
    void setUp() {
        customers = new FakeCustomerRepository();
        notifications = new FakeNotificationRepository();
        vehicleService = new VehicleService(new FakeVehicleRepository(), customers);
        events = new NotificationEventService(
                new NotificationService(notifications, new CustomerService(customers)), vehicleService);
    }

    // ---------- plantillas ----------

    @Test
    void templateFillsItsFields() {
        Map<String, String> values = NotificationTemplate.baseValues();
        values.put("cliente", "Ana");
        values.put("vehiculo", "Mazda 3");
        values.put("placa", "XYZ789");
        values.put("fecha", "20/11/2026");

        assertEquals("Mantenimiento de tu Mazda 3 (XYZ789)", NotificationTemplate.MAINTENANCE_DUE.subject(values));
        assertEquals("Hola Ana, te recordamos que el mantenimiento de tu Mazda 3 de placa XYZ789 está programado "
                + "para el 20/11/2026. Escríbenos para agendar tu cita en AutoGest.",
                NotificationTemplate.MAINTENANCE_DUE.message(values));
    }

    @Test
    void missingValuesLeaveNoBracesNorDoubleSpaces() {
        Map<String, String> values = NotificationTemplate.baseValues();
        values.put("cliente", "Ana");

        String subject = NotificationTemplate.VEHICLE_READY.subject(values);

        assertFalse(subject.contains("{"), subject);
        assertFalse(subject.contains("  "), subject);
    }

    @Test
    void unknownFieldIsRejected() {
        assertThrows(ValidationException.class, () -> NotificationTemplate.fill("Hola {apodo}", Map.of()));
    }

    @Test
    void everyTemplateUsesOnlyKnownFields() {
        for (NotificationTemplate template : NotificationTemplate.values()) {
            NotificationTemplate.fill(template.getSubject(), Map.of());
            NotificationTemplate.fill(template.getMessage(), Map.of());
        }
    }

    // ---------- vehículo listo ----------

    @Test
    void vehicleReadyIsSentToTheOwnerWithTheOrder() {
        Vehicle vehicle = vehicle("ana@correo.com", null);
        ServiceOrder order = new ServiceOrder();
        order.setId(9);
        order.setNumber("0045");
        order.setVehicle(vehicle);

        Notification sent = events.notifyVehicleReady(order);

        assertEquals(NotificationType.SERVICE_UPDATE, sent.getType());
        assertEquals(NotificationStatus.SENT, sent.getStatus());
        assertEquals(9, sent.getOrder().getId());
        assertTrue(sent.getMessage().contains("(orden #0045)"), sent.getMessage());
    }

    @Test
    void vehicleWithoutOwnerCannotBeNotified() {
        ServiceOrder order = new ServiceOrder();
        order.setVehicle(new Vehicle());

        assertThrows(ValidationException.class, () -> events.notifyVehicleReady(order));
    }

    // ---------- recordatorios de mantenimiento ----------

    @Test
    void remindersCoverDueAndSoonVehiclesOnly() {
        Vehicle overdue = vehicle("a@correo.com", TODAY.minusDays(10));
        Vehicle soon = vehicle("b@correo.com", TODAY.plusDays(5));
        vehicle("c@correo.com", TODAY.plusDays(90));
        vehicle("d@correo.com", null);

        List<Vehicle> pending = events.pendingMaintenanceReminders(TODAY);

        assertEquals(List.of(overdue.getId(), soon.getId()), pending.stream().map(Vehicle::getId).toList(),
                "Del más urgente al menos urgente");
    }

    @Test
    void inactiveVehiclesAreNotReminded() {
        Vehicle inactive = vehicle("a@correo.com", TODAY.minusDays(1));
        inactive.setStatus(VehicleStatus.INACTIVE);

        assertTrue(events.pendingMaintenanceReminders(TODAY).isEmpty());
    }

    @Test
    void sendingRemindersCreatesOneNoticePerVehicleAndThenNone() {
        vehicle("a@correo.com", TODAY.minusDays(1));
        vehicle("b@correo.com", TODAY.plusDays(3));

        assertEquals(2, events.sendMaintenanceReminders(TODAY));
        assertTrue(notifications.findAll().stream()
                .allMatch(n -> n.getType() == NotificationType.MAINTENANCE_REMINDER));

        assertEquals(0, events.sendMaintenanceReminders(TODAY), "No se repite dentro de la ventana");
        assertTrue(events.pendingMaintenanceReminders(TODAY).isEmpty());
    }

    @Test
    void reminderIsRepeatedAfterTheWindow() {
        vehicle("a@correo.com", TODAY.minusDays(1));
        events.sendMaintenanceReminders(TODAY);

        LocalDate later = LocalDate.now().plusDays(NotificationEventService.REMINDER_WINDOW_DAYS + 1);

        assertEquals(1, events.pendingMaintenanceReminders(later).size());
    }

    @Test
    void twoVehiclesOfTheSameOwnerGetTheirOwnReminder() {
        Vehicle first = vehicle("a@correo.com", TODAY.minusDays(1));
        Vehicle second = vehicle(null, TODAY.minusDays(2));
        second.setOwner(first.getOwner());

        assertEquals(2, events.sendMaintenanceReminders(TODAY));
    }

    @Test
    void ownerWithoutEmailLeavesAFailedReminder() {
        vehicle(null, TODAY.minusDays(1));

        assertEquals(1, events.sendMaintenanceReminders(TODAY));
        assertEquals(NotificationStatus.FAILED, notifications.findAll().get(0).getStatus());
        assertTrue(events.pendingMaintenanceReminders(TODAY).isEmpty(), "Se reintenta desde Notificaciones");
    }

    // ---------- ayudas ----------

    /** Vehículo registrado con su propio cliente y la fecha de mantenimiento dada. */
    private Vehicle vehicle(String email, LocalDate nextMaintenance) {
        Customer owner = new Customer();
        owner.setDocumentType(DocumentType.CC);
        owner.setDocumentNumber("10" + (1000 + plateSequence));
        owner.setFirstName("Cliente" + plateSequence);
        owner.setLastName("Prueba");
        owner.setEmail(email);
        customers.create(owner);
        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(owner);
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setYear(Year.now().getValue() - 3);
        vehicle.setLicensePlate("ABC" + (100 + plateSequence++));
        vehicle.setFuelType(FuelType.GASOLINE);
        vehicle.setMileage(30000);
        Vehicle saved = vehicleService.registerVehicle(vehicle);
        saved.setNextMaintenanceDate(nextMaintenance);
        return saved;
    }
}
