package com.autogest.controller.catalog;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controlador del formulario «Nuevo servicio». Reutiliza el comportamiento
 * común de {@link ServiceFormController} en modo creación.
 *
 * <p>Principles: Template Method, SRP, High Cohesion.</p>
 */
public class ServiceNewController extends ServiceFormController {

    @FXML
    private VBox root;

    @Override
    protected VBox getRoot() {
        return root;
    }

    @Override
    protected boolean isEditing() {
        return false;
    }

    @Override
    protected String title() {
        return "Nuevo servicio";
    }
}
