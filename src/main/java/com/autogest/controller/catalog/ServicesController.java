package com.autogest.controller.catalog;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FilterBar;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;
import com.autogest.service.ServiceOfferingService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla «Servicios» (catálogo). Muestra indicadores,
 * filtros (texto, categoría, estado) y tabla paginada con acciones para
 * consultar, editar o dar de baja servicios.
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class ServicesController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 10;
    private static final String FILTER_ALL = "Todos";

    @FXML
    private VBox root;

    private KpiCard totalCard;
    private KpiCard activeCard;
    private KpiCard inactiveCard;
    private PagedTable<ServiceOffering> table;
    private ComboBox<String> categoryCombo;

    private ViewNavigator navigator;
    private String searchText = "";
    private String categoryFilter;
    private ServiceOfferingStatus statusFilter;
    private boolean suppressReload;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = buildHeader();
        HBox kpis = buildKpiRow();
        FilterBar filters = buildFilters();
        table = buildTable();

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(header, kpis, filters, table);

        Platform.runLater(this::reload);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader("Servicios",
                "Administra el catálogo de servicios del taller.");
        header.setBreadcrumbs(List.of("Catálogo", "Servicios"));
        header.setPrimaryAction("+ Nuevo servicio", () -> open(View.SERVICE_NEW, 0));
        return header;
    }

    private HBox buildKpiRow() {
        totalCard = new KpiCard("Servicios registrados", "-", "Total en catálogo", IconSet.WRENCH);
        activeCard = new KpiCard("Activos", "-", "Disponibles para órdenes", IconSet.CHECK);
        inactiveCard = new KpiCard("Inactivos", "-", "No disponibles", IconSet.X);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        addKpi(row, totalCard, activeCard, inactiveCard);
        return row;
    }

    private void addKpi(HBox row, KpiCard... cards) {
        for (KpiCard card : cards) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.addSearch("Buscar por código, nombre o descripción...", text -> {
            searchText = text;
            reload();
        });
        categoryCombo = bar.addCombo("Categoría", List.of(FILTER_ALL), value -> {
            categoryFilter = value == null || FILTER_ALL.equals(value) ? null : value;
            reload();
        });
        bar.addCombo("Estado", options(FILTER_ALL, ServiceLabels.statusOptions()), value -> {
            statusFilter = ServiceLabels.statusFromText(value);
            reload();
        });
        return bar;
    }

    private List<String> options(String first, List<String> values) {
        List<String> options = new ArrayList<>();
        options.add(first);
        options.addAll(values);
        return options;
    }

    private PagedTable<ServiceOffering> buildTable() {
        PagedTable<ServiceOffering> pagedTable = new PagedTable<>(PAGE_SIZE);
        pagedTable.addColumn("Servicio", this::serviceCell);
        pagedTable.addColumn("Categoría", service -> service.getCategory() == null ? "-" : service.getCategory());
        pagedTable.addColumn("Duración", service -> ServiceLabels.duration(service.getDurationHours()));
        pagedTable.addColumn("Precio", service -> ServiceLabels.price(service.getPrice()));
        pagedTable.addColumn("Estado", service -> StatusBadge.of(
                ServiceLabels.statusText(service.getStatus()), ServiceLabels.statusType(service.getStatus())));
        pagedTable.addColumn("Acciones", this::actionsCell);
        pagedTable.setOnSelect(service -> open(View.SERVICE_DETAIL, service.getId()));
        return pagedTable;
    }

    private Node serviceCell(ServiceOffering service) {
        Label name = new Label(service.getName());
        name.getStyleClass().add("user-cell-name");
        Label code = new Label(service.getCode() == null ? "" : service.getCode());
        code.getStyleClass().add("user-cell-username");
        VBox cell = new VBox(2, name, code);
        cell.setAlignment(Pos.CENTER_LEFT);
        return cell;
    }

    private Node actionsCell(ServiceOffering service) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("icon-button");
        menu.setFocusTraversable(false);

        MenuItem detail = new MenuItem("Ver detalle");
        detail.setOnAction(event -> open(View.SERVICE_DETAIL, service.getId()));

        MenuItem edit = new MenuItem("Editar");
        edit.setOnAction(event -> open(View.SERVICE_EDIT, service.getId()));

        MenuItem deactivate = new MenuItem("Dar de baja");
        deactivate.setDisable(service.getStatus() == ServiceOfferingStatus.INACTIVE);
        deactivate.setOnAction(event -> deactivate(service));

        menu.getItems().addAll(detail, edit, deactivate);
        return menu;
    }

    private void open(View view, int id) {
        if (navigator == null) {
            return;
        }
        if (view == View.SERVICE_NEW) {
            navigator.navigateTo(view);
        } else {
            navigator.navigateWithParameter(view, id);
        }
    }

    private void deactivate(ServiceOffering service) {
        if (!ConfirmDialog.confirm("Dar de baja el servicio",
                "¿Deseas dar de baja a " + service.getName() + "?")) {
            return;
        }
        try {
            service().deactivateService(service.getId());
            reload();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo dar de baja el servicio", e.getMessage());
        }
    }

    private void reload() {
        if (suppressReload) {
            return;
        }
        ServiceOfferingService svc = service();
        try {
            table.setData(svc.searchServices(searchText, statusFilter, categoryFilter));
            List<ServiceOffering> all = svc.findAll();
            totalCard.setValue(String.valueOf(all.size()));
            long active = all.stream().filter(ServiceOffering::isActive).count();
            long inactive = all.size() - active;
            activeCard.setValue(String.valueOf(active));
            inactiveCard.setValue(String.valueOf(inactive));
        } catch (AutoGestException e) {
            table.setData(List.of());
            ErrorDialog.show("No se pudieron cargar los servicios", e.getMessage());
        }
        refreshCategories(svc);
    }

    private void refreshCategories(ServiceOfferingService svc) {
        if (categoryCombo == null) {
            return;
        }
        suppressReload = true;
        try {
            List<String> cats = safeCategories(svc);
            String selected = categoryCombo.getValue();
            List<String> opts = options(FILTER_ALL, cats);
            categoryCombo.getItems().setAll(opts);
            String target = selected != null && opts.contains(selected) ? selected : FILTER_ALL;
            if (opts.contains(target)) {
                categoryCombo.setValue(target);
            } else if (!opts.isEmpty()) {
                categoryCombo.setValue(FILTER_ALL);
            }
        } finally {
            suppressReload = false;
        }
    }

    private List<String> safeCategories(ServiceOfferingService svc) {
        try {
            return svc.findCategories();
        } catch (AutoGestException e) {
            return List.of();
        }
    }

    private ServiceOfferingService service() {
        return AppContext.getInstance().getServiceOfferingService();
    }
}
