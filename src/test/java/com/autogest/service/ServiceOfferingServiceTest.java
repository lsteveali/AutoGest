package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link ServiceOfferingService} con repositorio falso, sin base de datos.
 */
class ServiceOfferingServiceTest {

    private FakeServiceOfferingRepository repository;
    private ServiceOfferingService service;

    @BeforeEach
    void setUp() {
        repository = new FakeServiceOfferingRepository();
        service = new ServiceOfferingService(repository);
    }

    // ---------- ayudas ----------

    private ServiceOffering validService() {
        ServiceOffering service = new ServiceOffering();
        service.setCode("SRV-001");
        service.setName("Cambio de aceite");
        service.setCategory("Mantenimiento");
        service.setDescription("Cambio de aceite y filtro.");
        service.setDurationHours(1.0);
        service.setPrice(new BigDecimal("120000.00"));
        return service;
    }

    private ServiceOffering registered() {
        return service.registerService(validService());
    }

    private ServiceOffering register(String code, String name, String category) {
        ServiceOffering offering = validService();
        offering.setCode(code);
        offering.setName(name);
        offering.setCategory(category);
        return service.registerService(offering);
    }

    private String errorOf(ServiceOffering offering) {
        return assertThrows(ValidationException.class, () -> service.registerService(offering)).getMessage();
    }

    // ---------- registro correcto ----------

    @Test
    void registerSavesActiveServiceWithId() {
        ServiceOffering saved = registered();

        assertTrue(saved.getId() > 0);
        assertEquals(ServiceOfferingStatus.ACTIVE, saved.getStatus());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void registerUsesActiveWhenTheCallerSendsNoStatus() {
        ServiceOffering offering = validService();
        offering.setStatus(null);

        assertEquals(ServiceOfferingStatus.ACTIVE, service.registerService(offering).getStatus());
    }

    @Test
    void registerNormalizesTextFields() {
        ServiceOffering offering = validService();
        offering.setCode("  SRV-001 ");
        offering.setName("  Cambio de aceite  ");
        offering.setCategory(" Mantenimiento ");
        offering.setDescription("  Cambio de aceite y filtro.  ");

        ServiceOffering saved = service.registerService(offering);

        assertEquals("SRV-001", saved.getCode());
        assertEquals("Cambio de aceite", saved.getName());
        assertEquals("Mantenimiento", saved.getCategory());
        assertEquals("Cambio de aceite y filtro.", saved.getDescription());
    }

    @Test
    void registerRoundsThePriceToTwoDecimals() {
        ServiceOffering offering = validService();
        offering.setPrice(new BigDecimal("120000.005"));

        assertEquals(new BigDecimal("120000.01"), service.registerService(offering).getPrice());
    }

    // ---------- validación ----------

    @Test
    void registerRejectsNullService() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.registerService(null));

