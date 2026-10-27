package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Part;
import com.autogest.model.StockStatus;
import com.autogest.repository.PartRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PartService {

    private final PartRepository repository;

    public PartService(PartRepository repository) {
        this.repository = repository;
    }

    public Part registerPart(Part part) {
        validateAndNormalize(part);
        if (repository.existsByCode(part.getCode())) {
            throw new ValidationException("Ya existe un repuesto con ese código");
        }
        return repository.create(part);
    }

    public Part updatePart(Part part) {
        validateAndNormalize(part);
        if (repository.existsByCodeExcluding(part.getCode(), part.getId())) {
            throw new ValidationException("Ya existe un repuesto con ese código");
        }
        Part existing = repository.findById(part.getId());
        if (existing == null) {
            throw new NotFoundException("No se encontró el repuesto solicitado");
        }
        return repository.update(part) ? part : existing;
    }

    public List<Part> searchParts(String text, StockStatus status, String category) {
        return repository.search(text, status, category);
    }

    public List<Part> findAll() {
        return repository.findAll();
    }

    public Part getDetail(int id) {
        Part part = repository.findById(id);
        if (part == null) {
            throw new NotFoundException("No se encontró el repuesto solicitado");
        }
        return part;
    }

    public void increaseStock(int partId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
        Part part = getDetail(partId);
        part.increaseStock(quantity);
        repository.update(part);
    }

    public void decreaseStock(int partId, int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
        Part part = getDetail(partId);
        part.decreaseStock(quantity);
        repository.update(part);
    }

    public void adjustStock(int partId, int newStock) {
        if (newStock < 0) {
            throw new ValidationException("El stock no puede ser negativo");
        }
        Part part = getDetail(partId);
        part.setCurrentStock(newStock);
        repository.update(part);
    }

    public int countLowStock() {
        return repository.countLowStock();
    }

    public int countOutOfStock() {
        return repository.countOutOfStock();
    }

    public int countParts() {
        return repository.findAll().size();
    }

    private void validateAndNormalize(Part part) {
        if (part == null) {
            throw new ValidationException("El repuesto es obligatorio");
        }
        if (part.getCode() == null || part.getCode().isBlank()) {
            throw new ValidationException("El código es obligatorio");
        }
        if (part.getName() == null || part.getName().isBlank()) {
            throw new ValidationException("El nombre es obligatorio");
        }
        if (part.getUnitPrice() == null) {
            throw new ValidationException("El precio es obligatorio");
        }
        if (part.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El precio no puede ser negativo");
        }
        if (part.getCurrentStock() < 0) {
            throw new ValidationException("El stock no puede ser negativo");
        }
        if (part.getMinimumStock() < 0) {
            throw new ValidationException("El stock mínimo no puede ser negativo");
        }
        part.setCode(part.getCode().trim());
        part.setName(part.getName().trim());
        if (part.getCategory() != null) {
            part.setCategory(part.getCategory().trim());
        }
        part.setUnitPrice(part.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
    }
}
