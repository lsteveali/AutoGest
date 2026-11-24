package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import com.autogest.model.SystemPreferences;
import com.autogest.model.WorkshopProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link SettingsService} y de los modelos de configuración.
 */
class SettingsServiceTest {

    private FakeSettingsRepository repository;
    private SettingsService service;

    @BeforeEach
    void setUp() {
        repository = new FakeSettingsRepository();
        service = new SettingsService(repository);
    }

    private WorkshopProfile profile() {
        WorkshopProfile profile = new WorkshopProfile();
        profile.setName("  AutoGest Taller Automotriz ");
        profile.setNit("901.456.789-1");
        profile.setEmail("contacto@autogest.com");
        profile.setWebsite("  ");
        return profile;
    }

    private String errorOf(WorkshopProfile profile) {
        return assertThrows(ValidationException.class, () -> service.updateWorkshopProfile(profile)).getMessage();
    }

    // ---------- datos del taller ----------

    @Test
    void updateProfileNormalizesAndSaves() {
        WorkshopProfile saved = service.updateWorkshopProfile(profile());
        assertEquals("AutoGest Taller Automotriz", saved.getName());
        assertNull(saved.getWebsite());
        assertEquals(1, repository.saves());
        assertEquals(saved, service.getWorkshopProfile());
    }

    @Test
    void profileRequiresNameNitAndEmail() {
        WorkshopProfile noName = profile();
        noName.setName(" ");
        assertEquals("El nombre del taller es obligatorio", errorOf(noName));
        WorkshopProfile noNit = profile();
        noNit.setNit(null);
        assertEquals("El NIT es obligatorio", errorOf(noNit));
        WorkshopProfile noEmail = profile();
        noEmail.setEmail("");
        assertEquals("El correo electrónico es obligatorio.", errorOf(noEmail));
    }

    @Test
    void profileValidatesFormats() {
        WorkshopProfile badNit = profile();
        badNit.setNit("ABC-123");
        errorOf(badNit);
        WorkshopProfile badEmail = profile();
        badEmail.setEmail("contacto");
        assertEquals("Ingresa un correo válido, por ejemplo nombre@gmail.com.", errorOf(badEmail));
        WorkshopProfile badMain = profile();
        badMain.setMainEmail("sin-arroba");
        assertEquals("Correo principal: Ingresa un correo válido, por ejemplo nombre@gmail.com.", errorOf(badMain));
        assertEquals(0, repository.saves());
    }

    @Test
    void displayNamePrefersCommercialName() {
        WorkshopProfile profile = profile();
        profile.setName("AutoGest S.A.S.");
        assertEquals("AutoGest S.A.S.", profile.getDisplayName());
        profile.setCommercialName("AutoGest");
        assertEquals("AutoGest", profile.getDisplayName());
        assertTrue(profile.isComplete());
        assertFalse(new WorkshopProfile().isComplete());
        assertEquals("Colombia", new WorkshopProfile().getCountry());
    }

    // ---------- preferencias ----------

    @Test
    void defaultsMatchTheMockup() {
        SystemPreferences defaults = SystemPreferences.defaults();
        assertEquals("Español", defaults.getLanguage());
        assertEquals(10, defaults.getPageSize());
        assertEquals(30, defaults.getInactivityMinutes());
        assertTrue(defaults.isConfirmBeforeDelete());
        assertFalse(defaults.isOpenLastModule());
    }

    @Test
    void updatePreferencesValidatesPageSize() {
        SystemPreferences preferences = SystemPreferences.defaults();
        preferences.setPageSize(25);
        assertEquals(25, service.updatePreferences(preferences).getPageSize());
        preferences.setPageSize(7);
        assertThrows(ValidationException.class, () -> service.updatePreferences(preferences));
    }

    @Test
    void inactivityIsValidatedOnlyWithAutoLogout() {
        SystemPreferences preferences = SystemPreferences.defaults();
        preferences.setInactivityMinutes(1);
        assertThrows(ValidationException.class, () -> service.updatePreferences(preferences));
        preferences.setAutoLogout(false);
        service.updatePreferences(preferences);
        assertEquals(1, repository.saves());
    }

    @Test
    void preferencesRequireLanguageAndCurrency() {
        SystemPreferences preferences = SystemPreferences.defaults();
        preferences.setCurrency(null);
        assertEquals("Selecciona la moneda",
                assertThrows(ValidationException.class, () -> service.updatePreferences(preferences)).getMessage());
    }

    @Test
    void restoreDefaultsSavesFactoryValues() {
        SystemPreferences custom = SystemPreferences.defaults();
        custom.setPageSize(100);
        service.updatePreferences(custom);
        assertEquals(10, service.restoreDefaultPreferences().getPageSize());
        assertEquals(10, service.getPreferences().getPageSize());
    }
}
