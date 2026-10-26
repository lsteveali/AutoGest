package com.autogest.controller.home;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.controller.vehicle.VehicleLabels;
import com.autogest.exception.AutoGestException;
import com.autogest.model.User;
import com.autogest.model.Vehicle;
import com.autogest.service.CustomerService;
import com.autogest.service.PartService;
import com.autogest.service.VehicleService;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador del panel de inicio (Dashboard v1, mockup Dashboard). Muestra el
 * saludo, los indicadores globales del taller, el estado del parque
 * automotor, las alertas de inventario, los próximos mantenimientos y el
 * espacio reservado para notificaciones.
 *
 * <p>Los módulos aún no construidos (órdenes, facturación, notificaciones)
 * muestran el marcador «-» con su sprint previsto, igual que otros tableros
 * del sistema (D-14).</p>
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class HomeController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 5;

    @FXML
    private VBox root;

    private KpiCard customersCard;
    private KpiCard vehiclesCard;
    private KpiCard ordersCard;
    private KpiCard billingCard;
    private PagedTable<Vehicle> maintenanceTable;
    private Label inventoryTotal;
    private Label inventoryLow;
    private Label inventoryOut;

    private ViewNavigator navigator;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        root.getChildren().addAll(buildHeader(), buildKpiRow(),
                buildStatusRow(), buildBottomRow());
        Platform.runLater(this::reload);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    // ---------- cabecera ----------

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader(greeting(),
                "Aquí tienes un resumen de lo que está pasando en tu taller.");
        header.setBreadcrumbs(List.of("Inicio"));
        header.setPrimaryAction("+ Nueva orden", () -> {
            if (navigator != null) {
                navigator.navigateTo(View.ORDERS);
            }
        });
        return header;
    }

    /** Saludo según la hora del día y el usuario en sesión. */
    private String greeting() {
        int hour = LocalTime.now().getHour();
        String base;
        if (hour >= 12 && hour < 19) {
            base = "Buenas tardes";
        } else if (hour >= 19 || hour < 5) {
            base = "Buenas noches";
        } else {
            base = "Buenos días";
        }

        User user = Session.getCurrentUser();
        if (user == null) {
            return base;
        }
        String name = user.getFirstName() == null || user.getFirstName().isBlank()
                ? user.getUsername() : user.getFirstName();
        return base + ", " + name;
    }

    // ---------- indicadores ----------

    private HBox buildKpiRow() {
        customersCard = new KpiCard("Clientes", "-", "Registrados en AutoGest", IconSet.USERS);
        vehiclesCard = new KpiCard("Vehículos registrados", "-", "Parque automotor", IconSet.CAR);
        ordersCard = new KpiCard("Órdenes activas", "-", "Disponible en el Sprint 4", IconSet.FILE_TEXT);
        billingCard = new KpiCard("Facturación del mes", "-", "Disponible en el Sprint 5", IconSet.CREDIT_CARD);

        HBox row = new HBox(16);
        row.getStyleClass().add("dashboard-row");
        for (KpiCard card : List.of(customersCard, vehiclesCard, ordersCard, billingCard)) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    // ---------- fila 1: estado + inventario ----------

    private HBox buildStatusRow() {
        HBox row = new HBox(16);
        row.getStyleClass().add("dashboard-row");

        VBox statusCard = buildFleetStatusCard();
        statusCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(statusCard, Priority.ALWAYS);
        row.getChildren().addAll(statusCard, buildInventoryCard());
        return row;
    }

    private VBox buildFleetStatusCard() {
        Label title = new Label("Estado del parque automotor");
        title.getStyleClass().add("card-title");

        HBox stages = new HBox(12);
        stages.setAlignment(Pos.CENTER);
        List<Stage> items = List.of(
                new Stage("Registrados", IconSet.CAR),
                new Stage("Activos", IconSet.CHECK),
                new Stage("En servicio", IconSet.WRENCH),
                new Stage("Mantenimiento", IconSet.CALENDAR),
                new Stage("Inactivos", IconSet.X));
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                // Separador entre etapas, al estilo del mockup del dashboard.
                stages.getChildren().add(IconSet.icon(IconSet.CHEVRON_RIGHT));
            }
            Node node = stageNode(items.get(i));
            HBox.setHgrow(node, Priority.ALWAYS);
            stages.getChildren().add(node);
        }

        VBox card = new VBox(16, title, stages);
        card.getStyleClass().add("card");
        return card;
    }

    /** Etapa del diagrama de estado: ícono, nombre y contador. */
    private Node stageNode(Stage stage) {
        Path icon = IconSet.icon(stage.iconData());

        Label iconBox = new Label();
        iconBox.getStyleClass().add("stage-icon");
        iconBox.setGraphic(icon);
        iconBox.setAlignment(Pos.CENTER);

        Label name = new Label(stage.name());
        name.getStyleClass().add("stage-title");
        name.setAlignment(Pos.CENTER);

        Label count = new Label("-");
        count.getStyleClass().add("stage-count");
        count.setAlignment(Pos.CENTER);
        count.setId("stage-" + stage.name().toLowerCase(java.util.Locale.ROOT).replace(' ', '-'));

        VBox box = new VBox(4, iconBox, name, count);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private VBox buildInventoryCard() {
        Label title = new Label("Alertas de inventario");
        title.getStyleClass().add("card-title");

        inventoryTotal = statValue();
        inventoryLow = statValue();
        inventoryOut = statValue();

        VBox card = new VBox(14, title,
                statRow("Repuestos en catálogo", inventoryTotal),
                statRow("Stock bajo", inventoryLow),
                statRow("Agotados", inventoryOut));
        card.getStyleClass().addAll("card", "side-card");
        return card;
    }

    /** Etiqueta de valor de una fila de estadística (se actualiza al recargar). */
    private Label statValue() {
        Label value = new Label("-");
        value.getStyleClass().add("stat-value");
        return value;
    }

    /** Fila «etiqueta ..... valor» de las tarjetas laterales. */
    private HBox statRow(String label, Label value) {
        Label name = new Label(label);
        name.getStyleClass().add("kpi-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(8, name, spacer, value);
        row.getStyleClass().add("stat-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------- fila 2: mantenimientos + notificaciones ----------

    private HBox buildBottomRow() {
        HBox row = new HBox(16);
        row.getStyleClass().add("dashboard-row");

        VBox maintCard = buildMaintenanceCard();
        maintCard.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(maintCard, Priority.ALWAYS);
        row.getChildren().addAll(maintCard, buildNotificationsCard());
        return row;
    }

    private VBox buildMaintenanceCard() {
        Label title = new Label("Próximos mantenimientos");
        title.getStyleClass().add("card-title");

        maintenanceTable = buildMaintenanceTable();
        VBox.setVgrow(maintenanceTable, Priority.ALWAYS);

        VBox card = new VBox(16, title, maintenanceTable);
        card.getStyleClass().add("card");
        return card;
    }

    private PagedTable<Vehicle> buildMaintenanceTable() {
        PagedTable<Vehicle> table = new PagedTable<>(PAGE_SIZE);
        table.addColumn("Cliente", vehicle -> vehicle.getOwner() == null
                ? "Sin propietario" : vehicle.getOwner().getFullName());
        table.addColumn("Vehículo", Vehicle::getShortDescription);
        table.addColumn("Placa", Vehicle::getLicensePlate);
        table.addColumn("Fecha", vehicle -> Formatter.date(vehicle.getNextMaintenanceDate()));
        table.addColumn("Estado", vehicle -> StatusBadge.of(
                VehicleLabels.maintenanceText(vehicle), VehicleLabels.maintenanceType(vehicle)));
        table.setOnSelect(vehicle -> {
            if (navigator != null) {
                navigator.navigateWithParameter(View.VEHICLE_DETAIL, vehicle.getId());
            }
        });
        return table;
    }

    private VBox buildNotificationsCard() {
        Label title = new Label("Notificaciones");
        title.getStyleClass().add("card-title");

        Label icon = new Label();
        icon.setGraphic(IconSet.icon(IconSet.BELL));
        icon.getStyleClass().add("stage-icon");

        Label empty = new Label("Sin notificaciones por ahora");
        empty.getStyleClass().add("card-title");
        empty.setAlignment(Pos.CENTER);

        Label note = new Label("Disponible en el Sprint 6");
        note.getStyleClass().add("card-note");
        note.setAlignment(Pos.CENTER);
        note.setWrapText(true);

        VBox body = new VBox(8, icon, empty, note);
        body.setAlignment(Pos.CENTER);
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox card = new VBox(16, title, body);
        card.getStyleClass().addAll("card", "side-card");
        card.setMinHeight(200);
        return card;
    }

    // ---------- carga de datos ----------

    private void reload() {
        loadCustomers();
        loadVehicles();
        loadInventory();
        loadMaintenance();
    }

    private void loadCustomers() {
        try {
            CustomerService service = AppContext.getInstance().getCustomerService();
            customersCard.setValue(String.valueOf(service.countCustomers()));
            customersCard.setDetail("+" + service.countNewCustomersThisMonth() + " este mes");
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron cargar los clientes", e.getMessage());
        }
    }

    private void loadVehicles() {
        try {
            VehicleService service = AppContext.getInstance().getVehicleService();
            int total = service.countVehicles();
            int active = service.countActiveVehicles();
            int inService = service.countInService();
            int maintenance = service.countMaintenanceDue();
            int inactive = Math.max(0, total - active - inService);

            vehiclesCard.setValue(String.valueOf(total));
            vehiclesCard.setDetail(active + " activos · " + inService + " en servicio");
            setStageCount("stage-registrados", total);
            setStageCount("stage-activos", active);
            setStageCount("stage-en-servicio", inService);
            setStageCount("stage-mantenimiento", maintenance);
            setStageCount("stage-inactivos", inactive);
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron cargar los vehículos", e.getMessage());
        }
    }

    private void loadInventory() {
        try {
            PartService service = AppContext.getInstance().getPartService();
            inventoryTotal.setText(String.valueOf(service.countParts()));
            inventoryLow.setText(String.valueOf(service.countLowStock()));
            inventoryOut.setText(String.valueOf(service.countOutOfStock()));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el inventario", e.getMessage());
        }
    }

    /** Carga los vehículos con mantenimiento vencido o por vencer (D-13). */
    private void loadMaintenance() {
        try {
            VehicleService service = AppContext.getInstance().getVehicleService();
            LocalDate today = LocalDate.now();
            List<Vehicle> upcoming = service.searchVehicles("", null, null, null).stream()
                    .filter(vehicle -> vehicle.isMaintenanceDue(today))
                    .sorted(Comparator.comparing(Vehicle::getNextMaintenanceDate))
                    .toList();
            maintenanceTable.setData(upcoming);
        } catch (AutoGestException e) {
            maintenanceTable.setData(List.of());
            ErrorDialog.show("No se pudieron cargar los mantenimientos", e.getMessage());
        }
    }

    private void setStageCount(String id, int value) {
        if (root.lookup("#" + id) instanceof Label label) {
            label.setText(String.valueOf(value));
        }
    }

    /** Ítem del diagrama de estado: nombre e ícono. */
    private record Stage(String name, String iconData) {
    }
}
