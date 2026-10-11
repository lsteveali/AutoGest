package com.autogest;

import com.autogest.controller.MainController;
import com.autogest.controller.user.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Punto de entrada de la aplicación de escritorio AutoGest.
 *
 * <p>Arranca en la pantalla de inicio de sesión ({@code /fxml/login.fxml}) y,
 * tras autenticar al usuario, muestra el shell ({@code /fxml/main.fxml}) con la
 * Sidebar, la Topbar y el área central donde el {@code ViewNavigator} cambia de
 * pantalla. Al cerrar sesión vuelve al login. La ventana tiene tamaño 1280x720
 * (RNF-12) y ambas escenas usan {@code autogest.css}.</p>
 *
 * <p>Principles: High Cohesion, Indirection.</p>
 */
public class AutoGestApplication extends Application {

    private static final String LOGIN_FXML = "/fxml/login.fxml";
    private static final String SHELL_FXML = "/fxml/main.fxml";
    private static final String STYLESHEET = "/css/autogest.css";

    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("AutoGest");
        showLogin();
        stage.show();
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
