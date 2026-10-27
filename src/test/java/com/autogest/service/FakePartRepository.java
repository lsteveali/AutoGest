package com.autogest.service;

import com.autogest.model.Part;
import com.autogest.model.StockStatus;
import com.autogest.repository.PartRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio falso del inventario de repuestos para las pruebas de los
 * Services, sin base de datos. Extiende {@link PartRepository} (clase concreta
 * no final) y guarda los repuestos en memoria. Se pasa un DAO {@code null}
 * porque la subclase nunca delega en el DAO real. Reproduce el comportamiento
 * del {@code PartDao}: búsqueda por nombre/código, orden por nombre, borrado
 * físico y conteos por estado de existencias.
 */
class FakePartRepository extends PartRepository {

    private final Map<Integer, Part> parts = new LinkedHashMap<>();
    private int sequence;

    FakePartRepository() {
        super(null);
    }

    @Override
    public Part create(Part part) {
        part.setId(++sequence);
        parts.put(part.getId(), part);
        return part;
    }

    @Override
    public Part findById(int id) {
        return parts.get(id);
    }

    @Override
    public List<Part> findAll() {
        List<Part> list = new ArrayList<>(parts.values());
        list.sort(Comparator.comparing(Part::getName));
        return list;
    }

    @Override
    public boolean update(Part part) {
        if (!parts.containsKey(part.getId())) {
            return false;
        }
        parts.put(part.getId(), part);
        return true;
    }

    @Override
    public boolean delete(int id) {
        return parts.remove(id) != null;
    }

    @Override
    public List<Part> search(String text, StockStatus status, String category) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        String categoryNeedle = category == null ? "" : category.trim();
        List<Part> result = new ArrayList<>();
        for (Part part : parts.values()) {
            boolean matchesText = needle.isEmpty()
                    || contains(part.getName(), needle)
                    || contains(part.getCode(), needle);
            boolean matchesStatus = status == null || part.getStatus() == status;
            boolean matchesCategory = categoryNeedle.isEmpty() || categoryNeedle.equals(part.getCategory());
            if (matchesText && matchesStatus && matchesCategory) {
                result.add(part);
            }
        }
        result.sort(Comparator.comparing(Part::getName));
        return result;
    }

    @Override
    public boolean existsByCode(String code) {
        return parts.values().stream().anyMatch(part -> part.getCode().equals(code));
    }

    @Override
    public boolean existsByCodeExcluding(String code, int id) {
        return parts.values().stream()
                .anyMatch(part -> part.getId() != id && part.getCode().equals(code));
    }

    @Override
    public int countLowStock() {
        return (int) parts.values().stream().filter(part -> part.getStatus() == StockStatus.LOW_STOCK).count();
    }

    @Override
    public int countOutOfStock() {
        return (int) parts.values().stream().filter(part -> part.getStatus() == StockStatus.OUT_OF_STOCK).count();
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
