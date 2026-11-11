package com.autogest;

import com.autogest.config.AppContext;
import com.autogest.controller.MainController;
import com.autogest.controller.user.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Punto de entrada de la aplicación de escritorio AutoGest.
 *
 * <p>Arranca en la pantalla de inicio de sesión ({@code /fxml/login.fxml}) y,
 * tras autenticar al usuario, muestra el shell ({@code /fxml/main.fxml}) con la
 * Sidebar, la Topbar y el área central donde el {@code ViewNavigator} cambia de
 * pantalla. Al cerrar sesión vuelve al login. La ventana tiene tamaño 1280x720
 * (RNF-12) y ambas escenas usan {@code autogest.css}.</p>
 *
 * <p>Antes de construir la primera escena carga las fuentes Roboto incluidas en
 * el tema ({@code /css/fonts}, decisión D-18) para que el CSS pueda usarlas
 * aunque no estén instaladas en el sistema.</p>
 *
 * <p>Principles: High Cohesion, Indirection.</p>
 */
public class AutoGestApplication extends Application {

    private static final String LOGIN_FXML = "/fxml/login.fxml";
    private static final String SHELL_FXML = "/fxml/main.fxml";
    private static final String STYLESHEET = "/css/autogest.css";

    /**
     * Fuentes Roboto incluidas en el tema (D-18). JavaFX solo distingue la cara
     * normal y la negrita de una familia; por eso Regular se carga después de
     * Medium: así el peso normal resuelve a Regular y la negrita a Bold (el peso
     * 500 del CSS se dibuja con Regular).
     */
    private static final String[] ROBOTO_FONTS = {
            "/css/fonts/Roboto-Medium.ttf",
            "/css/fonts/Roboto-Regular.ttf",
            "/css/fonts/Roboto-Bold.ttf"
    };

    /** Tuerca del logo en varios tamaños para la ventana y la barra de tareas. */
    private static final String[] APP_ICONS = {
            "/images/logo-gear-32.png",
            "/images/logo-gear-64.png",
            "/images/logo-gear-256.png"
    };

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("AutoGest");
        loadAppIcons();
        loadFonts();
        showLogin();
        stage.show();
        warmUpDatabase();
    }

    /** Cierra el pool de conexiones con la base de datos al salir. */
    @Override
    public void stop() {
        AppContext.shutdown();
    }

    /**
     * Crea el contexto en segundo plano mientras el usuario escribe sus datos,
     * para que el pool abra la primera conexión con Supabase antes del login.
     * Si falla (sin red o sin configuración), el login mostrará el error.
     */
    private void warmUpDatabase() {
        Thread warmUp = new Thread(() -> {
            try {
                AppContext.getInstance();
            } catch (RuntimeException ignored) {
                // El error se informa al intentar iniciar sesión.
            }
        }, "autogest-db-warmup");
        warmUp.setDaemon(true);
        warmUp.start();
    }

    /**
     * Carga las fuentes Roboto del tema con {@code Font.loadFont} (D-18) antes de
     * crear la primera escena. Si un recurso falta, la aplicación continúa con la
     * fuente de reserva declarada en el CSS.
     */
    private void loadFonts() {
        for (String resource : ROBOTO_FONTS) {
            try (InputStream stream = getClass().getResourceAsStream(resource)) {
                if (stream != null) {
                    Font.loadFont(stream, 12);
                }
            } catch (IOException e) {
                // Fuente de reserva: el CSS usa "Segoe UI" si Roboto no carga.
            }
        }
    }

    /** Pone el logo como ícono de la ventana; si falta un recurso, se omite. */
    private void loadAppIcons() {
        for (String resource : APP_ICONS) {
            URL url = getClass().getResource(resource);
            if (url != null) {
                stage.getIcons().add(new Image(url.toExternalForm()));
            }
        }
    }

    /** Carga y muestra la pantalla de inicio de sesión. */
    private void showLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(LOGIN_FXML));
            Parent root = loader.load();
            LoginController controller = loader.getController();
            controller.setOnLoginSuccess(this::showShell);
            setScene(root);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir la pantalla de inicio de sesión.", e);
        }
    }

    /** Carga y muestra el shell de la aplicación con el usuario ya autenticado. */
    private void showShell() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(SHELL_FXML));
            Parent root = loader.load();
            MainController controller = loader.getController();
            controller.setOnLogout(this::showLogin);
            setScene(root);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo abrir la aplicación.", e);
        }
    }

    /** Reemplaza el contenido de la ventana por la raíz indicada y aplica el CSS. */
    private void setScene(Parent root) {
        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
