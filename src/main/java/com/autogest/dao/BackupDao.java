package com.autogest.dao;

import com.autogest.config.DatabaseConnection;
import com.autogest.exception.DataAccessException;
import com.autogest.exception.ValidationException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Lee la base de datos completa para una copia de seguridad. Descubre las
 * tablas en el catálogo de PostgreSQL (así las tablas nuevas, como
 * {@code app_settings}, entran solas) y las lee todas con una sola conexión
 * de solo lectura en {@code REPEATABLE READ}: la copia es una foto coherente
 * aunque alguien esté trabajando en el taller.
 *
 * <p>También restaura una copia ({@link #restoreAllTables(List)}): vacía y
 * recarga las tablas en orden de dependencias dentro de una transacción.</p>
 *
 * <p>No implementa {@code CrudOperations}: no hay entidad con id (ISP).</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations (el SQL solo vive aquí).</p>
 */
public class BackupDao {

    private static final String TABLES_SQL = "SELECT table_name FROM information_schema.tables "
            + "WHERE table_schema = 'public' AND table_type = 'BASE TABLE' ORDER BY table_name";

    /** Columnas de cada tabla con su tipo, si son identidad o calculadas y su secuencia. */
    private static final String COLUMNS_SQL = "SELECT c.relname, a.attname, "
            + "format_type(a.atttypid, a.atttypmod), a.attidentity::text, a.attgenerated::text, "
            + "pg_get_serial_sequence(quote_ident(c.relname), a.attname) "
            + "FROM pg_attribute a "
            + "JOIN pg_class c ON c.oid = a.attrelid "
            + "JOIN pg_namespace n ON n.oid = c.relnamespace "
            + "WHERE n.nspname = 'public' AND c.relkind = 'r' AND a.attnum > 0 AND NOT a.attisdropped "
            + "ORDER BY c.relname, a.attnum";

    /** Pares {hija, padre} de las llaves foráneas entre tablas del esquema public. */
    private static final String FOREIGN_KEYS_SQL = "SELECT child.relname, parent.relname "
            + "FROM pg_constraint k "
            + "JOIN pg_class child ON child.oid = k.conrelid "
            + "JOIN pg_class parent ON parent.oid = k.confrelid "
            + "JOIN pg_namespace n ON n.oid = child.relnamespace "
            + "WHERE k.contype = 'f' AND n.nspname = 'public'";

    /** Filas por lote al insertar: pocas idas y vueltas sin armar lotes enormes. */
    private static final int BATCH_SIZE = 500;

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

    /**
     * Reemplaza el contenido de las tablas de la copia, en una sola
     * transacción: si algo falla, la base de datos queda exactamente como
     * estaba. Bloquea las tablas mientras tanto, las vacía de hijas a padres,
     * las carga de padres a hijas (el orden sale de las llaves foráneas del
     * catálogo), conserva los ids originales y ajusta los contadores de ids
     * para que los registros nuevos sigan después del mayor.
     *
     * <p>Las tablas de la base que no vienen en la copia (por ejemplo, una
     * tabla agregada después) no se tocan.</p>
     *
     * @param tables contenido de la copia
     * @throws com.autogest.exception.ValidationException si la copia tiene tablas o columnas que la base no tiene
     * @throws DataAccessException si la carga falla (no queda ningún cambio)
     */
    public void restoreAllTables(List<TableSnapshot> tables) {
        try (Connection connection = databaseConnection.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                restoreInto(connection, tables);
                connection.commit();
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException e) {
            throw new DataAccessException(restoreErrorText(e), e);
        }
    }

    /**
     * Hace la restauración sobre una conexión con la transacción ya abierta,
     * sin confirmarla: quien llama decide si confirma o deshace.
     */
    static void restoreInto(Connection connection, List<TableSnapshot> tables) throws SQLException {
        restoreInto(connection, tables, true);
    }

    /**
     * @param resetSequences {@code false} solo para ensayos que deshacen la
     *                       transacción: {@code setval} no se deshace con un rollback
     */
    static void restoreInto(Connection connection, List<TableSnapshot> tables, boolean resetSequences)
            throws SQLException {
        Map<String, Map<String, ColumnInfo>> catalog = columns(connection);
        Map<String, TableSnapshot> byName = new LinkedHashMap<>();
        for (TableSnapshot table : tables) {
            Map<String, ColumnInfo> existing = catalog.get(table.name());
            if (existing == null) {
                throw new ValidationException("La copia trae la tabla «" + table.name()
                        + "», que no existe en la base de datos.");
            }
            for (String column : table.columns()) {
                if (!existing.containsKey(column)) {
                    throw new ValidationException("La copia trae la columna «" + column + "» en «"
                            + table.name() + "», que no existe en la base de datos.");
                }
            }
            byName.put(table.name(), table);
        }
        List<String> order = restoreOrder(byName.keySet(), foreignKeys(connection));

        try (Statement statement = connection.createStatement()) {
            statement.execute("LOCK TABLE " + String.join(", ", order.stream()
                    .map(BackupDao::quoteIdentifier).toList()) + " IN ACCESS EXCLUSIVE MODE");
            for (int i = order.size() - 1; i >= 0; i--) {
                statement.executeUpdate("DELETE FROM " + quoteIdentifier(order.get(i)));
            }
        }
        for (String name : order) {
            Map<String, ColumnInfo> columns = catalog.get(name);
            insertRows(connection, byName.get(name), columns);
            if (resetSequences) {
                resetSequences(connection, name, columns);
            }
        }
    }

    /**
     * Orden de carga: cada tabla después de las tablas de las que depende
     * (orden topológico; a igualdad, alfabético). Las dependencias con tablas
     * fuera de la copia y las de una tabla consigo misma no cuentan.
     *
     * @param tables      tablas a restaurar
     * @param foreignKeys pares {hija, padre} de las llaves foráneas
     * @return las tablas de padres a hijas
     * @throws ValidationException si hay un ciclo de dependencias
     */
    static List<String> restoreOrder(Set<String> tables, List<String[]> foreignKeys) {
        Map<String, Set<String>> parents = new TreeMap<>();
        for (String table : tables) {
            parents.put(table, new TreeSet<>());
        }
        for (String[] edge : foreignKeys) {
            String child = edge[0];
            String parent = edge[1];
            if (!child.equals(parent) && parents.containsKey(child) && parents.containsKey(parent)) {
                parents.get(child).add(parent);
            }
        }
        List<String> order = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        while (order.size() < parents.size()) {
            String next = null;
            for (Map.Entry<String, Set<String>> entry : parents.entrySet()) {
                if (!placed.contains(entry.getKey()) && placed.containsAll(entry.getValue())) {
                    next = entry.getKey();
                    break;
                }
            }
            if (next == null) {
                throw new ValidationException("Las tablas de la base dependen unas de otras en círculo; "
                        + "la copia no se puede restaurar desde AutoGest.");
            }
            order.add(next);
            placed.add(next);
        }
        return order;
    }

    private static void insertRows(Connection connection, TableSnapshot table, Map<String, ColumnInfo> columns)
            throws SQLException {
        if (table.rows().isEmpty()) {
            return;
        }
        // Las columnas calculadas (GENERATED ... STORED) no se insertan: la base las recalcula.
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < table.columns().size(); i++) {
            if (!columns.get(table.columns().get(i)).generated()) {
                indexes.add(i);
            }
        }
        boolean identityAlways = indexes.stream()
                .anyMatch(i -> columns.get(table.columns().get(i)).identityAlways());
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(quoteIdentifier(table.name())).append(" (");
        StringBuilder values = new StringBuilder();
        for (int k = 0; k < indexes.size(); k++) {
            String column = table.columns().get(indexes.get(k));
            if (k > 0) {
                sql.append(", ");
                values.append(", ");
            }
            sql.append(quoteIdentifier(column));
            // Los valores vienen como texto; el CAST al tipo de la columna los convierte.
            values.append("CAST(? AS ").append(columns.get(column).type()).append(')');
        }
        sql.append(')');
        if (identityAlways) {
            // Conserva los ids de la copia aunque la columna sea GENERATED ALWAYS.
            sql.append(" OVERRIDING SYSTEM VALUE");
        }
        sql.append(" VALUES (").append(values).append(')');

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int pending = 0;
            for (List<String> row : table.rows()) {
                for (int k = 0; k < indexes.size(); k++) {
                    statement.setString(k + 1, row.get(indexes.get(k)));
                }
                statement.addBatch();
                if (++pending == BATCH_SIZE) {
                    statement.executeBatch();
                    pending = 0;
                }
            }
            if (pending > 0) {
                statement.executeBatch();
            }
        }
    }

    /** Deja cada contador de ids justo después del mayor id cargado. */
    private static void resetSequences(Connection connection, String table, Map<String, ColumnInfo> columns)
            throws SQLException {
        for (Map.Entry<String, ColumnInfo> entry : columns.entrySet()) {
            String sequence = entry.getValue().sequence();
            if (sequence == null) {
                continue;
            }
            String sql = "SELECT setval(CAST(? AS regclass), COALESCE((SELECT MAX("
                    + quoteIdentifier(entry.getKey()) + ") FROM " + quoteIdentifier(table) + "), 0) + 1, false)";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, sequence);
                statement.executeQuery().close();
            }
        }
    }

    private static Map<String, Map<String, ColumnInfo>> columns(Connection connection) throws SQLException {
        Map<String, Map<String, ColumnInfo>> catalog = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(COLUMNS_SQL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                catalog.computeIfAbsent(rs.getString(1), table -> new LinkedHashMap<>())
                        .put(rs.getString(2), new ColumnInfo(rs.getString(3), "a".equals(rs.getString(4)),
                                !rs.getString(5).isEmpty(), rs.getString(6)));
            }
        }
        return catalog;
    }

    private static List<String[]> foreignKeys(Connection connection) throws SQLException {
        List<String[]> edges = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(FOREIGN_KEYS_SQL);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                edges.add(new String[] {rs.getString(1), rs.getString(2)});
            }
        }
        return edges;
    }

    /** Mensaje para el usuario según el error de PostgreSQL. */
    static String restoreErrorText(SQLException e) {
        String state = e.getSQLState() == null ? "" : e.getSQLState();
        if (state.equals("23503")) {
            return "No se restauró nada: hay registros en la base que dependen de datos que la copia no trae.";
        }
        if (state.startsWith("22") || state.startsWith("23")) {
            return "No se restauró nada: los datos de la copia no encajan con la base de datos actual.";
        }
        return "No se restauró nada: falló la conexión con la base de datos. Inténtalo de nuevo.";
    }

    /**
     * Columna del catálogo: tipo SQL, si el id es GENERATED ALWAYS, si es
     * calculada y la secuencia que le da valores ({@code null} si no tiene).
     */
    private record ColumnInfo(String type, boolean identityAlways, boolean generated, String sequence) {
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
