package com.autogest.controller.vehicle;

import com.autogest.controller.ParameterizedController;
import com.autogest.exception.AutoGestException;
import com.autogest.controller.components.ErrorDialog;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controlador del formulario «Editar vehículo». Reutiliza el comportamiento
 * común de {@link VehicleFormController} en modo edición: recibe el id del
 * vehículo, lo carga con el Service y guarda los cambios conservando el estado.
 *
 * <p>Principles: Template Method, SRP, High Cohesion.</p>
 */
public class VehicleEditController extends VehicleFormController implements ParameterizedController {

    @FXML
    private VBox root;

    @Override
    protected VBox getRoot() {
        return root;
    }

    @Override
    protected boolean isEditing() {
        return true;
    }

    @Override
    protected String title() {
        return "Editar vehículo";
    }

    @Override
    public void receiveParameter(int parameter) {
        editingId = parameter;
        try {
            loadVehicle(service().getDetail(parameter));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el vehículo", e.getMessage());
        }
    }
}
