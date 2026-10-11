package com.autogest.model;

import com.autogest.exception.ValidationException;
import java.time.LocalDate;

/**
 * Vehículo de un cliente; es el eje del historial de mantenimiento.
 *
 * <p>El aviso «Mantenimiento próximo» no es un estado guardado: se calcula con
 * {@link #isMaintenanceDue(LocalDate)} (decisión D-13).</p>
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class Vehicle {

    private int id;
    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private String vin;
    private String color;
    private FuelType fuelType;
    private int mileage;
    private VehicleStatus status;
    private Customer owner;
    private VehicleType vehicleType;
    private Transmission transmission;
    private String engineNumber;
    private String registrationCardNumber;
    private String observations;
    private Integer lastMaintenanceMileage;
    private LocalDate lastMaintenanceDate;
    private Integer nextMaintenanceMileage;
    private LocalDate nextMaintenanceDate;

    /**
     * Crea un vehículo vacío, con {@code id} 0 y estado {@link VehicleStatus#ACTIVE}.
     */
    public Vehicle() {
        this.status = VehicleStatus.ACTIVE;
    }

    /** Días de anticipación con los que se avisa que un mantenimiento está próximo. */
    private static final int MAINTENANCE_WARNING_DAYS = 30;

    /**
     * Indica si el vehículo tiene una orden de servicio en curso.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si el estado es {@code IN_SERVICE}
     */
    public boolean isInService() {
        return status == VehicleStatus.IN_SERVICE;
    }

    /**
     * Actualiza el kilometraje validando que no disminuya.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param mileage nuevo kilometraje; puede ser igual al actual
     * @throws ValidationException si es menor al kilometraje actual
     */
    public void updateMileage(int mileage) {
        if (mileage < this.mileage) {
            throw new ValidationException("El kilometraje no puede ser menor al actual");
        }
        this.mileage = mileage;
    }

    /**
     * Devuelve un texto corto del vehículo, por ejemplo {@code Toyota Corolla ABC-123}.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return marca, modelo y placa
     */
    public String getShortDescription() {
        return make + " " + model + " " + licensePlate;
    }

    /**
     * Indica si el próximo mantenimiento ya venció o vence dentro de los próximos
     * {@value #MAINTENANCE_WARNING_DAYS} días (inclusive). Sin fecha próxima devuelve
     * {@code false}.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param today fecha de referencia (se recibe para poder probar con fechas fijas)
     * @return {@code true} si hay que avisar el mantenimiento
     */
    public boolean isMaintenanceDue(LocalDate today) {
        if (nextMaintenanceDate == null) {
            return false;
        }
        return !nextMaintenanceDate.isAfter(today.plusDays(MAINTENANCE_WARNING_DAYS));
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve la marca. */
    public String getMake() {
        return make;
    }

    /** Asigna la marca. */
    public void setMake(String make) {
        this.make = make;
    }

    /** Devuelve el modelo. */
    public String getModel() {
        return model;
    }

    /** Asigna el modelo. */
    public void setModel(String model) {
        this.model = model;
    }

    /** Devuelve el año de fabricación. */
    public int getYear() {
        return year;
    }

    /** Asigna el año de fabricación. */
    public void setYear(int year) {
        this.year = year;
    }

    /** Devuelve la placa (única). */
    public String getLicensePlate() {
        return licensePlate;
    }

    /** Asigna la placa (única). */
    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    /** Devuelve el número de identificación del vehículo (opcional). */
    public String getVin() {
        return vin;
    }

    /** Asigna el número de identificación del vehículo (opcional). */
    public void setVin(String vin) {
        this.vin = vin;
    }

    /** Devuelve el color. */
    public String getColor() {
        return color;
    }

    /** Asigna el color. */
    public void setColor(String color) {
        this.color = color;
    }

    /** Devuelve el tipo de combustible. */
    public FuelType getFuelType() {
        return fuelType;
    }

    /** Asigna el tipo de combustible. */
    public void setFuelType(FuelType fuelType) {
        this.fuelType = fuelType;
    }

    /** Devuelve el kilometraje actual; para cambiarlo con validación use {@link #updateMileage(int)}. */
    public int getMileage() {
        return mileage;
    }

    /** Asigna el kilometraje actual; para cambiarlo con validación use {@link #updateMileage(int)}. */
    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    /** Devuelve el estado del vehículo. */
    public VehicleStatus getStatus() {
        return status;
    }

    /** Asigna el estado del vehículo. */
    public void setStatus(VehicleStatus status) {
        this.status = status;
    }

    /** Devuelve el cliente propietario. */
    public Customer getOwner() {
        return owner;
    }

    /** Asigna el cliente propietario. */
    public void setOwner(Customer owner) {
        this.owner = owner;
    }

    /** Devuelve la clase de vehículo (opcional). */
    public VehicleType getVehicleType() {
        return vehicleType;
    }

    /** Asigna la clase de vehículo (opcional). */
    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    /** Devuelve el tipo de transmisión (opcional). */
    public Transmission getTransmission() {
        return transmission;
    }

    /** Asigna el tipo de transmisión (opcional). */
    public void setTransmission(Transmission transmission) {
        this.transmission = transmission;
    }

    /** Devuelve el número de motor (opcional). */
    public String getEngineNumber() {
        return engineNumber;
    }

    /** Asigna el número de motor (opcional). */
    public void setEngineNumber(String engineNumber) {
        this.engineNumber = engineNumber;
    }

    /** Devuelve el número de la tarjeta de propiedad (opcional). */
    public String getRegistrationCardNumber() {
        return registrationCardNumber;
    }

    /** Asigna el número de la tarjeta de propiedad (opcional). */
    public void setRegistrationCardNumber(String registrationCardNumber) {
        this.registrationCardNumber = registrationCardNumber;
    }

    /** Devuelve las observaciones (opcional). */
    public String getObservations() {
        return observations;
    }

    /** Asigna las observaciones (opcional). */
    public void setObservations(String observations) {
        this.observations = observations;
    }

    /** Devuelve el kilometraje del último mantenimiento (opcional). */
    public Integer getLastMaintenanceMileage() {
        return lastMaintenanceMileage;
    }

    /** Asigna el kilometraje del último mantenimiento (opcional). */
    public void setLastMaintenanceMileage(Integer lastMaintenanceMileage) {
        this.lastMaintenanceMileage = lastMaintenanceMileage;
    }

    /** Devuelve la fecha del último mantenimiento (opcional). */
    public LocalDate getLastMaintenanceDate() {
        return lastMaintenanceDate;
    }

    /** Asigna la fecha del último mantenimiento (opcional). */
    public void setLastMaintenanceDate(LocalDate lastMaintenanceDate) {
        this.lastMaintenanceDate = lastMaintenanceDate;
    }

    /** Devuelve el kilometraje del próximo mantenimiento (opcional). */
    public Integer getNextMaintenanceMileage() {
        return nextMaintenanceMileage;
    }

    /** Asigna el kilometraje del próximo mantenimiento (opcional). */
    public void setNextMaintenanceMileage(Integer nextMaintenanceMileage) {
        this.nextMaintenanceMileage = nextMaintenanceMileage;
    }

    /** Devuelve la fecha del próximo mantenimiento (opcional). */
    public LocalDate getNextMaintenanceDate() {
        return nextMaintenanceDate;
    }

    /** Asigna la fecha del próximo mantenimiento (opcional). */
    public void setNextMaintenanceDate(LocalDate nextMaintenanceDate) {
        this.nextMaintenanceDate = nextMaintenanceDate;
    }
}
