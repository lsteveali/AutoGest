package com.autogest.config;

import com.autogest.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Fábrica de conexiones JDBC a la base de datos PostgreSQL alojada en Supabase.
 * Lee las credenciales del archivo {@code application.local.properties}
 * (ignorado por Git, decisión de seguridad) con las claves {@code db.url},
 * {@code db.user} y {@code db.password}. Las credenciales nunca se escriben en
 * el código.
 *
 * <p>Cada llamada a {@link #getConnection()} abre una conexión nueva; el
 * llamador es responsable de cerrarla con try-with-resources. Si falta el
 * archivo o alguna clave, o falla la conexión, se lanza
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

    private final String url;
    private final String user;
    private final String password;

    /**
     * Carga la configuración de la base de datos desde
     * {@code application.local.properties}.
     *
     * @throws DataAccessException si falta el archivo o alguna clave obligatoria
     */
    public DatabaseConnection() {
        Properties properties = loadProperties();
        this.url = require(properties, URL_KEY);
        this.user = require(properties, USER_KEY);
        this.password = require(properties, PASSWORD_KEY);
    }

    /**
     * Abre una conexión nueva con la base de datos.
     *
     * @return conexión JDBC lista para usarse (el llamador debe cerrarla)
     * @throws DataAccessException si no se puede establecer la conexión
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DataAccessException("No se pudo conectar con la base de datos de AutoGest.", e);
        }
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
