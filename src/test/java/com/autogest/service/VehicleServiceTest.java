package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.model.Catalogs.DocumentType;
import com.autogest.model.FuelType;
import com.autogest.model.Transmission;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleStatus;
import com.autogest.model.VehicleType;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link VehicleService} con repositorios falsos, sin base de datos.
 */
class VehicleServiceTest {

    private FakeVehicleRepository repository;
    private FakeCustomerRepository customerRepository;
    private VehicleService service;
    private Customer owner;
    private int ownerSequence;

    private static final int MIN_YEAR = 1900;

    @BeforeEach
    void setUp() {
        repository = new FakeVehicleRepository();
        customerRepository = new FakeCustomerRepository();
        service = new VehicleService(repository, customerRepository);
        owner = createOwner("Ana", "Torres");
    }

    // ---------- ayudas ----------

    private Customer createOwner(String firstName, String lastName) {
        Customer customer = new Customer();
        customer.setDocumentType(DocumentType.CC);
        customer.setDocumentNumber("DOC" + (++ownerSequence));
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhone("3001234567");
        customer.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        return customerRepository.create(customer);
    }

    private Vehicle validVehicle() {
        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(owner);
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setYear(Year.now().getValue() - 5);
        vehicle.setLicensePlate("ABC123");
        vehicle.setFuelType(FuelType.GASOLINE);
        vehicle.setMileage(50000);
        vehicle.setVehicleType(VehicleType.CAR);
        vehicle.setTransmission(Transmission.AUTOMATIC);
        vehicle.setColor("Blanco");
        return vehicle;
    }

    private Vehicle registered() {
        return service.registerVehicle(validVehicle());
    }

    private String errorOf(Vehicle vehicle) {
        return assertThrows(ValidationException.class, () -> service.registerVehicle(vehicle)).getMessage();
    }

    private int maxYear() {
        return Year.now().getValue() + 1;
    }

    // ---------- registro correcto ----------

    @Test
    void registerSavesActiveVehicleWithId() {
        Vehicle saved = registered();

        assertTrue(saved.getId() > 0);
        assertEquals(VehicleStatus.ACTIVE, saved.getStatus());
        assertEquals(1, repository.count());
        assertEquals(owner.getId(), saved.getOwner().getId());
    }

    @Test
    void registerIgnoresStatusSentByTheCaller() {
        Vehicle vehicle = validVehicle();
        vehicle.setStatus(VehicleStatus.IN_SERVICE);

        assertEquals(VehicleStatus.ACTIVE, service.registerVehicle(vehicle).getStatus());
    }

    @Test
    void registerNormalizesTextFields() {
        Vehicle vehicle = validVehicle();
        vehicle.setMake("  Toyota   ");
        vehicle.setModel(" Corolla  X ");
        vehicle.setLicensePlate("  abc-123 ");
        vehicle.setColor("  Rojo   ");
        vehicle.setEngineNumber("  1NZ-FE  ");
        vehicle.setRegistrationCardNumber("  ABC 123  ");
        vehicle.setObservations("  Sin  detalles ");

        Vehicle saved = service.registerVehicle(vehicle);

        assertEquals("Toyota", saved.getMake());
        assertEquals("Corolla X", saved.getModel());
        assertEquals("ABC-123", saved.getLicensePlate());
        assertEquals("Rojo", saved.getColor());
        assertEquals("1NZ-FE", saved.getEngineNumber());
        assertEquals("ABC 123", saved.getRegistrationCardNumber());
        assertEquals("Sin  detalles", saved.getObservations());
    }

    @Test
    void registerTurnsBlankOptionalFieldsIntoNull() {
        Vehicle vehicle = validVehicle();
        vehicle.setVin("   ");
        vehicle.setColor("");
        vehicle.setEngineNumber("  ");
        vehicle.setRegistrationCardNumber("");
        vehicle.setObservations("  ");

        Vehicle saved = service.registerVehicle(vehicle);

        assertNull(saved.getVin());
        assertNull(saved.getColor());
        assertNull(saved.getEngineNumber());
        assertNull(saved.getRegistrationCardNumber());
        assertNull(saved.getObservations());
    }

