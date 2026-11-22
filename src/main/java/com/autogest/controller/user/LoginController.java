package com.autogest.controller.user;

import com.autogest.config.AppContext;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.CarTurntable;
import com.autogest.exception.AutoGestException;
import com.autogest.util.Background;
import com.autogest.util.LoginAppearanceStore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

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

    @FXML
    private HBox emailBox;

    @FXML
    private HBox passwordBox;

    @FXML
    private StackPane carStage;

    private Runnable onLoginSuccess;
    /** Evita lanzar dos inicios de sesión a la vez (clic y Enter). */
    private boolean loggingIn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Íconos dentro de los campos, como en el mockup Login V2.
        emailBox.getChildren().add(0, IconSet.icon(IconSet.MAIL));
        passwordBox.getChildren().add(0, IconSet.icon(IconSet.LOCK));
        // Vehículo 3D sobre una plataforma giratoria, con la personalización guardada en este equipo.
        carStage.getChildren().add(new CarTurntable(460, 400, new LoginAppearanceStore().load()));
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

    /**
     * Intenta iniciar sesión con los datos del formulario. La consulta y la
     * verificación de la contraseña corren en segundo plano para que la
     * ventana (y la vitrina 3D) no se congelen mientras tanto.
     */
    @FXML
    private void handleLogin() {
        if (loggingIn) {
            return;
        }
        loggingIn = true;
        hideError();
        loginButton.setDisable(true);
        String buttonText = loginButton.getText();
        loginButton.setText("Ingresando...");
        String email = emailField.getText();
        String password = passwordField.getText();
        Background.run(
                () -> AppContext.getInstance().getAuthenticationService().login(email, password),
                user -> {
                    finishLogin(buttonText);
                    if (onLoginSuccess != null) {
                        onLoginSuccess.run();
                    }
                },
                error -> {
                    finishLogin(buttonText);
                    showError(error instanceof AutoGestException ? error.getMessage() : null);
                    passwordField.clear();
                    passwordField.requestFocus();
                });
    }

    /** Devuelve el botón de ingreso a su estado normal. */
    private void finishLogin(String buttonText) {
        loggingIn = false;
        loginButton.setText(buttonText);
        loginButton.setDisable(false);
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
