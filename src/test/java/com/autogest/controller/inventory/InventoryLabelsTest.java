package com.autogest.controller.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.MovementType;
import com.autogest.model.StockStatus;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de los textos y colores del módulo Inventario (sin JavaFX).
 */
class InventoryLabelsTest {

    @Test
    void stockTextAndTypeMatchTheMockup() {
        assertEquals("Disponible", InventoryLabels.stockText(StockStatus.AVAILABLE));
        assertEquals("Stock bajo", InventoryLabels.stockText(StockStatus.LOW_STOCK));
        assertEquals("Agotado", InventoryLabels.stockText(StockStatus.OUT_OF_STOCK));
        assertEquals(StatusBadge.BadgeType.WARNING, InventoryLabels.stockType(StockStatus.LOW_STOCK));
        assertEquals(StatusBadge.BadgeType.DANGER, InventoryLabels.stockType(StockStatus.OUT_OF_STOCK));
        assertEquals("-", InventoryLabels.stockText(null));
    }

    @Test
    void stockFromTextRoundTrips() {
        for (StockStatus status : StockStatus.values()) {
            assertEquals(status, InventoryLabels.stockFromText(InventoryLabels.stockText(status)));
        }
        assertNull(InventoryLabels.stockFromText(InventoryLabels.ALL));
        assertNull(InventoryLabels.stockFromText(null));
    }

    @Test
    void signedQuantityShowsDirection() {
        assertEquals("+5", InventoryLabels.signedQuantity(MovementType.ENTRY, 5));
        assertEquals("-3", InventoryLabels.signedQuantity(MovementType.EXIT, 3));
        assertEquals("2", InventoryLabels.signedQuantity(MovementType.ADJUSTMENT, 2));
        assertEquals("Ajuste", InventoryLabels.movementText(MovementType.ADJUSTMENT));
    }
}