    // ---------- propietario ----------

    @Test
    void registerRejectsMissingOwner() {
        Vehicle noOwner = validVehicle();
        noOwner.setOwner(null);
        Vehicle emptyOwner = validVehicle();
        emptyOwner.setOwner(new Customer());

        assertEquals("Selecciona el propietario del vehículo", errorOf(noOwner));
        assertEquals("Selecciona el propietario del vehículo", errorOf(emptyOwner));
    }

    @Test
    void registerRejectsUnknownOwner() {
        Vehicle vehicle = validVehicle();
        Customer unknown = new Customer();
        unknown.setId(999);
        vehicle.setOwner(unknown);

        assertEquals("El propietario seleccionado no existe", errorOf(vehicle));
        assertEquals(0, repository.count());
    }

    // ---------- marca y modelo ----------

    @Test
    void registerRequiresMakeAndModel() {
        Vehicle noMake = validVehicle();
        noMake.setMake("   ");
        Vehicle noModel = validVehicle();
        noModel.setModel(null);

        assertEquals("La marca es obligatoria", errorOf(noMake));
        assertEquals("El modelo es obligatorio", errorOf(noModel));
    }

    // ---------- año ----------

    @Test
    void registerRejectsYearOutsideAllowedRange() {
        Vehicle tooOld = validVehicle();
        tooOld.setYear(MIN_YEAR - 1);
        Vehicle tooFuture = validVehicle();
        tooFuture.setYear(maxYear() + 1);

        String message = "El año debe estar entre 1900 y " + maxYear();
        assertEquals(message, errorOf(tooOld));
        assertEquals(message, errorOf(tooFuture));
    }

    @Test
    void yearRangeLimitsAreInclusive() {
        Vehicle earliest = validVehicle();
        earliest.setYear(MIN_YEAR);
        Vehicle latest = validVehicle();
        latest.setYear(maxYear());
        latest.setLicensePlate("ZZZ999");

        assertEquals(MIN_YEAR, service.registerVehicle(earliest).getYear());
        assertEquals(maxYear(), service.registerVehicle(latest).getYear());
    }

    // ---------- placa ----------

    @Test
    void registerRequiresPlate() {
        Vehicle blank = validVehicle();
        blank.setLicensePlate("   ");
        Vehicle missing = validVehicle();
        missing.setLicensePlate(null);

        assertEquals("La placa es obligatoria", errorOf(blank));
        assertEquals("La placa es obligatoria", errorOf(missing));
    }

    @Test
    void registerRejectsInvalidPlateCharacters() {
        Vehicle vehicle = validVehicle();
        vehicle.setLicensePlate("ABC@123");

        assertEquals("La placa solo puede tener letras, números y guiones", errorOf(vehicle));
    }

    @Test
    void plateLengthMustBeBetweenFiveAndTen() {
        Vehicle tooShort = validVehicle();
        tooShort.setLicensePlate("AB12");
        Vehicle tooLong = validVehicle();
        tooLong.setLicensePlate("ABCDEFGHIJK");

        assertEquals("La placa debe tener entre 5 y 10 caracteres", errorOf(tooShort));
        assertEquals("La placa debe tener entre 5 y 10 caracteres", errorOf(tooLong));
    }

    @Test
    void registerRejectsDuplicatedPlate() {
        registered();

        Vehicle second = validVehicle();
        second.setOwner(createOwner("Luis", "Gómez"));

        assertEquals("Ya existe un vehículo con esa placa", errorOf(second));
        assertEquals(1, repository.count());
    }

    @Test
    void registerRejectsDuplicatedPlateEvenWithDifferentCaseAndSpacing() {
        registered();

        Vehicle second = validVehicle();
        second.setOwner(createOwner("Luis", "Gómez"));
        second.setLicensePlate("  abc 123 ");

        assertEquals("Ya existe un vehículo con esa placa", errorOf(second));
    }

    // ---------- combustible y kilometraje ----------

    @Test
    void registerRequiresFuelType() {
        Vehicle vehicle = validVehicle();
        vehicle.setFuelType(null);

        assertEquals("Selecciona el tipo de combustible", errorOf(vehicle));
    }

