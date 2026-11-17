package com.autogest.service;

import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link SmtpEmailSender} contra GreenMail, un servidor de correo
 * falso en memoria: los correos no salen del equipo.
 */
class SmtpEmailSenderTest {

    private static final String USER = "taller@autogest.test";
    private static final String PASSWORD = "clave-de-aplicacion";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser(USER, USER, PASSWORD))
            .withPerMethodLifecycle(true);

    private SmtpEmailSender sender(String password) {
        return new SmtpEmailSender("localhost", ServerSetupTest.SMTP.getPort(), USER, password,
                "Taller AutoGest", false);
    }

    @Test
    void sendsSubjectBodyAndSenderName() throws Exception {
        sender(PASSWORD).send("juan@cliente.test", "Juan Pérez", "Tu vehículo está listo",
                "Hola Juan, ya puedes recogerlo.");

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertEquals(1, received.length);
        MimeMessage message = received[0];
        assertEquals("Tu vehículo está listo", message.getSubject());
        assertEquals("juan@cliente.test", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals(USER, from.getAddress());
        assertEquals("Taller AutoGest", from.getPersonal());
        String body = GreenMailUtil.getBody(message);
        assertTrue(body.contains("ya puedes recogerlo"), body);
        assertTrue(body.contains("Taller AutoGest"), "Lleva la firma del taller: " + body);
    }

    @Test
    void spacesInTheAppPasswordAreIgnored() {
        new SmtpEmailSender("localhost", ServerSetupTest.SMTP.getPort(), USER, "clave-de- aplicacion",
                null, false).send("ana@cliente.test", null, "Prueba", "Texto");

        assertEquals(1, greenMail.getReceivedMessages().length);
    }

    @Test
    void wrongPasswordFailsWithAClearMessage() {
        String message = assertThrows(AutoGestException.class,
                () -> sender("otra-clave").send("juan@cliente.test", null, "Asunto", "Texto")).getMessage();

        assertTrue(message.contains("contraseña"), message);
        assertEquals(0, greenMail.getReceivedMessages().length);
    }

    @Test
    void unreachableServerFails() {
        greenMail.stop();

        assertThrows(AutoGestException.class, () -> sender(PASSWORD).send("juan@cliente.test", null, "A", "B"));
    }

    @Test
    void missingRecipientOrCredentialsAreRejected() {
        assertThrows(ValidationException.class, () -> sender(PASSWORD).send(" ", null, "A", "B"));
        assertThrows(ValidationException.class,
                () -> new SmtpEmailSender("smtp.gmail.com", 587, USER, "", null, true));
    }
}
