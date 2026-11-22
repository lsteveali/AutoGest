package com.autogest.service;

import com.autogest.model.Notification;

import java.util.List;

/**
 * Decide qué archivos acompañan el correo de una notificación (por ejemplo,
 * la factura en los avisos de Facturación). Así {@link NotificationService}
 * no necesita saber de facturas ni de PDF.
 *
 * <p>Principles: Protected Variations, DIP, Indirection.</p>
 */
@FunctionalInterface
public interface EmailAttachmentProvider {

    /**
     * Adjuntos del correo de la notificación.
     *
     * @param notification notificación a enviar (con su tipo, cliente y orden)
     * @return adjuntos; lista vacía si no lleva ninguno
     * @throws com.autogest.exception.AutoGestException si no se pudieron preparar
     */
    List<EmailAttachment> attachmentsFor(Notification notification);
}
