package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link SupplierService} con repositorio falso.
 */
class SupplierServiceTest {

    private SupplierService service;

    @BeforeEach
    void setUp() {
        service = new SupplierService(new FakeSupplierRepository());
    }

    private Supplier supplier(String name, String phone, String email) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        return supplier;
    }

    @Test
    void registerSupplierNormalizesAndAssignsId() {
        Supplier saved = service.registerSupplier(supplier("  LubriAuto ", " ", " ventas@lubri.com "));
        assertTrue(saved.getId() > 0);
        assertEquals("LubriAuto", saved.getName());
        assertNull(saved.getPhone());
        assertEquals("ventas@lubri.com", saved.getEmail());
    }

    @Test
    void registerSupplierRequiresName() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> service.registerSupplier(supplier(" ", null, null)));
        assertEquals("El nombre del proveedor es obligatorio", e.getMessage());
    }

    @Test
    void registerSupplierRejectsInvalidEmail() {
        assertThrows(ValidationException.class,
                () -> service.registerSupplier(supplier("Frenos Nacionales", null, "correo-malo")));
    }

    @Test
    void registerSupplierRejectsDuplicatedNameIgnoringCase() {
        service.registerSupplier(supplier("AutoPartes Colombia", null, null));
        assertThrows(ValidationException.class,
                () -> service.registerSupplier(supplier("autopartes colombia", null, null)));
    }

    @Test
    void updateSupplierKeepsOwnName() {
        Supplier saved = service.registerSupplier(supplier("LubriAuto", null, null));
        saved.setPhone("300 111 2222");
        assertEquals("300 111 2222", service.updateSupplier(saved).getPhone());
    }

    @Test
    void updateUnknownSupplierThrowsNotFound() {
        Supplier ghost = supplier("Fantasma", null, null);
        ghost.setId(99);
        assertThrows(NotFoundException.class, () -> service.updateSupplier(ghost));
    }

    @Test
    void findAllIsSortedByName() {
        service.registerSupplier(supplier("Zeta Repuestos", null, null));
        service.registerSupplier(supplier("Alfa Partes", null, null));
        assertEquals("Alfa Partes", service.findAll().get(0).getName());
    }
}
