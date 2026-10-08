package com.autogest.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador raíz del shell de la aplicación.
 *
 * <p>Contiene el área central donde el {@code ViewNavigator} carga las
 * pantallas y arranca la navegación en la pantalla de inicio.</p>
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

    /** Devuelve el navegador de pantallas usado por el área central. */
    public ViewNavigator getNavigator() {
        return navigator;
    }
}