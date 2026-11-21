package com.autogest.service;

import com.autogest.config.EmailConfig;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Catalogs.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.model.Catalogs.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Envío real de correos en el canal EMAIL del {@link NotificationService}
 * (Dev 1) con un {@link EmailSender} falso, y la lectura de la configuración
 * ({@link EmailConfig}).
 */
class NotificationEmailTest {

    private FakeNotificationRepository notifications;
    private NotificationService service;
    private Customer juan;
    private final List<String> sent = new ArrayList<>();

    @BeforeEach
    void setUp() {
        FakeCustomerRepository customers = new FakeCustomerRepository();
        notifications = new FakeNotificationRepository();
        service = new NotificationService(notifications, new CustomerService(customers));
        juan = new Customer();
        juan.setDocumentType(DocumentType.CC);
        juan.setDocumentNumber("1020304050");
        juan.setFirstName("Juan");
        juan.setLastName("Pérez");
        juan.setEmail("juan@cliente.test");
        customers.create(juan);
    }

    @Test
    void emailIsReallySentWhenASenderIsConfigured() {
        service.setEmailSender((to, name, subject, body) -> sent.add(to + "|" + name + "|" + subject + "|" + body));

        Notification result = service.createAndSend(draft());

        assertEquals(NotificationStatus.SENT, result.getStatus());
        assertEquals(List.of("juan@cliente.test|Juan Pérez|Tu vehículo está listo|Hola Juan"), sent);
    }

    @Test
    void sendingErrorLeavesTheNoticeFailed() {
        service.setEmailSender((to, name, subject, body) -> {
            throw new AutoGestException("sin conexión");
        });

        Notification result = service.createAndSend(draft());

        assertEquals(NotificationStatus.FAILED, result.getStatus());
        assertEquals(NotificationStatus.FAILED, notifications.findById(result.getId()).getStatus());
    }

    @Test
    void failedNoticeCanBeRetriedOnceTheConnectionIsBack() {
        boolean[] online = {false};
        service.setEmailSender((to, name, subject, body) -> {
            if (!online[0]) {
                throw new AutoGestException("sin conexión");
            }
            sent.add(subject);
        });
        Notification failed = service.createAndSend(draft());

        online[0] = true;
        Notification retried = service.retry(failed.getId());

        assertEquals(NotificationStatus.SENT, retried.getStatus());
        assertEquals(List.of("Tu vehículo está listo"), sent);
    }

    @Test
    void withoutSenderEmailIsSimulatedAsBefore() {
        assertEquals(NotificationStatus.SENT, service.createAndSend(draft()).getStatus());
    }

    @Test
    void customerWithoutEmailFailsWithoutCallingTheSender() {
        juan.setEmail(null);
        service.setEmailSender((to, name, subject, body) -> sent.add(to));

        assertEquals(NotificationStatus.FAILED, service.createAndSend(draft()).getStatus());
        assertTrue(sent.isEmpty());
    }

    // ---------- adjuntos ----------

    @Test
    void attachmentsFromTheProviderReachTheSender() {
        List<String> files = new ArrayList<>();
        service.setEmailSender(new EmailSender() {
            @Override
            public void send(String to, String toName, String subject, String body) {
                files.add("sin adjuntos");
            }

            @Override
            public void send(String to, String toName, String subject, String body,
                             List<EmailAttachment> attachments) {
                attachments.forEach(file -> files.add(file.fileName()));
            }
        });
        service.setAttachmentProvider(notification -> notification.getType() == NotificationType.BILLING
                ? List.of(new EmailAttachment("Factura.pdf", EmailAttachment.PDF, new byte[] {1}),
                        new EmailAttachment("Factura.png", EmailAttachment.PNG, new byte[] {2}))
                : List.of());
        Notification billing = draft();
        billing.setType(NotificationType.BILLING);

        assertEquals(NotificationStatus.SENT, service.createAndSend(billing).getStatus());
        assertEquals(NotificationStatus.SENT, service.createAndSend(draft()).getStatus());

        assertEquals(List.of("Factura.pdf", "Factura.png"), files);
    }

    @Test
    void senderWithoutAttachmentSupportFailsTheNotice() {
        service.setEmailSender((to, name, subject, body) -> sent.add(subject));
        service.setAttachmentProvider(notification ->
                List.of(new EmailAttachment("Factura.pdf", EmailAttachment.PDF, new byte[] {1})));

        assertEquals(NotificationStatus.FAILED, service.createAndSend(draft()).getStatus());
        assertTrue(sent.isEmpty());
    }

    @Test
    void attachmentErrorLeavesTheNoticeFailedForRetry() {
        service.setEmailSender((to, name, subject, body) -> sent.add(subject));
        service.setAttachmentProvider(notification -> {
            throw new AutoGestException("sin conexión con la base de datos");
        });

        assertEquals(NotificationStatus.FAILED, service.createAndSend(draft()).getStatus());
        assertTrue(sent.isEmpty());
    }

    // ---------- configuración ----------

    @Test
    void noAccountMeansSimulatedEmail() {
        assertNull(EmailConfig.fromProperties(new Properties()));
        Properties onlyUser = new Properties();
        onlyUser.setProperty("mail.username", "taller@gmail.com");
        assertNull(EmailConfig.fromProperties(onlyUser));
    }

    @Test
    void accountCreatesTheSmtpSender() {
        Properties properties = new Properties();
        properties.setProperty("mail.username", "taller@gmail.com");
        properties.setProperty("mail.password", "abcd efgh ijkl mnop");

        assertTrue(EmailConfig.fromProperties(properties) instanceof SmtpEmailSender);
    }

    private Notification draft() {
        Notification notification = new Notification();
        notification.setCustomer(juan);
        notification.setType(NotificationType.SERVICE_UPDATE);
        notification.setChannel(Channel.EMAIL);
        notification.setSubject("Tu vehículo está listo");
        notification.setMessage("Hola Juan");
        return notification;
    }
}
