package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.NotificationType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Plantillas de los avisos automáticos (Sprint 6, Líder): asunto y mensaje con
 * campos entre llaves que se completan con los datos del cliente, el vehículo
 * y la orden, por ejemplo {@code Hola {cliente}, tu {vehiculo} ...}.
 *
 * <p>Campos disponibles: {@code {cliente}} (nombre), {@code {vehiculo}} (marca
 * y modelo), {@code {placa}}, {@code {orden}} (número con «#»), {@code {fecha}}
 * y {@code {taller}}. Un campo sin valor queda vacío, nunca con las llaves.</p>
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
                    + "está programado para el {fecha}. Escríbenos para agendar tu cita en {taller}.");

    /** Nombre del taller mientras no exista «Datos del taller» en Configuración. */
    public static final String WORKSHOP_NAME = "AutoGest";

    private static final Pattern FIELD = Pattern.compile("\\{([a-z]+)\\}");

    /** Campos que aceptan las plantillas. */
    public static final List<String> FIELDS = List.of("cliente", "vehiculo", "placa", "orden", "fecha", "taller");

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
