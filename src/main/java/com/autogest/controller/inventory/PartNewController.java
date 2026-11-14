package com.autogest.controller.inventory;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controlador de «Nuevo repuesto»: usa {@link PartFormController} en modo creación.
 *
 * <p>Principles: Template Method, SRP.</p>
 */
public class PartNewController extends PartFormController {

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
        return "Nuevo repuesto";
    }
}
