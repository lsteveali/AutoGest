package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Customer;
import com.autogest.model.FuelType;
import com.autogest.model.Transmission;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleCounts;
import com.autogest.model.VehicleStatus;
import com.autogest.model.VehicleType;

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
 * Acceso SQL a la tabla {@code vehicles}. Implementa {@link CrudOperations}
 * para las operaciones genéricas y agrega las consultas propias del módulo de
 * vehículos (búsqueda con filtros, verificación de placa repetida, vehículos de
 * un propietario y conteos para los indicadores).
 *
 * <p>Todo el SQL se ejecuta con {@link PreparedStatement} para evitar inyección
 * y usa los nombres de columna en inglés definidos en {@code schema.sql}. El
 * tipo de combustible, la clase, la transmisión y el estado se guardan y leen
 * como texto con el {@code name()} del enum. El propietario se guarda como
 * {@code customer_id} y al leer se trae completo con un LEFT JOIN a
 * {@code customers}, en la misma consulta. La eliminación es
 * lógica (cambio de estado), según la decisión D-02. Este DAO no valida reglas
 * de negocio: eso lo hace {@code VehicleService}.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion,
 * Low Coupling.</p>
 */
public class VehicleDao implements CrudOperations<Vehicle> {

    /** Columnas de la tabla en el orden usado por el mapeo y las consultas. */
    private static final String COLUMNS = "id, customer_id, make, model, year, license_plate, vin, color, "
            + "fuel_type, mileage, status, vehicle_type, transmission, engine_number, "
            + "registration_card_number, observations, last_maintenance_mileage, last_maintenance_date, "
            + "next_maintenance_mileage, next_maintenance_date";

    /** Prefijo de las columnas del propietario traídas con el JOIN. */
    private static final String OWNER_PREFIX = "owner_";

    /**
     * Inicio de las consultas de lectura: trae el vehículo y, con un LEFT JOIN,
     * los datos de su propietario en la misma consulta (columnas {@code owner_*}),
     * para no consultar el cliente fila por fila.
     */
    private static final String SELECT_WITH_OWNER = "SELECT "
            + prefixed("v", COLUMNS) + ", " + CustomerDao.prefixedColumns("c", OWNER_PREFIX)
            + " FROM vehicles v LEFT JOIN customers c ON c.id = v.customer_id";

    private final DatabaseConnection connection;

    /**
     * Crea el DAO con la fábrica de conexiones inyectada.
     *
     * @param connection fábrica de conexiones JDBC de AutoGest
     */
    public VehicleDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /** {@inheritDoc} */
    @Override
    public Vehicle create(Vehicle entity) {
        String sql = "INSERT INTO vehicles (customer_id, make, model, year, license_plate, vin, color, "
                + "fuel_type, mileage, status, vehicle_type, transmission, engine_number, "
                + "registration_card_number, observations, last_maintenance_mileage, last_maintenance_date, "
                + "next_maintenance_mileage, next_maintenance_date) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            throw new DataAccessException("No se pudo crear el vehículo.", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vehicle findById(int id) {
        String sql = SELECT_WITH_OWNER + " WHERE v.id = ?";
        return query(sql, List.of(id)).stream().findFirst().orElse(null);
    }

    /** {@inheritDoc} */
    @Override
    public List<Vehicle> findAll() {
        String sql = SELECT_WITH_OWNER + " ORDER BY v.make, v.model";
        return query(sql, List.of());
    }

