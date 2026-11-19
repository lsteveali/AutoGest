package com.autogest.controller.customer;

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
import com.autogest.model.Customer;
import com.autogest.model.CustomerStatus;
import com.autogest.service.CustomerService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla «Clientes» (mockup Cliente): indicadores, búsqueda,
 * filtro de estado y tabla paginada con las acciones de cada cliente. Solo
 * traduce eventos de pantalla en llamadas a {@link CustomerService}: no valida
 * reglas de negocio ni conoce el repositorio ni el DAO.
 *
 * <p>Mientras no exista el módulo de órdenes, «Última visita» y la columna del
 * mismo nombre muestran «-» y su filtro queda deshabilitado (decisión D-12). La
 * columna «Vehículos» usa {@code VehicleService.countVehiclesByCustomer()}.</p>
 *
 * <p>Principles: GRASP Controller, Low Coupling, High Cohesion, SRP.</p>
 */
public class CustomerListController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 10;
    private static final String DEFAULT_STATUS_FILTER = "Activo";

    /**
     * Fracción del ancho de la tabla que ocupa cada columna, en el orden en que se agregan.
     * La última («Acciones») no se fija: ocupa el espacio que sobra.
     */
    private static final double[] COLUMN_WIDTHS = {0.17, 0.15, 0.13, 0.17, 0.07, 0.08, 0.11};

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private KpiCard totalCard;
    private KpiCard activeCard;
    private KpiCard newCard;
    private PagedTable<Customer> table;
    private ComboBox<String> statusCombo;
    /** Vehículos de cada cliente (id → cantidad), cargados en cada recarga. */
    private Map<Integer, Integer> vehicleCounts = Map.of();

    private String searchText = "";
    private CustomerStatus statusFilter = CustomerStatus.ACTIVE;

    /** {@inheritDoc} */
    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = buildHeader();
        HBox kpis = buildKpiRow();
        table = buildTable();
        FilterBar filters = buildFilters();

        VBox.setVgrow(table, Priority.ALWAYS);
        root.getChildren().addAll(header, kpis, filters, table);

        // La consulta se hace cuando la pantalla ya está armada; fijar el estado
        // inicial dispara la primera carga a través del filtro.
        Platform.runLater(() -> statusCombo.setValue(DEFAULT_STATUS_FILTER));
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader("Clientes",
                "Administra la información de los clientes registrados en AutoGest.");
        header.setBreadcrumbs(List.of("Inicio", "Clientes"));
        header.setPrimaryAction("+ Nuevo cliente", () -> navigator.navigateTo(View.CUSTOMER_NEW));
        return header;
    }

    private HBox buildKpiRow() {
        totalCard = new KpiCard("Total clientes", "-", "Registrados en AutoGest", IconSet.USERS);
        activeCard = new KpiCard("Clientes activos", "-", "Con estado activo", IconSet.USERS);
        newCard = new KpiCard("Nuevos este mes", "-", "Registrados desde el día 1", IconSet.BAR_CHART);

        HBox row = new HBox();
        row.getStyleClass().add("customer-kpi-row");
        row.setAlignment(Pos.CENTER_LEFT);
        for (KpiCard card : List.of(totalCard, activeCard, newCard)) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.addSearch("Buscar por nombre, documento o correo...", text -> {
            searchText = text;
            reload();
        });
        statusCombo = bar.addCombo("Estado", List.of(CustomerLabels.FILTER_ALL,
                        CustomerLabels.statusText(CustomerStatus.ACTIVE),
                        CustomerLabels.statusText(CustomerStatus.INACTIVE)),
                value -> {
                    statusFilter = CustomerLabels.statusFromText(value);
                    reload();
                });

        // «Última visita» y «Filtros» se ven como en el mockup pero no hacen nada todavía (D-12).
        ComboBox<String> lastVisit = new ComboBox<>();
        lastVisit.setPromptText("Última visita");
        lastVisit.getStyleClass().addAll("input", "filter-combo");
        lastVisit.setDisable(true);
        Button moreFilters = new Button("Filtros");
        moreFilters.getStyleClass().add("btn-secondary");
        moreFilters.setDisable(true);
        bar.getChildren().addAll(lastVisit, moreFilters);
        return bar;
    }

    private PagedTable<Customer> buildTable() {
        PagedTable<Customer> pagedTable = new PagedTable<>(PAGE_SIZE);
        pagedTable.addColumn("Cliente", this::nameCell);
        pagedTable.addColumn("Documento", CustomerLabels::documentText);
        pagedTable.addColumn("Teléfono", customer -> CustomerLabels.phoneText(customer.getPhone()));
        pagedTable.addColumn("Correo", Customer::getEmail);
        pagedTable.addColumn("Vehículos",
                customer -> String.valueOf(vehicleCounts.getOrDefault(customer.getId(), 0)));
        pagedTable.addColumn("Última visita", customer -> CustomerLabels.PLACEHOLDER);
        pagedTable.addColumn("Estado", customer -> {
            Label badge = StatusBadge.of(CustomerLabels.statusText(customer.getStatus()),
                    CustomerLabels.statusType(customer.getStatus()));
            badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            return badge;
        });
        pagedTable.addColumn("Acciones", this::actionsCell);
        applyColumnWidths(pagedTable);
        return pagedTable;
    }

    /**
     * {@code PagedTable} no permite fijar el ancho de sus columnas y por defecto
     * todas miden lo mismo, con lo que documento, teléfono y correo se cortan.
     * Se busca la tabla interna y se reparte su ancho en proporciones.
     */
    private void applyColumnWidths(PagedTable<Customer> pagedTable) {
        if (!(pagedTable.lookup(".table-view") instanceof TableView<?> tableView)) {
            return;
        }
        List<? extends TableColumn<?, ?>> columns = tableView.getColumns();
        for (int i = 0; i < columns.size() && i < COLUMN_WIDTHS.length; i++) {
            columns.get(i).prefWidthProperty().bind(tableView.widthProperty().multiply(COLUMN_WIDTHS[i]));
        }
    }

    private Node nameCell(Customer customer) {
        Label name = new Label(customer.getFullName());
        name.getStyleClass().add("customer-cell-name");
        return name;
    }

    private Node actionsCell(Customer customer) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("customer-actions");
        menu.setFocusTraversable(false);

        MenuItem detail = new MenuItem("Ver detalle");
        detail.setOnAction(event -> navigator.navigateWithParameter(View.CUSTOMER_DETAIL, customer.getId()));

        MenuItem edit = new MenuItem("Editar");
        edit.setOnAction(event -> navigator.navigateWithParameter(View.CUSTOMER_EDIT, customer.getId()));

        MenuItem deactivate = new MenuItem("Desactivar");
        deactivate.setDisable(!customer.isActive());
        deactivate.setOnAction(event -> deactivate(customer));

        menu.getItems().addAll(detail, edit, deactivate);
        return menu;
    }

    /** Recarga la tabla y los indicadores con los filtros actuales. */
    private void reload() {
        try {
            CustomerService customerService = service();
            // Una sola consulta agrupada antes de pintar la tabla (no una por fila).
            vehicleCounts = AppContext.getInstance().getVehicleService().countVehiclesByCustomer();
            table.setData(customerService.searchCustomers(searchText, statusFilter));
            totalCard.setValue(String.valueOf(customerService.countCustomers()));
            activeCard.setValue(String.valueOf(customerService.countActiveCustomers()));
            newCard.setValue(String.valueOf(customerService.countNewCustomersThisMonth()));
        } catch (AutoGestException e) {
            table.setData(List.of());
            ErrorDialog.show("No se pudieron cargar los clientes", e.getMessage());
        }
    }

    /** Desactiva un cliente, previa confirmación. */
    private void deactivate(Customer customer) {
        if (!ConfirmDialog.confirm("Desactivar cliente", "¿Deseas desactivar a " + customer.getFullName()
                + "? Conservará su historial, pero no podrá recibir nuevas órdenes.")) {
            return;
        }
        try {
            service().deactivateCustomer(customer.getId());
            reload();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo desactivar el cliente", e.getMessage());
        }
    }

    /** Obtiene el servicio de clientes en el momento de usarlo, no al construir la pantalla. */
    private CustomerService service() {
        return AppContext.getInstance().getCustomerService();
    }
}
