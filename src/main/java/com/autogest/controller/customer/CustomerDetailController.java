package com.autogest.controller.customer;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.KpiCard;
import com.autogest.controller.components.StatusBadge;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.NotFoundException;
import com.autogest.model.Customer;
import com.autogest.service.CustomerService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Controller of the customer detail screen (Detalle cliente mockup): header with
 * avatar, status and contact data, indicators, customer information, vehicles and
 * the recent-activity cards. It only translates screen events into calls to
 * {@link CustomerService}; it holds no business rules and does not know the
 * repository or the DAO.
 *
 * <p>The screen receives the customer id through {@link ParameterizedController}
 * and builds its content from the customer loaded by the Service. Until the orders,
 * invoices and notifications modules exist, their indicators show «-» and their
 * cards show an empty state (decision D-12). The vehicles card and indicator behave
 * the same way until {@code VehicleService} is available in {@code AppContext}.</p>
 *
 * <p>Principles: GRASP Controller, Low Coupling, High Cohesion, SRP.</p>
 */
public class CustomerDetailController implements NavigatorAware, ParameterizedController {

    private static final String CONTACT_SEPARATOR = "  |  ";

    /** Root container of the screen; the content is rebuilt on it every time the customer is shown. */
    @FXML
    private VBox root;

    private ViewNavigator navigator;

    /** {@inheritDoc} */
    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the customer and builds the screen. If the customer no longer exists,
     * it warns the user and goes back to the list.</p>
     */
    @Override
    public void receiveParameter(int parameter) {
        try {
            render(service().getDetail(parameter));
        } catch (NotFoundException e) {
            ErrorDialog.show("No se pudo abrir el cliente", e.getMessage());
            Platform.runLater(() -> navigator.navigateTo(View.CUSTOMERS));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el cliente", e.getMessage());
            Platform.runLater(() -> navigator.navigateTo(View.CUSTOMERS));
        }
    }

    /**
     * Builds the whole screen for a customer. Also used to refresh it after the
     * customer is deactivated.
     *
     * @param customer customer to show
     */
    private void render(Customer customer) {
        root.getChildren().setAll(buildHeader(customer), buildKpiRow(), buildColumns(customer));
    }

    /**
     * Builds the breadcrumb and the header: avatar, name, status badge, member-since
     * text, contact line and the action buttons.
     *
     * @param customer customer to show
     * @return the header block
     */
    private Node buildHeader(Customer customer) {
        Label breadcrumb = new Label(String.join("  /  ", "Clientes", customer.getFullName()));
        breadcrumb.getStyleClass().add("breadcrumb");

        Label avatar = new Label(CustomerLabels.initials(customer));
        avatar.getStyleClass().add("customer-avatar-large");

        Label name = new Label(customer.getFullName());
        name.getStyleClass().add("customer-detail-name");
        Label badge = StatusBadge.of(CustomerLabels.statusText(customer.getStatus()),
                CustomerLabels.statusType(customer.getStatus()));
        HBox nameRow = new HBox(10, name, badge);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label memberSince = new Label(CustomerLabels.memberSinceText(customer.getRegistrationDate()));
        memberSince.getStyleClass().add("customer-detail-meta");
        Label contact = new Label(contactLine(customer));
        contact.getStyleClass().add("customer-detail-contact");
        VBox identity = new VBox(4, nameRow, memberSince, contact);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(avatar, identity, spacer, buildActions(customer));
        header.getStyleClass().add("customer-detail-header");
        return new VBox(8, breadcrumb, header);
    }

