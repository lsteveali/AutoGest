package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.model.Diagnosis;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Acceso SQL a la tabla {@code diagnoses} (relación 1 a 0..1 con la orden).
 * El diagnóstico se identifica por su orden ({@code order_id UNIQUE}).
 *
 * <p>Principles: Pure Fabrication, Protected Variations, ISP.</p>
 */
public class DiagnosisDao {

    private final DatabaseConnection connection;

    /**
     * @param connection fábrica de conexiones a Supabase
     */
    public DiagnosisDao(DatabaseConnection connection) {
        this.connection = connection;
    }

    /**
     * Guarda o reemplaza el diagnóstico de una orden.
     *
     * @param orderId   id de la orden
     * @param diagnosis diagnóstico a guardar
     */
    public void save(int orderId, Diagnosis diagnosis) {
        String sql = "INSERT INTO diagnoses (order_id, description, recommendations, date) VALUES (?, ?, ?, ?)"
                + " ON CONFLICT (order_id) DO UPDATE SET description = EXCLUDED.description,"
                + " recommendations = EXCLUDED.recommendations, date = EXCLUDED.date";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, diagnosis.getDescription());
            ps.setString(3, diagnosis.getRecommendations());
            LocalDate date = diagnosis.getDate() == null ? LocalDate.now() : diagnosis.getDate();
            ps.setDate(4, Date.valueOf(date));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo guardar el diagnóstico.", e);
        }
    }

    /**
     * Diagnóstico de una orden.
     *
     * @return el diagnóstico, o {@code null} si la orden aún no tiene
     */
    public Diagnosis findByOrder(int orderId) {
        String sql = "SELECT description, recommendations, date FROM diagnoses WHERE order_id = ?";
        try (Connection conn = connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setDescription(rs.getString("description"));
                diagnosis.setRecommendations(rs.getString("recommendations"));
                Date date = rs.getDate("date");
                diagnosis.setDate(date == null ? null : date.toLocalDate());
                return diagnosis;
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo consultar el diagnóstico.", e);
        }
    }
}
