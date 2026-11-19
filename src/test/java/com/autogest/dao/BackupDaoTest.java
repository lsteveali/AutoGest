package com.autogest.dao;

import com.autogest.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la parte de {@link BackupDao} que no necesita base de datos:
 * el orden de carga según las llaves foráneas y los mensajes de error.
 */
class BackupDaoTest {

    /** Llaves foráneas del esquema de AutoGest ({hija, padre}). */
    private static final List<String[]> SCHEMA = List.of(
            fk("vehicles", "customers"),
            fk("parts", "suppliers"),
            fk("service_offering_parts", "service_offerings"),
            fk("service_offering_parts", "parts"),
            fk("service_orders", "vehicles"),
            fk("service_orders", "users"),
            fk("diagnoses", "service_orders"),
            fk("order_history", "service_orders"),
            fk("order_history", "users"),
            fk("order_lines", "service_orders"),
            fk("order_lines", "service_offerings"),
            fk("order_lines", "parts"),
            fk("inventory_movements", "parts"),
            fk("inventory_movements", "users"),
            fk("invoices", "service_orders"),
            fk("notifications", "service_orders"),
            fk("notifications", "customers"));

    @Test
    void parentsAreLoadedBeforeTheirChildren() {
        Set<String> tables = Set.of("customers", "vehicles", "suppliers", "service_offerings", "parts",
                "service_offering_parts", "service_orders", "users", "diagnoses", "order_history",
                "order_lines", "inventory_movements", "invoices", "notifications", "app_settings");

        List<String> order = BackupDao.restoreOrder(tables, SCHEMA);

        assertEquals(tables.size(), order.size());
        for (String[] edge : SCHEMA) {
            assertTrue(order.indexOf(edge[1]) < order.indexOf(edge[0]),
                    edge[1] + " debe cargarse antes que " + edge[0]);
        }
    }

    @Test
    void orderIsAlphabeticalWhenThereAreNoDependencies() {
        assertEquals(List.of("app_settings", "customers", "users"),
                BackupDao.restoreOrder(Set.of("users", "customers", "app_settings"), List.of()));
    }

    @Test
    void dependenciesOutsideTheBackupAndSelfReferencesAreIgnored() {
        List<String> order = BackupDao.restoreOrder(Set.of("vehicles", "employees"),
                List.of(fk("vehicles", "customers"), fk("employees", "employees")));

        assertEquals(List.of("employees", "vehicles"), order);
    }

    @Test
    void cyclesAreRejected() {
        assertThrows(ValidationException.class, () -> BackupDao.restoreOrder(Set.of("a", "b"),
                List.of(fk("a", "b"), fk("b", "a"))));
    }

    @Test
    void errorTextExplainsThatNothingChanged() {
        assertTrue(BackupDao.restoreErrorText(new SQLException("fk", "23503")).contains("dependen"));
        assertTrue(BackupDao.restoreErrorText(new SQLException("tipo", "22P02")).contains("no encajan"));
        assertTrue(BackupDao.restoreErrorText(new SQLException("red", "08006")).contains("conexión"));
        assertTrue(BackupDao.restoreErrorText(new SQLException("sin estado")).startsWith("No se restauró nada"));
    }

    @Test
    void quoteIdentifierEscapesQuotes() {
        assertEquals("\"order\"", BackupDao.quoteIdentifier("order"));
        assertEquals("\"a\"\"b\"", BackupDao.quoteIdentifier("a\"b"));
    }

    private static String[] fk(String child, String parent) {
        return new String[] {child, parent};
    }
}
