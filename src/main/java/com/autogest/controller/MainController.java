package com.autogest.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Root controller of the application shell.
 *
 * <p>Holds the central container where the {@code ViewNavigator} loads the
 * screens, and starts the navigation on the home screen.</p>
 *
 * <p>Principles: High Cohesion, Controller.</p>
 */
public class MainController implements Initializable {

    @FXML
    private StackPane contentArea;

    private ViewNavigator navigator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navigator = new ViewNavigator(contentArea);
        navigator.navigateTo(View.HOME);
    }

    /** Returns the screen navigator used by the central area. */
    public ViewNavigator getNavigator() {
        return navigator;
    }
}