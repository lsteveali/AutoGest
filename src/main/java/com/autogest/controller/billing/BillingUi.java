package com.autogest.controller.billing;

import com.autogest.config.AppContext;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.FormDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.IconSet;
import com.autogest.exception.ValidationException;
import com.autogest.model.Invoice;
import com.autogest.model.OrderLine;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceOrder;
import com.autogest.service.InvoiceService;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.math.BigDecimal;
import java.util.List;

/**
 * Piezas de interfaz que comparten las pantallas de Facturación: tarjetas con
 * ícono, tabla de conceptos, resumen de importes y los diálogos de pago y
 * anulación. Reutiliza los estilos {@code order-*} del detalle de la orden
 * para que ambas pantallas se vean iguales.
 *
 * <p>Principles: Pure Fabrication, DRY.</p>
 */
final class BillingUi {

    private BillingUi() {
        // Clase de utilidad: no se instancia.
    }

    // ---------- tarjetas y textos ----------

    /** Tarjeta con ícono y título; {@code headerAction} (opcional) va a la derecha. */
    static VBox card(String title, String icon, Node headerAction, Node... body) {
        Node iconNode = IconSet.icon(icon);
        iconNode.getStyleClass().add("order-card-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox titleRow = new HBox(10, iconNode, titleLabel, spacer);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        if (headerAction != null) {
            titleRow.getChildren().add(headerAction);
        }
        VBox card = new VBox(12, titleRow);
        card.getChildren().addAll(body);
        card.getStyleClass().add("card");
        return card;
    }

