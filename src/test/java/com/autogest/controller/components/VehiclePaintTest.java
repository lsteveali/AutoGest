package com.autogest.controller.components;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehiclePaintTest {

    private static double brightness(Color color) {
        return color.getRed() + color.getGreen() + color.getBlue();
    }

    @Test
    void silverKeepsTheOriginalLook() {
        Color[] tones = VehiclePaint.tones(Color.web("#C2C8CF"));

        assertClose(Color.web("#D5DADF"), tones[0]);
        assertClose(Color.web("#8B929B"), tones[3]);
        assertClose(Color.web("#5E646B"), tones[6]);
    }

    @Test
    void skyIsLighterAndHorizonDarkerForAnyColor() {
        for (String hex : new String[]{"#B3261E", "#1F4E8C", "#2A2D31", "#E0B021"}) {
            Color base = Color.web(hex);
            Color[] tones = VehiclePaint.tones(base);

            assertEquals(VehiclePaint.STOPS.length, tones.length);
            assertTrue(brightness(tones[0]) > brightness(base), hex);
            assertTrue(brightness(tones[3]) < brightness(base), hex);
            assertTrue(brightness(tones[6]) < brightness(tones[3]), hex);
        }
    }

    private static void assertClose(Color expected, Color actual) {
        double tolerance = 6 / 255.0;
        assertEquals(expected.getRed(), actual.getRed(), tolerance);
        assertEquals(expected.getGreen(), actual.getGreen(), tolerance);
        assertEquals(expected.getBlue(), actual.getBlue(), tolerance);
    }
}
