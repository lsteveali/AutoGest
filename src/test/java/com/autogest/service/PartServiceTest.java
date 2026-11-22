package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Part;
import com.autogest.model.Catalogs.StockStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link PartService} con repositorio falso, sin base de datos.
 */
class PartServiceTest {

    private FakePartRepository repository;
    private PartService service;

    @BeforeEach
    void setUp() {
        repository = new FakePartRepository();
        service = new PartService(repository);
    }

    // ---------- ayudas ----------

    private Part validPart() {
        Part part = new Part();
        part.setCode("REP-001");
        part.setName("Filtro de aceite");
        part.setCategory("Motor");
        part.setCurrentStock(25);
        part.setMinimumStock(10);
        part.setUnitPrice(new BigDecimal("35000.00"));
        return part;
    }

    private Part registered() {
        return service.registerPart(validPart());
    }

    private Part register(String code, String name, String category, int stock, int minimum) {
        Part part = validPart();
        part.setCode(code);
        part.setName(name);
        part.setCategory(category);
        part.setCurrentStock(stock);
        part.setMinimumStock(minimum);
        return service.registerPart(part);
    }

    private String errorOf(Part part) {
        return assertThrows(ValidationException.class, () -> service.registerPart(part)).getMessage();
    }

    private String updateErrorOf(Part part) {
        return assertThrows(ValidationException.class, () -> service.updatePart(part)).getMessage();
    }

    // ---------- registro correcto ----------

