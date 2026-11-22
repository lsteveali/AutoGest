package com.autogest.model;

import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Orden de servicio: núcleo operativo del taller. Reúne el vehículo, el
 * responsable, el diagnóstico, los servicios y repuestos, los costos y el estado.
 *
 * <p>Calcula sus propios totales recorriendo sus líneas con polimorfismo, controla
 * las transiciones de estado (decisión D-04) y es dueña de su historial: cada
 * cambio se registra con fecha, hora y usuario. Que el responsable tenga rol
 * {@code MECHANIC} lo valida el Service, no el modelo.</p>
 *
 * <p>Principles: Information Expert, Creator, Polymorphism, OCP.</p>
 */
public class ServiceOrder implements Billable {

    private int id;
    private String number;
    private LocalDate entryDate;
    private LocalDate estimatedDeliveryDate;
    private String entryReason;
    private String observations;
    private OrderStatus status;
    private Vehicle vehicle;
    private User responsible;
    private Diagnosis diagnosis;
    private List<OrderLine> lines;
    private List<OrderHistory> history;

    /**
     * Crea una orden vacía, con {@code id} 0, estado {@link OrderStatus#RECEIVED} y sin líneas ni historial.
     */
    public ServiceOrder() {
        this.status = OrderStatus.RECEIVED;
        this.lines = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    /**
     * Agrega un servicio o un repuesto a la orden.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param line línea a agregar
     * @throws ValidationException si la línea es {@code null}
     */
    public void addLine(OrderLine line) {
        if (line == null) {
            throw new ValidationException("La línea de la orden es obligatoria");
        }
        lines.add(line);
    }

    /**
     * Indica si la orden puede pasar al estado indicado. Solo se avanza un paso por
     * vez (Recibido, Diagnóstico, En reparación, Listo, Entregado) y se puede
     * cancelar desde cualquier estado excepto Entregado y Cancelado (decisión D-04).
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param newStatus estado al que se quiere pasar
     * @return {@code true} si la transición es válida
     */
    public boolean canChangeTo(OrderStatus newStatus) {
        if (newStatus == null || status == null) {
            return false;
        }
        return switch (status) {
            case RECEIVED -> newStatus == OrderStatus.DIAGNOSIS || newStatus == OrderStatus.CANCELLED;
            case DIAGNOSIS -> newStatus == OrderStatus.IN_REPAIR || newStatus == OrderStatus.CANCELLED;
            case IN_REPAIR -> newStatus == OrderStatus.READY || newStatus == OrderStatus.CANCELLED;
            case READY -> newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }

    /**
     * Cambia el estado de la orden y registra el cambio en el historial.
     *
     * <p>Principles: Information Expert, Creator.</p>
     *
     * @param newStatus nuevo estado
     * @param user      usuario que realiza el cambio
     * @throws ValidationException si el estado o el usuario son {@code null}, o si la
     *                             transición no es válida
     */
    public void changeStatus(OrderStatus newStatus, User user) {
        if (newStatus == null) {
            throw new ValidationException("El nuevo estado es obligatorio");
        }
        if (user == null) {
            throw new ValidationException("El usuario es obligatorio");
        }
        if (!canChangeTo(newStatus)) {
            throw new ValidationException("No se puede cambiar el estado de "
                    + statusLabel(status) + " a " + statusLabel(newStatus));
        }
        OrderStatus previous = status;
        status = newStatus;
        recordHistory("Estado cambiado de " + statusLabel(previous) + " a " + statusLabel(newStatus), user);
    }

    /**
     * Registra una entrada en el historial con la fecha y hora actuales.
     *
     * <p>Principles: Creator, Information Expert.</p>
     *
     * @param text descripción de lo ocurrido
     * @param user usuario que realizó la acción
     * @throws ValidationException si el texto está vacío o el usuario es {@code null}
     */
    public void recordHistory(String text, User user) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("La descripción del historial es obligatoria");
        }
        if (user == null) {
            throw new ValidationException("El usuario es obligatorio");
        }
        OrderHistory entry = new OrderHistory();
        entry.setTimestamp(LocalDateTime.now());
        entry.setDescription(text);
        entry.setUser(user);
        history.add(entry);
    }

