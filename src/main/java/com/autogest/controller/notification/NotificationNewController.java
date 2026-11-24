package com.autogest.controller.notification;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.SearchableCombo;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Catalogs.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.model.Invoice;
import com.autogest.model.Catalogs.InvoiceStatus;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationType;
import com.autogest.model.ServiceOrder;
import com.autogest.service.NotificationService;
import com.autogest.service.NotificationTemplate;
import com.autogest.util.Background;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Controlador de «Nueva notificación» (mockup Nueva notificación V1): elige
 * cliente (y opcionalmente una de sus órdenes), tipo, canal, asunto, mensaje y
 * fecha de programación. «Guardar» la deja pendiente o programada; «Enviar
 * ahora» la envía por su canal. Si recibe el id de un cliente lo preselecciona.
 *
 * <p>Al elegir el tipo, el asunto y el mensaje se llenan con el texto
 * predeterminado de {@code NotificationTemplate} (Líder); en Facturación, con
 * los datos de la factura de la orden, que va adjunta en PDF y PNG.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP, Low Coupling.</p>
 */
public class NotificationNewController implements Initializable, NavigatorAware, ParameterizedController {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private ComboBox<Customer> customerBox;
    private ComboBox<ServiceOrder> orderBox;
    private ComboBox<String> typeBox;
    private ComboBox<String> channelBox;
    private TextField subjectField;
    private TextArea messageArea;
    private DatePicker datePicker;
    private TextField timeField;
    private int pendingCustomerId;
    private Label presetHint;
    private String presetSubject;
    private String presetMessage;
    private Invoice presetInvoice;
    private int presetRequest;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Nueva notificación",
                "Envía o programa una comunicación para un cliente del taller.");
        header.setBreadcrumbs(List.of("Inicio", "Notificaciones", "Nueva notificación"));
        root.getChildren().addAll(header, buildRecipientCard(), buildContentCard(), buildScheduleCard(),
                FormLayout.actions(
                        FormLayout.button("Cancelar", "btn-secondary", this::goBack),
                        FormLayout.button("Guardar", "btn-secondary", () -> save(false)),
                        FormLayout.button("Enviar ahora", "btn-primary", () -> save(true))));
        Platform.runLater(this::loadCustomers);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /** Recibe el id de un cliente para dejarlo preseleccionado. */
    @Override
    public void receiveParameter(int customerId) {
        pendingCustomerId = customerId;
    }

    private VBox buildRecipientCard() {
        customerBox = FormLayout.combo("Seleccionar cliente", Customer::getFullName);
        orderBox = FormLayout.combo("Sin orden asociada", order -> "#" + order.getNumber() + " · "
                + (order.getVehicle() == null || order.getVehicle().getMake() == null ? ""
                : order.getVehicle().getMake() + " " + order.getVehicle().getModel()));
        customerBox.valueProperty().addListener((obs, old, customer) -> loadOrders(customer));
        orderBox.valueProperty().addListener((obs, old, order) -> refreshPreset(false));
        // Se pueden escribir: la lista se reduce por nombre o documento del cliente,
        // y por número, placa o vehículo de la orden (Líder, SearchableCombo).
        SearchableCombo.enable(customerBox, Customer::getFullName,
                customer -> customer.getFullName() + " " + customer.getDocumentNumber());
        SearchableCombo.enable(orderBox, orderBox.getConverter()::toString,
                order -> orderBox.getConverter().toString(order) + " "
                        + (order.getVehicle() == null ? "" : order.getVehicle().getLicensePlate()));
        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Cliente *", customerBox),
                FormLayout.labeled("Orden relacionada", orderBox));
        return FormLayout.card("Destinatario", grid);
    }

    private VBox buildContentCard() {
        typeBox = FormLayout.combo("Seleccionar tipo");
        typeBox.getItems().setAll(NotificationLabels.typeOptions());
        typeBox.setValue(NotificationLabels.typeText(NotificationType.GENERAL));
        channelBox = FormLayout.combo("Seleccionar canal");
        channelBox.getItems().setAll(NotificationLabels.channelOptions());
        channelBox.setValue(NotificationLabels.channelText(Channel.EMAIL));
        subjectField = FormLayout.textField("Actualización de su orden");
        messageArea = FormLayout.textArea("Escribe el mensaje para el cliente...", 5);
        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Tipo *", typeBox), FormLayout.labeled("Canal *", channelBox));
        FormLayout.fullRow(grid, 1, FormLayout.labeled("Asunto", subjectField));
        FormLayout.fullRow(grid, 2, FormLayout.labeled("Mensaje *", messageArea));
        FormLayout.fullRow(grid, 3, buildPresetRow());
        typeBox.valueProperty().addListener((obs, old, type) -> refreshPreset(false));
        channelBox.valueProperty().addListener((obs, old, channel) -> updatePresetHint());
        updatePresetHint();
        return FormLayout.card("Contenido", grid);
    }

    // ---------- mensaje predeterminado por tipo (Líder) ----------

    /**
     * Fila bajo el mensaje: aviso de adjuntos (Facturación) y «Usar mensaje
     * predeterminado», que vuelve al texto del tipo si se editó.
     */
    private HBox buildPresetRow() {
        presetHint = new Label();
        presetHint.setWrapText(true);
        presetHint.getStyleClass().add("settings-note");
        HBox.setHgrow(presetHint, Priority.ALWAYS);
        presetHint.setMaxWidth(Double.MAX_VALUE);
        Button reset = new Button("Usar mensaje predeterminado");
        reset.getStyleClass().addAll("btn-secondary", "btn-small");
        reset.setMinWidth(Region.USE_PREF_SIZE);
        reset.setOnAction(event -> refreshPreset(true));
        HBox row = new HBox(12, presetHint, reset);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /**
     * Llena asunto y mensaje con el texto predeterminado del tipo, completado
     * con el cliente, la orden y (en Facturación) la factura de la orden. No
     * pisa lo que el usuario haya escrito, salvo que lo pida ({@code force}).
     */
    private void refreshPreset(boolean force) {
        Customer customer = customerBox.getValue();
        ServiceOrder order = orderBox.getValue();
        NotificationType type = NotificationLabels.typeFromText(typeBox.getValue());
        int request = ++presetRequest;
        if (type != NotificationType.BILLING || order == null) {
            presetInvoice = null;
            applyPreset(customer, order, type, null, force);
            return;
        }
        Background.run(() -> Optional.ofNullable(
                        AppContext.getInstance().getInvoiceService().findByOrder(order.getId())),
                invoice -> {
                    if (request == presetRequest) {
                        presetInvoice = invoice.filter(found -> found.getStatus() != InvoiceStatus.VOIDED)
                                .orElse(null);
                        applyPreset(customer, order, type, presetInvoice, force);
                    }
                },
                error -> {
                    if (request == presetRequest) {
                        presetInvoice = null;
                        applyPreset(customer, order, type, null, force);
                    }
                });
    }

    private void applyPreset(Customer customer, ServiceOrder order, NotificationType type, Invoice invoice,
                             boolean force) {
        updatePresetHint();
        if (customer == null) {
            return;
        }
        boolean untouched = presetUntouched(subjectField.getText(), presetSubject)
                && presetUntouched(messageArea.getText(), presetMessage);
        if (!force && !untouched) {
            return;
        }
        NotificationTemplate template = NotificationTemplate.forManual(type, invoice);
        Map<String, String> values = NotificationTemplate.valuesFor(customer, order, invoice);
        presetSubject = template.subject(values);
        presetMessage = template.message(values);
        subjectField.setText(presetSubject);
        messageArea.setText(presetMessage);
    }

    /** El campo sigue como lo dejó el texto predeterminado (o vacío). */
    private static boolean presetUntouched(String current, String preset) {
        return current == null || current.isBlank() || current.equals(preset);
    }

    /** Explica si el correo llevará la factura adjunta. */
    private void updatePresetHint() {
        NotificationType type = NotificationLabels.typeFromText(typeBox.getValue());
        String text;
        if (type != NotificationType.BILLING) {
            text = "El asunto y el mensaje cambian según el tipo; puedes editarlos antes de enviar.";
        } else if (NotificationLabels.channelFromText(channelBox.getValue()) != Channel.EMAIL) {
            text = "La factura en PDF y PNG solo se adjunta cuando el canal es Correo.";
        } else if (orderBox.getValue() == null) {
            text = "Elige la orden facturada: el correo llevará la factura adjunta en PDF y PNG.";
        } else if (presetInvoice == null) {
            text = "Esta orden no tiene una factura vigente: el correo saldrá sin adjuntos.";
        } else {
            text = "Se adjuntará la factura " + presetInvoice.getNumber() + " en PDF y PNG.";
        }
        presetHint.setText(text);
    }

    private VBox buildScheduleCard() {
        datePicker = new DatePicker();
        datePicker.getStyleClass().add("input");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setPromptText("Enviar ahora");
        timeField = FormLayout.textField("08:00");
        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Fecha de envío (opcional)", datePicker),
                FormLayout.labeled("Hora (HH:mm)", timeField));
        return FormLayout.card("Programación", grid);
    }

    private void save(boolean sendNow) {
        Notification notification;
        try {
            notification = readForm();
        } catch (ValidationException e) {
            ErrorDialog.show("Revisa la notificación", e.getMessage());
            return;
        }
        // Guardar y enviar corre en segundo plano: Gmail tarda unos segundos en
        // responder y la ventana no debe quedarse congelada mientras tanto.
        root.setDisable(true);
        NotificationService service = AppContext.getInstance().getNotificationService();
        Background.run(() -> sendNow ? service.createAndSend(notification) : service.createNotification(notification),
                saved -> {
                    root.setDisable(false);
                    if (navigator != null) {
                        navigator.navigateWithParameter(View.NOTIFICATION_DETAIL, saved.getId());
                    }
                },
                error -> {
                    root.setDisable(false);
                    ErrorDialog.show("No se pudo guardar la notificación", NotificationsController.errorText(error));
                });
    }

    private Notification readForm() {
        Notification notification = new Notification();
        notification.setCustomer(customerBox.getValue());
        notification.setOrder(orderBox.getValue());
        notification.setType(NotificationLabels.typeFromText(typeBox.getValue()));
        notification.setChannel(NotificationLabels.channelFromText(channelBox.getValue()));
        notification.setSubject(FormLayout.text(subjectField));
        notification.setMessage(FormLayout.text(messageArea));
        notification.setScheduledDate(readSchedule());
        return notification;
    }

    /** Fecha y hora de programación; {@code null} = enviar ahora. */
    private LocalDateTime readSchedule() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            return null;
        }
        String time = FormLayout.text(timeField);
        try {
            return date.atTime(time == null ? LocalTime.of(8, 0) : LocalTime.parse(time));
        } catch (DateTimeParseException e) {
            throw new ValidationException("La hora debe tener el formato HH:mm (por ejemplo 08:30)");
        }
    }

    private void loadCustomers() {
        try {
            SearchableCombo.setItems(customerBox, AppContext.getInstance().getCustomerService()
                    .searchCustomers(null, CustomerStatus.ACTIVE));
            if (pendingCustomerId > 0) {
                customerBox.getItems().stream().filter(c -> c.getId() == pendingCustomerId)
                        .findFirst().ifPresent(customerBox::setValue);
            }
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron cargar los clientes", e.getMessage());
        }
    }

    private void loadOrders(Customer customer) {
        orderBox.setValue(null);
        SearchableCombo.setItems(orderBox, java.util.List.of());
        refreshPreset(false);
        if (customer == null) {
            return;
        }
        try {
            SearchableCombo.setItems(orderBox, AppContext.getInstance().getServiceOrderService()
                    .findByCustomer(customer.getId()));
        } catch (AutoGestException e) {
            // La orden es opcional: si falla la carga se puede notificar sin ella.
        }
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }
}
