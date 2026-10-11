package com.autogest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link Supplier#getContact()}.
 */
class SupplierTest {

    @Test
    void contactJoinsPhoneAndEmail() {
        Supplier supplier = new Supplier();
        supplier.setPhone("601 555 0101");
        supplier.setEmail("ventas@autopartes.com");

        assertEquals("601 555 0101 - ventas@autopartes.com", supplier.getContact());
    }

    @Test
    void contactShowsOnlyWhatExists() {
        Supplier onlyPhone = new Supplier();
        onlyPhone.setPhone("601 555 0101");
        assertEquals("601 555 0101", onlyPhone.getContact());

        Supplier onlyEmail = new Supplier();
        onlyEmail.setEmail("ventas@autopartes.com");
        assertEquals("ventas@autopartes.com", onlyEmail.getContact());
    }

    @Test
    void contactIsEmptyWithoutData() {
        Supplier supplier = new Supplier();
        supplier.setPhone("  ");

        assertEquals("", supplier.getContact());
    }
}
