package com.autogest.config;

import com.autogest.service.EmailSender;
import com.autogest.service.SmtpEmailSender;

import java.util.Properties;

/**
 * Arma el envío de correos a partir de {@code application.local.properties}.
 * Si faltan {@code mail.username} o {@code mail.password}, devuelve
 * {@code null} y las notificaciones por correo se simulan como antes: así la
 * aplicación funciona igual en un equipo sin cuenta de correo configurada.
 *
 * <p>Claves (con sus valores por defecto para Gmail):</p>
 * <ul>
 *   <li>{@code mail.username}: dirección de Gmail del taller (obligatoria).</li>
 *   <li>{@code mail.password}: contraseña de aplicación de Google (obligatoria).</li>
 *   <li>{@code mail.from.name}: nombre del remitente ({@code AutoGest}).</li>
 *   <li>{@code mail.smtp.host}: servidor ({@code smtp.gmail.com}).</li>
 *   <li>{@code mail.smtp.port}: puerto ({@code 587}).</li>
 *   <li>{@code mail.smtp.starttls}: cifrado STARTTLS ({@code true}).</li>
 * </ul>
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public final class EmailConfig {

    static final String DEFAULT_HOST = "smtp.gmail.com";
    static final int DEFAULT_PORT = 587;

    private EmailConfig() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Envío de correos según el archivo local.
     *
     * @return el envío SMTP, o {@code null} si no hay cuenta configurada
     */
    static EmailSender fromLocalFile() {
        return fromProperties(DatabaseConnection.loadProperties());
    }

    /**
     * Envío de correos según las propiedades dadas.
     *
     * @param properties propiedades con las claves {@code mail.*}
     * @return el envío SMTP, o {@code null} si faltan usuario o contraseña
     */
    public static EmailSender fromProperties(Properties properties) {
        String username = value(properties, "mail.username");
        String password = value(properties, "mail.password");
        if (username == null || password == null) {
            return null;
        }
        String host = value(properties, "mail.smtp.host");
        String port = value(properties, "mail.smtp.port");
        String starttls = value(properties, "mail.smtp.starttls");
        return new SmtpEmailSender(host == null ? DEFAULT_HOST : host,
                port == null ? DEFAULT_PORT : Integer.parseInt(port),
                username, password, value(properties, "mail.from.name"),
                starttls == null || Boolean.parseBoolean(starttls));
    }

    private static String value(Properties properties, String key) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? null : value.trim();
    }
}
