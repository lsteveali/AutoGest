package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.CustomerStatus;
import com.autogest.model.DocumentType;
import com.autogest.repository.CustomerRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de negocio del módulo de clientes: registro, edición, consulta y baja
 * lógica. Valida los datos antes de guardarlos (documento, nombres, teléfono y
 * correo), evita documentos repetidos y normaliza los textos. No conoce SQL ni
 * JavaFX.
 *
 * <p>Las validaciones viven aquí y no en el controlador ni en el DAO, para que
 * una misma regla sirva a la pantalla y al asistente. El servicio depende de
 * {@link CustomerRepository}, que recibe por el constructor.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP, High Cohesion, Low Coupling.</p>
 */
public class CustomerService {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    private static final String PHONE_PATTERN = "^(\\+57)?\\d{7,15}$";
    private static final String DIGITS_PATTERN = "\\d+";
    private static final String ALPHANUMERIC_PATTERN = "[A-Za-z0-9-]+";
    private static final int MIN_DOCUMENT_LENGTH = 5;
    private static final int MAX_DOCUMENT_LENGTH = 15;

    private final CustomerRepository repository;

    /**
     * Crea el servicio con el repositorio de clientes inyectado.
     *
     * @param repository acceso a los clientes
     */
    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    /**
     * Registra un cliente nuevo. Valida y normaliza sus datos, comprueba que el
     * documento no exista, lo deja {@code ACTIVE} y le pone la fecha de registro
     * de hoy.
     *
     * @param customer datos del cliente a registrar
     * @return el cliente guardado con su id
     * @throws ValidationException si algún dato no cumple las reglas o el documento ya existe
     */
    public Customer registerCustomer(Customer customer) {
        validateAndNormalize(customer, 0);
        customer.setRegistrationDate(LocalDate.now());
        customer.setStatus(CustomerStatus.ACTIVE);
        return repository.create(customer);
    }

    /**
     * Actualiza los datos de un cliente existente. Aplica las mismas validaciones
     * que el registro (el documento puede ser el propio, pero no el de otro
     * cliente) y conserva la fecha de registro y el estado actuales.
     *
     * @param customer cliente con el id y los nuevos valores
     * @return el cliente actualizado
     * @throws NotFoundException   si el cliente no existe
     * @throws ValidationException si algún dato no cumple las reglas o el documento es de otro cliente
     */
    public Customer updateCustomer(Customer customer) {
        requireData(customer);
        Customer current = requireCustomer(customer.getId());
        validateAndNormalize(customer, customer.getId());
        customer.setRegistrationDate(current.getRegistrationDate());
        customer.setStatus(current.getStatus());
        repository.update(customer);
        return customer;
    }

    /**
     * Busca clientes por texto y estado, ordenados por apellidos y nombres.
     *
     * @param text   texto a buscar en nombres, apellidos, documento o correo (o {@code null})
     * @param status estado a filtrar; {@code null} trae todos
     * @return lista de clientes que cumplen los filtros
     */
    public List<Customer> searchCustomers(String text, CustomerStatus status) {
        return repository.search(text, status);
    }

    /**
     * Carga un cliente para su pantalla de detalle.
     *
     * @param id identificador del cliente
     * @return el cliente encontrado
     * @throws NotFoundException si el cliente no existe
     */
    public Customer getDetail(int id) {
        return requireCustomer(id);
    }

    /**
     * Da de baja al cliente cambiando su estado a {@code INACTIVE}; conserva su
     * historial. Si ya estaba inactivo no hace nada.
     *
     * @param id identificador del cliente
     * @throws NotFoundException si el cliente no existe
     */
    public void deactivateCustomer(int id) {
        Customer customer = requireCustomer(id);
        if (customer.isActive()) {
            customer.deactivate();
            repository.update(customer);
        }
    }

    /**
     * Cuenta todos los clientes registrados.
     *
     * @return número total de clientes
     */
    public int countCustomers() {
        return repository.count();
    }

    /**
     * Cuenta los clientes activos.
     *
     * @return número de clientes ACTIVE
     */
    public int countActiveCustomers() {
        return repository.countByStatus(CustomerStatus.ACTIVE);
    }

    /**
     * Cuenta los clientes registrados desde el primer día del mes actual.
     *
     * @return número de clientes nuevos este mes
     */
    public int countNewCustomersThisMonth() {
        return repository.countRegisteredSince(LocalDate.now().withDayOfMonth(1));
    }

    /** Busca el cliente o lanza {@link NotFoundException}. */
    private Customer requireCustomer(int id) {
        Customer customer = repository.findById(id);
        if (customer == null) {
            throw new NotFoundException("Cliente no encontrado");
        }
        return customer;
    }

