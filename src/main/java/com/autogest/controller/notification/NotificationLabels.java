package com.autogest.controller.notification;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Catalogs.Channel;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.model.Catalogs.NotificationType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Utilidades de presentación del módulo Notificaciones: textos en español de
 * tipos, canales y estados, colores de insignia y opciones de los filtros.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public final class NotificationLabels {

    /** Opción «sin filtro» de los desplegables. */
    public static final String ALL = "Todos";

    private NotificationLabels() {
        // Clase de utilidad: no se instancia.
    }

    /** Texto visible del tipo. */
    public static String typeText(NotificationType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case SERVICE_UPDATE -> "Actualización de servicio";
            case MAINTENANCE_REMINDER -> "Recordatorio de mantenimiento";
            case BILLING -> "Facturación";
            case PROMOTION -> "Promoción";
            case GENERAL -> "General";
        };
    }

    /** Texto visible del canal. */
    public static String channelText(Channel channel) {
        if (channel == null) {
            return "-";
        }
        return switch (channel) {
            case EMAIL -> "Correo electrónico";
            case SYSTEM -> "Sistema";
            case WHATSAPP -> "WhatsApp";
        };
    }

    /** Texto visible del estado. */
    public static String statusText(NotificationStatus status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case SENT -> "Enviada";
            case PENDING -> "Pendiente";
            case SCHEDULED -> "Programada";
            case FAILED -> "Fallida";
        };
    }

    /** Color de insignia: verde enviada, amarillo pendiente, azul programada, rojo fallida. */
    public static StatusBadge.BadgeType statusType(NotificationStatus status) {
        if (status == null) {
            return StatusBadge.BadgeType.NEUTRAL;
        }
        return switch (status) {
            case SENT -> StatusBadge.BadgeType.SUCCESS;
            case PENDING -> StatusBadge.BadgeType.WARNING;
            case SCHEDULED -> StatusBadge.BadgeType.INFO;
            case FAILED -> StatusBadge.BadgeType.DANGER;
        };
    }

    /** Opciones de tipo (sin «Todos»). */
    public static List<String> typeOptions() {
        return texts(NotificationType.values(), NotificationLabels::typeText);
    }

    /** Opciones de canal (sin «Todos»). */
    public static List<String> channelOptions() {
        return texts(Channel.values(), NotificationLabels::channelText);
    }

    /** Opciones de estado (sin «Todos»). */
    public static List<String> statusOptions() {
        return texts(NotificationStatus.values(), NotificationLabels::statusText);
    }

    /** Tipo a partir de su texto; desconocido o «Todos» = {@code null}. */
    public static NotificationType typeFromText(String text) {
        return fromText(NotificationType.values(), NotificationLabels::typeText, text);
    }

    /** Canal a partir de su texto; desconocido o «Todos» = {@code null}. */
    public static Channel channelFromText(String text) {
        return fromText(Channel.values(), NotificationLabels::channelText, text);
    }

    /** Estado a partir de su texto; desconocido o «Todos» = {@code null}. */
    public static NotificationStatus statusFromText(String text) {
        return fromText(NotificationStatus.values(), NotificationLabels::statusText, text);
    }

    private static <E> List<String> texts(E[] values, Function<E, String> toText) {
        return Arrays.stream(values).map(toText).toList();
    }

    private static <E> E fromText(E[] values, Function<E, String> toText, String text) {
        for (E value : values) {
            if (toText.apply(value).equals(text)) {
                return value;
            }
        }
        return null;
    }
}
