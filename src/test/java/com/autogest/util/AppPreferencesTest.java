package com.autogest.util;

import com.autogest.model.SystemPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas de {@link AppPreferences}. */
class AppPreferencesTest {

    @AfterEach
    void reset() {
        AppPreferences.update(null);
    }

    @Test
    void factoryValuesUntilPreferencesAreLoaded() {
        AppPreferences.update(null);

        assertEquals(10, AppPreferences.pageSize());
        assertTrue(AppPreferences.confirmBeforeDelete());
        assertTrue(AppPreferences.showSaveConfirmation());
        assertEquals(30, AppPreferences.inactivityMinutes());
    }

    @Test
    void savedPreferencesAreApplied() {
        SystemPreferences preferences = SystemPreferences.defaults();
        preferences.setPageSize(25);
        preferences.setConfirmBeforeDelete(false);
        preferences.setShowSaveConfirmation(false);
        preferences.setInactivityMinutes(60);

        AppPreferences.update(preferences);

        assertEquals(25, AppPreferences.pageSize());
        assertFalse(AppPreferences.confirmBeforeDelete());
        assertFalse(AppPreferences.showSaveConfirmation());
        assertEquals(60, AppPreferences.inactivityMinutes());
    }

    @Test
    void autoLogoutOffMeansNoTimeout() {
        SystemPreferences preferences = SystemPreferences.defaults();
        preferences.setAutoLogout(false);

        AppPreferences.update(preferences);

        assertEquals(0, AppPreferences.inactivityMinutes());
    }

    @Test
    void invalidPageSizeFallsBackToTen() {
        SystemPreferences preferences = SystemPreferences.defaults();
        preferences.setPageSize(0);

        AppPreferences.update(preferences);

        assertEquals(10, AppPreferences.pageSize());
    }

    @Test
    void listenersHearEveryChange() {
        List<SystemPreferences> heard = new ArrayList<>();
        Consumer<SystemPreferences> listener = heard::add;
        AppPreferences.addListener(listener);
        SystemPreferences preferences = SystemPreferences.defaults();

        AppPreferences.update(preferences);
        AppPreferences.removeListener(listener);
        AppPreferences.update(null);

        assertEquals(1, heard.size());
        assertSame(preferences, heard.get(0));
    }
}
