package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Catalogs.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.Invoice;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.model.Catalogs.NotificationType;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link NotificationService}: creación, programación, envío por
 * canal (D-17), reintentos y notificaciones de factura.
 */
class NotificationServiceTest {

    private FakeNotificationRepository repository;
    private FakeCustomerRepository customers;
    private NotificationService service;
    private Customer juan;
    private int documentSequence;

    @BeforeEach
    void setUp() {
        repository = new FakeNotificationRepository();
        customers = new FakeCustomerRepository();
        service = new NotificationService(repository, new CustomerService(customers));
        juan = customer("Juan", "juan@example.com", true);
    }

    private Customer customer(String name, String email, boolean promotions) {
        Customer customer = new Customer();
        customer.setDocumentType(DocumentType.CC);
        customer.setDocumentNumber("10" + (++documentSequence));
        customer.setFirstName(name);
        customer.setLastName("Pérez");
        customer.setEmail(email);
        customer.setAcceptsPromotions(promotions);
        return customers.create(customer);
    }

    private Notification draft(Customer customer, Channel channel) {
        Notification notification = new Notification();
        notification.setCustomer(customer);
        notification.setType(NotificationType.SERVICE_UPDATE);
        notification.setChannel(channel);
        notification.setSubject(" Actualización de orden #0145 ");
        notification.setMessage(" Tu vehículo está listo. ");
        return notification;
    }

    private Invoice invoice() {
        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(juan);
        ServiceOrder order = new ServiceOrder();
        order.setId(3);
        order.setNumber("0145");
        order.setVehicle(vehicle);
        Invoice invoice = new Invoice();
        invoice.setNumber("FAC-00128");
        invoice.setOrder(order);
        invoice.setTotal(new BigDecimal("270000"));
        invoice.setDueDate(LocalDate.of(2026, 11, 30));
        return invoice;
    }

    // ---------- creación ----------

    @Test
    void createWithoutDateIsPendingNowAndTrimmed() {
        Notification saved = service.createNotification(draft(juan, Channel.EMAIL));
        assertEquals(NotificationStatus.PENDING, saved.getStatus());
        assertTrue(saved.getScheduledDate() != null);
        assertEquals("Actualización de orden #0145", saved.getSubject());
        assertEquals("Tu vehículo está listo.", saved.getMessage());
    }

    @Test
    void createWithFutureDateIsScheduled() {
        Notification draft = draft(juan, Channel.EMAIL);
        draft.setScheduledDate(LocalDateTime.now().plusDays(2));
        assertEquals(NotificationStatus.SCHEDULED, service.createNotification(draft).getStatus());
    }

    @Test
    void createValidatesRequiredFields() {
        Notification noCustomer = draft(null, Channel.EMAIL);
        assertEquals("Selecciona el cliente",
                assertThrows(ValidationException.class, () -> service.createNotification(noCustomer)).getMessage());
        Notification noMessage = draft(juan, Channel.EMAIL);
        noMessage.setMessage(" ");
        assertThrows(ValidationException.class, () -> service.createNotification(noMessage));
        Notification noChannel = draft(juan, null);
        assertThrows(ValidationException.class, () -> service.createNotification(noChannel));
        Notification longSubject = draft(juan, Channel.EMAIL);
        longSubject.setSubject("x".repeat(151));
        assertThrows(ValidationException.class, () -> service.createNotification(longSubject));
    }

    @Test
    void createRejectsUnknownCustomer() {
        Customer ghost = new Customer();
        ghost.setId(99);
        assertThrows(NotFoundException.class, () -> service.createNotification(draft(ghost, Channel.EMAIL)));
    }

    @Test
    void promotionRequiresConsent() {
        Customer noPromos = customer("Ana", "ana@example.com", false);
        Notification promo = draft(noPromos, Channel.EMAIL);
        promo.setType(NotificationType.PROMOTION);
        assertEquals("El cliente no acepta recibir promociones",
                assertThrows(ValidationException.class, () -> service.createNotification(promo)).getMessage());
    }

    // ---------- envío por canal ----------

    @Test
    void emailWithAddressIsSent() {
        assertEquals(NotificationStatus.SENT, service.createAndSend(draft(juan, Channel.EMAIL)).getStatus());
    }

    @Test
    void emailWithoutAddressFails() {
        Customer noEmail = customer("Luis", null, false);
        assertEquals(NotificationStatus.FAILED, service.createAndSend(draft(noEmail, Channel.EMAIL)).getStatus());
    }

