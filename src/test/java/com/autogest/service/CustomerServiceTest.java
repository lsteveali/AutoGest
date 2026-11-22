package com.autogest.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.model.Catalogs.DocumentType;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link CustomerService} con un repositorio falso, sin base de datos.
 */
class CustomerServiceTest {

    private FakeCustomerRepository repository;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        repository = new FakeCustomerRepository();
        service = new CustomerService(repository);
    }

    // ---------- ayudas ----------

    private Customer validCustomer() {
        Customer customer = new Customer();
        customer.setDocumentType(DocumentType.CC);
        customer.setDocumentNumber("1234567890");
        customer.setFirstName("Juan");
        customer.setLastName("Pérez");
        customer.setPhone("3001234567");
        customer.setEmail("juan.perez@example.com");
        customer.setCity("Valledupar");
        return customer;
    }

    private Customer registered(String document, String firstName, String lastName) {
        Customer customer = validCustomer();
        customer.setDocumentNumber(document);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(firstName.toLowerCase() + "@example.com");
        return service.registerCustomer(customer);
    }

    private String errorOf(Customer customer) {
        return assertThrows(ValidationException.class, () -> service.registerCustomer(customer)).getMessage();
    }

    // ---------- registro correcto ----------

    @Test
    void registerSavesActiveCustomerWithTodaysDate() {
        LocalDate before = LocalDate.now();

        Customer saved = service.registerCustomer(validCustomer());

        LocalDate after = LocalDate.now();
        assertTrue(saved.getId() > 0);
        assertEquals(CustomerStatus.ACTIVE, saved.getStatus());
        assertFalse(saved.getRegistrationDate().isBefore(before));
        assertFalse(saved.getRegistrationDate().isAfter(after));
        assertEquals(1, repository.count());
    }

    @Test
    void registerIgnoresStatusAndDateSentByTheCaller() {
        Customer customer = validCustomer();
        customer.setStatus(CustomerStatus.INACTIVE);
        customer.setRegistrationDate(LocalDate.of(2000, 1, 1));

        Customer saved = service.registerCustomer(customer);

        assertEquals(CustomerStatus.ACTIVE, saved.getStatus());
        assertTrue(saved.getRegistrationDate().isAfter(LocalDate.of(2000, 1, 1)));
    }

    @Test
    void registerNormalizesTextFields() {
        Customer customer = validCustomer();
        customer.setDocumentNumber(" 1 234 567 890 ");
        customer.setFirstName("  Juan   Carlos ");
        customer.setLastName(" Pérez   Gómez  ");
        customer.setPhone("+57 300 123 4567");
        customer.setEmail("  Juan.Perez@Example.COM ");
        customer.setCity("  Santa    Marta ");
        customer.setAddress("  Calle  15   # 10-20 ");
        customer.setNotes("  Cliente frecuente  ");

        Customer saved = service.registerCustomer(customer);

        assertEquals("1234567890", saved.getDocumentNumber());
        assertEquals("Juan Carlos", saved.getFirstName());
        assertEquals("Pérez Gómez", saved.getLastName());
        assertEquals("+573001234567", saved.getPhone());
        assertEquals("juan.perez@example.com", saved.getEmail());
        assertEquals("Santa Marta", saved.getCity());
        assertEquals("Calle 15 # 10-20", saved.getAddress());
        assertEquals("Cliente frecuente", saved.getNotes());
    }

    @Test
    void registerTurnsBlankOptionalFieldsIntoNull() {
        Customer customer = validCustomer();
        customer.setCity("   ");
        customer.setAddress("");
        customer.setNotes("  ");

        Customer saved = service.registerCustomer(customer);

        assertNull(saved.getCity());
        assertNull(saved.getAddress());
        assertNull(saved.getNotes());
    }

    // ---------- documento ----------

    @Test
    void registerRejectsDuplicatedDocument() {
        service.registerCustomer(validCustomer());

        Customer second = validCustomer();
        second.setEmail("otro@example.com");

        assertEquals("Ya existe un cliente con ese documento", errorOf(second));
        assertEquals(1, repository.count());
    }

    @Test
    void registerRejectsDuplicatedDocumentEvenWithDifferentSpacing() {
        service.registerCustomer(validCustomer());

        Customer second = validCustomer();
        second.setDocumentNumber("1234 567 890");

        assertEquals("Ya existe un cliente con ese documento", errorOf(second));
    }

    @Test
    void registerRequiresDocumentType() {
        Customer customer = validCustomer();
        customer.setDocumentType(null);

        assertEquals("Selecciona el tipo de documento", errorOf(customer));
    }

    @Test
    void registerRequiresDocumentNumber() {
        Customer blank = validCustomer();
        blank.setDocumentNumber("   ");
        Customer missing = validCustomer();
        missing.setDocumentNumber(null);

        assertEquals("El número de documento es obligatorio", errorOf(blank));
        assertEquals("El número de documento es obligatorio", errorOf(missing));
    }

    @Test
    void identityCardsAndForeignerCardsAcceptOnlyDigits() {
        for (DocumentType type : new DocumentType[] {DocumentType.CC, DocumentType.CE}) {
            Customer withLetters = validCustomer();
            withLetters.setDocumentType(type);
            withLetters.setDocumentNumber("12345AB");
            Customer withDots = validCustomer();
            withDots.setDocumentType(type);
            withDots.setDocumentNumber("1.234.567.890");

            assertEquals("El documento debe contener solo números", errorOf(withLetters), type + " con letras");
            assertEquals("El documento debe contener solo números", errorOf(withDots), type + " con puntos");
        }
    }

    @Test
    void documentLengthMustBeBetweenFiveAndFifteen() {
        Customer tooShort = validCustomer();
        tooShort.setDocumentNumber("1234");
        Customer tooLong = validCustomer();
        tooLong.setDocumentNumber("1234567890123456");

        assertEquals("El documento debe tener entre 5 y 15 caracteres", errorOf(tooShort));
        assertEquals("El documento debe tener entre 5 y 15 caracteres", errorOf(tooLong));
    }

    @Test
    void documentLengthLimitsAreInclusive() {
        Customer fiveDigits = validCustomer();
        fiveDigits.setDocumentNumber("12345");
        Customer fifteenDigits = validCustomer();
        fifteenDigits.setDocumentNumber("123456789012345");
        fifteenDigits.setEmail("quince@example.com");

        assertEquals("12345", service.registerCustomer(fiveDigits).getDocumentNumber());
        assertEquals("123456789012345", service.registerCustomer(fifteenDigits).getDocumentNumber());
    }

    @Test
    void taxIdAndPassportAcceptLettersDigitsAndHyphens() {
        Customer nit = validCustomer();
        nit.setDocumentType(DocumentType.NIT);
        nit.setDocumentNumber("900123456-7");
        Customer passport = validCustomer();
        passport.setDocumentType(DocumentType.PASSPORT);
        passport.setDocumentNumber("AB123456");
        passport.setEmail("pasaporte@example.com");

        assertEquals("900123456-7", service.registerCustomer(nit).getDocumentNumber());
        assertEquals("AB123456", service.registerCustomer(passport).getDocumentNumber());
    }

    @Test
    void taxIdRejectsInvalidCharacters() {
        Customer customer = validCustomer();
        customer.setDocumentType(DocumentType.NIT);
        customer.setDocumentNumber("900.123.456");

        assertEquals("El documento solo puede tener letras, números y guiones", errorOf(customer));
    }

    // ---------- nombres ----------

    @Test
    void registerRequiresFirstAndLastName() {
        Customer noFirst = validCustomer();
        noFirst.setFirstName("   ");
        Customer noLast = validCustomer();
        noLast.setLastName(null);

        assertEquals("Los nombres son obligatorios", errorOf(noFirst));
        assertEquals("Los apellidos son obligatorios", errorOf(noLast));
    }

    @Test
    void registerRejectsAnEmptyForm() {
        assertThrows(ValidationException.class, () -> service.registerCustomer(new Customer()));
        assertEquals(0, repository.count());
    }

    @Test
    void registerRejectsNullCustomer() {
        ValidationException error = assertThrows(ValidationException.class, () -> service.registerCustomer(null));

        assertEquals("Los datos del cliente son obligatorios", error.getMessage());
    }

    // ---------- teléfono ----------

    @Test
    void registerRequiresPhone() {
        Customer blank = validCustomer();
        blank.setPhone("  ");
        Customer missing = validCustomer();
        missing.setPhone(null);

        assertEquals("El teléfono es obligatorio", errorOf(blank));
        assertEquals("El teléfono es obligatorio", errorOf(missing));
    }

    @Test
    void registerRejectsShortPhone() {
        Customer customer = validCustomer();
        customer.setPhone("123456");

        assertEquals("El teléfono debe tener entre 7 y 15 dígitos (puede iniciar con +57)", errorOf(customer));
    }

    @Test
    void registerRejectsLongPhoneAndPhoneWithLetters() {
        Customer tooLong = validCustomer();
        tooLong.setPhone("1234567890123456");
        Customer withLetters = validCustomer();
        withLetters.setPhone("300abc4567");

        assertEquals("El teléfono debe tener entre 7 y 15 dígitos (puede iniciar con +57)", errorOf(tooLong));
        assertEquals("El teléfono debe tener entre 7 y 15 dígitos (puede iniciar con +57)", errorOf(withLetters));
    }

    @Test
    void phoneAcceptsSevenDigitsAndThePlusFiftySevenPrefix() {
        Customer landline = validCustomer();
        landline.setPhone("5851234");
        Customer international = validCustomer();
        international.setDocumentNumber("9876543210");
        international.setPhone("+57 300 123 4567");
        international.setEmail("internacional@example.com");

        assertEquals("5851234", service.registerCustomer(landline).getPhone());
        assertEquals("+573001234567", service.registerCustomer(international).getPhone());
    }

    // ---------- correo ----------

    @Test
    void registerRejectsInvalidEmail() {
        for (String invalid : new String[] {"juan", "juan@", "juan@correo", "@correo.com", "juan perez@correo.com"}) {
            Customer customer = validCustomer();
            customer.setEmail(invalid);

            assertEquals("Ingresa un correo electrónico válido", errorOf(customer), invalid);
        }
    }

    @Test
    void registerRequiresEmail() {
        Customer blank = validCustomer();
        blank.setEmail("  ");
        Customer missing = validCustomer();
        missing.setEmail(null);

        assertEquals("El correo electrónico es obligatorio", errorOf(blank));
        assertEquals("El correo electrónico es obligatorio", errorOf(missing));
    }

    // ---------- actualización ----------

    @Test
    void updateKeepsRegistrationDateAndStatus() {
        Customer saved = service.registerCustomer(validCustomer());
        LocalDate originalDate = LocalDate.of(2024, 2, 10);
        saved.setRegistrationDate(originalDate);
        repository.update(saved);

        Customer edited = validCustomer();
        edited.setId(saved.getId());
        edited.setFirstName("Juan Andrés");
        edited.setStatus(CustomerStatus.INACTIVE);
        edited.setRegistrationDate(LocalDate.of(2030, 1, 1));

        Customer result = service.updateCustomer(edited);

        assertEquals("Juan Andrés", repository.findById(saved.getId()).getFirstName());
        assertEquals(originalDate, result.getRegistrationDate());
        assertEquals(CustomerStatus.ACTIVE, result.getStatus());
    }

    @Test
    void updateAcceptsTheCustomersOwnDocument() {
        Customer saved = service.registerCustomer(validCustomer());

        Customer edited = validCustomer();
        edited.setId(saved.getId());
        edited.setCity("Bogotá");

        assertEquals("Bogotá", service.updateCustomer(edited).getCity());
    }

    @Test
    void updateRejectsTheDocumentOfAnotherCustomer() {
        service.registerCustomer(validCustomer());
        Customer other = registered("1098765432", "María", "Rodríguez");

        Customer edited = validCustomer();
        edited.setId(other.getId());
        edited.setFirstName("María");
        edited.setLastName("Rodríguez");
        edited.setDocumentNumber("1234567890");

        ValidationException error = assertThrows(ValidationException.class, () -> service.updateCustomer(edited));

        assertEquals("Ya existe un cliente con ese documento", error.getMessage());
        assertEquals("1098765432", repository.findById(other.getId()).getDocumentNumber());
    }

    @Test
    void updateValidatesTheData() {
        Customer saved = service.registerCustomer(validCustomer());

        Customer edited = validCustomer();
        edited.setId(saved.getId());
        edited.setEmail("sin-arroba");

        ValidationException error = assertThrows(ValidationException.class, () -> service.updateCustomer(edited));

        assertEquals("Ingresa un correo electrónico válido", error.getMessage());
    }

    @Test
    void updateThrowsNotFoundForMissingCustomer() {
        Customer edited = validCustomer();
        edited.setId(999);

        NotFoundException error = assertThrows(NotFoundException.class, () -> service.updateCustomer(edited));

        assertEquals("Cliente no encontrado", error.getMessage());
    }

    @Test
    void updateRejectsNullCustomer() {
        assertThrows(ValidationException.class, () -> service.updateCustomer(null));
    }

    // ---------- detalle ----------

    @Test
    void getDetailReturnsTheCustomer() {
        Customer saved = service.registerCustomer(validCustomer());

        assertSame(saved, service.getDetail(saved.getId()));
    }

    @Test
    void getDetailThrowsNotFoundForMissingCustomer() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.getDetail(999));

        assertEquals("Cliente no encontrado", error.getMessage());
    }

    // ---------- baja lógica ----------

    @Test
    void deactivateChangesStatusToInactiveAndKeepsTheCustomer() {
        Customer saved = service.registerCustomer(validCustomer());

        service.deactivateCustomer(saved.getId());

        Customer found = repository.findById(saved.getId());
        assertEquals(CustomerStatus.INACTIVE, found.getStatus());
        assertEquals("Juan", found.getFirstName());
        assertEquals(1, repository.count());
    }

    @Test
    void deactivateAnAlreadyInactiveCustomerDoesNothing() {
        Customer saved = service.registerCustomer(validCustomer());
        service.deactivateCustomer(saved.getId());

        service.deactivateCustomer(saved.getId());

        assertEquals(CustomerStatus.INACTIVE, repository.findById(saved.getId()).getStatus());
    }

    @Test
    void deactivateThrowsNotFoundForMissingCustomer() {
        NotFoundException error = assertThrows(NotFoundException.class, () -> service.deactivateCustomer(999));

        assertEquals("Cliente no encontrado", error.getMessage());
    }

    // ---------- búsqueda ----------

    @Test
    void searchFindsByNameDocumentAndEmail() {
        registered("1234567890", "Juan", "Pérez");
        registered("1098765432", "María", "Rodríguez");

        assertEquals(1, service.searchCustomers("rodríguez", null).size());
        assertEquals(1, service.searchCustomers("María Rodríguez", null).size());
        assertEquals(1, service.searchCustomers("123456", null).size());
        assertEquals(1, service.searchCustomers("juan@example", null).size());
        assertTrue(service.searchCustomers("inexistente", null).isEmpty());
    }

    @Test
    void searchWithNullStatusReturnsAllAndFiltersByStatus() {
        Customer first = registered("1234567890", "Juan", "Pérez");
        registered("1098765432", "María", "Rodríguez");
        service.deactivateCustomer(first.getId());

        List<Customer> all = service.searchCustomers(null, null);
        List<Customer> active = service.searchCustomers(null, CustomerStatus.ACTIVE);
        List<Customer> inactive = service.searchCustomers(null, CustomerStatus.INACTIVE);

        assertEquals(2, all.size());
        assertEquals(1, active.size());
        assertEquals("María", active.get(0).getFirstName());
        assertEquals(1, inactive.size());
        assertEquals("Juan", inactive.get(0).getFirstName());
    }

    // ---------- conteos ----------

    @Test
    void countsTotalAndActiveCustomers() {
        Customer first = registered("1234567890", "Juan", "Pérez");
        registered("1098765432", "María", "Rodríguez");
        registered("1112223334", "Carlos", "Gómez");
        service.deactivateCustomer(first.getId());

        assertEquals(3, service.countCustomers());
        assertEquals(2, service.countActiveCustomers());
    }

    @Test
    void countNewCustomersThisMonthAsksFromTheFirstDayOfTheMonth() {
        Customer thisMonth = registered("1234567890", "Juan", "Pérez");
        Customer lastMonth = registered("1098765432", "María", "Rodríguez");
        thisMonth.setRegistrationDate(LocalDate.now().withDayOfMonth(1));
        lastMonth.setRegistrationDate(LocalDate.now().withDayOfMonth(1).minusDays(1));

        int count = service.countNewCustomersThisMonth();

        assertEquals(LocalDate.now().withDayOfMonth(1), repository.lastRegisteredSinceArgument());
        assertEquals(1, count);
    }
}
