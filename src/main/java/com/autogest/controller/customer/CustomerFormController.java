package com.autogest.controller.customer;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ParameterizedController;
import com.autogest.controller.View;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.InputFilters;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PhoneField;
import com.autogest.exception.AutoGestException;
import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.DocumentType;
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
 * Controlador del formulario de clientes (maqueta de Nuevo cliente), compartido por las
 * vistas {@code CUSTOMER_NEW} y {@code CUSTOMER_EDIT}. Cuando la pantalla recibe
 * un identificador de cliente ({@link ParameterizedController}), carga ese cliente y
 * permite editarlo; de lo contrario, registra uno nuevo.
 *
 * <p>Solo lee los campos, llama a {@link CustomerService} y muestra el resultado:
 * la validación se realiza en el servicio y su mensaje en español se muestra debajo del
 * formulario. Lo único que controla la pantalla es la longitud máxima de cada campo,
 * que coincide con la longitud de la columna correspondiente en la base de datos.</p>
 *
 * <p>Principios: controlador GRASP, bajo acoplamiento, alta cohesión y SRP.</p>
 */
public class CustomerFormController implements Initializable, NavigatorAware, ParameterizedController {

    private static final String NEW_SUBTITLE =
            "Registra la información del cliente para comenzar a gestionar sus vehículos y servicios.";
    private static final String EDIT_SUBTITLE = "Actualiza la información del cliente.";

    /** Contenedor raíz de la pantalla; el controlador construye en él el encabezado y la tarjeta del formulario. */
    @FXML
    private VBox root;

    private ViewNavigator navigator;

    /** Identificador del cliente que se está editando, o 0 cuando el formulario registra uno nuevo. */
    private int customerId;

