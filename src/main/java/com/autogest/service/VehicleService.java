package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.CustomerStatus;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleStatus;
import com.autogest.model.VehicleType;
import com.autogest.repository.CustomerRepository;
import com.autogest.repository.VehicleRepository;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Locale;

/**
 * Reglas de negocio del módulo de vehículos: registro, edición, consulta y baja
 * lógica. Valida los datos antes de guardarlos (marca, modelo, año, placa,
 * combustible, propietario y kilometraje), evita placas repetidas y normaliza
 * los textos. También resuelve el propietario para las pantallas. No conoce SQL
 * ni JavaFX.
 *
 * <p>Las validaciones viven aquí y no en el controlador ni en el DAO, para que
 * una misma regla sirva a la pantalla y al asistente. El servicio depende de
 * {@link VehicleRepository} y de {@link CustomerRepository} (para validar y
 * resolver al propietario), que recibe por el constructor.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP, High Cohesion, Low Coupling.</p>
 */
public class VehicleService {

    private static final String PLATE_PATTERN = "[A-Z0-9-]+";
    private static final String VIN_PATTERN = "[A-Z0-9]+";
    private static final int MIN_PLATE_LENGTH = 5;
    private static final int MAX_PLATE_LENGTH = 10;
    private static final int MIN_VIN_LENGTH = 11;
    private static final int MAX_VIN_LENGTH = 17;
    private static final int MIN_YEAR = 1900;

    /** Días de anticipación del aviso de mantenimiento, igual que {@code Vehicle}. */
    private static final int MAINTENANCE_WARNING_DAYS = 30;

    private final VehicleRepository repository;
    private final CustomerRepository customerRepository;

    /**
     * Crea el servicio con los repositorios inyectados.
     *
     * @param repository         acceso a los vehículos
     * @param customerRepository acceso a los clientes (propietarios)
     */
    public VehicleService(VehicleRepository repository, CustomerRepository customerRepository) {
        this.repository = repository;
        this.customerRepository = customerRepository;
    }

    /**
     * Registra un vehículo nuevo. Valida y normaliza sus datos, comprueba que la
     * placa no exista y lo deja {@code ACTIVE}.
     *
     * @param vehicle datos del vehículo a registrar
     * @return el vehículo guardado con su id
     * @throws ValidationException si algún dato no cumple las reglas o la placa ya existe
     */
    public Vehicle registerVehicle(Vehicle vehicle) {
        validateAndNormalize(vehicle, 0);
        vehicle.setStatus(VehicleStatus.ACTIVE);
        return repository.create(vehicle);
    }

    /**
     * Actualiza los datos de un vehículo existente. Aplica las mismas
     * validaciones que el registro (la placa puede ser la propia, pero no la de
     * otro vehículo) y conserva el estado actual.
     *
     * @param vehicle vehículo con el id y los nuevos valores
     * @return el vehículo actualizado
     * @throws NotFoundException   si el vehículo no existe
     * @throws ValidationException si algún dato no cumple las reglas o la placa es de otro vehículo
     */
    public Vehicle updateVehicle(Vehicle vehicle) {
        requireData(vehicle);
        Vehicle current = requireVehicle(vehicle.getId());
        validateAndNormalize(vehicle, vehicle.getId());
        vehicle.setStatus(current.getStatus());
        repository.update(vehicle);
        return vehicle;
    }

    /**
     * Busca vehículos por texto, estado y clase, ordenados por marca y modelo.
     *
     * @param text   texto a buscar en marca, modelo, placa, VIN o propietario (o {@code null})
     * @param status estado a filtrar; {@code null} trae todos
     * @param type   clase de vehículo a filtrar; {@code null} trae todas
     * @return lista de vehículos que cumplen los filtros, con el propietario resuelto
     */
    public List<Vehicle> searchVehicles(String text, VehicleStatus status, VehicleType type) {
        List<Vehicle> vehicles = repository.search(text, status, type);
        for (Vehicle vehicle : vehicles) {
            resolveOwner(vehicle);
        }
        return vehicles;
    }

