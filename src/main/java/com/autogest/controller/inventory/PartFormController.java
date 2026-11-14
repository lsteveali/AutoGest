package com.autogest.controller.inventory;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Part;
import com.autogest.model.Supplier;
import com.autogest.service.PartService;
import com.autogest.service.SupplierService;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Base de los formularios «Nuevo repuesto» y «Editar repuesto» (mockup Nuevo
 * repuesto V1). Arma el formulario, lo carga en edición y guarda a través de
 * los Services. En edición el stock no se cambia aquí: se mueve con entradas,
 * salidas o ajustes para que quede en el historial.
 *
 * <p>Principles: GRASP Controller, Template Method, SRP, DIP, Low Coupling.</p>
 */
public abstract class PartFormController implements Initializable, NavigatorAware {

    private ViewNavigator navigator;

    private TextField codeField;
    private TextField nameField;
    private ComboBox<String> categoryBox;
    private ComboBox<Supplier> supplierBox;
    private TextField stockField;
    private TextField minimumField;
    private TextField priceField;

    /** Id del repuesto en edición (0 en creación). */
    protected int editingId;
    /** Stock actual del repuesto en edición (no se modifica desde el formulario). */
    private int editingStock;

    /** Nodo raíz del FXML. */
    protected abstract VBox getRoot();

    /** {@code true} si edita un repuesto existente. */
    protected abstract boolean isEditing();

    /** Título de la pantalla. */
    protected abstract String title();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader(title(),
                "Los campos marcados con * son obligatorios. El stock se controla con movimientos.");
        header.setBreadcrumbs(List.of("Inicio", "Inventario", title()));
        getRoot().getChildren().addAll(header, FormLayout.card("Datos del repuesto", buildGrid()), buildActions());
        Platform.runLater(this::loadOptions);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /** Carga un repuesto existente en el formulario (solo edición). */
    protected void loadPart(Part part) {
        editingStock = part.getCurrentStock();
        codeField.setText(FormLayout.orEmpty(part.getCode()));
        nameField.setText(FormLayout.orEmpty(part.getName()));
        if (part.getCategory() != null) {
            if (!categoryBox.getItems().contains(part.getCategory())) {
                categoryBox.getItems().add(part.getCategory());
            }
            categoryBox.setValue(part.getCategory());
        }
        if (part.getSupplier() != null) {
            supplierBox.getItems().stream()
                    .filter(s -> s.getId() == part.getSupplier().getId())
                    .findFirst()
                    .ifPresentOrElse(supplierBox::setValue, () -> {
                        supplierBox.getItems().add(part.getSupplier());
                        supplierBox.setValue(part.getSupplier());
                    });
        }
        stockField.setText(String.valueOf(part.getCurrentStock()));
        minimumField.setText(String.valueOf(part.getMinimumStock()));
        priceField.setText(part.getUnitPrice() == null ? "" : part.getUnitPrice().toPlainString());
    }

    private GridPane buildGrid() {
        codeField = FormLayout.textField("REP-001");
        nameField = FormLayout.textField("Filtro de aceite");
        categoryBox = FormLayout.combo("Seleccionar o escribir una categoría");
        categoryBox.setEditable(true);
        supplierBox = FormLayout.combo("Seleccionar proveedor", Supplier::getName);
        stockField = FormLayout.textField("0");
        stockField.setDisable(isEditing());
        minimumField = FormLayout.textField("5");
        priceField = FormLayout.textField("35000");

        Button newSupplier = FormLayout.button("+ Nuevo proveedor", "btn-secondary", this::createSupplier);
        HBox supplierRow = new HBox(8, supplierBox, newSupplier);
        supplierRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(supplierBox, Priority.ALWAYS);

        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Código *", codeField), FormLayout.labeled("Nombre *", nameField));
        FormLayout.row(grid, 1, FormLayout.labeled("Categoría", categoryBox), FormLayout.labeled("Proveedor", supplierRow));
        FormLayout.row(grid, 2, FormLayout.labeled(isEditing() ? "Stock actual" : "Stock inicial", stockField),
                FormLayout.labeled("Stock mínimo", minimumField));
        FormLayout.row(grid, 3, FormLayout.labeled("Precio unitario *", priceField));
        return grid;
    }

