package com.autogest.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** «Abrir automáticamente el último módulo utilizado»: qué guardado se puede abrir. */
class MainControllerLastModuleTest {

    @Test
    void sidebarModulesAreOpened() {
        assertEquals(View.ORDERS, MainController.lastModule("ORDERS"));
        assertEquals(View.SETTINGS, MainController.lastModule("SETTINGS"));
    }

    @Test
    void detailsTheAssistantAndUnknownNamesAreIgnored() {
        assertNull(MainController.lastModule("ORDER_DETAIL"));
        assertNull(MainController.lastModule("ASSISTANT"));
        assertNull(MainController.lastModule("MODULO_QUE_YA_NO_EXISTE"));
        assertNull(MainController.lastModule(null));
    }
}
