package com.autogest.repository;

import com.autogest.dao.InventoryMovementDao;
import com.autogest.model.InventoryMovement;
import com.autogest.model.Part;
import com.autogest.model.User;

import com.autogest.model.Catalogs.MovementType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Organiza el acceso a los movimientos de inventario y completa sus
 * relaciones: el DAO devuelve el repuesto y el usuario solo con su id y aquí
 * se piden a sus repositorios.
 *
 * <p>Principles: Indirection, Pure Fabrication, Low Coupling.</p>
 */
public class InventoryMovementRepository {

    private final InventoryMovementDao dao;
    private final PartRepository partRepository;
    private final UserRepository userRepository;

    /**
     * @param dao            DAO de movimientos
     * @param partRepository repositorio de repuestos (completa {@code part})
     * @param userRepository repositorio de usuarios (completa {@code user})
     */
    public InventoryMovementRepository(InventoryMovementDao dao, PartRepository partRepository,
                                       UserRepository userRepository) {
        this.dao = dao;
        this.partRepository = partRepository;
        this.userRepository = userRepository;
    }

    /** Guarda un movimiento y lo devuelve con su id. */
    public InventoryMovement create(InventoryMovement movement) {
        return dao.create(movement);
    }

    /** Busca un movimiento por id (con sus relaciones); {@code null} si no existe. */
    public InventoryMovement findById(int id) {
        InventoryMovement movement = dao.findById(id);
        if (movement != null) {
            resolve(movement);
        }
        return movement;
    }

    /** Movimientos de un repuesto, del más reciente al más antiguo. */
    public List<InventoryMovement> findByPart(int partId) {
        return resolveAll(dao.findByPart(partId));
    }

    /** Últimos {@code limit} movimientos del taller. */
    public List<InventoryMovement> findRecent(int limit) {
        return resolveAll(dao.findRecent(limit));
    }

    /** Suma de cantidades por tipo entre dos fechas (apoyo para Reportes). */
    public Map<MovementType, Integer> sumQuantityByType(LocalDate from, LocalDate to) {
        return dao.sumQuantityByType(from, to);
    }

    private List<InventoryMovement> resolveAll(List<InventoryMovement> movements) {
        movements.forEach(this::resolve);
        return movements;
    }

    private void resolve(InventoryMovement movement) {
        if (movement.getPart() != null) {
            Part part = partRepository.findById(movement.getPart().getId());
            if (part != null) {
                movement.setPart(part);
            }
        }
        if (movement.getUser() != null) {
            User user = userRepository.findById(movement.getUser().getId());
            if (user != null) {
                movement.setUser(user);
            }
        }
    }
}