    /** Exige que lleguen datos del cliente. */
    private void requireData(Customer customer) {
        if (customer == null) {
            throw new ValidationException("Los datos del cliente son obligatorios");
        }
    }

    /**
     * Valida y normaliza todos los campos del cliente. La comprobación de
     * documento repetido va al final para no consultar la base de datos si otro
     * dato ya es inválido.
     *
     * @param customer  cliente a validar
     * @param currentId id del cliente que se edita (0 si es nuevo), para no chocar consigo mismo
     */
    private void validateAndNormalize(Customer customer, int currentId) {
        requireData(customer);
        validateDocument(customer);
        validateNames(customer);
        validatePhone(customer);
        validateEmail(customer);
        customer.setCity(blankToNull(normalizeSpaces(customer.getCity())));
        customer.setAddress(blankToNull(normalizeSpaces(customer.getAddress())));
        customer.setNotes(blankToNull(customer.getNotes() == null ? null : customer.getNotes().trim()));
        checkDocumentIsNotTaken(customer, currentId);
    }

    /** Exige tipo y número de documento válidos; quita los espacios del número. */
    private void validateDocument(Customer customer) {
        DocumentType type = customer.getDocumentType();
        if (type == null) {
            throw new ValidationException("Selecciona el tipo de documento");
        }
        String number = customer.getDocumentNumber() == null
                ? "" : customer.getDocumentNumber().replaceAll("\\s+", "");
        if (number.isEmpty()) {
            throw new ValidationException("El número de documento es obligatorio");
        }
        if (type == DocumentType.CC || type == DocumentType.CE) {
            if (!number.matches(DIGITS_PATTERN)) {
                throw new ValidationException("El documento debe contener solo números");
            }
        } else if (!number.matches(ALPHANUMERIC_PATTERN)) {
            throw new ValidationException("El documento solo puede tener letras, números y guiones");
        }
        if (number.length() < MIN_DOCUMENT_LENGTH || number.length() > MAX_DOCUMENT_LENGTH) {
            throw new ValidationException("El documento debe tener entre 5 y 15 caracteres");
        }
        customer.setDocumentNumber(number);
    }

    /** Exige nombres y apellidos no vacíos y limpia sus espacios. */
    private void validateNames(Customer customer) {
        String firstName = normalizeSpaces(customer.getFirstName());
        String lastName = normalizeSpaces(customer.getLastName());
        if (isBlank(firstName)) {
            throw new ValidationException("Los nombres son obligatorios");
        }
        if (isBlank(lastName)) {
            throw new ValidationException("Los apellidos son obligatorios");
        }
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
    }

    /** Exige un teléfono de 7 a 15 dígitos (con prefijo +57 opcional); lo guarda sin espacios. */
    private void validatePhone(Customer customer) {
        String phone = customer.getPhone() == null ? "" : customer.getPhone().replaceAll("\\s+", "");
        if (phone.isEmpty()) {
            throw new ValidationException("El teléfono es obligatorio");
        }
        if (!phone.matches(PHONE_PATTERN)) {
            throw new ValidationException("El teléfono debe tener entre 7 y 15 dígitos (puede iniciar con +57)");
        }
        customer.setPhone(phone);
    }

    /** Exige un correo con formato válido; lo guarda recortado y en minúsculas. */
    private void validateEmail(Customer customer) {
        String email = customer.getEmail() == null ? "" : customer.getEmail().trim();
        if (email.isEmpty()) {
            throw new ValidationException("El correo electrónico es obligatorio");
        }
        if (!email.matches(EMAIL_PATTERN)) {
            throw new ValidationException("Ingresa un correo electrónico válido");
        }
        customer.setEmail(email.toLowerCase(Locale.ROOT));
    }

    /** Rechaza el documento si pertenece a otro cliente. */
    private void checkDocumentIsNotTaken(Customer customer, int currentId) {
        String number = customer.getDocumentNumber();
        boolean taken = currentId == 0
                ? repository.existsByDocument(number)
                : repository.existsByDocumentExcluding(number, currentId);
        if (taken) {
            throw new ValidationException("Ya existe un cliente con ese documento");
        }
    }

    /** Recorta un texto y reduce los espacios repetidos a uno solo; {@code null} se mantiene. */
    private static String normalizeSpaces(String value) {
        return value == null ? null : value.trim().replaceAll("\\s+", " ");
    }

    /** Convierte un texto vacío en {@code null} (campo opcional sin dato). */
    private static String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    /** Indica si una cadena es nula o solo espacios. */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
