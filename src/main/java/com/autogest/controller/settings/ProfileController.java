package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.user.UserLabels;
import com.autogest.exception.AutoGestException;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.User;
import com.autogest.service.UserService;
import com.autogest.util.Background;
import com.autogest.util.Session;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Pantalla «Perfil y cuenta»: el usuario en sesión edita sus nombres y su
 * correo, y cambia su contraseña. La abre cualquier rol (menú del avatar) y el
 * administrador también desde Configuración. Las reglas viven en
 * {@code UserService}; aquí solo se leen los campos y se muestra el resultado.
 *
 * <p>Las llamadas al Service van en segundo plano: el hash de la contraseña
 * (PBKDF2, D-07) tarda lo suficiente para congelar la ventana.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class ProfileController implements Initializable {

    private static final int COLUMNS = 2;

    @FXML
    private VBox root;

    private TextField firstNameField;
    private TextField lastNameField;
    private TextField emailField;
    private Label profileMessage;
    private Button saveProfileButton;

    private PasswordField currentPasswordField;
    private PasswordField newPasswordField;
    private PasswordField confirmPasswordField;
    private Label passwordMessage;
    private Button changePasswordButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User user = Session.getCurrentUser();
        PageHeader header = new PageHeader("Perfil y cuenta",
                "Tu información personal y tu contraseña de acceso a AutoGest.");
        boolean admin = user != null && user.hasRole(Role.ADMINISTRATOR);
        header.setBreadcrumbs(List.of(admin ? "Configuración" : "Inicio", "Perfil y cuenta"));

        root.getChildren().addAll(header, buildProfileCard(user), buildPasswordCard());
    }

    // ---------- información personal ----------

    private VBox buildProfileCard(User user) {
        firstNameField = field(user == null ? "" : user.getFirstName());
        lastNameField = field(user == null ? "" : user.getLastName());
        emailField = field(user == null ? "" : user.getEmail());
        TextField usernameField = field(user == null ? "" : user.getUsername());
        usernameField.setEditable(false);
        usernameField.getStyleClass().add("input-readonly");
        TextField roleField = field(user == null ? "" : UserLabels.roleText(user.getRole()));
        roleField.setEditable(false);
        roleField.getStyleClass().add("input-readonly");

        GridPane grid = grid();
        grid.add(labeled("Nombres *", firstNameField), 0, 0);
        grid.add(labeled("Apellidos *", lastNameField), 1, 0);
        grid.add(labeled("Correo electrónico *", emailField), 0, 1);
        grid.add(labeled("Usuario", usernameField), 1, 1);
        grid.add(labeled("Rol", roleField), 0, 2);

        Label note = new Label("El usuario y el rol los cambia un administrador en Usuarios y roles.");
        note.getStyleClass().add("settings-note");
        note.setWrapText(true);

        profileMessage = message();
        saveProfileButton = new Button("Guardar cambios");
        saveProfileButton.getStyleClass().add("btn-primary");
        saveProfileButton.setOnAction(event -> saveProfile());
        saveProfileButton.setDisable(user == null);

        return card("Información personal", grid, note, actions(profileMessage, saveProfileButton));
    }

    private void saveProfile() {
        User current = Session.getCurrentUser();
        if (current == null) {
            return;
        }
        User edited = new User();
        edited.setId(current.getId());
        edited.setFirstName(text(firstNameField));
        edited.setLastName(text(lastNameField));
        edited.setEmail(text(emailField));
        edited.setRole(current.getRole());
        edited.setSpecialty(current.getSpecialty());

        busy(saveProfileButton, profileMessage, "Guardando...");
        Background.run(() -> service().updateUser(edited),
                updated -> {
                    // Se conserva lo que no se edita aquí (último acceso, etc.).
                    current.setFirstName(updated.getFirstName());
                    current.setLastName(updated.getLastName());
                    current.setEmail(updated.getEmail());
                    Session.start(current);
                    done(saveProfileButton, profileMessage, "Cambios guardados.", false);
                },
                error -> done(saveProfileButton, profileMessage, errorText(error), true));
    }

    // ---------- contraseña ----------

    private VBox buildPasswordCard() {
        currentPasswordField = password("Tu contraseña actual");
        newPasswordField = password("Mínimo 8 caracteres, con letras y números");
        confirmPasswordField = password("Repite la nueva contraseña");

        GridPane grid = grid();
        grid.add(labeled("Contraseña actual *", currentPasswordField), 0, 0);
        grid.add(labeled("Nueva contraseña *", newPasswordField), 0, 1);
        grid.add(labeled("Confirmar nueva contraseña *", confirmPasswordField), 1, 1);

        passwordMessage = message();
        changePasswordButton = new Button("Cambiar contraseña");
        changePasswordButton.getStyleClass().add("btn-primary");
        changePasswordButton.setOnAction(event -> changePassword());
        changePasswordButton.setDisable(Session.getCurrentUser() == null);
        confirmPasswordField.setOnAction(event -> changePassword());

        return card("Cambiar contraseña", grid, actions(passwordMessage, changePasswordButton));
    }

    private void changePassword() {
        User current = Session.getCurrentUser();
        if (current == null) {
            return;
        }
        String currentPassword = currentPasswordField.getText();
        String newPassword = newPasswordField.getText();
        if (!newPassword.equals(confirmPasswordField.getText())) {
            showMessage(passwordMessage, "La confirmación no coincide con la nueva contraseña.", true);
            confirmPasswordField.requestFocus();
            return;
        }

        busy(changePasswordButton, passwordMessage, "Cambiando contraseña...");
        Background.run(() -> {
                    service().changePassword(current.getId(), currentPassword, newPassword);
                    return Boolean.TRUE;
                },
                ok -> {
                    currentPasswordField.clear();
                    newPasswordField.clear();
                    confirmPasswordField.clear();
                    done(changePasswordButton, passwordMessage, "Contraseña actualizada.", false);
                },
                error -> done(changePasswordButton, passwordMessage, errorText(error), true));
    }

    // ---------- ayudas de construcción ----------

    private static VBox card(String title, Node... body) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        VBox card = new VBox(16, titleLabel);
        card.getChildren().addAll(body);
        card.getStyleClass().add("card");
        return card;
    }

    private static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        for (int i = 0; i < COLUMNS; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / COLUMNS);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    private static VBox labeled(String text, Node field) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        VBox box = new VBox(6, label, field);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private static HBox actions(Label message, Button button) {
        HBox.setHgrow(message, Priority.ALWAYS);
        message.setMaxWidth(Double.MAX_VALUE);
        HBox row = new HBox(12, message, button);
        row.setAlignment(Pos.CENTER_RIGHT);
        return row;
    }

    private static TextField field(String value) {
        TextField field = new TextField(value == null ? "" : value);
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

    private static Label message() {
        Label label = new Label();
        label.getStyleClass().add("form-message");
        label.setWrapText(true);
        return label;
    }

    private static void busy(Button button, Label message, String text) {
        button.setDisable(true);
        showMessage(message, text, false);
        message.getStyleClass().remove("form-message-success");
    }

    private static void done(Button button, Label message, String text, boolean error) {
        button.setDisable(false);
        showMessage(message, text, error);
    }

    private static void showMessage(Label label, String text, boolean error) {
        label.setText(text);
        label.getStyleClass().removeAll("form-message-error", "form-message-success");
        label.getStyleClass().add(error ? "form-message-error" : "form-message-success");
    }

    private static String errorText(RuntimeException error) {
        if (error instanceof AutoGestException && error.getMessage() != null) {
            return error.getMessage();
        }
        return "No se pudo completar la operación. Revisa la conexión e inténtalo de nuevo.";
    }

    private static String text(TextField field) {
        return field.getText() == null ? null : field.getText().trim();
    }

    private static UserService service() {
        return AppContext.getInstance().getUserService();
    }
}
