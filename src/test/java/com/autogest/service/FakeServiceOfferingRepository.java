package com.autogest.service;

import com.autogest.model.ServiceOffering;
import com.autogest.model.Catalogs.ServiceOfferingStatus;
import com.autogest.repository.ServiceOfferingRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio falso del catálogo de servicios para las pruebas de los Services,
 * sin base de datos. Extiende {@link ServiceOfferingRepository} (clase concreta
 * no final) y guarda los servicios en memoria. Se pasa un DAO {@code null} porque
 * la subclase nunca delega en el DAO real. Reproduce el comportamiento del
 * {@code ServiceOfferingDao}: búsqueda por nombre/código/descripción, orden por
 * nombre, baja lógica y categorías distintas ordenadas.
 */
class FakeServiceOfferingRepository extends ServiceOfferingRepository {

    private final Map<Integer, ServiceOffering> services = new LinkedHashMap<>();
    private int sequence;

    FakeServiceOfferingRepository() {
        super(null);
    }

    @Override
    public ServiceOffering create(ServiceOffering service) {
        service.setId(++sequence);
        services.put(service.getId(), service);
        return service;
    }

    @Override
    public ServiceOffering findById(int id) {
        return services.get(id);
    }

    @Override
    public List<ServiceOffering> findAll() {
        List<ServiceOffering> list = new ArrayList<>(services.values());
        list.sort(Comparator.comparing(ServiceOffering::getName));
        return list;
    }

    @Override
    public boolean update(ServiceOffering service) {
        if (!services.containsKey(service.getId())) {
            return false;
        }
        services.put(service.getId(), service);
        return true;
    }

    @Override
    public boolean delete(int id) {
        ServiceOffering service = services.get(id);
        if (service == null) {
            return false;
        }
        service.setStatus(ServiceOfferingStatus.INACTIVE);
        return true;
    }

    @Override
    public List<ServiceOffering> search(String text, ServiceOfferingStatus status, String category) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        String categoryNeedle = category == null ? "" : category.trim();
        List<ServiceOffering> result = new ArrayList<>();
        for (ServiceOffering service : services.values()) {
            boolean matchesText = needle.isEmpty()
                    || contains(service.getName(), needle)
                    || contains(service.getCode(), needle)
                    || contains(service.getDescription(), needle);
            boolean matchesStatus = status == null || service.getStatus() == status;
            boolean matchesCategory = categoryNeedle.isEmpty() || categoryNeedle.equals(service.getCategory());
            if (matchesText && matchesStatus && matchesCategory) {
                result.add(service);
            }
        }
        result.sort(Comparator.comparing(ServiceOffering::getName));
        return result;
    }

    @Override
    public List<String> findCategories() {
        return services.values().stream()
                .map(ServiceOffering::getCategory)
                .filter(category -> category != null && !category.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    @Override
    public boolean existsByCode(String code) {
        return services.values().stream().anyMatch(service -> service.getCode().equals(code));
    }

    @Override
    public boolean existsByCodeExcluding(String code, int id) {
        return services.values().stream()
                .anyMatch(service -> service.getId() != id && service.getCode().equals(code));
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
