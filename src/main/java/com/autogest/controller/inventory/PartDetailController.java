package com.autogest.controller.inventory;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.InventoryMovement;
import com.autogest.model.MovementType;
import com.autogest.model.Part;
import com.autogest.service.InventoryService;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Detalle del repuesto» (mockup Detalle repuesto V1): datos
 * del repuesto y su proveedor, acciones de stock y el historial de
 * movimientos.
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling.</p>
 */
public class PartDetailController implements Initializable, ParameterizedController, NavigatorAware {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private int partId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // El contenido se arma cuando llega el id.
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void receiveParameter(int parameter) {
        partId = parameter;
        refresh();
    }

    private void refresh() {
        Part part;
        List<InventoryMovement> movements;
        try {
            part = AppContext.getInstance().getPartService().getDetail(partId);
            movements = inventory().findMovementsByPart(partId);
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el repuesto", e.getMessage());
            return;
        }
        root.getChildren().setAll(buildHeader(part), buildActions(part), buildInfo(part), buildMovements(movements));
    }

    private PageHeader buildHeader(Part part) {
        PageHeader header = new PageHeader(part.getName(),
                part.getCategory() == null ? "Repuesto del inventario" : part.getCategory());
        header.setBreadcrumbs(List.of("Inicio", "Inventario", "Detalle"));
        header.setPrimaryAction("Editar", () -> {
            if (navigator != null) {
                navigator.navigateWithParameter(View.PART_EDIT, part.getId());
            }
        });
        return header;
    }

    private HBox buildActions(Part part) {
        HBox actions = FormLayout.actions(
                FormLayout.button("Registrar entrada", "btn-secondary", () -> move(MovementType.ENTRY, part)),
                FormLayout.button("Registrar salida", "btn-secondary", () -> move(MovementType.EXIT, part)),
                FormLayout.button("Ajustar stock", "btn-secondary", () -> move(MovementType.ADJUSTMENT, part)));
        actions.getChildren().add(0, FormLayout.button("Volver", "btn-secondary", this::goBack));
        return actions;
    }

    private VBox buildInfo(Part part) {
        GridPane grid = FormLayout.grid(3);
        String supplier = part.getSupplier() == null || part.getSupplier().getName() == null
                ? "Sin proveedor" : part.getSupplier().getName();
        String contact = part.getSupplier() == null ? "" : part.getSupplier().getContact();
        FormLayout.row(grid, 0,
                FormLayout.detail("Código", part.getCode()),
                FormLayout.detail("Categoría", part.getCategory()),
                FormLayout.detail("Precio unitario", Formatter.money(part.getUnitPrice())));
        FormLayout.row(grid, 1,
                FormLayout.detail("Stock actual", String.valueOf(part.getCurrentStock())),
                FormLayout.detail("Stock mínimo", String.valueOf(part.getMinimumStock())),
                FormLayout.detail("Estado", StatusBadge.of(InventoryLabels.stockText(part.getStatus()),
                        InventoryLabels.stockType(part.getStatus()))));
        FormLayout.row(grid, 2,
                FormLayout.detail("Proveedor", supplier),
                FormLayout.detail("Contacto del proveedor", contact));
        return FormLayout.card("Información del repuesto", grid);
    }

    private VBox buildMovements(List<InventoryMovement> movements) {
        PagedTable<InventoryMovement> table = new PagedTable<>(8);
        table.addColumn("Fecha", m -> Formatter.timestamp(m.getTimestamp()));
        table.addColumn("Tipo", m -> StatusBadge.of(InventoryLabels.movementText(m.getType()),
                InventoryLabels.movementType(m.getType())));
        table.addColumn("Cantidad", m -> InventoryLabels.signedQuantity(m.getType(), m.getQuantity()));
        table.addColumn("Motivo", m -> m.getReason() == null ? "-" : m.getReason());
        table.addColumn("Usuario", m -> m.getUser() == null || m.getUser().getFirstName() == null
                ? "-" : m.getUser().getFullName());
        table.setData(movements);
        return FormLayout.card("Historial de movimientos", table);
    }

    private void move(MovementType type, Part part) {
        MovementDialog.show(type, part, List.of()).ifPresent(request -> {
            try {
                switch (type) {
                    case ENTRY -> inventory().registerEntry(partId, request.quantity(), request.reason(),
                            Session.getCurrentUser());
                    case EXIT -> inventory().registerExit(partId, request.quantity(), request.reason(),
                            Session.getCurrentUser());
                    case ADJUSTMENT -> inventory().registerAdjustment(partId, request.quantity(), request.reason(),
                            Session.getCurrentUser());
                }
                refresh();
            } catch (AutoGestException e) {
                ErrorDialog.show("No se pudo registrar el movimiento", e.getMessage());
            }
        });
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }

    private InventoryService inventory() {
        return AppContext.getInstance().getInventoryService();
    }
}
