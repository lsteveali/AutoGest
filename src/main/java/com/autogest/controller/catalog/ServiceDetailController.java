package com.autogest.controller.catalog;

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
import com.autogest.model.Part;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;
import com.autogest.service.ServiceOfferingService;
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
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla «Detalle del servicio». Muestra los datos del
 * servicio, permite editarlo o darlo de baja y lista los repuestos que suele
 * requerir.
 *
 * <p>El contenido se arma cuando llega el id (recibido en
 * {@code receiveParameter}).</p>
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class ServiceDetailController implements Initializable, ParameterizedController, NavigatorAware {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private ServiceOffering service;

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
            service = service().getDetail(parameter);
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el servicio", e.getMessage());
            return;
        }
        buildScreen();
    }

    private void buildScreen() {
        root.getChildren().setAll(buildHeader(), buildActions(),
                buildDetailsCard(), buildPartsSection());
    }

    private PageHeader buildHeader() {
        PageHeader header = new PageHeader(service.getName(),
                service.getCategory() == null || service.getCategory().isBlank()
                        ? "Servicio del catálogo del taller" : service.getCategory());
        header.setBreadcrumbs(List.of("Catálogo", "Servicios", "Detalle"));
        header.setPrimaryAction("Editar", () -> openEdit(service.getId()));
        return header;
    }

    private HBox buildActions() {
        Button back = new Button("Volver");
        back.getStyleClass().add("btn-secondary");
        back.setOnAction(event -> goBack());

        Button edit = new Button("Editar");
        edit.getStyleClass().add("btn-secondary");
        edit.setOnAction(event -> openEdit(service.getId()));

        Button deactivate = new Button("Dar de baja");
        deactivate.getStyleClass().add("btn-danger");
        deactivate.setDisable(service.getStatus() == ServiceOfferingStatus.INACTIVE);
        deactivate.setOnAction(event -> deactivate());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(12, back, spacer, edit, deactivate);
        actions.setAlignment(Pos.CENTER_LEFT);
        return actions;
    }

    private VBox buildDetailsCard() {
        Label title = new Label("Información del servicio");
        title.getStyleClass().add("card-title");

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(14);

        addDetail(grid, 0, "Código", text(service.getCode()));
        addDetail(grid, 0, "Categoría", text(service.getCategory()), 1);
        addDetail(grid, 1, "Precio base", ServiceLabels.price(service.getPrice()));
        addDetail(grid, 1, "Duración estimada", ServiceLabels.duration(service.getDurationHours()), 1);
        addDetail(grid, 2, "Estado", StatusBadge.of(
                ServiceLabels.statusText(service.getStatus()),
                ServiceLabels.statusType(service.getStatus())), 0);
        addDetail(grid, 3, "Descripción", text(service.getDescription()), 0, 2);

        VBox card = new VBox(16, title, grid);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox buildPartsSection() {
        Label title = new Label("Repuestos habituales");
        title.getStyleClass().add("card-title");

        PagedTable<Part> table = new PagedTable<>(5);
        table.addColumn("Repuesto", part -> part.getName() == null ? "-" : part.getName());
        table.addColumn("Código", part -> part.getCode() == null ? "-" : part.getCode());
        table.addColumn("Stock", Part::getCurrentStock);
        table.addColumn("Precio unitario", part -> Formatter.money(part.getUnitPrice()));
        table.setData(service.getParts() == null ? List.of() : service.getParts());

        VBox card = new VBox(16, title, table);
        card.getStyleClass().add("card");
        VBox.setVgrow(table, Priority.ALWAYS);
        return card;
    }

    private void addDetail(GridPane grid, int row, String label, String value) {
        addDetail(grid, row, label, value, 0, 1);
    }

    private void addDetail(GridPane grid, int row, String label, String value, int column) {
        addDetail(grid, row, label, value, column, 1);
    }

    private void addDetail(GridPane grid, int row, String label, String value, int column, int colspan) {
        Label valueNode = new Label(value == null || value.isEmpty() ? "—" : value);
        valueNode.setWrapText(true);
        addDetail(grid, row, label, valueNode, column, colspan);
    }

    private void addDetail(GridPane grid, int row, String label, Node value, int column) {
        addDetail(grid, row, label, value, column, 1);
    }

    private void addDetail(GridPane grid, int row, String label, Node value, int column, int colspan) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        VBox box = new VBox(4, labelNode, value);
        GridPane.setHgrow(box, Priority.ALWAYS);
        grid.add(box, column, row, colspan, 1);
    }

    private void deactivate() {
        if (!ConfirmDialog.confirm("Dar de baja el servicio",
                "¿Deseas dar de baja a " + service.getName() + "?")) {
            return;
        }
        try {
            service().deactivateService(service.getId());
            buildScreen();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo dar de baja el servicio", e.getMessage());
        }
    }

    private void openEdit(int id) {
        if (navigator != null) {
            navigator.navigateWithParameter(View.SERVICE_EDIT, id);
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

    private ServiceOfferingService service() {
        return AppContext.getInstance().getServiceOfferingService();
    }
}