        assertEquals("El servicio es obligatorio", error.getMessage());
    }

    @Test
    void registerRequiresCode() {
        ServiceOffering blank = validService();
        blank.setCode("   ");
        ServiceOffering missing = validService();
        missing.setCode(null);

        assertEquals("El código es obligatorio", errorOf(blank));
        assertEquals("El código es obligatorio", errorOf(missing));
        assertEquals(0, repository.findAll().size());
    }

    @Test
    void registerRequiresName() {
        ServiceOffering blank = validService();
        blank.setName("  ");
        ServiceOffering missing = validService();
        missing.setName(null);

        assertEquals("El nombre es obligatorio", errorOf(blank));
        assertEquals("El nombre es obligatorio", errorOf(missing));
    }

    @Test
    void registerRequiresPrice() {
        ServiceOffering offering = validService();
        offering.setPrice(null);

        assertEquals("El precio es obligatorio", errorOf(offering));
    }

    @Test
    void registerRejectsNegativePrice() {
        ServiceOffering offering = validService();
        offering.setPrice(new BigDecimal("-0.01"));

        assertEquals("El precio no puede ser negativo", errorOf(offering));
    }

    @Test
    void registerRejectsNegativeDuration() {
        ServiceOffering offering = validService();
        offering.setDurationHours(-0.5);

        assertEquals("La duración no puede ser negativa", errorOf(offering));
    }

    @Test
    void registerAcceptsZeroPriceAndZeroDuration() {
        ServiceOffering offering = validService();
        offering.setPrice(BigDecimal.ZERO);
        offering.setDurationHours(0);
        offering.setCode("SRV-002");

        ServiceOffering saved = service.registerService(offering);

        assertEquals(0, saved.getPrice().compareTo(BigDecimal.ZERO));
        assertEquals(0.0, saved.getDurationHours());
    }

    @Test
    void registerRejectsDuplicatedCode() {
        registered();

        ServiceOffering second = validService();

        assertEquals("Ya existe un servicio con ese código", errorOf(second));
        assertEquals(1, repository.findAll().size());
    }

    // ---------- actualización ----------

    @Test
    void updatePersistsTheChanges() {
        ServiceOffering saved = registered();

        ServiceOffering edited = validService();
        edited.setId(saved.getId());
        edited.setName("Cambio de aceite y filtro");
        edited.setPrice(new BigDecimal("130000.00"));

        service.updateService(edited);

        ServiceOffering found = service.getDetail(saved.getId());
        assertEquals("Cambio de aceite y filtro", found.getName());
        assertEquals(0, found.getPrice().compareTo(new BigDecimal("130000.00")));
    }

    @Test
    void updateAcceptsTheServicesOwnCode() {
        ServiceOffering saved = registered();

        ServiceOffering edited = validService();
        edited.setId(saved.getId());
        edited.setCategory("Frenos");

        assertEquals("Frenos", service.updateService(edited).getCategory());
    }

    @Test
    void updateRejectsTheCodeOfAnotherService() {
        registered();
        ServiceOffering other = register("SRV-002", "Cambio de pastillas", "Frenos");

        ServiceOffering edited = validService();
        edited.setId(other.getId());
        edited.setName("Pastillas delanteras");

        ValidationException error = assertThrows(ValidationException.class, () -> service.updateService(edited));

        assertEquals("Ya existe un servicio con ese código", error.getMessage());
        assertEquals("SRV-002", service.getDetail(other.getId()).getCode());
    }

    @Test
    void updateThrowsNotFoundForMissingService() {
        ServiceOffering edited = validService();
        edited.setId(999);

        NotFoundException error = assertThrows(NotFoundException.class, () -> service.updateService(edited));

        assertEquals("No se encontró el servicio solicitado", error.getMessage());
    }

    @Test
    void updateValidatesTheData() {
        ServiceOffering saved = registered();

        ServiceOffering edited = validService();
        edited.setId(saved.getId());
        edited.setPrice(null);

        ValidationException error = assertThrows(ValidationException.class, () -> service.updateService(edited));

        assertEquals("El precio es obligatorio", error.getMessage());
    }

    // ---------- detalle ----------

    @Test
    void getDetailReturnsTheService() {
        ServiceOffering saved = registered();

        assertSame(saved, service.getDetail(saved.getId()));
    }

    @Test
    void getDetailThrowsNotFoundForMissingService() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.getDetail(999));

        assertEquals("No se encontró el servicio solicitado", error.getMessage());
    }

    // ---------- baja lógica ----------

    @Test
    void deactivateChangesStatusToInactiveAndKeepsTheService() {
        ServiceOffering saved = registered();

        service.deactivateService(saved.getId());

        ServiceOffering found = service.getDetail(saved.getId());
        assertEquals(ServiceOfferingStatus.INACTIVE, found.getStatus());
        assertEquals("Cambio de aceite", found.getName());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void deactivateThrowsNotFoundForMissingService() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.deactivateService(999));

        assertEquals("No se encontró el servicio solicitado", error.getMessage());
    }

    @Test
    void deleteServiceMarksItInactiveAndKeepsTheData() {
        ServiceOffering saved = registered();

        service.deleteService(saved.getId());

        ServiceOffering deleted = service.getDetail(saved.getId());
        assertEquals(ServiceOfferingStatus.INACTIVE, deleted.getStatus());
        assertEquals("SRV-001", deleted.getCode());
    }

    // ---------- búsqueda ----------

    @Test
    void searchFindsByNameCodeAndDescription() {
        ServiceOffering oil = validService();
        service.registerService(oil);

        ServiceOffering brakes = validService();
        brakes.setCode("SRV-002");
        brakes.setName("Cambio de pastillas");
        brakes.setCategory("Frenos");
        brakes.setDescription("Reemplazo de pastillas delanteras.");
        service.registerService(brakes);

        assertEquals(1, service.searchServices("cambio de aceite", null, null).size());
        assertEquals(1, service.searchServices("srv-002", null, null).size());
        assertEquals(1, service.searchServices("delanteras", null, null).size());
        assertTrue(service.searchServices("inexistente", null, null).isEmpty());
    }

    @Test
    void searchFiltersByStatusAndCategory() {
        ServiceOffering first = register("SRV-001", "Cambio de aceite", "Mantenimiento");
        register("SRV-002", "Cambio de pastillas", "Frenos");
        register("SRV-003", "Revisión general", "Mantenimiento");
        service.deactivateService(first.getId());

        List<ServiceOffering> all = service.searchServices(null, null, null);
        List<ServiceOffering> active = service.searchServices(null, ServiceOfferingStatus.ACTIVE, null);
        List<ServiceOffering> inactive = service.searchServices(null, ServiceOfferingStatus.INACTIVE, null);
        List<ServiceOffering> brakes = service.searchServices(null, null, "Frenos");

        assertEquals(3, all.size());
        assertEquals(2, active.size());
        assertEquals(1, inactive.size());
        assertEquals("Cambio de aceite", inactive.get(0).getName());
        assertEquals(1, brakes.size());
        assertEquals("Cambio de pastillas", brakes.get(0).getName());
    }

    @Test
    void searchWithBlankCriteriaReturnsEverything() {
        register("SRV-001", "Cambio de aceite", "Mantenimiento");
        register("SRV-002", "Cambio de pastillas", "Frenos");

        assertEquals(2, service.searchServices("   ", null, null).size());
        assertEquals(2, service.searchServices(null, null, "  ").size());
    }

    // ---------- categorías ----------

    @Test
    void findCategoriesReturnsDistinctOrderedCategories() {
        register("SRV-001", "Cambio de aceite", "Mantenimiento");
        register("SRV-002", "Cambio de pastillas", "Frenos");
        register("SRV-003", "Revisión general", "Mantenimiento");

        assertEquals(List.of("Frenos", "Mantenimiento"), service.findCategories());
    }

    @Test
    void findCategoriesExcludesNullAndBlankValues() {
        ServiceOffering noCategory = validService();
        noCategory.setCategory(null);
        service.registerService(noCategory);

        ServiceOffering blankCategory = validService();
        blankCategory.setCode("SRV-002");
        blankCategory.setName("Revisión general");
        blankCategory.setCategory("   ");
        service.registerService(blankCategory);

        assertTrue(service.findCategories().isEmpty());
    }

    @Test
    void findCategoriesIsEmptyWithoutServices() {
        assertTrue(service.findCategories().isEmpty());
    }

    // ---------- listado ----------

    @Test
    void findAllReturnsEveryServiceOrderedByName() {
        register("SRV-002", "Cambio de pastillas", "Frenos");
        register("SRV-001", "Cambio de aceite", "Mantenimiento");

        List<ServiceOffering> all = service.findAll();

        assertEquals(2, all.size());
        assertEquals("Cambio de aceite", all.get(0).getName());
        assertEquals("Cambio de pastillas", all.get(1).getName());
    }
}
