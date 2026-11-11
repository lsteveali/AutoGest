package com.autogest.config;

import com.autogest.exception.DataAccessException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Properties;

/**
 * Fábrica de conexiones JDBC a la base de datos PostgreSQL alojada en Supabase.
 * Lee las credenciales del archivo {@code application.local.properties}
 * (ignorado por Git, decisión de seguridad) con las claves {@code db.url},
 * {@code db.user} y {@code db.password}. Las credenciales nunca se escriben en
 * el código.
 *
 * <p>Las conexiones salen de un pool (HikariCP): abrir una conexión nueva con
 * Supabase cuesta cerca de un segundo (red, SSL y autenticación), así que el
 * pool las mantiene abiertas y las reutiliza. El llamador sigue usando
 * try-with-resources; al cerrar, la conexión vuelve al pool en lugar de
 * cerrarse. Si falta el archivo o alguna clave, o falla la conexión, se lanza
 * {@link DataAccessException} con un mensaje claro para el usuario.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations, High Cohesion,
 * Low Coupling.</p>
 */
public class DatabaseConnection {

    private static final String CONFIG_FILE = "application.local.properties";
    private static final String URL_KEY = "db.url";
    private static final String USER_KEY = "db.user";
    private static final String PASSWORD_KEY = "db.password";

    /** Conexiones abiertas como máximo; el pooler de Supabase limita los clientes. */
    private static final int MAX_POOL_SIZE = 4;

    private final HikariDataSource dataSource;

    /**
     * Carga la configuración de la base de datos desde
     * {@code application.local.properties} y prepara el pool. El pool no
     * bloquea al crearse: abre su primera conexión en segundo plano.
     *
     * @throws DataAccessException si falta el archivo o alguna clave obligatoria
     */
    public DatabaseConnection() {
        Properties properties = loadProperties();
        HikariConfig config = new HikariConfig();
        config.setPoolName("AutoGest");
        config.setJdbcUrl(require(properties, URL_KEY));
        config.setUsername(require(properties, USER_KEY));
        config.setPassword(require(properties, PASSWORD_KEY));
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(Duration.ofSeconds(15).toMillis());
        config.setIdleTimeout(Duration.ofMinutes(5).toMillis());
        config.setKeepaliveTime(Duration.ofMinutes(2).toMillis());
        config.setMaxLifetime(Duration.ofMinutes(15).toMillis());
        // -1: no intenta conectar al crear el pool; los errores salen en getConnection().
        config.setInitializationFailTimeout(-1);
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Toma una conexión del pool (o abre una si no hay libres).
     *
     * @return conexión JDBC lista para usarse (el llamador debe cerrarla)
     * @throws DataAccessException si no se puede establecer la conexión
     */
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar con la base de datos de AutoGest.", e);
        }
    }

    /** Cierra todas las conexiones del pool. Se llama al salir de la aplicación. */
    public void close() {
        dataSource.close();
    }

    /** Lee el archivo de configuración local o, si no existe, el del classpath. */
    private static Properties loadProperties() {
        Path file = Paths.get(CONFIG_FILE);
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                return read(input);
            } catch (IOException e) {
                throw new DataAccessException("No se pudo leer el archivo " + CONFIG_FILE + ".", e);
            }
        }
        try (InputStream input = DatabaseConnection.class.getResourceAsStream("/" + CONFIG_FILE)) {
            if (input == null) {
                throw new DataAccessException(
                        "No se encontró " + CONFIG_FILE + ". Copia application.example.properties "
                                + "a application.local.properties y completa las credenciales.");
            }
            return read(input);
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer el archivo " + CONFIG_FILE + ".", e);
        }
    }

    /** Carga las propiedades desde el flujo indicado. */
    private static Properties read(InputStream input) throws IOException {
        Properties properties = new Properties();
        properties.load(input);
        return properties;
    }

    /** Exige que la clave exista y no esté vacía. */
    private static String require(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DataAccessException("Falta la clave " + key + " en " + CONFIG_FILE + ".");
        }
        return value;
    }
}
