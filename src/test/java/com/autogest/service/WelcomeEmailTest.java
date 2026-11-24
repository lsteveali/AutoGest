package com.autogest.service;

import com.autogest.exception.AutoGestException;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.model.Catalogs.NotificationType;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.Customer;
import com.autogest.model.Notification;
import com.autogest.model.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Correos «registrado con éxito» con remitentes falsos: nadie recibe nada. */
class WelcomeEmailTest {

    private static User newUser(String email, Role role) {
        User user = new User();
        user.setFirstName("Carlos");
        user.setLastName("Ruiz");
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    @Test
    void newUserGetsWelcomeWithRoleAndNoPassword() {
        List<String> sent = new ArrayList<>();
        UserService service = new UserService(new FakeUserRepository());
        service.setAccountMailer(new AccountMailer(() -> (to, name, subject, body) ->
                sent.add(to + "|" + subject + "|" + body)));

        service.createUser(newUser("carlos@autogest.com", Role.MECHANIC), "Clave1234");

        assertEquals(1, sent.size());
        String mail = sent.get(0);
        assertTrue(mail.startsWith("carlos@autogest.com|¡Te registraste con éxito en AutoGest!|"), mail);
        assertTrue(mail.contains("Mecánico"), mail);
        assertFalse(mail.contains("Clave1234"), "La contraseña nunca va en el correo");
    }

    @Test
    void failedWelcomeDoesNotUndoTheNewUser() {
        FakeUserRepository repository = new FakeUserRepository();
        UserService service = new UserService(repository);
        service.setAccountMailer(new AccountMailer(() -> (to, name, subject, body) -> {
            throw new AutoGestException("Gmail no responde");
        }));

        User created = service.createUser(newUser("carlos@autogest.com", Role.ADMINISTRATOR), "Clave1234");

        assertTrue(created.getId() > 0);
        assertEquals(1, repository.countAll());
    }

    @Test
    void newCustomerWithEmailGetsTheWelcomeNotification() {
        FakeCustomerRepository customers = new FakeCustomerRepository();
        FakeNotificationRepository notifications = new FakeNotificationRepository();
        CustomerService customerService = new CustomerService(customers);
        NotificationService notificationService = new NotificationService(notifications, customerService);
        List<String> sent = new ArrayList<>();
        notificationService.setEmailSender((to, name, subject, body) -> sent.add(to + "|" + subject));
        NotificationEventService events = new NotificationEventService(notificationService,
                new VehicleService(new FakeVehicleRepository(), customers));
        customerService.setOnRegistered(events::welcomeCustomer);

        Customer customer = new Customer();
        customer.setFirstName("Juan");
        customer.setLastName("Pérez");
        customer.setDocumentType(DocumentType.CC);
        customer.setDocumentNumber("1234567890");
        customer.setPhone("3001234567");
        customer.setEmail("juan@cliente.test");
        customer.setCity("Valledupar");
        customerService.registerCustomer(customer);

        assertEquals(List.of("juan@cliente.test|¡Te registraste con éxito en AutoGest!"), sent);
        Notification saved = notifications.findAll().get(0);
        assertEquals(NotificationType.GENERAL, saved.getType());
        assertEquals(NotificationStatus.SENT, saved.getStatus());
    }

    @Test
    void customerWithoutEmailGetsNothing() {
        FakeCustomerRepository customers = new FakeCustomerRepository();
        NotificationEventService events = new NotificationEventService(
                new NotificationService(new FakeNotificationRepository(), new CustomerService(customers)),
                new VehicleService(new FakeVehicleRepository(), customers));
        Customer customer = new Customer();
        customer.setId(5);
        customer.setFirstName("Sin");

        assertNull(events.welcomeCustomer(customer));
    }
}
