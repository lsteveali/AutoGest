package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.StatusBadge;
import com.autogest.controller.user.UserLabels;
import com.autogest.model.User;
import com.autogest.util.Background;
import com.autogest.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Pantalla general de Configuración (mockup «Configuración»): tarjeta del
 * usuario en sesión, las secciones de configuración y la información del
 * sistema. Solo navega; cada sección guarda sus propios datos.
 *
 * <p>Las secciones se describen como datos ({@link Section}). Una tarjeta se
 * habilita si su pantalla existe en {@link View} y su FXML está en el
 * proyecto; si no, se muestra «Próximamente». Así «Datos del taller» y
 * «Preferencias del sistema» (Desarrollador 1) se activan solas al integrarse,
 * sin que este controlador nombre constantes que todavía no existen.</p>
 *
 * <p>Principles: GRASP Controller, Indirection, Protected Variations, OCP.</p>
 */
public class SettingsController implements Initializable, NavigatorAware {

    /** Versión mostrada en «Información del sistema» (coincide con el pom). */
    static final String APP_VERSION = "1.0";

    /** Sección de Configuración: texto, ícono y nombre de su {@link View}. */
    record Section(String title, String description, String icon, String viewName) {
    }

    /** Orden y textos del mockup «Configuración». */
    static final List<Section> SECTIONS = List.of(
            new Section("Perfil y cuenta",
                    "Administra tu información personal y tu contraseña.",
                    IconSet.USER, "SETTINGS_PROFILE"),
            new Section("Datos del taller",
                    "Configura el nombre, la información de contacto, la dirección y los datos generales del taller.",
                    IconSet.WAREHOUSE, "SETTINGS_WORKSHOP"),
            new Section("Usuarios y roles",
                    "Administra los usuarios que tienen acceso a AutoGest y define sus permisos.",
                    IconSet.USERS, "USERS_ROLES"),
            new Section("Notificaciones",
                    "Configura los canales, los avisos automáticos y las plantillas de comunicación.",
                    IconSet.BELL, "SETTINGS_NOTIFICATIONS"),
            new Section("Preferencias del sistema",
                    "Configura opciones generales de funcionamiento, formatos y comportamiento de AutoGest.",
                    IconSet.SETTINGS, "SETTINGS_PREFERENCES"),
            new Section("Facturación",
                    "Configura impuestos, métodos de pago, numeración de facturas y parámetros de facturación.",
                    IconSet.FILE_TEXT, "SETTINGS_BILLING"),
            new Section("Seguridad",
                    "Administra la política de contraseñas, las sesiones y los permisos por rol.",
                    IconSet.SHIELD, "SETTINGS_SECURITY"),
            new Section("Copias de seguridad",
                    "Crea y consulta las copias de seguridad de la información de AutoGest.",
                    IconSet.CLOUD_DOWNLOAD, "SETTINGS_BACKUP"),
            new Section("Personalización",
                    "Elige el vehículo y el color que giran en la pantalla de inicio de sesión de este equipo.",
                    IconSet.CAR, "SETTINGS_APPEARANCE"));

