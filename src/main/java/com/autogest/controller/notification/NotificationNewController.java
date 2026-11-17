package com.autogest.controller.notification;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Channel;
import com.autogest.model.Customer;
import com.autogest.model.CustomerStatus;
import com.autogest.model.Notification;
import com.autogest.model.NotificationType;
import com.autogest.model.ServiceOrder;
import com.autogest.service.NotificationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Nueva notificación» (mockup Nueva notificación V1): elige
 * cliente (y opcionalmente una de sus órdenes), tipo, canal, asunto, mensaje y
 * fecha de programación. «Guardar» la deja pendiente o programada; «Enviar
 * ahora» la envía por su canal. Si recibe el id de un cliente lo preselecciona.
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
        return FormLayout.card("Contenido", grid);
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
        try {
            NotificationService service = AppContext.getInstance().getNotificationService();
            Notification saved = sendNow ? service.createAndSend(notification) : service.createNotification(notification);
            if (navigator != null) {
                navigator.navigateWithParameter(View.NOTIFICATION_DETAIL, saved.getId());
            }
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo guardar la notificación", e.getMessage());
        }
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
            customerBox.getItems().setAll(AppContext.getInstance().getCustomerService()
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
        orderBox.getItems().clear();
        if (customer == null) {
            return;
        }
        try {
            orderBox.getItems().setAll(AppContext.getInstance().getServiceOrderService()
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
