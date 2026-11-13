package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Lee la base de datos completa para una copia de seguridad. Descubre las
 * tablas en el catálogo de PostgreSQL (así las tablas nuevas, como
 * {@code app_settings}, entran solas) y las lee todas con una sola conexión
 * de solo lectura en {@code REPEATABLE READ}: la copia es una foto coherente
 * aunque alguien esté trabajando en el taller.
 *
 * <p>No implementa {@code CrudOperations}: no hay entidad con id (ISP).</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations (el SQL solo vive aquí).</p>
 */
public class BackupDao {

    private static final String TABLES_SQL = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name";

    private final DatabaseConnection databaseConnection;

    /**
     * @param databaseConnection fábrica de conexiones compartida
     */
    public BackupDao(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    /**
     * Lee todas las tablas del esquema {@code public}.
     *
     * @return una instantánea por tabla, en orden alfabético
     * @throws DataAccessException si la lectura falla
     */
    public List<TableSnapshot> readAllTables() {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            int isolation = connection.getTransactionIsolation();
            boolean readOnly = connection.isReadOnly();
            try {
                connection.setAutoCommit(false);
                connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                connection.setReadOnly(true);

                List<TableSnapshot> tables = new ArrayList<>();
                for (String table : tableNames(connection)) {
                    tables.add(readTable(connection, table));
                }
                connection.commit();
                return tables;
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                // La conexión vuelve al pool: se deja como estaba.
                connection.setReadOnly(readOnly);
                connection.setTransactionIsolation(isolation);
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo leer la base de datos para la copia de seguridad.", e);
        }
    }

    private static List<String> tableNames(Connection connection) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(TABLES_SQL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                names.add(rs.getString(1));
            }
        }
        return names;
    }

    private static TableSnapshot readTable(Connection connection, String table) throws SQLException {
        // El nombre viene del catálogo, no del usuario; igual se cita como identificador.
        String sql = "SELECT * FROM " + quoteIdentifier(table);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();
            List<String> columns = new ArrayList<>(count);
            for (int i = 1; i <= count; i++) {
                columns.add(meta.getColumnName(i));
            }
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>(count);
                for (int i = 1; i <= count; i++) {
                    row.add(rs.getString(i));
                }
                rows.add(row);
            }
            return new TableSnapshot(table, columns, rows);
        }
    }

    /** Cita un identificador de PostgreSQL duplicando las comillas internas. */
    static String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
