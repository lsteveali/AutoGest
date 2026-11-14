package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.OrderLine;
import com.autogest.model.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.Role;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.UserStatus;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleStatus;
import com.autogest.repository.ServiceOrderRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Casos de uso de las órdenes de servicio del Desarrollador 1 (Sprint 4):
 * crear la orden (ingreso del vehículo), armar sus líneas, buscar, consultar y
 * contar. El cambio de estado, el historial posterior y el descuento de stock
 * los agrega el Líder en este mismo Service.
 *
 * <p>Principles: GRASP Creator (crea la orden porque recibe vehículo,
 * responsable y motivo), GRASP Controller, Low Coupling (usa
 * {@link VehicleService}, no su Repository), SRP, DIP.</p>
 */
public class ServiceOrderService {

    /** Máximo de caracteres del motivo (columna {@code entry_reason VARCHAR(255)}). */
    private static final int MAX_REASON = 255;

    private final ServiceOrderRepository repository;
    private final VehicleService vehicleService;

    /**
     * @param repository     repositorio de órdenes
     * @param vehicleService servicio de vehículos (estado y kilometraje)
     */
    public ServiceOrderService(ServiceOrderRepository repository, VehicleService vehicleService) {
        this.repository = repository;
        this.vehicleService = vehicleService;
    }

    /**
     * Crea una orden en estado {@code RECEIVED}: valida los datos, genera el
     * número, registra «Orden creada» en el historial, guarda todo en una
     * transacción y pasa el vehículo a {@code IN_SERVICE}.
     *
     * @param order          datos de la orden (vehículo, motivo, fechas, responsable y líneas)
     * @param currentMileage kilometraje al ingreso ({@code null} si no se registró)
     * @param createdBy      usuario que crea la orden
     * @return la orden guardada con su id y número
     * @throws ValidationException si algún dato no cumple las reglas
     * @throws NotFoundException   si el vehículo no existe
     */
    public ServiceOrder createOrder(ServiceOrder order, Integer currentMileage, User createdBy) {
        if (order == null) {
            throw new ValidationException("La orden es obligatoria");
        }
        if (createdBy == null) {
            throw new ValidationException("Debe haber un usuario en sesión para crear la orden");
        }
        Vehicle vehicle = validateVehicle(order.getVehicle());
        validateReason(order);
        validateDates(order);
        validateResponsible(order.getResponsible());
        for (OrderLine line : order.getLines()) {
            if (line.getQuantity() <= 0) {
                throw new ValidationException("La cantidad de cada línea debe ser mayor que cero");
            }
        }
        if (currentMileage != null && currentMileage < vehicle.getMileage()) {
            throw new ValidationException("El kilometraje no puede ser menor al registrado ("
                    + vehicle.getMileage() + " km)");
        }

        order.setVehicle(vehicle);
        order.setStatus(OrderStatus.RECEIVED);
        order.setNumber(formatNumber(repository.nextSequence()));
        order.setObservations(blankToNull(order.getObservations()));
        order.recordHistory("Orden creada: " + order.getEntryReason(), createdBy);

        ServiceOrder saved = repository.create(order);
        if (currentMileage != null && currentMileage > vehicle.getMileage()) {
            vehicleService.updateMileage(vehicle.getId(), currentMileage);
        }
        vehicleService.changeStatus(vehicle.getId(), VehicleStatus.IN_SERVICE);
        return saved;
    }

    /**
     * Crea una línea de servicio con el precio del catálogo.
     *
     * @param offering servicio del catálogo (debe estar activo)
     * @param quantity cantidad (mayor que cero)
     * @return línea lista para agregar a la orden
     */
    public ServiceLine buildServiceLine(ServiceOffering offering, int quantity) {
        if (offering == null) {
            throw new ValidationException("Selecciona un servicio");
        }
        if (!offering.isActive()) {
            throw new ValidationException("El servicio " + offering.getName() + " está inactivo");
        }
        requirePositive(quantity);
        ServiceLine line = new ServiceLine();
        line.setServiceOffering(offering);
        line.setQuantity(quantity);
        line.setUnitPrice(offering.getPrice());
        return line;
    }

