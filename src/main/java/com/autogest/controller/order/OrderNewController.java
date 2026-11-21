package com.autogest.controller.order;

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
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.model.OrderLine;
import com.autogest.model.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOfferingStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Vehicle;
import com.autogest.model.VehicleStatus;
import com.autogest.service.ServiceOrderService;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Nueva orden de servicio» (mockup Nueva orden V1): elige
 * cliente y vehículo, datos de ingreso, motivo, servicios y repuestos, y
 * muestra el resumen con los totales que calcula el modelo
 * ({@code ServiceOrder.calculateTotal}). Si recibe el id de un vehículo
 * (desde el detalle del vehículo), lo deja preseleccionado.
 *
 * <p>Principles: GRASP Controller, Information Expert (los totales los
 * calcula el modelo), SRP, DIP, Low Coupling.</p>
 */
public class OrderNewController implements Initializable, NavigatorAware, ParameterizedController {

    /** Alto de una fila de las tablas de servicios y repuestos (con el botón «Quitar»). */
    private static final double LINE_ROW_HEIGHT = 52;

    private static final List<String> COMMON_REASONS = List.of(
            "Mantenimiento preventivo y revisión general",
            "Cambio de aceite y filtros",
            "Revisión del sistema de frenos",
            "Falla mecánica",
            "Ruido o vibración",
            "Revisión eléctrica");

    @FXML
    private VBox root;

    private ViewNavigator navigator;

    private ComboBox<Customer> customerBox;
    private ComboBox<Vehicle> vehicleBox;
    private DatePicker entryDatePicker;
    private DatePicker deliveryPicker;
    private TextField mileageField;
    private ComboBox<User> mechanicBox;
    private ComboBox<String> reasonBox;
    private TextArea observationsArea;
    private ComboBox<ServiceOffering> serviceBox;
    private ComboBox<Part> partBox;
    private TextField partQuantityField;
    private PagedTable<ServiceLine> servicesTable;
    private PagedTable<PartLine> partsTable;
    private Label servicesSubtotal;
    private Label partsSubtotal;

