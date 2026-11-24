package com.autogest.controller;

import com.autogest.config.AppContext;
import com.autogest.controller.assistant.AssistantAction;
import com.autogest.controller.assistant.AssistantFormFiller;
import com.autogest.controller.assistant.AssistantPanel;
import com.autogest.controller.assistant.AssistantResolver;
import com.autogest.controller.assistant.ServiceAssistantLookup;
import com.autogest.controller.components.AssistantButton;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.Sidebar;
import com.autogest.controller.components.Topbar;
import com.autogest.controller.user.UserLabels;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.SystemPreferences;
import com.autogest.model.User;
import com.autogest.util.AppPreferences;
import com.autogest.util.AppTheme;
import com.autogest.util.Background;
import com.autogest.util.LocalMemory;
import com.autogest.util.LoginAppearanceStore;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

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

    /** Columna de la Topbar y el contenido; en el Inicio lleva el tema elegido. */
    @FXML
    private VBox centerColumn;

    private ViewNavigator navigator;
    private Runnable onLogout;
    /** Cierre de sesión por inactividad, según las Preferencias del sistema. */
    private final InactivityTimer inactivity = new InactivityTimer(this::endSessionForInactivity);
    /** Panel del Asistente; se crea la primera vez que se abre. */
    private AssistantPanel assistantPanel;
    /** Último módulo de cada usuario en este equipo. */
    private final LocalMemory memory = new LocalMemory();
    /** El usuario ya eligió una pantalla: no se le cambia por el último módulo. */
    private boolean navigatedByUser;
    /** Aplica la barra lateral cuando se guardan las Preferencias del sistema. */
    private final Consumer<SystemPreferences> preferencesChanged =
            preferences -> Platform.runLater(() -> applyPreferences(preferences));

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        navigator = new ViewNavigator(contentArea);
        // Tras cada navegación se refresca la Topbar: así refleja el nombre
        // editado en «Perfil y cuenta» sin acoplar esa pantalla al shell.
        navigator.setOnViewChange(view -> {
            sidebar.setActiveItem(view);
            applyDashboardLook(view);
            refreshTopbar();
            rememberModule(view);
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
        // cierre por inactividad, formato regional, barra lateral, último módulo y
        // filtros). Si no se pueden leer, siguen las de fábrica.
        AppPreferences.addListener(preferencesChanged);
        applyPreferences(AppPreferences.current());
        Background.run(() -> AppContext.getInstance().getSettingsService().getPreferences(),
                preferences -> {
                    AppPreferences.update(preferences);
                    openLastModule();
                }, error -> { });
        // Se enciende cuando la ventana ya existe (después de initialize).
        javafx.application.Platform.runLater(inactivity::start);

        assistantButton.setOnAction(event -> toggleAssistant());
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

    /**
     * En el Inicio, la foto del encabezado sube hasta el borde de la ventana
     * (la Topbar queda encima, sin fondo) y la columna toma el tema elegido en
     * Personalización: claro (Ivory) u oscuro (Steel). Las demás pantallas
     * siguen como estaban.
     */
    private void applyDashboardLook(View view) {
        centerColumn.getStyleClass().removeAll("dashboard-mode", "dashboard-light", "dashboard-dark");
        centerColumn.spacingProperty().unbind();
        centerColumn.setSpacing(0);
        topbar.setViewOrder(0);
        if (view != View.HOME) {
            return;
        }
        AppTheme theme = new LoginAppearanceStore().theme();
        centerColumn.getStyleClass().addAll("dashboard-mode",
                theme == AppTheme.DARK ? "dashboard-dark" : "dashboard-light");
        // El contenido sube bajo la Topbar y la Topbar se dibuja encima.
        centerColumn.spacingProperty().bind(topbar.heightProperty().negate());
        topbar.setViewOrder(-1);
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
                    return notifications.countByStatus(com.autogest.model.Catalogs.NotificationStatus.FAILED)
                            + notifications.countByStatus(com.autogest.model.Catalogs.NotificationStatus.PENDING);
                },
                topbar::setNotificationCount,
                error -> topbar.setNotificationCount(0));
    }

    // ---------- Asistente AutoGest (Sprint 9, Líder) ----------

    /** Abre o cierra el panel del Asistente (botón flotante e ítem de la barra lateral). */
    private void toggleAssistant() {
        if (assistantPanel == null) {
            assistantPanel = new AssistantPanel(AppContext.getInstance().getAssistantService(),
                    new AssistantResolver(new ServiceAssistantLookup()), this::runAssistantAction);
            assistantPanel.setOnClose(this::toggleAssistant);
            assistantPanel.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    toggleAssistant();
                }
            });
            StackPane.setAlignment(assistantPanel, Pos.CENTER_RIGHT);
            StackPane.setMargin(assistantPanel, new Insets(12, 12, 12, 0));
            ((StackPane) contentArea.getParent()).getChildren().add(assistantPanel);
            assistantPanel.setVisible(false);
        }
        boolean show = !assistantPanel.isVisible();
        assistantPanel.setVisible(show);
        assistantButton.setVisible(!show);
        if (show) {
            assistantPanel.focusInput();
        }
    }

    /**
     * Ejecuta una acción del Asistente con los mismos permisos que la barra
     * lateral: abre la pantalla y aplica filtros o precarga campos. Nunca
     * guarda nada.
     *
     * @return {@code false} si el rol no puede abrir la pantalla
     */
    private boolean runAssistantAction(AssistantAction action) {
        navigatedByUser = true;
        if (!Permissions.canAccess(currentRole(), action.view())) {
            return false;
        }
        navigator.navigate(action.view(), action.parameter(), root -> AssistantFormFiller.apply(root, action));
        return true;
    }

    /** Abre una pantalla comprobando antes los permisos del rol. */
    private void open(View view) {
        if (view == null) {
            return;
        }
        navigatedByUser = true;
        if (view == View.ASSISTANT) {
            // El Asistente es un panel junto al contenido, no una pantalla aparte.
            toggleAssistant();
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
    /** Barra lateral expandida o compacta según «Mostrar barra lateral». */
    private void applyPreferences(SystemPreferences preferences) {
        sidebar.setCompact("Compacto".equals(preferences.getSidebarMode()));
    }

    /** Recuerda en el equipo el módulo abierto (la sección de la barra lateral). */
    private void rememberModule(View view) {
        User user = Session.getCurrentUser();
        if (user != null && view != null) {
            memory.rememberModule(user.getId(), view.getSidebarSection().name());
        }
    }

    /**
     * Con «Abrir automáticamente el último módulo utilizado», al entrar se abre
     * el módulo donde el usuario quedó la última vez en este equipo, si su rol
     * todavía lo puede abrir y si no ha elegido otra pantalla mientras cargaban
     * las preferencias.
     */
    private void openLastModule() {
        User user = Session.getCurrentUser();
        if (navigatedByUser || user == null || !AppPreferences.current().isOpenLastModule()) {
            return;
        }
        View last = lastModule(memory.lastModule(user.getId()));
        if (last != null && last != View.HOME && Permissions.canAccess(user.getRole(), last)) {
            navigator.navigateTo(last);
        }
    }

    /** Módulo guardado, o {@code null} si no existe o ya no es un módulo de la barra. */
    static View lastModule(String name) {
        if (name == null) {
            return null;
        }
        try {
            View view = View.valueOf(name);
            return view.getSidebarSection() == view && view != View.ASSISTANT ? view : null;
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    private void endSession() {
        AppPreferences.removeListener(preferencesChanged);
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
