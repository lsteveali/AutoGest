package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.OrderHistory;
import com.autogest.model.OrderLine;
import com.autogest.model.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Vehicle;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL a la tabla {@code service_orders} (cabecera de la orden). La
 * creación completa (orden + líneas + historial) se hace en una sola
 * transacción con {@link #createWithDetails(ServiceOrder)} (decisión D-09).
 * El vehículo y el responsable se devuelven «mínimos» (solo con su id).
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion.</p>
 */
public class ServiceOrderDao implements CrudOperations<ServiceOrder> {

    private static final String COLUMNS = "o.id, o.number, o.vehicle_id, o.responsible_id, o.entry_date,"
            + " o.estimated_delivery_date, o.entry_reason, o.observations, o.status";
    private static final String FROM = " FROM service_orders o";
    private static final String FROM_JOINED = " FROM service_orders o"
            + " JOIN vehicles v ON v.id = o.vehicle_id JOIN customers c ON c.id = v.customer_id";
    private static final String ORDER_BY = " ORDER BY o.entry_date DESC, o.id DESC";
    private static final String OPEN = "o.status NOT IN ('DELIVERED', 'CANCELLED')";

    private final DatabaseConnection connection;
    private final OrderLineDao lineDao;
    private final OrderHistoryDao historyDao;

    /**
     * @param connection fábrica de conexiones a Supabase
     * @param lineDao    DAO de líneas (se usa dentro de la transacción de creación)
     * @param historyDao DAO de historial (se usa dentro de la transacción de creación)
     */
    public ServiceOrderDao(DatabaseConnection connection, OrderLineDao lineDao, OrderHistoryDao historyDao) {
        this.connection = connection;
        this.lineDao = lineDao;
        this.historyDao = historyDao;
    }

    /** Inserta solo la cabecera. Para guardar también líneas e historial usa {@link #createWithDetails}. */
    @Override
    public ServiceOrder create(ServiceOrder entity) {
        try (Connection conn = connection.getConnection()) {
            insertHeader(conn, entity);
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo crear la orden de servicio.", e);
        }
    }

    /**
     * Inserta la orden con sus líneas y su historial en una sola transacción:
     * si algo falla no queda nada guardado.
     *
     * @param entity orden completa
     * @return la orden con su id
     */
    public ServiceOrder createWithDetails(ServiceOrder entity) {
        try (Connection conn = connection.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                insertHeader(conn, entity);
                for (OrderLine line : entity.getLines()) {
                    lineDao.insert(conn, entity.getId(), line);
                }
                for (OrderHistory entry : entity.getHistory()) {
                    historyDao.insert(conn, entity.getId(), entry);
                }
                conn.commit();
                return entity;
            } catch (SQLException | RuntimeException e) {
                // Se deshace todo antes de restaurar el auto-commit (que confirmaría lo pendiente).
                conn.rollback();
                entity.setId(0);
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo crear la orden de servicio.", e);
        }
    }

    @Override
    public ServiceOrder findById(int id) {
        return query("SELECT " + COLUMNS + FROM + " WHERE o.id = ?", List.of(id)).stream().findFirst().orElse(null);
    }

    @Override
    public List<ServiceOrder> findAll() {
        return query("SELECT " + COLUMNS + FROM + ORDER_BY, List.of());
    }

    @Override
    public boolean update(ServiceOrder entity) {
        String sql = "UPDATE service_orders SET number = ?, vehicle_id = ?, responsible_id = ?, entry_date = ?,"
                + " estimated_delivery_date = ?, entry_reason = ?, observations = ?, status = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindHeader(ps, entity);
            ps.setInt(9, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la orden de servicio.", e);
        }
    }

    /** Baja lógica (D-02): la orden pasa a {@code CANCELLED}; nunca se borra. */
    @Override
    public boolean delete(int id) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE service_orders SET status = ? WHERE id = ?")) {
            ps.setString(1, OrderStatus.CANCELLED.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo cancelar la orden de servicio.", e);
        }
    }

    /**
     * Busca órdenes por texto (número, placa, marca, modelo o nombre del
     * cliente), estado y responsable.
     *
     * @param text          texto libre (vacío = todas)
     * @param status        estado ({@code null} = todos)
     * @param responsibleId id del mecánico (0 = todos)
     * @return órdenes de la más reciente a la más antigua
     */
    public List<ServiceOrder> search(String text, OrderStatus status, int responsibleId) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(FROM_JOINED);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            conditions.add("(o.number ILIKE ? OR v.license_plate ILIKE ? OR v.make ILIKE ? OR v.model ILIKE ?"
                    + " OR (c.first_name || ' ' || c.last_name) ILIKE ?)");
            String like = "%" + escapeLike(text.trim().replace("#", "")) + "%";
            for (int i = 0; i < 5; i++) {
                params.add(like);
            }
        }
        if (status != null) {
            conditions.add("o.status = ?");
            params.add(status.name());
        }
        if (responsibleId > 0) {
            conditions.add("o.responsible_id = ?");
            params.add(responsibleId);
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(ORDER_BY);
        return query(sql.toString(), params);
    }

    /** Órdenes de un vehículo, de la más reciente a la más antigua. */
    public List<ServiceOrder> findByVehicle(int vehicleId) {
        return query("SELECT " + COLUMNS + FROM + " WHERE o.vehicle_id = ?" + ORDER_BY, List.of(vehicleId));
    }

    /** Órdenes de todos los vehículos de un cliente. */
    public List<ServiceOrder> findByCustomer(int customerId) {
        return query("SELECT " + COLUMNS + FROM_JOINED + " WHERE v.customer_id = ?" + ORDER_BY, List.of(customerId));
    }

    /** Cantidad de órdenes en un estado. */
    public int countByStatus(OrderStatus status) {
        return count("SELECT COUNT(*) FROM service_orders o WHERE o.status = ?", List.of(status.name()));
    }

    /** Cantidad de órdenes abiertas (ni entregadas ni canceladas). */
    public int countOpen() {
        return count("SELECT COUNT(*) FROM service_orders o WHERE " + OPEN, List.of());
    }

    /** Indica si el vehículo tiene una orden abierta. */
    public boolean existsOpenOrderForVehicle(int vehicleId) {
        return count("SELECT COUNT(*) FROM service_orders o WHERE o.vehicle_id = ? AND " + OPEN,
                List.of(vehicleId)) > 0;
    }

    /**
     * Siguiente consecutivo para el número de orden (el id más alto + 1).
     *
     * @return número sugerido, mayor que cero
     */
    public int nextSequence() {
        return count("SELECT COALESCE(MAX(id), 0) + 1 FROM service_orders", List.of());
    }

    // ---------- ayudas ----------

    private void insertHeader(Connection conn, ServiceOrder entity) throws SQLException {
        String sql = "INSERT INTO service_orders (number, vehicle_id, responsible_id, entry_date,"
                + " estimated_delivery_date, entry_reason, observations, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindHeader(ps, entity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
        }
    }

    private void bindHeader(PreparedStatement ps, ServiceOrder entity) throws SQLException {
        ps.setString(1, entity.getNumber());
        ps.setInt(2, entity.getVehicle().getId());
        if (entity.getResponsible() == null) {
            ps.setNull(3, Types.INTEGER);
        } else {
            ps.setInt(3, entity.getResponsible().getId());
        }
        LocalDate entry = entity.getEntryDate() == null ? LocalDate.now() : entity.getEntryDate();
        ps.setDate(4, Date.valueOf(entry));
        if (entity.getEstimatedDeliveryDate() == null) {
            ps.setNull(5, Types.DATE);
        } else {
            ps.setDate(5, Date.valueOf(entity.getEstimatedDeliveryDate()));
        }
        ps.setString(6, entity.getEntryReason());
        ps.setString(7, entity.getObservations());
        ps.setString(8, (entity.getStatus() == null ? OrderStatus.RECEIVED : entity.getStatus()).name());
    }

    private List<ServiceOrder> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<ServiceOrder> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las órdenes de servicio.", e);
        }
    }

    private int count(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron contar las órdenes de servicio.", e);
        }
    }

    private static void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private ServiceOrder map(ResultSet rs) throws SQLException {
        ServiceOrder order = new ServiceOrder();
        order.setId(rs.getInt("id"));
        order.setNumber(rs.getString("number"));
        Vehicle vehicle = new Vehicle();
        vehicle.setId(rs.getInt("vehicle_id"));
        order.setVehicle(vehicle);
        int responsibleId = rs.getInt("responsible_id");
        if (!rs.wasNull()) {
            User responsible = new User();
            responsible.setId(responsibleId);
            order.setResponsible(responsible);
        }
        Date entry = rs.getDate("entry_date");
        order.setEntryDate(entry == null ? null : entry.toLocalDate());
        Date delivery = rs.getDate("estimated_delivery_date");
        order.setEstimatedDeliveryDate(delivery == null ? null : delivery.toLocalDate());
        order.setEntryReason(rs.getString("entry_reason"));
        order.setObservations(rs.getString("observations"));
        order.setStatus(OrderStatus.valueOf(rs.getString("status")));
        return order;
    }
}