    /**
     * Builds the header actions: edit, new vehicle and the «...» menu.
     *
     * @param customer customer the actions apply to
     * @return the row of actions
     */
    private HBox buildActions(Customer customer) {
        Button edit = new Button("Editar cliente");
        edit.getStyleClass().add("btn-primary");
        edit.setOnAction(event -> editCustomer(customer));

        Button newVehicle = new Button("+ Nuevo vehículo");
        newVehicle.getStyleClass().add("btn-primary");
        newVehicle.setOnAction(event -> newVehicle(customer));

        MenuItem editItem = new MenuItem("Editar");
        editItem.setOnAction(event -> editCustomer(customer));
        MenuItem createVehicle = new MenuItem("Crear vehículo");
        createVehicle.setOnAction(event -> newVehicle(customer));
        MenuItem invoices = new MenuItem("Ver facturas");
        invoices.setDisable(true);
        MenuItem deactivate = new MenuItem("Desactivar cliente");
        deactivate.setDisable(!customer.isActive());
        deactivate.setOnAction(event -> deactivate(customer));

        MenuButton more = new MenuButton();
        more.setGraphic(IconSet.icon(IconSet.MORE));
        more.getStyleClass().add("customer-actions");
        more.setFocusTraversable(false);
        more.getItems().addAll(editItem, createVehicle, invoices, deactivate);

        HBox actions = new HBox(8, edit, newVehicle, more);
        actions.setAlignment(Pos.CENTER);
        return actions;
    }

    /**
     * Builds the row of four indicators. Every value is «-» for now: vehicles wait
     * for {@code VehicleService}; orders, services and billing wait for their modules (D-12).
     *
     * @return the row of indicator cards
     */
    private HBox buildKpiRow() {
        HBox row = new HBox();
        row.getStyleClass().add("customer-kpi-row");
        row.setAlignment(Pos.CENTER_LEFT);
        for (KpiCard card : new KpiCard[] {
                new KpiCard("Vehículos", CustomerLabels.PLACEHOLDER, "A su nombre", IconSet.CAR),
                new KpiCard("Órdenes realizadas", CustomerLabels.PLACEHOLDER, "Todas sus órdenes", IconSet.WRENCH),
                new KpiCard("Servicios realizados", CustomerLabels.PLACEHOLDER, "En todas sus órdenes", IconSet.CHECK),
                new KpiCard("Total facturado", CustomerLabels.PLACEHOLDER, "Facturas pagadas", IconSet.CREDIT_CARD)}) {
            card.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
            row.getChildren().add(card);
        }
        return row;
    }

    /**
     * Lays out the two columns of the mockup: customer information and invoices on
     * the left; vehicles, activity and notifications on the right.
     *
     * @param customer customer to show
     * @return the two-column block
     */
    private Node buildColumns(Customer customer) {
        VBox left = new VBox(16,
                buildInfoCard(customer),
                miniCard("Facturas recientes", "Aún no hay facturas para este cliente."));

        HBox bottom = new HBox(16,
                miniCard("Actividad reciente", "Aún no hay actividad reciente."),
                miniCard("Notificaciones", "Aún no hay notificaciones."));
        for (Node card : bottom.getChildren()) {
            ((Region) card).setPrefWidth(0);
            ((Region) card).setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(card, Priority.ALWAYS);
        }
        VBox right = new VBox(16, buildVehiclesCard(customer), bottom);

        GridPane columns = new GridPane();
        columns.setHgap(16);
        ColumnConstraints leftColumn = new ColumnConstraints();
        leftColumn.setPercentWidth(36);
        ColumnConstraints rightColumn = new ColumnConstraints();
        rightColumn.setPercentWidth(64);
        columns.getColumnConstraints().addAll(leftColumn, rightColumn);
        columns.add(left, 0, 0);
        columns.add(right, 1, 0);
        return columns;
    }

    /**
     * Builds the «Información del cliente» card with its data pairs.
     *
     * @param customer customer to show
     * @return the information card
     */
    private Node buildInfoCard(Customer customer) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(14);
        ColumnConstraints first = new ColumnConstraints();
        first.setPercentWidth(50);
        ColumnConstraints second = new ColumnConstraints();
        second.setPercentWidth(50);
        grid.getColumnConstraints().addAll(first, second);

        grid.add(infoPair("Documento", CustomerLabels.documentText(customer)), 0, 0);
        grid.add(infoPair("Teléfono", CustomerLabels.phoneText(customer.getPhone())), 1, 0);
        grid.add(infoPair("Correo", customer.getEmail()), 0, 1, 2, 1);
        grid.add(infoPair("Dirección", customer.getAddress()), 0, 2);
        grid.add(infoPair("Ciudad", customer.getCity()), 1, 2);
        grid.add(infoPair("Preferencia de comunicación", CustomerLabels.communicationPreferenceText(customer)),
                0, 3, 2, 1);

