package com.autogest.controller.customer;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.DocumentType;
import com.autogest.service.CustomerService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller of the customer form (New customer mockup), shared by the
 * {@code CUSTOMER_NEW} and {@code CUSTOMER_EDIT} views. When the screen receives
 * a customer id ({@link ParameterizedController}) it loads that customer and
 * edits it; otherwise it registers a new one.
 *
 * <p>It only reads the fields, calls {@link CustomerService} and shows the result:
 * validation lives in the Service and its Spanish message is displayed below the
 * form. The only thing the screen enforces is the maximum length of each field,
 * equal to the length of the matching database column.</p>
 *
 * <p>Principles: GRASP Controller, Low Coupling, High Cohesion, SRP.</p>
 */
public class CustomerFormController implements Initializable, NavigatorAware, ParameterizedController {

    private static final String NEW_SUBTITLE =
            "Registra la información del cliente para comenzar a gestionar sus vehículos y servicios.";
    private static final String EDIT_SUBTITLE = "Actualiza la información del cliente.";

    /** Root container of the screen; the controller builds the header and the form card on it. */
    @FXML
    private VBox root;

    private ViewNavigator navigator;

    /** Id of the customer being edited, or 0 when the form registers a new one. */
    private int customerId;

    private ComboBox<DocumentType> documentType;
    private TextField documentNumber;
    private TextField firstName;
    private TextField lastName;
    private TextField phone;
    private TextField email;
    private TextField address;
    private ComboBox<String> city;
    private TextArea notes;
    private CheckBox promotions;
    private Label errorLabel;

