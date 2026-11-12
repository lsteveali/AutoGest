package com.autogest.controller.user;

import com.autogest.config.AppContext;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.service.UserService;
import com.autogest.util.Background;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

/**
 * Formulario «Nuevo usuario» / «Editar usuario» de Usuarios y roles. El
 * diálogo no se cierra si el Service rechaza los datos: el error se muestra
 * junto a los botones y lo escrito se conserva. El guardado va en segundo
 * plano (el hash de la clave inicial congelaba la ventana).
 *
 * <p>Devuelve el usuario guardado, o vacío si se canceló.</p>
 *
 * <p>Principles: GRASP Controller, SRP (el formulario sale de
 * {@code UsersRolesController}), DIP.</p>
 */
public class UserFormDialog extends Dialog<User> {

    private static final String STYLESHEET = "/css/autogest.css";

    private final User existing;
    private final TextField firstName;
    private final TextField lastName;
    private final TextField email;
    private final ComboBox<Role> roleBox;
    private final TextField specialty;
    private final PasswordField password = password("Mín. 8, con letras y números");
    private final PasswordField confirm = password("Repite la clave inicial");
    private final Label error = new Label();

    /**
     * @param existing usuario a editar, o {@code null} para crear uno nuevo
     */
    public UserFormDialog(User existing) {
        this.existing = existing;
        boolean editing = existing != null;
        setTitle(editing ? "Editar usuario" : "Nuevo usuario");

        firstName = field(editing ? existing.getFirstName() : "", "Ej.: Ana María");
        lastName = field(editing ? existing.getLastName() : "", "Ej.: López Pérez");
        email = field(editing ? existing.getEmail() : "", "nombre@autogest.com");
        specialty = field(editing && existing.getSpecialty() != null ? existing.getSpecialty() : "",
                "Ej.: Motor, frenos, eléctrico");
        roleBox = roleCombo();
        if (editing) {
            roleBox.setValue(existing.getRole());
        }

        DialogPane pane = getDialogPane();
        pane.getStylesheets().add(getClass().getResource(STYLESHEET).toExternalForm());
        pane.getStyleClass().add("app-dialog");
        pane.setPrefWidth(560);
        pane.setContent(buildContent(editing));

        ButtonType saveType = new ButtonType(editing ? "Guardar cambios" : "Crear usuario",
                ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(ButtonType.CANCEL, saveType);
        Button save = (Button) pane.lookupButton(saveType);
        save.getStyleClass().add("btn-primary");
        Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
        cancel.setText("Cancelar");
        cancel.getStyleClass().add("btn-secondary");
        // Cancelar a la izquierda y la acción principal a la derecha, en cualquier sistema.
        ButtonBar bar = (ButtonBar) pane.lookup(".button-bar");
        if (bar != null) {
            bar.setButtonOrder(ButtonBar.BUTTON_ORDER_MAC_OS);
        }
        // Se intercepta «Guardar»: el diálogo solo se cierra si el Service acepta.
        save.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            submit(save, cancel);
        });

        setResultConverter(button -> null);
        setOnShown(event -> firstName.requestFocus());
    }

    private VBox buildContent(boolean editing) {
        Label title = new Label(editing ? "Editar usuario" : "Nuevo usuario");
        title.getStyleClass().add("dialog-title");
        Label subtitle = new Label(editing
                ? "Cambia los datos y el rol. La contraseña la cambia cada usuario en «Perfil y cuenta»."
                : "La persona podrá entrar con su correo y la clave inicial, y cambiarla luego en «Perfil y cuenta».");
        subtitle.getStyleClass().add("dialog-subtitle");
        subtitle.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(50);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        grid.add(labeled("Nombres *", firstName), 0, 0);
        grid.add(labeled("Apellidos *", lastName), 1, 0);
        grid.add(labeled("Correo electrónico *", email), 0, 1, 2, 1);
        grid.add(labeled("Rol *", roleBox), 0, 2);
        VBox specialtyBox = labeled("Especialidad", specialty);
        specialtyBox.visibleProperty().bind(roleBox.valueProperty().isEqualTo(Role.MECHANIC));
        grid.add(specialtyBox, 1, 2);
        if (!editing) {
            grid.add(labeled("Clave inicial *", password), 0, 3);
            grid.add(labeled("Confirmar clave *", confirm), 1, 3);
        }

        error.getStyleClass().addAll("form-message", "form-message-error");
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox content = new VBox(16, new VBox(4, title, subtitle), grid, error);
        content.getStyleClass().add("dialog-content");
        return content;
    }

    private void submit(Button save, Button cancel) {
        boolean editing = existing != null;
        if (!editing && !password.getText().equals(confirm.getText())) {
            showError("La confirmación no coincide con la clave inicial.");
            confirm.requestFocus();
            return;
        }
        User user = new User();
        if (editing) {
            user.setId(existing.getId());
        }
        user.setFirstName(text(firstName));
        user.setLastName(text(lastName));
        user.setEmail(text(email));
        user.setRole(roleBox.getValue());
        user.setSpecialty(roleBox.getValue() == Role.MECHANIC ? text(specialty) : null);
        String initialPassword = password.getText();

        save.setDisable(true);
        cancel.setDisable(true);
        hideError();
        Background.run(() -> editing ? service().updateUser(user) : service().createUser(user, initialPassword),
                saved -> {
                    setResult(saved);
                    close();
                },
                failure -> {
                    save.setDisable(false);
                    cancel.setDisable(false);
                    showError(failure instanceof AutoGestException && failure.getMessage() != null
                            ? failure.getMessage()
                            : "No se pudo guardar. Revisa la conexión e inténtalo de nuevo.");
                });
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
        error.setManaged(true);
        getDialogPane().getScene().getWindow().sizeToScene();
    }

    private void hideError() {
        error.setVisible(false);
        error.setManaged(false);
    }

    // ---------- ayudas ----------

    private static ComboBox<Role> roleCombo() {
        ComboBox<Role> box = new ComboBox<>();
        box.getItems().addAll(Role.values());
        box.getStyleClass().add("input");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPromptText("Selecciona un rol");
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(Role role) {
                return role == null ? "" : UserLabels.roleText(role);
            }

            @Override
            public Role fromString(String text) {
                return UserLabels.roleFromText(text);
            }
        });
        return box;
    }

    private static TextField field(String value, String prompt) {
        TextField field = new TextField(value == null ? "" : value);
        field.setPromptText(prompt);
        field.getStyleClass().add("input");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static PasswordField password(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.getStyleClass().addAll("input", "password-field");
        field.setMaxWidth(Double.MAX_VALUE);
        return field;
    }

    private static VBox labeled(String text, Node field) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        return new VBox(6, label, field);
    }

    private static String text(TextField field) {
        return field.getText() == null ? null : field.getText().trim();
    }

    private static UserService service() {
        return AppContext.getInstance().getUserService();
    }
}
