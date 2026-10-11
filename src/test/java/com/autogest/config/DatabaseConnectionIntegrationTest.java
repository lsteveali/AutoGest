package com.autogest.config;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración real contra PostgreSQL/Supabase. Comprueba que
 * {@link DatabaseConnection} abre una conexión y ejecuta una consulta.
 *
 * <p>Requiere un {@code application.local.properties} con credenciales válidas.
 * Está etiquetada como {@code integration}, por lo que queda excluida de
 * {@code mvn clean test} para que el build no dependa de la base de datos.
 * Para ejecutarla: {@code mvn test -DexcludedGroups= -Dgroups=integration}.</p>
 *
 * <p>Principles: (prueba de integración).</p>
 */
@Tag("integration")
class DatabaseConnectionIntegrationTest {

    @Test
    void connectsAndRunsSelectOne() throws Exception {
        DatabaseConnection databaseConnection = new DatabaseConnection();
        try (Connection connection = databaseConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next(), "La consulta SELECT 1 debe devolver una fila");
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