    private final List<ServiceLine> serviceLines = new ArrayList<>();
    private final List<PartLine> partLines = new ArrayList<>();
    private final GridPane summary = FormLayout.grid(2);
    private int pendingVehicleId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Nueva orden de servicio",
                "Registra el ingreso de un vehículo y crea una nueva orden de trabajo.");
        header.setBreadcrumbs(List.of("Inicio", "Órdenes de servicio", "Nueva orden"));

        // Dos columnas de alto parecido (el área de contenido mide ~1000 px con la
        // ventana mínima): datos de la orden a la izquierda; servicios, repuestos y
        // estado inicial a la derecha. Las tablas toman el alto de sus filas (antes
        // crecían y dejaban un hueco en blanco bajo la columna izquierda). El
        // resumen va abajo, a todo el ancho.
        VBox left = column(buildCustomerCard(), buildEntryCard(), buildReasonCard(), buildObservationsCard());
        VBox right = column(buildServicesCard(), buildPartsCard(), buildStatusCard());
        HBox columns = new HBox(16, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(420);
        right.setPrefWidth(520);
        VBox summaryCard = FormLayout.card("Resumen de la orden", summary);

        root.getChildren().addAll(header, columns, summaryCard, FormLayout.actions(
                FormLayout.button("Cancelar", "btn-secondary", this::goBack),
                FormLayout.button("Crear orden", "btn-primary", this::save)));
        refreshSummary();
        Platform.runLater(this::loadOptions);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /** Recibe el id de un vehículo para dejarlo preseleccionado con su propietario. */
    @Override
    public void receiveParameter(int vehicleId) {
        pendingVehicleId = vehicleId;
    }

    // ---------- tarjetas ----------

    private VBox buildCustomerCard() {
        customerBox = FormLayout.combo("Seleccionar cliente", c -> c.getFullName() + ", "
                + c.getDocumentType() + " " + c.getDocumentNumber());
        vehicleBox = FormLayout.combo("Seleccionar vehículo", OrderLabels::vehicleOption);
        customerBox.valueProperty().addListener((obs, old, customer) -> loadVehicles(customer));
        vehicleBox.valueProperty().addListener((obs, old, vehicle) -> {
            mileageField.setText(vehicle == null ? "" : String.valueOf(vehicle.getMileage()));
            refreshSummary();
        });
        GridPane grid = FormLayout.grid(1);
        FormLayout.row(grid, 0, FormLayout.labeled("Cliente *", customerBox));
        FormLayout.row(grid, 1, FormLayout.labeled("Vehículo *", vehicleBox));
        return FormLayout.card("Cliente y vehículo", grid);
    }

    private VBox buildEntryCard() {
        entryDatePicker = new DatePicker(LocalDate.now());
        deliveryPicker = new DatePicker();
        styleDate(entryDatePicker);
        styleDate(deliveryPicker);
        mileageField = FormLayout.textField("48230");
        mileageField.textProperty().addListener((obs, old, text) -> refreshSummary());
        mechanicBox = FormLayout.combo("Asignar posteriormente", User::getFullName);
        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Fecha de ingreso *", entryDatePicker),
                FormLayout.labeled("Kilometraje actual", mileageField));
        FormLayout.row(grid, 1, FormLayout.labeled("Entrega estimada", deliveryPicker),
                FormLayout.labeled("Mecánico responsable", mechanicBox));
        return FormLayout.card("Información de ingreso", grid);
    }

    private VBox buildReasonCard() {
        reasonBox = FormLayout.combo("Escribe o elige el motivo");
        reasonBox.setEditable(true);
        reasonBox.getItems().setAll(COMMON_REASONS);
        return FormLayout.card("Motivo del ingreso", FormLayout.labeled("Motivo de ingreso *", reasonBox));
    }

    private VBox buildServicesCard() {
        serviceBox = FormLayout.combo("Seleccionar servicio",
                s -> s.getName() + " · " + Formatter.money(s.getPrice()));
        Button add = FormLayout.button("Agregar", "btn-secondary", this::addService);
        HBox picker = new HBox(8, serviceBox, add);
        HBox.setHgrow(serviceBox, Priority.ALWAYS);

        servicesTable = new PagedTable<>(5);
        servicesTable.addColumn("Servicio", ServiceLine::getDescription);
        servicesTable.addColumn("Precio estimado", line -> Formatter.money(line.calculateTotal()));
        servicesTable.addColumn("", line -> removeButton(() -> {
            serviceLines.remove(line);
            refreshLines();
        }));
        servicesTable.fitHeightToRows(LINE_ROW_HEIGHT);
        servicesSubtotal = new Label();
        servicesSubtotal.getStyleClass().add("order-subtotal");
        return FormLayout.card("Servicios", picker, servicesTable, servicesSubtotal);
    }

    private VBox buildPartsCard() {
        partBox = FormLayout.combo("Seleccionar repuesto",
                p -> p.getName() + " · " + Formatter.money(p.getUnitPrice()) + " · stock " + p.getCurrentStock());
        partQuantityField = FormLayout.textField("1");
        partQuantityField.setText("1");
        partQuantityField.setPrefColumnCount(3);
        partQuantityField.setMaxWidth(70);
        Button add = FormLayout.button("Agregar", "btn-secondary", this::addPart);
        HBox picker = new HBox(8, partBox, partQuantityField, add);
        HBox.setHgrow(partBox, Priority.ALWAYS);

        partsTable = new PagedTable<>(5);
        partsTable.addColumn("Repuesto", PartLine::getDescription);
        partsTable.addColumn("Cantidad", PartLine::getQuantity);
        partsTable.addColumn("Precio unitario", line -> Formatter.money(line.getUnitPrice()));
        partsTable.addColumn("Subtotal", line -> Formatter.money(line.calculateTotal()));
        partsTable.addColumn("", line -> removeButton(() -> {
            partLines.remove(line);
            refreshLines();
        }));
        partsTable.fitHeightToRows(LINE_ROW_HEIGHT);
        Label note = new Label("Los repuestos se descontarán del inventario cuando la orden pase a En reparación.");
        note.setWrapText(true);
        note.getStyleClass().add("page-subtitle");
        partsSubtotal = new Label();
        partsSubtotal.getStyleClass().add("order-subtotal");
        return FormLayout.card("Repuestos", picker, partsTable, note, partsSubtotal);
    }

    private VBox buildObservationsCard() {
        observationsArea = FormLayout.textArea("Agregar observaciones internas sobre la orden...", 3);
        VBox card = FormLayout.card("Observaciones", observationsArea);
        // Última tarjeta de la columna izquierda: se estira hasta el alto de la
        // derecha (y el área de texto con ella) para que no quede un hueco debajo.
        VBox.setVgrow(card, Priority.ALWAYS);
        VBox.setVgrow(observationsArea, Priority.ALWAYS);
        card.setMaxHeight(Double.MAX_VALUE);
        return card;
    }

    private VBox buildStatusCard() {
        Label text = new Label("La orden comenzará en estado Recibido. Posteriormente podrá avanzar a "
                + "Diagnóstico, En reparación, Listo y Entregado.");
        text.setWrapText(true);
        HBox status = new HBox(8, new Label("Estado inicial:"), StatusBadge.of(
                OrderLabels.statusText(OrderStatus.RECEIVED), OrderLabels.statusType(OrderStatus.RECEIVED)));
        status.setAlignment(Pos.CENTER_LEFT);
        return FormLayout.card(null, status, text);
    }

    // ---------- acciones ----------

    private void addService() {
        try {
            ServiceLine line = orders().buildServiceLine(serviceBox.getValue(), 1);
            boolean repeated = serviceLines.stream()
                    .anyMatch(l -> l.getServiceOffering().getId() == line.getServiceOffering().getId());
            if (repeated) {
                throw new ValidationException("Ese servicio ya está en la orden");
            }
            serviceLines.add(line);
            serviceBox.setValue(null);
            refreshLines();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo agregar el servicio", e.getMessage());
        }
    }

    private void addPart() {
        try {
            int quantity = FormLayout.parseInt(partQuantityField.getText(), "La cantidad", true);
            PartLine line = orders().buildPartLine(partBox.getValue(), quantity);
            boolean repeated = partLines.stream().anyMatch(l -> l.getPart().getId() == line.getPart().getId());
            if (repeated) {
                throw new ValidationException("Ese repuesto ya está en la orden; quítalo para cambiar la cantidad");
            }
            partLines.add(line);
            partBox.setValue(null);
            partQuantityField.setText("1");
            refreshLines();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo agregar el repuesto", e.getMessage());
        }
    }

    private void save() {
        ServiceOrder order;
        Integer mileage;
        try {
            order = readForm();
            String mileageText = FormLayout.text(mileageField);
            mileage = mileageText == null ? null : FormLayout.parseInt(mileageText, "El kilometraje", true);
        } catch (ValidationException e) {
            ErrorDialog.show("Revisa los datos de la orden", e.getMessage());
            return;
        }
        try {
            ServiceOrder saved = orders().createOrder(order, mileage, Session.getCurrentUser());
            if (navigator != null) {
                navigator.navigateWithParameter(View.ORDER_DETAIL, saved.getId());
            }
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo crear la orden", e.getMessage());
        }
    }

    private ServiceOrder readForm() {
        if (customerBox.getValue() == null) {
            throw new ValidationException("Selecciona el cliente");
        }
        ServiceOrder order = new ServiceOrder();
        order.setVehicle(vehicleBox.getValue());
        order.setResponsible(mechanicBox.getValue());
        order.setEntryDate(entryDatePicker.getValue());
        order.setEstimatedDeliveryDate(deliveryPicker.getValue());
        order.setEntryReason(reasonBox.getEditor().getText());
        order.setObservations(FormLayout.text(observationsArea));
        List<OrderLine> lines = new ArrayList<>(serviceLines);
        lines.addAll(partLines);
        order.setLines(lines);
        return order;
    }

    // ---------- carga de datos ----------

    private void loadOptions() {
        try {
            AppContext context = AppContext.getInstance();
            customerBox.getItems().setAll(context.getCustomerService().searchCustomers(null, CustomerStatus.ACTIVE));
            mechanicBox.getItems().setAll(context.getUserService().findActiveMechanics());
            serviceBox.getItems().setAll(context.getServiceOfferingService()
                    .searchServices(null, ServiceOfferingStatus.ACTIVE, null));
            partBox.getItems().setAll(context.getPartService().findAll().stream()
                    .filter(p -> p.getCurrentStock() > 0).toList());
            if (pendingVehicleId > 0) {
                preselectVehicle(pendingVehicleId);
            }
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron cargar los datos del formulario", e.getMessage());
        }
    }

    private void preselectVehicle(int vehicleId) {
        Vehicle vehicle = AppContext.getInstance().getVehicleService().getDetail(vehicleId);
        if (vehicle.getOwner() == null) {
            return;
        }
        customerBox.getItems().stream().filter(c -> c.getId() == vehicle.getOwner().getId())
                .findFirst().ifPresent(customer -> {
                    customerBox.setValue(customer);
                    vehicleBox.getItems().stream().filter(v -> v.getId() == vehicleId)
                            .findFirst().ifPresent(vehicleBox::setValue);
                });
    }

    private void loadVehicles(Customer customer) {
        vehicleBox.setValue(null);
        if (customer == null) {
            vehicleBox.getItems().clear();
            return;
        }
        try {
            vehicleBox.getItems().setAll(AppContext.getInstance().getVehicleService()
                    .findByCustomer(customer.getId()).stream()
                    .filter(v -> v.getStatus() != VehicleStatus.INACTIVE).toList());
            if (vehicleBox.getItems().size() == 1) {
                vehicleBox.setValue(vehicleBox.getItems().get(0));
            }
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron cargar los vehículos", e.getMessage());
        }
        refreshSummary();
    }

    // ---------- resumen ----------

    private void refreshLines() {
        servicesTable.setData(new ArrayList<>(serviceLines));
        partsTable.setData(new ArrayList<>(partLines));
        refreshSummary();
    }

    /** Recalcula el resumen; los totales los calcula {@link ServiceOrder} (Information Expert). */
    private void refreshSummary() {
        BigDecimal services = subtotal(new ArrayList<>(serviceLines));
        BigDecimal parts = subtotal(new ArrayList<>(partLines));
        ServiceOrder draft = new ServiceOrder();
        List<OrderLine> all = new ArrayList<>(serviceLines);
        all.addAll(partLines);
        draft.setLines(all);

        if (servicesSubtotal != null) {
            servicesSubtotal.setText("Subtotal servicios: " + Formatter.money(services));
            partsSubtotal.setText("Subtotal repuestos: " + Formatter.money(parts));
        }
        Customer customer = customerBox == null ? null : customerBox.getValue();
        Vehicle vehicle = vehicleBox == null ? null : vehicleBox.getValue();
        summary.getChildren().clear();
        int row = 0;
        row = summaryRow(row, "Cliente", customer == null ? "-" : customer.getFullName());
        row = summaryRow(row, "Vehículo", vehicle == null ? "-" : vehicle.getMake() + " " + vehicle.getModel());
        row = summaryRow(row, "Placa", vehicle == null ? "-" : vehicle.getLicensePlate());
        String mileage = mileageField == null ? null : FormLayout.text(mileageField);
        row = summaryRow(row, "Kilometraje", mileage == null ? "-" : mileage + " km");
        row = summaryRow(row, "Servicios", Formatter.money(services));
        row = summaryRow(row, "Repuestos", Formatter.money(parts));
        row = summaryRow(row, "Subtotal estimado", Formatter.money(draft.calculateSubtotal()));
        row = summaryRow(row, "Impuestos", Formatter.money(draft.calculateTaxes()));
        Label total = new Label(Formatter.money(draft.calculateTotal()));
        total.getStyleClass().add("order-total");
        Label totalLabel = new Label("Total estimado");
        totalLabel.getStyleClass().add("order-total-label");
        summary.add(totalLabel, 0, row);
        summary.add(total, 1, row);
    }

    private int summaryRow(int row, String label, String value) {
        Label name = new Label(label);
        name.getStyleClass().add("field-label");
        Label text = new Label(value);
        text.setWrapText(true);
        summary.add(name, 0, row);
        summary.add(text, 1, row);
        return row + 1;
    }

    private static BigDecimal subtotal(List<OrderLine> lines) {
        ServiceOrder draft = new ServiceOrder();
        draft.setLines(lines);
        return draft.calculateSubtotal();
    }

    // ---------- ayudas ----------

    private static VBox column(Node... cards) {
        VBox column = new VBox(16, cards);
        column.setMaxWidth(Double.MAX_VALUE);
        return column;
    }

    private static void styleDate(DatePicker picker) {
        picker.getStyleClass().add("input");
        picker.setMaxWidth(Double.MAX_VALUE);
    }

    private static Button removeButton(Runnable action) {
        Button button = FormLayout.button("Quitar", "btn-secondary", action);
        button.setFocusTraversable(false);
        return button;
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }

    private ServiceOrderService orders() {
        return AppContext.getInstance().getServiceOrderService();
    }
}
