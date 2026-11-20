package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.NotificationType;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import com.autogest.util.Formatter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plantillas de los correos al cliente (Sprint 6, Líder): asunto y mensaje con
 * campos entre llaves que se completan con los datos del cliente, el vehículo,
 * la orden y la factura, por ejemplo {@code Hola {cliente}, tu {vehiculo} ...}.
 * Las usan los avisos automáticos (vehículo listo, mantenimiento, facturación)
 * y, como mensaje predeterminado por tipo, el formulario «Nueva notificación»
 * ({@link #forManual}).
 *
 * <p>Campos disponibles: {@code {cliente}} (nombre), {@code {vehiculo}} (marca
 * y modelo), {@code {placa}}, {@code {orden}} (número con «#»), {@code {fecha}},
 * {@code {taller}}, {@code {factura}} (número), {@code {total}} y
 * {@code {vencimiento}}. Un campo sin valor queda vacío, nunca con las llaves.</p>
 *
 * <p>Los textos viven aquí mientras no exista la configuración de
 * Notificaciones (se guardará en {@code app_settings} cuando llegue el
 * {@code SettingsService} de Dev 1, Sprint 8).</p>
 *
 * <p>Principles: Information Expert (cada plantilla sabe armar su texto),
 * Protected Variations (los textos cambian sin tocar los eventos).</p>
 */
public enum NotificationTemplate {

    /** La orden pasó a Lista: el vehículo se puede recoger. */
    VEHICLE_READY(NotificationType.SERVICE_UPDATE, "Tu vehículo está listo",
            "Tu {vehiculo} ({placa}) está listo para entregar",
            "Hola {cliente}, terminamos el servicio de tu {vehiculo} de placa {placa} (orden {orden}). "
                    + "Ya puedes pasar a recogerlo. ¡Gracias por confiar en {taller}!"),

    /** Mantenimiento vencido o próximo a vencer. */
    MAINTENANCE_DUE(NotificationType.MAINTENANCE_REMINDER, "Recordatorio de mantenimiento",
            "Mantenimiento de tu {vehiculo} ({placa})",
            "Hola {cliente}, te recordamos que el mantenimiento de tu {vehiculo} de placa {placa} "
                    + "está programado para el {fecha}. Escríbenos para agendar tu cita en {taller}."),

    /** Factura emitida y pendiente de pago; va con la factura en PDF y PNG. */
    INVOICE_AVAILABLE(NotificationType.BILLING, "Factura disponible",
            "Factura {factura} disponible",
            "Hola {cliente}, te enviamos la factura {factura} por {total} del servicio de tu {vehiculo} "
                    + "({placa}), orden {orden}. La encuentras adjunta en PDF y como imagen. "
                    + "Puedes pagarla hasta el {vencimiento}. ¡Gracias por confiar en {taller}!"),

    /** Pago registrado; va con la factura en PDF y PNG. */
    INVOICE_PAID(NotificationType.BILLING, "Pago recibido",
            "Pago recibido de la factura {factura}",
            "Hola {cliente}, recibimos el pago de tu factura {factura} por {total}. Te la adjuntamos en PDF "
                    + "y como imagen para tus registros. ¡Gracias por confiar en {taller}!"),

    /** Factura vencida; va con la factura en PDF y PNG. */
    INVOICE_OVERDUE(NotificationType.BILLING, "Factura vencida",
            "Factura {factura} vencida",
            "Hola {cliente}, tu factura {factura} por {total} venció el {vencimiento}. Te la adjuntamos en PDF "
                    + "y como imagen. Por favor comunícate con {taller} para ponerte al día."),

    /** Mensaje predeterminado de «Actualización de servicio» (Nueva notificación). */
    SERVICE_UPDATE(NotificationType.SERVICE_UPDATE, "Novedades del servicio",
            "Novedades del servicio de tu {vehiculo}",
            "Hola {cliente}, te contamos que el servicio de tu {vehiculo} ({placa}) sigue avanzando en "
                    + "{taller}. Te avisaremos apenas esté listo para entregar. Si tienes alguna pregunta, "
                    + "responde a este correo."),

    /** Mensaje predeterminado de «Recordatorio de mantenimiento» (Nueva notificación). */
    MAINTENANCE_INVITE(NotificationType.MAINTENANCE_REMINDER, "Invitación a mantenimiento",
            "Es momento del mantenimiento de tu {vehiculo}",
            "Hola {cliente}, según nuestros registros se acerca el mantenimiento de tu {vehiculo} ({placa}). "
                    + "Mantenerlo al día evita daños mayores. Escríbenos para agendar tu cita en {taller}."),

    /** Mensaje predeterminado de «Promoción» (Nueva notificación). */
    PROMOTION(NotificationType.PROMOTION, "Promoción",
            "Una promoción para ti en {taller}",
            "Hola {cliente}, en {taller} tenemos una promoción especial para el cuidado de tu vehículo. "
                    + "Escríbenos para conocer los detalles y agendar tu cita."),

    /** Mensaje predeterminado de «General» (Nueva notificación). */
    GENERAL(NotificationType.GENERAL, "Mensaje general",
            "Mensaje de {taller}",
            "Hola {cliente}, te escribimos desde {taller}. Si tienes alguna pregunta, responde a este correo.");

    /** Nombre del taller mientras no exista «Datos del taller» en Configuración. */
    public static final String WORKSHOP_NAME = "AutoGest";

    private static final Pattern FIELD = Pattern.compile("\\{([a-z]+)\\}");

    /** Campos que aceptan las plantillas. */
    public static final List<String> FIELDS = List.of("cliente", "vehiculo", "placa", "orden", "fecha", "taller",
            "factura", "total", "vencimiento");

    private final NotificationType type;
    private final String title;
    private final String subject;
    private final String message;

    NotificationTemplate(NotificationType type, String title, String subject, String message) {
        this.type = type;
        this.title = title;
        this.subject = subject;
        this.message = message;
    }

    /** Tipo de notificación que genera. */
    public NotificationType getType() {
        return type;
    }

    /** Nombre visible de la plantilla. */
    public String getTitle() {
        return title;
    }

    /** Asunto con sus campos sin completar. */
    public String getSubject() {
        return subject;
    }

    /** Mensaje con sus campos sin completar. */
    public String getMessage() {
        return message;
    }

    /** Completa el asunto con los valores dados. */
    public String subject(Map<String, String> values) {
        return fill(subject, values);
    }

    /** Completa el mensaje con los valores dados. */
    public String message(Map<String, String> values) {
        return fill(message, values);
    }

    /**
     * Valores base de toda plantilla (el nombre del taller); se agregan los
     * del cliente, vehículo y orden con {@code put}.
     *
     * @return mapa modificable
     */
    public static Map<String, String> baseValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("taller", WORKSHOP_NAME);
        return values;
    }

    /**
     * Mensaje predeterminado para una notificación que se escribe a mano,
     * según su tipo. En Facturación depende del estado de la factura:
     * pendiente → «Factura disponible», pagada → «Pago recibido», vencida →
     * «Factura vencida».
     *
     * @param type    tipo elegido
     * @param invoice factura de la orden elegida (puede ser {@code null})
     * @return plantilla a usar
     */
    public static NotificationTemplate forManual(NotificationType type, Invoice invoice) {
        if (type == null) {
            return GENERAL;
        }
        return switch (type) {
            case SERVICE_UPDATE -> SERVICE_UPDATE;
            case MAINTENANCE_REMINDER -> MAINTENANCE_INVITE;
            case BILLING -> invoice == null ? INVOICE_AVAILABLE : switch (invoice.getStatus()) {
                case PAID -> INVOICE_PAID;
                case OVERDUE -> INVOICE_OVERDUE;
                default -> INVOICE_AVAILABLE;
            };
            case PROMOTION -> PROMOTION;
            case GENERAL -> GENERAL;
        };
    }

    /**
     * Valores de los campos a partir del cliente, la orden y la factura. Lo
     * que falte queda vacío; sin vehículo, {@code {vehiculo}} dice «vehículo»
     * para que «tu {vehiculo}» siga leyéndose bien.
     *
     * @param customer cliente (puede ser {@code null})
     * @param order    orden (puede ser {@code null}); de ahí sale el vehículo
     * @param invoice  factura (puede ser {@code null})
     * @return mapa modificable
     */
    public static Map<String, String> valuesFor(Customer customer, ServiceOrder order, Invoice invoice) {
        Map<String, String> values = baseValues();
        if (customer != null) {
            values.put("cliente", customer.getFirstName());
        }
        Vehicle vehicle = order == null ? null : order.getVehicle();
        String vehicleText = vehicle == null ? "" : ((vehicle.getMake() == null ? "" : vehicle.getMake()) + " "
                + (vehicle.getModel() == null ? "" : vehicle.getModel())).trim();
        values.put("vehiculo", vehicleText.isEmpty() ? "vehículo" : vehicleText);
        if (vehicle != null) {
            values.put("placa", vehicle.getLicensePlate());
        }
        if (order != null && order.getNumber() != null) {
            values.put("orden", "#" + order.getNumber());
        }
        if (invoice != null) {
            values.put("factura", invoice.getNumber());
            values.put("total", invoice.getTotal() == null ? "" : Formatter.money(invoice.getTotal()));
            values.put("vencimiento", invoice.getDueDate() == null ? "" : Formatter.date(invoice.getDueDate()));
        }
        return values;
    }

    /**
     * Reemplaza cada {@code {campo}} por su valor; un campo sin valor queda vacío.
     *
     * @param text   texto con campos
     * @param values valores por nombre de campo
     * @return texto completo, sin espacios dobles
     * @throws ValidationException si el texto usa un campo que no existe
     */
    static String fill(String text, Map<String, String> values) {
        Matcher matcher = FIELD.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String field = matcher.group(1);
            if (!FIELDS.contains(field)) {
                throw new ValidationException("La plantilla usa un campo que no existe: {" + field + "}");
            }
            String value = values.get(field);
            matcher.appendReplacement(out, Matcher.quoteReplacement(value == null ? "" : value.trim()));
        }
        matcher.appendTail(out);
        return out.toString().replaceAll(" {2,}", " ").replace(" ()", "").trim();
    }
}
