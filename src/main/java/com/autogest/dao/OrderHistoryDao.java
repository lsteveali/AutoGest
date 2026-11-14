package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.OrderHistory;
import com.autogest.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL a la tabla {@code order_history} (auditoría de una orden). Como
 * las líneas, el historial se lee y escribe siempre a través de su orden.
 *
 * <p>Principles: Pure Fabrication, Protected Variations, ISP.</p>
 */
public class OrderHistoryDao {

    private static final String INSERT =
            "INSERT INTO order_history (order_id, timestamp, description, user_id) VALUES (?, ?, ?, ?)";

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public OrderHistoryDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /**
     * Agrega una entrada de historial a una orden existente.
     *
     * @param orderId id de la orden
     * @param entry   entrada a guardar
     */
    public void insert(int orderId, OrderHistory entry) {
        try (Connection conn = connection.getConnection()) {
            insert(conn, orderId, entry);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el historial de la orden.", e);
        }
    }

    /** Agrega una entrada usando una conexión abierta (transacciones de {@link ServiceOrderDao}). */
    void insert(Connection conn, int orderId, OrderHistory entry) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, orderId);
            LocalDateTime when = entry.getTimestamp() == null ? LocalDateTime.now() : entry.getTimestamp();
            ps.setTimestamp(2, Timestamp.valueOf(when));
            ps.setString(3, entry.getDescription());
            if (entry.getUser() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, entry.getUser().getId());
            }
            ps.executeUpdate();
        }
    }

    /**
     * Historial de una orden, del más antiguo al más reciente. El usuario se
     * devuelve «mínimo» (solo con su id).
     */
    public List<OrderHistory> findByOrder(int orderId) {
        String sql = "SELECT timestamp, description, user_id FROM order_history WHERE order_id = ? ORDER BY timestamp, id";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderHistory> history = new ArrayList<>();
                while (rs.next()) {
                    OrderHistory entry = new OrderHistory();
                    Timestamp timestamp = rs.getTimestamp("timestamp");
                    entry.setTimestamp(timestamp == null ? null : timestamp.toLocalDateTime());
                    entry.setDescription(rs.getString("description"));
                    int userId = rs.getInt("user_id");
                    if (!rs.wasNull()) {
                        User user = new User();
                        user.setId(userId);
                        entry.setUser(user);
                    }
                    history.add(entry);
                }
                return history;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el historial de la orden.", e);
        }
    }
}