    private static final int COLUMNS = 2;

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private Label databaseValue;
    private Node databaseDot;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Configuración",
                "Administra las preferencias, usuarios y parámetros generales de AutoGest.");
        header.setBreadcrumbs(List.of("Inicio", "Configuración"));

        Label generalTitle = new Label("Configuración general");
        generalTitle.getStyleClass().add("settings-group-title");

        root.getChildren().addAll(header, buildProfileCard(), generalTitle, buildSectionGrid(),
                buildSystemInfoCard());
        checkDatabase();
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    // ---------- tarjeta del usuario ----------

    private HBox buildProfileCard() {
        User user = Session.getCurrentUser();
        String name = user == null ? "Invitado" : user.getFullName();
        String role = user == null ? "Sin sesión" : UserLabels.roleText(user.getRole());
        String email = user == null ? "" : user.getEmail();

        Label avatar = new Label(initialsOf(name));
        avatar.getStyleClass().add("settings-avatar");

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("settings-profile-name");
        Label roleLabel = new Label(role);
        roleLabel.getStyleClass().add("settings-profile-role");
        Label emailLabel = new Label(email);
        emailLabel.getStyleClass().add("settings-profile-email");
        VBox info = new VBox(2, nameLabel, roleLabel, emailLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button edit = new Button("Editar perfil");
        edit.getStyleClass().add("btn-primary");
        edit.setOnAction(event -> open(View.SETTINGS_PROFILE));

        HBox card = new HBox(16, avatar, info, spacer, edit);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().addAll("card", "settings-profile-card");
        return card;
    }

    // ---------- secciones ----------

    private GridPane buildSectionGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("settings-grid");
        for (int i = 0; i < COLUMNS; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / COLUMNS);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        for (int i = 0; i < SECTIONS.size(); i++) {
            HBox card = buildSectionCard(SECTIONS.get(i));
            GridPane.setHgrow(card, Priority.ALWAYS);
            GridPane.setFillHeight(card, true);
            grid.add(card, i % COLUMNS, i / COLUMNS);
        }
        return grid;
    }

    /**
     * Tarjeta de sección. Es un contenedor (no un {@code Button}: con texto que
     * se ajusta, el botón calcula mal su alto) que toma el foco con Tab y se
     * abre con clic, Enter o Espacio.
     */
    private HBox buildSectionCard(Section section) {
        StackPane iconTile = new StackPane(IconSet.icon(section.icon()));
        iconTile.getStyleClass().add("settings-card-icon");

        Label title = new Label(section.title());
        title.getStyleClass().add("settings-card-title");
        Label description = new Label(section.description());
        description.getStyleClass().add("settings-card-description");
        description.setWrapText(true);
        VBox texts = new VBox(4, title, description);
        texts.setMinWidth(0);
        HBox.setHgrow(texts, Priority.ALWAYS);

        Optional<View> target = availableView(section.viewName());
        Node trailing;
        if (target.isPresent()) {
            trailing = IconSet.icon(IconSet.ARROW_RIGHT);
            trailing.getStyleClass().add("settings-card-arrow");
        } else {
            Label soon = StatusBadge.of("Próximamente", StatusBadge.BadgeType.NEUTRAL);
            soon.setMinWidth(Region.USE_PREF_SIZE);
            trailing = soon;
        }

        HBox card = new HBox(16, iconTile, texts, trailing);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        card.getStyleClass().add("settings-card");
        card.setAccessibleRole(AccessibleRole.BUTTON);
        card.setAccessibleText(section.title());
        if (target.isPresent()) {
            card.setFocusTraversable(true);
            card.setOnMouseClicked(event -> open(target.get()));
            card.setOnKeyPressed(event -> {
                if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE) {
                    open(target.get());
                    event.consume();
                }
            });
        } else {
            card.setDisable(true);
        }
        return card;
    }

    /**
     * Devuelve la pantalla de una sección si existe en {@link View} y su FXML
     * está en el proyecto.
     */
    static Optional<View> availableView(String viewName) {
        return Arrays.stream(View.values())
                .filter(view -> view.name().equals(viewName))
                .filter(view -> SettingsController.class.getResource(view.getFxmlPath()) != null)
                .findFirst();
    }

    // ---------- información del sistema ----------

    private VBox buildSystemInfoCard() {
        Label title = new Label("Información del sistema");
        title.getStyleClass().add("card-title");
        HBox titleRow = new HBox(10, IconSet.icon(IconSet.INFO), title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        User user = Session.getCurrentUser();
        databaseValue = new Label("Comprobando...");
        databaseValue.getStyleClass().add("settings-info-value");
        Circle dot = new Circle(4);
        dot.getStyleClass().addAll("status-dot", "status-dot-pending");
        databaseDot = dot;
        HBox databaseRow = new HBox(8, dot, databaseValue);
        databaseRow.setAlignment(Pos.CENTER_LEFT);

        GridPane rows = new GridPane();
        rows.getStyleClass().add("settings-info-grid");
        addInfoRow(rows, 0, "Versión de AutoGest", valueLabel(APP_VERSION));
        addInfoRow(rows, 1, "Base de datos", databaseRow);
        addInfoRow(rows, 2, "Java", valueLabel(System.getProperty("java.version")));
        addInfoRow(rows, 3, "JavaFX", valueLabel(System.getProperty("javafx.runtime.version", "-")));
        addInfoRow(rows, 4, "Sesión", valueLabel(user == null ? "-" : user.getEmail()));

        VBox card = new VBox(14, titleRow, rows);
        card.getStyleClass().addAll("card", "settings-info-card");
        return card;
    }

    private static Label valueLabel(String text) {
        Label label = new Label(text == null ? "-" : text);
        label.getStyleClass().add("settings-info-value");
        return label;
    }

    private static void addInfoRow(GridPane grid, int row, String name, Node value) {
        Label label = new Label(name);
        label.getStyleClass().add("settings-info-label");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    /**
     * Comprueba la conexión con una consulta liviana en segundo plano, a través
     * del Service (el controlador no usa {@code java.sql}).
     */
    private void checkDatabase() {
        Background.run(() -> AppContext.getInstance().getUserService().countUsers(),
                count -> showDatabase(true),
                error -> showDatabase(false));
    }

    private void showDatabase(boolean connected) {
        databaseValue.setText(connected ? "Conectada" : "Sin conexión");
        databaseDot.getStyleClass().removeAll("status-dot-pending", "status-dot-ok", "status-dot-error");
        databaseDot.getStyleClass().add(connected ? "status-dot-ok" : "status-dot-error");
    }

    // ---------- navegación ----------

    private void open(View view) {
        User user = Session.getCurrentUser();
        if (navigator != null && user != null && Permissions.canAccess(user.getRole(), view)) {
            navigator.navigateTo(view);
        }
    }

    /** Iniciales de la primera y la última palabra del nombre. */
    static String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
