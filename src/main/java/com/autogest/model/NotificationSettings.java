package com.autogest.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configuración de las notificaciones (Configuración → Notificaciones): qué
 * avisos automáticos se envían y los textos editados de las plantillas.
 *
 * <p>Las plantillas se identifican por el nombre de su constante en
 * {@code NotificationTemplate} ({@code VEHICLE_READY}, {@code INVOICE_PAID}…).
 * Una plantilla sin texto propio usa el original.</p>
 *
 * <p>Principles: Information Expert.</p>
 */
public class NotificationSettings {

    /** Asunto y mensaje propios de una plantilla. */
    public record TemplateText(String subject, String message) {
    }

    private boolean vehicleReadyEnabled = true;
    private boolean billingNoticesEnabled = true;
    private boolean attachInvoice = true;
    private final Map<String, TemplateText> templates = new LinkedHashMap<>();

    /**
     * Valores de fábrica: todos los avisos activos, la factura adjunta y los
     * textos originales.
     *
     * @return configuración nueva
     */
    public static NotificationSettings defaults() {
        return new NotificationSettings();
    }

    /** Avisar al cliente cuando la orden pasa a Lista. */
    public boolean isVehicleReadyEnabled() {
        return vehicleReadyEnabled;
    }

    public void setVehicleReadyEnabled(boolean vehicleReadyEnabled) {
        this.vehicleReadyEnabled = vehicleReadyEnabled;
    }

    /** Enviar los avisos de facturación: factura disponible, pago recibido y factura vencida. */
    public boolean isBillingNoticesEnabled() {
        return billingNoticesEnabled;
    }

    public void setBillingNoticesEnabled(boolean billingNoticesEnabled) {
        this.billingNoticesEnabled = billingNoticesEnabled;
    }

    /** Adjuntar la factura en PDF y PNG a los correos de Facturación. */
    public boolean isAttachInvoice() {
        return attachInvoice;
    }

    public void setAttachInvoice(boolean attachInvoice) {
        this.attachInvoice = attachInvoice;
    }

    /**
     * Texto propio de una plantilla.
     *
     * @param template nombre de la plantilla
     * @return asunto y mensaje propios, o {@code null} si usa el original
     */
    public TemplateText getTemplate(String template) {
        return templates.get(template);
    }

    /**
     * Define (o quita, con {@code null}) el texto propio de una plantilla.
     *
     * @param template nombre de la plantilla
     * @param text     asunto y mensaje, o {@code null} para volver al original
     */
    public void setTemplate(String template, TemplateText text) {
        if (text == null) {
            templates.remove(template);
        } else {
            templates.put(template, text);
        }
    }

    /** Plantillas con texto propio. */
    public Map<String, TemplateText> getTemplates() {
        return Map.copyOf(templates);
    }
}
