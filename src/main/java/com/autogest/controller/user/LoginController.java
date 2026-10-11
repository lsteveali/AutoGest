package com.autogest.controller.user;

import com.autogest.config.AppContext;
import com.autogest.exception.AutoGestException;
import com.autogest.model.User;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de inicio de sesión (decisión D-01: solo correo,
 * contraseña, «Recordarme» y el enlace deshabilitado «¿Olvidaste tu
 * contraseña?»). Traduce el evento del formulario en una llamada a
 * {@code AuthenticationService} y avisa al shell cuando el acceso es correcto.
 *
 * <p>No valida reglas de negocio ni conoce el Repository: las credenciales las
 * comprueba el Service y sus mensajes se muestran bajo el formulario. El
 * servicio se obtiene de forma diferida para que la pantalla pueda abrirse
 * aunque falte la configuración de la base de datos.</p>
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class LoginController implements Initializable {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberCheck;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private Runnable onLoginSuccess;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        hideError();
        loginButton.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());
        emailField.setOnAction(event -> passwordField.requestFocus());
    }

    /**
     * Registra la acción que se ejecuta tras un inicio de sesión correcto
     * (el shell de la aplicación la usa para mostrar la pantalla principal).
     *
     * @param onLoginSuccess acción a ejecutar con el usuario ya autenticado
     */
    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    /** Intenta iniciar sesión con los datos del formulario. */
    @FXML
    private void handleLogin() {
        hideError();
        loginButton.setDisable(true);
        try {
            User user = AppContext.getInstance().getAuthenticationService()
                    .login(emailField.getText(), passwordField.getText());
            if (onLoginSuccess != null) {
                onLoginSuccess.run();
            }
        } catch (AutoGestException e) {
            showError(e.getMessage());
            passwordField.clear();
            passwordField.requestFocus();
        } finally {
            loginButton.setDisable(false);
        }
    }

    /** Muestra un mensaje de error bajo el formulario. */
    private void showError(String message) {
        errorLabel.setText(message == null ? "No se pudo iniciar sesión." : message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /** Oculta el mensaje de error del formulario. */
    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
