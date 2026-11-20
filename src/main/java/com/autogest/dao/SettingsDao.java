package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Acceso SQL a la tabla clave/valor {@code app_settings} (script
 * {@code docs/database/sprint8-settings.sql}). Las entradas se identifican por
 * su clave de texto, no por un id entero, por eso no implementa
 * {@code CrudOperations} (ISP).
 *
 * <p>Principles: Pure Fabrication, Protected Variations, ISP.</p>
 */
public class SettingsDao {

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public SettingsDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /**
     * Ajustes cuya clave empieza por el prefijo (por ejemplo {@code "workshop."}).
     *
     * @param prefix prefijo de las claves
     * @return clave → valor (vacío si no hay)
     */
    public Map<String, String> findByPrefix(String prefix) {
        String sql = "SELECT setting_key, setting_value FROM app_settings WHERE setting_key LIKE ? ORDER BY setting_key";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, prefix + "%");
            try (ResultSet rs = ps.executeQuery()) {
                Map<String, String> values = new LinkedHashMap<>();
                while (rs.next()) {
                    values.put(rs.getString(1), rs.getString(2));
                }
                return values;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo leer la configuración.", e);
        }
    }

    /**
     * Guarda (inserta o reemplaza) varios ajustes en una sola transacción.
     *
     * @param values clave → valor ({@code null} guarda un valor vacío)
     */
    public void saveAll(Map<String, String> values) {
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)"
                + " ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value";
        try (Connection conn = connection.getConnection()) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    ps.setString(1, entry.getKey());
                    ps.setString(2, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar la configuración.", e);
        }
    }
}
