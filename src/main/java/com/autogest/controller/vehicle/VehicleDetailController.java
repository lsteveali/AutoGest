package com.autogest.controller.vehicle;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Customer;
import com.autogest.model.Vehicle;
import com.autogest.service.VehicleService;
import com.autogest.util.Formatter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla «Detalle del vehículo» (mockup Detalle de
 * Vehículo). Muestra los datos del vehículo, el propietario, el estado del
 * mantenimiento y los demás vehículos del propietario, y permite editar o dar
 * de baja el vehículo.
 *
 * <p>Es un controlador de presentación: solo lee de {@code VehicleService} y
 * muestra; los textos del dominio vienen de {@link VehicleLabels}.</p>
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class VehicleDetailController implements Initializable, ParameterizedController, NavigatorAware {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private Vehicle vehicle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // El contenido se arma cuando llega el id (receiveParameter).
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void receiveParameter(int parameter) {
        try {
            vehicle = service().getDetail(parameter);
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el vehículo", e.getMessage());
            return;
        }
        buildScreen();
    }

    private void buildScreen() {
        root.getChildren().setAll(buildHeader(), buildActions(),
                buildDetailsCard(), buildOwnerVehiclesSection());
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader(vehicle.getShortDescription(),
                VehicleLabels.typeText(vehicle.getVehicleType())
                        + " · " + VehicleLabels.fuelText(vehicle.getFuelType())
                        + " · " + vehicle.getYear());
        header.setBreadcrumbs(List.of("Vehículos", "Detalle del vehículo"));
        header.setPrimaryAction("Editar", () -> openEdit(vehicle.getId()));
        return header;
    }

    private HBox buildActions() {
        Button edit = new Button("Editar");
        edit.getStyleClass().add("btn-secondary");
        edit.setOnAction(event -> openEdit(vehicle.getId()));

        Button deactivate = new Button("Dar de baja");
        deactivate.getStyleClass().add("btn-danger");
        deactivate.setDisable(vehicle.getStatus() == com.autogest.model.VehicleStatus.INACTIVE);
        deactivate.setOnAction(event -> deactivate());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, spacer, edit, deactivate);
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private VBox buildDetailsCard() {
        Label title = new Label("Información del vehículo");
        title.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(14);

        Customer owner = vehicle.getOwner();
        addDetail(grid, 0, "Propietario", owner == null ? "Sin propietario" : owner.getFullName());
        addDetail(grid, 0, "Estado", StatusBadge.of(
                VehicleLabels.statusText(vehicle.getStatus()),
                VehicleLabels.statusType(vehicle.getStatus())), 1);
        addDetail(grid, 1, "Marca", text(vehicle.getMake()));
        addDetail(grid, 1, "Modelo", text(vehicle.getModel()), 1);
        addDetail(grid, 2, "Año", String.valueOf(vehicle.getYear()));
        addDetail(grid, 2, "Placa", text(vehicle.getLicensePlate()), 1);
        addDetail(grid, 3, "VIN", text(vehicle.getVin()));
        addDetail(grid, 3, "Color", text(vehicle.getColor()), 1);
        addDetail(grid, 4, "Combustible", VehicleLabels.fuelText(vehicle.getFuelType()));
        addDetail(grid, 4, "Transmisión", text(VehicleLabels.transmissionText(vehicle.getTransmission())), 1);
        addDetail(grid, 5, "Kilometraje", VehicleLabels.mileage(vehicle.getMileage()));
        addDetail(grid, 5, "Clase", VehicleLabels.typeText(vehicle.getVehicleType()), 1);
        addDetail(grid, 6, "Número de motor", text(vehicle.getEngineNumber()));
        addDetail(grid, 6, "Tarjeta de propiedad", text(vehicle.getRegistrationCardNumber()), 1);
        addDetail(grid, 7, "Último mantenimiento", maintenanceLine(
                vehicle.getLastMaintenanceMileage(), vehicle.getLastMaintenanceDate()));
        addDetail(grid, 7, "Mantenimiento", StatusBadge.of(
                VehicleLabels.maintenanceText(vehicle), VehicleLabels.maintenanceType(vehicle)), 1);
        addDetail(grid, 8, "Próximo mantenimiento", VehicleLabels.nextMaintenanceDateText(vehicle));
        addDetail(grid, 8, "Próximo mantenimiento (km)",
                VehicleLabels.mileage(vehicle.getNextMaintenanceMileage()), 1);
        addDetail(grid, 9, "Observaciones", text(vehicle.getObservations()));

        VBox card = new VBox(16, title, grid);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildOwnerVehiclesSection() {
        Label title = new Label("Otros vehículos del propietario");
        title.getStyleClass().add("card-title");

        PagedTable<Vehicle> table = new PagedTable<>(10);
        table.addColumn("Vehículo", Vehicle::getShortDescription);
        table.addColumn("Año", Vehicle::getYear);
        table.addColumn("Kilometraje", v -> VehicleLabels.mileage(v.getMileage()));
        table.addColumn("Estado", v -> StatusBadge.of(
                VehicleLabels.statusText(v.getStatus()), VehicleLabels.statusType(v.getStatus())));
        table.setOnSelect(v -> openDetail(v.getId()));
        if (vehicle.getOwner() != null) {
            table.setData(ownerVehicles());
        }

        VBox card = new VBox(16, title, table);
        card.getStyleClass().add("card");
        VBox.setVgrow(table, Priority.ALWAYS);
        return card;
    }

    private List<Vehicle> ownerVehicles() {
        try {
            return service().findByCustomer(vehicle.getOwner().getId()).stream()
                    .filter(v -> v.getId() != vehicle.getId())
                    .toList();
        } catch (AutoGestException e) {
            return List.of();
        }
    }

    private void addDetail(GridPane grid, int row, String label, String value) {
        addDetail(grid, row, label, new Label(value == null || value.isEmpty() ? "—" : value), 0);
    }

    private void addDetail(GridPane grid, int row, String label, String value, int column) {
        addDetail(grid, row, label, new Label(value == null || value.isEmpty() ? "—" : value), column);
    }

    private void addDetail(GridPane grid, int row, String label, Node value, int column) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        VBox box = new VBox(4, labelNode, value);
        grid.add(box, column, row);
    }

    private String maintenanceLine(Integer mileage, LocalDate date) {
        String km = mileage == null ? "—" : VehicleLabels.mileage(mileage);
        String when = date == null ? "sin fecha" : Formatter.date(date);
        return km + " · " + when;
    }

    private void deactivate() {
        if (!ConfirmDialog.confirm("Dar de baja el vehículo",
                "¿Deseas dar de baja a " + vehicle.getShortDescription() + "?")) {
            return;
        }
        try {
            service().deleteVehicle(vehicle.getId());
            goBack();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo dar de baja el vehículo", e.getMessage());
        }
    }

    private void openEdit(int id) {
        if (navigator != null) {
            navigator.navigateWithParameter(View.VEHICLE_EDIT, id);
        }
    }

    private void openDetail(int id) {
        if (navigator != null) {
            navigator.navigateWithParameter(View.VEHICLE_DETAIL, id);
        }
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private VehicleService service() {
        return AppContext.getInstance().getVehicleService();
    }
}