    @Test
    void registerSavesPartWithId() {
        Part saved = registered();

        assertTrue(saved.getId() > 0);
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void registerNormalizesTextFields() {
        Part part = validPart();
        part.setCode("  REP-001 ");
        part.setName("  Filtro de aceite  ");
        part.setCategory(" Motor ");

        Part saved = service.registerPart(part);

        assertEquals("REP-001", saved.getCode());
        assertEquals("Filtro de aceite", saved.getName());
        assertEquals("Motor", saved.getCategory());
    }

    @Test
    void registerRoundsThePriceToTwoDecimals() {
        Part part = validPart();
        part.setUnitPrice(new BigDecimal("35000.005"));

        assertEquals(new BigDecimal("35000.01"), service.registerPart(part).getUnitPrice());
    }

    // ---------- estado derivado ----------

    @Test
    void registerDerivesTheStatusFromStockAndMinimum() {
        Part available = register("REP-001", "Filtro de aceite", "Motor", 25, 10);
        Part atMinimum = register("REP-002", "Pastillas de freno", "Frenos", 10, 10);
        Part outOfStock = register("REP-003", "Aceite de motor", "Lubricantes", 0, 5);

        assertEquals(StockStatus.AVAILABLE, available.getStatus());
        assertEquals(StockStatus.LOW_STOCK, atMinimum.getStatus());
        assertEquals(StockStatus.OUT_OF_STOCK, outOfStock.getStatus());
    }

    // ---------- validación ----------

    @Test
    void registerRejectsNullPart() {
        ValidationException error = assertThrows(ValidationException.class, () -> service.registerPart(null));

        assertEquals("El repuesto es obligatorio", error.getMessage());
    }

    @Test
    void registerRequiresCode() {
        Part blank = validPart();
        blank.setCode("   ");
        Part missing = validPart();
        missing.setCode(null);

        assertEquals("El código es obligatorio", errorOf(blank));
        assertEquals("El código es obligatorio", errorOf(missing));
        assertEquals(0, repository.findAll().size());
    }

    @Test
    void registerRequiresName() {
        Part blank = validPart();
        blank.setName("  ");
        Part missing = validPart();
        missing.setName(null);

        assertEquals("El nombre es obligatorio", errorOf(blank));
        assertEquals("El nombre es obligatorio", errorOf(missing));
    }

    @Test
    void registerRequiresPrice() {
        Part part = validPart();
        part.setUnitPrice(null);

        assertEquals("El precio es obligatorio", errorOf(part));
    }

    @Test
    void registerRejectsNegativePrice() {
        Part part = validPart();
        part.setUnitPrice(new BigDecimal("-0.01"));

        assertEquals("El precio no puede ser negativo", errorOf(part));
    }

    // El stock negativo no llega al Service: el propio modelo lo rechaza en
    // setCurrentStock (ver PartTest), y el Service también lo valida por seguridad.

    @Test
    void registerRejectsNegativeMinimumStock() {
        Part part = validPart();
        part.setMinimumStock(-1);

        assertEquals("El stock mínimo no puede ser negativo", errorOf(part));
    }

    @Test
    void registerRejectsDuplicatedCode() {
        registered();

        assertEquals("Ya existe un repuesto con ese código", errorOf(validPart()));
        assertEquals(1, repository.findAll().size());
    }

    // ---------- actualización ----------

    @Test
    void updatePersistsTheChanges() {
        Part saved = registered();

        Part edited = validPart();
        edited.setId(saved.getId());
        edited.setUnitPrice(new BigDecimal("38000.00"));
        service.updatePart(edited);

        Part found = service.getDetail(saved.getId());
        assertEquals(0, found.getUnitPrice().compareTo(new BigDecimal("38000.00")));
        assertEquals("Filtro de aceite", found.getName());
    }

    @Test
    void updateAcceptsThePartsOwnCode() {
        Part saved = registered();

        Part edited = validPart();
        edited.setId(saved.getId());
        edited.setCategory("Frenos");

        assertEquals("Frenos", service.updatePart(edited).getCategory());
    }

    @Test
    void updateRejectsTheCodeOfAnotherPart() {
        registered();
        Part other = register("REP-002", "Pastillas de freno", "Frenos", 8, 10);

        Part edited = validPart();
        edited.setId(other.getId());
        edited.setName("Pastillas delanteras");

        assertEquals("Ya existe un repuesto con ese código", updateErrorOf(edited));
        assertEquals("REP-002", service.getDetail(other.getId()).getCode());
    }

    @Test
    void updateThrowsNotFoundForMissingPart() {
        Part edited = validPart();
        edited.setId(999);

        NotFoundException error = assertThrows(NotFoundException.class, () -> service.updatePart(edited));

        assertEquals("No se encontró el repuesto solicitado", error.getMessage());
    }

    @Test
    void updateValidatesTheData() {
        Part saved = registered();

        Part edited = validPart();
        edited.setId(saved.getId());
        edited.setUnitPrice(null);

        assertEquals("El precio es obligatorio", updateErrorOf(edited));
    }

    // ---------- detalle ----------

    @Test
    void getDetailReturnsThePart() {
        Part saved = registered();

        assertSame(saved, service.getDetail(saved.getId()));
    }

    @Test
    void getDetailThrowsNotFoundForMissingPart() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.getDetail(999));

