package com.autogest.controller.inventory;

import com.autogest.controller.ParameterizedController;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.exception.AutoGestException;
import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controlador de «Editar repuesto»: recibe el id, carga el repuesto y guarda
 * los cambios con {@link PartFormController}.
 *
 * <p>Principles: Template Method, SRP.</p>
 */
public class PartEditController extends PartFormController implements ParameterizedController {

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
        return "Editar repuesto";
    }

    @Override
    public void receiveParameter(int parameter) {
        editingId = parameter;
        try {
            loadPart(partService().getDetail(parameter));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el repuesto", e.getMessage());
        }
    }
}