    private HBox buildActions() {
        return FormLayout.actions(
                FormLayout.button("Cancelar", "btn-secondary", this::goBack),
                FormLayout.button(isEditing() ? "Guardar cambios" : "Guardar repuesto", "btn-primary", this::save));
    }

    private void save() {
        Part part;
        try {
            part = readForm();
        } catch (ValidationException e) {
            ErrorDialog.show("Revisa los datos del repuesto", e.getMessage());
            return;
        }
        try {
            if (isEditing()) {
                partService().updatePart(part);
            } else {
                AppContext.getInstance().getInventoryService().registerPart(part, Session.getCurrentUser());
            }
            goBack();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo guardar el repuesto", e.getMessage());
        }
    }

    private Part readForm() {
        Part part = new Part();
        part.setId(editingId);
        part.setCode(FormLayout.text(codeField));
        part.setName(FormLayout.text(nameField));
        String category = categoryBox.getEditor() == null ? categoryBox.getValue() : categoryBox.getEditor().getText();
        part.setCategory(category == null || category.isBlank() ? null : category.trim());
        part.setSupplier(supplierBox.getValue());
        int stock = isEditing() ? editingStock : FormLayout.parseInt(stockField.getText(), "El stock inicial", false);
        if (stock < 0) {
            throw new ValidationException("El stock inicial no puede ser negativo");
        }
        part.setCurrentStock(stock);
        part.setMinimumStock(FormLayout.parseInt(minimumField.getText(), "El stock mínimo", false));
        part.setUnitPrice(FormLayout.parseMoney(priceField.getText(), "El precio unitario"));
        return part;
    }

    /** Carga categorías y proveedores; un fallo aquí no bloquea el formulario. */
    private void loadOptions() {
        try {
            String category = categoryBox.getValue();
            categoryBox.getItems().setAll(partService().findCategories());
            if (category != null) {
                categoryBox.setValue(category);
            }
            Supplier supplier = supplierBox.getValue();
            supplierBox.getItems().setAll(supplierService().findAll());
            if (supplier != null) {
                supplierBox.getItems().stream().filter(s -> s.getId() == supplier.getId())
                        .findFirst().ifPresent(supplierBox::setValue);
            }
        } catch (AutoGestException e) {
            // Las listas son opcionales: el formulario sigue funcionando.
        }
    }

    /** Diálogo corto para registrar un proveedor sin salir del formulario. */
    private void createSupplier() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nuevo proveedor");
        ButtonType save = new ButtonType("Guardar proveedor", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
        TextField name = FormLayout.textField("AutoPartes Colombia");
        TextField phone = FormLayout.textField("300 123 4567");
        TextField email = FormLayout.textField("ventas@proveedor.com");
        GridPane grid = FormLayout.grid(1);
        grid.setPrefWidth(380);
        FormLayout.row(grid, 0, FormLayout.labeled("Nombre *", name));
        FormLayout.row(grid, 1, FormLayout.labeled("Teléfono", phone));
        FormLayout.row(grid, 2, FormLayout.labeled("Correo", email));
        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> answer = dialog.showAndWait();
        if (answer.isEmpty() || answer.get() != save) {
            return;
        }
        Supplier supplier = new Supplier();
        supplier.setName(FormLayout.text(name));
        supplier.setPhone(FormLayout.text(phone));
        supplier.setEmail(FormLayout.text(email));
        try {
            Supplier saved = supplierService().registerSupplier(supplier);
            supplierBox.getItems().add(saved);
            supplierBox.setValue(saved);
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo guardar el proveedor", e.getMessage());
        }
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }

    /** Servicio de repuestos (diferido para no tocar la BD al construir). */
    protected PartService partService() {
        return AppContext.getInstance().getPartService();
    }

    private SupplierService supplierService() {
        return AppContext.getInstance().getSupplierService();
    }
}
