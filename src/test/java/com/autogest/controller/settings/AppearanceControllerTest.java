package com.autogest.controller.settings;

import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.model.Catalogs.Role;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppearanceControllerTest {

    @Test
    void pickerColorBecomesHexText() {
        assertEquals("#B3261E", AppearanceController.hex(Color.web("#b3261e")));
        assertEquals("#000000", AppearanceController.hex(Color.BLACK));
        assertEquals("#FFFFFF", AppearanceController.hex(Color.WHITE));
    }

    @Test
    void onlyTheAdministratorCanCustomizeTheLogin() {
        assertTrue(Permissions.canAccess(Role.ADMINISTRATOR, View.SETTINGS_APPEARANCE));
        for (Role role : new Role[]{Role.RECEPTIONIST, Role.MECHANIC, Role.SUPERVISOR}) {
            assertFalse(Permissions.canAccess(role, View.SETTINGS_APPEARANCE), role.name());
        }
    }

    @Test
    void settingsShowsThePersonalizationCard() {
        assertTrue(SettingsController.SECTIONS.stream()
                .anyMatch(section -> section.viewName().equals(View.SETTINGS_APPEARANCE.name())));
    }
}
