package com.autogest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas propias de {@link Customer}.
 */
class CustomerTest {

    @Test
    void newCustomerHasIdZeroAndIsActive() {
        Customer customer = new Customer();

        assertEquals(0, customer.getId());
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        assertTrue(customer.isActive());
    }

    @Test
    void getFullNameJoinsFirstAndLastName() {
        Customer customer = new Customer();
        customer.setFirstName("Juan");
        customer.setLastName("Pérez");

        assertEquals("Juan Pérez", customer.getFullName());
    }

    @Test
    void deactivateChangesStatusToInactive() {
        Customer customer = new Customer();

        customer.deactivate();

        assertEquals(CustomerStatus.INACTIVE, customer.getStatus());
        assertFalse(customer.isActive());
    }
}