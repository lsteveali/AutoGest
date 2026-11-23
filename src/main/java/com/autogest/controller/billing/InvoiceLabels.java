package com.autogest.controller.billing;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.Catalogs.InvoiceStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import com.autogest.service.InvoiceService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Textos en español de Facturación: estados, métodos de pago, periodos del
 * filtro de fecha y los datos de cliente y vehículo de una factura. El modelo
 * y la base de datos guardan códigos; aquí se traducen para la pantalla.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class InvoiceLabels {

    /** Opción «sin filtro». */
    public static final String ALL = "Todos";

    /** Periodos del filtro de fecha de emisión. */
    public static final List<String> PERIODS = List.of("Todas", "Hoy", "Esta semana", "Este mes");

    private InvoiceLabels() {
        // Clase de utilidad: no se instancia.
    }

    /** Nombre visible del estado. */
    public static String statusText(InvoiceStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case PENDING -> "Pendiente";
            case PAID -> "Pagada";
            case OVERDUE -> "Vencida";
            case VOIDED -> "Anulada";
        };
    }

    /** Color del distintivo del estado. */
    public static StatusBadge.BadgeType statusType(InvoiceStatus status) {
        if (status == null) {
            return StatusBadge.BadgeType.NEUTRAL;
        }
        return switch (status) {
            case PENDING -> StatusBadge.BadgeType.WARNING;
            case PAID -> StatusBadge.BadgeType.SUCCESS;
            case OVERDUE -> StatusBadge.BadgeType.DANGER;
            case VOIDED -> StatusBadge.BadgeType.NEUTRAL;
        };
    }

    /** Opciones del filtro de estado, con «Todos» primero. */
    public static List<String> statusOptions() {
        return List.of(ALL, statusText(InvoiceStatus.PAID), statusText(InvoiceStatus.PENDING),
                statusText(InvoiceStatus.OVERDUE), statusText(InvoiceStatus.VOIDED));
    }

    /** Estado elegido en el filtro ({@code null} = todos). */
    public static InvoiceStatus statusFromText(String text) {
        for (InvoiceStatus status : InvoiceStatus.values()) {
            if (statusText(status).equals(text)) {
                return status;
            }
        }
        return null;
    }

    /** Nombres visibles de todos los métodos de pago (filtros de la lista), en el orden del Service. */
    public static List<String> methodOptions() {
        return methodOptions(InvoiceService.PAYMENT_METHODS);
    }

    /**
     * Nombres visibles de los métodos indicados (por ejemplo, los habilitados
     * en Configuración → Facturación para registrar un pago).
     *
     * @param codes códigos de los métodos
     * @return nombres en español, en el mismo orden
     */
    public static List<String> methodOptions(List<String> codes) {
        return codes.stream().map(InvoiceService::methodText).toList();
    }

    /** Código del método elegido ({@code null} = todos o ninguno). */
    public static String methodFromText(String text) {
        for (String code : InvoiceService.PAYMENT_METHODS) {
            if (InvoiceService.methodText(code).equals(text)) {
                return code;
            }
        }
        return null;
    }

    /** Método de pago para la tabla: «-» si aún no se ha pagado. */
    public static String methodText(Invoice invoice) {
        return invoice.getPaymentMethod() == null ? "-" : InvoiceService.methodText(invoice.getPaymentMethod());
    }

    /**
     * Primer día del periodo del filtro de fecha.
     *
     * @param period «Todas», «Hoy», «Esta semana» o «Este mes»
     * @param today  fecha de referencia
     * @return fecha desde (inclusive), o {@code null} para todas
     */
    public static LocalDate periodStart(String period, LocalDate today) {
        if (period == null) {
            return null;
        }
        return switch (period) {
            case "Hoy" -> today;
            case "Esta semana" -> today.with(DayOfWeek.MONDAY);
            case "Este mes" -> today.withDayOfMonth(1);
            default -> null;
        };
    }

    /** Indica si la factura todavía se puede pagar o anular. */
    public static boolean isOpen(Invoice invoice) {
        return invoice.getStatus() == InvoiceStatus.PENDING || invoice.getStatus() == InvoiceStatus.OVERDUE;
    }

    /** Nombre del cliente de la factura. */
    public static String customer(Invoice invoice) {
        Customer owner = owner(invoice.getOrder());
        return owner == null || owner.getFirstName() == null ? "-" : owner.getFullName();
    }

    /** Marca y modelo del vehículo. */
    public static String vehicle(ServiceOrder order) {
        Vehicle vehicle = order == null ? null : order.getVehicle();
        if (vehicle == null || vehicle.getMake() == null) {
            return "-";
        }
        return vehicle.getMake() + " " + (vehicle.getModel() == null ? "" : vehicle.getModel());
    }

    /** Placa del vehículo. */
    public static String plate(ServiceOrder order) {
        Vehicle vehicle = order == null ? null : order.getVehicle();
        return vehicle == null || vehicle.getLicensePlate() == null ? "-" : vehicle.getLicensePlate();
    }

    /** Número de la orden con «#». */
    public static String orderNumber(ServiceOrder order) {
        return order == null || order.getNumber() == null ? "-" : "#" + order.getNumber();
    }

    /**
     * Indica si la factura es de ese cliente (el dueño del vehículo de su orden).
     *
     * @param invoice    factura
     * @param customerId id del cliente
     * @return {@code true} si el cliente coincide
     */
    static boolean belongsTo(Invoice invoice, int customerId) {
        Customer owner = owner(invoice.getOrder());
        return owner != null && owner.getId() == customerId;
    }

    private static Customer owner(ServiceOrder order) {
        return order == null || order.getVehicle() == null ? null : order.getVehicle().getOwner();
    }
}
