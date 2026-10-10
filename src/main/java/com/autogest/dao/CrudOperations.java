package com.autogest.dao;

import java.util.List;

/**
 * Contrato genérico de acceso a datos para las entidades de AutoGest. Todos los
 * DAO lo implementan para ofrecer las operaciones CRUD con una firma uniforme,
 * de modo que los Repository y los Services trabajen siempre contra el mismo
 * contrato (contrato 7.1 de la guía).
 *
 * <p>{@code findById} devuelve {@code null} cuando el registro no existe (el
 * Service lo traduce a {@code NotFoundException}); {@code delete} es una baja
 * lógica en las entidades que nunca se eliminan (clientes, usuarios, facturas,
 * órdenes), según la decisión D-02.</p>
 *
 * <p>Principles: ISP, DIP, Protected Variations, High Cohesion.</p>
 *
 * @param <T> entidad del modelo que administra el DAO
 */
public interface CrudOperations<T> {

    /**
     * Inserta la entidad y devuelve la misma instancia con el id generado por
     * la base de datos.
     *
     * @param entity entidad a insertar (id igual a 0)
     * @return la entidad con su id asignado
     */
    T create(T entity);

    /**
     * Busca una entidad por su id.
     *
     * @param id identificador de la entidad
     * @return la entidad encontrada o {@code null} si no existe
     */
    T findById(int id);

    /**
     * Devuelve todas las entidades.
     *
     * @return lista con todas las entidades (vacía si no hay ninguna)
     */
    List<T> findAll();

    /**
     * Actualiza la entidad existente.
     *
     * @param entity entidad con el id y los nuevos valores
     * @return {@code true} si se actualizó alguna fila; {@code false} en caso contrario
     */
    boolean update(T entity);

    /**
     * Elimina la entidad. En las entidades que se conservan es una baja lógica
     * (cambio de estado), nunca un borrado físico.
     *
     * @param id identificador de la entidad
     * @return {@code true} si se eliminó (o desactivó) alguna fila; {@code false} en caso contrario
     */
    boolean delete(int id);
}
