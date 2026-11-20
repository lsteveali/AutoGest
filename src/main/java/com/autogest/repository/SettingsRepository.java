package com.autogest.repository;

import com.autogest.dao.SettingsDao;
import com.autogest.model.SystemPreferences;
import com.autogest.model.WorkshopProfile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Convierte la configuración guardada como clave/valor en los objetos del
 * dominio {@link WorkshopProfile} y {@link SystemPreferences}, y al revés. Si
 * una clave no existe se conserva el valor por defecto del modelo.
 *
 * <p>Principles: Indirection, Pure Fabrication, Protected Variations (el
 * formato de almacenamiento no se filtra al resto del sistema).</p>
 */
public class SettingsRepository {

    private static final String WORKSHOP = "workshop.";
    private static final String PREFERENCES = "preferences.";

    private final SettingsDao dao;

    /**
     * @param dao DAO de la tabla {@code app_settings}
     */
    public SettingsRepository(SettingsDao dao) {
        this.dao = dao;
    }

    /** Carga los datos del taller (valores por defecto si aún no se han guardado). */
    public WorkshopProfile loadWorkshopProfile() {
        Map<String, String> v = dao.findByPrefix(WORKSHOP);
        WorkshopProfile p = new WorkshopProfile();
        p.setName(get(v, WORKSHOP + "name", p.getName()));
        p.setNit(get(v, WORKSHOP + "nit", p.getNit()));
        p.setPhone(get(v, WORKSHOP + "phone", p.getPhone()));
        p.setEmail(get(v, WORKSHOP + "email", p.getEmail()));
        p.setWebsite(get(v, WORKSHOP + "website", p.getWebsite()));
        p.setWorkshopType(get(v, WORKSHOP + "workshopType", p.getWorkshopType()));
        p.setCommercialName(get(v, WORKSHOP + "commercialName", p.getCommercialName()));
        p.setDescription(get(v, WORKSHOP + "description", p.getDescription()));
        p.setAddress(get(v, WORKSHOP + "address", p.getAddress()));
        p.setWhatsapp(get(v, WORKSHOP + "whatsapp", p.getWhatsapp()));
        p.setCity(get(v, WORKSHOP + "city", p.getCity()));
        p.setPostalCode(get(v, WORKSHOP + "postalCode", p.getPostalCode()));
        p.setDepartment(get(v, WORKSHOP + "department", p.getDepartment()));
        p.setCountry(get(v, WORKSHOP + "country", p.getCountry()));
        p.setMainEmail(get(v, WORKSHOP + "mainEmail", p.getMainEmail()));
        p.setMainPhone(get(v, WORKSHOP + "mainPhone", p.getMainPhone()));
        p.setBusinessHours(get(v, WORKSHOP + "businessHours", p.getBusinessHours()));
        return p;
    }

