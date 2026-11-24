package com.autogest.controller.components;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** El aro de la plataforma toma el color del carro. */
class CarTurntableTest {

    @Test
    void darkAndStrongColorsAreUsedAsTheyAre() {
        Color red = Color.web("#B3261E");
        Color blue = Color.web("#1F4E8C");

        assertEquals(red, CarTurntable.accentFor(red));
        assertEquals(blue, CarTurntable.accentFor(blue));
    }

    @Test
    void lightColorsAreDarkenedToStayVisibleOnTheWhitePlatform() {
        for (String hex : new String[]{"#E9ECEF", "#C2C8CF", "#E0B021"}) {
            Color paint = Color.web(hex);
            Color ring = CarTurntable.accentFor(paint);

            assertTrue(ring.getBrightness() < paint.getBrightness(), hex);
            assertEquals(paint.getHue(), ring.getHue(), 0.5, hex);
        }
    }
}