    /** {@inheritDoc} */
    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Builds the screen in "new customer" mode. If the navigator later delivers
     * an id, {@link #receiveParameter(int)} switches it to edit mode.</p>
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createFields();
        root.getChildren().addAll(buildHeader(false), buildFormCard());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Switches the screen to edit mode: loads the customer and fills the form.
     * If the customer no longer exists, it warns the user and goes back to the list.</p>
     */
    @Override
    public void receiveParameter(int parameter) {
        try {
            Customer customer = service().getDetail(parameter);
            customerId = parameter;
            fill(customer);
            root.getChildren().set(0, buildHeader(true));
        } catch (NotFoundException e) {
            ErrorDialog.show("No se pudo abrir el cliente", e.getMessage());
            Platform.runLater(() -> navigator.navigateTo(View.CUSTOMERS));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo cargar el cliente", e.getMessage());
            Platform.runLater(() -> navigator.navigateTo(View.CUSTOMERS));
        }
    }

    /**
     * Builds the page header with its breadcrumb.
     *
     * @param editing {@code true} for the "Editar cliente" header, {@code false} for "Nuevo cliente"
     * @return the header component
     */
    private PageHeader buildHeader(boolean editing) {
        String title = editing ? "Editar cliente" : "Nuevo cliente";
        PageHeader header = new PageHeader(title, editing ? EDIT_SUBTITLE : NEW_SUBTITLE);
        header.setBreadcrumbs(List.of("Clientes", title));
        return header;
    }

    /**
     * Creates every input control of the form with its prompt text, style class and
     * maximum length. The maximum lengths match the columns of the {@code customers} table.
     */
    private void createFields() {
        documentType = new ComboBox<>();
        documentType.getItems().addAll(DocumentType.values());
        documentType.setPromptText("Seleccionar");
        documentType.getStyleClass().add("input");
        documentType.setConverter(new StringConverter<>() {
            @Override
            public String toString(DocumentType type) {
                return CustomerLabels.documentTypeText(type);
            }

            @Override
            public DocumentType fromString(String text) {
                return CustomerLabels.documentTypeFromText(text);
            }
        });

        documentNumber = textField("Ingresar número de documento", 30);
        firstName = textField("Ingresar nombres", 100);
        lastName = textField("Ingresar apellidos", 100);
        phone = textField("+57 300 000 0000", 30);
        email = textField("correo@ejemplo.com", 150);
        address = textField("Dirección del cliente", 200);

        city = new ComboBox<>();
        city.setEditable(true);
        city.getItems().addAll(CustomerLabels.COMMON_CITIES);
        city.setPromptText("Seleccionar ciudad");
        city.getStyleClass().add("input");
        limitLength(city.getEditor(), 100);

        notes = new TextArea();
        notes.setPromptText("Información adicional sobre el cliente...");
        notes.setPrefRowCount(3);
        notes.setWrapText(true);
        notes.getStyleClass().add("input");

        promotions = new CheckBox("Acepta recibir notificaciones y promociones por correo electrónico.");

        errorLabel = new Label();
        errorLabel.getStyleClass().add("customer-form-error");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Lays out the form card: the three sections of the mockup, the error label
     * and the row with the promotions checkbox and the Cancel / Save buttons.
     *
     * @return the card that contains the whole form
     */
    private Node buildFormCard() {
        GridPane personal = grid();
        personal.add(field("Tipo de documento", true, documentType), 0, 0);
        personal.add(field("Nombres", true, firstName), 1, 0);
        personal.add(field("Número de documento", true, documentNumber), 0, 1);
        personal.add(field("Apellidos", true, lastName), 1, 1);

        GridPane contact = grid();
        contact.add(field("Teléfono", true, phone), 0, 0);
        contact.add(field("Dirección", false, address), 1, 0);
        contact.add(field("Correo electrónico", true, email), 0, 1);
        contact.add(field("Ciudad", false, city), 1, 1);

        Button cancel = new Button("Cancelar");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(event -> cancel());
        Button save = new Button("Guardar cliente");
        save.getStyleClass().add("btn-primary");
        save.setOnAction(event -> save());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(promotions, spacer, cancel, save);
        actions.getStyleClass().add("customer-form-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(
                section("Información personal", personal),
                section("Información de contacto", contact),
                section("Información adicional", field("Notas", false, notes)),
                errorLabel,
                actions);
        card.getStyleClass().addAll("card", "customer-form-card");
        return card;
    }

    /**
     * Fills the form with the data of an existing customer. Optional columns can be
     * {@code null} in the database, so they go through {@link #orEmpty(String)}.
     *
     * @param customer customer whose data is shown
     */
    private void fill(Customer customer) {
        documentType.setValue(customer.getDocumentType());
        documentNumber.setText(customer.getDocumentNumber());
        firstName.setText(customer.getFirstName());
        lastName.setText(customer.getLastName());
        phone.setText(customer.getPhone());
        email.setText(customer.getEmail());
        address.setText(orEmpty(customer.getAddress()));
        city.getEditor().setText(orEmpty(customer.getCity()));
        notes.setText(orEmpty(customer.getNotes()));
        promotions.setSelected(customer.isAcceptsPromotions());
    }

    /**
     * Returns the text unchanged, or an empty string when it is {@code null}. Used
     * before showing optional database values in the form fields.
     *
     * @param value text that may be {@code null}
     * @return the same text, or {@code ""} if it was {@code null}
     */
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Builds a {@link Customer} with what is typed in the form. It does not validate:
     * that is the job of {@link CustomerService}.
     *
     * @return customer with the form values (id is 0 when registering a new one)
     */
    private Customer readForm() {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setDocumentType(documentType.getValue());
        customer.setDocumentNumber(documentNumber.getText());
        customer.setFirstName(firstName.getText());
        customer.setLastName(lastName.getText());
        customer.setPhone(phone.getText());
        customer.setEmail(email.getText());
        customer.setAddress(address.getText());
        customer.setCity(city.getEditor().getText());
        customer.setNotes(notes.getText());
        customer.setAcceptsPromotions(promotions.isSelected());
        return customer;
    }

    /**
     * Saves the customer. On success it goes back to the list (new customer) or to
     * the detail screen (edited customer). A {@link ValidationException} is shown
     * below the form; any other error is shown in a dialog.
     */
    private void save() {
        hideError();
        Customer customer = readForm();
        try {
            if (customerId == 0) {
                service().registerCustomer(customer);
                navigator.navigateTo(View.CUSTOMERS);
            } else {
                service().updateCustomer(customer);
                navigator.navigateWithParameter(View.CUSTOMER_DETAIL, customerId);
            }
        } catch (ValidationException e) {
            showError(e.getMessage());
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudo guardar el cliente", e.getMessage());
        }
    }

    /**
     * Leaves the form without saving: back to the list (new customer) or to the
     * detail screen (edited customer).
     */
    private void cancel() {
        if (customerId == 0) {
            navigator.navigateTo(View.CUSTOMERS);
        } else {
            navigator.navigateWithParameter(View.CUSTOMER_DETAIL, customerId);
        }
    }

    /**
     * Shows a validation message below the form.
     *
     * @param message message in Spanish, as thrown by the Service
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /** Hides the message below the form (called before each save attempt). */
    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Creates a text field with the shared input style and a maximum length.
     *
     * @param prompt    placeholder text (Spanish)
     * @param maxLength maximum number of characters, equal to the database column length
     * @return the configured text field
     */
    private TextField textField(String prompt, int maxLength) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("input");
        limitLength(field, maxLength);
        return field;
    }

    /**
     * Prevents typing more characters than the database column accepts.
     *
     * @param control   text control to limit
     * @param maxLength maximum number of characters
     */
    private static void limitLength(TextInputControl control, int maxLength) {
        control.setTextFormatter(new TextFormatter<String>(
                change -> change.getControlNewText().length() <= maxLength ? change : null));
    }

    /**
     * Creates a two-column grid (50% each) for one section of the form.
     *
     * @return an empty grid with the form-grid style
     */
    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("customer-form-grid");
        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(50);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(50);
        grid.getColumnConstraints().addAll(left, right);
        return grid;
    }

    /**
     * Wraps an input with its label, adding the red asterisk when it is required.
     *
     * @param labelText label shown above the input (Spanish)
     * @param required  {@code true} to show the required-field asterisk
     * @param input     input control to wrap
     * @return the label and input stacked in a box
     */
    private static VBox field(String labelText, boolean required, Region input) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        HBox top = new HBox(2, label);
        if (required) {
            Label star = new Label("*");
            star.getStyleClass().add("label-required");
            top.getChildren().add(star);
        }
        input.setMaxWidth(Double.MAX_VALUE);
        VBox box = new VBox(top, input);
        box.getStyleClass().add("customer-field");
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /**
     * Wraps a block of fields under a section title.
     *
     * @param title section title (Spanish)
     * @param body  content of the section
     * @return the title and the content stacked in a box
     */
    private static VBox section(String title, Node body) {
        Label heading = new Label(title);
        heading.getStyleClass().add("customer-section-title");
        VBox section = new VBox(heading, body);
        section.getStyleClass().add("customer-section");
        return section;
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