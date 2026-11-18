package com.autogest.service;

import com.autogest.model.InventoryMovement;
import com.autogest.repository.InventoryMovementRepository;

import com.autogest.model.MovementType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Comparator;
import java.util.List;

/**
 * Repositorio falso de movimientos de inventario, en memoria. Los devuelve del
 * más reciente al más antiguo, como {@code InventoryMovementDao}.
 */
class FakeInventoryMovementRepository extends InventoryMovementRepository {

    private final List<InventoryMovement> movements = new ArrayList<>();
    private int sequence;

    FakeInventoryMovementRepository() {
        super(null, null, null);
    }

    @Override
    public InventoryMovement create(InventoryMovement movement) {
        movement.setId(++sequence);
        movements.add(movement);
        return movement;
    }

    @Override
    public InventoryMovement findById(int id) {
        return movements.stream().filter(m -> m.getId() == id).findFirst().orElse(null);
    }

    @Override
    public List<InventoryMovement> findByPart(int partId) {
        return newestFirst(movements.stream().filter(m -> m.getPart().getId() == partId).toList());
    }

    @Override
    public List<InventoryMovement> findRecent(int limit) {
        return newestFirst(movements).stream().limit(limit).toList();
    }

    @Override
    public Map<MovementType, Integer> sumQuantityByType(LocalDate from, LocalDate to) {
        Map<MovementType, Integer> result = new EnumMap<>(MovementType.class);
        for (MovementType type : MovementType.values()) {
            result.put(type, 0);
        }
        movements.stream()
                .filter(m -> !m.getTimestamp().toLocalDate().isBefore(from) && !m.getTimestamp().toLocalDate().isAfter(to))
                .forEach(m -> result.merge(m.getType(), m.getQuantity(), Integer::sum));
        return result;
    }

    /** Todos los movimientos guardados, en orden de registro (para las aserciones). */
    List<InventoryMovement> all() {
        return movements;
    }

    private static List<InventoryMovement> newestFirst(List<InventoryMovement> list) {
        List<InventoryMovement> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparing(InventoryMovement::getId).reversed());
        return copy;
    }
}