    private ComboBox<DocumentType> documentType;
    private TextField documentNumber;
    private TextField firstName;
    private TextField lastName;
    private PhoneField phone;
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
     * <p>Construye la pantalla en modo «nuevo cliente». Si el navegador proporciona
     * posteriormente un identificador, {@link #receiveParameter(int)} cambia la pantalla al modo de edición.</p>
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createFields();
        root.getChildren().addAll(buildHeader(false), buildFormCard());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Cambia la pantalla al modo de edición: carga el cliente y completa el formulario.
     * Si el cliente ya no existe, avisa al usuario y vuelve a la lista.</p>
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
     * Construye el encabezado de la página con su ruta de navegación.
     *
     * @param editing {@code true} para el encabezado «Editar cliente» y {@code false} para «Nuevo cliente»
     * @return el componente del encabezado
     */
    private PageHeader buildHeader(boolean editing) {
        String title = editing ? "Editar cliente" : "Nuevo cliente";
        PageHeader header = new PageHeader(title, editing ? EDIT_SUBTITLE : NEW_SUBTITLE);
        header.setBreadcrumbs(List.of("Clientes", title));
        return header;
    }

    /**
     * Crea todos los controles de entrada del formulario con su texto de ayuda, clase de estilo y
     * longitud máxima. Las longitudes máximas coinciden con las columnas de la tabla {@code customers}.
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
        // Solo letras en nombres y apellidos; teléfono con su país (Líder, validaciones).
        InputFilters.lettersOnly(firstName, 100);
        InputFilters.lettersOnly(lastName, 100);
        phone = new PhoneField();
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
     * Organiza la tarjeta del formulario: las tres secciones de la maqueta, la etiqueta de error
     * y la fila con la casilla de promociones y los botones Cancelar / Guardar.
     *
     * @return la tarjeta que contiene todo el formulario
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
     * Completa el formulario con los datos de un cliente existente. Las columnas opcionales pueden ser
     * {@code null} en la base de datos, por lo que se procesan mediante {@link #orEmpty(String)}.
     *
     * @param customer cliente cuyos datos se muestran
     */
    private void fill(Customer customer) {
        documentType.setValue(customer.getDocumentType());
        documentNumber.setText(customer.getDocumentNumber());
        firstName.setText(customer.getFirstName());
        lastName.setText(customer.getLastName());
        phone.setPhone(customer.getPhone());
        email.setText(customer.getEmail());
        address.setText(orEmpty(customer.getAddress()));
        city.getEditor().setText(orEmpty(customer.getCity()));
        notes.setText(orEmpty(customer.getNotes()));
        promotions.setSelected(customer.isAcceptsPromotions());
    }

    /**
     * Devuelve el texto sin cambios o una cadena vacía cuando es {@code null}. Se usa
     * antes de mostrar en los campos del formulario los valores opcionales de la base de datos.
     *
     * @param value texto que puede ser {@code null}
     * @return el mismo texto o {@code ""} si era {@code null}
     */
    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Construye un {@link Customer} con los datos escritos en el formulario. No realiza validaciones:
     * esa es responsabilidad de {@link CustomerService}.
     *
     * @return cliente con los valores del formulario (el id es 0 al registrar uno nuevo)
     */
    private Customer readForm() {
        Customer customer = new Customer();
        customer.setId(customerId);
        customer.setDocumentType(documentType.getValue());
        customer.setDocumentNumber(documentNumber.getText());
        customer.setFirstName(firstName.getText());
        customer.setLastName(lastName.getText());
        customer.setPhone(phone.getPhone());
        customer.setEmail(email.getText());
        customer.setAddress(address.getText());
        customer.setCity(city.getEditor().getText());
        customer.setNotes(notes.getText());
        customer.setAcceptsPromotions(promotions.isSelected());
        return customer;
    }

    /**
     * Guarda el cliente. Si la operación tiene éxito, vuelve a la lista (cliente nuevo) o a
     * la pantalla de detalles (cliente editado). Una {@link ValidationException} se muestra
     * debajo del formulario; cualquier otro error se muestra en un cuadro de diálogo.
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
     * Sale del formulario sin guardar: vuelve a la lista (cliente nuevo) o a la
     * pantalla de detalles (cliente editado).
     */
    private void cancel() {
        if (customerId == 0) {
            navigator.navigateTo(View.CUSTOMERS);
        } else {
            navigator.navigateWithParameter(View.CUSTOMER_DETAIL, customerId);
        }
    }

    /**
     * Muestra un mensaje de validación debajo del formulario.
     *
     * @param message mensaje en español generado por el servicio
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /** Oculta el mensaje debajo del formulario (se llama antes de cada intento de guardado). */
    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    /**
     * Crea un campo de texto con el estilo de entrada compartido y una longitud máxima.
     *
     * @param prompt    texto de marcador de posición (en español)
     * @param maxLength número máximo de caracteres, igual a la longitud de la columna de la base de datos
     * @return el campo de texto configurado
     */
    private TextField textField(String prompt, int maxLength) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("input");
        limitLength(field, maxLength);
        return field;
    }

    /**
     * Impide escribir más caracteres de los que acepta la columna de la base de datos.
     *
     * @param control   control de texto que se limitará
     * @param maxLength número máximo de caracteres
     */
    private static void limitLength(TextInputControl control, int maxLength) {
        control.setTextFormatter(new TextFormatter<String>(
                change -> change.getControlNewText().length() <= maxLength ? change : null));
    }

    /**
     * Crea una cuadrícula de dos columnas (50 % cada una) para una sección del formulario.
     *
     * @return una cuadrícula vacía con el estilo de cuadrícula del formulario
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
     * Agrupa una entrada con su etiqueta y agrega un asterisco rojo cuando es obligatoria.
     *
     * @param labelText etiqueta que se muestra encima de la entrada (en español)
     * @param required  {@code true} para mostrar el asterisco de campo obligatorio
     * @param input     control de entrada que se agrupará
     * @return la etiqueta y la entrada apiladas en un contenedor
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
     * Agrupa un conjunto de campos debajo del título de una sección.
     *
     * @param title título de la sección (en español)
     * @param body  contenido de la sección
     * @return el título y el contenido apilados en un contenedor
     */
    private static VBox section(String title, Node body) {
        Label heading = new Label(title);
        heading.getStyleClass().add("customer-section-title");
        VBox section = new VBox(heading, body);
        section.getStyleClass().add("customer-section");
        return section;
    }

    /**
     * Obtiene el servicio de clientes cuando se utiliza, no cuando se construye la pantalla.
     *
     * @return el servicio de clientes compartido
     */
    private CustomerService service() {
        return AppContext.getInstance().getCustomerService();
    }
}