package com.autogest.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Usa un nodo de preferencias propio y lo borra al terminar. */
class LocalMemoryTest {

    private Preferences node;
    private LocalMemory memory;

    @BeforeEach
    void setUp() {
        node = Preferences.userRoot().node("com/autogest/test/" + UUID.randomUUID());
        memory = new LocalMemory(node);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        node.removeNode();
    }

    @Test
    void lastModuleIsRememberedPerUser() {
        memory.rememberModule(1, "ORDERS");
        memory.rememberModule(2, "BILLING");

        assertEquals("ORDERS", new LocalMemory(node).lastModule(1));
        assertEquals("BILLING", memory.lastModule(2));
        assertNull(memory.lastModule(3));
    }

    @Test
    void filtersAreRememberedPerScreenAndLabel() {
        memory.rememberFilter("ORDERS", "Estado", "En reparación");
        memory.rememberFilter("BILLING", "Estado", "Vencida");

        assertEquals("En reparación", memory.filter("ORDERS", "Estado"));
        assertEquals("Vencida", memory.filter("BILLING", "Estado"));
        assertNull(memory.filter("ORDERS", "Fecha"));
    }

    @Test
    void emptyValueForgetsTheFilter() {
        memory.rememberFilter("ORDERS", "search", "ABC");
        memory.rememberFilter("ORDERS", "search", "");

        assertNull(memory.filter("ORDERS", "search"));
    }

    @Test
    void veryLongKeysAreCutInsteadOfFailing() {
        String label = "x".repeat(200);
        memory.rememberFilter("ORDERS", label, "valor");

        assertEquals("valor", memory.filter("ORDERS", label));
    }
}
