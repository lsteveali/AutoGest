package com.autogest.util;

import com.autogest.util.LoginAppearance.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoginAppearanceTest {

    @Test
    void colorsAreNormalizedAndInvalidOnesFallBackToSilver() {
        assertEquals("#B3261E", LoginAppearance.normalizeColor(" b3261e "));
        assertEquals("#1F4E8C", LoginAppearance.normalizeColor("#1f4e8c"));
        assertEquals(LoginAppearance.DEFAULT_COLOR, LoginAppearance.normalizeColor("rojo"));
        assertEquals(LoginAppearance.DEFAULT_COLOR, LoginAppearance.normalizeColor("#12345"));
        assertEquals(LoginAppearance.DEFAULT_COLOR, LoginAppearance.normalizeColor(null));
    }

    @Test
    void missingVehicleIsTheTruck() {
        LoginAppearance appearance = new LoginAppearance(null, "#E0B021");

        assertEquals(Vehicle.TRUCK, appearance.vehicle());
        assertEquals("Amarillo", appearance.colorName());
    }

    @Test
    void colorOutsideThePaletteIsCustom() {
        assertEquals("Personalizado", LoginAppearance.DEFAULT.withColor("#123456").colorName());
        assertEquals("Plata", LoginAppearance.DEFAULT.colorName());
    }

    @Test
    void copiesChangeOnlyOneField() {
        LoginAppearance classic = LoginAppearance.DEFAULT.withVehicle(Vehicle.CLASSIC);

        assertEquals(Vehicle.CLASSIC, classic.vehicle());
        assertEquals(LoginAppearance.DEFAULT_COLOR, classic.color());
        assertEquals("#2A2D31", classic.withColor("#2a2d31").color());
    }
}
