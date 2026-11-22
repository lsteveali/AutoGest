package com.autogest.controller.vehicle;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.FuelType;
import com.autogest.model.Catalogs.Transmission;
import com.autogest.model.Vehicle;
import com.autogest.model.Catalogs.VehicleType;
import com.autogest.service.VehicleService;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Base de los formularios de vehículo (nuevo y edición). Construye el formulario
 * completo a partir de los datos del dominio, carga los datos cuando se edita,
 * arma el {@link Vehicle} y guarda a través de {@code VehicleService}. Las
 * validaciones de negocio viven en el Service; aquí solo se leen los campos y se
 * muestran los errores.
 *
 * <p>Los formularios concretos aportan el nodo raíz del FXML y si están en modo
 * creación o edición; el resto del comportamiento es común.</p>
 *
 * <p>Principles: GRASP Controller, Template Method, SRP, DIP, Low Cohesion,
 * High Cohesion.</p>
 */
public abstract class VehicleFormController implements Initializable, NavigatorAware {

    private static final int COLUMNS = 2;

    private ViewNavigator navigator;

    private ComboBox<Customer> ownerBox;
    private TextField makeField;
    private TextField modelField;
    private TextField yearField;
    private TextField plateField;
    private TextField vinField;
    private TextField colorField;
    private ComboBox<FuelType> fuelBox;
    private TextField mileageField;
    private ComboBox<VehicleType> typeBox;
    private ComboBox<Transmission> transmissionBox;
    private TextField engineNumberField;
    private TextField registrationCardField;
    private TextField lastMaintenanceMileageField;
    private DatePicker lastMaintenanceDatePicker;
    private TextField nextMaintenanceMileageField;
    private DatePicker nextMaintenanceDatePicker;
    private TextArea observationsArea;

    /** Nodo raíz del FXML donde se monta el formulario. */
    protected abstract VBox getRoot();

    /** {@code true} si el formulario edita un vehículo existente. */
    protected abstract boolean isEditing();

    /** Id del vehículo en edición; 0 mientras no se reciba el parámetro. */
    protected int editingId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildForm();
        Platform.runLater(this::loadOwners);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /** Carga un vehículo existente en el formulario (solo edición). */
    protected void loadVehicle(Vehicle vehicle) {
        ownerBox.setValue(vehicle.getOwner());
        makeField.setText(text(vehicle.getMake()));
        modelField.setText(text(vehicle.getModel()));
        yearField.setText(vehicle.getYear() == 0 ? "" : String.valueOf(vehicle.getYear()));
        plateField.setText(text(vehicle.getLicensePlate()));
        vinField.setText(text(vehicle.getVin()));
        colorField.setText(text(vehicle.getColor()));
        fuelBox.setValue(vehicle.getFuelType());
        mileageField.setText(String.valueOf(vehicle.getMileage()));
        typeBox.setValue(vehicle.getVehicleType());
        transmissionBox.setValue(vehicle.getTransmission());
        engineNumberField.setText(text(vehicle.getEngineNumber()));
        registrationCardField.setText(text(vehicle.getRegistrationCardNumber()));
        lastMaintenanceMileageField.setText(number(vehicle.getLastMaintenanceMileage()));
        lastMaintenanceDatePicker.setValue(vehicle.getLastMaintenanceDate());
        nextMaintenanceMileageField.setText(number(vehicle.getNextMaintenanceMileage()));
        nextMaintenanceDatePicker.setValue(vehicle.getNextMaintenanceDate());
        observationsArea.setText(text(vehicle.getObservations()));
    }

    /** Carga los propietarios activos en el desplegable. */
    private void loadOwners() {
        try {
            ownerBox.getItems().setAll(service().listActiveOwners());
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron cargar los propietarios", e.getMessage());
        }
    }

    /** Construye la cabecera, el formulario y las acciones sobre el nodo raíz. */
    private void buildForm() {
        PageHeader header = new PageHeader(title(),
                "Los campos marcados con * son obligatorios. El resto es opcional.");
        header.setBreadcrumbs(List.of("Vehículos", title()));

        GridPane grid = buildGrid();
        VBox card = new VBox(grid);
        card.getStyleClass().add("card");

        VBox root = getRoot();
        root.getChildren().addAll(header, card, buildActions());
    }

    private GridPane buildGrid() {
        ownerBox = new ComboBox<>();
        ownerBox.getStyleClass().add("input");
        ownerBox.setMaxWidth(Double.MAX_VALUE);
        ownerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getFullName();
            }

