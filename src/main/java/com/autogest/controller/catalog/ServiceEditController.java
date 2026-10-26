package com.autogest.controller.catalog;

import com.autogest.controller.ParameterizedController;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.exception.AutoGestException;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controlador del formulario «Editar servicio». Reutiliza el comportamiento
 * común de {@link ServiceFormController} en modo edición: recibe el id del
 * servicio, lo carga con el Service y guarda los cambios.
 *
 * <p>Principles: Template Method, SRP, High Cohesion.</p>
 */
public class ServiceEditController extends ServiceFormController implements ParameterizedController {

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
        return "Editar servicio";
    }

    @Override
    public void receiveParameter(int parameter) {
        editingId = parameter;
        try {
            loadService(service().getDetail(parameter));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el servicio", e.getMessage());
        }
    }
}