    @Test
    void registerRejectsNegativeMileage() {
        Vehicle vehicle = validVehicle();
        vehicle.setMileage(-1);

        assertEquals("El kilometraje no puede ser negativo", errorOf(vehicle));
    }

    // ---------- VIN ----------

    @Test
    void registerAcceptsAValidVin() {
        Vehicle vehicle = validVehicle();
        vehicle.setVin(" 1hgcm82633a004352 ");

        assertEquals("1HGCM82633A004352", service.registerVehicle(vehicle).getVin());
    }

    @Test
    void registerRejectsVinWithWrongLengthOrCharacters() {
        Vehicle tooShort = validVehicle();
        tooShort.setVin("1234567890");
        Vehicle tooLong = validVehicle();
        tooLong.setVin("123456789012345678");
        Vehicle withSymbol = validVehicle();
        withSymbol.setVin("12345678901!");

        String message = "El VIN debe tener entre 11 y 17 caracteres alfanuméricos";
        assertEquals(message, errorOf(tooShort));
        assertEquals(message, errorOf(tooLong));
        assertEquals(message, errorOf(withSymbol));
    }

    // ---------- entrada inválida ----------

    @Test
    void registerRejectsNullVehicle() {
        ValidationException error = assertThrows(ValidationException.class, () -> service.registerVehicle(null));

        assertEquals("Los datos del vehículo son obligatorios", error.getMessage());
    }

