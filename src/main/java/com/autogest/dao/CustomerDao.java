package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Customer;
import com.autogest.model.CustomerStatus;
import com.autogest.model.DocumentType;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL a la tabla {@code customers}. Implementa {@link CrudOperations} para
 * las operaciones genéricas y agrega las consultas propias del módulo de clientes
 * (búsqueda con filtros, verificación de documento repetido y conteos para los
 * indicadores).
 *
 * <p>Todo el SQL se ejecuta con {@link PreparedStatement} para evitar inyección
 * y usa los nombres de columna en inglés definidos en {@code schema.sql}. El tipo
 * de documento y el estado se guardan y leen como texto con el {@code name()} del
 * enum. La eliminación es lógica (cambio de estado), según la decisión D-02. Este
 * DAO no valida reglas de negocio: eso lo hace {@code CustomerService}.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion,
 * Low Coupling.</p>
 */
public class CustomerDao implements CrudOperations<Customer> {

    /** Columnas de la tabla en el orden usado por el mapeo y las consultas. */
    private static final String COLUMNS = "id, document_type, document_number, first_name, last_name, "
            + "phone, email, city, address, notes, accepts_promotions, status, registration_date";

    private final DatabaseConnection connection;

    /**
     * Crea el DAO con la fábrica de conexiones inyectada.
     *
     * @param connection fábrica de conexiones JDBC de AutoGest
     */
    public CustomerDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /** {@inheritDoc} */
    @Override
    public Customer create(Customer entity) {
        String sql = "INSERT INTO customers (document_type, document_number, first_name, last_name, "
                + "phone, email, city, address, notes, accepts_promotions, status, registration_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            throw new DataAccessException("No se pudo crear el cliente.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Customer findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM customers WHERE id = ?";
        return query(sql, List.of(id)).stream().findFirst().orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    public List<Customer> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM customers ORDER BY last_name, first_name";
        return query(sql, List.of());
    }