            @Override
            public Customer fromString(String text) {
                return null;
            }
        });

        makeField = field("Toyota");
        modelField = field("Corolla");
        yearField = field("2020");
        plateField = field("ABC123");
        vinField = field("1HGCM82633A004352");
        colorField = field("Blanco");
        mileageField = field("50000");
        engineNumberField = field("");
        registrationCardField = field("");
        lastMaintenanceMileageField = field("");
        nextMaintenanceMileageField = field("");

        fuelBox = combo(FuelType.values(), VehicleLabels::fuelText);
        typeBox = combo(VehicleType.values(), VehicleLabels::typeText);
        transmissionBox = combo(Transmission.values(), VehicleLabels::transmissionText);

        lastMaintenanceDatePicker = new DatePicker();
        lastMaintenanceDatePicker.getStyleClass().add("input");
        lastMaintenanceDatePicker.setMaxWidth(Double.MAX_VALUE);
        nextMaintenanceDatePicker = new DatePicker();
        nextMaintenanceDatePicker.getStyleClass().add("input");
        nextMaintenanceDatePicker.setMaxWidth(Double.MAX_VALUE);

        observationsArea = new TextArea();
        observationsArea.getStyleClass().add("input");
        observationsArea.setPrefRowCount(3);
        observationsArea.setPromptText("Notas del vehículo...");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        for (int i = 0; i < COLUMNS; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / COLUMNS);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }

        addPair(grid, 0, "Propietario *", ownerBox, "Marca *", makeField);
        addPair(grid, 1, "Modelo *", modelField, "Año *", yearField);
        addPair(grid, 2, "Placa *", plateField, "VIN", vinField);
        addPair(grid, 3, "Color", colorField, "Combustible *", fuelBox);
        addPair(grid, 4, "Kilometraje *", mileageField, "Clase", typeBox);
        addPair(grid, 5, "Transmisión", transmissionBox, "Número de motor", engineNumberField);
        addPair(grid, 6, "Tarjeta de propiedad", registrationCardField,
                "Último mantenimiento (km)", lastMaintenanceMileageField);
        addPair(grid, 7, "Fecha último mantenimiento", lastMaintenanceDatePicker,
                "Próximo mantenimiento (km)", nextMaintenanceMileageField);
        addPair(grid, 8, "Fecha próximo mantenimiento", nextMaintenanceDatePicker, null, null);
        addFull(grid, 9, "Observaciones", observationsArea);
        return grid;
    }

    private HBox buildActions() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancel = new Button("Cancelar");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(event -> goBack());

        Button save = new Button(isEditing() ? "Guardar cambios" : "Guardar vehículo");
        save.getStyleClass().add("btn-primary");
        save.setOnAction(event -> save());

        HBox actions = new HBox(12, spacer, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    /** Lee el formulario y guarda el vehículo (creación o edición). */
    private void save() {
        Vehicle vehicle;
        try {
            vehicle = readForm();
        } catch (ValidationException e) {
            ErrorDialog.show("Revisa los datos del vehículo", e.getMessage());
            return;
        }
        try {
            if (isEditing()) {
                service().updateVehicle(vehicle);
            } else {
                service().registerVehicle(vehicle);
            }
            goBack();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo guardar el vehículo", e.getMessage());
        }
    }

    /** Arma el {@link Vehicle} con los valores del formulario. */
    private Vehicle readForm() {
        Vehicle vehicle = new Vehicle();
        vehicle.setOwner(ownerBox.getValue());
        vehicle.setMake(value(makeField));
        vehicle.setModel(value(modelField));
        vehicle.setYear(requiredInt(yearField, "El año"));
        vehicle.setLicensePlate(value(plateField));
        vehicle.setVin(value(vinField));
        vehicle.setColor(value(colorField));
        vehicle.setFuelType(fuelBox.getValue());
        vehicle.setMileage(requiredInt(mileageField, "El kilometraje"));
        vehicle.setVehicleType(typeBox.getValue());
        vehicle.setTransmission(transmissionBox.getValue());
        vehicle.setEngineNumber(value(engineNumberField));
        vehicle.setRegistrationCardNumber(value(registrationCardField));
        vehicle.setLastMaintenanceMileage(optionalInt(lastMaintenanceMileageField, "El último mantenimiento (km)"));
        vehicle.setLastMaintenanceDate(lastMaintenanceDatePicker.getValue());
        vehicle.setNextMaintenanceMileage(optionalInt(nextMaintenanceMileageField, "El próximo mantenimiento (km)"));
        vehicle.setNextMaintenanceDate(nextMaintenanceDatePicker.getValue());
        vehicle.setObservations(value(observationsArea));
        if (isEditing()) {
            vehicle.setId(editingId);
        }
        return vehicle;
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }

    // ---------- ayudas de construcción ----------

    private void addPair(GridPane grid, int row, String leftLabel, Node leftField,
                         String rightLabel, Node rightField) {
        if (leftLabel != null) {
            grid.add(labeled(leftLabel, leftField), 0, row);
        }
        if (rightLabel != null) {
            grid.add(labeled(rightLabel, rightField), 1, row);
        }
    }

    private void addFull(GridPane grid, int row, String label, Node field) {
        VBox box = labeled(label, field);
        grid.add(box, 0, row, COLUMNS, 1);
    }

    private VBox labeled(String text, Node field) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        VBox box = new VBox(6, label, field);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private TextField field(String prompt) {
        TextField field = new TextField();
        field.getStyleClass().add("input");
        field.setMaxWidth(Double.MAX_VALUE);
        if (!prompt.isEmpty()) {
            field.setPromptText(prompt);
        }
        return field;
    }

    private <T> ComboBox<T> combo(T[] values, java.util.function.Function<T, String> toString) {
        ComboBox<T> box = new ComboBox<>();
        box.getItems().addAll(values);
        box.getStyleClass().add("input");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toString.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        });
        return box;
    }

    // ---------- ayudas de lectura ----------

    private static String value(TextField field) {
        return field.getText() == null ? null : field.getText().trim();
    }

    private static String value(TextArea area) {
        return area.getText() == null ? null : area.getText().trim();
    }

    private int requiredInt(TextField field, String name) {
        String text = value(field);
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " debe ser un número entero");
        }
    }

    private Integer optionalInt(TextField field, String name) {
        String text = value(field);
        if (text == null || text.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " debe ser un número entero");
        }
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    private static String number(Integer value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** Título del formulario mostrado en la cabecera y el breadcrumb. */
    protected abstract String title();

    /** Obtiene el servicio de vehículos del contexto (diferido). */
    protected VehicleService service() {
        return AppContext.getInstance().getVehicleService();
    }
}
