package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Part;
import com.autogest.model.StockStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso SQL al inventario de repuestos.
 */
public class PartDao implements CrudOperations<Part> {

    private static final String COLUMNS = "id, code, name, category, current_stock, minimum_stock, unit_price, status, supplier_id";

    private final DatabaseConnection connection;

    public PartDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    @Override
    public Part create(Part entity) {
        String sql = "INSERT INTO parts (code, name, category, current_stock, minimum_stock, unit_price, status, supplier_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
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
            throw new DataAccessException("No se pudo crear el repuesto.", e);
        }
    }

    @Override
    public Part findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM parts WHERE id = ?";
        return query(sql, List.of(id)).stream().findFirst().orElse(null);
    }

    @Override
    public List<Part> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM parts ORDER BY name";
        return query(sql, List.of());
    }

    @Override
    public boolean update(Part entity) {
        String sql = "UPDATE parts SET code = ?, name = ?, category = ?, current_stock = ?, minimum_stock = ?, unit_price = ?, status = ?, supplier_id = ? WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, entity);
            ps.setInt(9, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo actualizar el repuesto.", e);
        }
    }

    @Override
    public boolean delete(int id) {
        // Baja lógica no requerida explícita; mantenemos compatibilidad
        String sql = "DELETE FROM parts WHERE id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo eliminar el repuesto.", e);
        }
    }

    public List<Part> search(String text, StockStatus status, String category) {
        StringBuilder sql = new StringBuilder("SELECT ").append(COLUMNS).append(" FROM parts");
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            conditions.add("(name ILIKE ? OR code ILIKE ?)");
            String like = "%" + escapeLike(text.trim()) + "%";
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

    public boolean existsByCode(String code) {
        String sql = "SELECT COUNT(*) FROM parts WHERE code = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo validar el código del repuesto.", e);
        }
    }

    public boolean existsByCodeExcluding(String code, int id) {
        String sql = "SELECT COUNT(*) FROM parts WHERE code = ? AND id <> ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo validar el código del repuesto.", e);
        }
    }

    public int countLowStock() {
        String sql = "SELECT COUNT(*) FROM parts WHERE status = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, StockStatus.LOW_STOCK.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar repuestos con stock bajo.", e);
        }
    }

    public int countOutOfStock() {
        String sql = "SELECT COUNT(*) FROM parts WHERE status = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, StockStatus.OUT_OF_STOCK.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo contar repuestos agotados.", e);
        }
    }

    private void bind(PreparedStatement ps, Part entity) throws SQLException {
        ps.setString(1, entity.getCode());
        ps.setString(2, entity.getName());
        ps.setString(3, entity.getCategory());
        ps.setInt(4, entity.getCurrentStock());
        ps.setInt(5, entity.getMinimumStock());
        ps.setBigDecimal(6, entity.getUnitPrice());
        ps.setString(7, entity.getStatus() == null ? StockStatus.AVAILABLE.name() : entity.getStatus().name());
        if (entity.getSupplier() == null) {
            ps.setObject(8, null);
        } else {
            ps.setInt(8, entity.getSupplier().getId());
        }
    }

    private List<Part> query(String sql, List<Object> params) {
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<Part> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudieron consultar los repuestos.", e);
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private Part map(ResultSet rs) throws SQLException {
        Part part = new Part();
        part.setId(rs.getInt("id"));
        part.setCode(rs.getString("code"));
        part.setName(rs.getString("name"));
        part.setCategory(rs.getString("category"));
        part.setCurrentStock(rs.getInt("current_stock"));
        part.setMinimumStock(rs.getInt("minimum_stock"));
        part.setUnitPrice(rs.getBigDecimal("unit_price"));
        // status is derived in model but we can set; model recalculates on setters
        return part;
    }
}
