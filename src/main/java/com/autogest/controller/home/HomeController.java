package com.autogest.controller.home;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigationEvent;
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
import com.autogest.controller.Permissions;
import com.autogest.controller.notification.NotificationLabels;
import com.autogest.model.InvoiceSummary;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.User;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleCounts;
import com.autogest.service.CustomerService;
import com.autogest.service.PartService;
import com.autogest.service.ServiceOrderService;
import com.autogest.service.VehicleService;
import com.autogest.util.Background;
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
 * automotor, las alertas de inventario, los próximos mantenimientos y las
 * notificaciones.
 *
 * <p>Órdenes activas (y cuántas están listas para entregar) y la facturación
 * del mes llegaron con los sprints 4 y 5; sus tarjetas abren el módulo. La
 * tarjeta «Notificaciones» (Sprint 6) muestra los últimos avisos, los que
 * fallaron y permite enviar los recordatorios de mantenimiento. Todo se lee en
 * una sola tarea en segundo plano.</p>
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
    private final VBox notificationsBody = new VBox(10);

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
        header.setPrimaryAction("+ Nueva orden", () -> header.fireEvent(NavigationEvent.open(View.ORDER_NEW)));
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
        ordersCard = new KpiCard("Órdenes activas", "-", "Sin entregar ni cancelar", IconSet.FILE_TEXT);
        billingCard = new KpiCard("Facturación del mes", "-", "Facturas emitidas este mes", IconSet.CREDIT_CARD);
        // NavigationEvent pasa por MainController, que aplica los permisos del rol.
        ordersCard.setOnMouseClicked(event -> ordersCard.fireEvent(NavigationEvent.open(View.ORDERS)));
        billingCard.setOnMouseClicked(event -> billingCard.fireEvent(NavigationEvent.open(View.BILLING)));
        ordersCard.getStyleClass().add("kpi-link");
        billingCard.getStyleClass().add("kpi-link");

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
        // Anchos mínimos para que no se corten el cliente ni el vehículo (marca y modelo, sin la placa,
        // que ya tiene su columna).
        table.addColumn("Cliente", vehicle -> vehicle.getOwner() == null
                ? "Sin propietario" : vehicle.getOwner().getFullName(), 140);
        table.addColumn("Vehículo", vehicle -> vehicle.getMake() + " " + vehicle.getModel(), 130);
        table.addColumn("Placa", Vehicle::getLicensePlate, 80);
        table.addColumn("Fecha", vehicle -> Formatter.date(vehicle.getNextMaintenanceDate()), 100);
        table.addColumn("Estado", vehicle -> StatusBadge.of(
                VehicleLabels.maintenanceText(vehicle), VehicleLabels.maintenanceType(vehicle)), 90);
        table.setOnSelect(vehicle -> {
            if (navigator != null) {
                navigator.navigateWithParameter(View.VEHICLE_DETAIL, vehicle.getId());
            }
        });
        return table;
    }

    /** Tarjeta «Notificaciones»: últimos avisos, fallidos y recordatorios de mantenimiento por enviar (Sprint 6). */
    private VBox buildNotificationsCard() {
        Label title = new Label("Notificaciones");
        title.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(8, title, spacer);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        if (canOpenNotifications()) {
            javafx.scene.control.Button all = new javafx.scene.control.Button("Ver todas");
            all.getStyleClass().add("breadcrumb-link");
            all.setOnAction(event -> all.fireEvent(NavigationEvent.open(View.NOTIFICATIONS)));
            titleRow.getChildren().add(all);
        }

        notificationsBody.getChildren().setAll(note("Cargando…"));
        VBox.setVgrow(notificationsBody, Priority.ALWAYS);

        VBox card = new VBox(14, titleRow, notificationsBody);
        card.getStyleClass().addAll("card", "side-card");
        card.setMinHeight(200);
        return card;
    }

    /** Llena la tarjeta de notificaciones con los datos cargados. */
    private void showNotifications(DashboardData data) {
        notificationsBody.getChildren().clear();
        if (data.recentNotifications().isEmpty()) {
            Label icon = new Label();
            icon.setGraphic(IconSet.icon(IconSet.BELL));
            icon.getStyleClass().add("stage-icon");
            Label empty = new Label("Sin notificaciones por ahora");
            empty.getStyleClass().add("card-title");
            VBox box = new VBox(8, icon, empty, note("Los avisos de facturas, vehículos listos y "
                    + "mantenimientos aparecerán aquí."));
            box.setAlignment(Pos.CENTER);
            notificationsBody.getChildren().add(box);
        }
        for (Notification notification : data.recentNotifications()) {
            notificationsBody.getChildren().add(notificationRow(notification));
        }
        if (data.failedNotifications() > 0) {
            Label failed = note(data.failedNotifications() + (data.failedNotifications() == 1
                    ? " aviso no se pudo enviar." : " avisos no se pudieron enviar.")
                    + " Revisa el correo del cliente y reintenta.");
            failed.getStyleClass().add("home-notification-alert");
            notificationsBody.getChildren().add(failed);
        }
        int reminders = data.pendingReminders();
        if (reminders > 0) {
            Label text = note(reminders + (reminders == 1 ? " vehículo necesita" : " vehículos necesitan")
                    + " recordatorio de mantenimiento.");
            VBox box = new VBox(8, text);
            box.getStyleClass().add("home-reminders");
            if (canOpenNotifications()) {
                javafx.scene.control.Button send = new javafx.scene.control.Button("Enviar recordatorios");
                send.getStyleClass().addAll("btn-secondary", "btn-small");
                send.setOnAction(event -> sendReminders(send));
                box.getChildren().add(send);
            }
            notificationsBody.getChildren().add(box);
        }
    }

    /** Fila de una notificación: asunto, cliente y fecha, y su estado. */
    private Node notificationRow(Notification notification) {
        String subject = notification.getSubject() == null || notification.getSubject().isBlank()
                ? NotificationLabels.typeText(notification.getType()) : notification.getSubject();
        Label title = new Label(subject);
        title.getStyleClass().add("home-notification-title");
        title.setWrapText(true);
        Label meta = new Label((notification.getCustomer() == null || notification.getCustomer().getFirstName() == null
                ? "" : notification.getCustomer().getFullName() + " · ")
                + Formatter.timestamp(notification.getScheduledDate()));
        meta.getStyleClass().add("card-note");
        VBox text = new VBox(2, title, meta);
        HBox.setHgrow(text, Priority.ALWAYS);
        Label badge = StatusBadge.of(NotificationLabels.statusText(notification.getStatus()),
                NotificationLabels.statusType(notification.getStatus()));
        badge.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = new HBox(10, text, badge);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("home-notification");
        return row;
    }

    private void sendReminders(javafx.scene.control.Button button) {
        button.setDisable(true);
        Background.run(() -> AppContext.getInstance().getNotificationEventService()
                        .sendMaintenanceReminders(LocalDate.now()),
                sent -> reload(),
                error -> {
                    button.setDisable(false);
                    ErrorDialog.show("No se pudieron enviar los recordatorios", error instanceof AutoGestException
                            ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo.");
                });
    }

    private static Label note(String text) {
        Label note = new Label(text);
        note.getStyleClass().add("card-note");
        note.setWrapText(true);
        return note;
    }

    /** Solo los roles con acceso a Notificaciones ven «Ver todas» y «Enviar recordatorios». */
    private static boolean canOpenNotifications() {
        User user = Session.getCurrentUser();
        return user != null && Permissions.canAccess(user.getRole(), View.NOTIFICATIONS);
    }

    // ---------- carga de datos ----------

    /** Datos del tablero, leídos juntos en segundo plano. */
    record DashboardData(int customers, int newCustomers, VehicleCounts vehicles, int inactiveVehicles,
                         int parts, int lowStock, int outOfStock, List<Vehicle> maintenance,
                         int activeOrders, int readyOrders, InvoiceSummary monthBilling,
                         List<Notification> recentNotifications, int failedNotifications, int pendingReminders) {
    }

    /** Avisos que se muestran en la tarjeta «Notificaciones». */
    private static final int RECENT_NOTIFICATIONS = 3;

    /** Lee todos los indicadores fuera del hilo de la interfaz y los muestra al terminar. */
    private void reload() {
        Background.run(HomeController::loadData, this::show,
                error -> ErrorDialog.show("No se pudo cargar el resumen del taller",
                        error instanceof AutoGestException ? error.getMessage()
                                : "Revisa la conexión e inténtalo de nuevo."));
    }

    private static DashboardData loadData() {
        AppContext context = AppContext.getInstance();
        CustomerService customers = context.getCustomerService();
        VehicleService vehicles = context.getVehicleService();
        PartService parts = context.getPartService();
        ServiceOrderService orders = context.getServiceOrderService();

        VehicleCounts counts = vehicles.getVehicleCounts();
        LocalDate today = LocalDate.now();
        // Mantenimientos vencidos o por vencer (D-13), del más próximo al más lejano.
        List<Vehicle> upcoming = vehicles.searchVehicles("", null, null, null).stream()
                .filter(vehicle -> vehicle.isMaintenanceDue(today))
                .sorted(Comparator.comparing(Vehicle::getNextMaintenanceDate))
                .toList();
        return new DashboardData(customers.countCustomers(), customers.countNewCustomersThisMonth(),
                counts, Math.max(0, counts.total() - counts.active() - counts.inService()),
                parts.countParts(), parts.countLowStock(), parts.countOutOfStock(), upcoming,
                orders.countActive(), orders.countByStatus(OrderStatus.READY),
                context.getInvoiceService().getMonthSummary(today),
                context.getNotificationService().findRecent(RECENT_NOTIFICATIONS),
                context.getNotificationService().countByStatus(NotificationStatus.FAILED),
                context.getNotificationEventService().pendingMaintenanceReminders(today).size());
    }

    /** Muestra los datos del tablero (visible en el paquete para las pruebas de interfaz). */
    void show(DashboardData data) {
        customersCard.setValue(String.valueOf(data.customers()));
        customersCard.setDetail("+" + data.newCustomers() + " este mes");

        VehicleCounts vehicles = data.vehicles();
        vehiclesCard.setValue(String.valueOf(vehicles.total()));
        vehiclesCard.setDetail(vehicles.active() + " activos · " + vehicles.inService() + " en servicio");
        setStageCount("stage-registrados", vehicles.total());
        setStageCount("stage-activos", vehicles.active());
        setStageCount("stage-en-servicio", vehicles.inService());
        setStageCount("stage-mantenimiento", vehicles.maintenanceDue());
        setStageCount("stage-inactivos", data.inactiveVehicles());

        ordersCard.setValue(String.valueOf(data.activeOrders()));
        ordersCard.setDetail(data.readyOrders() == 0 ? "Ninguna lista para entregar"
                : data.readyOrders() + (data.readyOrders() == 1 ? " lista" : " listas") + " para entregar");

        InvoiceSummary billing = data.monthBilling();
        billingCard.setValue(Formatter.money(billing.billedAmount()));
        billingCard.setDetail(billing.total() == 0 ? "Sin facturas este mes"
                : billing.total() + (billing.total() == 1 ? " factura" : " facturas") + " · "
                + Formatter.money(billing.paidAmount()) + " cobrado");

        inventoryTotal.setText(String.valueOf(data.parts()));
        inventoryLow.setText(String.valueOf(data.lowStock()));
        inventoryOut.setText(String.valueOf(data.outOfStock()));
        maintenanceTable.setData(data.maintenance());
        showNotifications(data);
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
