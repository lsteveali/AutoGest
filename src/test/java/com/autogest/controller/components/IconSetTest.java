package com.autogest.controller.components;

import javafx.scene.shape.ArcTo;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.PathElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the SVG path data parser used by {@link IconSet}.
 */
class IconSetTest {

    @Test
    void parsesHomeIconWithMoveAndLines() {
        List<PathElement> elements = IconSet.parsePathData(IconSet.HOME);
        assertTrue(elements.size() >= 3);
        assertInstanceOf(MoveTo.class, elements.get(0));
        assertInstanceOf(LineTo.class, elements.get(1));
    }

    @Test
    void parsesBellWithCubicCurves() {
        List<PathElement> elements = IconSet.parsePathData(IconSet.BELL);
        assertTrue(elements.stream().anyMatch(CubicCurveTo.class::isInstance));
    }

    @Test
    void parsesCarWithArcs() {
        List<PathElement> elements = IconSet.parsePathData(IconSet.CAR);
        assertTrue(elements.stream().anyMatch(ArcTo.class::isInstance));
    }

    @Test
    void allDefinedIconsParseWithoutError() {
        String[] icons = {
                IconSet.HOME, IconSet.USERS, IconSet.CAR, IconSet.FILE_TEXT, IconSet.WRENCH,
                IconSet.BOX, IconSet.CREDIT_CARD, IconSet.BELL, IconSet.BAR_CHART, IconSet.SETTINGS,
                IconSet.SPARKLES, IconSet.SEARCH, IconSet.PLUS, IconSet.CHEVRON_DOWN, IconSet.CHEVRON_RIGHT,
                IconSet.EDIT, IconSet.TRASH, IconSet.LOGOUT, IconSet.X, IconSet.ARROW_LEFT, IconSet.CHECK,
                IconSet.CALENDAR, IconSet.MAIL, IconSet.LOCK
        };
        for (String icon : icons) {
            assertDoesNotThrow(() -> IconSet.parsePathData(icon), "no parseable: " + icon);
        }
    }
}