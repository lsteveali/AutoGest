package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.NotificationSettings;
import com.autogest.model.NotificationSettings.TemplateText;
import com.autogest.repository.NotificationSettingsRepository;
import com.autogest.util.Formatter;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Casos de uso de Configuración → Notificaciones: activar o desactivar los
 * avisos automáticos, editar el asunto y el mensaje de cada plantilla y ver
 * cómo quedan con datos de ejemplo.
 *
 * <p>También es el {@link NotificationTemplate.TextSource} de la aplicación:
 * las plantillas toman de aquí los textos editados y el nombre del taller
 * («Datos del taller», de Dev 1). La configuración se guarda en memoria y se
 * renueva al guardar; el nombre del taller se vuelve a leer cada minuto, así
 * un cambio en «Datos del taller» llega a los correos sin reiniciar.</p>
 *
 * <p>Principles: GRASP Controller, SRP, Protected Variations.</p>
 */
public class NotificationSettingsService implements NotificationTemplate.TextSource {

    /** Largo máximo del asunto (el mismo que acepta NotificationService). */
    public static final int MAX_SUBJECT = 150;
    /** Largo máximo del mensaje. */
    public static final int MAX_MESSAGE = 2000;

    private static final long WORKSHOP_TTL_SECONDS = 60;

    private final NotificationSettingsRepository repository;
    private final Supplier<String> workshopName;
    private final Clock clock;
    private volatile NotificationSettings cached;
    private volatile String cachedWorkshop;
    private volatile Instant workshopReadAt;

    /**
     * @param repository   almacenamiento de la configuración
     * @param workshopName nombre del taller (de «Datos del taller»); puede ser {@code null}
     */
    public NotificationSettingsService(NotificationSettingsRepository repository, Supplier<String> workshopName) {
        this(repository, workshopName, Clock.systemDefaultZone());
    }

    NotificationSettingsService(NotificationSettingsRepository repository, Supplier<String> workshopName, Clock clock) {
        this.repository = repository;
        this.workshopName = workshopName;
        this.clock = clock;
    }

    /** Nombres de todas las plantillas, en el orden en que se muestran. */
    public static List<String> templateNames() {
        return Arrays.stream(NotificationTemplate.values()).map(Enum::name).toList();
    }

    /**
     * Lee la configuración guardada (y renueva la copia en memoria).
     *
     * @return configuración actual
     */
    public NotificationSettings load() {
        NotificationSettings settings = repository.load(templateNames());
        cached = settings;
        return settings;
    }

    /**
     * Configuración en memoria; la primera vez la lee. Si la lectura falla,
     * usa los valores de fábrica: un aviso nunca se frena por esto.
     *
     * @return configuración vigente
     */
    public NotificationSettings current() {
        NotificationSettings settings = cached;
        if (settings != null) {
            return settings;
        }
        try {
            return load();
        } catch (RuntimeException e) {
            return NotificationSettings.defaults();
        }
    }

    /**
     * Valida y guarda la configuración. Un texto igual al original se guarda
     * como «sin texto propio».
     *
     * @param settings datos del formulario
     * @return la configuración guardada
     * @throws ValidationException si un asunto o un mensaje no es válido
     */
    public NotificationSettings update(NotificationSettings settings) {
        if (settings == null) {
            throw new ValidationException("La configuración de notificaciones es obligatoria");
        }
        for (NotificationTemplate template : NotificationTemplate.values()) {
            TemplateText text = settings.getTemplate(template.name());
            if (text == null) {
                continue;
            }
            settings.setTemplate(template.name(), normalize(template, text.subject(), text.message()));
        }
        repository.save(settings, templateNames());
        cached = settings;
        return settings;
    }

    /**
     * Valida un asunto y un mensaje de una plantilla.
     *
     * @return el texto limpio, o {@code null} si es igual al original
     * @throws ValidationException si falta el mensaje, es muy largo o usa un campo que no existe
     */
    static TemplateText normalize(NotificationTemplate template, String subject, String message) {
        String cleanSubject = subject == null || subject.isBlank() ? null : subject.trim();
        String cleanMessage = message == null ? "" : message.trim();
        String name = "«" + template.getTitle() + "»";
        if (cleanMessage.isEmpty()) {
            throw new ValidationException("El mensaje de " + name + " no puede quedar vacío");
        }
        if (cleanMessage.length() > MAX_MESSAGE) {
            throw new ValidationException("El mensaje de " + name + " no puede superar " + MAX_MESSAGE + " caracteres");
        }
        if (cleanSubject != null && cleanSubject.length() > MAX_SUBJECT) {
            throw new ValidationException("El asunto de " + name + " no puede superar " + MAX_SUBJECT + " caracteres");
        }
        // Un campo mal escrito ({clinete}) se detecta al completarlo.
        NotificationTemplate.fill(cleanMessage, Map.of());
        if (cleanSubject != null) {
            NotificationTemplate.fill(cleanSubject, Map.of());
        }
        boolean sameSubject = cleanSubject == null || cleanSubject.equals(template.getSubject());
        if (sameSubject && cleanMessage.equals(template.getMessage())) {
            return null;
        }
        return new TemplateText(cleanSubject, cleanMessage);
    }

    /**
     * Restaura y guarda los valores de fábrica (avisos activos y textos originales).
     *
     * @return la configuración por defecto
     */
    public NotificationSettings restoreDefaults() {
        return update(NotificationSettings.defaults());
    }

    /**
     * Cómo se vería una plantilla con datos de ejemplo.
     *
     * @param template plantilla
     * @param subject  asunto escrito (vacío = el original)
     * @param message  mensaje escrito
     * @return asunto y mensaje completos
     * @throws ValidationException si el texto usa un campo que no existe
     */
    public TemplateText preview(NotificationTemplate template, String subject, String message) {
        Map<String, String> values = sampleValues();
        String usedSubject = subject == null || subject.isBlank() ? template.getSubject() : subject;
        return new TemplateText(NotificationTemplate.fill(usedSubject, values),
                NotificationTemplate.fill(message == null ? "" : message, values));
    }

    /** Datos de ejemplo para la vista previa. */
    Map<String, String> sampleValues() {
        Map<String, String> values = NotificationTemplate.baseValues();
        values.put("cliente", "Carlos");
        values.put("vehiculo", "Toyota Corolla");
        values.put("placa", "ABC123");
        values.put("orden", "#0045");
        values.put("fecha", Formatter.date(LocalDate.now(clock).plusDays(7)));
        values.put("factura", "FAC-2026-001");
        values.put("total", Formatter.money(new java.math.BigDecimal("155000")));
        values.put("vencimiento", Formatter.date(LocalDate.now(clock).plusDays(14)));
        return values;
    }

    // ---------- origen de los textos de las plantillas ----------

    @Override
    public String subject(NotificationTemplate template) {
        TemplateText text = current().getTemplate(template.name());
        return text == null ? null : text.subject();
    }

    @Override
    public String message(NotificationTemplate template) {
        TemplateText text = current().getTemplate(template.name());
        return text == null ? null : text.message();
    }

    @Override
    public String workshopName() {
        Instant now = clock.instant();
        Instant readAt = workshopReadAt;
        if (readAt != null && readAt.plusSeconds(WORKSHOP_TTL_SECONDS).isAfter(now)) {
            return cachedWorkshop;
        }
        String name;
        try {
            name = workshopName == null ? null : workshopName.get();
        } catch (RuntimeException e) {
            name = cachedWorkshop;
        }
        cachedWorkshop = name == null || name.isBlank() ? null : name.trim();
        workshopReadAt = now;
        return cachedWorkshop;
    }
}
