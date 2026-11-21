package com.autogest.repository;

import com.autogest.dao.CustomerDao;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.CustomerStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Acceso a datos organizado de los clientes. Delega cada operación en
 * {@link CustomerDao} con los mismos métodos, de modo que {@code CustomerService}
 * dependa de esta clase y no del SQL. Al ser una clase concreta no final, las
 * pruebas la pueden extender con un repositorio falso (subclase) sin base de
 * datos.
 *
 * <p>Principles: Indirection, DIP, Low Coupling, High Cohesion.</p>
 */
public class CustomerRepository {

    private final CustomerDao dao;

    /**
     * Crea el repositorio con el DAO de clientes inyectado.
     *
     * @param dao acceso SQL a la tabla {@code customers}
     */
    public CustomerRepository(CustomerDao dao) {
        this.dao = dao;
    }

    /**
     * Inserta el cliente y devuelve la misma instancia con el id generado.
     *
     * @param customer cliente a insertar
     * @return el cliente con su id asignado
     */
    public Customer create(Customer customer) {
        return dao.create(customer);
    }

    /**
     * Busca un cliente por su id.
     *
     * @param id identificador del cliente
     * @return el cliente encontrado o {@code null} si no existe
     */
    public Customer findById(int id) {
        return dao.findById(id);
    }

    /**
     * Devuelve todos los clientes ordenados por apellidos y nombres.
     *
     * @return lista de clientes (vacía si no hay ninguno)
     */
    public List<Customer> findAll() {
        return dao.findAll();
    }

    /**
     * Actualiza el cliente existente.
     *
     * @param customer cliente con el id y los nuevos valores
     * @return {@code true} si se actualizó alguna fila
     */
    public boolean update(Customer customer) {
        return dao.update(customer);
    }

    /**
     * Da de baja lógica al cliente (lo marca INACTIVE).
     *
     * @param id identificador del cliente
     * @return {@code true} si se desactivó alguna fila
     */
    public boolean delete(int id) {
        return dao.delete(id);
    }

    /**
     * Busca clientes por texto y estado, ordenados por apellidos y nombres.
     *
     * @param text   texto a buscar en nombres, apellidos, documento o correo (o {@code null})
     * @param status estado a filtrar (o {@code null} para todos)
     * @return lista de clientes que cumplen los filtros
     */
    public List<Customer> search(String text, CustomerStatus status) {
        return dao.search(text, status);
    }

    /**
     * Indica si ya existe un cliente con ese número de documento.
     *
     * @param number número de documento ya normalizado
     * @return {@code true} si hay un cliente con ese número
     */
    public boolean existsByDocument(String number) {
        return dao.existsByDocument(number);
    }

    /**
     * Indica si otro cliente (distinto del indicado) tiene ese número de documento.
     *
     * @param number número de documento ya normalizado
     * @param id     identificador del cliente que se excluye de la búsqueda
     * @return {@code true} si otro cliente tiene ese número
     */
    public boolean existsByDocumentExcluding(String number, int id) {
        return dao.existsByDocumentExcluding(number, id);
    }

    /**
     * Cuenta todos los clientes.
     *
     * @return número total de clientes
     */
    public int count() {
        return dao.count();
    }

    /**
     * Cuenta los clientes que tienen el estado indicado.
     *
     * @param status estado a contar
     * @return número de clientes con ese estado
     */
    public int countByStatus(CustomerStatus status) {
        return dao.countByStatus(status);
    }

    /**
     * Cuenta los clientes registrados desde una fecha (inclusive).
     *
     * @param date fecha inicial
     * @return número de clientes con fecha de registro igual o posterior
     */
    public int countRegisteredSince(LocalDate date) {
        return dao.countRegisteredSince(date);
    }
}