    /**
     * Crea una línea de repuesto con el precio unitario del inventario. Valida
     * que haya stock; el descuento real del stock lo hace el Líder al avanzar
     * la orden (integración de stock).
     *
     * @param part     repuesto
     * @param quantity cantidad (mayor que cero y sin superar el stock)
     * @return línea lista para agregar a la orden
     */
    public PartLine buildPartLine(Part part, int quantity) {
        if (part == null) {
            throw new ValidationException("Selecciona un repuesto");
        }
        requirePositive(quantity);
        if (quantity > part.getCurrentStock()) {
            throw new ValidationException("Stock insuficiente de " + part.getName() + ": solo hay "
                    + part.getCurrentStock() + " unidades");
        }
        PartLine line = new PartLine();
        line.setPart(part);
        line.setQuantity(quantity);
        line.setUnitPrice(part.getUnitPrice());
        return line;
    }

    /**
     * Busca órdenes.
     *
     * @param text          número, placa, vehículo o cliente (vacío = todas)
     * @param status        estado ({@code null} = todos)
     * @param responsibleId mecánico responsable (0 = todos)
     */
    public List<ServiceOrder> searchOrders(String text, OrderStatus status, int responsibleId) {
        return repository.search(text, status, responsibleId);
    }

    /** Todas las órdenes, de la más reciente a la más antigua. */
    public List<ServiceOrder> findAll() {
        return repository.findAll();
    }

    /**
     * Orden completa para la pantalla de detalle.
     *
     * @throws NotFoundException si no existe
     */
    public ServiceOrder getDetail(int id) {
        ServiceOrder order = repository.findById(id);
        if (order == null) {
            throw new NotFoundException("No se encontró la orden de servicio solicitada");
        }
        return order;
    }

    /** Órdenes de un vehículo (historial del vehículo). */
    public List<ServiceOrder> findByVehicle(int vehicleId) {
        return repository.findByVehicle(vehicleId);
    }

    /** Órdenes de los vehículos de un cliente (detalle del cliente, notificaciones). */
    public List<ServiceOrder> findByCustomer(int customerId) {
        return repository.findByCustomer(customerId);
    }

    /** Órdenes abiertas (ni entregadas ni canceladas): KPI «Órdenes activas». */
    public int countActive() {
        return repository.countOpen();
    }

    /** Órdenes en un estado. */
    public int countByStatus(OrderStatus status) {
        if (status == null) {
            throw new ValidationException("El estado es obligatorio");
        }
        return repository.countByStatus(status);
    }

    /**
     * Número visible de la orden a partir del consecutivo: 145 → «0145».
     *
     * @param sequence consecutivo mayor que cero
     * @return número con al menos cuatro dígitos
     */
    public static String formatNumber(int sequence) {
        return String.format("%04d", sequence);
    }

    // ---------- validaciones ----------

    private Vehicle validateVehicle(Vehicle selected) {
        if (selected == null || selected.getId() <= 0) {
            throw new ValidationException("Selecciona el vehículo");
        }
        Vehicle vehicle = vehicleService.getDetail(selected.getId());
        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            throw new ValidationException("El vehículo está dado de baja");
        }
        if (repository.existsOpenOrderForVehicle(vehicle.getId())) {
            throw new ValidationException("El vehículo ya tiene una orden de servicio abierta");
        }
        return vehicle;
    }

    private void validateReason(ServiceOrder order) {
        String reason = blankToNull(order.getEntryReason());
        if (reason == null) {
            throw new ValidationException("El motivo de ingreso es obligatorio");
        }
        if (reason.length() > MAX_REASON) {
            throw new ValidationException("El motivo de ingreso no puede superar " + MAX_REASON + " caracteres");
        }
        order.setEntryReason(reason);
    }

    private void validateDates(ServiceOrder order) {
        LocalDate today = LocalDate.now();
        if (order.getEntryDate() == null) {
            order.setEntryDate(today);
        }
        if (order.getEntryDate().isAfter(today)) {
            throw new ValidationException("La fecha de ingreso no puede ser futura");
        }
        LocalDate delivery = order.getEstimatedDeliveryDate();
        if (delivery != null && delivery.isBefore(order.getEntryDate())) {
            throw new ValidationException("La entrega estimada no puede ser anterior a la fecha de ingreso");
        }
    }

    private void validateResponsible(User responsible) {
        if (responsible == null) {
            return;
        }
        if (!responsible.hasRole(Role.MECHANIC)) {
            throw new ValidationException("El responsable de la orden debe ser un mecánico");
        }
        if (responsible.getStatus() == UserStatus.INACTIVE) {
            throw new ValidationException("El mecánico seleccionado está inactivo");
        }
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
