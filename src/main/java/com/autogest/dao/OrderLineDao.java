package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.OrderLine;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL a la tabla {@code order_lines} (líneas de servicio y de repuesto
 * de una orden). Las líneas no tienen id propio en el modelo: siempre se leen
 * y escriben a través de su orden, por eso este DAO no implementa
 * {@code CrudOperations} (ISP: no se fuerzan operaciones que no aplican).
 *
 * <p>Aquí vive la traducción entre la jerarquía {@code OrderLine} y la columna
 * {@code line_type}: el resto del sistema usa polimorfismo sin {@code instanceof}.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations, ISP.</p>
 */
public class OrderLineDao {

    private static final String INSERT = "INSERT INTO order_lines"
            + " (order_id, line_type, service_offering_id, part_id, quantity, unit_price) VALUES (?, ?, ?, ?, ?, ?)";

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public OrderLineDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /**
     * Agrega una línea a una orden existente (operación independiente).
     *
     * @param orderId id de la orden
     * @param line    línea a guardar
     */
    public void insert(int orderId, OrderLine line) {
        try (Connection conn = connection.getConnection()) {
            insert(conn, orderId, line);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo agregar la línea a la orden.", e);
        }
    }

    /**
     * Agrega una línea usando una conexión abierta (para transacciones de
     * {@link ServiceOrderDao}).
     */
    void insert(Connection conn, int orderId, OrderLine line) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT)) {
            ps.setInt(1, orderId);
            if (line instanceof ServiceLine serviceLine) {
                ps.setString(2, "SERVICE");
                ps.setInt(3, serviceLine.getServiceOffering().getId());
                ps.setNull(4, Types.INTEGER);
            } else if (line instanceof PartLine partLine) {
                ps.setString(2, "PART");
                ps.setNull(3, Types.INTEGER);
                ps.setInt(4, partLine.getPart().getId());
            } else {
                throw new SQLException("Tipo de línea no soportado: " + line.getClass().getSimpleName());
            }
            ps.setInt(5, line.getQuantity());
            ps.setBigDecimal(6, line.getUnitPrice());
            ps.executeUpdate();
        }
    }

    /**
     * Líneas de una orden, en el orden en que se agregaron. El servicio y el
     * repuesto se devuelven «mínimos» (solo con su id).
     *
     * @param orderId id de la orden
     * @return líneas de la orden
     */
    public List<OrderLine> findByOrder(int orderId) {
        String sql = "SELECT line_type, service_offering_id, part_id, quantity, unit_price"
                + " FROM order_lines WHERE order_id = ? ORDER BY id";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrderLine> lines = new ArrayList<>();
                while (rs.next()) {
                    lines.add(map(rs));
                }
                return lines;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las líneas de la orden.", e);
        }
    }

    private OrderLine map(ResultSet rs) throws SQLException {
        OrderLine line;
        if ("SERVICE".equals(rs.getString("line_type"))) {
            ServiceOffering offering = new ServiceOffering();
            offering.setId(rs.getInt("service_offering_id"));
            ServiceLine serviceLine = new ServiceLine();
            serviceLine.setServiceOffering(offering);
            line = serviceLine;
        } else {
            Part part = new Part();
            part.setId(rs.getInt("part_id"));
            PartLine partLine = new PartLine();
            partLine.setPart(part);
            line = partLine;
        }
        line.setQuantity(rs.getInt("quantity"));
        line.setUnitPrice(rs.getBigDecimal("unit_price"));
        return line;
    }
}
