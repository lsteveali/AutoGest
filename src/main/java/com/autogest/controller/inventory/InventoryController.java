package com.autogest.controller.inventory;

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
import com.autogest.model.Catalogs.MovementType;
import com.autogest.model.Part;
import com.autogest.model.Catalogs.StockStatus;
import com.autogest.model.Supplier;
import com.autogest.service.InventoryService;
import com.autogest.service.PartService;
import com.autogest.service.SupplierService;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla «Inventario» (mockup Inventario V1): indicadores
 * de stock, filtros por texto, categoría, estado y proveedor, tabla paginada
 * de repuestos con acciones y aviso de productos con stock bajo.
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class InventoryController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 10;

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private KpiCard totalCard;
    private KpiCard availableCard;
    private KpiCard lowCard;
    private KpiCard outCard;
    private ComboBox<String> categoryCombo;
    private ComboBox<String> statusCombo;
    private ComboBox<String> supplierCombo;
    private PagedTable<Part> table;
    private VBox lowStockBanner;
    private Label lowStockTitle;

    private final List<Supplier> suppliers = new ArrayList<>();
    private String searchText = "";
    private String categoryFilter;
    private StockStatus statusFilter;
    private int supplierFilter;
    private boolean suppressReload;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        table = buildTable();
        lowStockBanner = buildLowStockBanner();
        VBox tableCard = FormLayout.card("Inventario de repuestos", buildFilters(), table);
        root.getChildren().addAll(buildHeader(), buildKpis(), tableCard, lowStockBanner);
        Platform.runLater(this::reload);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    private VBox buildHeader() {
        PageHeader header = new PageHeader("Inventario",
                "Controla los repuestos, productos y existencias disponibles en tu taller.");
        header.setBreadcrumbs(List.of("Inicio", "Inventario"));
        header.setPrimaryAction("+ Nuevo repuesto", () -> {
            if (navigator != null) {
                navigator.navigateTo(View.PART_NEW);
            }
        });
        Button entry = FormLayout.button("Registrar entrada", "btn-secondary",
                () -> registerMovement(MovementType.ENTRY, null));
        return new VBox(8, header, FormLayout.actions(entry));
    }

    private HBox buildKpis() {
        totalCard = new KpiCard("Productos registrados", "-", "Repuestos en catálogo", IconSet.BOX);
        availableCard = new KpiCard("Stock disponible", "-", "Por encima del mínimo", IconSet.CHECK);
        lowCard = new KpiCard("Stock bajo", "-", "En o por debajo del mínimo", IconSet.BELL);
        outCard = new KpiCard("Sin existencias", "-", "Stock en cero", IconSet.X);
        HBox row = new HBox(16);
        for (KpiCard card : List.of(totalCard, availableCard, lowCard, outCard)) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.addSearch("Buscar por nombre o código...", text -> {
            searchText = text;
            reload();
        });
        categoryCombo = bar.addCombo("Categoría", List.of(InventoryLabels.ALL), value -> {
            categoryFilter = value == null || InventoryLabels.ALL.equals(value) ? null : value;
            reload();
        });
        statusCombo = bar.addCombo("Stock", withAll(InventoryLabels.stockOptions()), value -> {
            statusFilter = InventoryLabels.stockFromText(value);
            reload();
        });
        supplierCombo = bar.addCombo("Proveedor", List.of(InventoryLabels.ALL), value -> {
            supplierFilter = suppliers.stream().filter(s -> s.getName().equals(value))
                    .mapToInt(Supplier::getId).findFirst().orElse(0);
            reload();
        });
        return bar;
    }

    private PagedTable<Part> buildTable() {
        PagedTable<Part> pagedTable = new PagedTable<>(PAGE_SIZE);
        // Anchos mínimos (Manrope) para que el repuesto, el proveedor y el estado no se corten.
        pagedTable.addColumn("Repuesto", Part::getName, 150);
        pagedTable.addColumn("Código", Part::getCode, 75);
        pagedTable.addColumn("Categoría", part -> part.getCategory() == null ? "-" : part.getCategory(), 90);
        pagedTable.addColumn("Proveedor", part -> part.getSupplier() == null || part.getSupplier().getName() == null
                ? "-" : part.getSupplier().getName(), 115);
        pagedTable.addColumn("Stock", Part::getCurrentStock);
        pagedTable.addColumn("Mínimo", Part::getMinimumStock);
        pagedTable.addColumn("Precio", part -> Formatter.money(part.getUnitPrice()), 90);
        pagedTable.addColumn("Estado", part -> {
            Label badge = StatusBadge.of(InventoryLabels.stockText(part.getStatus()),
                    InventoryLabels.stockType(part.getStatus()));
            badge.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
            return badge;
        }, 105);
        pagedTable.addColumn("Acciones", this::actionsCell);
        pagedTable.setOnSelect(part -> open(View.PART_DETAIL, part.getId()));
        return pagedTable;
    }

    private Node actionsCell(Part part) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("icon-button");
        menu.setFocusTraversable(false);
        menu.getItems().addAll(
                item("Ver detalle", () -> open(View.PART_DETAIL, part.getId())),
                item("Editar", () -> open(View.PART_EDIT, part.getId())),
                item("Registrar entrada", () -> registerMovement(MovementType.ENTRY, part)),
                item("Registrar salida", () -> registerMovement(MovementType.EXIT, part)),
                item("Ajustar stock", () -> registerMovement(MovementType.ADJUSTMENT, part)));
        return menu;
    }

    private VBox buildLowStockBanner() {
        lowStockTitle = new Label();
        lowStockTitle.getStyleClass().add("inventory-alert-title");
        Label detail = new Label("Revisa estos productos para evitar interrupciones en los servicios del taller.");
        detail.getStyleClass().add("inventory-alert-text");
        Button show = FormLayout.button("Ver productos con stock bajo", "btn-primary",
                () -> statusCombo.setValue("Stock bajo"));
        HBox row = new HBox(16, IconSet.icon(IconSet.BELL), new VBox(4, lowStockTitle, detail));
        row.setAlignment(Pos.CENTER_LEFT);
        HBox content = new HBox(16, row, FormLayout.actions(show));
        HBox.setHgrow(content.getChildren().get(1), Priority.ALWAYS);
        VBox banner = new VBox(content);
        banner.getStyleClass().add("inventory-alert");
        banner.setVisible(false);
        banner.setManaged(false);
        return banner;
    }

    private void registerMovement(MovementType type, Part fixed) {
        try {
            List<Part> options = fixed == null ? partService().findAll() : List.of();
            MovementDialog.show(type, fixed, options).ifPresent(request -> {
                InventoryService inventory = AppContext.getInstance().getInventoryService();
                int id = request.part().getId();
                switch (type) {
                    case ENTRY -> inventory.registerEntry(id, request.quantity(), request.reason(),
                            Session.getCurrentUser());
                    case EXIT -> inventory.registerExit(id, request.quantity(), request.reason(),
                            Session.getCurrentUser());
                    case ADJUSTMENT -> inventory.registerAdjustment(id, request.quantity(), request.reason(),
                            Session.getCurrentUser());
                }
                reload();
            });
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo registrar el movimiento", e.getMessage());
        }
    }

    private void reload() {
        if (suppressReload) {
            return;
        }
        PartService parts = partService();
        try {
            table.setData(parts.searchParts(searchText, statusFilter, categoryFilter, supplierFilter));
            int low = parts.countLowStock();
            int out = parts.countOutOfStock();
            totalCard.setValue(String.valueOf(parts.countParts()));
            availableCard.setValue(String.valueOf(parts.countAvailable()));
            lowCard.setValue(String.valueOf(low));
            outCard.setValue(String.valueOf(out));
            lowStockTitle.setText((low + out) + " productos tienen stock bajo o están agotados");
            lowStockBanner.setVisible(low + out > 0);
            lowStockBanner.setManaged(low + out > 0);
            refreshCombos(parts);
        } catch (AutoGestException e) {
            table.setData(List.of());
            ErrorDialog.show("No se pudo cargar el inventario", e.getMessage());
        }
    }

    /** Recarga categorías y proveedores conservando la selección (sin disparar otra recarga). */
    private void refreshCombos(PartService parts) {
        suppressReload = true;
        try {
            keepSelection(categoryCombo, withAll(parts.findCategories()));
            SupplierService supplierService = AppContext.getInstance().getSupplierService();
            suppliers.clear();
            suppliers.addAll(supplierService.findAll());
            keepSelection(supplierCombo, withAll(suppliers.stream().map(Supplier::getName).toList()));
        } finally {
            suppressReload = false;
        }
    }

    private static void keepSelection(ComboBox<String> combo, List<String> options) {
        String selected = combo.getValue();
        combo.getItems().setAll(options);
        combo.setValue(selected != null && options.contains(selected) ? selected : InventoryLabels.ALL);
    }

    private static List<String> withAll(List<String> values) {
        List<String> options = new ArrayList<>();
        options.add(InventoryLabels.ALL);
        options.addAll(values);
        return options;
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }

    private void open(View view, int id) {
        if (navigator != null) {
            navigator.navigateWithParameter(view, id);
        }
    }

    private PartService partService() {
        return AppContext.getInstance().getPartService();
    }
}