        assertEquals("No se encontró el repuesto solicitado", error.getMessage());
    }

    // ---------- entrada de existencias ----------

    @Test
    void increaseStockAddsUnitsAndRecalculatesTheStatus() {
        Part saved = register("REP-001", "Aceite de motor", "Lubricantes", 0, 5);

        service.increaseStock(saved.getId(), 2);
        assertEquals(2, service.getDetail(saved.getId()).getCurrentStock());
        assertEquals(StockStatus.LOW_STOCK, service.getDetail(saved.getId()).getStatus());

        service.increaseStock(saved.getId(), 10);
        assertEquals(12, service.getDetail(saved.getId()).getCurrentStock());
        assertEquals(StockStatus.AVAILABLE, service.getDetail(saved.getId()).getStatus());
    }

    @Test
    void increaseStockPersistsTheNewStock() {
        Part saved = registered();

        service.increaseStock(saved.getId(), 5);

        assertEquals(30, repository.findById(saved.getId()).getCurrentStock());
    }

    @Test
    void increaseStockRejectsNonPositiveQuantity() {
        Part saved = registered();

        assertEquals("La cantidad debe ser mayor que cero",
                assertThrows(ValidationException.class,
                        () -> service.increaseStock(saved.getId(), 0)).getMessage());
        assertEquals("La cantidad debe ser mayor que cero",
                assertThrows(ValidationException.class,
                        () -> service.increaseStock(saved.getId(), -3)).getMessage());
        assertEquals(25, service.getDetail(saved.getId()).getCurrentStock());
    }

    @Test
    void increaseStockThrowsNotFoundForMissingPart() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.increaseStock(999, 5));

        assertEquals("No se encontró el repuesto solicitado", error.getMessage());
    }

    // ---------- salida de existencias ----------

    @Test
    void decreaseStockRemovesUnitsAndRecalculatesTheStatus() {
        Part saved = register("REP-001", "Filtro de aceite", "Motor", 25, 10);

        service.decreaseStock(saved.getId(), 10);
        assertEquals(15, service.getDetail(saved.getId()).getCurrentStock());
        assertEquals(StockStatus.AVAILABLE, service.getDetail(saved.getId()).getStatus());

        service.decreaseStock(saved.getId(), 6);
        assertEquals(StockStatus.LOW_STOCK, service.getDetail(saved.getId()).getStatus());

        service.decreaseStock(saved.getId(), 9);
        assertEquals(0, service.getDetail(saved.getId()).getCurrentStock());
        assertEquals(StockStatus.OUT_OF_STOCK, service.getDetail(saved.getId()).getStatus());
    }

    @Test
    void decreaseStockPersistsTheNewStock() {
        Part saved = registered();

        service.decreaseStock(saved.getId(), 5);

        assertEquals(20, repository.findById(saved.getId()).getCurrentStock());
    }

    @Test
    void decreaseStockRejectsNonPositiveQuantity() {
        Part saved = registered();

        assertEquals("La cantidad debe ser mayor que cero",
                assertThrows(ValidationException.class,
                        () -> service.decreaseStock(saved.getId(), 0)).getMessage());
        assertEquals("La cantidad debe ser mayor que cero",
                assertThrows(ValidationException.class,
                        () -> service.decreaseStock(saved.getId(), -1)).getMessage());
        assertEquals(25, service.getDetail(saved.getId()).getCurrentStock());
    }

    @Test
    void decreaseStockRejectsMoreThanAvailable() {
        Part saved = registered();

        ValidationException error = assertThrows(ValidationException.class,
                () -> service.decreaseStock(saved.getId(), 26));

        assertEquals("Stock insuficiente: solo hay 25 unidades", error.getMessage());
        assertEquals(25, service.getDetail(saved.getId()).getCurrentStock());
    }

    @Test
    void decreaseStockAllowsRemovingTheLastUnit() {
        Part saved = register("REP-001", "Bujia", "Motor", 1, 5);

        service.decreaseStock(saved.getId(), 1);

        assertEquals(0, service.getDetail(saved.getId()).getCurrentStock());
        assertEquals(StockStatus.OUT_OF_STOCK, service.getDetail(saved.getId()).getStatus());
    }

    @Test
    void decreaseStockThrowsNotFoundForMissingPart() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.decreaseStock(999, 1));

        assertEquals("No se encontró el repuesto solicitado", error.getMessage());
    }

    // ---------- ajuste de existencias ----------

    @Test
    void adjustStockSetsTheExactValueAndRecalculatesTheStatus() {
        Part saved = register("REP-001", "Filtro de aceite", "Motor", 25, 10);

        service.adjustStock(saved.getId(), 7);

        Part found = service.getDetail(saved.getId());
        assertEquals(7, found.getCurrentStock());
        assertEquals(StockStatus.LOW_STOCK, found.getStatus());
        assertEquals(7, repository.findById(saved.getId()).getCurrentStock());
    }

    @Test
    void adjustStockRejectsNegativeStock() {
        Part saved = registered();

        assertEquals("El stock no puede ser negativo",
                assertThrows(ValidationException.class,
                        () -> service.adjustStock(saved.getId(), -1)).getMessage());
        assertEquals(25, service.getDetail(saved.getId()).getCurrentStock());
    }

    @Test
    void adjustStockThrowsNotFoundForMissingPart() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.adjustStock(999, 5));

        assertEquals("No se encontró el repuesto solicitado", error.getMessage());
    }

    // ---------- conteos ----------

    @Test
    void countLowStockCountsOnlyPartsAtOrBelowTheMinimum() {
        register("REP-001", "Filtro de aceite", "Motor", 25, 10);
        register("REP-002", "Pastillas de freno", "Frenos", 8, 10);
        register("REP-003", "Bujia", "Motor", 10, 10);
        register("REP-004", "Aceite de motor", "Lubricantes", 0, 5);

        assertEquals(2, service.countLowStock());
    }

    @Test
    void countOutOfStockCountsOnlyEmptyParts() {
        register("REP-001", "Filtro de aceite", "Motor", 25, 10);
        register("REP-002", "Pastillas de freno", "Frenos", 8, 10);
        register("REP-004", "Aceite de motor", "Lubricantes", 0, 5);

        assertEquals(1, service.countOutOfStock());
    }

    @Test
    void countPartsCountsTheWholeCatalog() {
        register("REP-001", "Filtro de aceite", "Motor", 25, 10);
        register("REP-004", "Aceite de motor", "Lubricantes", 0, 5);

        assertEquals(2, service.countParts());
    }

    // ---------- búsqueda ----------

    @Test
    void searchFindsByNameAndCode() {
        register("REP-001", "Filtro de aceite", "Motor", 25, 10);
        register("REP-002", "Pastillas de freno", "Frenos", 8, 10);

        assertEquals(1, service.searchParts("filtro", null, null).size());
        assertEquals(1, service.searchParts("rep-002", null, null).size());
        assertTrue(service.searchParts("inexistente", null, null).isEmpty());
    }

    @Test
    void searchFiltersByStatusAndCategory() {
        register("REP-001", "Filtro de aceite", "Motor", 25, 10);
        register("REP-002", "Pastillas de freno", "Frenos", 8, 10);
        register("REP-004", "Aceite de motor", "Lubricantes", 0, 5);

        List<Part> all = service.searchParts(null, null, null);
        List<Part> low = service.searchParts(null, StockStatus.LOW_STOCK, null);
        List<Part> out = service.searchParts(null, StockStatus.OUT_OF_STOCK, null);
        List<Part> motor = service.searchParts(null, null, "Motor");

        assertEquals(3, all.size());
        assertEquals(1, low.size());
        assertEquals("Pastillas de freno", low.get(0).getName());
        assertEquals(1, out.size());
        assertEquals("Aceite de motor", out.get(0).getName());
        assertEquals(1, motor.size());
        assertEquals("Filtro de aceite", motor.get(0).getName());
    }

    @Test
    void searchWithBlankCriteriaReturnsEverything() {
        register("REP-001", "Filtro de aceite", "Motor", 25, 10);
        register("REP-002", "Pastillas de freno", "Frenos", 8, 10);

        assertEquals(2, service.searchParts("   ", null, null).size());
        assertEquals(2, service.searchParts(null, null, "  ").size());
    }

    // ---------- listado ----------

    @Test
    void findAllReturnsEveryPartOrderedByName() {
        register("REP-002", "Pastillas de freno", "Frenos", 8, 10);
        register("REP-001", "Filtro de aceite", "Motor", 25, 10);

        List<Part> all = service.findAll();

        assertEquals(2, all.size());
        assertEquals("Filtro de aceite", all.get(0).getName());
        assertEquals("Pastillas de freno", all.get(1).getName());
    }
}