    /**
     * Calcula el subtotal de la orden sumando el total de cada línea.
     *
     * <p>Principles: Information Expert, Polymorphism, OCP.</p>
     *
     * @return subtotal con escala 2
     */
    @Override
    public BigDecimal calculateSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderLine line : lines) {
            subtotal = subtotal.add(line.calculateTotal());
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula los impuestos. Devuelve cero hasta que Facturación o Configuración
     * definan la tasa (decisión D-05).
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return cero con escala 2
     */
    @Override
    public BigDecimal calculateTaxes() {
        return BigDecimal.ZERO.setScale(2);
    }

    /**
     * Calcula el total de la orden: subtotal más impuestos.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return total con escala 2
     */
    @Override
    public BigDecimal calculateTotal() {
        return calculateSubtotal().add(calculateTaxes());
    }

    /** Devuelve las líneas de la orden como una vista de solo lectura. */
    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    /** Reemplaza las líneas por una copia de la lista recibida ({@code null} deja la lista vacía). */
    public void setLines(List<OrderLine> lines) {
        this.lines = lines == null ? new ArrayList<>() : new ArrayList<>(lines);
    }

    /** Devuelve el historial de la orden como una vista de solo lectura. */
    public List<OrderHistory> getHistory() {
        return Collections.unmodifiableList(history);
    }

    /** Reemplaza el historial por una copia de la lista recibida ({@code null} deja la lista vacía). */
    public void setHistory(List<OrderHistory> history) {
        this.history = history == null ? new ArrayList<>() : new ArrayList<>(history);
    }

    /** Nombre en español de un estado, para el texto del historial. */
    private static String statusLabel(OrderStatus status) {
        return switch (status) {
            case RECEIVED -> "Recibido";
            case DIAGNOSIS -> "Diagnóstico";
            case IN_REPAIR -> "En reparación";
            case READY -> "Listo";
            case DELIVERED -> "Entregado";
            case CANCELLED -> "Cancelado";
        };
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el número visible, por ejemplo #0145. */
    public String getNumber() {
        return number;
    }

    /** Asigna el número visible, por ejemplo #0145. */
    public void setNumber(String number) {
        this.number = number;
    }

    /** Devuelve la fecha de ingreso del vehículo. */
    public LocalDate getEntryDate() {
        return entryDate;
    }

    /** Asigna la fecha de ingreso del vehículo. */
    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    /** Devuelve la fecha prevista de entrega. */
    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    /** Asigna la fecha prevista de entrega. */
    public void setEstimatedDeliveryDate(LocalDate estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    /** Devuelve el motivo de ingreso reportado por el cliente. */
    public String getEntryReason() {
        return entryReason;
    }

    /** Asigna el motivo de ingreso reportado por el cliente. */
    public void setEntryReason(String entryReason) {
        this.entryReason = entryReason;
    }

    /** Devuelve las observaciones generales. */
    public String getObservations() {
        return observations;
    }

    /** Asigna las observaciones generales. */
    public void setObservations(String observations) {
        this.observations = observations;
    }

    /** Devuelve el estado actual; para cambiarlo con validación use {@link #changeStatus(OrderStatus, User)}. */
    public OrderStatus getStatus() {
        return status;
    }

    /** Asigna el estado actual; para cambiarlo con validación use {@link #changeStatus(OrderStatus, User)}. */
    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    /** Devuelve el vehículo atendido. */
    public Vehicle getVehicle() {
        return vehicle;
    }

    /** Asigna el vehículo atendido. */
    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    /** Devuelve el usuario responsable de la orden. */
    public User getResponsible() {
        return responsible;
    }

    /** Asigna el usuario responsable de la orden. */
    public void setResponsible(User responsible) {
        this.responsible = responsible;
    }

    /** Devuelve el diagnóstico de la orden. */
    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    /** Asigna el diagnóstico de la orden. */
    public void setDiagnosis(Diagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }
}
