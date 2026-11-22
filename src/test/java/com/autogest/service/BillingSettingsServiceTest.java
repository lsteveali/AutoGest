package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.BillingSettings;
import com.autogest.repository.BillingSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas de {@link BillingSettingsService} y {@link BillingSettingsRepository}
 * sobre un {@code app_settings} en memoria.
 */
class BillingSettingsServiceTest {

    private MemorySettingsDao dao;
    private BillingSettingsService service;

    @BeforeEach
    void setUp() {
        dao = new MemorySettingsDao();
        service = new BillingSettingsService(new BillingSettingsRepository(dao));
    }

    @Test
    void emptyTableGivesTheFactoryValues() {
        BillingSettings settings = service.load();

        assertEquals(0, settings.getTaxPercent().signum());
        assertEquals(14, settings.getDueDays());
        assertEquals("FAC", settings.getNumberPrefix());
        assertEquals(BillingSettings.ALL_PAYMENT_METHODS, settings.getPaymentMethods());
        assertNull(settings.getInvoiceNote());
    }

    @Test
    void savedSettingsAreReadBack() {
        service.update(settings("19", 30, "fe", List.of("TRANSFER", "CASH"), "  Cuenta Bancolombia 123  "));

        BillingSettings read = new BillingSettingsService(new BillingSettingsRepository(dao)).load();

        assertEquals(new BigDecimal("19"), read.getTaxPercent());
        assertEquals(new BigDecimal("0.19"), read.getTaxRate());
        assertEquals(30, read.getDueDays());
        assertEquals("FE", read.getNumberPrefix());
        assertEquals(List.of("CASH", "TRANSFER"), read.getPaymentMethods());
        assertEquals("Cuenta Bancolombia 123", read.getInvoiceNote());
    }

    @Test
    void invalidValuesAreRejected() {
        assertThrows(ValidationException.class, () -> service.update(settings("-1", 14, "FAC", List.of("CASH"), null)));
        assertThrows(ValidationException.class, () -> service.update(settings("101", 14, "FAC", List.of("CASH"), null)));
        assertThrows(ValidationException.class, () -> service.update(settings("19.555", 14, "FAC", List.of("CASH"), null)));
        assertThrows(ValidationException.class, () -> service.update(settings("19", -1, "FAC", List.of("CASH"), null)));
        assertThrows(ValidationException.class, () -> service.update(settings("19", 181, "FAC", List.of("CASH"), null)));
        assertThrows(ValidationException.class, () -> service.update(settings("19", 14, "FA C", List.of("CASH"), null)));
        assertThrows(ValidationException.class, () -> service.update(settings("19", 14, "FACTURA", List.of("CASH"), null)));
        assertThrows(ValidationException.class, () -> service.update(settings("19", 14, "FAC", List.of(), null)));
        assertThrows(ValidationException.class,
                () -> service.update(settings("19", 14, "FAC", List.of("CASH"), "x".repeat(301))));
        assertThrows(ValidationException.class, () -> service.update(null));
        assertEquals(0, dao.table.size(), "Nada se guarda si algo no es válido");
    }

    @Test
    void damagedValuesFallBackToDefaults() {
        dao.table.put("billing.taxPercent", "diecinueve");
        dao.table.put("billing.dueDays", "");
        dao.table.put("billing.paymentMethods", "BITCOIN");

        BillingSettings read = service.load();

        assertEquals(0, read.getTaxPercent().signum());
        assertEquals(14, read.getDueDays());
        assertEquals(BillingSettings.ALL_PAYMENT_METHODS, read.getPaymentMethods());
    }

    @Test
    void currentIsCachedAndRefreshedOnSave() {
        service.current();
        service.current();
        assertEquals(1, dao.reads, "La segunda consulta usa la copia en memoria");

        service.update(settings("8", 7, "FAC", List.of("CASH"), null));

        assertEquals(new BigDecimal("8"), service.current().getTaxPercent());
        assertEquals(1, dao.reads);
    }

    @Test
    void restoreDefaultsSavesTheFactoryValues() {
        service.update(settings("19", 30, "FE", List.of("CASH"), "Nota"));

        BillingSettings restored = service.restoreDefaults();

        assertEquals("FAC", restored.getNumberPrefix());
        assertEquals("FAC", service.load().getNumberPrefix());
        assertNull(service.load().getInvoiceNote());
    }

    private static BillingSettings settings(String tax, int days, String prefix, List<String> methods, String note) {
        BillingSettings settings = new BillingSettings();
        settings.setTaxPercent(new BigDecimal(tax));
        settings.setDueDays(days);
        settings.setNumberPrefix(prefix);
        settings.setPaymentMethods(methods);
        settings.setInvoiceNote(note);
        return settings;
    }
}
