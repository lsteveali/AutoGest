package com.autogest.controller.vehicle;

import com.autogest.config.AppContext;
import com.autogest.config.DatabaseConnection;
import com.autogest.model.Vehicle;
import com.autogest.service.VehicleService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de humo temporal de las pantallas del módulo de vehículos: carga los
 * cuatro FXML con el toolkit JavaFX iniciado y comprueba que los controladores
 * construyen su contenido sin excepción. Se omite cuando no hay base de datos
 * disponible, porque la lista y el detalle consultan datos reales al cargarse.
 * Necesita toolkit JavaFX y base de datos, por eso va con la etiqueta
 * {@code integration} (excluida por defecto en {@code mvn clean test}).
 */
@Tag("integration")
class VehicleScreensSmokeTest {

    /** Acción que puede lanzar excepciones (la carga de FXML lanza {@code IOException}). */
    @FunctionalInterface
    private interface FxAction {
        void run() throws Exception;
    }

    @AfterAll
    static void stopToolkit() {
        Platform.exit();
    }

    @Test
    void loadsAllVehicleScreens() throws Exception {
        Assumptions.assumeTrue(databaseReachable(), "sin base de datos disponible");
        startToolkit();

        for (String screen : SCREENS) {
            Exception error = onFxThread(() -> {
                URL url = VehicleScreensSmokeTest.class.getResource(screen);
                assertNotNull(url, "no se encontró " + screen);
                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();
                assertNotNull(root, "raíz de " + screen);
            });
            assertNull(error, () -> "No se pudo cargar " + screen + ": " + error);
        }
    }

    @Test
    void detailScreenBuildsItsContent() throws Exception {
        Assumptions.assumeTrue(databaseReachable(), "sin base de datos disponible");
        startToolkit();

        VehicleService service = AppContext.getInstance().getVehicleService();
        Vehicle any = service.searchVehicles(null, null, null, null).stream().findFirst().orElse(null);
        Assumptions.assumeTrue(any != null, "no hay vehículos en la base de datos");

        Exception error = onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(
                    VehicleScreensSmokeTest.class.getResource(VEHICLE_DETAIL));
            Parent root = loader.load();
            assertNotNull(root);
            VehicleDetailController controller = loader.getController();
            controller.receiveParameter(any.getId());
        });
        assertNull(error, () -> "No se pudo construir el detalle: " + error);
    }

    /** Ejecuta una acción en el hilo de JavaFX y devuelve la excepción, si la hubo. */
    private Exception onFxThread(FxAction action) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Exception e) {
                error.set(e);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(30, TimeUnit.SECONDS), "la operación excedió los 30 segundos");
        return error.get();
    }

    private static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // El toolkit ya quedó iniciado por otra prueba.
        }
    }

    private boolean databaseReachable() {
        try (java.sql.Connection connection = new DatabaseConnection().getConnection()) {
            return connection.isValid(5);
        } catch (Exception e) {
            return false;
        }
    }

    private static final String VEHICLE_DETAIL = "/fxml/vehicle/vehicle-detail.fxml";

    private static final List<String> SCREENS = List.of(
            "/fxml/vehicle/vehicles.fxml",
            "/fxml/vehicle/vehicle-new.fxml",
            "/fxml/vehicle/vehicle-edit.fxml",
            VEHICLE_DETAIL);
}
