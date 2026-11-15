package com.autogest.controller.billing;

import com.autogest.controller.NavigatorAware;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FilterBar;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.InvoiceSummary;
import com.autogest.service.InvoiceService;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Pantalla «Facturación» (mockup Facturación V1): indicadores por estado,
 * filtros por texto, estado, fecha y método de pago, tabla de facturas con
 * sus acciones y el resumen de lo facturado, pagado y por cobrar.
 *
 * <p>Al cargar, primero pasa a vencidas las facturas pendientes cuyo plazo ya
 * pasó ({@link InvoiceService#refreshOverdue()}). Todo va en segundo plano.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class BillingController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 10;

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private KpiCard totalCard;
    private KpiCard paidCard;
    private KpiCard pendingCard;
    private KpiCard overdueCard;
    private Label billedAmount;
    private Label paidAmount;
    private Label pendingAmount;
    private PagedTable<Invoice> table;

    private String searchText = "";
    private InvoiceStatus statusFilter;
    private String periodFilter;
    private String methodFilter;
    private int reloadRequest;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Facturación",
                "Administra las facturas generadas a partir de las órdenes de servicio del taller.");
        header.setBreadcrumbs(List.of("Inicio", "Facturación"));
        header.setPrimaryAction("+ Nueva factura", () -> {
            if (navigator != null) {
                navigator.navigateTo(View.INVOICE_NEW);
            }
        });
        table = buildTable();
        VBox list = BillingUi.card("Facturas", IconSet.FILE_TEXT, null, buildFilters(), table);
        root.getChildren().addAll(header, buildKpis(), list, buildSummary());
        Platform.runLater(this::reload);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    // ---------- armado ----------

    private HBox buildKpis() {
        totalCard = new KpiCard("Facturas generadas", "-", "Incluye las anuladas", IconSet.FILE_TEXT);
        paidCard = new KpiCard("Pagadas", "-", "Cobro completado", IconSet.CHECK);
        pendingCard = new KpiCard("Pendientes", "-", "Dentro del plazo de pago", IconSet.CALENDAR);
        overdueCard = new KpiCard("Vencidas", "-", "Plazo de pago cumplido", IconSet.INFO);
        paidCard.getStyleClass().add("kpi-success");
        overdueCard.getStyleClass().add("kpi-danger");
        HBox row = new HBox(16);
        for (KpiCard card : List.of(totalCard, paidCard, pendingCard, overdueCard)) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.getStyleClass().add("filter-bar-flat");
        bar.addSearch("Buscar por número, cliente o placa...", text -> {
            searchText = text;
            reload();
        });
        bar.addCombo("Estado", InvoiceLabels.statusOptions(), value -> {
            statusFilter = InvoiceLabels.statusFromText(value);
            reload();
        });
        bar.addCombo("Fecha", InvoiceLabels.PERIODS, value -> {
            periodFilter = value;
            reload();
        });
        List<String> methods = new java.util.ArrayList<>();
        methods.add(InvoiceLabels.ALL);
        methods.addAll(InvoiceLabels.methodOptions());
        bar.addCombo("Método", methods, value -> {
            methodFilter = InvoiceLabels.methodFromText(value);
            reload();
        });
        return bar;
    }

    private PagedTable<Invoice> buildTable() {
        PagedTable<Invoice> pagedTable = new PagedTable<>(PAGE_SIZE);
        // Anchos mínimos para que no se corten el número, el cliente ni el estado; el
        // vehículo lleva la placa debajo y el menú «…» no lleva título, para caber en 1280 px.
        pagedTable.addColumn("Número", Invoice::getNumber, 118);
        pagedTable.addColumn("Cliente", InvoiceLabels::customer, 125);
        pagedTable.addColumn("Vehículo", this::vehicleCell, 120);
        pagedTable.addColumn("Emisión", invoice -> Formatter.date(invoice.getIssueDate()), 106);
        pagedTable.addColumn("Vence", invoice -> invoice.getDueDate() == null ? "-"
                : Formatter.date(invoice.getDueDate()), 106);
        pagedTable.addColumn("Total", invoice -> Formatter.money(invoice.getTotal()), 100);
        pagedTable.addColumn("Método", InvoiceLabels::methodText, 105);
        pagedTable.addColumn("Estado", invoice -> {
            Label badge = StatusBadge.of(InvoiceLabels.statusText(invoice.getStatus()),
                    InvoiceLabels.statusType(invoice.getStatus()));
            badge.setMinWidth(Region.USE_PREF_SIZE);
            return badge;
        }, 100);
        pagedTable.addColumn("", this::actionsCell, 50);
        pagedTable.setOnSelect(invoice -> openDetail(invoice.getId()));
        // La página tiene más contenido debajo: la tabla toma el alto de sus filas (vehículo y placa).
        pagedTable.fitHeightToRows(62);
        return pagedTable;
    }

    private Node vehicleCell(Invoice invoice) {
        Label vehicle = new Label(InvoiceLabels.vehicle(invoice.getOrder()));
        vehicle.getStyleClass().add("order-cell-vehicle");
        Label plate = new Label(InvoiceLabels.plate(invoice.getOrder()));
        plate.getStyleClass().add("order-cell-plate");
        return new VBox(2, vehicle, plate);
    }

    private Node actionsCell(Invoice invoice) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("icon-button");
        menu.setFocusTraversable(false);
        MenuItem detail = new MenuItem("Ver factura");
        detail.setOnAction(event -> openDetail(invoice.getId()));
        MenuItem pdf = new MenuItem("Descargar PDF");
        // La fila trae la orden resumida: el PDF necesita el detalle completo (líneas y cliente).
        pdf.setOnAction(event -> BillingUi.downloadPdf(window(), invoice,
                () -> BillingUi.service().getDetail(invoice.getId())));
        menu.getItems().addAll(detail, pdf);
        if (InvoiceLabels.isOpen(invoice)) {
            MenuItem pay = new MenuItem("Registrar pago");
            pay.setOnAction(event -> {
                if (BillingUi.pay(window(), invoice)) {
                    reload();
                }
            });
            MenuItem voidItem = new MenuItem("Anular");
            voidItem.setOnAction(event -> voidInvoice(invoice));
            menu.getItems().addAll(pay, voidItem);
        }
        return menu;
    }

    /** Resumen de dinero: facturado, pagado y por cobrar. */
    private Node buildSummary() {
        billedAmount = new Label("-");
        paidAmount = new Label("-");
        pendingAmount = new Label("-");
        HBox tiles = new HBox(12, amountTile("Valor facturado", billedAmount, "Sin las anuladas"),
                amountTile("Total pagado", paidAmount, "Facturas pagadas"),
                amountTile("Por cobrar", pendingAmount, "Pendientes y vencidas"));
        tiles.getChildren().forEach(tile -> HBox.setHgrow(tile, Priority.ALWAYS));
        return BillingUi.card("Resumen de facturación", IconSet.CREDIT_CARD, null, tiles);
    }

    private static VBox amountTile(String title, Label value, String detail) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("billing-tile-title");
        value.getStyleClass().add("billing-tile-value");
        Label detailLabel = new Label(detail);
        detailLabel.getStyleClass().add("billing-tile-detail");
        VBox tile = new VBox(2, titleLabel, value, detailLabel);
        tile.getStyleClass().add("billing-tile");
        tile.setMaxWidth(Double.MAX_VALUE);
        return tile;
    }

    // ---------- datos ----------

    private record ScreenData(List<Invoice> invoices, InvoiceSummary summary) {
    }

    private void reload() {
        int request = ++reloadRequest;
        String text = searchText;
        InvoiceStatus status = statusFilter;
        LocalDate from = InvoiceLabels.periodStart(periodFilter, LocalDate.now());
        String method = methodFilter;
        Background.run(() -> {
            InvoiceService service = BillingUi.service();
            service.refreshOverdue();
            return new ScreenData(service.searchInvoices(text, status, from, method), service.getSummary());
        }, data -> {
            if (request == reloadRequest) {
                show(data.invoices(), data.summary());
            }
        }, error -> {
            if (request == reloadRequest) {
                table.setData(List.of());
                ErrorDialog.show("No se pudieron cargar las facturas", error instanceof AutoGestException
                        ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo.");
            }
        });
    }

    /** Muestra las facturas y los indicadores (visible en el paquete para las pruebas de interfaz). */
    void show(List<Invoice> invoices, InvoiceSummary summary) {
        table.setData(invoices);
        totalCard.setValue(String.valueOf(summary.total()));
        paidCard.setValue(String.valueOf(summary.paid()));
        pendingCard.setValue(String.valueOf(summary.pending()));
        overdueCard.setValue(String.valueOf(summary.overdue()));
        billedAmount.setText(Formatter.money(orZero(summary.billedAmount())));
        paidAmount.setText(Formatter.money(orZero(summary.paidAmount())));
        pendingAmount.setText(Formatter.money(orZero(summary.pendingAmount())));
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    // ---------- acciones ----------

    private void voidInvoice(Invoice invoice) {
        if (!BillingUi.confirmVoid(invoice)) {
            return;
        }
        Background.run(() -> BillingUi.service().voidInvoice(invoice.getId(), Session.getCurrentUser()),
                voided -> reload(),
                error -> ErrorDialog.show("No se pudo anular la factura", error instanceof AutoGestException
                        ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo."));
    }

    private void openDetail(int id) {
        if (navigator != null) {
            navigator.navigateWithParameter(View.INVOICE_DETAIL, id);
        }
    }

    private javafx.stage.Window window() {
        return root.getScene() == null ? null : root.getScene().getWindow();
    }
}