    /** {@inheritDoc} */
    @Override
    public boolean update(Vehicle entity) {
        String sql = "UPDATE vehicles SET customer_id = ?, make = ?, model = ?, year = ?, license_plate = ?, "
                + "vin = ?, color = ?, fuel_type = ?, mileage = ?, status = ?, vehicle_type = ?, "
                + "transmission = ?, engine_number = ?, registration_card_number = ?, observations = ?, "
                + "last_maintenance_mileage = ?, last_maintenance_date = ?, next_maintenance_mileage = ?, "
                + "next_maintenance_date = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setFields(ps, entity);
            ps.setInt(20, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el vehículo.", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Baja lógica (D-02): no se borra la fila, se marca el vehículo como
     * {@link VehicleStatus#INACTIVE} para conservar su historial.</p>
     */
    @Override
    public boolean delete(int id) {
        String sql = "UPDATE vehicles SET status = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, VehicleStatus.INACTIVE.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo desactivar el vehículo.", e);
        }
    }

    /**
     * Busca vehículos por texto, marca, estado y clase, ordenados por marca y modelo.
     *
     * @param text   texto a buscar (o {@code null} para no filtrar)
     * @param make   marca a filtrar (o {@code null} para todas)
     * @param status estado a filtrar (o {@code null} para todos)
     * @param type   clase de vehículo a filtrar (o {@code null} para todas)
     * @return la lista de vehículos que cumplen los filtros
     */
    public List<Vehicle> search(String text, String make, VehicleStatus status, VehicleType type) {
        StringBuilder sql = new StringBuilder(SELECT_WITH_OWNER);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (text != null && !text.isBlank()) {
            conditions.add("(v.make ILIKE ? OR v.model ILIKE ? OR v.license_plate ILIKE ? OR v.vin ILIKE ? "
                    + "OR c.first_name ILIKE ? OR c.last_name ILIKE ? "
                    + "OR (c.first_name || ' ' || c.last_name) ILIKE ?)");
            String like = "%" + escapeLike(text.trim()) + "%";
            for (int i = 0; i < 7; i++) {
                params.add(like);
            }
        }
        if (make != null && !make.isBlank()) {
            conditions.add("v.make ILIKE ?");
            params.add("%" + escapeLike(make.trim()) + "%");
        }
        if (status != null) {
            conditions.add("v.status = ?");
            params.add(status.name());
        }
        if (type != null) {
            conditions.add("v.vehicle_type = ?");
            params.add(type.name());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY v.make, v.model");
        return query(sql.toString(), params);
    }

    /**
     * Busca vehículos por texto y estado, ordenados por marca y modelo.
     */
    public List<Vehicle> search(String text, VehicleStatus status, VehicleType type) {
        return search(text, null, status, type);
    }

    /**
     * Devuelve los vehículos de un propietario, ordenados por marca y modelo.
     *
     * @param customerId identificador del cliente propietario
     * @return la lista de vehículos del propietario
     */
    public List<Vehicle> findByCustomer(int customerId) {
        String sql = SELECT_WITH_OWNER + " WHERE v.customer_id = ? ORDER BY v.make, v.model";
        return query(sql, List.of(customerId));
    }

    /**
     * Indica si ya existe un vehículo con esa placa.
     *
     * @param plate placa ya normalizada
     * @return {@code true} si hay un vehículo con esa placa
     */
    public boolean existsByLicensePlate(String plate) {
        return count("SELECT COUNT(*) FROM vehicles WHERE license_plate = ?", List.of(plate)) > 0;
    }

    /**
     * Indica si otro vehículo (distinto del indicado) tiene esa placa. Sirve
     * para validar una edición sin chocar consigo mismo.
     *
     * @param plate placa ya normalizada
     * @param id    identificador del vehículo que se excluye de la búsqueda
     * @return {@code true} si otro vehículo tiene esa placa
     */
    public boolean existsByLicensePlateExcluding(String plate, int id) {
        return count("SELECT COUNT(*) FROM vehicles WHERE license_plate = ? AND id <> ?",
                List.of(plate, id)) > 0;
    }

    /**
     * Cuenta todos los vehículos.
     *
     * @return número total de vehículos
     */
    public int count() {
        return count("SELECT COUNT(*) FROM vehicles", List.of());
    }

    /**
     * Cuenta los vehículos que tienen el estado indicado.
     *
     * @param status estado a contar
     * @return número de vehículos con ese estado
     */
    public int countByStatus(VehicleStatus status) {
        return count("SELECT COUNT(*) FROM vehicles WHERE status = ?", List.of(status.name()));
    }

    /**
     * Cuenta los vehículos de la clase indicada.
     *
     * @param type clase a contar
     * @return número de vehículos de esa clase
     */
    public int countByType(VehicleType type) {
        return count("SELECT COUNT(*) FROM vehicles WHERE vehicle_type = ?", List.of(type.name()));
    }

    /**
     * Cuenta los vehículos cuyo próximo mantenimiento ya venció o vence hasta la
     * fecha indicada (inclusive).
     *
     * @param date fecha límite del aviso
     * @return número de vehículos con mantenimiento próximo o vencido
     */
    public int countMaintenanceDue(LocalDate date) {
        return count("SELECT COUNT(*) FROM vehicles WHERE next_maintenance_date IS NOT NULL "
                + "AND next_maintenance_date <= ?", List.of(Date.valueOf(date)));
    }

    /**
     * Calcula los cuatro indicadores de la pantalla de vehículos en una sola
     * consulta: total, activos, en servicio y con mantenimiento próximo o
     * vencido hasta la fecha indicada.
     *
     * @param maintenanceDate fecha límite del aviso de mantenimiento
     * @return los indicadores
     */
    public VehicleCounts countSummary(LocalDate maintenanceDate) {
        String sql = "SELECT COUNT(*), "
                + "COUNT(*) FILTER (WHERE status = ?), "
                + "COUNT(*) FILTER (WHERE status = ?), "
                + "COUNT(*) FILTER (WHERE next_maintenance_date IS NOT NULL AND next_maintenance_date <= ?) "
                + "FROM vehicles";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, List.of(VehicleStatus.ACTIVE.name(), VehicleStatus.IN_SERVICE.name(),
                    Date.valueOf(maintenanceDate)));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new VehicleCounts(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4));
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar los vehículos.", e);
        }
    }

    /**
     * Cuenta los vehículos de un propietario.
     *
     * @param customerId identificador del cliente propietario
     * @return número de vehículos del propietario
     */
    public int countByCustomer(int customerId) {
        return count("SELECT COUNT(*) FROM vehicles WHERE customer_id = ?", List.of(customerId));
    }

    /**
     * Devuelve las marcas registradas, sin repetir y en orden alfabético.
     *
     * @return lista de marcas distintas (vacía si no hay vehículos)
     */
    public List<String> findMakes() {
        String sql = "SELECT DISTINCT make FROM vehicles WHERE make IS NOT NULL AND TRIM(make) <> '' ORDER BY make";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> makes = new ArrayList<>();
            while (rs.next()) {
                makes.add(rs.getString("make"));
            }
            return makes;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar las marcas de vehículos.", e);
        }
    }

    /** Asigna los diecinueve campos del vehículo a los diecinueve primeros parámetros. */
    private void setFields(PreparedStatement ps, Vehicle vehicle) throws SQLException {
        ps.setInt(1, vehicle.getOwner() == null ? 0 : vehicle.getOwner().getId());
        ps.setString(2, vehicle.getMake());
        ps.setString(3, vehicle.getModel());
        ps.setInt(4, vehicle.getYear());
        ps.setString(5, vehicle.getLicensePlate());
        ps.setString(6, vehicle.getVin());
        ps.setString(7, vehicle.getColor());
        ps.setString(8, vehicle.getFuelType() == null ? null : vehicle.getFuelType().name());
        ps.setInt(9, vehicle.getMileage());
        ps.setString(10, vehicle.getStatus() == null ? null : vehicle.getStatus().name());
        ps.setString(11, vehicle.getVehicleType() == null ? null : vehicle.getVehicleType().name());
        ps.setString(12, vehicle.getTransmission() == null ? null : vehicle.getTransmission().name());
        ps.setString(13, vehicle.getEngineNumber());
        ps.setString(14, vehicle.getRegistrationCardNumber());
        ps.setString(15, vehicle.getObservations());
        setInteger(ps, 16, vehicle.getLastMaintenanceMileage());
        setDate(ps, 17, vehicle.getLastMaintenanceDate());
        setInteger(ps, 18, vehicle.getNextMaintenanceMileage());
        setDate(ps, 19, vehicle.getNextMaintenanceDate());
    }

    /** Asigna un entero que puede ser nulo. */
    private static void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    /** Asigna una fecha que puede ser nula. */
    private static void setDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }

    /** Ejecuta una consulta de vehículos y mapea cada fila. */
    private List<Vehicle> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Vehicle> vehicles = new ArrayList<>();
                while (rs.next()) {
                    vehicles.add(map(rs));
                }
                return vehicles;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar los vehículos.", e);
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
            throw new DataAccessException("No se pudo contar los vehículos.", e);
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

    /** Convierte una fila de {@code vehicles} en un {@link Vehicle}. */
    private Vehicle map(ResultSet rs) throws SQLException {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(rs.getInt("id"));
        vehicle.setOwner(mapOwner(rs));
        vehicle.setMake(rs.getString("make"));
        vehicle.setModel(rs.getString("model"));
        vehicle.setYear(rs.getInt("year"));
        vehicle.setLicensePlate(rs.getString("license_plate"));
        vehicle.setVin(rs.getString("vin"));
        vehicle.setColor(rs.getString("color"));
        vehicle.setFuelType(FuelType.valueOf(rs.getString("fuel_type")));
        vehicle.setMileage(rs.getInt("mileage"));
        vehicle.setStatus(VehicleStatus.valueOf(rs.getString("status")));
        String type = rs.getString("vehicle_type");
        vehicle.setVehicleType(type == null ? null : VehicleType.valueOf(type));
        String transmission = rs.getString("transmission");
        vehicle.setTransmission(transmission == null ? null : Transmission.valueOf(transmission));
        vehicle.setEngineNumber(rs.getString("engine_number"));
        vehicle.setRegistrationCardNumber(rs.getString("registration_card_number"));
        vehicle.setObservations(rs.getString("observations"));
        vehicle.setLastMaintenanceMileage(getInteger(rs, "last_maintenance_mileage"));
        vehicle.setLastMaintenanceDate(getDate(rs, "last_maintenance_date"));
        vehicle.setNextMaintenanceMileage(getInteger(rs, "next_maintenance_mileage"));
        vehicle.setNextMaintenanceDate(getDate(rs, "next_maintenance_date"));
        return vehicle;
    }

    /**
     * Lee el propietario que trajo el JOIN. Si el cliente no existe, devuelve
     * uno con solo el id de {@code customer_id}.
     */
    private static Customer mapOwner(ResultSet rs) throws SQLException {
        if (rs.getObject(OWNER_PREFIX + "id") != null) {
            return CustomerDao.map(rs, OWNER_PREFIX);
        }
        Customer owner = new Customer();
        owner.setId(rs.getInt("customer_id"));
        return owner;
    }

    /** Antepone el alias de tabla a cada columna de la lista. */
    private static String prefixed(String tableAlias, String columns) {
        StringBuilder sql = new StringBuilder();
        for (String column : columns.split(",\\s*")) {
            if (sql.length() > 0) {
                sql.append(", ");
            }
            sql.append(tableAlias).append('.').append(column);
        }
        return sql.toString();
    }

    /** Lee un entero que puede ser nulo. */
    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /** Lee una fecha que puede ser nula. */
    private static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }
}
