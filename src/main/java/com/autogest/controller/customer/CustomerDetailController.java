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
 * Controlador de la pantalla de detalles del cliente (maqueta «Detalle cliente»): encabezado con
 * avatar, estado y datos de contacto, indicadores, información del cliente, vehículos y
 * tarjetas de actividad reciente. Solo traduce los eventos de la pantalla en llamadas a
 * {@link CustomerService}; no contiene reglas de negocio ni conoce el repositorio
 * ni el DAO.
 *
 * <p>La pantalla recibe el id del cliente mediante {@link ParameterizedController}
 * y construye su contenido a partir del cliente cargado por el servicio. Hasta que existan
 * los módulos de órdenes, facturas y notificaciones, sus indicadores muestran «-» y sus
 * tarjetas muestran un estado vacío (decisión D-12). La tarjeta y el indicador de vehículos
 * funcionan de la misma manera hasta que {@code VehicleService} esté disponible en {@code AppContext}.</p>
 *
 * <p>Principios: controlador GRASP, bajo acoplamiento, alta cohesión y SRP.</p>
 */
public class CustomerDetailController implements NavigatorAware, ParameterizedController {

    private static final String CONTACT_SEPARATOR = "  |  ";

    /** Contenedor raíz de la pantalla; el contenido se reconstruye en él cada vez que se muestra el cliente. */
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
     * <p>Carga el cliente y construye la pantalla. Si el cliente ya no existe,
     * avisa al usuario y vuelve a la lista.</p>
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
     * Construye toda la pantalla de un cliente. También se utiliza para actualizarla
     * después de desactivar al cliente.
     *
     * @param customer cliente que se mostrará
     */
    private void render(Customer customer) {
        root.getChildren().setAll(buildHeader(customer), buildKpiRow(), buildColumns(customer));
    }

    /**
     * Construye la ruta de navegación y el encabezado: avatar, nombre, insignia de estado,
     * texto que indica desde cuándo es cliente, línea de contacto y botones de acción.
     *
     * @param customer cliente que se mostrará
     * @return el bloque del encabezado
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
     * Construye las acciones del encabezado: editar, crear vehículo y el menú «...».
     *
     * @param customer cliente al que se aplican las acciones
     * @return la fila de acciones
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
     * Construye la fila de cuatro indicadores. Por ahora, todos los valores son «-»:
     * los vehículos dependen de {@code VehicleService}; las órdenes, los servicios y la
     * facturación dependen de sus respectivos módulos (D-12).
     *
     * @return la fila de tarjetas de indicadores
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
     * Organiza las dos columnas de la maqueta: información del cliente y facturas a
     * la izquierda; vehículos, actividad y notificaciones a la derecha.
     *
     * @param customer cliente que se mostrará
     * @return el bloque de dos columnas
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
     * Construye la tarjeta «Información del cliente» con sus pares de datos.
     *
     * @param customer cliente que se mostrará
     * @return la tarjeta de información
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
     * Construye la tarjeta «Vehículos del cliente». {@code VehicleService} aún no está en
     * {@code AppContext}, por lo que la tarjeta muestra su estado vacío; cuando se integre
     * el módulo de vehículos, la tabla con {@code findByCustomer} reemplazará el mensaje.
     *
     * @param customer cliente cuyos vehículos se mostrarán
     * @return la tarjeta de vehículos
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
     * Abre el formulario de edición del cliente.
     *
     * @param customer cliente que se editará
     */
    private void editCustomer(Customer customer) {
        navigator.navigateWithParameter(View.CUSTOMER_EDIT, customer.getId());
    }

    /**
     * Abre la pantalla para crear un vehículo con este cliente preseleccionado como propietario.
     *
     * @param customer propietario del nuevo vehículo
     */
    private void newVehicle(Customer customer) {
        navigator.navigateWithParameter(View.VEHICLE_NEW, customer.getId());
    }

    /**
     * Desactiva al cliente después de obtener la confirmación y actualiza la pantalla.
     *
     * @param customer cliente que se desactivará
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
     * Une el documento, el teléfono y el correo electrónico en una sola línea de contacto
     * en el encabezado, omitiendo los datos que no estén disponibles.
     *
     * @param customer cliente que se mostrará
     * @return la línea de contacto, por ejemplo {@code CC 1.234.567.890  |  300 123 4567  |  a@b.com}
     */
    private static String contactLine(Customer customer) {
        return Stream.of(CustomerLabels.documentText(customer),
                        CustomerLabels.phoneText(customer.getPhone()),
                        customer.getEmail())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(CONTACT_SEPARATOR));
    }

    /**
     * Construye una etiqueta y su valor apilados en la tarjeta de información.
     *
     * @param labelText nombre del dato (en español)
     * @param value     valor que se mostrará; se muestra un guion si está vacío
     * @return la etiqueta y el valor dentro de un contenedor
     */
    private static VBox infoPair(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("customer-info-label");
        Label text = new Label(value == null || value.isBlank() ? CustomerLabels.PLACEHOLDER : value);
        text.getStyleClass().add("customer-info-value");
        return new VBox(2, label, text);
    }

    /**
     * Construye una tarjeta con un título y un mensaje de estado vacío.
     *
     * @param title        título de la tarjeta (en español)
     * @param emptyMessage mensaje que se muestra mientras los datos aún no existen
     * @return la tarjeta
     */
    private static Node miniCard(String title, String emptyMessage) {
        VBox card = new VBox(sectionTitle(title), emptyText(emptyMessage));
        card.getStyleClass().addAll("empty-state", "customer-mini-card");
        return card;
    }

    /**
     * Construye la etiqueta del título de una tarjeta.
     *
     * @param text título (en español)
     * @return la etiqueta del título
     */
    private static Label sectionTitle(String text) {
        Label title = new Label(text);
        title.getStyleClass().add("card-title");
        return title;
    }

    /**
     * Construye el texto atenuado que se utiliza en los estados vacíos.
     *
     * @param text mensaje (en español)
     * @return la etiqueta del mensaje
     */
    private static Label emptyText(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("customer-empty-text");
        return label;
    }

    /**
     * Obtiene el servicio de clientes cuando se necesita, no cuando se construye la pantalla.
     *
     * @return el servicio de clientes compartido
     */
    private CustomerService service() {
        return AppContext.getInstance().getCustomerService();
    }
}