    @Test
    void systemChannelIsAlwaysSent() {
        Customer noEmail = customer("Luis", null, false);
        assertEquals(NotificationStatus.SENT, service.createAndSend(draft(noEmail, Channel.SYSTEM)).getStatus());
    }

    @Test
    void whatsappIsPreparedButFails() {
        assertEquals(NotificationStatus.FAILED, service.createAndSend(draft(juan, Channel.WHATSAPP)).getStatus());
    }

    @Test
    void createAndSendKeepsScheduledUntilDue() {
        Notification draft = draft(juan, Channel.EMAIL);
        draft.setScheduledDate(LocalDateTime.now().plusHours(3));
        assertEquals(NotificationStatus.SCHEDULED, service.createAndSend(draft).getStatus());
    }

    @Test
    void sendTwiceIsRejected() {
        Notification sent = service.createAndSend(draft(juan, Channel.SYSTEM));
        assertThrows(ValidationException.class, () -> service.send(sent.getId()));
    }

    @Test
    void retryOnlyFailedAndCanSucceed() {
        Customer noEmail = customer("Luis", null, false);
        Notification failed = service.createAndSend(draft(noEmail, Channel.EMAIL));
        noEmail.setEmail("luis@example.com");
        assertEquals(NotificationStatus.SENT, service.retry(failed.getId()).getStatus());
        assertThrows(ValidationException.class, () -> service.retry(failed.getId()));
    }

    @Test
    void sendDueScheduledSendsOnlyPastOnes() {
        Notification due = service.createNotification(draft(juan, Channel.SYSTEM));
        due.setStatus(NotificationStatus.SCHEDULED);
        due.setScheduledDate(LocalDateTime.now().minusMinutes(5));
        Notification later = draft(juan, Channel.SYSTEM);
        later.setScheduledDate(LocalDateTime.now().plusDays(1));
        service.createNotification(later);
        assertEquals(1, service.sendDueScheduled());
        assertEquals(1, service.countByStatus(NotificationStatus.SCHEDULED));
        assertEquals(1, service.countByStatus(NotificationStatus.SENT));
    }

    // ---------- consultas y borrado ----------

    @Test
    void searchFiltersAndDeleteRemoves() {
        Notification saved = service.createAndSend(draft(juan, Channel.SYSTEM));
        assertEquals(1, service.searchNotifications("listo", null, null, null).size());
        assertEquals(1, service.searchNotifications("juan", null, Channel.SYSTEM, NotificationStatus.SENT).size());
        assertEquals(0, service.searchNotifications(null, NotificationType.PROMOTION, null, null).size());
        service.deleteNotification(saved.getId());
        assertThrows(NotFoundException.class, () -> service.getDetail(saved.getId()));
    }

    @Test
    void recentAndByCustomer() {
        service.createNotification(draft(juan, Channel.SYSTEM));
        service.createNotification(draft(juan, Channel.EMAIL));
        assertEquals(1, service.findRecent(1).size());
        assertEquals(2, service.findByCustomer(juan.getId()).size());
        assertThrows(ValidationException.class, () -> service.findRecent(0));
    }

    // ---------- facturación ----------

    @Test
    void invoiceIssuedMessageHasNumberTotalOrderAndDueDate() {
        Notification notification = service.buildInvoiceIssued(invoice());
        assertEquals(NotificationType.BILLING, notification.getType());
        assertEquals(Channel.EMAIL, notification.getChannel());
        assertEquals("Factura FAC-00128 disponible", notification.getSubject());
        assertTrue(notification.getMessage().contains("FAC-00128"));
        assertTrue(notification.getMessage().contains("#0145"));
        assertTrue(notification.getMessage().contains("30/11/2026"));
        assertEquals(juan, notification.getCustomer());
        assertEquals(0, notification.getId());
        assertNull(notification.getScheduledDate());
    }

    @Test
    void notifyInvoiceIssuedSavesAndSends() {
        Notification sent = service.notifyInvoiceIssued(invoice());
        assertTrue(sent.getId() > 0);
        assertEquals(NotificationStatus.SENT, sent.getStatus());
    }

    @Test
    void paidAndOverdueMessages() {
        assertTrue(service.buildInvoicePaid(invoice()).getSubject().startsWith("Pago recibido"));
        assertTrue(service.buildInvoiceOverdue(invoice()).getMessage().contains("venció"));
    }

    @Test
    void invoiceWithoutCustomerIsRejected() {
        Invoice invoice = invoice();
        invoice.getOrder().setVehicle(null);
        assertThrows(ValidationException.class, () -> service.buildInvoiceIssued(invoice));
    }
}
