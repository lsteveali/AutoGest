package com.autogest.repository;

import com.autogest.dao.PartDao;
import com.autogest.model.Part;
import com.autogest.model.StockStatus;
import com.autogest.model.Supplier;

import java.util.List;

/**
 * Organiza el acceso a los repuestos del inventario. Si recibe el repositorio
 * de proveedores, completa el {@link Supplier} de cada repuesto (el DAO lo
 * devuelve solo con su id).
 *
 * <p>Principles: Indirection, Pure Fabrication, Low Coupling.</p>
 */
public class PartRepository {

    private final PartDao dao;
    private final SupplierRepository supplierRepository;

    /**
     * Crea el repositorio sin completar proveedores (compatibilidad con las
     * pruebas y con el cableado del Sprint 3 inicial).
     *
     * @param dao DAO de repuestos
     */
    public PartRepository(PartDao dao) {
        this(dao, null);
    }

    /**
     * @param dao                DAO de repuestos
     * @param supplierRepository repositorio de proveedores (puede ser {@code null})
     */
    public PartRepository(PartDao dao, SupplierRepository supplierRepository) {
        this.dao = dao;
        this.supplierRepository = supplierRepository;
    }

    public Part create(Part entity) {
        return dao.create(entity);
    }

    public Part findById(int id) {
        Part part = dao.findById(id);
        if (part != null) {
            resolveSupplier(part);
        }
        return part;
    }

    public List<Part> findAll() {
        return resolveAll(dao.findAll());
    }

    public boolean update(Part entity) {
        return dao.update(entity);
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    public List<Part> search(String text, StockStatus status, String category) {
        return resolveAll(dao.search(text, status, category));
    }

    public boolean existsByCode(String code) {
        return dao.existsByCode(code);
    }

    public boolean existsByCodeExcluding(String code, int id) {
        return dao.existsByCodeExcluding(code, id);
    }

    public int countLowStock() {
        return dao.countLowStock();
    }

    public int countOutOfStock() {
        return dao.countOutOfStock();
    }

    /** Categorías distintas de los repuestos, en orden alfabético. */
    public List<String> findCategories() {
        return dao.findCategories();
    }

    private List<Part> resolveAll(List<Part> parts) {
        parts.forEach(this::resolveSupplier);
        return parts;
    }

    private void resolveSupplier(Part part) {
        if (supplierRepository == null || part.getSupplier() == null) {
            return;
        }
        Supplier supplier = supplierRepository.findById(part.getSupplier().getId());
        if (supplier != null) {
            part.setSupplier(supplier);
        }
    }
}
