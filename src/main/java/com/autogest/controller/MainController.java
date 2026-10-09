package com.autogest.controller;

import com.autogest.controller.components.AssistantButton;
import com.autogest.controller.components.Sidebar;
import com.autogest.controller.components.Topbar;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador raíz del shell de la aplicación.
 *
 * <p>Conecta la barra lateral, la cabecera y el {@link ViewNavigator} sobre el
 * área central: la Sidebar navega, el navegador resalta el ítem activo y el
 * botón flotante abre el Asistente. No contiene reglas de negocio; solo
 * coordina componentes de interfaz.</p>
 *
 * <p>Principles: Controller, High Cohesion, Indirection.</p>
 */
public class MainController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private Sidebar sidebar;

    @FXML
    private Topbar topbar;

    @FXML
    private AssistantButton assistantButton;

    private ViewNavigator navigator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navigator = new ViewNavigator(contentArea);
        navigator.setOnViewChange(sidebar::setActiveItem);
        sidebar.setOnNavigate(navigator::navigateTo);

        assistantButton.setOnAction(event -> navigator.navigateTo(View.ASSISTANT));
        StackPane.setMargin(assistantButton, new Insets(0, 24, 24, 0));

        navigator.navigateTo(View.HOME);
    }

    /** Devuelve el navegador de pantallas usado por el área central. */
    public ViewNavigator getNavigator() {
        return navigator;
    }
}
