package com.autogest.controller.order;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormDialog;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.StatusBadge;
import com.autogest.controller.customer.CustomerLabels;
import com.autogest.controller.vehicle.VehicleLabels;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Customer;
import com.autogest.model.Diagnosis;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.OrderHistory;
import com.autogest.model.OrderLine;
import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Vehicle;
import com.autogest.service.InvoiceService;
import com.autogest.service.OrderWorkflowService;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import com.autogest.util.Session;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Detalle de la orden de servicio (mockup «Detalle orden», Sprint 4, parte del
 * Líder): estado con sus pasos, avance y cancelación, cliente y vehículo,
 * diagnóstico, servicios y repuestos (con su estado en el inventario),
 * resumen de costos, mecánico responsable e historial con notas.
 *
 * <p>Las reglas viven en {@link OrderWorkflowService}; aquí solo se arman la
 * pantalla y los diálogos. Las acciones van en segundo plano y, al terminar,
 * la pantalla se vuelve a cargar desde la base de datos. «Generar factura»
 * (Sprint 5) aparece en las órdenes Listas o Entregadas sin factura vigente; si
 * ya tienen una, se muestra «Ver factura» y la orden no se puede cancelar.
 * «Comunicación» del mockup llega con el sprint de Notificaciones.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class OrderDetailController implements NavigatorAware, ParameterizedController {

    /** Pasos del flujo normal, en orden (D-04). */
    private static final List<OrderStatus> STEPS = List.of(OrderStatus.RECEIVED, OrderStatus.DIAGNOSIS,
            OrderStatus.IN_REPAIR, OrderStatus.READY, OrderStatus.DELIVERED);

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private int orderId;
    /** Factura de la orden (Sprint 5), o {@code null} si no tiene o no aplica. */
    private Invoice invoice;

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void receiveParameter(int parameter) {
        this.orderId = parameter;
        reload();
    }

    /** Orden y su factura (o {@code null}), leídas juntas en segundo plano. */
    private record Loaded(ServiceOrder order, Invoice invoice) {
    }

    private void reload() {
        Background.run(() -> {
                    AppContext context = AppContext.getInstance();
                    ServiceOrder order = context.getServiceOrderService().getDetail(orderId);
                    Invoice invoice = InvoiceService.canInvoice(order.getStatus())
                            ? context.getInvoiceService().findByOrder(orderId) : null;
                    return new Loaded(order, invoice);
                },
                loaded -> render(loaded.order(), loaded.invoice()),
                error -> {
                    ErrorDialog.show("No se pudo abrir la orden", error.getMessage());
                    if (navigator != null) {
                        navigator.navigateTo(View.ORDERS);
                    }
                });
    }

    /** Arma la pantalla con la orden cargada y sin factura (visible en el paquete para las pruebas de interfaz). */
    void render(ServiceOrder order) {
        render(order, null);
    }

    /**
     * Arma la pantalla con la orden cargada y su factura.
     *
     * @param order   orden completa
     * @param invoice factura de la orden, o {@code null} si no tiene
     */
    void render(ServiceOrder order, Invoice invoice) {
        this.invoice = invoice;
        GridPane columns = new GridPane();
        columns.setHgap(16);
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(62);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(38);
        columns.getColumnConstraints().addAll(left, right);

        HBox parties = new HBox(16, customerCard(order), vehicleCard(order));
        parties.getChildren().forEach(card -> HBox.setHgrow(card, Priority.ALWAYS));
        VBox leftColumn = new VBox(16, parties, diagnosisCard(order), servicesCard(order), partsCard(order));
        VBox rightColumn = new VBox(16, costsCard(order), responsibleCard(order), historyCard(order));
        columns.add(leftColumn, 0, 0);
        columns.add(rightColumn, 1, 0);

        root.getChildren().setAll(header(order), stepper(order), columns);
    }

    // ---------- cabecera y estado ----------

    private Node header(ServiceOrder order) {
        HBox breadcrumb = PageHeader.breadcrumbBar(List.of("Órdenes de servicio", "Orden #" + order.getNumber()));

        Label title = new Label("Orden #" + order.getNumber());
        title.getStyleClass().add("page-title");
        Label badge = StatusBadge.of(OrderLabels.statusText(order.getStatus()),
                OrderLabels.statusType(order.getStatus()));
        HBox titleRow = new HBox(12, title, badge);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label subtitle = new Label("Ingreso " + Formatter.date(order.getEntryDate())
                + (order.getEstimatedDeliveryDate() == null ? ""
                : " · Entrega estimada " + Formatter.date(order.getEstimatedDeliveryDate()))
                + " · " + order.getEntryReason());
        subtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(4, titleRow, subtitle);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        boolean invoiced = invoice != null && invoice.getStatus() != InvoiceStatus.VOIDED;
        // Con una factura vigente la orden ya no se cancela: primero hay que anular la factura.
        if (order.canChangeTo(OrderStatus.CANCELLED) && !invoiced) {
            Button cancel = new Button("Cancelar orden");
            cancel.getStyleClass().add("btn-secondary");
            cancel.setOnAction(event -> cancelOrder(order));
            actions.getChildren().add(cancel);
        }
        if (invoiced) {
            Button view = new Button("Ver factura " + invoice.getNumber());
            view.getStyleClass().add("btn-secondary");
            view.setOnAction(event -> navigator.navigateWithParameter(View.INVOICE_DETAIL, invoice.getId()));
            actions.getChildren().add(view);
        } else if (InvoiceService.canInvoice(order.getStatus()) && !order.getLines().isEmpty()) {
            Button generate = new Button("Generar factura");
            OrderStatus nextStep = OrderWorkflowService.nextStatus(order.getStatus());
            // Entregada: es la acción principal; Lista: lo es «Entregar al cliente».
            generate.getStyleClass().add(nextStep == null ? "btn-primary" : "btn-secondary");
            generate.setOnAction(event -> navigator.navigateWithParameter(View.INVOICE_NEW, order.getId()));
            actions.getChildren().add(generate);
        }
        OrderStatus next = OrderWorkflowService.nextStatus(order.getStatus());
        if (next != null) {
            Button advance = new Button(advanceText(next));
            advance.getStyleClass().add("btn-primary");
            advance.setOnAction(event -> advance(order, next));
            actions.getChildren().add(advance);
        }

        HBox row = new HBox(16, titles, spacer, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, breadcrumb, row);
    }

    private static String advanceText(OrderStatus next) {
        return switch (next) {
            case DIAGNOSIS -> "Pasar a diagnóstico";
            case IN_REPAIR -> "Pasar a reparación";
            case READY -> "Marcar como lista";
            case DELIVERED -> "Entregar al cliente";
            default -> "Avanzar";
        };
    }

    /** Pasos Recibido → … → Entregado; los cumplidos y el actual se destacan. */
    private Node stepper(ServiceOrder order) {
        HBox steps = new HBox(8);
        steps.getStyleClass().add("order-stepper");
        steps.setAlignment(Pos.CENTER_LEFT);
        boolean cancelled = order.getStatus() == OrderStatus.CANCELLED;
        int current = STEPS.indexOf(order.getStatus());
        for (int i = 0; i < STEPS.size(); i++) {
            if (i > 0) {
                Node arrow = IconSet.icon(IconSet.ARROW_RIGHT);
                arrow.getStyleClass().add("order-step-arrow");
                steps.getChildren().add(arrow);
            }
            Label step = new Label(OrderLabels.statusText(STEPS.get(i)));
            step.getStyleClass().add("order-step");
            if (!cancelled && i < current) {
                step.getStyleClass().add("order-step-done");
            } else if (!cancelled && i == current) {
                step.getStyleClass().add("order-step-current");
            }
            steps.getChildren().add(step);
        }
        if (cancelled) {
            Label note = new Label("Orden cancelada");
            note.getStyleClass().add("order-step-cancelled");
            steps.getChildren().add(note);
        }
        return steps;
    }

    private void advance(ServiceOrder order, OrderStatus next) {
        if (next == OrderStatus.IN_REPAIR) {
            int units = partUnits(order);
            if (units > 0 && !ConfirmDialog.confirm("Pasar a reparación",
                    "Se descontarán " + units + (units == 1 ? " unidad" : " unidades")
                            + " de repuestos del inventario. ¿Continuar?")) {
                return;
            }
        }
        if (next == OrderStatus.DELIVERED && !ConfirmDialog.confirm("Entregar al cliente",
                "La orden quedará cerrada y el vehículo volverá a Activo. ¿Continuar?")) {
            return;
        }
        runAction("No se pudo cambiar el estado",
                () -> workflow().changeStatus(order.getId(), next, currentUser()));
    }

    private void cancelOrder(ServiceOrder order) {
        boolean stockOut = order.getStatus() == OrderStatus.IN_REPAIR || order.getStatus() == OrderStatus.READY;
        String message = "La orden quedará cancelada y el vehículo volverá a Activo."
                + (stockOut && partUnits(order) > 0 ? " Los repuestos descontados volverán al inventario." : "")
                + " Esta acción no se puede deshacer. ¿Cancelar la orden?";
        if (ConfirmDialog.confirm("Cancelar orden", message)) {
            runAction("No se pudo cancelar la orden",
                    () -> workflow().changeStatus(order.getId(), OrderStatus.CANCELLED, currentUser()));
        }
    }

    // ---------- cliente y vehículo ----------

    private Node customerCard(ServiceOrder order) {
        Vehicle vehicle = order.getVehicle();
        Customer owner = vehicle == null ? null : vehicle.getOwner();
        VBox body = new VBox(2);
        if (owner == null) {
            body.getChildren().add(muted("Sin cliente"));
        } else {
            body.getChildren().addAll(strong(owner.getFullName()), muted(CustomerLabels.documentText(owner)),
                    muted(CustomerLabels.phoneText(owner.getPhone())));
        }
        VBox card = card("Cliente", IconSet.USERS, null, body);
        if (owner != null) {
            card.getChildren().add(link("Ver cliente", () -> navigator.navigateWithParameter(View.CUSTOMER_DETAIL,
                    owner.getId())));
        }
        return card;
    }

    private Node vehicleCard(ServiceOrder order) {
        Vehicle vehicle = order.getVehicle();
        VBox body = new VBox(2);
        if (vehicle == null) {
            body.getChildren().add(muted("Sin vehículo"));
        } else {
            body.getChildren().addAll(strong(vehicle.getMake() + " " + vehicle.getModel()),
                    muted(vehicle.getLicensePlate() + " · " + vehicle.getYear()),
                    muted(VehicleLabels.mileage(vehicle.getMileage())));
        }
        VBox card = card("Vehículo", IconSet.CAR, null, body);
        if (vehicle != null) {
            card.getChildren().add(link("Ver vehículo", () -> navigator.navigateWithParameter(View.VEHICLE_DETAIL,
                    vehicle.getId())));
        }
        return card;
    }

    // ---------- diagnóstico ----------

    private Node diagnosisCard(ServiceOrder order) {
        Diagnosis diagnosis = order.getDiagnosis();
        VBox body = new VBox(6);
        if (diagnosis == null) {
            body.getChildren().add(muted("Aún no hay diagnóstico registrado."));
        } else {
            body.getChildren().add(paragraph(diagnosis.getDescription()));
            if (diagnosis.getRecommendations() != null) {
                body.getChildren().add(paragraph("Recomendaciones: " + diagnosis.getRecommendations()));
            }
            if (diagnosis.getDate() != null) {
                body.getChildren().add(muted("Registrado el " + Formatter.date(diagnosis.getDate())));
            }
        }
        Button action = null;
        if (isOpen(order)) {
            action = smallButton(diagnosis == null ? "Registrar diagnóstico" : "Editar diagnóstico",
                    () -> editDiagnosis(order));
        }
        return card("Diagnóstico", IconSet.SEARCH, action, body);
    }

    private void editDiagnosis(ServiceOrder order) {
        Diagnosis current = order.getDiagnosis();
        TextArea description = textArea(current == null ? "" : current.getDescription(),
                "Qué se encontró al revisar el vehículo");
        TextArea recommendations = textArea(current == null || current.getRecommendations() == null ? ""
                : current.getRecommendations(), "Trabajos recomendados (opcional)");
        VBox content = new VBox(12, labeled("Diagnóstico *", description), labeled("Recomendaciones", recommendations));
        openDialog(new FormDialog(current == null ? "Registrar diagnóstico" : "Editar diagnóstico", null, content,
                "Guardar", () -> workflow().saveDiagnosis(order.getId(), description.getText(),
                recommendations.getText(), currentUser())));
    }

    // ---------- servicios y repuestos ----------

    private Node servicesCard(ServiceOrder order) {
        List<OrderLine> lines = lines(order, ServiceLine.class);
        Node table = lines.isEmpty() ? muted("Sin servicios.") : linesTable("Servicio", lines);
        Button add = canAddLines(order) ? smallButton("+ Agregar servicio", () -> addService(order)) : null;
        return card("Servicios", IconSet.WRENCH, add, table);
    }

    private Node partsCard(ServiceOrder order) {
        List<OrderLine> lines = lines(order, PartLine.class);
        VBox body = new VBox(10);
        body.getChildren().add(lines.isEmpty() ? muted("Sin repuestos.") : linesTable("Repuesto", lines));
        if (!lines.isEmpty()) {
            boolean deducted = order.getStatus() == OrderStatus.IN_REPAIR || order.getStatus() == OrderStatus.READY
                    || order.getStatus() == OrderStatus.DELIVERED;
            String note = switch (order.getStatus()) {
                case RECEIVED, DIAGNOSIS -> "Se descontarán del inventario al pasar a reparación.";
                case CANCELLED -> "Orden cancelada: los repuestos no se usaron o ya se devolvieron al inventario.";
                default -> "Descontados del inventario.";
            };
            Label stockNote = new Label(note);
            stockNote.getStyleClass().addAll("order-stock-note", deducted ? "order-stock-done" : "order-stock-pending");
            stockNote.setWrapText(true);
            body.getChildren().add(stockNote);
        }
        Button add = canAddLines(order) ? smallButton("+ Agregar repuesto", () -> addPart(order)) : null;
        return card("Repuestos", IconSet.BOX, add, body);
    }

    private void addService(ServiceOrder order) {
        ComboBox<ServiceOffering> box = combo("Selecciona un servicio", ServiceOffering::getName);
        TextField quantity = quantityField();
        Background.run(() -> AppContext.getInstance().getServiceOfferingService().findAll().stream()
                        .filter(ServiceOffering::isActive).toList(),
                services -> box.getItems().setAll(services),
                error -> { });
        VBox content = new VBox(12, labeled("Servicio *", box), labeled("Cantidad *", quantity));
        openDialog(new FormDialog("Agregar servicio", "Se cobra con el precio del catálogo.", content, "Agregar",
                () -> workflow().addService(order.getId(), box.getValue(), parseQuantity(quantity), currentUser())));
    }

    private void addPart(ServiceOrder order) {
        boolean deductNow = order.getStatus() == OrderStatus.IN_REPAIR;
        ComboBox<Part> box = combo("Selecciona un repuesto",
                part -> part.getName() + " · " + part.getCurrentStock() + " en stock");
        TextField quantity = quantityField();
        Background.run(() -> AppContext.getInstance().getPartService().findAll(),
                parts -> box.getItems().setAll(parts),
                error -> { });
        VBox content = new VBox(12, labeled("Repuesto *", box), labeled("Cantidad *", quantity));
        openDialog(new FormDialog("Agregar repuesto", deductNow
                ? "La orden está en reparación: el repuesto se descuenta del inventario al agregarlo."
                : "Se descontará del inventario cuando la orden pase a reparación.", content, "Agregar",
                () -> workflow().addPart(order.getId(), box.getValue(), parseQuantity(quantity), currentUser())));
    }

    private static Node linesTable(String firstColumn, List<OrderLine> lines) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("order-lines");
        ColumnConstraints name = new ColumnConstraints();
        name.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().add(name);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints number = new ColumnConstraints();
            number.setHalignment(HPos.RIGHT);
            number.setMinWidth(i == 0 ? 70 : 110);
            grid.getColumnConstraints().add(number);
        }
        String[] headers = {firstColumn, "Cantidad", "Valor unitario", "Total"};
        for (int c = 0; c < headers.length; c++) {
            Label header = new Label(headers[c]);
            header.getStyleClass().add("order-lines-header");
            grid.add(header, c, 0);
        }
        int row = 1;
        for (OrderLine line : lines) {
            grid.add(cell(line.getDescription(), false), 0, row);
            grid.add(cell(String.valueOf(line.getQuantity()), true), 1, row);
            grid.add(cell(Formatter.money(line.getUnitPrice()), true), 2, row);
            Label total = cell(Formatter.money(line.calculateTotal()), true);
            total.getStyleClass().add("order-lines-total");
            grid.add(total, 3, row);
            row++;
        }
        return grid;
    }

    // ---------- costos ----------

    private Node costsCard(ServiceOrder order) {
        BigDecimal services = sum(lines(order, ServiceLine.class));
        BigDecimal parts = sum(lines(order, PartLine.class));
        GridPane grid = new GridPane();
        grid.getStyleClass().add("order-costs");
        ColumnConstraints label = new ColumnConstraints();
        label.setHgrow(Priority.ALWAYS);
        ColumnConstraints value = new ColumnConstraints();
        value.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(label, value);
        addCost(grid, 0, "Servicios", services, false);
        addCost(grid, 1, "Repuestos", parts, false);
        addCost(grid, 2, "Subtotal", order.calculateSubtotal(), false);
        addCost(grid, 3, "Impuestos", order.calculateTaxes(), false);
        Region divider = new Region();
        divider.getStyleClass().add("order-costs-divider");
        grid.add(divider, 0, 4, 2, 1);
        addCost(grid, 5, "Total", order.calculateTotal(), true);
        VBox card = card("Resumen de costos", IconSet.CREDIT_CARD, null, grid);
        card.getStyleClass().add("order-costs-card");
        return card;
    }

    private static void addCost(GridPane grid, int row, String name, BigDecimal amount, boolean total) {
        Label label = new Label(name);
        Label value = new Label(Formatter.money(amount));
        label.getStyleClass().add(total ? "order-cost-total-label" : "order-cost-label");
        value.getStyleClass().add(total ? "order-cost-total" : "order-cost-value");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
    }

    // ---------- responsable ----------

    private Node responsibleCard(ServiceOrder order) {
        User responsible = order.getResponsible();
        VBox body = new VBox(2);
        if (responsible == null) {
            body.getChildren().add(muted("Sin asignar"));
        } else {
            body.getChildren().add(strong(responsible.getFullName()));
            if (responsible.getSpecialty() != null && !responsible.getSpecialty().isBlank()) {
                body.getChildren().add(muted("Especialidad: " + responsible.getSpecialty()));
            }
        }
        Button change = isOpen(order)
                ? smallButton(responsible == null ? "Asignar" : "Cambiar", () -> changeResponsible(order))
                : null;
        return card("Mecánico responsable", IconSet.USER, change, body);
    }

    private void changeResponsible(ServiceOrder order) {
        User none = new User();
        none.setId(0);
        none.setFirstName("Sin asignar");
        none.setLastName("");
        ComboBox<User> box = combo("Selecciona un mecánico", user -> user.getId() == 0 ? "Sin asignar"
                : user.getFullName() + (user.getSpecialty() == null || user.getSpecialty().isBlank()
                ? "" : " · " + user.getSpecialty()));
        Background.run(() -> AppContext.getInstance().getUserService().findActiveMechanics(),
                mechanics -> {
                    List<User> options = new ArrayList<>();
                    options.add(none);
                    options.addAll(mechanics);
                    box.getItems().setAll(options);
                    int currentId = order.getResponsible() == null ? 0 : order.getResponsible().getId();
                    options.stream().filter(u -> u.getId() == currentId).findFirst().ifPresent(box::setValue);
                },
                error -> { });
        openDialog(new FormDialog("Mecánico responsable", "Solo aparecen los mecánicos activos.",
                labeled("Mecánico", box), "Guardar",
                () -> workflow().assignResponsible(order.getId(),
                        box.getValue() == null || box.getValue().getId() == 0 ? null : box.getValue(),
                        currentUser())));
    }

    // ---------- historial ----------

    private Node historyCard(ServiceOrder order) {
        VBox list = new VBox(10);
        List<OrderHistory> history = new ArrayList<>(order.getHistory());
        java.util.Collections.reverse(history);
        if (history.isEmpty()) {
            list.getChildren().add(muted("Sin movimientos."));
        }
        for (OrderHistory entry : history) {
            Label text = new Label(entry.getDescription());
            text.setWrapText(true);
            text.getStyleClass().add("order-history-text");
            Label meta = new Label(Formatter.timestamp(entry.getTimestamp())
                    + (entry.getUser() == null ? "" : " · " + entry.getUser().getFullName()));
            meta.getStyleClass().add("order-history-meta");
            VBox item = new VBox(2, text, meta);
            item.getStyleClass().add("order-history-item");
            list.getChildren().add(item);
        }

        TextField note = new TextField();
        note.setPromptText("Agregar una nota al historial");
        note.getStyleClass().add("input");
        HBox.setHgrow(note, Priority.ALWAYS);
        Button add = new Button("Agregar");
        add.getStyleClass().add("btn-secondary");
        Runnable save = () -> {
            String text = note.getText();
            if (text == null || text.isBlank()) {
                return;
            }
            runAction("No se pudo agregar la nota", () -> workflow().addNote(order.getId(), text, currentUser()));
        };
        add.setOnAction(event -> save.run());
        note.setOnAction(event -> save.run());
        HBox noteRow = new HBox(8, note, add);
        noteRow.setAlignment(Pos.CENTER_LEFT);

        return card("Historial de la orden", IconSet.FILE_TEXT, null, noteRow, list);
    }

    // ---------- acciones ----------

    private void openDialog(FormDialog dialog) {
        if (root.getScene() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }
        if (dialog.open()) {
            reload();
        }
    }

    private void runAction(String errorTitle, Supplier<?> action) {
        root.setDisable(true);
        Background.run(action::get,
                result -> {
                    root.setDisable(false);
                    reload();
                },
                error -> {
                    root.setDisable(false);
                    ErrorDialog.show(errorTitle, error instanceof AutoGestException ? error.getMessage()
                            : "Revisa la conexión e inténtalo de nuevo.");
                });
    }

    // ---------- ayudas ----------

    private static boolean isOpen(ServiceOrder order) {
        return order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED;
    }

    /** Se pueden agregar líneas en Recibida, Diagnóstico y En reparación. */
    private static boolean canAddLines(ServiceOrder order) {
        OrderStatus status = order.getStatus();
        return status == OrderStatus.RECEIVED || status == OrderStatus.DIAGNOSIS || status == OrderStatus.IN_REPAIR;
    }

    private static List<OrderLine> lines(ServiceOrder order, Class<? extends OrderLine> type) {
        return order.getLines().stream().filter(type::isInstance).toList();
    }

    private static int partUnits(ServiceOrder order) {
        return lines(order, PartLine.class).stream().mapToInt(OrderLine::getQuantity).sum();
    }

    private static BigDecimal sum(List<OrderLine> lines) {
        return lines.stream().map(OrderLine::calculateTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Tarjeta con ícono y título; {@code headerAction} (opcional) va a la derecha del título. */
    private static VBox card(String title, String icon, Node headerAction, Node... body) {
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

    private static Button smallButton(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().addAll("btn-secondary", "btn-small");
        button.setOnAction(event -> action.run());
        return button;
    }

    private static Button link(String text, Runnable action) {
        Button link = new Button(text);
        link.getStyleClass().add("breadcrumb-link");
        link.setOnAction(event -> action.run());
        return link;
    }

    private static Label strong(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("order-strong");
        return label;
    }

    private static Label muted(String text) {
        Label label = new Label(text == null ? "-" : text);
        label.getStyleClass().add("order-muted");
        label.setWrapText(true);
        return label;
    }

    private static Label paragraph(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("order-paragraph");
        label.setWrapText(true);
        return label;
    }

    /** Celda que ocupa todo el ancho de su columna, para que la línea entre filas sea continua. */
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

    private static VBox labeled(String text, Node field) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return new VBox(6, label, field);
    }

    private static TextArea textArea(String value, String prompt) {
        TextArea area = new TextArea(value);
        area.setPromptText(prompt);
        area.setPrefRowCount(3);
        area.setWrapText(true);
        area.getStyleClass().add("input");
        return area;
    }

    private static TextField quantityField() {
        TextField field = new TextField("1");
        field.getStyleClass().add("input");
        field.setMaxWidth(120);
        return field;
    }

    private static int parseQuantity(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new com.autogest.exception.ValidationException("La cantidad debe ser un número entero");
        }
    }

    private static <T> ComboBox<T> combo(String prompt, Function<T, String> text) {
        ComboBox<T> box = new ComboBox<>();
        box.setPromptText(prompt);
        box.getStyleClass().add("input");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : text.apply(value);
            }

            @Override
            public T fromString(String string) {
                return null;
            }
        });
        return box;
    }

    private static User currentUser() {
        return Session.getCurrentUser();
    }

    private static OrderWorkflowService workflow() {
        return AppContext.getInstance().getOrderWorkflowService();
    }
}
