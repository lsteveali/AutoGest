package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL al catálogo de servicios. Implementa {@link CrudOperations}.
 */
public class ServiceOfferingDao implements CrudOperations<ServiceOffering> {

    private static final String COLUMNS = "id, code, name, category, description, duration_hours, price, status";

    private final DatabaseConnection connection;

    public ServiceOfferingDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    @Override
    public ServiceOffering create(ServiceOffering entity) {
        String sql = "INSERT INTO service_offerings (code, name, category, description, duration_hours, price, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindCreate(ps, entity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getInt(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo crear el servicio.", e);
        }
    }

    @Override
    public ServiceOffering findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM service_offerings WHERE id = ?";
        return query(sql, List.of(id)).stream().findFirst().orElse(null);
    }

    @Override
    public List<ServiceOffering> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM service_offerings ORDER BY name";
        return query(sql, List.of());
    }

    @Override
    public boolean update(ServiceOffering entity) {
        String sql = "UPDATE service_offerings SET code = ?, name = ?, category = ?, description = ?, duration_hours = ?, price = ?, status = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindCreate(ps, entity);
            ps.setInt(8, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el servicio.", e);
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "UPDATE service_offerings SET status = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ServiceOfferingStatus.INACTIVE.name());
            ps.setInt(2, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo desactivar el servicio.", e);
        }
    }

    public List<ServiceOffering> search(String text, ServiceOfferingStatus status, String category) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM service_offerings");
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            conditions.add("(name ILIKE ? OR code ILIKE ? OR description ILIKE ?)");
            String like = "%" + escapeLike(text.trim()) + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (status != null) {
            conditions.add("status = ?");
            params.add(status.name());
        }
        if (category != null && !category.isBlank()) {
            conditions.add("category = ?");
            params.add(category.trim());
        }
        if (!conditions.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        sql.append(" ORDER BY name");
        return query(sql.toString(), params);
    }

    public List<String> findCategories() {
        String sql = "SELECT DISTINCT category FROM service_offerings WHERE category IS NOT NULL AND category <> '' ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
            return categories;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar las categorías de servicios.", e);
        }
    }

    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(*) FROM service_offerings WHERE code = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo validar el código del servicio.", e);
        }
    }

    public boolean existsByCodeExcluding(String code, int id) {
        String sql = "SELECT COUNT(*) FROM service_offerings WHERE code = ? AND id <> ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo validar el código del servicio.", e);
        }
    }

    private void bindCreate(PreparedStatement ps, ServiceOffering entity) throws SQLException {
        ps.setString(1, entity.getCode());
        ps.setString(2, entity.getName());
        ps.setString(3, entity.getCategory());
        ps.setString(4, entity.getDescription());
        ps.setBigDecimal(5, BigDecimal.valueOf(entity.getDurationHours()));
        ps.setBigDecimal(6, entity.getPrice());
        ps.setString(7, entity.getStatus() == null ? ServiceOfferingStatus.ACTIVE.name() : entity.getStatus().name());
    }

    private List<ServiceOffering> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<ServiceOffering> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los servicios.", e);
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private ServiceOffering map(ResultSet rs) throws SQLException {
        ServiceOffering service = new ServiceOffering();
        service.setId(rs.getInt("id"));
        service.setCode(rs.getString("code"));
        service.setName(rs.getString("name"));
        service.setCategory(rs.getString("category"));
        service.setDescription(rs.getString("description"));
        service.setDurationHours(rs.getBigDecimal("duration_hours").doubleValue());
        service.setPrice(rs.getBigDecimal("price"));
        service.setStatus(ServiceOfferingStatus.valueOf(rs.getString("status")));
        return service;
    }
}