        Button editInfo = new Button("Editar información");
        editInfo.getStyleClass().add("btn-primary");
        editInfo.setMaxWidth(Double.MAX_VALUE);
        editInfo.setOnAction(event -> editCustomer(customer));

        VBox card = new VBox(sectionTitle("Información del cliente"), grid, editInfo);
        card.getStyleClass().addAll("card", "customer-info-card");
        return card;
    }

    /**
     * Builds the «Vehículos del cliente» card. {@code VehicleService} is not in
     * {@code AppContext} yet, so the card shows its empty state; when the vehicles
     * module is merged, the table with {@code findByCustomer} replaces the message.
     *
     * @param customer customer whose vehicles will be listed
     * @return the vehicles card
     */
    private Node buildVehiclesCard(Customer customer) {
        Button add = new Button("+ Agregar vehículo");
        add.getStyleClass().add("btn-primary");
        add.setOnAction(event -> newVehicle(customer));
        HBox footer = new HBox(add);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox card = new VBox(12,
                sectionTitle("Vehículos del cliente"),
                emptyText("No hay vehículos para mostrar."),
                footer);
        card.getStyleClass().add("card");
        return card;
    }

    /**
     * Goes to the edit form of the customer.
     *
     * @param customer customer to edit
     */
    private void editCustomer(Customer customer) {
        navigator.navigateWithParameter(View.CUSTOMER_EDIT, customer.getId());
    }

    /**
     * Goes to the new-vehicle screen with this customer preselected as owner.
     *
     * @param customer owner of the new vehicle
     */
    private void newVehicle(Customer customer) {
        navigator.navigateWithParameter(View.VEHICLE_NEW, customer.getId());
    }

    /**
     * Deactivates the customer after confirmation and refreshes the screen.
     *
     * @param customer customer to deactivate
     */
    private void deactivate(Customer customer) {
        if (!ConfirmDialog.confirm("Desactivar cliente", "¿Deseas desactivar a " + customer.getFullName()
                + "? Conservará su historial, pero no podrá recibir nuevas órdenes.")) {
            return;
        }
        try {
            service().deactivateCustomer(customer.getId());
            render(service().getDetail(customer.getId()));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo desactivar el cliente", e.getMessage());
        }
    }

    /**
     * Joins document, phone and email in the single contact line of the header,
     * skipping the ones that are missing.
     *
     * @param customer customer to show
     * @return the contact line, for example {@code CC 1.234.567.890  |  300 123 4567  |  a@b.com}
     */
    private static String contactLine(Customer customer) {
        return Stream.of(CustomerLabels.documentText(customer),
                        CustomerLabels.phoneText(customer.getPhone()),
                        customer.getEmail())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(CONTACT_SEPARATOR));
    }

    /**
     * Builds a label and its value stacked, for the information card.
     *
     * @param labelText name of the data (Spanish)
     * @param value     value to show; a dash is shown when it is empty
     * @return the label and value in a box
     */
    private static VBox infoPair(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("customer-info-label");
        Label text = new Label(value == null || value.isBlank() ? CustomerLabels.PLACEHOLDER : value);
        text.getStyleClass().add("customer-info-value");
        return new VBox(2, label, text);
    }

    /**
     * Builds a card with a title and an empty-state message.
     *
     * @param title        card title (Spanish)
     * @param emptyMessage message shown while the data does not exist yet
     * @return the card
     */
    private static Node miniCard(String title, String emptyMessage) {
        VBox card = new VBox(sectionTitle(title), emptyText(emptyMessage));
        card.getStyleClass().addAll("empty-state", "customer-mini-card");
        return card;
    }

    /**
     * Builds the title label of a card.
     *
     * @param text title (Spanish)
     * @return the title label
     */
    private static Label sectionTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("card-title");
        return title;
    }

    /**
     * Builds the muted text used by empty states.
     *
     * @param text message (Spanish)
     * @return the message label
     */
    private static Label emptyText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("customer-empty-text");
        return label;
    }

    /**
     * Gets the customer service when it is used, not when the screen is built.
     *
     * @return the shared customer service
     */
    private CustomerService service() {
        return AppContext.getInstance().getCustomerService();
    }
}
