package com.autogest.controller.order;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FilterBar;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.service.ServiceOrderService;
import com.autogest.util.Formatter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Órdenes de servicio» (mockup Órdenes de servicio V1):
 * indicadores por estado, filtros por texto, estado y mecánico, tabla
 * paginada y el flujo de estados con sus conteos.
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling.</p>
 */
public class OrdersController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 10;

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private KpiCard activeCard;
    private KpiCard diagnosisCard;
    private KpiCard repairCard;
    private KpiCard readyCard;
    private ComboBox<String> mechanicCombo;
    private PagedTable<ServiceOrder> table;
    private FlowPane flow;

    private final List<User> mechanics = new ArrayList<>();
    private String searchText = "";
    private OrderStatus statusFilter;
    private int mechanicFilter;
    private boolean suppressReload;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Órdenes de servicio",
                "Gestiona y realiza seguimiento a las reparaciones de los vehículos.");
        header.setBreadcrumbs(List.of("Inicio", "Órdenes de servicio"));
        header.setPrimaryAction("+ Nueva orden", () -> {
            if (navigator != null) {
                navigator.navigateTo(View.ORDER_NEW);
            }
        });
        table = buildTable();
        flow = new FlowPane(8, 8);
        flow.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(header, buildKpis(),
                FormLayout.card(null, buildFilters(), table),
                FormLayout.card("Flujo de órdenes", flow));
        Platform.runLater(this::reload);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    private HBox buildKpis() {
        activeCard = new KpiCard("Órdenes activas", "-", "Sin entregar ni cancelar", IconSet.FILE_TEXT);
        diagnosisCard = new KpiCard("En diagnóstico", "-", "Revisión del vehículo", IconSet.SEARCH);
        repairCard = new KpiCard("En reparación", "-", "Trabajo en curso", IconSet.WRENCH);
        readyCard = new KpiCard("Listas para entregar", "-", "Esperando al cliente", IconSet.CHECK);
        HBox row = new HBox(16);
        for (KpiCard card : List.of(activeCard, diagnosisCard, repairCard, readyCard)) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.addSearch("Buscar por número, cliente, placa o vehículo...", text -> {
            searchText = text;
            reload();
        });
        List<String> statuses = new ArrayList<>();
        statuses.add(OrderLabels.ALL);
        statuses.addAll(OrderLabels.statusOptions());
        bar.addCombo("Estado", statuses, value -> {
            statusFilter = OrderLabels.statusFromText(value);
            reload();
        });
        mechanicCombo = bar.addCombo("Mecánico", List.of(OrderLabels.ALL), value -> {
            mechanicFilter = mechanics.stream().filter(m -> m.getFullName().equals(value))
                    .mapToInt(User::getId).findFirst().orElse(0);
            reload();
        });
        return bar;
    }

    private PagedTable<ServiceOrder> buildTable() {
        PagedTable<ServiceOrder> pagedTable = new PagedTable<>(PAGE_SIZE);
        // Anchos mínimos para que no se corten el cliente, el vehículo ni el estado
        // («En reparación» es el más largo). Para que la tabla quepa sin barra
        // horizontal, la placa va bajo el vehículo, los títulos de fechas son
        // cortos y la columna del menú «…» no lleva título.
        pagedTable.addColumn("Orden", OrderLabels::number);
        pagedTable.addColumn("Cliente", OrderLabels::customer, 130);
        pagedTable.addColumn("Vehículo", this::vehicleCell, 130);
        pagedTable.addColumn("Ingreso", order -> Formatter.date(order.getEntryDate()), 96);
        pagedTable.addColumn("Entrega", order -> order.getEstimatedDeliveryDate() == null
                ? "-" : Formatter.date(order.getEstimatedDeliveryDate()), 96);
        pagedTable.addColumn("Responsable", OrderLabels::responsible, 110);
        pagedTable.addColumn("Estado", order -> {
            Label badge = StatusBadge.of(OrderLabels.statusText(order.getStatus()),
                    OrderLabels.statusType(order.getStatus()));
            badge.setMinWidth(Region.USE_PREF_SIZE);
            return badge;
        }, 140);
        pagedTable.addColumn("Total", order -> Formatter.money(order.calculateTotal()), 90);
        pagedTable.addColumn("", this::actionsCell, 58);
        pagedTable.setOnSelect(order -> openDetail(order.getId()));
        return pagedTable;
    }

    /** Vehículo arriba y placa debajo, en una sola columna. */
    private Node vehicleCell(ServiceOrder order) {
        Label vehicle = new Label(OrderLabels.vehicle(order));
        vehicle.getStyleClass().add("order-cell-vehicle");
        Label plate = new Label(OrderLabels.plate(order));
        plate.getStyleClass().add("order-cell-plate");
        return new VBox(2, vehicle, plate);
    }

    private Node actionsCell(ServiceOrder order) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("icon-button");
        menu.setFocusTraversable(false);
        MenuItem detail = new MenuItem("Ver detalle");
        detail.setOnAction(event -> openDetail(order.getId()));
        menu.getItems().add(detail);
        return menu;
    }

    private void reload() {
        if (suppressReload) {
            return;
        }
        ServiceOrderService orders = AppContext.getInstance().getServiceOrderService();
        try {
            table.setData(orders.searchOrders(searchText, statusFilter, mechanicFilter));
            activeCard.setValue(String.valueOf(orders.countActive()));
            diagnosisCard.setValue(String.valueOf(orders.countByStatus(OrderStatus.DIAGNOSIS)));
            repairCard.setValue(String.valueOf(orders.countByStatus(OrderStatus.IN_REPAIR)));
            readyCard.setValue(String.valueOf(orders.countByStatus(OrderStatus.READY)));
            buildFlow(orders);
            refreshMechanics();
        } catch (AutoGestException e) {
            table.setData(List.of());
            ErrorDialog.show("No se pudieron cargar las órdenes", e.getMessage());
        }
    }

    /** Flujo RECEIVED → DELIVERED con el conteo de cada estado. */
    private void buildFlow(ServiceOrderService orders) {
        flow.getChildren().clear();
        OrderStatus[] steps = {OrderStatus.RECEIVED, OrderStatus.DIAGNOSIS, OrderStatus.IN_REPAIR,
                OrderStatus.READY, OrderStatus.DELIVERED};
        for (int i = 0; i < steps.length; i++) {
            OrderStatus step = steps[i];
            flow.getChildren().add(StatusBadge.of(OrderLabels.statusText(step) + " (" + orders.countByStatus(step) + ")",
                    OrderLabels.statusType(step)));
            if (i < steps.length - 1) {
                Label arrow = new Label("→");
                arrow.getStyleClass().add("order-flow-arrow");
                flow.getChildren().add(arrow);
            }
        }
    }

    private void refreshMechanics() {
        suppressReload = true;
        try {
            mechanics.clear();
            mechanics.addAll(AppContext.getInstance().getUserService().findActiveMechanics());
            String selected = mechanicCombo.getValue();
            List<String> options = new ArrayList<>();
            options.add(OrderLabels.ALL);
            mechanics.forEach(m -> options.add(m.getFullName()));
            mechanicCombo.getItems().setAll(options);
            mechanicCombo.setValue(selected != null && options.contains(selected) ? selected : OrderLabels.ALL);
        } finally {
            suppressReload = false;
        }
    }

    private void openDetail(int id) {
        if (navigator != null) {
            navigator.navigateWithParameter(View.ORDER_DETAIL, id);
        }
    }
}
