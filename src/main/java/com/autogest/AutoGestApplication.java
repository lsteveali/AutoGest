package com.autogest;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point of the AutoGest desktop application.
 *
 * <p>Loads the application shell ({@code /fxml/main.fxml}) in a window with
 * minimum size 1280x720. The shell contains the Sidebar, the Topbar and the
 * central area where the {@code ViewNavigator} changes screens.</p>
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