    /** Dos tarjetas lado a lado con el mismo ancho y la misma altura, sea cual sea su contenido. */
    static GridPane pair(Node left, Node right) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        javafx.scene.layout.RowConstraints row = new javafx.scene.layout.RowConstraints();
        row.setFillHeight(true);
        grid.getRowConstraints().add(row);
        if (left instanceof Region region) {
            region.setMaxHeight(Double.MAX_VALUE);
        }
        if (right instanceof Region region) {
            region.setMaxHeight(Double.MAX_VALUE);
        }
        grid.add(left, 0, 0);
        grid.add(right, 1, 0);
        return grid;
    }

    /** Vehículo en un recuadro gris con ícono; {@code action} (opcional) va debajo del texto. */
    static Node vehicleBox(String name, String detail, Node action) {
        Node icon = IconSet.icon(IconSet.CAR);
        icon.getStyleClass().add("order-card-icon");
        VBox text = new VBox(2, strong(name), muted(detail));
        if (action != null) {
            text.getChildren().add(action);
        }
        HBox box = new HBox(12, icon, text);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("billing-vehicle-box");
        return box;
    }

    static Label strong(String text) {
        Label label = new Label(text == null ? "-" : text);
        label.getStyleClass().add("order-strong");
        label.setWrapText(true);
        return label;
    }

    static Label muted(String text) {
        Label label = new Label(text == null ? "-" : text);
        label.getStyleClass().add("order-muted");
        label.setWrapText(true);
        return label;
    }

    // ---------- conceptos ----------

    /** Conceptos (líneas de la orden): tipo, descripción, cantidad, valor unitario y subtotal. */
    static Node conceptsTable(ServiceOrder order) {
        List<OrderLine> lines = order == null ? List.of() : order.getLines();
        if (lines.isEmpty()) {
            return muted("La orden no tiene servicios ni repuestos.");
        }
        GridPane grid = new GridPane();
        grid.getStyleClass().add("order-lines");
        ColumnConstraints type = new ColumnConstraints();
        type.setMinWidth(90);
        ColumnConstraints name = new ColumnConstraints();
        name.setHgrow(Priority.ALWAYS);
        name.setMinWidth(140);
        grid.getColumnConstraints().addAll(type, name);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints number = new ColumnConstraints();
            number.setHalignment(HPos.RIGHT);
            number.setMinWidth(i == 0 ? 80 : 115);
            grid.getColumnConstraints().add(number);
        }
        String[] headers = {"Tipo", "Descripción", "Cantidad", "Valor unitario", "Subtotal"};
        for (int c = 0; c < headers.length; c++) {
            Label header = new Label(headers[c]);
            header.getStyleClass().add("order-lines-header");
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(c >= 2 ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            grid.add(header, c, 0);
        }
        int row = 1;
        for (OrderLine line : lines) {
            grid.add(cell(line instanceof PartLine ? "Repuesto" : "Servicio", false), 0, row);
            grid.add(cell(line.getDescription(), false), 1, row);
            grid.add(cell(String.valueOf(line.getQuantity()), true), 2, row);
            grid.add(cell(Formatter.money(line.getUnitPrice()), true), 3, row);
            Label total = cell(Formatter.money(line.calculateTotal()), true);
            total.getStyleClass().add("order-lines-total");
            grid.add(total, 4, row);
            row++;
        }
        return grid;
    }

    private static Label cell(String text, boolean number) {
        Label label = new Label(text);
        label.getStyleClass().add("order-lines-cell");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setAlignment(number ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        if (number) {
            label.getStyleClass().add("order-lines-number");
        }
        return label;
    }

    // ---------- importes ----------

    /** Subtotal, descuento, IVA y total de la factura. */
    static GridPane totals(Invoice invoice) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("order-costs");
        ColumnConstraints label = new ColumnConstraints();
        label.setHgrow(Priority.ALWAYS);
        ColumnConstraints value = new ColumnConstraints();
        value.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(label, value);
        addAmount(grid, 0, "Subtotal", Formatter.money(invoice.getSubtotal()), false);
        addAmount(grid, 1, "Descuento", invoice.getDiscount().signum() == 0 ? Formatter.money(BigDecimal.ZERO)
                : "-" + Formatter.money(invoice.getDiscount()), false);
        addAmount(grid, 2, "IVA (" + InvoiceService.taxRateText(invoice) + ")", Formatter.money(invoice.getTaxes()), false);
        Region divider = new Region();
        divider.getStyleClass().add("order-costs-divider");
        grid.add(divider, 0, 3, 2, 1);
        addAmount(grid, 4, "Total", Formatter.money(invoice.getTotal()), true);
        return grid;
    }

    private static void addAmount(GridPane grid, int row, String name, String amount, boolean total) {
        Label label = new Label(name);
        Label value = new Label(amount);
        label.getStyleClass().add(total ? "order-cost-total-label" : "order-cost-label");
        value.getStyleClass().add(total ? "order-cost-total" : "order-cost-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }


    /**
     * Lee un valor en pesos escrito con o sin «$» y puntos de miles
     * («10.000», «$10.000», «10000»). Vacío = 0.
     */
    static BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        String clean = text.trim().replace("$", "").replace(" ", "").replace(".", "").replace(",", ".");
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            throw new ValidationException("El descuento debe ser un valor en pesos, por ejemplo 10.000");
        }
    }

    // ---------- diálogos ----------

    /**
     * Pide el método de pago y registra el pago de la factura.
     *
     * @return {@code true} si se registró
     */
    static boolean pay(Window owner, Invoice invoice) {
        ComboBox<String> method = FormLayout.combo("Selecciona el método");
        method.getItems().setAll(InvoiceLabels.methodOptions(service().paymentMethods()));
        VBox content = new VBox(12, FormLayout.labeled("Método de pago *", method));
        FormDialog dialog = new FormDialog("Registrar pago", "Factura " + invoice.getNumber() + " por "
                + Formatter.money(invoice.getTotal()) + ". Quedará como pagada.", content, "Registrar pago",
                () -> service().markAsPaid(invoice.getId(), InvoiceLabels.methodFromText(method.getValue()),
                        Session.getCurrentUser()));
        if (owner != null) {
            dialog.initOwner(owner);
        }
        return dialog.open();
    }

    /**
     * Pide dónde guardar el PDF de la factura, lo genera en segundo plano y
     * ofrece abrirlo. {@code loader} trae la factura completa (con su orden);
     * se ejecuta fuera del hilo de la interfaz.
     *
     * @param owner   ventana dueña de los diálogos
     * @param invoice factura (para el nombre del archivo)
     * @param loader  carga la factura completa
     */
    static void downloadPdf(Window owner, Invoice invoice, java.util.function.Supplier<Invoice> loader) {
        com.autogest.controller.components.PdfExport.save(owner, "Guardar factura en PDF",
                com.autogest.service.InvoicePdfService.fileName(invoice),
                path -> AppContext.getInstance().getInvoicePdfService().export(loader.get(), path));
    }

    /**
     * Igual que {@link #downloadPdf}, pero guarda la factura como imagen PNG
     * (cómoda para enviarla por chat o verla en el celular).
     *
     * @param owner   ventana dueña de los diálogos
     * @param invoice factura (para el nombre del archivo)
     * @param loader  carga la factura completa
     */
    static void downloadPng(Window owner, Invoice invoice, java.util.function.Supplier<Invoice> loader) {
        com.autogest.controller.components.PdfExport.savePng(owner, "Guardar factura como imagen",
                com.autogest.service.InvoicePdfService.pngFileName(invoice),
                path -> AppContext.getInstance().getInvoicePdfService().exportPng(loader.get(), path));
    }

    /** Pide confirmación para anular la factura. */
    static boolean confirmVoid(Invoice invoice) {
        return ConfirmDialog.confirm("Anular factura", "La factura " + invoice.getNumber()
                + " quedará anulada y no se podrá cobrar. La orden podrá facturarse de nuevo. ¿Anular la factura?");
    }

    static InvoiceService service() {
        return AppContext.getInstance().getInvoiceService();
    }
}
