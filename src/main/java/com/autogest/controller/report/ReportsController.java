package com.autogest.controller.report;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigationEvent;
import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PdfExport;
import com.autogest.controller.inventory.InventoryLabels;
import com.autogest.controller.order.OrderLabels;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Catalogs.MovementType;
import com.autogest.model.OrderStatus;
import com.autogest.model.ReportData;
import com.autogest.model.User;
import com.autogest.service.ReportPdfService;
import com.autogest.service.ReportService;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Pantalla «Reportes» (mockup Reportes V1): periodo, indicadores comparados
 * con el periodo anterior, ingresos, estado de órdenes, rankings, inventario,
 * facturación e indicadores del taller. Se puede exportar a PDF.
 *
 * <p>Los cálculos viven en {@link ReportService}; aquí solo se arma la vista.
 * Las consultas van en segundo plano y un resultado viejo se descarta si se
 * eligió otro periodo mientras tanto.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class ReportsController implements Initializable {

    @FXML
    private VBox root;

    private final VBox content = new VBox(16);
    private final Label periodMessage = new Label();
    private final ToggleGroup periods = new ToggleGroup();
    private DatePicker fromPicker;
    private DatePicker toPicker;
    private ReportData.Snapshot snapshot;
    private int loadRequest;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Reportes",
                "Analiza el rendimiento, las operaciones y los resultados de tu taller.");
        header.setBreadcrumbs(List.of("Inicio", "Reportes"));
        header.setPrimaryAction("Exportar PDF", this::exportPdf);
        root.getChildren().addAll(header, buildPeriodBar(), content);
        content.getChildren().setAll(ReportCharts.emptyNote("Cargando el reporte…"));
        Platform.runLater(() -> selectPeriod(ReportService.Period.THIS_MONTH));
    }

    // ---------- periodo ----------

    private Node buildPeriodBar() {
        Map<ReportService.Period, String> options = new LinkedHashMap<>();
        options.put(ReportService.Period.THIS_MONTH, "Este mes");
        options.put(ReportService.Period.TODAY, "Hoy");
        options.put(ReportService.Period.THIS_WEEK, "Esta semana");
        options.put(ReportService.Period.LAST_3_MONTHS, "Últimos 3 meses");
        options.put(ReportService.Period.THIS_YEAR, "Este año");
        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);
        options.forEach((period, text) -> {
            ToggleButton chip = new ToggleButton(text);
            chip.getStyleClass().add("report-period");
            chip.setToggleGroup(periods);
            chip.setUserData(period);
            chip.setOnAction(event -> {
                if (chip.isSelected()) {
                    selectPeriod(period);
                } else {
                    chip.setSelected(true);
                }
            });
            chips.getChildren().add(chip);
        });

        fromPicker = datePicker();
        toPicker = datePicker();
        Button apply = FormLayout.button("Aplicar", "btn-secondary", this::applyCustomPeriod);
        HBox custom = new HBox(8, fieldLabel("Desde"), fromPicker, fieldLabel("Hasta"), toPicker, apply);
        custom.setAlignment(Pos.CENTER_LEFT);

        periodMessage.getStyleClass().addAll("form-message", "form-message-error");
        periodMessage.setWrapText(true);
        showPeriodMessage(null);

        FlowPane bar = new FlowPane(16, 10, chips, custom);
        bar.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(8, bar, periodMessage);
        box.getStyleClass().add("report-period-bar");
        return box;
    }

    private static DatePicker datePicker() {
        DatePicker picker = new DatePicker();
        picker.getStyleClass().add("input");
        picker.setPrefWidth(142);
        return picker;
    }

    private static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return label;
    }

    private void selectPeriod(ReportService.Period period) {
        periods.getToggles().stream().filter(toggle -> toggle.getUserData() == period).findFirst()
                .ifPresent(periods::selectToggle);
        LocalDate today = LocalDate.now();
        fromPicker.setValue(period.start(today));
        toPicker.setValue(today);
        load(period.start(today), today);
    }

    private void applyCustomPeriod() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        periods.selectToggle(null);
        load(from, to);
    }

    private void showPeriodMessage(String message) {
        periodMessage.setText(message == null ? "" : message);
        periodMessage.setVisible(message != null);
        periodMessage.setManaged(message != null);
    }

    // ---------- datos ----------

    private void load(LocalDate from, LocalDate to) {
        int request = ++loadRequest;
        showPeriodMessage(null);
        content.setOpacity(0.55);
        Background.run(() -> AppContext.getInstance().getReportService().build(from, to),
                report -> {
                    if (request == loadRequest) {
                        content.setOpacity(1);
                        render(report);
                    }
                },
                error -> {
                    if (request != loadRequest) {
                        return;
                    }
                    content.setOpacity(1);
                    if (error instanceof ValidationException) {
                        showPeriodMessage(error.getMessage());
                    } else {
                        ErrorDialog.show("No se pudo generar el reporte", error instanceof AutoGestException
                                ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo.");
                    }
                });
    }

    /** Muestra el reporte (visible en el paquete para las pruebas de interfaz). */
    void render(ReportData.Snapshot report) {
        this.snapshot = report;
        content.getChildren().setAll(kpiRow(report),
                columns(new double[]{50, 25, 25}, revenueCard(report), inventoryCard(report), billingCard(report)),
                columns(new double[]{34, 33, 33}, orderStatusCard(report),
                        FormLayout.card("Servicios más realizados",
                                ReportCharts.bars(report.topServices(), String::valueOf)),
                        FormLayout.card("Ingresos por servicio",
                                ReportCharts.bars(report.revenueByService(), Formatter::money))),
                indicatorsCard(report),
                columns(new double[]{34, 33, 33},
                        FormLayout.card("Repuestos más usados", ReportCharts.bars(report.topParts(), String::valueOf)),
                        FormLayout.card("Órdenes por mecánico",
                                ReportCharts.bars(report.ordersByMechanic(), String::valueOf)),
                        movementsCard(report)));
    }

    // ---------- tarjetas ----------

    private static Node kpiRow(ReportData.Snapshot report) {
        ReportData.Kpis now = report.current();
        ReportData.Kpis before = report.previous();
        KpiCard revenue = new KpiCard("Ingresos", Formatter.money(now.revenue()),
                ReportService.changeText(now.revenue(), before.revenue()), IconSet.CREDIT_CARD);
        KpiCard orders = new KpiCard("Órdenes completadas", String.valueOf(now.completedOrders()),
                change(now.completedOrders(), before.completedOrders()), IconSet.FILE_TEXT);
        KpiCard services = new KpiCard("Servicios realizados", String.valueOf(now.servicesPerformed()),
                change(now.servicesPerformed(), before.servicesPerformed()), IconSet.WRENCH);
        KpiCard customers = new KpiCard("Clientes atendidos", String.valueOf(now.customersServed()),
                change(now.customersServed(), before.customersServed()), IconSet.USERS);
        return columns(new double[]{25, 25, 25, 25}, revenue, orders, services, customers);
    }

    private static String change(int current, int previous) {
        return ReportService.changeText(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private static Node revenueCard(ReportData.Snapshot report) {
        return FormLayout.card("Ingresos del periodo", ReportCharts.revenueChart(report.revenue()));
    }

    private Node inventoryCard(ReportData.Snapshot report) {
        ReportData.Inventory inventory = report.inventory();
        VBox card = FormLayout.card("Estado del inventario",
                statRow("En catálogo", String.valueOf(inventory.parts())),
                statRow("Stock bajo", String.valueOf(inventory.lowStock())),
                statRow("Agotados", String.valueOf(inventory.outOfStock())),
                statRow("Valor", Formatter.money(inventory.value())));
        addLink(card, "Ver inventario", View.INVENTORY);
        return card;
    }

    private Node billingCard(ReportData.Snapshot report) {
        ReportData.Billing billing = report.billing();
        VBox card = FormLayout.card("Resumen de facturación",
                statRow("Facturado", Formatter.money(billing.billed())),
                statRow("Pagado", Formatter.money(billing.paid())),
                statRow("Pendiente", Formatter.money(billing.pending())),
                statRow("Vencido", Formatter.money(billing.overdue())));
        addLink(card, "Ver facturación", View.BILLING);
        return card;
    }

    /** Enlace al módulo, solo si el rol puede abrirlo. */
    private static void addLink(VBox card, String text, View view) {
        User user = Session.getCurrentUser();
        if (user == null || !Permissions.canAccess(user.getRole(), view)) {
            return;
        }
        Button link = new Button(text);
        link.getStyleClass().add("breadcrumb-link");
        link.setOnAction(event -> link.fireEvent(NavigationEvent.open(view)));
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        card.getChildren().addAll(spacer, link);
    }

    private static Node orderStatusCard(ReportData.Snapshot report) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            values.put(OrderLabels.statusText(status), report.orderStatus().getOrDefault(status, 0));
        }
        return FormLayout.card("Estado actual de las órdenes", ReportCharts.bars(values, String::valueOf));
    }

    private static Node indicatorsCard(ReportData.Snapshot report) {
        ReportData.Kpis now = report.current();
        return FormLayout.card("Indicadores del taller", columns(new double[]{25, 25, 25, 25},
                tile("Tiempo promedio de reparación", repairDays(report.averageRepairDays())),
                tile("Ticket promedio", Formatter.money(now.averageTicket())),
                tile("Clientes recurrentes", report.recurringRate() + " %"),
                tile("Órdenes canceladas", now.cancelledRate() + " %")));
    }

    /** Días con un decimal, por ejemplo «2,4 días». */
    static String repairDays(Double days) {
        if (days == null) {
            return "Sin datos";
        }
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f días", days);
    }

    private static Node tile(String title, String value) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("billing-tile-title");
        titleLabel.setWrapText(true);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("billing-tile-value");
        VBox tile = new VBox(4, titleLabel, valueLabel);
        tile.getStyleClass().add("billing-tile");
        tile.setMaxWidth(Double.MAX_VALUE);
        return tile;
    }

    private static Node movementsCard(ReportData.Snapshot report) {
        VBox card = FormLayout.card("Inventario y avisos");
        for (MovementType type : MovementType.values()) {
            card.getChildren().add(statRow(InventoryLabels.movementText(type) + "s",
                    report.movements().getOrDefault(type, 0) + " unid."));
        }
        card.getChildren().add(statRow("Avisos entregados", report.deliveryRate() + " %"));
        return card;
    }

    private static Node statRow(String name, String value) {
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("kpi-title");
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("report-stat-value");
        valueLabel.setMinWidth(Region.USE_PREF_SIZE);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, nameLabel, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /** Fila de tarjetas con los anchos dados (en porcentaje) y la misma altura. */
    private static GridPane columns(double[] widths, Node... cards) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        for (int i = 0; i < cards.length; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(widths[i]);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
            if (cards[i] instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
            }
            grid.add(cards[i], i, 0);
        }
        return grid;
    }

    // ---------- exportar ----------

    private void exportPdf() {
        if (snapshot == null) {
            ErrorDialog.show("Exportar reporte", "Espera a que termine de cargar el reporte.");
            return;
        }
        ReportData.Snapshot report = snapshot;
        PdfExport.save(root.getScene() == null ? null : root.getScene().getWindow(), "Guardar reporte en PDF",
                ReportPdfService.fileName(report),
                path -> AppContext.getInstance().getReportPdfService().export(report, path));
    }
}
