package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Catalogs.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationType;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import com.autogest.model.Catalogs.VehicleStatus;
import com.autogest.util.Formatter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Eventos que avisan solos al cliente (Sprint 6, parte del Líder), con las
 * plantillas de {@link NotificationTemplate} y el envío del
 * {@link NotificationService} de Dev 1:
 *
 * <ul>
 *   <li><b>Vehículo listo:</b> cuando una orden pasa a Lista
 *       ({@link OrderWorkflowService} lo dispara).</li>
 *   <li><b>Recordatorio de mantenimiento:</b> para los vehículos con el
 *       mantenimiento vencido o por vencer (D-13), una sola vez cada
 *       {@value #REMINDER_WINDOW_DAYS} días por vehículo. Se envían desde el
 *       Inicio con «Enviar recordatorios».</li>
 * </ul>
 *
 * <p>Los avisos van por correo (canal EMAIL); si el cliente no tiene correo la
 * notificación queda «Fallida» y se reintenta desde Notificaciones.</p>
 *
 * <p>Principles: GRASP Controller (de caso de uso), Indirection (los módulos
 * disparan eventos sin conocer las plantillas), SRP, DIP.</p>
 */
public class NotificationEventService {

    /** Días durante los que no se repite el recordatorio de un mismo vehículo. */
    public static final int REMINDER_WINDOW_DAYS = 30;

    private final NotificationService notificationService;
    private final VehicleService vehicleService;

    /**
     * @param notificationService creación y envío de notificaciones (Dev 1)
     * @param vehicleService      vehículos con mantenimiento próximo
     */
    public NotificationEventService(NotificationService notificationService, VehicleService vehicleService) {
        this.notificationService = notificationService;
        this.vehicleService = vehicleService;
    }

    private java.util.function.Supplier<com.autogest.model.NotificationSettings> settings =
            com.autogest.model.NotificationSettings::defaults;

    /**
     * Define de dónde sale la configuración de notificaciones (qué avisos
     * automáticos están activos). Sin llamarlo, todos están activos.
     *
     * @param settings proveedor de la configuración vigente
     */
    public void setSettings(java.util.function.Supplier<com.autogest.model.NotificationSettings> settings) {
        this.settings = settings == null ? com.autogest.model.NotificationSettings::defaults : settings;
    }

    /**
     * @return {@code true} si el aviso «vehículo listo» está activo en Configuración
     */
    public boolean isVehicleReadyEnabled() {
        try {
            return settings.get().isVehicleReadyEnabled();
        } catch (RuntimeException e) {
            return true;
        }
    }

    // ---------- vehículo listo ----------

    /**
     * Avisa al propietario que su vehículo está listo para entregar.
     *
     * @param order orden completa (con vehículo y propietario)
     * @return la notificación enviada (o fallida si el cliente no tiene correo)
     * @throws ValidationException si la orden no tiene vehículo o propietario
     */
    public Notification notifyVehicleReady(ServiceOrder order) {
        Vehicle vehicle = order == null ? null : order.getVehicle();
        Customer owner = requireOwner(vehicle);
        Map<String, String> values = values(owner, vehicle);
        values.put("orden", order.getNumber() == null ? "" : "#" + order.getNumber());
        Notification notification = build(NotificationTemplate.VEHICLE_READY, owner, values);
        notification.setOrder(order);
        return notificationService.createAndSend(notification);
    }

    // ---------- recordatorios de mantenimiento ----------

    /**
     * Vehículos que necesitan recordatorio: mantenimiento vencido o por vencer,
     * no dados de baja, con propietario y sin recordatorio en los últimos
     * {@value #REMINDER_WINDOW_DAYS} días. Del más urgente al menos urgente.
     *
     * @param today fecha de referencia
     * @return vehículos por recordar
     */
    public List<Vehicle> pendingMaintenanceReminders(LocalDate today) {
        Map<Integer, List<Notification>> byCustomer = new HashMap<>();
        List<Vehicle> pending = new ArrayList<>();
        for (Vehicle vehicle : vehicleService.searchVehicles("", null, null, null)) {
            if (!vehicle.isMaintenanceDue(today) || vehicle.getStatus() == VehicleStatus.INACTIVE
                    || vehicle.getOwner() == null || vehicle.getOwner().getId() <= 0) {
                continue;
            }
            List<Notification> sent = byCustomer.computeIfAbsent(vehicle.getOwner().getId(),
                    notificationService::findByCustomer);
            if (!alreadyReminded(sent, vehicle, today)) {
                pending.add(vehicle);
            }
        }
        pending.sort(Comparator.comparing(Vehicle::getNextMaintenanceDate));
        return pending;
    }

    /**
     * Envía el recordatorio a cada vehículo pendiente. Si uno falla (cliente
     * borrado, error de conexión), sigue con los demás.
     *
     * @param today fecha de referencia
     * @return cuántos recordatorios se registraron
     */
    public int sendMaintenanceReminders(LocalDate today) {
        int created = 0;
        for (Vehicle vehicle : pendingMaintenanceReminders(today)) {
            try {
                notifyMaintenanceDue(vehicle);
                created++;
            } catch (RuntimeException ignored) {
                // Un recordatorio que no se pudo crear no detiene los demás.
            }
        }
        return created;
    }

    /**
     * Envía el recordatorio de mantenimiento de un vehículo.
     *
     * @param vehicle vehículo con propietario y fecha de mantenimiento
     * @return la notificación enviada (o fallida si el cliente no tiene correo)
     */
    public Notification notifyMaintenanceDue(Vehicle vehicle) {
        Customer owner = requireOwner(vehicle);
        Map<String, String> values = values(owner, vehicle);
        values.put("fecha", Formatter.date(vehicle.getNextMaintenanceDate()));
        return notificationService.createAndSend(build(NotificationTemplate.MAINTENANCE_DUE, owner, values));
    }

    // ---------- ayudas ----------

    /** Ya se recordó si hay un recordatorio de ese vehículo (por su placa) dentro de la ventana. */
    private static boolean alreadyReminded(List<Notification> sent, Vehicle vehicle, LocalDate today) {
        LocalDate since = today.minusDays(REMINDER_WINDOW_DAYS);
        String plate = vehicle.getLicensePlate();
        for (Notification notification : sent) {
            boolean sameVehicle = plate != null && notification.getSubject() != null
                    && notification.getSubject().contains(plate);
            boolean recent = notification.getScheduledDate() != null
                    && !notification.getScheduledDate().toLocalDate().isBefore(since);
            if (notification.getType() == NotificationType.MAINTENANCE_REMINDER && sameVehicle && recent) {
                return true;
            }
        }
        return false;
    }

    private static Notification build(NotificationTemplate template, Customer owner, Map<String, String> values) {
        Notification notification = new Notification();
        notification.setType(template.getType());
        notification.setChannel(Channel.EMAIL);
        notification.setCustomer(owner);
        notification.setSubject(template.subject(values));
        notification.setMessage(template.message(values));
        return notification;
    }

    private static Map<String, String> values(Customer owner, Vehicle vehicle) {
        Map<String, String> values = NotificationTemplate.baseValues();
        values.put("cliente", owner.getFirstName());
        values.put("vehiculo", ((vehicle.getMake() == null ? "" : vehicle.getMake()) + " "
                + (vehicle.getModel() == null ? "" : vehicle.getModel())).trim());
        values.put("placa", vehicle.getLicensePlate());
        return values;
    }

    private static Customer requireOwner(Vehicle vehicle) {
        if (vehicle == null || vehicle.getOwner() == null || vehicle.getOwner().getId() <= 0) {
            throw new ValidationException("El vehículo debe tener propietario para avisarle");
        }
        return vehicle.getOwner();
    }
}
