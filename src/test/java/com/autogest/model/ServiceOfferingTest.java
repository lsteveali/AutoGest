package com.autogest.model;

import com.autogest.model.Catalogs.ServiceOfferingStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas propias de {@link ServiceOffering}.
 */
class ServiceOfferingTest {

    @Test
    void newServiceIsActiveWithoutParts() {
        ServiceOffering service = new ServiceOffering();

        assertTrue(service.isActive());
        assertTrue(service.getParts().isEmpty());
    }

    @Test
    void inactiveServiceIsNotActive() {
        ServiceOffering service = new ServiceOffering();
        service.setStatus(ServiceOfferingStatus.INACTIVE);

        assertFalse(service.isActive());
    }

    @Test
    void addPartAssociatesThePart() {
        ServiceOffering service = new ServiceOffering();
        Part filter = new Part();
        filter.setName("Filtro de aceite");

        service.addPart(filter);

        assertEquals(1, service.getParts().size());
        assertEquals("Filtro de aceite", service.getParts().get(0).getName());
    }

    @Test
    void addPartRejectsNull() {
        ServiceOffering service = new ServiceOffering();

        assertThrows(ValidationException.class, () -> service.addPart(null));
    }

    @Test
    void partsListCannotBeModifiedFromOutside() {
        ServiceOffering service = new ServiceOffering();
        List<Part> view = service.getParts();

        assertThrows(UnsupportedOperationException.class, () -> view.add(new Part()));
    }

    @Test
    void priceUsesScaleTwo() {
        ServiceOffering service = new ServiceOffering();
        service.setPrice(new BigDecimal("120000"));

        assertEquals(new BigDecimal("120000.00"), service.getPrice());
    }
}
