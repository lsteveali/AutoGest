package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.InvoiceSummary;
import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;

import java.math.BigDecimal;
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
import java.util.Set;
import java.util.TreeSet;

/**
 * Acceso SQL a la tabla {@code invoices}. Implementa {@link CrudOperations}
 * y agrega la búsqueda con filtros, los indicadores, el paso a vencidas y el
 * consecutivo del número.
 *
 * <p>Las lecturas traen con un JOIN el número y el estado de la orden, el
 * vehículo y el nombre del cliente, para que la lista se arme con una sola
 * consulta. La orden completa (con sus líneas) la carga el Service cuando se
 * abre el detalle. La anulación es lógica (D-02): {@link #delete(int)} pasa
 * la factura a {@code VOIDED}.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion.</p>
 */
public class InvoiceDao implements CrudOperations<Invoice> {

    private static final String COLUMNS = "i.id, i.number, i.order_id, i.issue_date, i.due_date, i.subtotal,"
            + " i.taxes, i.discount, i.total, i.status, i.payment_method,"
            + " o.number AS order_number, o.status AS order_status, o.entry_date AS order_entry_date,"
            + " v.id AS vehicle_id, v.make, v.model, v.license_plate, v.year,"
            + " c.id AS customer_id, c.first_name, c.last_name";
    private static final String FROM = " FROM invoices i"
            + " JOIN service_orders o ON o.id = i.order_id"
            + " JOIN vehicles v ON v.id = o.vehicle_id"
            + " JOIN customers c ON c.id = v.customer_id";
    private static final String ORDER_BY = " ORDER BY i.issue_date DESC, i.id DESC";

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public InvoiceDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    @Override
    public Invoice create(Invoice entity) {
        String sql = "INSERT INTO invoices (number, order_id, issue_date, due_date, subtotal, taxes, discount,"
                + " total, status, payment_method) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            throw new DataAccessException("No se pudo guardar la factura.", e);
        }
    }

    @Override
    public Invoice findById(int id) {
        return first(query("SELECT " + COLUMNS + FROM + " WHERE i.id = ?", List.of(id)));
    }

    @Override
    public List<Invoice> findAll() {
        return query("SELECT " + COLUMNS + FROM + ORDER_BY, List.of());
    }

    @Override
    public boolean update(Invoice entity) {
        String sql = "UPDATE invoices SET number = ?, order_id = ?, issue_date = ?, due_date = ?, subtotal = ?,"
                + " taxes = ?, discount = ?, total = ?, status = ?, payment_method = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, entity);
            ps.setInt(11, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar la factura.", e);
        }
    }

    /** Anulación lógica (D-02): la factura pasa a {@code VOIDED}; nunca se borra. */
    @Override
    public boolean delete(int id) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE invoices SET status = ? WHERE id = ?")) {
            ps.setString(1, InvoiceStatus.VOIDED.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo anular la factura.", e);
        }
    }

    /**
     * Factura de una orden (una orden tiene 0..1 factura).
     *
     * @param orderId id de la orden
     * @return la factura, o {@code null} si la orden no tiene
     */
    public Invoice findByOrder(int orderId) {
        return first(query("SELECT " + COLUMNS + FROM + " WHERE i.order_id = ?", List.of(orderId)));
    }

    /**
     * Busca facturas por texto (número de factura u orden, placa, vehículo o
     * cliente), estado, fecha de emisión desde y método de pago.
     *
     * @param text          texto libre (vacío = todas)
     * @param status        estado ({@code null} = todos)
     * @param issuedFrom    emitidas desde esta fecha, inclusive ({@code null} = todas)
     * @param paymentMethod método de pago ({@code null} = todos)
     * @return facturas de la más reciente a la más antigua
     */
    public List<Invoice> search(String text, InvoiceStatus status, LocalDate issuedFrom, String paymentMethod) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(FROM);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            conditions.add("(i.number ILIKE ? OR o.number ILIKE ? OR v.license_plate ILIKE ? OR v.make ILIKE ?"
                    + " OR v.model ILIKE ? OR (c.first_name || ' ' || c.last_name) ILIKE ?)");
            String like = "%" + escapeLike(text.trim().replace("#", "")) + "%";
            for (int i = 0; i < 6; i++) {
                params.add(like);
            }
        }
        if (status != null) {
            conditions.add("i.status = ?");
            params.add(status.name());
        }
        if (issuedFrom != null) {
            conditions.add("i.issue_date >= ?");
            params.add(Date.valueOf(issuedFrom));
        }
        if (paymentMethod != null) {
            conditions.add("i.payment_method = ?");
            params.add(paymentMethod);
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(ORDER_BY);
        return query(sql.toString(), params);
    }

    /**
     * Calcula los indicadores de Facturación en una sola consulta.
     *
     * @return conteos por estado y valores facturado, pagado y por cobrar
     */
    public InvoiceSummary summary() {
        return summary(null);
    }

    /**
     * Calcula los indicadores de las facturas emitidas desde una fecha (el
     * Dashboard lo usa para el mes en curso).
     *
     * @param issuedFrom emitidas desde esta fecha, inclusive ({@code null} = todas)
     * @return conteos por estado y valores facturado, pagado y por cobrar
     */
    public InvoiceSummary summary(LocalDate issuedFrom) {
        String sql = "SELECT COUNT(*),"
                + " COUNT(*) FILTER (WHERE status = 'PAID'),"
                + " COUNT(*) FILTER (WHERE status = 'PENDING'),"
                + " COUNT(*) FILTER (WHERE status = 'OVERDUE'),"
                + " COALESCE(SUM(total) FILTER (WHERE status <> 'VOIDED'), 0),"
                + " COALESCE(SUM(total) FILTER (WHERE status = 'PAID'), 0),"
                + " COALESCE(SUM(total) FILTER (WHERE status IN ('PENDING', 'OVERDUE')), 0)"
                + " FROM invoices" + (issuedFrom == null ? "" : " WHERE issue_date >= ?");
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (issuedFrom != null) {
                ps.setDate(1, Date.valueOf(issuedFrom));
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new InvoiceSummary(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4),
                        rs.getBigDecimal(5), rs.getBigDecimal(6), rs.getBigDecimal(7));
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron calcular los indicadores de facturación.", e);
        }
    }

    /**
     * Pasa a {@code OVERDUE} las facturas pendientes cuyo vencimiento es
     * anterior a la fecha dada, en una sola sentencia.
     *
     * @param today fecha de referencia
     * @return los ids de las facturas que acaban de vencer (para avisar al cliente)
     */
    public List<Integer> markOverdue(LocalDate today) {
        String sql = "UPDATE invoices SET status = 'OVERDUE'"
                + " WHERE status = 'PENDING' AND due_date IS NOT NULL AND due_date < ? RETURNING id";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(today));
            try (ResultSet rs = ps.executeQuery()) {
                List<Integer> ids = new ArrayList<>();
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
                return ids;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron actualizar las facturas vencidas.", e);
        }
    }

    /**
     * Ids de las órdenes que ya tienen una factura vigente (no anulada).
     *
     * @return ids de orden
     */
    public Set<Integer> findInvoicedOrderIds() {
        String sql = "SELECT order_id FROM invoices WHERE status <> 'VOIDED'";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            Set<Integer> ids = new TreeSet<>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las órdenes facturadas.", e);
        }
    }

    /**
     * Siguiente consecutivo para el número de factura (el id más alto + 1).
     *
     * @return consecutivo mayor que cero
     */
    public int nextSequence() {
        String sql = "SELECT COALESCE(MAX(id), 0) + 1 FROM invoices";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo calcular el número de la factura.", e);
        }
    }

    // ---------- ayudas ----------

    private void bind(PreparedStatement ps, Invoice invoice) throws SQLException {
        ps.setString(1, invoice.getNumber());
        ps.setInt(2, invoice.getOrder().getId());
        ps.setDate(3, Date.valueOf(invoice.getIssueDate() == null ? LocalDate.now() : invoice.getIssueDate()));
        if (invoice.getDueDate() == null) {
            ps.setNull(4, Types.DATE);
        } else {
            ps.setDate(4, Date.valueOf(invoice.getDueDate()));
        }
        ps.setBigDecimal(5, invoice.getSubtotal());
        ps.setBigDecimal(6, invoice.getTaxes());
        ps.setBigDecimal(7, invoice.getDiscount());
        ps.setBigDecimal(8, invoice.getTotal());
        ps.setString(9, (invoice.getStatus() == null ? InvoiceStatus.PENDING : invoice.getStatus()).name());
        ps.setString(10, invoice.getPaymentMethod());
    }

    private List<Invoice> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Invoice> invoices = new ArrayList<>();
                while (rs.next()) {
                    invoices.add(map(rs));
                }
                return invoices;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las facturas.", e);
        }
    }

    private static Invoice first(List<Invoice> invoices) {
        return invoices.isEmpty() ? null : invoices.get(0);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Factura con su orden «resumida»: número, estado, vehículo y nombre del cliente. */
    private static Invoice map(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(rs.getInt("id"));
        invoice.setNumber(rs.getString("number"));
        invoice.setIssueDate(toLocalDate(rs.getDate("issue_date")));
        invoice.setDueDate(toLocalDate(rs.getDate("due_date")));
        invoice.setSubtotal(money(rs.getBigDecimal("subtotal")));
        invoice.setTaxes(money(rs.getBigDecimal("taxes")));
        invoice.setDiscount(money(rs.getBigDecimal("discount")));
        invoice.setTotal(money(rs.getBigDecimal("total")));
        invoice.setStatus(InvoiceStatus.valueOf(rs.getString("status")));
        invoice.setPaymentMethod(rs.getString("payment_method"));

        Customer owner = new Customer();
        owner.setId(rs.getInt("customer_id"));
        owner.setFirstName(rs.getString("first_name"));
        owner.setLastName(rs.getString("last_name"));
        Vehicle vehicle = new Vehicle();
        vehicle.setId(rs.getInt("vehicle_id"));
        vehicle.setMake(rs.getString("make"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setLicensePlate(rs.getString("license_plate"));
        vehicle.setYear(rs.getInt("year"));
        vehicle.setOwner(owner);
        ServiceOrder order = new ServiceOrder();
        order.setId(rs.getInt("order_id"));
        order.setNumber(rs.getString("order_number"));
        order.setStatus(OrderStatus.valueOf(rs.getString("order_status")));
        order.setEntryDate(toLocalDate(rs.getDate("order_entry_date")));
        order.setVehicle(vehicle);
        invoice.setOrder(order);
        return invoice;
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
