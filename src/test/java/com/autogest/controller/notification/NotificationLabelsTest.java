package com.autogest.controller.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Channel;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de los textos del módulo Notificaciones (sin JavaFX).
 */
class NotificationLabelsTest {

    @Test
    void textsRoundTrip() {
        for (NotificationType type : NotificationType.values()) {
            assertEquals(type, NotificationLabels.typeFromText(NotificationLabels.typeText(type)));
        }
        for (Channel channel : Channel.values()) {
            assertEquals(channel, NotificationLabels.channelFromText(NotificationLabels.channelText(channel)));
        }
        for (NotificationStatus status : NotificationStatus.values()) {
            assertEquals(status, NotificationLabels.statusFromText(NotificationLabels.statusText(status)));
        }
        assertNull(NotificationLabels.typeFromText(NotificationLabels.ALL));
    }

    @Test
    void statusColorsMatchTheMockup() {
        assertEquals(StatusBadge.BadgeType.SUCCESS, NotificationLabels.statusType(NotificationStatus.SENT));
        assertEquals(StatusBadge.BadgeType.INFO, NotificationLabels.statusType(NotificationStatus.SCHEDULED));
        assertEquals(StatusBadge.BadgeType.DANGER, NotificationLabels.statusType(NotificationStatus.FAILED));
        assertEquals("Correo electrónico", NotificationLabels.channelText(Channel.EMAIL));
    }

    @Test
    void titleFallsBackToType() {
        Notification notification = new Notification();
        notification.setType(NotificationType.BILLING);
        assertEquals("Facturación", NotificationsController.title(notification));
        notification.setSubject("Factura FAC-00128 disponible");
        assertEquals("Factura FAC-00128 disponible", NotificationsController.title(notification));
    }
}
