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
import com.autogest.util.AppPreferences;
import com.autogest.util.Background;
import com.autogest.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
    /** Cierre de sesión por inactividad, según las Preferencias del sistema. */
    private final InactivityTimer inactivity = new InactivityTimer(this::endSessionForInactivity);

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
        topbar.setOnNotifications(() -> open(View.NOTIFICATIONS));

        // «Volver» y las migas de pan de cada pantalla (PageHeader) suben hasta aquí.
        contentArea.addEventHandler(NavigationEvent.BACK, event -> {
            navigator.goBack();
            event.consume();
        });
        contentArea.addEventHandler(NavigationEvent.OPEN, event -> {
            open(event.getView());
            event.consume();
        });
        contentArea.addEventHandler(NavigationEvent.END_SESSION, event -> {
            event.consume();
            endSession();
        });

        // Preferencias del sistema de esta sesión (registros por página, confirmaciones,
        // cierre por inactividad). Si no se pueden leer, siguen las de fábrica.
        Background.run(() -> AppContext.getInstance().getSettingsService().getPreferences(),
                AppPreferences::update, error -> { });
        // Se enciende cuando la ventana ya existe (después de initialize).
        javafx.application.Platform.runLater(inactivity::start);

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
        refreshNotificationCount();
    }

    /**
     * Cuenta en segundo plano los avisos que necesitan atención (fallidos o
     * pendientes) para el contador de la campana. Solo para los roles que
     * pueden abrir Notificaciones; si falla la consulta, el contador se oculta.
     */
    private void refreshNotificationCount() {
        if (!Permissions.canAccess(currentRole(), View.NOTIFICATIONS)) {
            topbar.setNotificationCount(0);
            return;
        }
        com.autogest.util.Background.run(() -> {
                    com.autogest.service.NotificationService notifications =
                            AppContext.getInstance().getNotificationService();
                    return notifications.countByStatus(com.autogest.model.NotificationStatus.FAILED)
                            + notifications.countByStatus(com.autogest.model.NotificationStatus.PENDING);
                },
                topbar::setNotificationCount,
                error -> topbar.setNotificationCount(0));
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
        endSession();
    }

    /** Cierra la sesión sin preguntar y vuelve al inicio de sesión. */
    private void endSession() {
        inactivity.stop();
        AppContext.getInstance().getAuthenticationService().logout();
        if (onLogout != null) {
            onLogout.run();
        }
    }

    /** Cierre por inactividad (preferencia del sistema): cierra la sesión y lo explica. */
    private void endSessionForInactivity() {
        endSession();
        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Por seguridad se cerró la sesión después de " + AppPreferences.inactivityMinutes()
                        + " minutos sin actividad. Inicia sesión de nuevo para continuar.");
        alert.setHeaderText(null);
        alert.setTitle("Sesión cerrada");
        alert.show();
    }

    /** Rol del usuario en sesión, o {@code null} si no hay sesión abierta. */
    private Role currentRole() {
        User user = Session.getCurrentUser();
        return user == null ? null : user.getRole();
    }
}
