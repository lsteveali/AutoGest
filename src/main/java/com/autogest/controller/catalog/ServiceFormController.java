package com.autogest.controller.catalog;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;
import com.autogest.service.ServiceOfferingService;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Base de los formularios de servicio (nuevo y edición). Construye el
 * formulario a partir de los campos del dominio, lo carga cuando se edita y
 * guarda a través de {@code ServiceOfferingService}. Las validaciones de
 * negocio viven en el Service; aquí solo se leen los campos y se muestran los
 * errores.
 *
 * <p>Principles: GRASP Controller, Template Method, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public abstract class ServiceFormController implements Initializable, NavigatorAware {

    private static final int COLUMNS = 2;

    private ViewNavigator navigator;

    private TextField codeField;
    private TextField nameField;
    private ComboBox<String> categoryBox;
    private TextField durationField;
    private TextField priceField;
    private ComboBox<String> statusBox;
    private TextArea descriptionArea;

    /** Nodo raíz del FXML donde se monta el formulario. */
    protected abstract VBox getRoot();

    /** {@code true} si el formulario edita un servicio existente. */
    protected abstract boolean isEditing();

    /** Título del formulario mostrado en la cabecera y el breadcrumb. */
    protected abstract String title();

    /** Id del servicio en edición; 0 mientras no se reciba el parámetro. */
    protected int editingId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buildForm();
        Platform.runLater(this::loadCategories);
        if (!isEditing()) {
            statusBox.setValue(ServiceLabels.statusText(ServiceOfferingStatus.ACTIVE));
        }
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /** Carga un servicio existente en el formulario (solo edición). */
    protected void loadService(ServiceOffering service) {
        codeField.setText(text(service.getCode()));
        nameField.setText(text(service.getName()));
        if (service.getCategory() != null && !service.getCategory().isBlank()) {
            if (!categoryBox.getItems().contains(service.getCategory())) {
                categoryBox.getItems().add(service.getCategory());
            }
            categoryBox.setValue(service.getCategory());
        }
        durationField.setText(service.getDurationHours() <= 0 ? "" : String.valueOf(service.getDurationHours()));
        priceField.setText(service.getPrice() == null ? "" : service.getPrice().toPlainString());
        statusBox.setValue(ServiceLabels.statusText(service.getStatus()));
        descriptionArea.setText(text(service.getDescription()));
    }

    /** Construye la cabecera, el formulario y las acciones sobre el nodo raíz. */
    private void buildForm() {
        PageHeader header = new PageHeader(title(),
                "Los campos marcados con * son obligatorios. El resto es opcional.");
        header.setBreadcrumbs(List.of("Servicios", title()));

        GridPane grid = buildGrid();
        VBox card = new VBox(grid);
        card.getStyleClass().add("card");

        VBox root = getRoot();
        root.getChildren().addAll(header, card, buildActions());
    }

    private GridPane buildGrid() {
        codeField = field("SRV-001");
        nameField = field("Cambio de aceite y filtro");
        durationField = field("1.5");
        priceField = field("120000");

        categoryBox = new ComboBox<>();
        categoryBox.setEditable(true);
        categoryBox.setPromptText("Seleccionar o escribir una categoría");

        statusBox = new ComboBox<>();
        statusBox.getItems().addAll(ServiceLabels.statusText(ServiceOfferingStatus.ACTIVE),
                ServiceLabels.statusText(ServiceOfferingStatus.INACTIVE));

        descriptionArea = new TextArea();
        descriptionArea.getStyleClass().add("input");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setWrapText(true);
        descriptionArea.setPromptText("Descripción del servicio...");

        style(categoryBox);
        style(statusBox);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        for (int i = 0; i < COLUMNS; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / COLUMNS);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }

        addPair(grid, 0, "Código *", codeField, "Nombre *", nameField);
        addPair(grid, 1, "Categoría", categoryBox, "Duración estimada (horas)", durationField);
        addPair(grid, 2, "Precio base *", priceField, "Estado", statusBox);
        addFull(grid, 3, "Descripción", descriptionArea);
        return grid;
    }

    private HBox buildActions() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button cancel = new Button("Cancelar");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(event -> goBack());

        Button save = new Button(isEditing() ? "Guardar cambios" : "Guardar servicio");
        save.getStyleClass().add("btn-primary");
        save.setOnAction(event -> save());

        HBox actions = new HBox(12, spacer, cancel, save);
        actions.setAlignment(Pos.CENTER_RIGHT);
        return actions;
    }

    /** Lee el formulario y guarda el servicio (creación o edición). */
    private void save() {
        ServiceOffering service;
        try {
            service = readForm();
        } catch (ValidationException e) {
            ErrorDialog.show("Revisa los datos del servicio", e.getMessage());
            return;
        }
        try {
            if (isEditing()) {
                service().updateService(service);
            } else {
                service().registerService(service);
            }
            goBack();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo guardar el servicio", e.getMessage());
        }
    }

    /** Arma el {@link ServiceOffering} con los valores del formulario. */
    private ServiceOffering readForm() {
        ServiceOffering service = new ServiceOffering();
        service.setCode(value(codeField));
        service.setName(value(nameField));
        service.setCategory(nullIfBlank(categoryBox.getValue()));
        service.setDurationHours(requiredDouble(durationField, "La duración"));
        service.setPrice(requiredDecimal(priceField, "El precio"));
        service.setStatus("Inactivo".equals(statusBox.getValue())
                ? ServiceOfferingStatus.INACTIVE : ServiceOfferingStatus.ACTIVE);
        service.setDescription(value(descriptionArea));
        if (isEditing()) {
            service.setId(editingId);
        }
        return service;
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }

    /** Carga las categorías existentes en el desplegable (conservando la selección). */
    private void loadCategories() {
        try {
            String selected = categoryBox.getValue();
            List<String> categories = service().findCategories();
            categoryBox.getItems().setAll(categories);
            if (selected != null && !selected.isBlank()) {
                if (!categoryBox.getItems().contains(selected)) {
                    categoryBox.getItems().add(selected);
                }
                categoryBox.setValue(selected);
            }
        } catch (AutoGestException e) {
            // La categoría es opcional: un fallo de carga no bloquea el formulario.
        }
    }

    // ---------- ayudas de construcción ----------

    private void addPair(GridPane grid, int row, String leftLabel, Node leftField,
                         String rightLabel, Node rightField) {
        grid.add(labeled(leftLabel, leftField), 0, row);
        grid.add(labeled(rightLabel, rightField), 1, row);
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
        field.setPromptText(prompt);
        return field;
    }

    private void style(ComboBox<String> box) {
        box.getStyleClass().add("input");
        box.setMaxWidth(Double.MAX_VALUE);
    }

    // ---------- ayudas de lectura ----------

    private static String value(TextField field) {
        String text = field.getText();
        return text == null ? null : text.trim();
    }

    private static String value(TextArea area) {
        return area.getText() == null ? null : area.getText().trim();
    }

    private static String nullIfBlank(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private double requiredDouble(TextField field, String name) {
        String text = value(field);
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " debe ser un número");
        }
    }

    private BigDecimal requiredDecimal(TextField field, String name) {
        String text = value(field);
        if (text == null || text.isEmpty()) {
            throw new ValidationException(name + " es obligatorio");
        }
        try {
            return new BigDecimal(text.replace(",", "."));
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " debe ser un número");
        }
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    /** Obtiene el servicio del catálogo del contexto (diferido). */
    protected ServiceOfferingService service() {
        return AppContext.getInstance().getServiceOfferingService();
    }
}
