package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;
import com.autogest.model.ServiceOrder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Acceso SQL a la tabla {@code notifications}. El cliente se devuelve
 * «mínimo» (solo con su id) y la orden con su id y su número (se lee con un
 * {@code LEFT JOIN} para mostrar «#0145» sin otra consulta).
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion.</p>
 */
public class NotificationDao implements CrudOperations<Notification> {

    private static final String SELECT = "SELECT n.id, n.type, n.channel, n.subject, n.message, n.status,"
            + " n.scheduled_date, n.order_id, n.customer_id, o.number AS order_number"
            + " FROM notifications n LEFT JOIN service_orders o ON o.id = n.order_id";
    private static final String ORDER_BY = " ORDER BY n.scheduled_date DESC NULLS LAST, n.id DESC";

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public NotificationDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    @Override
    public Notification create(Notification entity) {
        String sql = "INSERT INTO notifications (type, channel, subject, message, status, scheduled_date,"
                + " order_id, customer_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
            throw new DataAccessException("No se pudo crear la notificación.", e);
        }
    }

    @Override
    public Notification findById(int id) {
        return query(SELECT + " WHERE n.id = ?", List.of(id)).stream().findFirst().orElse(null);
    }

    @Override
    public List<Notification> findAll() {
        return query(SELECT + ORDER_BY, List.of());
    }

    @Override
    public boolean update(Notification entity) {
        String sql = "UPDATE notifications SET type = ?, channel = ?, subject = ?, message = ?, status = ?,"
                + " scheduled_date = ?, order_id = ?, customer_id = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, entity);
            ps.setInt(9, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la notificación.", e);
        }
    }

    /** Elimina la notificación (no tiene filas que dependan de ella). */
    @Override
    public boolean delete(int id) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM notifications WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar la notificación.", e);
        }
    }

    /**
     * Busca notificaciones por texto (asunto, mensaje o nombre del cliente),
     * tipo, canal y estado; los filtros nulos no se aplican.
     */
    public List<Notification> search(String text, NotificationType type, Channel channel, NotificationStatus status) {
        StringBuilder sql = new StringBuilder(SELECT).append(" JOIN customers c ON c.id = n.customer_id");
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            conditions.add("(n.subject ILIKE ? OR n.message ILIKE ? OR (c.first_name || ' ' || c.last_name) ILIKE ?)");
            String like = "%" + escapeLike(text.trim()) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (type != null) {
            conditions.add("n.type = ?");
            params.add(type.name());
        }
        if (channel != null) {
            conditions.add("n.channel = ?");
            params.add(channel.name());
        }
        if (status != null) {
            conditions.add("n.status = ?");
            params.add(status.name());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(ORDER_BY);
        return query(sql.toString(), params);
    }

    /** Notificaciones de un cliente, de la más reciente a la más antigua. */
    public List<Notification> findByCustomer(int customerId) {
        return query(SELECT + " WHERE n.customer_id = ?" + ORDER_BY, List.of(customerId));
    }

    /** Programadas cuya fecha ya llegó (para enviarlas). */
    public List<Notification> findDueScheduled(LocalDateTime now) {
        return query(SELECT + " WHERE n.status = 'SCHEDULED' AND n.scheduled_date <= ?" + ORDER_BY,
                List.of(Timestamp.valueOf(now)));
    }

    /** Cantidad de notificaciones en un estado. */
    public int countByStatus(NotificationStatus status) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM notifications WHERE status = ?")) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron contar las notificaciones.", e);
        }
    }

    /**
     * Notificaciones por canal (todos los canales, con 0 si no hay). Apoyo
     * para Reportes (Sprint 7).
     */
    public Map<Channel, Integer> countGroupedByChannel() {
        Map<Channel, Integer> result = new EnumMap<>(Channel.class);
        for (Channel channel : Channel.values()) {
            result.put(channel, 0);
        }
        grouped("SELECT channel, COUNT(*) FROM notifications GROUP BY channel",
                (key, count) -> result.put(Channel.valueOf(key), count));
        return result;
    }

    /** Notificaciones por estado (todos los estados, con 0 si no hay). */
    public Map<NotificationStatus, Integer> countGroupedByStatus() {
        Map<NotificationStatus, Integer> result = new EnumMap<>(NotificationStatus.class);
        for (NotificationStatus status : NotificationStatus.values()) {
            result.put(status, 0);
        }
        grouped("SELECT status, COUNT(*) FROM notifications GROUP BY status",
                (key, count) -> result.put(NotificationStatus.valueOf(key), count));
        return result;
    }

    private void grouped(String sql, BiConsumer<String, Integer> consumer) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                consumer.accept(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo resumir las notificaciones.", e);
        }
    }

    private void bind(PreparedStatement ps, Notification entity) throws SQLException {
        ps.setString(1, entity.getType().name());
        ps.setString(2, entity.getChannel().name());
        ps.setString(3, entity.getSubject());
        ps.setString(4, entity.getMessage());
        ps.setString(5, (entity.getStatus() == null ? NotificationStatus.PENDING : entity.getStatus()).name());
        if (entity.getScheduledDate() == null) {
            ps.setNull(6, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(6, Timestamp.valueOf(entity.getScheduledDate()));
        }
        if (entity.getOrder() == null) {
            ps.setNull(7, Types.INTEGER);
        } else {
            ps.setInt(7, entity.getOrder().getId());
        }
        ps.setInt(8, entity.getCustomer().getId());
    }

    private List<Notification> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Notification> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las notificaciones.", e);
        }
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification notification = new Notification();
        notification.setId(rs.getInt("id"));
        notification.setType(NotificationType.valueOf(rs.getString("type")));
        notification.setChannel(Channel.valueOf(rs.getString("channel")));
        notification.setSubject(rs.getString("subject"));
        notification.setMessage(rs.getString("message"));
        notification.setStatus(NotificationStatus.valueOf(rs.getString("status")));
        Timestamp scheduled = rs.getTimestamp("scheduled_date");
        notification.setScheduledDate(scheduled == null ? null : scheduled.toLocalDateTime());
        int orderId = rs.getInt("order_id");
        if (!rs.wasNull()) {
            ServiceOrder order = new ServiceOrder();
            order.setId(orderId);
            order.setNumber(rs.getString("order_number"));
            notification.setOrder(order);
        }
        Customer customer = new Customer();
        customer.setId(rs.getInt("customer_id"));
        notification.setCustomer(customer);
        return notification;
    }
}
