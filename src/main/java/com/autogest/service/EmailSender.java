package com.autogest.service;

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
}