    /**
     * Carga un vehículo para su pantalla de detalle, con el propietario resuelto.
     *
     * @param id identificador del vehículo
     * @return el vehículo encontrado
     * @throws NotFoundException si el vehículo no existe
     */
    public Vehicle getDetail(int id) {
        Vehicle vehicle = requireVehicle(id);
        resolveOwner(vehicle);
        return vehicle;
    }

    /**
     * Cambia el estado de un vehículo (por ejemplo a {@code IN_SERVICE} cuando
     * entra a una orden). Conserva el resto de sus datos.
     *
     * @param id     identificador del vehículo
     * @param status nuevo estado
     * @throws NotFoundException   si el vehículo no existe
     * @throws ValidationException si el estado es nulo
     */
    public void changeStatus(int id, VehicleStatus status) {
        if (status == null) {
            throw new ValidationException("Selecciona el estado del vehículo");
        }
        Vehicle vehicle = requireVehicle(id);
        vehicle.setStatus(status);
        repository.update(vehicle);
    }

    /**
     * Da de baja al vehículo cambiando su estado a {@code INACTIVE}; conserva su
     * historial. Si ya estaba inactivo no hace nada.
     *
     * @param id identificador del vehículo
     * @throws NotFoundException si el vehículo no existe
     */
    public void deactivateVehicle(int id) {
        Vehicle vehicle = requireVehicle(id);
        if (vehicle.getStatus() != VehicleStatus.INACTIVE) {
            vehicle.setStatus(VehicleStatus.INACTIVE);
            repository.update(vehicle);
        }
    }

    /**
     * Cuenta todos los vehículos registrados.
     *
     * @return número total de vehículos
     */
    public int countVehicles() {
        return repository.count();
    }

    /**
     * Cuenta los vehículos activos.
     *
     * @return número de vehículos ACTIVE
     */
    public int countActiveVehicles() {
        return repository.countByStatus(VehicleStatus.ACTIVE);
    }

    /**
     * Cuenta los vehículos que están en el taller (con orden en curso).
     *
     * @return número de vehículos IN_SERVICE
     */
    public int countInServiceVehicles() {
        return repository.countByStatus(VehicleStatus.IN_SERVICE);
    }

    /**
     * Cuenta los vehículos cuyo próximo mantenimiento ya venció o vence en los
     * próximos {@value #MAINTENANCE_WARNING_DAYS} días.
     *
     * @return número de vehículos con mantenimiento próximo o vencido
     */
    public int countMaintenanceDue() {
        return repository.countMaintenanceDue(LocalDate.now().plusDays(MAINTENANCE_WARNING_DAYS));
    }

    /**
     * Devuelve los clientes activos para el desplegable de propietario.
     *
     * @return lista de clientes activos
     */
    public List<Customer> listActiveOwners() {
        return customerRepository.search(null, CustomerStatus.ACTIVE);
    }

    /** Busca el vehículo o lanza {@link NotFoundException}. */
    private Vehicle requireVehicle(int id) {
        Vehicle vehicle = repository.findById(id);
        if (vehicle == null) {
            throw new NotFoundException("Vehículo no encontrado");
        }
        return vehicle;
    }

    /** Exige que lleguen datos del vehículo. */
    private void requireData(Vehicle vehicle) {
        if (vehicle == null) {
            throw new ValidationException("Los datos del vehículo son obligatorios");
        }
    }

    /** Completa los datos del propietario del vehículo consultando el repositorio. */
    private void resolveOwner(Vehicle vehicle) {
        Customer owner = vehicle.getOwner();
        if (owner == null || owner.getId() <= 0) {
            return;
        }
        Customer full = customerRepository.findById(owner.getId());
        if (full != null) {
            vehicle.setOwner(full);
        }
    }

    /**
     * Valida y normaliza todos los campos del vehículo. La comprobación de placa
     * repetida va al final para no consultar la base de datos si otro dato ya es
     * inválido.
     *
     * @param vehicle   vehículo a validar
     * @param currentId id del vehículo que se edita (0 si es nuevo), para no chocar consigo mismo
     */
    private void validateAndNormalize(Vehicle vehicle, int currentId) {
        requireData(vehicle);
        validateOwner(vehicle);
        validateMakeAndModel(vehicle);
        validateYear(vehicle);
        validatePlate(vehicle);
        validateFuelType(vehicle);
        validateMileage(vehicle);
        vehicle.setVin(normalizeVin(vehicle.getVin()));
        vehicle.setColor(blankToNull(normalizeSpaces(vehicle.getColor())));
        vehicle.setEngineNumber(blankToNull(normalizeSpaces(vehicle.getEngineNumber())));
        vehicle.setRegistrationCardNumber(blankToNull(normalizeSpaces(vehicle.getRegistrationCardNumber())));
        vehicle.setObservations(blankToNull(vehicle.getObservations() == null ? null : vehicle.getObservations().trim()));
        checkPlateIsNotTaken(vehicle, currentId);
    }

