package com.autogest.controller;

import com.autogest.config.AppContext;
import com.autogest.controller.components.AssistantButton;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.Sidebar;
import com.autogest.controller.components.Topbar;
import com.autogest.controller.user.UserLabels;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.util.Session;
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
 * botón flotante abre el Asistente. Además refleja la sesión actual: muestra en
 * la Topbar al usuario y su rol, ofrece «Cerrar sesión» y filtra la Sidebar por
 * la matriz de acceso de {@link Permissions} (D-11). No contiene reglas de
 * negocio; solo coordina componentes de interfaz.</p>
 *
 * <p>Principles: Controller, Indirection, Low Coupling, High Cohesion.</p>
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
    private Runnable onLogout;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navigator = new ViewNavigator(contentArea);
        // Tras cada navegación se refresca la Topbar: así refleja el nombre
        // editado en «Perfil y cuenta» sin acoplar esa pantalla al shell.
        navigator.setOnViewChange(view -> {
            sidebar.setActiveItem(view);
            refreshTopbar();
        });

        User currentUser = Session.getCurrentUser();
        Role role = currentUser == null ? null : currentUser.getRole();
        sidebar.setRole(role);
        sidebar.setOnNavigate(this::open);
        refreshTopbar();
        topbar.setOnLogout(this::logout);
        topbar.setOnProfile(() -> open(View.SETTINGS_PROFILE));

        assistantButton.setOnAction(event -> open(View.ASSISTANT));
        StackPane.setMargin(assistantButton, new Insets(0, 24, 24, 0));

        navigator.navigateTo(View.HOME);
    }

    /**
     * Registra la acción que se ejecuta tras cerrar la sesión (la aplicación la
     * usa para volver a la pantalla de inicio de sesión).
     *
     * @param onLogout acción a ejecutar tras cerrar la sesión
     */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    /** Devuelve el navegador de pantallas usado por el área central. */
    public ViewNavigator getNavigator() {
        return navigator;
    }

    /** Muestra en la Topbar al usuario de la sesión (o «Invitado»). */
    private void refreshTopbar() {
        User user = Session.getCurrentUser();
        if (user != null) {
            topbar.setUser(user.getFullName(), UserLabels.roleText(user.getRole()));
        } else {
            topbar.setUser("Invitado", "Sin sesión");
        }
    }

    /** Abre una pantalla comprobando antes los permisos del rol. */
    private void open(View view) {
        if (view == null) {
            return;
        }
        if (Permissions.canAccess(currentRole(), view)) {
            navigator.navigateTo(view);
        } else {
            ErrorDialog.show("Acceso restringido", "No tienes permiso para abrir esta sección.");
        }
    }

    /** Cierra la sesión actual y avisa a la aplicación para volver al login. */
    private void logout() {
        if (!ConfirmDialog.confirm("Cerrar sesión", "¿Deseas cerrar la sesión actual?")) {
            return;
        }
        AppContext.getInstance().getAuthenticationService().logout();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    /** Rol del usuario en sesión, o {@code null} si no hay sesión abierta. */
    private Role currentRole() {
        User user = Session.getCurrentUser();
        return user == null ? null : user.getRole();
    }
}
