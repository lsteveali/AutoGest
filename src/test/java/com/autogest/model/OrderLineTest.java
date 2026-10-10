package com.autogest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del contrato de {@link OrderLine}, que deben cumplir por igual
 * {@link ServiceLine} y {@link PartLine} (sustitución de Liskov).
 */
class OrderLineTest {

    private ServiceLine serviceLine(int quantity, String price) {
        ServiceLine line = new ServiceLine();
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(price));
        return line;
    }

    private PartLine partLine(int quantity, String price) {
        PartLine line = new PartLine();
        line.setQuantity(quantity);
        line.setUnitPrice(new BigDecimal(price));
        return line;
    }

    @Test
    void bothSubclassesCalculateQuantityTimesUnitPrice() {
        List<OrderLine> lines = List.of(serviceLine(3, "35000"), partLine(3, "35000"));

        for (OrderLine line : lines) {
            assertEquals(new BigDecimal("105000.00"), line.calculateTotal());
        }
    }

    @Test
    void zeroQuantityGivesZeroTotal() {
        assertEquals(new BigDecimal("0.00"), serviceLine(0, "35000").calculateTotal());
        assertEquals(new BigDecimal("0.00"), partLine(0, "35000").calculateTotal());
    }

    @Test
    void newLineHasZeroTotalAndNeverNegative() {
        for (OrderLine line : List.of(new ServiceLine(), new PartLine())) {
            assertEquals(new BigDecimal("0.00"), line.calculateTotal());
            assertTrue(line.calculateTotal().signum() >= 0);
        }
    }

    @Test
    void unitPriceIsRoundedHalfUpToScaleTwoBeforeMultiplying() {
        assertEquals(new BigDecimal("30.03"), serviceLine(3, "10.005").calculateTotal());
    }

    @Test
    void negativeQuantityIsRejected() {
        assertThrows(ValidationException.class, () -> new ServiceLine().setQuantity(-1));
        assertThrows(ValidationException.class, () -> new PartLine().setQuantity(-1));
    }

    @Test
    void nullOrNegativeUnitPriceIsRejected() {
        assertThrows(ValidationException.class, () -> new ServiceLine().setUnitPrice(null));
        assertThrows(ValidationException.class, () -> new PartLine().setUnitPrice(new BigDecimal("-1")));
    }

    @Test
    void descriptionIsTheNameOfTheServiceOrPart() {
        ServiceOffering offering = new ServiceOffering();
        offering.setName("Cambio de aceite");
        ServiceLine serviceLine = new ServiceLine();
        serviceLine.setServiceOffering(offering);

        Part part = new Part();
        part.setName("Filtro de aceite");
        PartLine partLine = new PartLine();
        partLine.setPart(part);

        assertEquals("Cambio de aceite", serviceLine.getDescription());
        assertEquals("Filtro de aceite", partLine.getDescription());
    }

    @Test
    void descriptionIsEmptyWithoutReference() {
        assertEquals("", new ServiceLine().getDescription());
        assertEquals("", new PartLine().getDescription());
    }
}
