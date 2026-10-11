package com.autogest.repository;

import com.autogest.dao.UserDao;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.model.UserStatus;

import java.util.List;

/**
 * Acceso a datos organizado de los usuarios. Delega cada operación en
 * {@link UserDao} con los mismos métodos, de modo que los Services dependan de
 * esta clase y no del SQL. Al ser una clase concreta no final, las pruebas la
 * pueden extender con un repositorio falso (subclase) sin base de datos.
 *
 * <p>Principles: Indirection, DIP, Low Coupling, High Cohesion.</p>
 */
public class UserRepository {

    private final UserDao dao;

    /**
     * Crea el repositorio con el DAO de usuarios inyectado.
     *
     * @param dao acceso SQL a la tabla {@code users}
     */
    public UserRepository(UserDao dao) {
        this.dao = dao;
    }

    /**
     * Inserta el usuario y devuelve la misma instancia con el id generado.
     *
     * @param user usuario a insertar
     * @return el usuario con su id asignado
     */
    public User create(User user) {
        return dao.create(user);
    }

    /**
     * Busca un usuario por su id.
     *
     * @param id identificador del usuario
     * @return el usuario encontrado o {@code null} si no existe
     */
    public User findById(int id) {
        return dao.findById(id);
    }

    /**
     * Devuelve todos los usuarios ordenados por nombre.
     *
     * @return lista de usuarios (vacía si no hay ninguno)
     */
    public List<User> findAll() {
        return dao.findAll();
    }

    /**
     * Actualiza el usuario existente.
     *
     * @param user usuario con el id y los nuevos valores
     * @return {@code true} si se actualizó alguna fila
     */
    public boolean update(User user) {
        return dao.update(user);
    }

    /**
     * Da de baja lógica al usuario (lo marca INACTIVE).
     *
     * @param id identificador del usuario
     * @return {@code true} si se desactivó alguna fila
     */
    public boolean delete(int id) {
        return dao.delete(id);
    }

    /**
     * Busca un usuario por su correo, sin distinguir mayúsculas.
     *
     * @param email correo a buscar
     * @return el usuario encontrado o {@code null} si no existe
     */
    public User findByEmail(String email) {
        return dao.findByEmail(email);
    }

    /**
     * Busca un usuario por su nombre de usuario, sin distinguir mayúsculas.
     *
     * @param username nombre de usuario a buscar
     * @return el usuario encontrado o {@code null} si no existe
     */
    public User findByUsername(String username) {
        return dao.findByUsername(username);
    }

    /**
     * Busca usuarios aplicando filtros opcionales.
     *
     * @param text   texto a buscar en nombre, apellido, correo o usuario
     * @param role   rol a filtrar (o {@code null} para todos)
     * @param status estado a filtrar (o {@code null} para todos)
     * @return lista de usuarios que cumplen los filtros
     */
    public List<User> search(String text, Role role, UserStatus status) {
        return dao.search(text, role, status);
    }

    /**
     * Devuelve los usuarios de un rol con un estado concreto.
     *
     * @param role   rol a filtrar
     * @param status estado a filtrar
     * @return lista de usuarios que cumplen ambos criterios
     */
    public List<User> findByRoleAndStatus(Role role, UserStatus status) {
        return dao.findByRoleAndStatus(role, status);
    }

    /**
     * Cuenta todos los usuarios del sistema.
     *
     * @return número total de usuarios
     */
    public int countAll() {
        return dao.countAll();
    }

    /**
     * Cuenta los usuarios que tienen el estado indicado.
     *
     * @param status estado a contar
     * @return número de usuarios con ese estado
     */
    public int countByStatus(UserStatus status) {
        return dao.countByStatus(status);
    }

    /**
     * Cuenta los usuarios que tienen el rol indicado.
     *
     * @param role rol a contar
     * @return número de usuarios con ese rol
     */
    public int countByRole(Role role) {
        return dao.countByRole(role);
    }

    /**
     * Cuenta los usuarios que cumplen a la vez un rol y un estado.
     *
     * @param role   rol a filtrar
     * @param status estado a filtrar
     * @return número de usuarios con ese rol y ese estado
     */
    public int countByRoleAndStatus(Role role, UserStatus status) {
        return dao.countByRoleAndStatus(role, status);
    }

    /**
     * Actualiza la fecha y hora del último acceso del usuario.
     *
     * @param id identificador del usuario
     * @return {@code true} si se actualizó la fila
     */
    public boolean updateLastAccess(int id) {
        return dao.updateLastAccess(id);
    }
}
