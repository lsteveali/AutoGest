package com.autogest.controller.billing;

import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.StatusBadge;
import com.autogest.controller.customer.CustomerLabels;
import com.autogest.controller.order.OrderLabels;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Pantalla «Nueva factura» (mockup Nueva factura V1): se elige una orden Lista
 * o Entregada sin factura y sus servicios y repuestos pasan a ser los
 * conceptos. Se puede aplicar un descuento, cambiar el vencimiento y registrar
 * el pago en el mismo momento. El resumen se recalcula mientras se escribe.
 *
 * <p>Si llega un id de orden (desde «Generar factura» en el detalle de la
 * orden), esa orden queda elegida. Los conceptos no se editan aquí: son los de
 * la orden.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class InvoiceNewController implements Initializable, NavigatorAware, ParameterizedController {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private int requestedOrderId;

    private ComboBox<ServiceOrder> orderBox;
    private final VBox orderInfo = new VBox(8);
    private final VBox partyInfo = new VBox(8);
    private final VBox concepts = new VBox();
    private final VBox totals = new VBox();
    private TextField discountField;
    private final Label discountError = new Label();
    private final javafx.beans.property.BooleanProperty discountInvalid =
            new javafx.beans.property.SimpleBooleanProperty(false);
    private DatePicker dueDate;
    private CheckBox payNow;
    private ComboBox<String> methodBox;
    private Label formError;
    private Button saveButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Nueva factura",
                "Genera una factura a partir de los servicios y repuestos de una orden de servicio.");
        header.setBreadcrumbs(List.of("Facturación", "Nueva factura"));

        GridPane columns = new GridPane();
        columns.setHgap(16);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(64);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(36);
        columns.getColumnConstraints().addAll(left, right);

        GridPane top = BillingUi.pair(orderCard(), partyCard());
        VBox conceptsCard = BillingUi.card("Conceptos de la factura", IconSet.FILE_TEXT, null, concepts,
                BillingUi.muted("Los conceptos son los servicios y repuestos de la orden. Para cambiarlos, "
                        + "edita la orden antes de facturarla."));
        VBox leftColumn = new VBox(16, top, conceptsCard);
        VBox rightColumn = new VBox(16, summaryCard(), paymentCard());
        columns.add(leftColumn, 0, 0);
        columns.add(rightColumn, 1, 0);

        root.getChildren().addAll(header, columns, actions());
        showOrder(null);
        Platform.runLater(this::loadOrders);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void receiveParameter(int parameter) {
        this.requestedOrderId = parameter;
        selectRequestedOrder();
    }

    // ---------- tarjetas ----------

    private Node orderCard() {
        orderBox = FormLayout.combo("Elige una orden", order ->
                OrderLabels.number(order) + " · " + OrderLabels.customer(order) + " · " + OrderLabels.plate(order));
        // En el campo cerrado basta número y cliente (la placa ya se ve en «Cliente y vehículo»).
        orderBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(ServiceOrder order, boolean empty) {
                super.updateItem(order, empty);
                setText(empty || order == null ? orderBox.getPromptText()
                        : OrderLabels.number(order) + " · " + OrderLabels.customer(order));
            }
        });
        orderBox.valueProperty().addListener((obs, old, order) -> showOrder(order));
        return BillingUi.card("Orden de servicio", IconSet.FILE_TEXT, null,
                FormLayout.labeled("Orden de servicio *", orderBox), orderInfo);
    }

    private Node partyCard() {
        return BillingUi.card("Cliente y vehículo", IconSet.USER, null, partyInfo);
    }

    private Node summaryCard() {
        discountField = FormLayout.textField("$0");
        discountField.textProperty().addListener((obs, old, text) -> refreshTotals());
        discountError.getStyleClass().addAll("form-message", "form-message-error");
        discountError.setWrapText(true);
        discountError.setVisible(false);
        discountError.setManaged(false);
        VBox discount = FormLayout.labeled("Descuento", discountField);
        discount.getChildren().add(discountError);
        VBox card = BillingUi.card("Resumen", IconSet.CREDIT_CARD, null, discount, totals);
        card.getStyleClass().add("order-costs-card");
        return card;
    }

    private Node paymentCard() {
        dueDate = new DatePicker(LocalDate.now().plusDays(BillingUi.service().settings().getDueDays()));
        dueDate.getStyleClass().add("input");
        dueDate.setMaxWidth(Double.MAX_VALUE);
        payNow = new CheckBox("Registrar el pago ahora");
        methodBox = FormLayout.combo("Selecciona el método");
        methodBox.getItems().setAll(InvoiceLabels.methodOptions(BillingUi.service().paymentMethods()));
        VBox methodField = FormLayout.labeled("Método de pago *", methodBox);
        methodField.visibleProperty().bind(payNow.selectedProperty());
        methodField.managedProperty().bind(payNow.selectedProperty());
        payNow.selectedProperty().addListener((obs, old, paid) ->
                saveButton.setText(paid ? "Guardar y registrar pago" : "Guardar factura"));
        return BillingUi.card("Información de pago", IconSet.CALENDAR, null,
                FormLayout.labeled("Fecha de vencimiento", dueDate), payNow, methodField,
                BillingUi.muted("Si no registras el pago, la factura queda Pendiente y pasa a Vencida "
                        + "cuando se cumple el plazo."));
    }

    private Node actions() {
        formError = new Label();
        formError.getStyleClass().addAll("form-message", "form-message-error");
        formError.setWrapText(true);
        formError.setVisible(false);
        formError.setManaged(false);
        Button cancel = FormLayout.button("Cancelar", "btn-secondary", () -> {
            if (navigator != null) {
                navigator.goBack();
            }
        });
        saveButton = FormLayout.button("Guardar factura", "btn-primary", this::save);
        // Sin orden elegida o con un descuento inválido no hay nada que guardar.
        saveButton.disableProperty().bind(orderBox.valueProperty().isNull().or(discountInvalid));
        HBox buttons = FormLayout.actions(cancel, saveButton);
        VBox box = new VBox(10, formError, buttons);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // ---------- orden elegida ----------

    private void loadOrders() {
        Background.run(() -> BillingUi.service().findBillableOrders(),
                orders -> {
                    orderBox.getItems().setAll(orders);
                    if (orders.isEmpty()) {
                        orderBox.setPromptText("No hay órdenes por facturar");
                        orderInfo.getChildren().setAll(BillingUi.muted("Solo se facturan órdenes Listas o "
                                + "Entregadas que aún no tienen factura."));
                    }
                    selectRequestedOrder();
                },
                error -> ErrorDialog.show("No se pudieron cargar las órdenes", error instanceof AutoGestException
                        ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo."));
    }

    private void selectRequestedOrder() {
        if (requestedOrderId <= 0 || orderBox == null || orderBox.getItems().isEmpty()) {
            return;
        }
        orderBox.getItems().stream().filter(order -> order.getId() == requestedOrderId).findFirst()
                .ifPresent(orderBox::setValue);
    }

    /** Muestra la orden elegida (visible en el paquete para las pruebas de interfaz). */
    void showOrder(ServiceOrder order) {
        if (orderBox != null && order != null && orderBox.getValue() != order) {
            if (!orderBox.getItems().contains(order)) {
                orderBox.getItems().add(order);
            }
            orderBox.setValue(order);
            return;
        }
        orderInfo.getChildren().clear();
        partyInfo.getChildren().clear();
        concepts.getChildren().clear();
        if (order == null) {
            orderInfo.getChildren().add(BillingUi.muted("Los servicios y repuestos de la orden se cargan "
                    + "como conceptos de la factura."));
            partyInfo.getChildren().add(BillingUi.muted("Elige una orden para ver su cliente y vehículo."));
            concepts.getChildren().add(BillingUi.muted("Sin conceptos todavía."));
        } else {
            Label badge = StatusBadge.of(OrderLabels.statusText(order.getStatus()),
                    OrderLabels.statusType(order.getStatus()));
            HBox status = new HBox(8, BillingUi.muted("Estado"), badge);
            status.setAlignment(Pos.CENTER_LEFT);
            orderInfo.getChildren().addAll(status, BillingUi.muted("Ingreso " + Formatter.date(order.getEntryDate())
                    + " · " + order.getEntryReason()));
            partyInfo.getChildren().addAll(customerBlock(order), vehicleBlock(order));
            concepts.getChildren().add(BillingUi.conceptsTable(order));
        }
        refreshTotals();
    }

    private static Node customerBlock(ServiceOrder order) {
        Customer owner = order.getVehicle() == null ? null : order.getVehicle().getOwner();
        if (owner == null || owner.getFirstName() == null) {
            return BillingUi.muted("Sin cliente");
        }
        return new VBox(2, BillingUi.strong(owner.getFullName()), BillingUi.muted(CustomerLabels.documentText(owner)),
                BillingUi.muted(CustomerLabels.phoneText(owner.getPhone())));
    }

    private static Node vehicleBlock(ServiceOrder order) {
        Vehicle vehicle = order.getVehicle();
        return BillingUi.vehicleBox(InvoiceLabels.vehicle(order), "Placa " + InvoiceLabels.plate(order)
                + (vehicle == null || vehicle.getYear() == 0 ? "" : " · " + vehicle.getYear()), null);
    }

    /** Recalcula el resumen con la orden y el descuento actuales (sin consultar la base de datos). */
    private void refreshTotals() {
        totals.getChildren().clear();
        ServiceOrder order = orderBox == null ? null : orderBox.getValue();
        if (order == null) {
            Invoice empty = new Invoice();
            totals.getChildren().add(BillingUi.totals(empty));
            return;
        }
        try {
            Invoice draft = BillingUi.service().draft(order, BillingUi.parseMoney(discountField.getText()), null);
            totals.getChildren().add(BillingUi.totals(draft));
            setDiscountError(null);
        } catch (AutoGestException e) {
            Invoice withoutDiscount = BillingUi.service().draft(order, BigDecimal.ZERO, null);
            totals.getChildren().add(BillingUi.totals(withoutDiscount));
            setDiscountError(e.getMessage());
        }
    }

    /** Marca el campo de descuento y muestra el motivo debajo ({@code null} lo limpia). */
    private void setDiscountError(String message) {
        boolean invalid = message != null;
        discountError.setText(invalid ? message : "");
        discountError.setVisible(invalid);
        discountError.setManaged(invalid);
        discountField.getStyleClass().remove("input-invalid");
        if (invalid) {
            discountField.getStyleClass().add("input-invalid");
        }
        discountInvalid.set(invalid);
    }

    // ---------- guardar ----------

    private void save() {
        ServiceOrder order = orderBox.getValue();
        if (order == null) {
            showError("Selecciona la orden de servicio");
            return;
        }
        BigDecimal discount;
        try {
            discount = BillingUi.parseMoney(discountField.getText());
        } catch (AutoGestException e) {
            showError(e.getMessage());
            return;
        }
        String method = null;
        if (payNow.isSelected()) {
            method = InvoiceLabels.methodFromText(methodBox.getValue());
            if (method == null) {
                showError("Selecciona el método de pago o desmarca «Registrar el pago ahora»");
                return;
            }
        }
        LocalDate due = dueDate.getValue();
        String paidWith = method;
        hideError();
        root.setDisable(true);
        Background.run(() -> BillingUi.service().createInvoice(order.getId(), discount, due, paidWith,
                        Session.getCurrentUser()),
                invoice -> {
                    root.setDisable(false);
                    if (navigator != null) {
                        navigator.navigateWithParameter(View.INVOICE_DETAIL, invoice.getId());
                    }
                },
                error -> {
                    root.setDisable(false);
                    showError(error instanceof AutoGestException ? error.getMessage()
                            : "No se pudo guardar la factura. Revisa la conexión e inténtalo de nuevo.");
                });
    }

    private void showError(String message) {
        formError.setText(message);
        formError.setVisible(true);
        formError.setManaged(true);
    }

    private void hideError() {
        formError.setVisible(false);
        formError.setManaged(false);
    }
}
