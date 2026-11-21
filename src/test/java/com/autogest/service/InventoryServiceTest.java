package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.InventoryMovement;
import com.autogest.model.MovementType;
import com.autogest.model.Part;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.StockStatus;
import com.autogest.model.Supplier;
import com.autogest.model.User;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link InventoryService}: entradas, salidas, ajustes y su
 * historial, con repositorios falsos (sin base de datos).
 */
class InventoryServiceTest {

    private FakePartRepository parts;
    private FakeInventoryMovementRepository movements;
    private PartService partService;
    private InventoryService service;
    private User user;

    @BeforeEach
    void setUp() {
        parts = new FakePartRepository();
        movements = new FakeInventoryMovementRepository();
        partService = new PartService(parts);
        service = new InventoryService(movements, partService);
        user = new User();
        user.setId(1);
        user.setFirstName("José");
        user.setLastName("García");
        user.setRole(Role.ADMINISTRATOR);
    }

    private Part part(int stock, int minimum) {
        Part part = new Part();
        part.setCode("REP-001");
        part.setName("Filtro de aceite");
        part.setCurrentStock(stock);
        part.setMinimumStock(minimum);
        part.setUnitPrice(new BigDecimal("35000"));
        return partService.registerPart(part);
    }

    @Test
    void registerPartWithInitialStockRecordsEntry() {
        Part draft = new Part();
        draft.setCode("REP-010");
        draft.setName("Bujía");
        draft.setCurrentStock(8);
        draft.setUnitPrice(new BigDecimal("28000"));
        Part saved = service.registerPart(draft, user);
        List<InventoryMovement> history = service.findMovementsByPart(saved.getId());
        assertEquals(1, history.size());
        assertEquals(MovementType.ENTRY, history.get(0).getType());
        assertEquals(8, history.get(0).getQuantity());
        assertEquals("Stock inicial", history.get(0).getReason());
    }

    @Test
    void registerPartWithoutStockRecordsNothing() {
        Part draft = new Part();
        draft.setCode("REP-011");
        draft.setName("Batería");
        draft.setUnitPrice(new BigDecimal("380000"));
        service.registerPart(draft, user);
        assertTrue(movements.all().isEmpty());
    }

    @Test
    void entryIncreasesStockAndRecordsMovement() {
        Part saved = part(5, 10);
        InventoryMovement movement = service.registerEntry(saved.getId(), 10, " Compra ", user);
        assertEquals(15, partService.getDetail(saved.getId()).getCurrentStock());
        assertEquals(StockStatus.AVAILABLE, partService.getDetail(saved.getId()).getStatus());
        assertEquals("Compra", movement.getReason());
        assertEquals(user, movement.getUser());
        assertTrue(movement.isEntry());
    }

    @Test
    void entryWithoutReasonUsesDefaultReason() {
        Part saved = part(5, 1);
        assertEquals("Entrada de inventario", service.registerEntry(saved.getId(), 1, null, user).getReason());
    }

    @Test
    void exitDecreasesStockAndUpdatesStatus() {
        Part saved = part(12, 10);
        service.registerExit(saved.getId(), 12, "Consumo", user);
        Part after = partService.getDetail(saved.getId());
        assertEquals(0, after.getCurrentStock());
        assertEquals(StockStatus.OUT_OF_STOCK, after.getStatus());
        assertEquals(MovementType.EXIT, movements.all().get(0).getType());
    }

    @Test
    void exitWithInsufficientStockFailsAndRecordsNothing() {
        Part saved = part(3, 1);
        assertThrows(ValidationException.class, () -> service.registerExit(saved.getId(), 4, null, user));
        assertEquals(3, partService.getDetail(saved.getId()).getCurrentStock());
        assertTrue(movements.all().isEmpty());
    }

    @Test
    void quantityMustBePositive() {
        Part saved = part(3, 1);
        assertThrows(ValidationException.class, () -> service.registerEntry(saved.getId(), 0, null, user));
        assertThrows(ValidationException.class, () -> service.registerExit(saved.getId(), -2, null, user));
    }

    @Test
    void movementsRequireUser() {
        Part saved = part(3, 1);
        assertThrows(ValidationException.class, () -> service.registerEntry(saved.getId(), 1, null, null));
    }

    @Test
    void unknownPartThrowsNotFound() {
        assertThrows(NotFoundException.class, () -> service.registerEntry(99, 1, null, user));
    }

    @Test
    void adjustmentStoresDifferenceAndReason() {
        Part saved = part(10, 2);
        InventoryMovement movement = service.registerAdjustment(saved.getId(), 7, "Conteo físico", user);
        assertEquals(7, partService.getDetail(saved.getId()).getCurrentStock());
        assertEquals(3, movement.getQuantity());
        assertEquals(MovementType.ADJUSTMENT, movement.getType());
        assertEquals("Conteo físico (de 10 a 7)", movement.getReason());
    }

    @Test
    void adjustmentRequiresReasonAndChange() {
        Part saved = part(10, 2);
        assertThrows(ValidationException.class, () -> service.registerAdjustment(saved.getId(), 7, " ", user));
        assertThrows(ValidationException.class, () -> service.registerAdjustment(saved.getId(), 10, "Conteo", user));
        assertThrows(ValidationException.class, () -> service.registerAdjustment(saved.getId(), -1, "Conteo", user));
    }

    @Test
    void historyIsNewestFirst() {
        Part saved = part(10, 2);
        service.registerEntry(saved.getId(), 1, "primero", user);
        service.registerExit(saved.getId(), 2, "segundo", user);
        assertEquals("segundo", service.findMovementsByPart(saved.getId()).get(0).getReason());
    }

    @Test
    void recentMovementsValidatesLimit() {
        assertThrows(ValidationException.class, () -> service.findRecentMovements(0));
        assertThrows(ValidationException.class, () -> service.findRecentMovements(101));
        assertTrue(service.findRecentMovements(5).isEmpty());
    }

    @Test
    void searchPartsFiltersBySupplier() {
        Supplier supplier = new Supplier();
        supplier.setId(7);
        Part withSupplier = part(5, 1);
        withSupplier.setSupplier(supplier);
        Part other = new Part();
        other.setCode("REP-002");
        other.setName("Pastillas");
        other.setUnitPrice(BigDecimal.TEN);
        partService.registerPart(other);
        assertEquals(1, partService.searchParts(null, null, null, 7).size());
        assertEquals(2, partService.searchParts(null, null, null, 0).size());
    }

    @Test
    void countAvailableExcludesLowAndOutOfStock() {
        part(20, 5);
        Part low = new Part();
        low.setCode("REP-002");
        low.setName("Pastillas");
        low.setCurrentStock(2);
        low.setMinimumStock(5);
        low.setUnitPrice(BigDecimal.TEN);
        partService.registerPart(low);
        assertEquals(1, partService.countAvailable());
    }
}
