package com.autogest.repository;

import com.autogest.dao.VehicleDao;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleStatus;
import com.autogest.model.VehicleType;

import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a datos organizado de los vehículos. Delega cada operación en
 * {@link VehicleDao} con los mismos métodos, de modo que {@code VehicleService}
 * dependa de esta clase y no del SQL. Al ser una clase concreta no final, las
 * pruebas la pueden extender con un repositorio falso (subclase) sin base de
 * datos.
 *
 * <p>Principles: Indirection, DIP, Low Coupling, High Cohesion.</p>
 */
public class VehicleRepository {

    private final VehicleDao dao;

    /**
     * Crea el repositorio con el DAO de vehículos inyectado.
     *
     * @param dao acceso SQL a la tabla {@code vehicles}
     */
    public VehicleRepository(VehicleDao dao) {
        this.dao = dao;
    }

    /**
     * Inserta el vehículo y devuelve la misma instancia con el id generado.
     *
     * @param vehicle vehículo a insertar
     * @return el vehículo con su id asignado
     */
    public Vehicle create(Vehicle vehicle) {
        return dao.create(vehicle);
    }

    /**
     * Busca un vehículo por su id.
     *
     * @param id identificador del vehículo
     * @return el vehículo encontrado o {@code null} si no existe
     */
    public Vehicle findById(int id) {
        return dao.findById(id);
    }

    /**
     * Devuelve todos los vehículos ordenados por marca y modelo.
     *
     * @return lista de vehículos (vacía si no hay ninguno)
     */
    public List<Vehicle> findAll() {
        return dao.findAll();
    }

    /**
     * Actualiza el vehículo existente.
     *
     * @param vehicle vehículo con el id y los nuevos valores
     * @return {@code true} si se actualizó alguna fila
     */
    public boolean update(Vehicle vehicle) {
        return dao.update(vehicle);
    }

    /**
     * Da de baja lógica al vehículo (lo marca INACTIVE).
     *
     * @param id identificador del vehículo
     * @return {@code true} si se desactivó alguna fila
     */
    public boolean delete(int id) {
        return dao.delete(id);
    }

    /**
     * Busca vehículos por texto, estado y clase, ordenados por marca y modelo.
     *
     * @param text   texto a buscar en marca, modelo, placa, VIN o propietario (o {@code null})
     * @param status estado a filtrar (o {@code null} para todos)
     * @param type   clase de vehículo a filtrar (o {@code null} para todas)
     * @return lista de vehículos que cumplen los filtros
     */
    public List<Vehicle> search(String text, VehicleStatus status, VehicleType type) {
        return dao.search(text, status, type);
    }

    /**
     * Devuelve los vehículos de un propietario.
     *
     * @param customerId identificador del cliente propietario
     * @return lista de vehículos del propietario
     */
    public List<Vehicle> findByCustomer(int customerId) {
        return dao.findByCustomer(customerId);
    }

    /**
     * Indica si ya existe un vehículo con esa placa.
     *
     * @param plate placa ya normalizada
     * @return {@code true} si hay un vehículo con esa placa
     */
    public boolean existsByLicensePlate(String plate) {
        return dao.existsByLicensePlate(plate);
    }

    /**
     * Indica si otro vehículo (distinto del indicado) tiene esa placa.
     *
     * @param plate placa ya normalizada
     * @param id    identificador del vehículo que se excluye de la búsqueda
     * @return {@code true} si otro vehículo tiene esa placa
     */
    public boolean existsByLicensePlateExcluding(String plate, int id) {
        return dao.existsByLicensePlateExcluding(plate, id);
    }

    /**
     * Cuenta todos los vehículos.
     *
     * @return número total de vehículos
     */
    public int count() {
        return dao.count();
    }

    /**
     * Cuenta los vehículos que tienen el estado indicado.
     *
     * @param status estado a contar
     * @return número de vehículos con ese estado
     */
    public int countByStatus(VehicleStatus status) {
        return dao.countByStatus(status);
    }

    /**
     * Cuenta los vehículos de la clase indicada.
     *
     * @param type clase a contar
     * @return número de vehículos de esa clase
     */
    public int countByType(VehicleType type) {
        return dao.countByType(type);
    }

    /**
     * Cuenta los vehículos con mantenimiento próximo o vencido hasta la fecha.
     *
     * @param date fecha límite del aviso
     * @return número de vehículos con mantenimiento próximo o vencido
     */
    public int countMaintenanceDue(LocalDate date) {
        return dao.countMaintenanceDue(date);
    }
}
