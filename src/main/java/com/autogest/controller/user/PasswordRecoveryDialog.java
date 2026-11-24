package com.autogest.controller.user;

import com.autogest.config.AppContext;
import com.autogest.controller.components.IconSet;
import com.autogest.exception.AutoGestException;
import com.autogest.service.PasswordRecoveryService;
import com.autogest.service.UserService;
import com.autogest.util.AppTheme;
import com.autogest.util.Background;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Ventana de «¿Olvidaste tu contraseña?», en dos pasos y con el mismo diseño
 * del inicio de sesión (respeta el tema claro u oscuro):
 * <ol>
 *   <li>Escribir el correo y pedir el código.</li>
 *   <li>Escribir el código de 6 dígitos y la contraseña nueva dos veces.</li>
 * </ol>
 * Las reglas viven en {@link PasswordRecoveryService}; las consultas y el
 * envío del correo corren en segundo plano.
 *
 * <p>Principles: GRASP Controller, SRP.</p>
 */
public final class PasswordRecoveryDialog {

    private final Stage stage = new Stage();
    private final VBox card = new VBox();
    private final TextField emailField = new TextField();
    private final TextField codeField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final Label error = new Label();
    private String email;
    private boolean changed;

    private PasswordRecoveryDialog(Window owner, AppTheme theme, String initialEmail) {
        card.getStyleClass().add("login-card");
        card.setMaxWidth(440);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        error.getStyleClass().add("login-error");
        error.setWrapText(true);
        hideError();

        StackPane root = new StackPane(card);
        root.getStyleClass().addAll("recovery-root", theme.styleClass());
        Scene scene = new Scene(root, 520, 700);
        scene.getStylesheets().add(PasswordRecoveryDialog.class.getResource("/css/autogest.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Recuperar contraseña");
        stage.initModality(Modality.WINDOW_MODAL);
        if (owner != null) {
            stage.initOwner(owner);
        }
        emailField.setText(initialEmail == null ? "" : initialEmail.trim());
        showEmailStep();
    }

    /**
     * Abre la ventana y espera a que se cierre.
     *
     * @param owner        ventana del inicio de sesión
     * @param theme        tema del equipo
     * @param initialEmail correo escrito en el login, para no volver a escribirlo
     * @return el correo de la cuenta si la contraseña cambió; {@code null} si no
     */
    public static String show(Window owner, AppTheme theme, String initialEmail) {
        PasswordRecoveryDialog dialog = new PasswordRecoveryDialog(owner, theme, initialEmail);
        dialog.stage.showAndWait();
        return dialog.changed ? dialog.email : null;
    }

    // ---------- pasos ----------

    private void showEmailStep() {
        Button send = primary("Enviar código");
        send.setOnAction(event -> requestCode(send));
        emailField.setOnAction(event -> requestCode(send));
        Hyperlink back = new Hyperlink("Volver al inicio de sesión");
        back.setOnAction(event -> stage.close());

        card.getChildren().setAll(
                header("Recupera tu contraseña",
                        "Escribe el correo de tu cuenta y te enviaremos un código para crear una contraseña nueva."),
                field("Correo electrónico", IconSet.MAIL, emailField, "usuario@ejemplo.com"),
                error, send, centered(back));
        hideError();
        emailField.requestFocus();
    }

    private void showCodeStep() {
        Button change = primary("Cambiar contraseña");
        change.setOnAction(event -> resetPassword(change));
        confirmField.setOnAction(event -> resetPassword(change));
        Hyperlink resend = new Hyperlink("Reenviar código");
        resend.setOnAction(event -> requestCode(null));
        Hyperlink other = new Hyperlink("Usar otro correo");
        other.setOnAction(event -> showEmailStep());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox links = new HBox(other, spacer, resend);
        links.setAlignment(Pos.CENTER_LEFT);

        Label hint = new Label("Mínimo " + UserService.MIN_PASSWORD_LENGTH + " caracteres, con letras y números.");
        hint.getStyleClass().add("login-subtitle");
        hint.setWrapText(true);

        codeField.clear();
        passwordField.clear();
        confirmField.clear();
        card.getChildren().setAll(
                header("Revisa tu correo", "Si " + email + " es de una cuenta activa, te enviamos un código de "
                        + "6 dígitos. Vence en 15 minutos."),
                field("Código", IconSet.SHIELD, codeField, "000000"),
                field("Nueva contraseña", IconSet.LOCK, passwordField, "Tu nueva contraseña"),
                field("Confirma la contraseña", IconSet.LOCK, confirmField, "Repite la contraseña"),
                hint, error, change, links);
        hideError();
        codeField.requestFocus();
    }

    // ---------- acciones ----------

    private void requestCode(Button button) {
        String typed = emailField.getText();
        busy(button, true);
        Background.run(() -> {
                    service().requestCode(typed);
                    return Boolean.TRUE;
                },
                ok -> {
                    busy(button, false);
                    email = typed.trim().toLowerCase();
                    showCodeStep();
                },
                failure -> {
                    busy(button, false);
                    showError(failure);
                });
    }

    private void resetPassword(Button button) {
        String code = codeField.getText();
        String password = passwordField.getText();
        String confirmation = confirmField.getText();
        busy(button, true);
        Background.run(() -> {
                    service().resetPassword(email, code, password, confirmation);
                    return Boolean.TRUE;
                },
                ok -> {
                    changed = true;
                    stage.close();
                },
                failure -> {
                    busy(button, false);
                    showError(failure);
                });
    }

    // ---------- piezas ----------

    private static VBox header(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("login-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("login-subtitle");
        subtitleLabel.setWrapText(true);
        // El subtítulo baja a varias líneas en vez de cortarse con «…».
        subtitleLabel.setMinHeight(Region.USE_PREF_SIZE);
        VBox box = new VBox(titleLabel, subtitleLabel);
        box.getStyleClass().add("login-card-header");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static VBox field(String label, String icon, TextInputControl input, String prompt) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("login-field-label");
        input.setPromptText(prompt);
        if (!input.getStyleClass().contains("input-bare")) {
            input.getStyleClass().add("input-bare");
        }
        HBox.setHgrow(input, Priority.ALWAYS);
        HBox group = new HBox(IconSet.icon(icon), input);
        group.getStyleClass().add("input-group");
        group.setAlignment(Pos.CENTER_LEFT);
        labelNode.setLabelFor(input);
        return new VBox(8, labelNode, group);
    }

    private static Button primary(String text) {
        Button button = new Button(text);
        button.getStyleClass().addAll("btn-primary", "login-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setDefaultButton(true);
        return button;
    }

    private static Node centered(Node node) {
        HBox box = new HBox(node);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void busy(Button button, boolean value) {
        card.setDisable(value);
        if (button == null) {
            return;
        }
        if (value) {
            button.setUserData(button.getText());
            button.setText("Un momento…");
        } else if (button.getUserData() instanceof String text) {
            button.setText(text);
        }
    }

    private void showError(RuntimeException failure) {
        error.setText(failure instanceof AutoGestException && failure.getMessage() != null
                ? failure.getMessage() : "No se pudo completar. Revisa la conexión e inténtalo de nuevo.");
        error.setVisible(true);
        error.setManaged(true);
    }

    private void hideError() {
        error.setText("");
        error.setVisible(false);
        error.setManaged(false);
    }

    private static PasswordRecoveryService service() {
        return AppContext.getInstance().getPasswordRecoveryService();
    }
}
