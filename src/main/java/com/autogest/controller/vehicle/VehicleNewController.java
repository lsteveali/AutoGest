package com.autogest.controller.vehicle;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controlador del formulario «Nuevo vehículo». Reutiliza el comportamiento
 * común de {@link VehicleFormController} en modo creación: no recibe id y el
 * Service valida y registra el vehículo.
 *
 * <p>Principles: Template Method, SRP, High Cohesion.</p>
 */
public class VehicleNewController extends VehicleFormController {

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
        return "Nuevo vehículo";
    }
}