    /** Guarda todos los datos del taller en una transacción. */
    public void saveWorkshopProfile(WorkshopProfile p) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put(WORKSHOP + "name", p.getName());
        v.put(WORKSHOP + "nit", p.getNit());
        v.put(WORKSHOP + "phone", p.getPhone());
        v.put(WORKSHOP + "email", p.getEmail());
        v.put(WORKSHOP + "website", p.getWebsite());
        v.put(WORKSHOP + "workshopType", p.getWorkshopType());
        v.put(WORKSHOP + "commercialName", p.getCommercialName());
        v.put(WORKSHOP + "description", p.getDescription());
        v.put(WORKSHOP + "address", p.getAddress());
        v.put(WORKSHOP + "whatsapp", p.getWhatsapp());
        v.put(WORKSHOP + "city", p.getCity());
        v.put(WORKSHOP + "postalCode", p.getPostalCode());
        v.put(WORKSHOP + "department", p.getDepartment());
        v.put(WORKSHOP + "country", p.getCountry());
        v.put(WORKSHOP + "mainEmail", p.getMainEmail());
        v.put(WORKSHOP + "mainPhone", p.getMainPhone());
        v.put(WORKSHOP + "businessHours", p.getBusinessHours());
        dao.saveAll(v);
    }

    /** Carga las preferencias (valores por defecto si aún no se han guardado). */
    public SystemPreferences loadPreferences() {
        Map<String, String> v = dao.findByPrefix(PREFERENCES);
        SystemPreferences p = new SystemPreferences();
        p.setLanguage(get(v, PREFERENCES + "language", p.getLanguage()));
        p.setCountry(get(v, PREFERENCES + "country", p.getCountry()));
        p.setTimeZone(get(v, PREFERENCES + "timeZone", p.getTimeZone()));
        p.setCurrency(get(v, PREFERENCES + "currency", p.getCurrency()));
        p.setDateFormat(get(v, PREFERENCES + "dateFormat", p.getDateFormat()));
        p.setTimeFormat(get(v, PREFERENCES + "timeFormat", p.getTimeFormat()));
        p.setDecimalSeparator(get(v, PREFERENCES + "decimalSeparator", p.getDecimalSeparator()));
        p.setSidebarMode(get(v, PREFERENCES + "sidebarMode", p.getSidebarMode()));
        p.setPageSize(getInt(v, PREFERENCES + "pageSize", p.getPageSize()));
        p.setDefaultSort(get(v, PREFERENCES + "defaultSort", p.getDefaultSort()));
        p.setConfirmBeforeDelete(getBoolean(v, PREFERENCES + "confirmBeforeDelete", p.isConfirmBeforeDelete()));
        p.setShowSaveConfirmation(getBoolean(v, PREFERENCES + "showSaveConfirmation", p.isShowSaveConfirmation()));
        p.setOpenLastModule(getBoolean(v, PREFERENCES + "openLastModule", p.isOpenLastModule()));
        p.setShowContextHelp(getBoolean(v, PREFERENCES + "showContextHelp", p.isShowContextHelp()));
        p.setSaveRecentFilters(getBoolean(v, PREFERENCES + "saveRecentFilters", p.isSaveRecentFilters()));
        p.setShowExtraTableInfo(getBoolean(v, PREFERENCES + "showExtraTableInfo", p.isShowExtraTableInfo()));
        p.setAutoLogout(getBoolean(v, PREFERENCES + "autoLogout", p.isAutoLogout()));
        p.setInactivityMinutes(getInt(v, PREFERENCES + "inactivityMinutes", p.getInactivityMinutes()));
        return p;
    }

    /** Guarda todas las preferencias en una transacción. */
    public void savePreferences(SystemPreferences p) {
        Map<String, String> v = new LinkedHashMap<>();
        v.put(PREFERENCES + "language", p.getLanguage());
        v.put(PREFERENCES + "country", p.getCountry());
        v.put(PREFERENCES + "timeZone", p.getTimeZone());
        v.put(PREFERENCES + "currency", p.getCurrency());
        v.put(PREFERENCES + "dateFormat", p.getDateFormat());
        v.put(PREFERENCES + "timeFormat", p.getTimeFormat());
        v.put(PREFERENCES + "decimalSeparator", p.getDecimalSeparator());
        v.put(PREFERENCES + "sidebarMode", p.getSidebarMode());
        v.put(PREFERENCES + "pageSize", String.valueOf(p.getPageSize()));
        v.put(PREFERENCES + "defaultSort", p.getDefaultSort());
        v.put(PREFERENCES + "confirmBeforeDelete", String.valueOf(p.isConfirmBeforeDelete()));
        v.put(PREFERENCES + "showSaveConfirmation", String.valueOf(p.isShowSaveConfirmation()));
        v.put(PREFERENCES + "openLastModule", String.valueOf(p.isOpenLastModule()));
        v.put(PREFERENCES + "showContextHelp", String.valueOf(p.isShowContextHelp()));
        v.put(PREFERENCES + "saveRecentFilters", String.valueOf(p.isSaveRecentFilters()));
        v.put(PREFERENCES + "showExtraTableInfo", String.valueOf(p.isShowExtraTableInfo()));
        v.put(PREFERENCES + "autoLogout", String.valueOf(p.isAutoLogout()));
        v.put(PREFERENCES + "inactivityMinutes", String.valueOf(p.getInactivityMinutes()));
        dao.saveAll(v);
    }

    private static String get(Map<String, String> values, String key, String fallback) {
        String value = values.get(key);
        return value == null || value.isEmpty() ? fallback : value;
    }

    private static int getInt(Map<String, String> values, String key, int fallback) {
        try {
            return Integer.parseInt(get(values, key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean getBoolean(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : Boolean.parseBoolean(value.trim());
    }
}
