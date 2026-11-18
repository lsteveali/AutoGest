package com.autogest.service;

import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

/**
 * Envía los correos por SMTP con usuario y contraseña. Pensado para Gmail
 * ({@code smtp.gmail.com}, puerto 587 con STARTTLS y una contraseña de
 * aplicación de Google), pero sirve para cualquier servidor SMTP.
 *
 * <p>Las credenciales llegan desde {@code application.local.properties}
 * (fuera de Git), nunca desde el código. Cada envío tiene un tiempo límite
 * para que un servidor que no responde no deje esperando a la aplicación.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public class SmtpEmailSender implements EmailSender {

    /** Milisegundos máximos para conectar, leer y escribir. */
    private static final String TIMEOUT_MS = "15000";

    private final Session session;
    private final String fromAddress;
    private final String fromName;

    /**
     * @param host     servidor SMTP, por ejemplo {@code smtp.gmail.com}
     * @param port     puerto (587 con STARTTLS)
     * @param username usuario (en Gmail, la dirección completa); también es el remitente
     * @param password contraseña (en Gmail, la contraseña de aplicación)
     * @param fromName nombre visible del remitente, por ejemplo «AutoGest»
     * @param starttls {@code true} para cifrar la conexión con STARTTLS
     */
    public SmtpEmailSender(String host, int port, String username, String password, String fromName,
                           boolean starttls) {
        if (isBlank(host) || isBlank(username) || isBlank(password)) {
            throw new ValidationException("Faltan el servidor, el usuario o la contraseña del correo");
        }
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", String.valueOf(port));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        properties.put("mail.smtp.starttls.required", String.valueOf(starttls));
        properties.put("mail.smtp.connectiontimeout", TIMEOUT_MS);
        properties.put("mail.smtp.timeout", TIMEOUT_MS);
        properties.put("mail.smtp.writetimeout", TIMEOUT_MS);
        String user = username.trim();
        String secret = password.replace(" ", "");
        this.session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, secret);
            }
        });
        this.fromAddress = user;
        this.fromName = isBlank(fromName) ? "AutoGest" : fromName.trim();
    }

    @Override
    public void send(String to, String toName, String subject, String body) {
        if (isBlank(to)) {
            throw new ValidationException("El cliente no tiene correo");
        }
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress, fromName, StandardCharsets.UTF_8.name()));
            message.setRecipient(Message.RecipientType.TO, isBlank(toName)
                    ? new InternetAddress(to.trim(), true)
                    : new InternetAddress(to.trim(), toName.trim(), StandardCharsets.UTF_8.name()));
            message.setSubject(subject == null ? "" : subject, StandardCharsets.UTF_8.name());
            message.setText(body == null ? "" : body + signature(), StandardCharsets.UTF_8.name());
            message.setSentDate(new Date());
            Transport.send(message);
        } catch (MessagingException | UnsupportedEncodingException e) {
            throw new AutoGestException("No se pudo enviar el correo a " + to + ": " + reason(e), e);
        }
    }

    private String signature() {
        return "\n\n—\n" + fromName + "\nEste mensaje se envió desde AutoGest. Si tienes preguntas, responde a este correo.";
    }

    /** Motivo legible del error (sin datos de la cuenta). */
    private static String reason(Exception e) {
        if (e instanceof jakarta.mail.AuthenticationFailedException) {
            return "el usuario o la contraseña de aplicación no son válidos";
        }
        if (e instanceof jakarta.mail.internet.AddressException) {
            return "la dirección no es válida";
        }
        return "no hubo conexión con el servidor de correo";
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }
}
