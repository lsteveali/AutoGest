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
 * screens. Sidebar and Topbar components are attached in later layers.</p>
 *
 * <p>Principles: High Cohesion, Controller.</p>
 */
public class MainController implements Initializable {

    @FXML
    private StackPane contentArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Navigation wiring is attached by ViewNavigator (Sprint 1, L3).
    }

    /** Returns the central container used for screen navigation. */
    public StackPane getContentArea() {
        return contentArea;
    }
}