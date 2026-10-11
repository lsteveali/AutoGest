package com.autogest.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link InventoryMovement#isEntry()}.
 */
class InventoryMovementTest {

    private InventoryMovement movementOf(MovementType type) {
        InventoryMovement movement = new InventoryMovement();
        movement.setType(type);
        return movement;
    }

    @Test
    void onlyEntryIsAnEntry() {
        assertTrue(movementOf(MovementType.ENTRY).isEntry());
        assertFalse(movementOf(MovementType.EXIT).isEntry());
        assertFalse(movementOf(MovementType.ADJUSTMENT).isEntry());
    }
}
