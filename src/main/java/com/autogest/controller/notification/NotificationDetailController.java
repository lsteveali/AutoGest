package com.autogest.controller.notification;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Customer;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.service.NotificationService;
import com.autogest.util.Formatter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Detalle de notificación» (mockup Detalle de notificación V1):
 * datos del envío, destinatario, mensaje y acciones (enviar, reintentar,
 * eliminar).
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP.</p>
 */
public class NotificationDetailController implements Initializable, ParameterizedController, NavigatorAware {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private int notificationId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // El contenido se arma cuando llega el id.
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public void receiveParameter(int parameter) {
        notificationId = parameter;
        refresh();
    }

    private void refresh() {
        Notification notification;
        try {
            notification = service().getDetail(notificationId);
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar la notificación", e.getMessage());
            return;
        }
        PageHeader header = new PageHeader(NotificationsController.title(notification),
                NotificationLabels.typeText(notification.getType()));
        header.setBreadcrumbs(List.of("Inicio", "Notificaciones", "Detalle"));

        Label message = new Label(notification.getMessage());
        message.setWrapText(true);
        root.getChildren().setAll(header, buildActions(notification), buildInfo(notification),
                FormLayout.card("Mensaje", message));
    }

    private HBox buildActions(Notification notification) {
        HBox actions = FormLayout.actions();
        if (notification.isPending()) {
            actions.getChildren().add(FormLayout.button("Enviar ahora", "btn-primary",
                    () -> run(() -> service().send(notificationId))));
        }
        if (notification.getStatus() == NotificationStatus.FAILED) {
            actions.getChildren().add(FormLayout.button("Reintentar", "btn-primary",
                    () -> run(() -> service().retry(notificationId))));
        }
        actions.getChildren().add(FormLayout.button("Eliminar", "btn-danger", this::delete));
        actions.getChildren().add(0, FormLayout.button("Volver", "btn-secondary", this::goBack));
        return actions;
    }

    private VBox buildInfo(Notification notification) {
        Customer customer = notification.getCustomer();
        GridPane grid = FormLayout.grid(3);
        FormLayout.row(grid, 0,
                FormLayout.detail("Cliente", customer == null ? "-" : customer.getFullName()),
                FormLayout.detail("Correo", customer == null ? "-" : customer.getEmail()),
                FormLayout.detail("Teléfono", customer == null ? "-" : customer.getPhone()));
        FormLayout.row(grid, 1,
                FormLayout.detail("Canal", NotificationLabels.channelText(notification.getChannel())),
                FormLayout.detail("Fecha", Formatter.timestamp(notification.getScheduledDate())),
                FormLayout.detail("Estado", StatusBadge.of(NotificationLabels.statusText(notification.getStatus()),
                        NotificationLabels.statusType(notification.getStatus()))));
        FormLayout.row(grid, 2,
                FormLayout.detail("Orden relacionada", notification.getOrder() == null
                        ? "Sin orden" : "#" + notification.getOrder().getNumber()));
        return FormLayout.card("Información del envío", grid);
    }

    private void delete() {
        if (!ConfirmDialog.confirmDelete("Eliminar notificación", "¿Deseas eliminar esta notificación?")) {
            return;
        }
        try {
            service().deleteNotification(notificationId);
            goBack();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo eliminar la notificación", e.getMessage());
        }
    }

    private void run(Runnable action) {
        try {
            action.run();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo completar la acción", e.getMessage());
        }
        refresh();
    }

    private void goBack() {
        if (navigator != null) {
            navigator.goBack();
        }
    }

    private NotificationService service() {
        return AppContext.getInstance().getNotificationService();
    }
}