    @Test
    void registerRejectsAnEmptyForm() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> service.registerVehicle(new Vehicle()));

        assertEquals("Selecciona el propietario del vehículo", error.getMessage());
        assertEquals(0, repository.count());
    }

    // ---------- actualización ----------

    @Test
    void updateKeepsCurrentStatus() {
        Vehicle saved = registered();
        service.changeStatus(saved.getId(), VehicleStatus.IN_SERVICE);

        Vehicle edited = validVehicle();
        edited.setId(saved.getId());
        edited.setModel("Corolla Cross");

        Vehicle result = service.updateVehicle(edited);

        assertEquals("Corolla Cross", repository.findById(saved.getId()).getModel());
        assertEquals(VehicleStatus.IN_SERVICE, result.getStatus());
    }

    @Test
    void updateAcceptsTheVehiclesOwnPlate() {
        Vehicle saved = registered();

        Vehicle edited = validVehicle();
        edited.setId(saved.getId());
        edited.setColor("Azul");

        assertEquals("Azul", service.updateVehicle(edited).getColor());
    }

    @Test
    void updateRejectsThePlateOfAnotherVehicle() {
        registered();
        Vehicle other = validVehicle();
        other.setLicensePlate("XYZ789");
        other.setOwner(createOwner("Luis", "Gómez"));
        service.registerVehicle(other);

        Vehicle edited = validVehicle();
        edited.setId(other.getId());
        edited.setMake("Mazda");
        edited.setLicensePlate("ABC123");

        ValidationException error = assertThrows(ValidationException.class, () -> service.updateVehicle(edited));

        assertEquals("Ya existe un vehículo con esa placa", error.getMessage());
        assertEquals("XYZ789", repository.findById(other.getId()).getLicensePlate());
    }

    @Test
    void updateThrowsNotFoundForMissingVehicle() {
        Vehicle edited = validVehicle();
        edited.setId(999);

        NotFoundException error = assertThrows(NotFoundException.class, () -> service.updateVehicle(edited));

        assertEquals("Vehículo no encontrado", error.getMessage());
    }

    @Test
    void updateValidatesTheData() {
        Vehicle saved = registered();

        Vehicle edited = validVehicle();
        edited.setId(saved.getId());
        edited.setMileage(-5);

        ValidationException error = assertThrows(ValidationException.class, () -> service.updateVehicle(edited));

        assertEquals("El kilometraje no puede ser negativo", error.getMessage());
    }

    // ---------- detalle ----------

    @Test
    void getDetailReturnsTheVehicle() {
        Vehicle saved = registered();

        assertSame(saved, service.getDetail(saved.getId()));
    }

    @Test
    void getDetailResolvesTheOwnerFromItsId() {
        Vehicle vehicle = validVehicle();
        Customer onlyId = new Customer();
        onlyId.setId(owner.getId());
        vehicle.setOwner(onlyId);
        Vehicle saved = service.registerVehicle(vehicle);

        Vehicle detail = service.getDetail(saved.getId());

        assertEquals("Ana", detail.getOwner().getFirstName());
        assertEquals("Torres", detail.getOwner().getLastName());
    }

    @Test
    void getDetailThrowsNotFoundForMissingVehicle() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.getDetail(999));

        assertEquals("Vehículo no encontrado", error.getMessage());
    }

    // ---------- estado y baja lógica ----------

    @Test
    void changeStatusUpdatesTheStatusAndKeepsTheData() {
        Vehicle saved = registered();

        service.changeStatus(saved.getId(), VehicleStatus.IN_SERVICE);

        Vehicle found = repository.findById(saved.getId());
        assertEquals(VehicleStatus.IN_SERVICE, found.getStatus());
        assertEquals("Toyota", found.getMake());
        assertEquals(1, repository.count());
    }

    @Test
    void changeStatusRejectsNullStatus() {
        Vehicle saved = registered();

        ValidationException error = assertThrows(ValidationException.class,
                () -> service.changeStatus(saved.getId(), null));

        assertEquals("Selecciona el estado del vehículo", error.getMessage());
    }

    @Test
    void changeStatusThrowsNotFoundForMissingVehicle() {
        NotFoundException error = assertThrows(NotFoundException.class,
                () -> service.changeStatus(999, VehicleStatus.ACTIVE));

        assertEquals("Vehículo no encontrado", error.getMessage());
    }

    @Test
    void deactivateChangesStatusToInactiveAndKeepsTheVehicle() {
        Vehicle saved = registered();

        service.deactivateVehicle(saved.getId());

        Vehicle found = repository.findById(saved.getId());
        assertEquals(VehicleStatus.INACTIVE, found.getStatus());
        assertEquals("Toyota", found.getMake());
        assertEquals(1, repository.count());
    }

    @Test
    void deactivateAnAlreadyInactiveVehicleDoesNothing() {
        Vehicle saved = registered();
        service.deactivateVehicle(saved.getId());

        service.deactivateVehicle(saved.getId());

        assertEquals(VehicleStatus.INACTIVE, repository.findById(saved.getId()).getStatus());
    }

    @Test
    void deactivateThrowsNotFoundForMissingVehicle() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.deactivateVehicle(999));

        assertEquals("Vehículo no encontrado", error.getMessage());
    }

    // ---------- búsqueda ----------

    @Test
    void searchFindsByMakePlateVinAndOwner() {
        Vehicle first = validVehicle();
        first.setMake("Toyota");
        first.setModel("Corolla");
        first.setLicensePlate("ABC123");
        first.setVin("1HGCM82633A004352");
        service.registerVehicle(first);

        Vehicle second = validVehicle();
        second.setMake("Mazda");
        second.setModel("CX5");
        second.setLicensePlate("XYZ789");
        second.setOwner(createOwner("Luis", "Gómez"));
        service.registerVehicle(second);

        assertEquals(1, service.searchVehiclesByType("corolla", null, null).size());
        assertEquals(1, service.searchVehiclesByType("xyz789", null, null).size());
        assertEquals(1, service.searchVehiclesByType("1HGCM82633", null, null).size());
        assertEquals(1, service.searchVehiclesByType("Torres", null, null).size());
        assertTrue(service.searchVehiclesByType("inexistente", null, null).isEmpty());
    }

    private Vehicle register(String make, String model, String plate) {
        Vehicle vehicle = validVehicle();
        vehicle.setMake(make);
        vehicle.setModel(model);
        vehicle.setLicensePlate(plate);
        return service.registerVehicle(vehicle);
    }

    @Test
    void searchFiltersByStatusAndType() {
        Vehicle first = register("Toyota", "Corolla", "ABC123");
        register("Mazda", "CX5", "XYZ789");
        service.changeStatus(first.getId(), VehicleStatus.IN_SERVICE);

        List<Vehicle> all = service.searchVehiclesByType(null, null, null);
        List<Vehicle> active = service.searchVehiclesByType(null, VehicleStatus.ACTIVE, null);
        List<Vehicle> inService = service.searchVehiclesByType(null, VehicleStatus.IN_SERVICE, null);
        List<Vehicle> cars = service.searchVehiclesByType(null, null, VehicleType.CAR);

        assertEquals(2, all.size());
        assertEquals(1, active.size());
        assertEquals("Mazda", active.get(0).getMake());
        assertEquals(1, inService.size());
        assertEquals("Toyota", inService.get(0).getMake());
        assertEquals(2, cars.size());
    }

    @Test
    void searchResolvesTheOwner() {
        register("Toyota", "Corolla", "ABC123");

        List<Vehicle> result = service.searchVehiclesByType("Toyota", null, null);

        assertEquals("Ana Torres", result.get(0).getOwner().getFullName());
    }

    // ---------- conteos ----------

    @Test
    void countsTotalActiveAndInServiceVehicles() {
        Vehicle first = register("Toyota", "Corolla", "ABC123");
        register("Mazda", "CX5", "XYZ789");
        register("Kia", "Picanto", "KIA001");
        service.changeStatus(first.getId(), VehicleStatus.IN_SERVICE);

        assertEquals(3, service.countVehicles());
        assertEquals(2, service.countActiveVehicles());
        assertEquals(1, service.countInService());
    }

    @Test
    void countMaintenanceDueCountsOnlyVehiclesWithinTheWarningWindow() {
        Vehicle due = validVehicle();
        due.setLicensePlate("DUE001");
        due.setNextMaintenanceDate(LocalDate.now().plusDays(10));
        service.registerVehicle(due);

        Vehicle expired = validVehicle();
        expired.setLicensePlate("EXP001");
        expired.setNextMaintenanceDate(LocalDate.now().minusDays(5));
        service.registerVehicle(expired);

        Vehicle farAway = validVehicle();
        farAway.setLicensePlate("FAR001");
        farAway.setNextMaintenanceDate(LocalDate.now().plusDays(60));
        service.registerVehicle(farAway);

        Vehicle none = validVehicle();
        none.setLicensePlate("NNE001");
        service.registerVehicle(none);

        assertEquals(2, service.countMaintenanceDue());
    }

    // ---------- propietarios para el desplegable ----------

    @Test
    void listActiveOwnersReturnsOnlyActiveCustomers() {
        Customer inactive = createOwner("Pedro", "Ruiz");
        inactive.setStatus(CustomerStatus.INACTIVE);
        customerRepository.update(inactive);

        List<Customer> owners = service.listActiveOwners();

        assertEquals(1, owners.size());
        assertEquals("Ana Torres", owners.get(0).getFullName());
    }

    // ---------- contrato 7.5 ----------

    @Test
    void searchByMakeFiltersByBrand() {
        register("Toyota", "Corolla", "ABC123");
        register("Mazda", "CX5", "XYZ789");
        register("Toyota", "Hilux", "TOY002");

        assertEquals(2, service.searchVehicles(null, "Toyota", null).size());
        assertEquals(2, service.searchVehicles(null, "toyota", null).size());
        assertEquals(1, service.searchVehicles(null, "Mazda", null).size());
        assertTrue(service.searchVehicles(null, "Chevrolet", null).isEmpty());
    }

    @Test
    void searchByMakeCombinesWithStatus() {
        Vehicle first = register("Toyota", "Corolla", "ABC123");
        register("Mazda", "CX5", "XYZ789");
        service.changeStatus(first.getId(), VehicleStatus.IN_SERVICE);

        assertEquals(1, service.searchVehicles(null, "Toyota", VehicleStatus.IN_SERVICE).size());
        assertTrue(service.searchVehicles(null, "Toyota", VehicleStatus.ACTIVE).isEmpty());
        assertEquals(1, service.searchVehicles(null, null, VehicleStatus.ACTIVE).size());
    }

    @Test
    void searchWithTypeFiltersByClass() {
        register("Toyota", "Corolla", "ABC123");
        Vehicle bike = validVehicle();
        bike.setMake("Yamaha");
        bike.setModel("FZ");
        bike.setLicensePlate("MOTO1");
        bike.setVehicleType(VehicleType.MOTORCYCLE);
        service.registerVehicle(bike);

        assertEquals(1, service.searchVehicles(null, null, null, VehicleType.MOTORCYCLE).size());
        assertEquals(1, service.searchVehicles(null, "Toyota", null, VehicleType.CAR).size());
        assertTrue(service.searchVehicles(null, "Toyota", null, VehicleType.MOTORCYCLE).isEmpty());
    }

    @Test
    void findByCustomerReturnsOnlyThatOwnersVehicles() {
        register("Toyota", "Corolla", "ABC123");
        Vehicle other = validVehicle();
        other.setOwner(createOwner("Luis", "Gómez"));
        other.setLicensePlate("OTR001");
        service.registerVehicle(other);

        List<Vehicle> result = service.findByCustomer(owner.getId());

        assertEquals(1, result.size());
        assertEquals("ABC123", result.get(0).getLicensePlate());
    }

    @Test
    void countByCustomerCountsThatOwnersVehicles() {
        register("Toyota", "Corolla", "ABC123");
        register("Mazda", "CX5", "XYZ789");
        Vehicle other = validVehicle();
        other.setOwner(createOwner("Luis", "Gómez"));
        other.setLicensePlate("OTR001");
        service.registerVehicle(other);

        assertEquals(2, service.countByCustomer(owner.getId()));
        assertEquals(1, service.countByCustomer(other.getOwner().getId()));
        assertEquals(0, service.countByCustomer(999));
    }

    @Test
    void countVehiclesByCustomerGroupsAllOwnersAtOnce() {
        register("Toyota", "Corolla", "ABC123");
        register("Mazda", "CX5", "XYZ789");
        Vehicle other = validVehicle();
        other.setOwner(createOwner("Luis", "Gómez"));
        other.setLicensePlate("OTR001");
        service.registerVehicle(other);

        java.util.Map<Integer, Integer> counts = service.countVehiclesByCustomer();

        assertEquals(2, counts.get(owner.getId()));
        assertEquals(1, counts.get(other.getOwner().getId()));
        assertEquals(null, counts.get(999));
    }

    @Test
    void findMakesReturnsDistinctBrandsOrdered() {
        register("Toyota", "Corolla", "ABC123");
        register("Mazda", "CX5", "XYZ789");
        register("Toyota", "Hilux", "TOY002");

        assertEquals(List.of("Mazda", "Toyota"), service.findMakes());
    }

    @Test
    void findMakesIsEmptyWithoutVehicles() {
        assertTrue(service.findMakes().isEmpty());
    }

    @Test
    void updateMileageChangesTheValue() {
        Vehicle saved = registered();

        service.updateMileage(saved.getId(), 60000);

        assertEquals(60000, service.getDetail(saved.getId()).getMileage());
    }

    @Test
    void updateMileageRejectsADecrease() {
        Vehicle saved = registered();

        ValidationException error = assertThrows(ValidationException.class,
                () -> service.updateMileage(saved.getId(), 40000));

        assertEquals("El kilometraje no puede ser menor al actual", error.getMessage());
    }

    @Test
    void updateMileageThrowsNotFoundForMissingVehicle() {
        assertThrows(NotFoundException.class, () -> service.updateMileage(999, 100));
    }

    @Test
    void deleteVehicleMarksItInactiveAndKeepsTheData() {
        Vehicle saved = registered();

        service.deleteVehicle(saved.getId());

        Vehicle deleted = service.getDetail(saved.getId());
        assertEquals(VehicleStatus.INACTIVE, deleted.getStatus());
        assertEquals("ABC123", deleted.getLicensePlate());
    }

    @Test
    void deleteVehicleThrowsNotFoundForMissingVehicle() {
        assertThrows(NotFoundException.class, () -> service.deleteVehicle(999));
    }
}
