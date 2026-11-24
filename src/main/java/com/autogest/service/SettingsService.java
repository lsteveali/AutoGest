package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.SystemPreferences;
import com.autogest.model.WorkshopProfile;
import com.autogest.repository.SettingsRepository;
import com.autogest.util.ContactValidator;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Casos de uso de «Datos del taller» y «Preferencias del sistema» (Sprint 8,
 * Desarrollador 1): consultar, validar y guardar la configuración, y
 * restaurar las preferencias de fábrica. El resto de Configuración (usuarios,
 * seguridad, copias de seguridad...) lo entrega el Líder.
 *
 * <p>Principles: GRASP Controller, SRP, DIP, Low Coupling.</p>
 */
public class SettingsService {

    /** Tamaños de página permitidos en las tablas. */
    public static final List<Integer> PAGE_SIZES = List.of(10, 25, 50, 100);

    private static final Pattern NIT = Pattern.compile("^[0-9.]+(-[0-9])?$");
    private static final int MIN_INACTIVITY = 5;
    private static final int MAX_INACTIVITY = 240;

    private final SettingsRepository repository;

    /**
     * @param repository repositorio de configuración
     */
    public SettingsService(SettingsRepository repository) {
        this.repository = repository;
    }

    /** Datos actuales del taller. */
    public WorkshopProfile getWorkshopProfile() {
        return repository.loadWorkshopProfile();
    }

    /**
     * Valida y guarda los datos del taller.
     *
     * @param profile datos del formulario
     * @return los datos guardados (normalizados)
     * @throws ValidationException si falta el nombre, el NIT o el correo, o tienen formato inválido
     */
    public WorkshopProfile updateWorkshopProfile(WorkshopProfile profile) {
        if (profile == null) {
            throw new ValidationException("Los datos del taller son obligatorios");
        }
        normalize(profile);
        if (profile.getName() == null) {
            throw new ValidationException("El nombre del taller es obligatorio");
        }
        if (profile.getNit() == null) {
            throw new ValidationException("El NIT es obligatorio");
        }
        if (!NIT.matcher(profile.getNit()).matches()) {
            throw new ValidationException("El NIT solo admite números, puntos y un dígito de verificación (901.456.789-1)");
        }
        // Correos con formato estricto y teléfonos con indicativo según el país (ContactValidator).
        profile.setEmail(ContactValidator.email(profile.getEmail()));
        profile.setMainEmail(inField("Correo principal", () -> ContactValidator.optionalEmail(profile.getMainEmail())));
        profile.setPhone(inField("Teléfono", () -> ContactValidator.optionalPhone(profile.getPhone())));
        profile.setWhatsapp(inField("WhatsApp", () -> ContactValidator.optionalPhone(profile.getWhatsapp())));
        profile.setMainPhone(inField("Teléfono principal", () -> ContactValidator.optionalPhone(profile.getMainPhone())));
        repository.saveWorkshopProfile(profile);
        return profile;
    }

    /** Preferencias actuales del sistema. */
    public SystemPreferences getPreferences() {
        return repository.loadPreferences();
    }

    /**
     * Valida y guarda las preferencias.
     *
     * @throws ValidationException si el tamaño de página o el tiempo de inactividad no son válidos
     */
    public SystemPreferences updatePreferences(SystemPreferences preferences) {
        if (preferences == null) {
            throw new ValidationException("Las preferencias son obligatorias");
        }
        if (!PAGE_SIZES.contains(preferences.getPageSize())) {
            throw new ValidationException("Los registros por página deben ser 10, 25, 50 o 100");
        }
        if (preferences.isAutoLogout() && (preferences.getInactivityMinutes() < MIN_INACTIVITY
                || preferences.getInactivityMinutes() > MAX_INACTIVITY)) {
            throw new ValidationException("El tiempo de inactividad debe estar entre "
                    + MIN_INACTIVITY + " y " + MAX_INACTIVITY + " minutos");
        }
        requireText(preferences.getLanguage(), "el idioma");
        requireText(preferences.getCurrency(), "la moneda");
        requireText(preferences.getDateFormat(), "el formato de fecha");
        repository.savePreferences(preferences);
        return preferences;
    }

    /**
     * Restaura y guarda las preferencias de fábrica.
     *
     * @return las preferencias por defecto
     */
    public SystemPreferences restoreDefaultPreferences() {
        SystemPreferences defaults = SystemPreferences.defaults();
        repository.savePreferences(defaults);
        return defaults;
    }

    private static void normalize(WorkshopProfile p) {
        p.setName(clean(p.getName()));
        p.setNit(clean(p.getNit()));
        p.setPhone(clean(p.getPhone()));
        p.setEmail(clean(p.getEmail()));
        p.setWebsite(clean(p.getWebsite()));
        p.setWorkshopType(clean(p.getWorkshopType()));
        p.setCommercialName(clean(p.getCommercialName()));
        p.setDescription(clean(p.getDescription()));
        p.setAddress(clean(p.getAddress()));
        p.setWhatsapp(clean(p.getWhatsapp()));
        p.setCity(clean(p.getCity()));
        p.setPostalCode(clean(p.getPostalCode()));
        p.setDepartment(clean(p.getDepartment()));
        p.setCountry(clean(p.getCountry()));
        p.setMainEmail(clean(p.getMainEmail()));
        p.setMainPhone(clean(p.getMainPhone()));
        p.setBusinessHours(clean(p.getBusinessHours()));
    }

    /** Valida un campo y, si falla, dice cuál fue (hay varios correos y teléfonos). */
    private static String inField(String field, java.util.function.Supplier<String> check) {
        try {
            return check.get();
        } catch (ValidationException error) {
            throw new ValidationException(field + ": " + error.getMessage());
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("Selecciona " + name);
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
