package com.autogest.controller.user;

import com.autogest.config.AppContext;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FilterBar;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.model.UserStatus;
import com.autogest.service.UserService;
import com.autogest.util.Formatter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla «Usuarios y roles» (mockup Usuarios y roles).
 * Muestra los indicadores, los filtros y la tabla de usuarios, permite crear y
 * editar cuentas y activarlas o desactivarlas, y presenta la tarjeta con los
 * roles del sistema. «Configuración» abre directamente esta pantalla porque en
 * este sprint solo la ve el administrador (D-11).
 *
 * <p>Es un controlador de presentación: traduce los eventos en llamadas a
 * {@code UserService} y muestra los errores; las validaciones viven en el
 * Service. El KPI «Sesiones activas» muestra «-» (D-14: no hay modelo de
 * sesiones).</p>
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class UsersRolesController implements Initializable {

    private static final int PAGE_SIZE = 10;
    /** Alto de una fila con avatar, nombre y usuario. */
    private static final double ROW_HEIGHT = 60;
    private static final String FILTER_ALL = "Todos";

    @FXML
    private VBox root;

    private KpiCard registeredCard;
    private KpiCard activeCard;
    private KpiCard administratorsCard;
    private PagedTable<User> table;

    private String searchText = "";
    private Role roleFilter;
    private UserStatus statusFilter;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = buildHeader();
        HBox kpis = buildKpiRow();
        FilterBar filters = buildFilters();
        table = buildTable();
        VBox rolesCard = buildRolesCard();

        // Dentro del ScrollPane la tabla toma el alto de sus filas: antes el VGrow la
        // dejaba sin espacio cuando la tarjeta de roles empujaba desde abajo.
        table.fitHeightToRows(ROW_HEIGHT);
        root.getChildren().addAll(header, kpis, filters, table, rolesCard);

        Platform.runLater(this::reload);
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader("Usuarios y roles",
                "Gestiona las cuentas, los roles y el acceso al sistema.");
        header.setBreadcrumbs(List.of("Configuración", "Usuarios y roles"));
        header.setPrimaryAction("+ Nuevo usuario", () -> showUserDialog(null));
        return header;
    }

    private HBox buildKpiRow() {
        registeredCard = new KpiCard("Usuarios registrados", "-", "Total en el sistema", IconSet.USERS);
        activeCard = new KpiCard("Usuarios activos", "-", "Con acceso habilitado", IconSet.CHECK);
        administratorsCard = new KpiCard("Administradores", "-", "Acceso completo", IconSet.SETTINGS);
        KpiCard sessionsCard = new KpiCard("Sesiones activas", "-", "Disponible en Seguridad", IconSet.BAR_CHART);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        addKpi(row, registeredCard, activeCard, administratorsCard, sessionsCard);
        return row;
    }

    /** Agrega tarjetas KPI que crecen por igual para ocupar el ancho disponible. */
    private void addKpi(HBox row, KpiCard... cards) {
        for (KpiCard card : cards) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.addSearch("Buscar por nombre, correo o usuario...", text -> {
            searchText = text;
            reload();
        });
        bar.addCombo("Rol", List.of(FILTER_ALL, UserLabels.roleText(Role.ADMINISTRATOR),
                        UserLabels.roleText(Role.RECEPTIONIST), UserLabels.roleText(Role.MECHANIC),
                        UserLabels.roleText(Role.SUPERVISOR)),
                value -> {
                    roleFilter = UserLabels.roleFromText(value);
                    reload();
                });
        bar.addCombo("Estado", List.of(FILTER_ALL, UserLabels.statusText(UserStatus.ACTIVE),
                        UserLabels.statusText(UserStatus.INACTIVE)),
                value -> {
                    statusFilter = UserLabels.statusFromText(value);
                    reload();
                });
        return bar;
    }

    private PagedTable<User> buildTable() {
        PagedTable<User> pagedTable = new PagedTable<>(PAGE_SIZE);
        // Anchos mínimos medidos con Manrope (más ancha que Roboto): «Inactivo» y la
        // fecha del último acceso deben verse completos sin barra horizontal en 1280 px.
        pagedTable.addColumn("Usuario", this::userCell, 200);
        pagedTable.addColumn("Correo", User::getEmail, 210);
        pagedTable.addColumn("Rol", user -> UserLabels.roleText(user.getRole()), 115);
        pagedTable.addColumn("Estado", user -> {
            javafx.scene.control.Label badge = StatusBadge.of(
                    UserLabels.statusText(user.getStatus()), UserLabels.statusType(user.getStatus()));
            badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            return badge;
        }, 105);
        pagedTable.addColumn("Último acceso", user -> lastAccessText(user), 140);
        pagedTable.addColumn("Acciones", this::actionsCell);
        return pagedTable;
    }

    private Node userCell(User user) {
        Label avatar = new Label(UserLabels.initials(user));
        avatar.getStyleClass().add("user-avatar");

        Label name = new Label(user.getFullName());
        name.getStyleClass().add("user-cell-name");
        Label username = new Label(user.getUsername() == null ? user.getEmail() : "@" + user.getUsername());
        username.getStyleClass().add("user-cell-username");

        HBox cell = new HBox(10, avatar, new VBox(2, name, username));
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    private Node actionsCell(User user) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("icon-button");
        menu.setFocusTraversable(false);

        MenuItem edit = new MenuItem("Editar");
        edit.setOnAction(event -> showUserDialog(user));

        boolean isActive = user.getStatus() == UserStatus.ACTIVE;
        MenuItem changeStatus = new MenuItem(isActive ? "Desactivar" : "Activar");
        changeStatus.setOnAction(event -> toggleStatus(user));

        menu.getItems().addAll(edit, changeStatus);
        return menu;
    }

    private VBox buildRolesCard() {
        Label title = new Label("Roles del sistema");
        title.getStyleClass().add("card-title");

        Button manageRoles = new Button("Administrar roles");
        manageRoles.getStyleClass().add("btn-secondary");
        manageRoles.setDisable(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, manageRoles);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, header);
        card.getStyleClass().add("card");
        for (Role role : Role.values()) {
            card.getChildren().add(roleRow(role));
        }
        return card;
    }

    private Node roleRow(Role role) {
        Label name = new Label(UserLabels.roleText(role));
        name.getStyleClass().add("role-name");
        Label description = new Label(UserLabels.roleDescription(role));
        description.getStyleClass().add("role-description");

        HBox row = new HBox(12, new VBox(2, name, description));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Recarga la tabla y los indicadores con los filtros actuales. */
    private void reload() {
        try {
            UserService userService = service();
            table.setData(userService.searchUsers(searchText, roleFilter, statusFilter));
            registeredCard.setValue(String.valueOf(userService.countUsers()));
            activeCard.setValue(String.valueOf(userService.countActiveUsers()));
            administratorsCard.setValue(String.valueOf(userService.countAdministrators()));
        } catch (AutoGestException e) {
            table.setData(List.of());
            ErrorDialog.show("No se pudieron cargar los usuarios", e.getMessage());
        }
    }

    /** Activa o desactiva un usuario, previa confirmación. */
    private void toggleStatus(User user) {
        boolean isActive = user.getStatus() == UserStatus.ACTIVE;
        UserStatus target = isActive ? UserStatus.INACTIVE : UserStatus.ACTIVE;
        String action = isActive ? "Desactivar" : "Activar";
        if (!ConfirmDialog.confirm(action + " usuario",
                "¿Deseas " + action.toLowerCase() + " a " + user.getFullName() + "?")) {
            return;
        }
        try {
            service().changeStatus(user.getId(), target);
            reload();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cambiar el estado", e.getMessage());
        }
    }

    /**
     * Abre el formulario de nuevo usuario ({@code null}) o de edición
     * ({@link UserFormDialog}); si se guardó, recarga la lista.
     *
     * @param existing usuario a editar, o {@code null} para crear uno nuevo
     */
    private void showUserDialog(User existing) {
        new UserFormDialog(existing).showAndWait().ifPresent(saved -> reload());
    }

    private String lastAccessText(User user) {
        return user.getLastAccess() == null ? "—" : Formatter.timestamp(user.getLastAccess());
    }

    /**
     * Obtiene el servicio de usuarios del contexto de la aplicación. Es diferido
     * para no inicializar la base de datos al abrir la pantalla si aún no hay
     * configuración local.
     */
    private UserService service() {
        return AppContext.getInstance().getUserService();
    }
}
