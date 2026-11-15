package com.autogest.controller.billing;

import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.StatusBadge;
import com.autogest.controller.customer.CustomerLabels;
import com.autogest.controller.order.OrderLabels;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.OrderHistory;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pantalla «Detalle de factura» (mockup Detalle de factura V1): número y
 * estado, cliente y vehículo, orden relacionada, conceptos, resumen de
 * importes, información de pago e historial de la factura. Desde aquí se
 * registra el pago o se anula.
 *
 * <p>El historial sale de las entradas del historial de la orden que
 * mencionan esta factura (generada, pagada, anulada). «Descargar PDF» y
 * «Enviar al cliente» del mockup quedan para cuando existan la exportación y
 * el {@code NotificationService}.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class InvoiceDetailController implements NavigatorAware, ParameterizedController {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private int invoiceId;

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void receiveParameter(int parameter) {
        this.invoiceId = parameter;
        reload();
    }

    private void reload() {
        Background.run(() -> {
                    BillingUi.service().refreshOverdue();
                    return BillingUi.service().getDetail(invoiceId);
                },
                this::render,
                error -> {
                    ErrorDialog.show("No se pudo abrir la factura", error.getMessage());
                    if (navigator != null) {
                        navigator.navigateTo(View.BILLING);
                    }
                });
    }

    /** Arma la pantalla con la factura cargada (visible en el paquete para las pruebas de interfaz). */
    void render(Invoice invoice) {
        GridPane columns = new GridPane();
        columns.setHgap(16);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(64);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(36);
        columns.getColumnConstraints().addAll(left, right);

        GridPane parties = BillingUi.pair(customerCard(invoice.getOrder()), orderCard(invoice.getOrder()));
        VBox leftColumn = new VBox(16, parties, BillingUi.card("Conceptos facturados", IconSet.FILE_TEXT, null,
                BillingUi.conceptsTable(invoice.getOrder())));
        VBox summary = BillingUi.card("Resumen de factura", IconSet.CREDIT_CARD, null, BillingUi.totals(invoice));
        summary.getStyleClass().add("order-costs-card");
        VBox rightColumn = new VBox(16, summary, paymentCard(invoice), historyCard(invoice));
        columns.add(leftColumn, 0, 0);
        columns.add(rightColumn, 1, 0);

        root.getChildren().setAll(header(invoice), columns);
    }

    // ---------- cabecera ----------

    private Node header(Invoice invoice) {
        HBox breadcrumb = PageHeader.breadcrumbBar(List.of("Facturación", invoice.getNumber()));

        Label title = new Label("Factura " + invoice.getNumber());
        title.getStyleClass().add("page-title");
        Label badge = StatusBadge.of(InvoiceLabels.statusText(invoice.getStatus()),
                InvoiceLabels.statusType(invoice.getStatus()));
        HBox titleRow = new HBox(12, title, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label subtitle = new Label("Emitida el " + Formatter.date(invoice.getIssueDate())
                + (invoice.getDueDate() == null ? "" : " · Vence el " + Formatter.date(invoice.getDueDate())));
        subtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(4, titleRow, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (InvoiceLabels.isOpen(invoice)) {
            Button cancel = new Button("Anular factura");
            cancel.getStyleClass().add("btn-secondary");
            cancel.setOnAction(event -> voidInvoice(invoice));
            Button pay = new Button("Registrar pago");
            pay.getStyleClass().add("btn-primary");
            pay.setOnAction(event -> {
                if (BillingUi.pay(root.getScene() == null ? null : root.getScene().getWindow(), invoice)) {
                    reload();
                }
            });
            actions.getChildren().addAll(cancel, pay);
        }

        HBox row = new HBox(16, titles, spacer, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, breadcrumb, row);
    }

    // ---------- cliente y orden ----------

    private Node customerCard(ServiceOrder order) {
        Vehicle vehicle = order == null ? null : order.getVehicle();
        Customer owner = vehicle == null ? null : vehicle.getOwner();
        VBox body = new VBox(2);
        if (owner == null || owner.getFirstName() == null) {
            body.getChildren().add(BillingUi.muted("Sin cliente"));
        } else {
            body.getChildren().addAll(BillingUi.strong(owner.getFullName()),
                    BillingUi.muted(CustomerLabels.documentText(owner)),
                    BillingUi.muted(CustomerLabels.phoneText(owner.getPhone())));
            if (owner.getEmail() != null && !owner.getEmail().isBlank()) {
                body.getChildren().add(BillingUi.muted(owner.getEmail()));
            }
        }
        VBox card = BillingUi.card("Cliente", IconSet.USER, null, body);
        if (vehicle != null) {
            card.getChildren().add(BillingUi.vehicleBox(InvoiceLabels.vehicle(order),
                    "Placa " + InvoiceLabels.plate(order) + (vehicle.getYear() == 0 ? "" : " · " + vehicle.getYear()),
                    link("Ver vehículo", () -> navigator.navigateWithParameter(View.VEHICLE_DETAIL,
                            vehicle.getId()))));
        }
        return card;
    }

    private Node orderCard(ServiceOrder order) {
        VBox body = new VBox(8);
        if (order == null) {
            body.getChildren().add(BillingUi.muted("Sin orden"));
        } else {
            Label badge = StatusBadge.of(OrderLabels.statusText(order.getStatus()),
                    OrderLabels.statusType(order.getStatus()));
            HBox number = new HBox(10, BillingUi.strong("Orden " + InvoiceLabels.orderNumber(order)), badge);
            number.setAlignment(Pos.CENTER_LEFT);
            body.getChildren().addAll(number, BillingUi.muted("Ingreso " + Formatter.date(order.getEntryDate())
                    + (order.getEntryReason() == null ? "" : " · " + order.getEntryReason())));
            Button open = new Button("Ver orden de servicio");
            open.getStyleClass().add("btn-secondary");
            open.setMaxWidth(Double.MAX_VALUE);
            open.setOnAction(event -> navigator.navigateWithParameter(View.ORDER_DETAIL, order.getId()));
            body.getChildren().add(open);
        }
        return BillingUi.card("Orden de servicio", IconSet.FILE_TEXT, null, body);
    }

    // ---------- pago e historial ----------

    private Node paymentCard(Invoice invoice) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        ColumnConstraints label = new ColumnConstraints();
        label.setMinWidth(Region.USE_PREF_SIZE);
        ColumnConstraints value = new ColumnConstraints();
        value.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(label, value);
        grid.add(BillingUi.muted("Estado"), 0, 0);
        grid.add(StatusBadge.of(InvoiceLabels.statusText(invoice.getStatus()),
                InvoiceLabels.statusType(invoice.getStatus())), 1, 0);
        grid.add(BillingUi.muted("Método"), 0, 1);
        grid.add(BillingUi.strong(InvoiceLabels.methodText(invoice)), 1, 1);
        boolean paid = invoice.getStatus() == InvoiceStatus.PAID;
        grid.add(BillingUi.muted("Valor pagado"), 0, 2);
        grid.add(BillingUi.strong(Formatter.money(paid ? invoice.getTotal() : BigDecimal.ZERO)), 1, 2);
        grid.add(BillingUi.muted("Saldo pendiente"), 0, 3);
        Label balance = BillingUi.strong(Formatter.money(InvoiceLabels.isOpen(invoice)
                ? invoice.getTotal() : BigDecimal.ZERO));
        if (invoice.getStatus() == InvoiceStatus.OVERDUE) {
            balance.getStyleClass().add("billing-overdue");
        }
        grid.add(balance, 1, 3);

        VBox card = BillingUi.card("Información de pago", IconSet.CREDIT_CARD, null, grid);
        String note = switch (invoice.getStatus()) {
            case PENDING -> "Pendiente de pago hasta el " + Formatter.date(invoice.getDueDate()) + ".";
            case OVERDUE -> "El plazo de pago venció el " + Formatter.date(invoice.getDueDate()) + ".";
            case VOIDED -> "Factura anulada: no se cobra.";
            case PAID -> null;
        };
        if (note != null) {
            card.getChildren().add(BillingUi.muted(note));
        }
        return card;
    }

    /** Entradas del historial de la orden que mencionan esta factura, de la más reciente a la más antigua. */
    private Node historyCard(Invoice invoice) {
        VBox list = new VBox(10);
        List<OrderHistory> entries = new ArrayList<>();
        if (invoice.getOrder() != null) {
            for (OrderHistory entry : invoice.getOrder().getHistory()) {
                if (entry.getDescription() != null && entry.getDescription().contains(invoice.getNumber())) {
                    entries.add(entry);
                }
            }
        }
        Collections.reverse(entries);
        if (entries.isEmpty()) {
            list.getChildren().add(BillingUi.muted("Sin movimientos registrados."));
        }
        for (OrderHistory entry : entries) {
            Label text = new Label(entry.getDescription());
            text.setWrapText(true);
            text.getStyleClass().add("order-history-text");
            Label meta = new Label(Formatter.timestamp(entry.getTimestamp())
                    + (entry.getUser() == null || entry.getUser().getFirstName() == null ? ""
                    : " · " + entry.getUser().getFullName()));
            meta.getStyleClass().add("order-history-meta");
            VBox item = new VBox(2, text, meta);
            item.getStyleClass().add("order-history-item");
            list.getChildren().add(item);
        }
        return BillingUi.card("Historial", IconSet.CALENDAR, null, list);
    }

    // ---------- acciones ----------

    private void voidInvoice(Invoice invoice) {
        if (!BillingUi.confirmVoid(invoice)) {
            return;
        }
        root.setDisable(true);
        Background.run(() -> BillingUi.service().voidInvoice(invoice.getId(), Session.getCurrentUser()),
                voided -> {
                    root.setDisable(false);
                    reload();
                },
                error -> {
                    root.setDisable(false);
                    ErrorDialog.show("No se pudo anular la factura", error instanceof AutoGestException
                            ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo.");
                });
    }

    private static Button link(String text, Runnable action) {
        Button link = new Button(text);
        link.getStyleClass().add("breadcrumb-link");
        link.setOnAction(event -> action.run());
        return link;
    }
}
