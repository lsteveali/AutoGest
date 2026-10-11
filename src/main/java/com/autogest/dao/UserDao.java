package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.model.UserStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL a la tabla {@code users}. Implementa {@link CrudOperations} para
 * las operaciones genéricas y agrega las consultas propias del módulo de
 * usuarios (búsqueda por correo o nombre de usuario, búsqueda con filtros y
 * conteos para los indicadores).
 *
 * <p>Todo el SQL se ejecuta con {@link PreparedStatement} para evitar inyección
 * y usa los nombres de columna en inglés definidos en {@code schema.sql}. El rol
 * y el estado se guardan y leen como texto con el {@code name()} del enum. La
 * eliminación es lógica (cambio de estado), según la decisión D-02.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion,
 * Low Coupling.</p>
 */
public class UserDao implements CrudOperations<User> {

    /** Columnas de la tabla en el orden usado por el mapeo y las consultas. */
    private static final String COLUMNS =
            "id, first_name, last_name, email, username, password_hash, role, status, specialty, last_access";

    private final DatabaseConnection connection;

    /**
     * Crea el DAO con la fábrica de conexiones inyectada.
     *
     * @param connection fábrica de conexiones JDBC de AutoGest
     */
    public UserDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /** {@inheritDoc} */
    @Override
    public User create(User entity) {
        String sql = "INSERT INTO users (first_name, last_name, email, username, password_hash, "
                + "role, status, specialty, last_access) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setFields(ps, entity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo crear el usuario.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public User findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el usuario.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<User> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY first_name, last_name";
        return query(sql, List.of());
    }

    /** {@inheritDoc} */
    @Override
    public boolean update(User entity) {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, username = ?, "
                + "password_hash = ?, role = ?, status = ?, specialty = ?, last_access = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setFields(ps, entity);
            ps.setInt(10, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el usuario.", e);
        }
    }

    /**
     * Actualiza la fecha y hora del último acceso del usuario (lo llama
     * {@code AuthenticationService} tras un inicio de sesión correcto).
     *
     * @param id identificador del usuario
     * @return {@code true} si se actualizó la fila; {@code false} si el id no existe
     */
    public boolean updateLastAccess(int id) {
        String sql = "UPDATE users SET last_access = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo registrar el último acceso.", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Baja lógica (D-02): no se borra la fila, se marca el usuario como
     * {@link UserStatus#INACTIVE} para conservar el historial.</p>
     */
    @Override
    public boolean delete(int id) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UserStatus.INACTIVE.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo desactivar el usuario.", e);
        }
    }

    /**
     * Busca un usuario por su correo, sin distinguir mayúsculas.
     *
     * @param email correo a buscar
     * @return el usuario encontrado o {@code null} si no existe
     */
    public User findByEmail(String email) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE lower(email) = lower(?)";
        return query(sql, List.of(email)).stream().findFirst().orElse(null);
    }

    /**
     * Busca un usuario por su nombre de usuario, sin distinguir mayúsculas.
     *
     * @param username nombre de usuario a buscar
     * @return el usuario encontrado o {@code null} si no existe
     */
    public User findByUsername(String username) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE lower(username) = lower(?)";
        return query(sql, List.of(username)).stream().findFirst().orElse(null);
    }

    /**
     * Busca usuarios aplicando filtros opcionales. Un filtro {@code null} o en
     * blanco no se aplica.
     *
     * @param text   texto a buscar en nombre, apellido, correo o usuario
     * @param role   rol a filtrar (o {@code null} para todos)
     * @param status estado a filtrar (o {@code null} para todos)
     * @return la lista de usuarios que cumplen los filtros
     */
    public List<User> search(String text, Role role, UserStatus status) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM users");
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (text != null && !text.isBlank()) {
            conditions.add("(lower(first_name) LIKE ? OR lower(last_name) LIKE ? "
                    + "OR lower(email) LIKE ? OR lower(username) LIKE ?)");
            String like = "%" + text.trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (role != null) {
            conditions.add("role = ?");
            params.add(role.name());
        }
        if (status != null) {
            conditions.add("status = ?");
            params.add(status.name());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY first_name, last_name");
        return query(sql.toString(), params);
    }

    /**
     * Devuelve los usuarios de un rol con un estado concreto.
     *
     * @param role   rol a filtrar
     * @param status estado a filtrar
     * @return la lista de usuarios que cumplen ambos criterios
     */
    public List<User> findByRoleAndStatus(Role role, UserStatus status) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE role = ? AND status = ? "
                + "ORDER BY first_name, last_name";
        return query(sql, List.of(role.name(), status.name()));
    }

    /**
     * Cuenta todos los usuarios del sistema.
     *
     * @return número total de usuarios
     */
    public int countAll() {
        return count("SELECT COUNT(*) FROM users", List.of());
    }

    /**
     * Cuenta los usuarios que tienen el estado indicado.
     *
     * @param status estado a contar
     * @return número de usuarios con ese estado
     */
    public int countByStatus(UserStatus status) {
        return count("SELECT COUNT(*) FROM users WHERE status = ?", List.of(status.name()));
    }

    /**
     * Cuenta los usuarios que tienen el rol indicado.
     *
     * @param role rol a contar
     * @return número de usuarios con ese rol
     */
    public int countByRole(Role role) {
        return count("SELECT COUNT(*) FROM users WHERE role = ?", List.of(role.name()));
    }

    /**
     * Cuenta los usuarios que cumplen a la vez un rol y un estado.
     *
     * @param role   rol a filtrar
     * @param status estado a filtrar
     * @return número de usuarios con ese rol y ese estado
     */
    public int countByRoleAndStatus(Role role, UserStatus status) {
        return count("SELECT COUNT(*) FROM users WHERE role = ? AND status = ?",
                List.of(role.name(), status.name()));
    }

    /** Asigna los campos del usuario a los nueve primeros parámetros. */
    private void setFields(PreparedStatement ps, User user) throws SQLException {
        ps.setString(1, user.getFirstName());
        ps.setString(2, user.getLastName());
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getUsername());
        ps.setString(5, user.getPasswordHash());
        ps.setString(6, user.getRole() == null ? null : user.getRole().name());
        ps.setString(7, user.getStatus() == null ? null : user.getStatus().name());
        ps.setString(8, user.getSpecialty());
        if (user.getLastAccess() != null) {
            ps.setTimestamp(9, Timestamp.valueOf(user.getLastAccess()));
        } else {
            ps.setNull(9, Types.TIMESTAMP);
        }
    }

    /** Ejecuta una consulta de usuarios y mapea cada fila. */
    private List<User> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    users.add(map(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los usuarios.", e);
        }
    }

    /** Ejecuta una consulta de conteo y devuelve el valor de la primera columna. */
    private int count(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar los usuarios.", e);
        }
    }

    /** Asigna la lista de parámetros a la sentencia. */
    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /** Convierte una fila de {@code users} en un {@link User}. */
    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.valueOf(rs.getString("role")));
        user.setStatus(UserStatus.valueOf(rs.getString("status")));
        user.setSpecialty(rs.getString("specialty"));
        Timestamp lastAccess = rs.getTimestamp("last_access");
        user.setLastAccess(lastAccess == null ? null : lastAccess.toLocalDateTime());
        return user;
    }
}
