package com.autogest.service;

import com.autogest.model.Supplier;
import com.autogest.repository.SupplierRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio falso de proveedores para las pruebas, sin base de datos.
 * Reproduce el comportamiento de {@code SupplierDao}: orden por nombre y
 * comparación de nombres sin distinguir mayúsculas.
 */
class FakeSupplierRepository extends SupplierRepository {

    private final Map<Integer, Supplier> suppliers = new LinkedHashMap<>();
    private int sequence;

    FakeSupplierRepository() {
        super(null);
    }

    @Override
    public Supplier create(Supplier supplier) {
        supplier.setId(++sequence);
        suppliers.put(supplier.getId(), supplier);
        return supplier;
    }

    @Override
    public Supplier findById(int id) {
        return suppliers.get(id);
    }

    @Override
    public List<Supplier> findAll() {
        List<Supplier> list = new ArrayList<>(suppliers.values());
        list.sort(Comparator.comparing(Supplier::getName));
        return list;
    }

    @Override
    public boolean update(Supplier supplier) {
        return suppliers.replace(supplier.getId(), supplier) != null;
    }

    @Override
    public boolean delete(int id) {
        return suppliers.remove(id) != null;
    }

    @Override
    public boolean existsByName(String name, int excludeId) {
        return suppliers.values().stream()
                .anyMatch(s -> s.getId() != excludeId && s.getName().equalsIgnoreCase(name));
    }
}
