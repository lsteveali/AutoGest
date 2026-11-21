package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.InventoryMovement;
import com.autogest.model.Catalogs.MovementType;
import com.autogest.model.Part;
import com.autogest.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL a la tabla {@code inventory_movements}. Devuelve el repuesto y el
 * usuario como objetos «mínimos» (solo con su id); el Repository completa la
 * relación.
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion.</p>
 */
public class InventoryMovementDao implements CrudOperations<InventoryMovement> {

    private static final String COLUMNS = "id, part_id, movement_type, quantity, timestamp, reason, user_id";

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public InventoryMovementDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    @Override
    public InventoryMovement create(InventoryMovement entity) {
        String sql = "INSERT INTO inventory_movements (part_id, movement_type, quantity, timestamp, reason, user_id)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
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
            throw new DataAccessException("No se pudo registrar el movimiento de inventario.", e);
        }
    }

    @Override
    public InventoryMovement findById(int id) {
        return query("SELECT " + COLUMNS + " FROM inventory_movements WHERE id = ?", List.of(id))
                .stream().findFirst().orElse(null);
    }

    @Override
    public List<InventoryMovement> findAll() {
        return query("SELECT " + COLUMNS + " FROM inventory_movements ORDER BY timestamp DESC, id DESC", List.of());
    }

    /**
     * Corrige el motivo de un movimiento. La cantidad, el tipo y el repuesto no
     * se cambian: un movimiento es un registro de auditoría.
     */
    @Override
    public boolean update(InventoryMovement entity) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE inventory_movements SET reason = ? WHERE id = ?")) {
            ps.setString(1, entity.getReason());
            ps.setInt(2, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el movimiento.", e);
        }
    }

    /** Elimina un movimiento (solo para depuración; el Service no lo expone). */
    @Override
    public boolean delete(int id) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM inventory_movements WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el movimiento.", e);
        }
    }

    /**
     * Movimientos de un repuesto, del más reciente al más antiguo.
     *
     * @param partId id del repuesto
     * @return movimientos del repuesto
     */
    public List<InventoryMovement> findByPart(int partId) {
        return query("SELECT " + COLUMNS + " FROM inventory_movements WHERE part_id = ?"
                + " ORDER BY timestamp DESC, id DESC", List.of(partId));
    }

    /**
     * Últimos movimientos registrados.
     *
     * @param limit cantidad máxima de filas
     * @return movimientos más recientes
     */
    public List<InventoryMovement> findRecent(int limit) {
        return query("SELECT " + COLUMNS + " FROM inventory_movements ORDER BY timestamp DESC, id DESC LIMIT ?",
                List.of(limit));
    }

    /**
     * Suma las cantidades por tipo de movimiento entre dos fechas (incluidas).
     * Apoyo para Reportes (Sprint 7).
     *
     * @param from fecha inicial
     * @param to   fecha final
     * @return cantidad total por tipo (los tipos sin movimientos quedan en 0)
     */
    public Map<MovementType, Integer> sumQuantityByType(LocalDate from, LocalDate to) {
        String sql = "SELECT movement_type, COALESCE(SUM(quantity), 0) FROM inventory_movements"
                + " WHERE timestamp >= ? AND timestamp < ? GROUP BY movement_type";
        Map<MovementType, Integer> result = new EnumMap<>(MovementType.class);
        for (MovementType type : MovementType.values()) {
            result.put(type, 0);
        }
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(from.atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(MovementType.valueOf(rs.getString(1)), rs.getInt(2));
                }
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron resumir los movimientos de inventario.", e);
        }
    }

    private void bind(PreparedStatement ps, InventoryMovement entity) throws SQLException {
        ps.setInt(1, entity.getPart().getId());
        ps.setString(2, entity.getType().name());
        ps.setInt(3, entity.getQuantity());
        LocalDateTime when = entity.getTimestamp() == null ? LocalDateTime.now() : entity.getTimestamp();
        ps.setTimestamp(4, Timestamp.valueOf(when));
        ps.setString(5, entity.getReason());
        if (entity.getUser() == null) {
            ps.setNull(6, Types.INTEGER);
        } else {
            ps.setInt(6, entity.getUser().getId());
        }
    }

    private List<InventoryMovement> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<InventoryMovement> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los movimientos de inventario.", e);
        }
    }

    private InventoryMovement map(ResultSet rs) throws SQLException {
        InventoryMovement movement = new InventoryMovement();
        movement.setId(rs.getInt("id"));
        movement.setType(MovementType.valueOf(rs.getString("movement_type")));
        movement.setQuantity(rs.getInt("quantity"));
        Timestamp timestamp = rs.getTimestamp("timestamp");
        movement.setTimestamp(timestamp == null ? null : timestamp.toLocalDateTime());
        movement.setReason(rs.getString("reason"));
        Part part = new Part();
        part.setId(rs.getInt("part_id"));
        movement.setPart(part);
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            User user = new User();
            user.setId(userId);
            movement.setUser(user);
        }
        return movement;
    }
}
