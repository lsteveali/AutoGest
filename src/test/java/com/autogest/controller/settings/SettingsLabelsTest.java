package com.autogest.controller.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.model.SystemPreferences;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las opciones de Configuración (sin JavaFX).
 */
class SettingsLabelsTest {

    @Test
    void minutesTextUsesHoursWhenExact() {
        assertEquals("30 minutos", SettingsLabels.minutesText(30));
        assertEquals("1 hora", SettingsLabels.minutesText(60));
        assertEquals("2 horas", SettingsLabels.minutesText(120));
    }

    @Test
    void defaultsAreAmongTheOptions() {
        SystemPreferences defaults = SystemPreferences.defaults();
        assertTrue(SettingsLabels.languages().contains(defaults.getLanguage()));
        assertTrue(SettingsLabels.timeZones().contains(defaults.getTimeZone()));
        assertTrue(SettingsLabels.currencies().contains(defaults.getCurrency()));
        assertTrue(SettingsLabels.dateFormats().contains(defaults.getDateFormat()));
        assertTrue(SettingsLabels.timeFormats().contains(defaults.getTimeFormat()));
        assertTrue(SettingsLabels.decimalSeparators().contains(defaults.getDecimalSeparator()));
        assertTrue(SettingsLabels.sidebarModes().contains(defaults.getSidebarMode()));
        assertTrue(SettingsLabels.sortOrders().contains(defaults.getDefaultSort()));
        assertTrue(SettingsLabels.inactivityMinutes().contains(defaults.getInactivityMinutes()));
    }
}
