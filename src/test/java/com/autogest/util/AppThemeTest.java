package com.autogest.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** El botón de tema de la barra superior pasa de un tema al otro. */
class AppThemeTest {

    @Test
    void toggledSwitchesBetweenLightAndDark() {
        assertEquals(AppTheme.DARK, AppTheme.LIGHT.toggled());
        assertEquals(AppTheme.LIGHT, AppTheme.DARK.toggled());
    }

    @Test
    void unknownSavedThemeIsLight() {
        assertEquals(AppTheme.LIGHT, AppTheme.parse("AZUL"));
        assertEquals(AppTheme.LIGHT, AppTheme.parse(null));
        assertEquals(AppTheme.DARK, AppTheme.parse("DARK"));
    }
}
