package com.autogest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación de escritorio AutoGest.
 *
 * <p>Carga el shell de la aplicación ({@code /fxml/main.fxml}) en una ventana
 * con tamaño mínimo 1280x720. El shell contiene la Sidebar, la Topbar y el
 * área central donde el {@code ViewNavigator} cambia de pantalla.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public class AutoGestApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/main.fxml"));

        Scene scene = new Scene(root, 1280, 720);
        scene.getStylesheets().add(getClass().getResource("/css/autogest.css").toExternalForm());
        stage.setTitle("AutoGest");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}