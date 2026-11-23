package com.autogest.util;

import com.autogest.util.LoginAppearance.Vehicle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Usa un nodo de preferencias propio y lo borra al terminar. */
class LoginAppearanceStoreTest {

    private Preferences node;
    private LoginAppearanceStore store;

    @BeforeEach
    void setUp() {
        node = Preferences.userRoot().node("com/autogest/test/" + UUID.randomUUID());
        store = new LoginAppearanceStore(node);
    }

    @AfterEach
    void tearDown() throws BackingStoreException {
        node.removeNode();
    }

    @Test
    void withNothingSavedTheFactoryLookIsUsed() {
        assertEquals(LoginAppearance.DEFAULT, store.load());
    }

    @Test
    void savedAppearanceIsReadBackByANewStore() {
        store.save(new LoginAppearance(Vehicle.SPORTS, "#B3261E"));

        assertEquals(new LoginAppearance(Vehicle.SPORTS, "#B3261E"), new LoginAppearanceStore(node).load());
    }

    @Test
    void unknownVehicleOrBadColorFallBackPerField() {
        node.put(LoginAppearanceStore.VEHICLE_KEY, "MOTO");
        node.put(LoginAppearanceStore.COLOR_KEY, "#1F4E8C");

        assertEquals(new LoginAppearance(Vehicle.TRUCK, "#1F4E8C"), store.load());

        node.put(LoginAppearanceStore.VEHICLE_KEY, "SEDAN");
        node.put(LoginAppearanceStore.COLOR_KEY, "azulito");

        assertEquals(new LoginAppearance(Vehicle.SEDAN, LoginAppearance.DEFAULT_COLOR), store.load());
    }

    @Test
    void savingNullRestoresTheFactoryLook() {
        store.save(new LoginAppearance(Vehicle.SEDAN, "#2F6B3A"));
        store.save(null);

        assertEquals(LoginAppearance.DEFAULT, store.load());
    }
}
