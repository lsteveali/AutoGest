package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.NotificationSettings;
import com.autogest.model.NotificationSettings.TemplateText;
import com.autogest.repository.NotificationSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link NotificationSettingsService}, del repositorio sobre un
 * {@code app_settings} en memoria y de cómo las plantillas toman los textos
 * editados y el nombre del taller.
 */
class NotificationSettingsServiceTest {

    private MemorySettingsDao dao;
    private String workshop;
    private int workshopReads;
    private MovableClock clock;
    private NotificationSettingsService service;

    @BeforeEach
    void setUp() {
        dao = new MemorySettingsDao();
        workshop = "Taller El Bulldog";
        clock = new MovableClock();
        service = new NotificationSettingsService(new NotificationSettingsRepository(dao), () -> {
            workshopReads++;
            return workshop;
        }, clock);
    }

    @AfterEach
    void tearDown() {
        NotificationTemplate.useTextSource(null);
    }

    @Test
    void factoryValuesHaveEveryNoticeOnAndNoCustomTexts() {
        NotificationSettings settings = service.load();

        assertTrue(settings.isVehicleReadyEnabled());
        assertTrue(settings.isBillingNoticesEnabled());
        assertTrue(settings.isAttachInvoice());
        assertTrue(settings.getTemplates().isEmpty());
    }

    @Test
    void togglesAndTextsAreSavedAndReadBack() {
        NotificationSettings settings = NotificationSettings.defaults();
        settings.setVehicleReadyEnabled(false);
        settings.setAttachInvoice(false);
        settings.setTemplate("VEHICLE_READY", new TemplateText("  ¡{cliente}, ya está! ", " Pasa por tu {vehiculo}. "));
        service.update(settings);

        NotificationSettings read = new NotificationSettingsService(new NotificationSettingsRepository(dao), null).load();

        assertFalse(read.isVehicleReadyEnabled());
        assertTrue(read.isBillingNoticesEnabled());
        assertFalse(read.isAttachInvoice());
        assertEquals(new TemplateText("¡{cliente}, ya está!", "Pasa por tu {vehiculo}."), read.getTemplate("VEHICLE_READY"));
    }

    @Test
    void textEqualToTheOriginalIsNotStored() {
        NotificationSettings settings = NotificationSettings.defaults();
        NotificationTemplate template = NotificationTemplate.INVOICE_PAID;
        settings.setTemplate(template.name(), new TemplateText(template.getSubject(), template.getMessage()));

        service.update(settings);

        assertNull(service.load().getTemplate(template.name()));
    }

    @Test
    void clearingATextGoesBackToTheOriginal() {
        NotificationSettings settings = NotificationSettings.defaults();
        settings.setTemplate("GENERAL", new TemplateText(null, "Hola {cliente}"));
        service.update(settings);

        settings.setTemplate("GENERAL", null);
        service.update(settings);

        assertNull(service.load().getTemplate("GENERAL"));
    }

    @Test
    void invalidTextsAreRejected() {
        assertThrows(ValidationException.class, () -> save("GENERAL", null, "   "));
        assertThrows(ValidationException.class, () -> save("GENERAL", null, "Hola {clinete}"));
        assertThrows(ValidationException.class, () -> save("GENERAL", "Asunto {nombre}", "Hola"));
        assertThrows(ValidationException.class, () -> save("GENERAL", "x".repeat(151), "Hola"));
        assertThrows(ValidationException.class, () -> save("GENERAL", null, "x".repeat(2001)));
        assertTrue(dao.table.isEmpty(), "Nada se guarda si algo no es válido");
    }

    @Test
    void templatesUseTheEditedTextAndTheWorkshopName() {
        save("VEHICLE_READY", "Listo tu {vehiculo}", "Hola {cliente}, pasa por {taller}.");
        NotificationTemplate.useTextSource(service);
        Map<String, String> values = NotificationTemplate.valuesFor(null, null, null);
        values.put("cliente", "Ana");

        assertEquals("Listo tu vehículo", NotificationTemplate.VEHICLE_READY.subject(values));
        assertEquals("Hola Ana, pasa por Taller El Bulldog.", NotificationTemplate.VEHICLE_READY.message(values));
        assertEquals(NotificationTemplate.GENERAL.getMessage(), NotificationTemplate.GENERAL.currentMessage());
    }

    @Test
    void withoutCustomSubjectTheOriginalSubjectIsUsed() {
        save("PROMOTION", null, "Hola {cliente}, 20 % de descuento.");
        NotificationTemplate.useTextSource(service);

        assertEquals(NotificationTemplate.PROMOTION.getSubject(), NotificationTemplate.PROMOTION.currentSubject());
        assertEquals("Hola {cliente}, 20 % de descuento.", NotificationTemplate.PROMOTION.currentMessage());
    }

    @Test
    void workshopNameIsReadAgainAfterAMinute() {
        assertEquals("Taller El Bulldog", service.workshopName());
        workshop = "Taller Nuevo";
        assertEquals("Taller El Bulldog", service.workshopName());
        assertEquals(1, workshopReads);

        clock.advance(Duration.ofSeconds(61));

        assertEquals("Taller Nuevo", service.workshopName());
    }

    @Test
    void blankOrFailingWorkshopNameFallsBackToAutoGest() {
        workshop = " ";
        NotificationTemplate.useTextSource(service);
        assertEquals("AutoGest", NotificationTemplate.baseValues().get("taller"));

        NotificationTemplate.useTextSource(new NotificationSettingsService(
                new NotificationSettingsRepository(dao), () -> {
                    throw new IllegalStateException("sin conexión");
                }));
        assertEquals("AutoGest", NotificationTemplate.baseValues().get("taller"));
    }

    @Test
    void previewFillsTheFieldsWithSampleData() {
        TemplateText preview = service.preview(NotificationTemplate.INVOICE_AVAILABLE, "",
                "Hola {cliente}, tu factura {factura} por {total}.");

        assertEquals("Factura FAC-2026-001 disponible", preview.subject());
        assertEquals("Hola Carlos, tu factura FAC-2026-001 por $155.000.", preview.message());
        assertThrows(ValidationException.class,
                () -> service.preview(NotificationTemplate.GENERAL, null, "Hola {nombre}"));
    }

    @Test
    void restoreDefaultsClearsEverything() {
        NotificationSettings settings = NotificationSettings.defaults();
        settings.setBillingNoticesEnabled(false);
        settings.setTemplate("GENERAL", new TemplateText(null, "Hola"));
        service.update(settings);

        NotificationSettings restored = service.restoreDefaults();

        assertTrue(restored.isBillingNoticesEnabled());
        assertTrue(service.load().getTemplates().isEmpty());
    }

    private void save(String template, String subject, String message) {
        NotificationSettings settings = NotificationSettings.defaults();
        settings.setTemplate(template, new TemplateText(subject, message));
        service.update(settings);
    }

    /** Reloj que se puede adelantar. */
    private static final class MovableClock extends Clock {

        private Instant now = Instant.parse("2026-11-20T10:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
