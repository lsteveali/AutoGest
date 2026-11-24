package com.autogest.controller.vehicle;

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
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleCounts;
import com.autogest.model.Catalogs.VehicleStatus;
import com.autogest.model.Catalogs.VehicleType;
import com.autogest.service.VehicleService;
import com.autogest.util.Background;
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
 * Controlador de la pantalla «Vehículos» (mockup Lista de Vehículos). Muestra
 * los indicadores, los filtros (texto, marca, estado y clase) y la tabla de
 * vehículos, y permite abrir el detalle, editar o dar de baja un vehículo, así
 * como registrar uno nuevo.
 *
 * <p>Es un controlador de presentación: traduce los eventos en llamadas a
 * {@code VehicleService} y muestra los errores; las validaciones viven en el
 * Service y los textos en {@link VehicleLabels}.</p>
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class VehiclesController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 10;
    private static final String FILTER_ALL = "Todas";

    @FXML
    private VBox root;

    private KpiCard totalCard;
    private KpiCard activeCard;
    private KpiCard inServiceCard;
    private KpiCard maintenanceCard;
    private PagedTable<Vehicle> table;
    private ComboBox<String> makeCombo;

    private ViewNavigator navigator;
    private String searchText = "";
    private String makeFilter;
    private VehicleStatus statusFilter;
    private VehicleType typeFilter;
    private boolean suppressReload;
    /** Número de la última recarga pedida; descarta resultados de recargas viejas. */
    private int reloadRequest;

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
        PageHeader header = new PageHeader("Vehículos",
                "Consulta el parque automotor, su propietario y el mantenimiento programado.");
        header.setBreadcrumbs(List.of("Inicio", "Vehículos"));
        header.setPrimaryAction("+ Nuevo vehículo", () -> open(View.VEHICLE_NEW, 0));
        return header;
    }

    private HBox buildKpiRow() {
        totalCard = new KpiCard("Vehículos registrados", "-", "Total en el taller", IconSet.CAR);
        activeCard = new KpiCard("Vehículos activos", "-", "Disponibles", IconSet.CHECK);
        inServiceCard = new KpiCard("En servicio", "-", "Con orden en curso", IconSet.WRENCH);
        maintenanceCard = new KpiCard("Mantenimiento próximo", "-", "Vence en 30 días", IconSet.CALENDAR);

        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        addKpi(row, totalCard, activeCard, inServiceCard, maintenanceCard);
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
        bar.addSearch("Buscar por placa, marca, modelo o propietario...", text -> {
            searchText = text;
            reload();
        });
        makeCombo = bar.addCombo("Marca", List.of(FILTER_ALL), value -> {
            makeFilter = value == null || FILTER_ALL.equals(value) ? null : value;
            reload();
        });
        bar.addCombo("Estado", options(FILTER_ALL, VehicleLabels.statusOptions()), value -> {
            statusFilter = VehicleLabels.statusFromText(value);
            reload();
        });
        bar.addCombo("Clase", options(FILTER_ALL, VehicleLabels.typeOptions()), value -> {
            typeFilter = VehicleLabels.typeFromText(value);
            reload();
        });
        return bar;
    }

    /** Combina una opción «Todas» con las opciones reales del filtro. */
    private List<String> options(String first, List<String> values) {
        List<String> options = new ArrayList<>();
        options.add(first);
        options.addAll(values);
        return options;
    }

    /** Distintivo que nunca se corta («En servicio» es el más largo). */
    private static Label badge(String text, StatusBadge.BadgeType type) {
        Label badge = StatusBadge.of(text, type);
        badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        return badge;
    }

    private PagedTable<Vehicle> buildTable() {
        PagedTable<Vehicle> pagedTable = new PagedTable<>(PAGE_SIZE);
        // Anchos mínimos para que el vehículo, el propietario y los estados no se corten (Manrope).
        pagedTable.addColumn("Vehículo", this::vehicleCell, 190);
        pagedTable.addColumn("Propietario", vehicle -> ownerText(vehicle), 160);
        pagedTable.addColumn("Año", Vehicle::getYear, 60);
        pagedTable.addColumn("Kilometraje", vehicle -> VehicleLabels.mileage(vehicle.getMileage()), 100);
        pagedTable.addColumn("Mantenimiento", vehicle -> badge(VehicleLabels.maintenanceText(vehicle),
                VehicleLabels.maintenanceType(vehicle)), 120);
        pagedTable.addColumn("Estado", vehicle -> badge(VehicleLabels.statusText(vehicle.getStatus()),
                VehicleLabels.statusType(vehicle.getStatus())), 115);
        pagedTable.addColumn("Acciones", this::actionsCell);
        pagedTable.setOnSelect(vehicle -> open(View.VEHICLE_DETAIL, vehicle.getId()));
        return pagedTable;
    }

    /** Marca, modelo y placa en una sola línea, como el resto de las tablas. */
    private String vehicleCell(Vehicle vehicle) {
        return vehicle.getShortDescription();
    }

    private String ownerText(Vehicle vehicle) {
        return vehicle.getOwner() == null ? "Sin propietario" : vehicle.getOwner().getFullName();
    }

    private Node actionsCell(Vehicle vehicle) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("icon-button");
        menu.setFocusTraversable(false);

        MenuItem detail = new MenuItem("Ver detalle");
        detail.setOnAction(event -> open(View.VEHICLE_DETAIL, vehicle.getId()));

        MenuItem edit = new MenuItem("Editar");
        edit.setOnAction(event -> open(View.VEHICLE_EDIT, vehicle.getId()));

        MenuItem delete = new MenuItem("Dar de baja");
        delete.setDisable(vehicle.getStatus() == VehicleStatus.INACTIVE);
        delete.setOnAction(event -> deactivate(vehicle));

        menu.getItems().addAll(detail, edit, delete);
        return menu;
    }

    /** Navega a una pantalla del módulo pasando el id del vehículo. */
    private void open(View view, int vehicleId) {
        if (navigator == null) {
            return;
        }
        if (view == View.VEHICLE_NEW) {
            navigator.navigateTo(view);
        } else {
            navigator.navigateWithParameter(view, vehicleId);
        }
    }

    /** Da de baja al vehículo, previa confirmación. */
    private void deactivate(Vehicle vehicle) {
        if (!ConfirmDialog.confirm("Dar de baja el vehículo",
                "¿Deseas dar de baja a " + vehicle.getShortDescription() + "?")) {
            return;
        }
        try {
            service().deleteVehicle(vehicle.getId());
            reload();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo dar de baja el vehículo", e.getMessage());
        }
    }

    /** Datos de la pantalla leídos en segundo plano. */
    private record ScreenData(List<Vehicle> vehicles, VehicleCounts counts, List<String> makes) {
    }

    /**
     * Recarga la tabla, los indicadores y las marcas del filtro. Las consultas
     * corren en segundo plano para no congelar la ventana; si mientras tanto
     * se pide otra recarga (por ejemplo, al escribir en el buscador), el
     * resultado viejo se descarta.
     */
    private void reload() {
        if (suppressReload) {
            return;
        }
        int request = ++reloadRequest;
        String text = searchText;
        String make = makeFilter;
        VehicleStatus status = statusFilter;
        VehicleType type = typeFilter;
        Background.run(() -> {
            VehicleService vehicleService = service();
            return new ScreenData(vehicleService.searchVehicles(text, make, status, type),
                    vehicleService.getVehicleCounts(), safeMakes(vehicleService));
        }, data -> {
            if (request != reloadRequest) {
                return;
            }
            table.setData(data.vehicles());
            totalCard.setValue(String.valueOf(data.counts().total()));
            activeCard.setValue(String.valueOf(data.counts().active()));
            inServiceCard.setValue(String.valueOf(data.counts().inService()));
            maintenanceCard.setValue(String.valueOf(data.counts().maintenanceDue()));
            refreshMakes(data.makes());
        }, error -> {
            if (request != reloadRequest) {
                return;
            }
            table.setData(List.of());
            ErrorDialog.show("No se pudieron cargar los vehículos", error instanceof AutoGestException
                    ? error.getMessage() : "Ocurrió un error inesperado.");
        });
    }

    /** Actualiza el desplegable de marcas conservando la selección actual. */
    private void refreshMakes(List<String> makes) {
        if (makeCombo == null) {
            return;
        }
        suppressReload = true;
        try {
            List<String> options = options(FILTER_ALL, makes);
            String selected = makeCombo.getValue();
            makeCombo.getItems().setAll(options);
            String target = selected != null && options.contains(selected) ? selected : FILTER_ALL;
            if (options.contains(target)) {
                makeCombo.setValue(target);
            } else if (!options.isEmpty()) {
                makeCombo.setValue(FILTER_ALL);
            }
        } finally {
            suppressReload = false;
        }
    }

    private List<String> safeMakes(VehicleService vehicleService) {
        try {
            return vehicleService.findMakes();
        } catch (AutoGestException e) {
            return List.of();
        }
    }

    /**
     * Obtiene el servicio de vehículos del contexto de la aplicación. Es
     * diferido para no inicializar la base de datos al abrir la pantalla si aún
     * no hay configuración local.
     */
    private VehicleService service() {
        return AppContext.getInstance().getVehicleService();
    }
}
