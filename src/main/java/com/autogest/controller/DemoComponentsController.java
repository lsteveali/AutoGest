package com.autogest.controller;

import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FilterBar;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Pantalla de desarrollo que muestra cada componente reutilizable de
 * {@code controller.components} con datos de ejemplo. Sirve para que los demás
 * integrantes entiendan cómo se ven y cómo se usan antes de construir sus
 * módulos, y no forma parte de la navegación oficial.
 *
 * <p>Principles: Controller, Pure Fabrication.</p>
 */
public class DemoComponentsController implements Initializable {

    /** Fila de ejemplo para la tabla paginada. */
    private record DemoUser(String name, String email, String role, String status) {
    }

    private static final List<DemoUser> SAMPLE_USERS = List.of(
            new DemoUser("José García", "jose.garcia@autogest.co", "Administrador", "ACTIVE"),
            new DemoUser("Carlos Martínez", "carlos.martinez@autogest.co", "Mecánico", "ACTIVE"),
            new DemoUser("Laura Rodríguez", "laura.rodriguez@autogest.co", "Recepción", "ACTIVE"),
            new DemoUser("Andrés Gómez", "andres.gomez@autogest.co", "Mecánico", "INACTIVE"),
            new DemoUser("María Rodríguez", "maria.rodriguez@autogest.co", "Supervisor", "ACTIVE"),
            new DemoUser("Juan Pérez", "juan.perez@autogest.co", "Mecánico", "ACTIVE"),
            new DemoUser("Ana Torres", "ana.torres@autogest.co", "Recepción", "INACTIVE"),
            new DemoUser("Luis Herrera", "luis.herrera@autogest.co", "Supervisor", "ACTIVE"),
            new DemoUser("Sofía Ramírez", "sofia.ramirez@autogest.co", "Mecánico", "ACTIVE"),
            new DemoUser("Diego Castro", "diego.castro@autogest.co", "Recepción", "ACTIVE"),
            new DemoUser("Paula Vargas", "paula.vargas@autogest.co", "Administrador", "ACTIVE"),
            new DemoUser("Ricardo Núñez", "ricardo.nunez@autogest.co", "Mecánico", "INACTIVE"));

    @FXML
    private VBox root;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.getChildren().addAll(
                buildHeader(),
                buildKpiRow(),
                buildFilters(),
                buildTable(),
                buildBadges(),
                buildDialogs());
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader("Demo de componentes",
                "Vista de desarrollo con cada componente reutilizable del sistema de diseño.");
        header.setBreadcrumbs(List.of("Inicio", "Demo de componentes"));
        header.setPrimaryAction("+ Nuevo usuario", () ->
                ErrorDialog.show("Demo", "El botón principal de la cabecera funciona."));
        return header;
    }

    private HBox buildKpiRow() {
        HBox row = new HBox(16,
                new KpiCard("Usuarios registrados", "12", "Total en el sistema", IconSet.USERS),
                new KpiCard("Usuarios activos", "9", "Con acceso habilitado", IconSet.CHECK),
                new KpiCard("Administradores", "2", "Acceso completo", IconSet.SETTINGS),
                new KpiCard("Sesiones activas", "-", "Disponible en Seguridad", IconSet.BAR_CHART));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.addSearch("Buscar usuario...", text -> System.out.println("Buscar: " + text));
        bar.addCombo("Rol", List.of("Todos", "Administrador", "Recepción", "Mecánico", "Supervisor"),
                value -> System.out.println("Rol: " + value));
        bar.addCombo("Estado", List.of("Todos", "Activo", "Inactivo"),
                value -> System.out.println("Estado: " + value));
        return bar;
    }

    private PagedTable<DemoUser> buildTable() {
        PagedTable<DemoUser> table = new PagedTable<>(10);
        table.addColumn("Usuario", DemoUser::name);
        table.addColumn("Correo", DemoUser::email);
        table.addColumn("Rol", DemoUser::role);
        table.addColumn("Estado", user -> StatusBadge.of(
                statusText(user.status()), statusType(user.status())));
        table.addColumn("Acciones", this::buildActions);
        table.setOnSelect(user -> System.out.println("Seleccionado: " + user.name()));
        table.setData(SAMPLE_USERS);
        return table;
    }

    private Node buildActions(DemoUser user) {
        Button edit = new Button();
        edit.setGraphic(IconSet.icon(IconSet.EDIT));
        edit.getStyleClass().add("icon-button");
        edit.setOnAction(event -> ErrorDialog.show("Demo", "Editar a " + user.name()));

        Button delete = new Button();
        delete.setGraphic(IconSet.icon(IconSet.TRASH));
        delete.getStyleClass().add("icon-button");
        delete.setOnAction(event -> ConfirmDialog.confirm("Desactivar usuario",
                "¿Deseas desactivar a " + user.name() + "?"));

        HBox actions = new HBox(4, edit, delete);
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private HBox buildBadges() {
        HBox row = new HBox(12,
                StatusBadge.of("Activo", StatusBadge.BadgeType.SUCCESS),
                StatusBadge.of("En reparación", StatusBadge.BadgeType.WARNING),
                StatusBadge.of("En servicio", StatusBadge.BadgeType.INFO),
                StatusBadge.of("Agotado", StatusBadge.BadgeType.DANGER),
                StatusBadge.of("Inactivo", StatusBadge.BadgeType.NEUTRAL));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildDialogs() {
        Button confirmButton = new Button("Probar confirmación");
        confirmButton.getStyleClass().add("btn-secondary");
        confirmButton.setOnAction(event -> ConfirmDialog.confirm("Confirmar acción",
                "¿Deseas continuar con la acción de ejemplo?"));

        Button errorButton = new Button("Probar error");
        errorButton.getStyleClass().add("btn-danger");
        errorButton.setOnAction(event -> ErrorDialog.show("No se pudo guardar el registro."));

        HBox row = new HBox(12, confirmButton, errorButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String statusText(String status) {
        return "ACTIVE".equals(status) ? "Activo" : "Inactivo";
    }

    private StatusBadge.BadgeType statusType(String status) {
        return "ACTIVE".equals(status) ? StatusBadge.BadgeType.SUCCESS : StatusBadge.BadgeType.NEUTRAL;
    }
}
