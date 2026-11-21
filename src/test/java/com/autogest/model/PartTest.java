package com.autogest.model;

import com.autogest.model.Catalogs.StockStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas de stock y precio de {@link Part}.
 */
class PartTest {

    private Part partWith(int stock, int minimum) {
        Part part = new Part();
        part.setMinimumStock(minimum);
        part.setCurrentStock(stock);
        return part;
    }

    @Test
    void newPartHasNoStockAndIsOutOfStock() {
        Part part = new Part();

        assertEquals(0, part.getId());
        assertEquals(0, part.getCurrentStock());
        assertTrue(part.isOutOfStock());
        assertEquals(StockStatus.OUT_OF_STOCK, part.getStatus());
    }

    @Test
    void increaseStockAddsUnitsAndUpdatesStatus() {
        Part part = partWith(0, 10);

        part.increaseStock(8);
        assertEquals(8, part.getCurrentStock());
        assertEquals(StockStatus.LOW_STOCK, part.getStatus());

        part.increaseStock(20);
        assertEquals(28, part.getCurrentStock());
        assertEquals(StockStatus.AVAILABLE, part.getStatus());
    }

    @Test
    void decreaseStockSubtractsUnitsAndUpdatesStatus() {
        Part part = partWith(24, 10);

        part.decreaseStock(14);
        assertEquals(10, part.getCurrentStock());
        assertEquals(StockStatus.LOW_STOCK, part.getStatus());

        part.decreaseStock(10);
        assertEquals(0, part.getCurrentStock());
        assertEquals(StockStatus.OUT_OF_STOCK, part.getStatus());
    }

    @Test
    void stockStatusBoundaries() {
        assertEquals(StockStatus.OUT_OF_STOCK, partWith(0, 10).getStatus());
        assertEquals(StockStatus.LOW_STOCK, partWith(1, 10).getStatus());
        assertEquals(StockStatus.LOW_STOCK, partWith(10, 10).getStatus());
        assertEquals(StockStatus.AVAILABLE, partWith(11, 10).getStatus());
    }

    @Test
    void isBelowMinimumIncludesTheMinimumItself() {
        assertTrue(partWith(10, 10).isBelowMinimum());
        assertTrue(partWith(9, 10).isBelowMinimum());
        assertFalse(partWith(11, 10).isBelowMinimum());
    }

    @Test
    void decreaseStockRejectsInsufficientStockAndKeepsTheValue() {
        Part part = partWith(5, 2);

        ValidationException error = assertThrows(ValidationException.class, () -> part.decreaseStock(6));

        assertEquals("Stock insuficiente: solo hay 5 unidades", error.getMessage());
        assertEquals(5, part.getCurrentStock());
    }

    @Test
    void stockMovementsRejectZeroAndNegativeQuantities() {
        Part part = partWith(5, 2);

        assertThrows(ValidationException.class, () -> part.increaseStock(0));
        assertThrows(ValidationException.class, () -> part.increaseStock(-3));
        assertThrows(ValidationException.class, () -> part.decreaseStock(0));
        assertThrows(ValidationException.class, () -> part.decreaseStock(-1));
        assertEquals(5, part.getCurrentStock());
    }

    @Test
    void setCurrentStockRejectsNegativeValues() {
        Part part = partWith(5, 2);

        assertThrows(ValidationException.class, () -> part.setCurrentStock(-1));
        assertEquals(5, part.getCurrentStock());
    }

    @Test
    void changingTheMinimumRecalculatesTheStatus() {
        Part part = partWith(8, 5);
        assertEquals(StockStatus.AVAILABLE, part.getStatus());

        part.setMinimumStock(10);
        assertEquals(StockStatus.LOW_STOCK, part.getStatus());
    }

    @Test
    void unitPriceUsesScaleTwoWithHalfUpRounding() {
        Part part = new Part();

        part.setUnitPrice(new BigDecimal("35000"));
        assertEquals(new BigDecimal("35000.00"), part.getUnitPrice());

        part.setUnitPrice(new BigDecimal("10.005"));
        assertEquals(new BigDecimal("10.01"), part.getUnitPrice());

        part.setUnitPrice(null);
        assertNull(part.getUnitPrice());
    }
}
