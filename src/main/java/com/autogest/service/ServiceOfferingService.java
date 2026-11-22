package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.ServiceOffering;
import com.autogest.model.Catalogs.ServiceOfferingStatus;
import com.autogest.repository.ServiceOfferingRepository;

import java.math.BigDecimal;
import java.util.List;

public class ServiceOfferingService {

    private final ServiceOfferingRepository repository;

    public ServiceOfferingService(ServiceOfferingRepository repository) {
        this.repository = repository;
    }

    public ServiceOffering registerService(ServiceOffering service) {
        validateAndNormalize(service);
        if (repository.existsByCode(service.getCode())) {
            throw new ValidationException("Ya existe un servicio con ese código");
        }
        service.setStatus(service.getStatus() == null ? ServiceOfferingStatus.ACTIVE : service.getStatus());
        return repository.create(service);
    }

    public ServiceOffering updateService(ServiceOffering service) {
        validateAndNormalize(service);
        if (repository.existsByCodeExcluding(service.getCode(), service.getId())) {
            throw new ValidationException("Ya existe un servicio con ese código");
        }
        ServiceOffering existing = repository.findById(service.getId());
        if (existing == null) {
            throw new NotFoundException("No se encontró el servicio solicitado");
        }
        return repository.update(service) ? service : existing;
    }

    public List<ServiceOffering> searchServices(String text, ServiceOfferingStatus status, String category) {
        return repository.search(text, status, category);
    }

    public List<ServiceOffering> findAll() {
        return repository.findAll();
    }

    public ServiceOffering getDetail(int id) {
        ServiceOffering service = repository.findById(id);
        if (service == null) {
            throw new NotFoundException("No se encontró el servicio solicitado");
        }
        return service;
    }

    public void deactivateService(int id) {
        ServiceOffering service = repository.findById(id);
        if (service == null) {
            throw new NotFoundException("No se encontró el servicio solicitado");
        }
        service.setStatus(ServiceOfferingStatus.INACTIVE);
        repository.update(service);
    }

    public void deleteService(int id) {
        deactivateService(id);
    }

    public List<String> findCategories() {
        return repository.findCategories();
    }

    private void validateAndNormalize(ServiceOffering service) {
        if (service == null) {
            throw new ValidationException("El servicio es obligatorio");
        }
        if (service.getCode() == null || service.getCode().isBlank()) {
            throw new ValidationException("El código es obligatorio");
        }
        if (service.getName() == null || service.getName().isBlank()) {
            throw new ValidationException("El nombre es obligatorio");
        }
        if (service.getPrice() == null) {
            throw new ValidationException("El precio es obligatorio");
        }
        if (service.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El precio no puede ser negativo");
        }
        if (service.getDurationHours() < 0) {
            throw new ValidationException("La duración no puede ser negativa");
        }
        service.setCode(service.getCode().trim());
        service.setName(service.getName().trim());
        if (service.getCategory() != null) {
            service.setCategory(service.getCategory().trim());
        }
        if (service.getDescription() != null) {
            service.setDescription(service.getDescription().trim());
        }
        service.setPrice(service.getPrice().setScale(2, java.math.RoundingMode.HALF_UP));
    }
}
