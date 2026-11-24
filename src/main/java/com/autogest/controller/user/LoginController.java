package com.autogest.controller.user;

import com.autogest.config.AppContext;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.CarTurntable;
import com.autogest.exception.AutoGestException;
import com.autogest.util.AppTheme;
import com.autogest.util.Background;
import com.autogest.util.LoginAppearanceStore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controlador de la pantalla de inicio de sesiÃ³n (decisiÃ³n D-01: solo correo,
 * contraseÃ±a, Â«RecordarmeÂ» y el enlace deshabilitado Â«Â¿Olvidaste tu
 * contraseÃ±a?Â»). Traduce el evento del formulario en una llamada a
 * {@code AuthenticationService} y avisa al shell cuando el acceso es correcto.
 *
 * <p>No valida reglas de negocio ni conoce el Repository: las credenciales las
 * comprueba el Service y sus mensajes se muestran bajo el formulario. El
 * servicio se obtiene de forma diferida para que la pantalla pueda abrirse
 * aunque falte la configuraciÃ³n de la base de datos.</p>
 *
 * <p>Principles: GRASP Controller, Indirection, SRP, DIP, Low Coupling,
 * High Cohesion.</p>
 */
public class LoginController implements Initializable {

    /** Logo con Â«AutoGestÂ» en negro, para el taller cÃ¡lido. */
    private static final String LIGHT_LOGO = "/images/logo-full-light.png";
    /** Logo con Â«AutoGestÂ» en blanco, para el taller nocturno. */
    private static final String DARK_LOGO = "/images/logo-full-white.png";

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

    @FXML
    private GridPane loginRoot;

    @FXML
    private ImageView logo;

    private Runnable onLoginSuccess;
    /** Evita lanzar dos inicios de sesiÃ³n a la vez (clic y Enter). */
    private boolean loggingIn;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Ãconos dentro de los campos, como en el mockup Login V2.
        emailBox.getChildren().add(0, IconSet.icon(IconSet.MAIL));
        passwordBox.getChildren().add(0, IconSet.icon(IconSet.LOCK));
        // VehÃ­culo 3D sobre una plataforma giratoria y tema (fondo del taller y
        // formulario), con la personalizaciÃ³n guardada en este equipo.
        LoginAppearanceStore store = new LoginAppearanceStore();
        applyTheme(store.theme());
        carStage.getChildren().add(new CarTurntable(460, 400, store.load()));
        hideError();
        loginButton.setOnAction(event -> handleLogin());
        passwordField.setOnAction(event -> handleLogin());
        emailField.setOnAction(event -> passwordField.requestFocus());
    }

    /**
     * Pone el tema en la raÃ­z (el CSS cambia el fondo del taller y el
     * formulario) y el logo que se lee sobre ese fondo.
     *
     * @param theme tema del equipo
     */
    public void applyTheme(AppTheme theme) {
        for (AppTheme each : AppTheme.values()) {
            loginRoot.getStyleClass().remove(each.styleClass());
        }
        loginRoot.getStyleClass().add(theme.styleClass());
        String image = theme == AppTheme.DARK ? DARK_LOGO : LIGHT_LOGO;
        URL url = LoginController.class.getResource(image);
        if (url != null) {
            logo.setImage(new Image(url.toExternalForm()));
        }
    }

    /**
     * Registra la acciÃ³n que se ejecuta tras un inicio de sesiÃ³n correcto
     * (el shell de la aplicaciÃ³n la usa para mostrar la pantalla principal).
     *
     * @param onLoginSuccess acciÃ³n a ejecutar con el usuario ya autenticado
     */
    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    /**
     * Intenta iniciar sesiÃ³n con los datos del formulario. La consulta y la
     * verificaciÃ³n de la contraseÃ±a corren en segundo plano para que la
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

    /** Devuelve el botÃ³n de ingreso a su estado normal. */
    private void finishLogin(String buttonText) {
        loggingIn = false;
        loginButton.setText(buttonText);
        loginButton.setDisable(false);
    }

    /** Muestra un mensaje de error bajo el formulario. */
    private void showError(String message) {
        errorLabel.setText(message == null ? "No se pudo iniciar sesiÃ³n." : message);
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
