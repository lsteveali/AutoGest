package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.InventoryMovement;
import com.autogest.model.MovementType;
import com.autogest.model.Part;
import com.autogest.model.User;
import com.autogest.repository.InventoryMovementRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Casos de uso de los movimientos de inventario: entradas, salidas y ajustes
 * de stock. Cada operación cambia el stock del repuesto (a través de
 * {@link PartService}) y deja un {@link InventoryMovement} con el usuario que
 * la hizo, para poder consultar el historial.
 *
 * <p>El stock lo calcula el modelo ({@code Part.increaseStock} /
 * {@code decreaseStock}); este Service solo coordina. Un Service usa otro
 * Service (no su Repository).</p>
 *
 * <p>Principles: GRASP Controller, Creator (crea el movimiento), Low Coupling,
 * SRP, DIP.</p>
 */
public class InventoryService {

    /** Máximo de caracteres del motivo (columna {@code reason VARCHAR(255)}). */
    private static final int MAX_REASON = 255;

    private final InventoryMovementRepository repository;
    private final PartService partService;

    /**
     * @param repository  repositorio de movimientos
     * @param partService servicio de repuestos (dueño del stock)
     */
    public InventoryService(InventoryMovementRepository repository, PartService partService) {
        this.repository = repository;
        this.partService = partService;
    }

    /**
     * Registra un repuesto nuevo y, si trae stock inicial, deja la entrada
     * «Stock inicial» en el historial.
     *
     * @param part datos del repuesto
     * @param user usuario que lo registra
     * @return repuesto guardado
     */
    public Part registerPart(Part part, User user) {
        requireUser(user);
        Part saved = partService.registerPart(part);
        if (saved.getCurrentStock() > 0) {
            save(MovementType.ENTRY, saved, saved.getCurrentStock(), "Stock inicial", user);
        }
        return saved;
    }

    /**
     * Registra una entrada de stock (compra o devolución).
     *
     * @param partId   id del repuesto
     * @param quantity unidades que ingresan (mayor que cero)
     * @param reason   motivo opcional (por ejemplo «Compra a proveedor»)
     * @param user     usuario que registra la entrada
     * @return movimiento guardado
     */
    public InventoryMovement registerEntry(int partId, int quantity, String reason, User user) {
        requireUser(user);
        requirePositive(quantity);
        partService.increaseStock(partId, quantity);
        return save(MovementType.ENTRY, partService.getDetail(partId), quantity,
                defaultIfBlank(reason, "Entrada de inventario"), user);
    }

    /**
     * Registra una salida de stock (consumo o pérdida).
     *
     * @throws ValidationException si no hay stock suficiente
     */
    public InventoryMovement registerExit(int partId, int quantity, String reason, User user) {
        requireUser(user);
        requirePositive(quantity);
        partService.decreaseStock(partId, quantity);
        return save(MovementType.EXIT, partService.getDetail(partId), quantity,
                defaultIfBlank(reason, "Salida de inventario"), user);
    }

    /**
     * Ajusta el stock a un valor contado físicamente. El movimiento guarda la
     * diferencia (siempre positiva) y el motivo, que es obligatorio.
     *
     * @param partId   id del repuesto
     * @param newStock stock real contado (cero o más)
     * @param reason   motivo del ajuste (obligatorio)
     * @param user     usuario que ajusta
     * @return movimiento guardado
     * @throws ValidationException si el stock no cambia o falta el motivo
     */
    public InventoryMovement registerAdjustment(int partId, int newStock, String reason, User user) {
        requireUser(user);
        if (reason == null || reason.isBlank()) {
            throw new ValidationException("El motivo del ajuste es obligatorio");
        }
        if (newStock < 0) {
            throw new ValidationException("El stock no puede ser negativo");
        }
        Part part = partService.getDetail(partId);
        int difference = Math.abs(newStock - part.getCurrentStock());
        if (difference == 0) {
            throw new ValidationException("El stock ya es " + newStock + ": no hay nada que ajustar");
        }
        String detail = reason.trim() + " (de " + part.getCurrentStock() + " a " + newStock + ")";
        partService.adjustStock(partId, newStock);
        return save(MovementType.ADJUSTMENT, partService.getDetail(partId), difference, detail, user);
    }

    /** Historial de movimientos de un repuesto, del más reciente al más antiguo. */
    public List<InventoryMovement> findMovementsByPart(int partId) {
        partService.getDetail(partId);
        return repository.findByPart(partId);
    }

    /**
     * Últimos movimientos del taller.
     *
     * @param limit cantidad máxima (entre 1 y 100)
     */
    public List<InventoryMovement> findRecentMovements(int limit) {
        if (limit <= 0 || limit > 100) {
            throw new ValidationException("La cantidad de movimientos debe estar entre 1 y 100");
        }
        return repository.findRecent(limit);
    }

    private InventoryMovement save(MovementType type, Part part, int quantity, String reason, User user) {
        InventoryMovement movement = new InventoryMovement();
        movement.setType(type);
        movement.setPart(part);
        movement.setQuantity(quantity);
        movement.setReason(truncate(reason));
        movement.setUser(user);
        movement.setTimestamp(LocalDateTime.now());
        return repository.create(movement);
    }

    private static void requireUser(User user) {
        if (user == null) {
            throw new ValidationException("Debe haber un usuario en sesión para mover el inventario");
        }
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("La cantidad debe ser mayor que cero");
        }
    }

    private static String defaultIfBlank(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    private static String truncate(String text) {
        return text.length() <= MAX_REASON ? text : text.substring(0, MAX_REASON);
    }
}
