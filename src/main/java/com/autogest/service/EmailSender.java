package com.autogest.service;

import com.autogest.exception.AutoGestException;

import java.util.List;

/**
 * Envía un correo a un cliente. Separa el «cómo se envía» (SMTP de Gmail,
 * otro proveedor o nada en un equipo sin configuración) de las reglas de las
 * notificaciones, que solo piden «envía este correo».
 *
 * <p>Principles: Protected Variations, DIP, Polymorphism.</p>
 */
public interface EmailSender {

    /**
     * Envía el correo.
     *
     * @param to      dirección del destinatario
     * @param toName  nombre del destinatario (puede ser {@code null})
     * @param subject asunto
     * @param body    texto del mensaje
     * @throws com.autogest.exception.AutoGestException si el correo no se pudo enviar
     */
    void send(String to, String toName, String subject, String body);

    /**
     * Envía el correo con archivos adjuntos. Sin adjuntos equivale a
     * {@link #send(String, String, String, String)}.
     *
     * @param attachments archivos a adjuntar (puede estar vacía)
     * @throws com.autogest.exception.AutoGestException si el correo no se pudo enviar
     *         o este medio de envío no admite adjuntos
     */
    default void send(String to, String toName, String subject, String body, List<EmailAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            send(to, toName, subject, body);
            return;
        }
        throw new AutoGestException("Este medio de envío no admite archivos adjuntos");
    }
}
