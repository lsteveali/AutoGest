package com.autogest.model;

import com.autogest.model.Catalogs.NotificationStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de los cambios de estado de {@link Notification}.
 */
class NotificationTest {

    private Notification notificationIn(NotificationStatus status) {
        Notification notification = new Notification();
        notification.setStatus(status);
        return notification;
    }

    @Test
    void newNotificationIsPending() {
        Notification notification = new Notification();

        assertEquals(0, notification.getId());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
        assertTrue(notification.isPending());
    }

    @Test
    void isPendingForPendingAndScheduledOnly() {
        assertTrue(notificationIn(NotificationStatus.PENDING).isPending());
        assertTrue(notificationIn(NotificationStatus.SCHEDULED).isPending());
        assertFalse(notificationIn(NotificationStatus.SENT).isPending());
        assertFalse(notificationIn(NotificationStatus.FAILED).isPending());
    }

    @Test
    void markAsSentFromPendingScheduledAndFailed() {
        for (NotificationStatus from : new NotificationStatus[] {
                NotificationStatus.PENDING, NotificationStatus.SCHEDULED, NotificationStatus.FAILED}) {
            Notification notification = notificationIn(from);

            notification.markAsSent();

            assertEquals(NotificationStatus.SENT, notification.getStatus(), "desde " + from);
        }
    }

    @Test
    void markAsSentRejectsAnAlreadySentNotification() {
        Notification notification = notificationIn(NotificationStatus.SENT);

        ValidationException error = assertThrows(ValidationException.class, notification::markAsSent);

        assertEquals("La notificación ya fue enviada", error.getMessage());
        assertEquals(NotificationStatus.SENT, notification.getStatus());
    }

    @Test
    void markAsFailedFromPendingAndScheduled() {
        Notification pending = notificationIn(NotificationStatus.PENDING);
        pending.markAsFailed();
        assertEquals(NotificationStatus.FAILED, pending.getStatus());

        Notification scheduled = notificationIn(NotificationStatus.SCHEDULED);
        scheduled.markAsFailed();
        assertEquals(NotificationStatus.FAILED, scheduled.getStatus());
    }

    @Test
    void markAsFailedRejectsSentAndAlreadyFailedNotifications() {
        Notification sent = notificationIn(NotificationStatus.SENT);
        ValidationException sentError = assertThrows(ValidationException.class, sent::markAsFailed);
        assertEquals("Una notificación enviada no puede marcarse como fallida", sentError.getMessage());
        assertEquals(NotificationStatus.SENT, sent.getStatus());

        Notification failed = notificationIn(NotificationStatus.FAILED);
        ValidationException failedError = assertThrows(ValidationException.class, failed::markAsFailed);
        assertEquals("La notificación ya está marcada como fallida", failedError.getMessage());
    }
}