    /** {@inheritDoc} */
    @Override
    public boolean update(Customer entity) {
        String sql = "UPDATE customers SET document_type = ?, document_number = ?, first_name = ?, "
                + "last_name = ?, phone = ?, email = ?, city = ?, address = ?, notes = ?, "
                + "accepts_promotions = ?, status = ?, registration_date = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setFields(ps, entity);
            ps.setInt(13, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el cliente.", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Baja lógica (D-02): no se borra la fila, se marca al cliente como
     * {@link CustomerStatus#INACTIVE} para conservar su historial.</p>
     */
    @Override
    public boolean delete(int id) {
        String sql = "UPDATE customers SET status = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, CustomerStatus.INACTIVE.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo desactivar el cliente.", e);
        }
    }

    /**
     * Busca clientes por texto y estado, ordenados por apellidos y nombres. El
     * texto se busca (sin distinguir mayúsculas, con {@code ILIKE}) en los
     * nombres, los apellidos, el nombre completo, el número de documento y el
     * correo. Un texto nulo o en blanco no filtra, y un estado nulo trae todos.
     *
     * @param text   texto a buscar (o {@code null} para no filtrar)
     * @param status estado a filtrar (o {@code null} para todos)
     * @return la lista de clientes que cumplen los filtros
     */
    public List<Customer> search(String text, CustomerStatus status) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM customers");
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (text != null && !text.isBlank()) {
            conditions.add("(first_name ILIKE ? OR last_name ILIKE ? "
                    + "OR (first_name || ' ' || last_name) ILIKE ? "
                    + "OR document_number ILIKE ? OR email ILIKE ?)");
            String like = "%" + escapeLike(text.trim()) + "%";
            for (int i = 0; i < 5; i++) {
                params.add(like);
            }
        }
        if (status != null) {
            conditions.add("status = ?");
            params.add(status.name());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY last_name, first_name");
        return query(sql.toString(), params);
    }

    /**
     * Indica si ya existe un cliente con ese número de documento.
     *
     * @param number número de documento ya normalizado
     * @return {@code true} si hay un cliente con ese número
     */
    public boolean existsByDocument(String number) {
        return count("SELECT COUNT(*) FROM customers WHERE document_number = ?", List.of(number)) > 0;
    }

    /**
     * Indica si otro cliente (distinto del indicado) tiene ese número de
     * documento. Sirve para validar una edición sin chocar consigo mismo.
     *
     * @param number número de documento ya normalizado
     * @param id     identificador del cliente que se excluye de la búsqueda
     * @return {@code true} si otro cliente tiene ese número
     */
    public boolean existsByDocumentExcluding(String number, int id) {
        return count("SELECT COUNT(*) FROM customers WHERE document_number = ? AND id <> ?",
                List.of(number, id)) > 0;
    }

    /**
     * Cuenta todos los clientes.
     *
     * @return número total de clientes
     */
    public int count() {
        return count("SELECT COUNT(*) FROM customers", List.of());
    }

    /**
     * Cuenta los clientes que tienen el estado indicado.
     *
     * @param status estado a contar
     * @return número de clientes con ese estado
     */
    public int countByStatus(CustomerStatus status) {
        return count("SELECT COUNT(*) FROM customers WHERE status = ?", List.of(status.name()));
    }

    /**
     * Cuenta los clientes registrados desde una fecha (inclusive).
     *
     * @param date fecha inicial
     * @return número de clientes con fecha de registro igual o posterior
     */
    public int countRegisteredSince(LocalDate date) {
        return count("SELECT COUNT(*) FROM customers WHERE registration_date >= ?",
                List.of(Date.valueOf(date)));
    }

    /** Asigna los doce campos del cliente a los doce primeros parámetros. */
    private void setFields(PreparedStatement ps, Customer customer) throws SQLException {
        ps.setString(1, customer.getDocumentType() == null ? null : customer.getDocumentType().name());
        ps.setString(2, customer.getDocumentNumber());
        ps.setString(3, customer.getFirstName());
        ps.setString(4, customer.getLastName());
        ps.setString(5, customer.getPhone());
        ps.setString(6, customer.getEmail());
        ps.setString(7, customer.getCity());
        ps.setString(8, customer.getAddress());
        ps.setString(9, customer.getNotes());
        ps.setBoolean(10, customer.isAcceptsPromotions());
        ps.setString(11, customer.getStatus() == null ? null : customer.getStatus().name());
        ps.setDate(12, customer.getRegistrationDate() == null ? null : Date.valueOf(customer.getRegistrationDate()));
    }

    /** Ejecuta una consulta de clientes y mapea cada fila. */
    private List<Customer> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Customer> customers = new ArrayList<>();
                while (rs.next()) {
                    customers.add(map(rs));
                }
                return customers;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los clientes.", e);
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
            throw new DataAccessException("No se pudo contar los clientes.", e);
        }
    }

    /** Asigna la lista de parámetros a la sentencia. */
    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    /** Escapa los comodines de LIKE para que el texto del usuario se busque tal cual. */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    /** Convierte una fila de {@code customers} en un {@link Customer}. */
    private Customer map(ResultSet rs) throws SQLException {
        return map(rs, "");
    }

    /**
     * Lista de columnas de {@code customers} con alias de tabla y prefijo, para
     * consultas de otros DAO que traen al cliente con un JOIN.
     *
     * @param tableAlias alias de la tabla {@code customers} en la consulta
     * @param prefix     prefijo de cada columna en el resultado
     * @return fragmento SQL {@code alias.columna AS prefijo_columna, ...}
     */
    static String prefixedColumns(String tableAlias, String prefix) {
        StringBuilder sql = new StringBuilder();
        for (String column : COLUMNS.split(",\\s*")) {
            if (sql.length() > 0) {
                sql.append(", ");
            }
            sql.append(tableAlias).append('.').append(column).append(" AS ").append(prefix).append(column);
        }
        return sql.toString();
    }

    /**
     * Convierte una fila en un {@link Customer} leyendo columnas con el prefijo
     * indicado (vacío para una consulta directa de {@code customers}).
     */
    static Customer map(ResultSet rs, String prefix) throws SQLException {
        Customer customer = new Customer();
        customer.setId(rs.getInt(prefix + "id"));
        customer.setDocumentType(DocumentType.valueOf(rs.getString(prefix + "document_type")));
        customer.setDocumentNumber(rs.getString(prefix + "document_number"));
        customer.setFirstName(rs.getString(prefix + "first_name"));
        customer.setLastName(rs.getString(prefix + "last_name"));
        customer.setPhone(rs.getString(prefix + "phone"));
        customer.setEmail(rs.getString(prefix + "email"));
        customer.setCity(rs.getString(prefix + "city"));
        customer.setAddress(rs.getString(prefix + "address"));
        customer.setNotes(rs.getString(prefix + "notes"));
        customer.setAcceptsPromotions(rs.getBoolean(prefix + "accepts_promotions"));
        customer.setStatus(CustomerStatus.valueOf(rs.getString(prefix + "status")));
        Date registration = rs.getDate(prefix + "registration_date");
        customer.setRegistrationDate(registration == null ? null : registration.toLocalDate());
        return customer;
    }
}