    /** Exige un propietario existente. */
    private void validateOwner(Vehicle vehicle) {
        Customer owner = vehicle.getOwner();
        if (owner == null || owner.getId() <= 0) {
            throw new ValidationException("Selecciona el propietario del vehículo");
        }
        if (customerRepository.findById(owner.getId()) == null) {
            throw new ValidationException("El propietario seleccionado no existe");
        }
    }

    /** Exige marca y modelo no vacíos y limpia sus espacios. */
    private void validateMakeAndModel(Vehicle vehicle) {
        String make = normalizeSpaces(vehicle.getMake());
        String model = normalizeSpaces(vehicle.getModel());
        if (isBlank(make)) {
            throw new ValidationException("La marca es obligatoria");
        }
        if (isBlank(model)) {
            throw new ValidationException("El modelo es obligatorio");
        }
        vehicle.setMake(make);
        vehicle.setModel(model);
    }

    /** Exige un año entre 1900 y el próximo año. */
    private void validateYear(Vehicle vehicle) {
        int maxYear = Year.now().getValue() + 1;
        if (vehicle.getYear() < MIN_YEAR || vehicle.getYear() > maxYear) {
            throw new ValidationException("El año debe estar entre " + MIN_YEAR + " y " + maxYear);
        }
    }

    /** Exige una placa válida; la guarda en mayúsculas y sin espacios. */
    private void validatePlate(Vehicle vehicle) {
        String plate = vehicle.getLicensePlate() == null
                ? "" : vehicle.getLicensePlate().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (plate.isEmpty()) {
            throw new ValidationException("La placa es obligatoria");
        }
        if (!plate.matches(PLATE_PATTERN)) {
            throw new ValidationException("La placa solo puede tener letras, números y guiones");
        }
        if (plate.length() < MIN_PLATE_LENGTH || plate.length() > MAX_PLATE_LENGTH) {
            throw new ValidationException("La placa debe tener entre " + MIN_PLATE_LENGTH + " y "
                    + MAX_PLATE_LENGTH + " caracteres");
        }
        vehicle.setLicensePlate(plate);
    }

    /** Exige el tipo de combustible. */
    private void validateFuelType(Vehicle vehicle) {
        if (vehicle.getFuelType() == null) {
            throw new ValidationException("Selecciona el tipo de combustible");
        }
    }

    /** Exige un kilometraje no negativo. */
    private void validateMileage(Vehicle vehicle) {
        if (vehicle.getMileage() < 0) {
            throw new ValidationException("El kilometraje no puede ser negativo");
        }
    }

    /** Normaliza el VIN opcional; si viene, exige 11 a 17 caracteres alfanuméricos. */
    private String normalizeVin(String vin) {
        if (isBlank(vin)) {
            return null;
        }
        String normalized = vin.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        if (!normalized.matches(VIN_PATTERN)
                || normalized.length() < MIN_VIN_LENGTH || normalized.length() > MAX_VIN_LENGTH) {
            throw new ValidationException("El VIN debe tener entre " + MIN_VIN_LENGTH + " y "
                    + MAX_VIN_LENGTH + " caracteres alfanuméricos");
        }
        return normalized;
    }

    /** Rechaza la placa si pertenece a otro vehículo. */
    private void checkPlateIsNotTaken(Vehicle vehicle, int currentId) {
        String plate = vehicle.getLicensePlate();
        boolean taken = currentId == 0
                ? repository.existsByLicensePlate(plate)
                : repository.existsByLicensePlateExcluding(plate, currentId);
        if (taken) {
            throw new ValidationException("Ya existe un vehículo con esa placa");
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
