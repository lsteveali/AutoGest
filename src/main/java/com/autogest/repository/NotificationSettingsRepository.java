package com.autogest.repository;

import com.autogest.dao.SettingsDao;
import com.autogest.model.NotificationSettings;
import com.autogest.model.NotificationSettings.TemplateText;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guarda la configuración de notificaciones en {@code app_settings} con las
 * claves {@code notifications.*}:
 * <ul>
 *   <li>{@code notifications.vehicleReady}, {@code notifications.billingNotices}
 *       y {@code notifications.attachInvoice}: {@code true}/{@code false};</li>
 *   <li>{@code notifications.template.<PLANTILLA>.subject} y {@code .message}:
 *       el texto propio; vacío = texto original.</li>
 * </ul>
 * Usa el {@link SettingsDao} de Dev 1 sin cambiarlo.
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public class NotificationSettingsRepository {

    static final String PREFIX = "notifications.";
    static final String VEHICLE_READY = PREFIX + "vehicleReady";
    static final String BILLING = PREFIX + "billingNotices";
    static final String ATTACH = PREFIX + "attachInvoice";
    static final String TEMPLATE = PREFIX + "template.";

    private final SettingsDao dao;

    /**
     * @param dao acceso a {@code app_settings}
     */
    public NotificationSettingsRepository(SettingsDao dao) {
        this.dao = dao;
    }

    /**
     * @param templates nombres de las plantillas que existen
     * @return la configuración guardada, con los valores de fábrica donde falte
     */
    public NotificationSettings load(Collection<String> templates) {
        Map<String, String> values = dao.findByPrefix(PREFIX);
        NotificationSettings settings = NotificationSettings.defaults();
        settings.setVehicleReadyEnabled(flag(values.get(VEHICLE_READY), true));
        settings.setBillingNoticesEnabled(flag(values.get(BILLING), true));
        settings.setAttachInvoice(flag(values.get(ATTACH), true));
        for (String template : templates) {
            String subject = values.get(TEMPLATE + template + ".subject");
            String message = values.get(TEMPLATE + template + ".message");
            if (message != null && !message.isBlank()) {
                settings.setTemplate(template, new TemplateText(
                        subject == null || subject.isBlank() ? null : subject, message));
            }
        }
        return settings;
    }

    /**
     * Guarda todo en una transacción. Las plantillas sin texto propio se
     * guardan vacías, así vuelven al original aunque antes se hubieran editado.
     *
     * @param settings  configuración ya validada
     * @param templates nombres de las plantillas que existen
     */
    public void save(NotificationSettings settings, Collection<String> templates) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(VEHICLE_READY, String.valueOf(settings.isVehicleReadyEnabled()));
        values.put(BILLING, String.valueOf(settings.isBillingNoticesEnabled()));
        values.put(ATTACH, String.valueOf(settings.isAttachInvoice()));
        for (String template : templates) {
            TemplateText text = settings.getTemplate(template);
            values.put(TEMPLATE + template + ".subject", text == null || text.subject() == null ? "" : text.subject());
            values.put(TEMPLATE + template + ".message", text == null ? "" : text.message());
        }
        dao.saveAll(values);
    }

    private static boolean flag(String value, boolean fallback) {
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }
}
