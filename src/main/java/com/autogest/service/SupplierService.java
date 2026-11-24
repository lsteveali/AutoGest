package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Supplier;
import com.autogest.repository.SupplierRepository;
import com.autogest.util.ContactValidator;

import java.util.List;

/**
 * Reglas de negocio de los proveedores de repuestos: valida y normaliza los
 * datos antes de guardarlos y ofrece las consultas que usan Inventario y el
 * Asistente.
 *
 * <p>Principles: GRASP Controller (caso de uso), SRP, DIP, Low Coupling.</p>
 */
public class SupplierService {


    private final SupplierRepository repository;

    /**
     * @param repository repositorio de proveedores
     */
    public SupplierService(SupplierRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra un proveedor nuevo.
     *
     * @param supplier datos del proveedor
     * @return proveedor guardado con su id
     * @throws ValidationException si los datos no son válidos o el nombre ya existe
     */
    public Supplier registerSupplier(Supplier supplier) {
        validateAndNormalize(supplier);
        if (repository.existsByName(supplier.getName(), 0)) {
            throw new ValidationException("Ya existe un proveedor con ese nombre");
        }
        return repository.create(supplier);
    }

    /**
     * Actualiza un proveedor existente.
     *
     * @param supplier datos con el id del proveedor
     * @return proveedor actualizado
     * @throws NotFoundException   si el proveedor no existe
     * @throws ValidationException si los datos no son válidos
     */
    public Supplier updateSupplier(Supplier supplier) {
        validateAndNormalize(supplier);
        getDetail(supplier.getId());
        if (repository.existsByName(supplier.getName(), supplier.getId())) {
            throw new ValidationException("Ya existe un proveedor con ese nombre");
        }
        repository.update(supplier);
        return supplier;
    }

    /** Devuelve todos los proveedores ordenados por nombre (para desplegables). */
    public List<Supplier> findAll() {
        return repository.findAll();
    }

    /**
     * Devuelve un proveedor por id.
     *
     * @throws NotFoundException si no existe
     */
    public Supplier getDetail(int id) {
        Supplier supplier = repository.findById(id);
        if (supplier == null) {
            throw new NotFoundException("No se encontró el proveedor solicitado");
        }
        return supplier;
    }

    private void validateAndNormalize(Supplier supplier) {
        if (supplier == null) {
            throw new ValidationException("El proveedor es obligatorio");
        }
        if (supplier.getName() == null || supplier.getName().isBlank()) {
            throw new ValidationException("El nombre del proveedor es obligatorio");
        }
        supplier.setName(supplier.getName().trim());
        // Teléfono y correo son opcionales; si vienen, con las reglas de ContactValidator.
        supplier.setPhone(ContactValidator.optionalPhone(supplier.getPhone()));
        supplier.setEmail(ContactValidator.optionalEmail(supplier.getEmail()));
    }
}
