package com.autogest.controller.notification;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FilterBar;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Catalogs.Channel;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.model.Catalogs.NotificationType;
import com.autogest.service.NotificationService;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Notificaciones» (mockup Notificaciones V1): indicadores por
 * estado, filtros por texto, tipo, canal y estado, historial paginado con
 * acciones y la tarjeta «Actividad reciente».
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling.</p>
 */
public class NotificationsController implements Initializable, NavigatorAware {

    private static final int PAGE_SIZE = 10;
    private static final int RECENT = 5;

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private KpiCard sentCard;
    private KpiCard pendingCard;
    private KpiCard scheduledCard;
    private KpiCard failedCard;
    private PagedTable<Notification> table;
    private VBox recentList;

    private String searchText = "";
    private NotificationType typeFilter;
    private Channel channelFilter;
    private NotificationStatus statusFilter;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Notificaciones",
                "Gestiona las comunicaciones y recordatorios enviados a los clientes de tu taller.");
        header.setBreadcrumbs(List.of("Inicio", "Notificaciones"));
        header.setPrimaryAction("+ Nueva notificación", () -> {
            if (navigator != null) {
                navigator.navigateTo(View.NOTIFICATION_NEW);
            }
        });
        table = buildTable();
        recentList = new VBox(10);
        VBox history = FormLayout.card("Historial de notificaciones", buildFilters(), table);
        root.getChildren().addAll(header,
                FormLayout.actions(FormLayout.button("Enviar programadas vencidas", "btn-secondary", this::sendDue)),
                buildKpis(), history, FormLayout.card("Actividad reciente", recentList));
        Platform.runLater(this::reload);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    private HBox buildKpis() {
        sentCard = new KpiCard("Notificaciones enviadas", "-", "Entregadas al cliente", IconSet.MAIL);
        pendingCard = new KpiCard("Pendientes", "-", "Por enviar", IconSet.BELL);
        scheduledCard = new KpiCard("Programadas", "-", "Con fecha futura", IconSet.CALENDAR);
        failedCard = new KpiCard("Fallidas", "-", "Revisa y reintenta", IconSet.X);
        HBox row = new HBox(16);
        for (KpiCard card : List.of(sentCard, pendingCard, scheduledCard, failedCard)) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    private FilterBar buildFilters() {
        FilterBar bar = new FilterBar();
        bar.addSearch("Buscar por cliente, asunto o contenido...", text -> {
            searchText = text;
            reload();
        });
        bar.addCombo("Tipo", withAll(NotificationLabels.typeOptions()), value -> {
            typeFilter = NotificationLabels.typeFromText(value);
            reload();
        });
        bar.addCombo("Canal", withAll(NotificationLabels.channelOptions()), value -> {
            channelFilter = NotificationLabels.channelFromText(value);
            reload();
        });
        bar.addCombo("Estado", withAll(NotificationLabels.statusOptions()), value -> {
            statusFilter = NotificationLabels.statusFromText(value);
            reload();
        });
        return bar;
    }

    private PagedTable<Notification> buildTable() {
        PagedTable<Notification> pagedTable = new PagedTable<>(PAGE_SIZE);
        pagedTable.addColumn("Notificación", NotificationsController::title);
        pagedTable.addColumn("Cliente", n -> n.getCustomer() == null || n.getCustomer().getFirstName() == null
                ? "-" : n.getCustomer().getFullName());
        // Anchos mínimos para que tipo, fecha y estado no se corten (revisión de la Sesión 23).
        pagedTable.addColumn("Tipo", n -> NotificationLabels.typeText(n.getType()), 150);
        pagedTable.addColumn("Canal", n -> NotificationLabels.channelText(n.getChannel()), 90);
        pagedTable.addColumn("Fecha", n -> Formatter.timestamp(n.getScheduledDate()), 140);
        pagedTable.addColumn("Estado", n -> StatusBadge.of(NotificationLabels.statusText(n.getStatus()),
                NotificationLabels.statusType(n.getStatus())), 110);
        pagedTable.addColumn("Acciones", this::actionsCell, 90);
        pagedTable.setOnSelect(n -> openDetail(n.getId()));
        return pagedTable;
    }

    private Node actionsCell(Notification notification) {
        MenuButton menu = new MenuButton();
        menu.setGraphic(IconSet.icon(IconSet.MORE));
        menu.getStyleClass().add("icon-button");
        menu.setFocusTraversable(false);
        menu.getItems().add(item("Ver detalle", () -> openDetail(notification.getId())));
        if (notification.isPending()) {
            menu.getItems().add(item("Enviar ahora", () -> run(() -> service().send(notification.getId()))));
        }
        if (notification.getStatus() == NotificationStatus.FAILED) {
            menu.getItems().add(item("Reintentar", () -> run(() -> service().retry(notification.getId()))));
        }
        menu.getItems().add(item("Eliminar", () -> delete(notification)));
        return menu;
    }

    private void delete(Notification notification) {
        if (ConfirmDialog.confirmDelete("Eliminar notificación", "¿Deseas eliminar «" + title(notification) + "»?")) {
            run(() -> service().deleteNotification(notification.getId()));
        }
    }

    private void sendDue() {
        busy(true);
        Background.run(() -> service().sendDueScheduled(),
                sent -> {
                    busy(false);
                    Alert info = new Alert(Alert.AlertType.INFORMATION);
                    info.setTitle("Notificaciones programadas");
                    info.setHeaderText(null);
                    info.setContentText(sent == 0 ? "No hay notificaciones programadas para enviar todavía."
                            : "Se procesaron " + sent + " notificaciones programadas.");
                    info.showAndWait();
                    reload();
                },
                error -> {
                    busy(false);
                    ErrorDialog.show("No se pudo completar la acción", errorText(error));
                    reload();
                });
    }

    /**
     * Ejecuta una acción del Service en segundo plano (enviar, reintentar y
     * eliminar esperan la respuesta de Gmail o de la base), recarga la pantalla
     * y muestra el error si ocurre. Mientras tanto la ventana sigue respondiendo.
     */
    private void run(Runnable action) {
        busy(true);
        Background.run(() -> {
                    action.run();
                    return Boolean.TRUE;
                },
                done -> {
                    busy(false);
                    reload();
                },
                error -> {
                    busy(false);
                    ErrorDialog.show("No se pudo completar la acción", errorText(error));
                    reload();
                });
    }

    /** Lo que muestra la pantalla, leído de una vez fuera del hilo de la interfaz. */
    private record Snapshot(List<Notification> rows, int sent, int pending, int scheduled, int failed,
                            List<Notification> recent) {
    }

    private void reload() {
        String text = searchText;
        NotificationType type = typeFilter;
        Channel channel = channelFilter;
        NotificationStatus status = statusFilter;
        Background.run(() -> {
                    NotificationService notifications = service();
                    return new Snapshot(notifications.searchNotifications(text, type, channel, status),
                            notifications.countByStatus(NotificationStatus.SENT),
                            notifications.countByStatus(NotificationStatus.PENDING),
                            notifications.countByStatus(NotificationStatus.SCHEDULED),
                            notifications.countByStatus(NotificationStatus.FAILED),
                            notifications.findRecent(RECENT));
                },
                this::show,
                error -> {
                    table.setData(List.of());
                    ErrorDialog.show("No se pudieron cargar las notificaciones", errorText(error));
                });
    }

    private void show(Snapshot data) {
        table.setData(data.rows());
        sentCard.setValue(String.valueOf(data.sent()));
        pendingCard.setValue(String.valueOf(data.pending()));
        scheduledCard.setValue(String.valueOf(data.scheduled()));
        failedCard.setValue(String.valueOf(data.failed()));
        recentList.getChildren().clear();
        for (Notification n : data.recent()) {
            Label line = new Label(Formatter.timestamp(n.getScheduledDate()) + "  —  " + title(n) + " · "
                    + (n.getCustomer() == null ? "" : n.getCustomer().getFullName())
                    + " (" + NotificationLabels.statusText(n.getStatus()) + ")");
            line.setWrapText(true);
            recentList.getChildren().add(line);
        }
        if (recentList.getChildren().isEmpty()) {
            recentList.getChildren().add(new Label("Aún no hay notificaciones."));
        }
    }

    /** Desactiva la pantalla mientras se envía, para no repetir la acción. */
    private void busy(boolean value) {
        root.setDisable(value);
    }

    /** Texto del error para el usuario. */
    static String errorText(RuntimeException error) {
        return error instanceof AutoGestException && error.getMessage() != null
                ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo.";
    }

    /** Asunto o, si no tiene, el tipo de la notificación. */
    static String title(Notification notification) {
        return notification.getSubject() == null || notification.getSubject().isBlank()
                ? NotificationLabels.typeText(notification.getType()) : notification.getSubject();
    }

    private static List<String> withAll(List<String> values) {
        List<String> options = new ArrayList<>();
        options.add(NotificationLabels.ALL);
        options.addAll(values);
        return options;
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(event -> action.run());
        return item;
    }

    private void openDetail(int id) {
        if (navigator != null) {
            navigator.navigateWithParameter(View.NOTIFICATION_DETAIL, id);
        }
    }

    private NotificationService service() {
        return AppContext.getInstance().getNotificationService();
    }
}
