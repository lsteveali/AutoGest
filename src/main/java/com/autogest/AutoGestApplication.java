package com.autogest;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Entry point of the AutoGest desktop application.
 *
 * <p>Creates the main window (minimum 1280x720) and the root layout. The
 * application shell and the navigation are loaded in later layers.</p>
 *
 * <p>Principles: High Cohesion.</p>
 */
public class AutoGestApplication extends Application {

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();

        Scene scene = new Scene(root, 1280, 720);
        stage.setTitle("AutoGest");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}