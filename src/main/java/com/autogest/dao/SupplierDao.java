package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Supplier;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL a la tabla {@code suppliers} (proveedores de repuestos). Solo
 * ejecuta consultas con {@link PreparedStatement} y convierte filas en
 * {@link Supplier}; no contiene reglas de negocio.
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion.</p>
 */
public class SupplierDao implements CrudOperations<Supplier> {

    private static final String COLUMNS = "id, name, phone, email";

    private final DatabaseConnection connection;

    /**
     * Crea el DAO con la fábrica de conexiones compartida.
     *
     * @param connection fábrica de conexiones a Supabase
     */
    public SupplierDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    @Override
    public Supplier create(Supplier entity) {
        String sql = "INSERT INTO suppliers (name, phone, email) VALUES (?, ?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, entity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo crear el proveedor.", e);
        }
    }

    @Override
    public Supplier findById(int id) {
        return query("SELECT " + COLUMNS + " FROM suppliers WHERE id = ?", List.of(id))
                .stream().findFirst().orElse(null);
    }

    @Override
    public List<Supplier> findAll() {
        return query("SELECT " + COLUMNS + " FROM suppliers ORDER BY name", List.of());
    }

    @Override
    public boolean update(Supplier entity) {
        String sql = "UPDATE suppliers SET name = ?, phone = ?, email = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, entity);
            ps.setInt(4, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el proveedor.", e);
        }
    }

    /**
     * Elimina el proveedor. La llave foránea {@code ON DELETE RESTRICT} de
     * {@code parts} impide borrar un proveedor que tenga repuestos asociados.
     */
    @Override
    public boolean delete(int id) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM suppliers WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el proveedor (puede tener repuestos asociados).", e);
        }
    }

    /**
     * Indica si ya existe un proveedor con el nombre dado (sin distinguir mayúsculas).
     *
     * @param name      nombre a buscar
     * @param excludeId id que se ignora (0 para no ignorar ninguno)
     * @return {@code true} si el nombre ya está registrado
     */
    public boolean existsByName(String name, int excludeId) {
        String sql = "SELECT COUNT(*) FROM suppliers WHERE LOWER(name) = LOWER(?) AND id <> ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo validar el nombre del proveedor.", e);
        }
    }

    private void bind(PreparedStatement ps, Supplier entity) throws SQLException {
        ps.setString(1, entity.getName());
        ps.setString(2, entity.getPhone());
        ps.setString(3, entity.getEmail());
    }

    private List<Supplier> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Supplier> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los proveedores.", e);
        }
    }

    private Supplier map(ResultSet rs) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setId(rs.getInt("id"));
        supplier.setName(rs.getString("name"));
        supplier.setPhone(rs.getString("phone"));
        supplier.setEmail(rs.getString("email"));
        return supplier;
    }
}
