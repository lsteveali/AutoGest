package com.autogest.repository;

import com.autogest.dao.SupplierDao;
import com.autogest.model.Supplier;

import java.util.List;

/**
 * Organiza el acceso a los proveedores para los Services. Delega el SQL en
 * {@link SupplierDao}. Es una clase concreta no final para que las pruebas
 * puedan sustituirla con una subclase en memoria.
 *
 * <p>Principles: Indirection, Pure Fabrication, Low Coupling.</p>
 */
public class SupplierRepository {

    private final SupplierDao dao;

    /**
     * @param dao DAO de proveedores
     */
    public SupplierRepository(SupplierDao dao) {
        this.dao = dao;
    }

    /** Guarda un proveedor nuevo y lo devuelve con su id. */
    public Supplier create(Supplier supplier) {
        return dao.create(supplier);
    }

    /** Busca un proveedor por id; {@code null} si no existe. */
    public Supplier findById(int id) {
        return dao.findById(id);
    }

    /** Devuelve todos los proveedores ordenados por nombre. */
    public List<Supplier> findAll() {
        return dao.findAll();
    }

    /** Actualiza un proveedor existente. */
    public boolean update(Supplier supplier) {
        return dao.update(supplier);
    }

    /** Elimina un proveedor sin repuestos asociados. */
    public boolean delete(int id) {
        return dao.delete(id);
    }

    /** Indica si el nombre ya está registrado por otro proveedor. */
    public boolean existsByName(String name, int excludeId) {
        return dao.existsByName(name, excludeId);
    }
}
