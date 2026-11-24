package com.autogest.controller.components;

import com.autogest.util.ThemeManager;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.net.URL;

/**
 * Avisos de AutoGest (errores, advertencias, información, éxito y
 * confirmaciones) con el diseño de la aplicación: una tarjeta con el escudo
 * del logo, el ícono del tipo de aviso, el título, el mensaje y los botones de
 * la paleta. No usa la barra de título de Windows y sigue el tema claro u
 * oscuro ({@link ThemeManager}).
 *
 * <p>Enter acepta, Escape cancela o cierra, y la tarjeta se puede arrastrar.
 * {@link ErrorDialog} y {@link ConfirmDialog} la usan por dentro.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion, DRY.</p>
 */
public final class AppDialog {

    private static final String STYLESHEET = "/css/autogest.css";
    private static final String LOGO = "/images/logo-shield-64.png";

    /** Tipo de aviso: decide el ícono y su color. */
    public enum Kind {
        /** Algo falló. */
        ERROR(IconSet.ALERT, "app-alert-error"),
        /** Algo requiere atención. */
        WARNING(IconSet.ALERT, "app-alert-warning"),
        /** Información general. */
        INFO(IconSet.INFO, "app-alert-info"),
        /** Algo se hizo bien. */
        SUCCESS(IconSet.CHECK, "app-alert-success"),
        /** Pregunta antes de seguir. */
        QUESTION(IconSet.INFO, "app-alert-question");

        private final String icon;
        private final String styleClass;

        Kind(String icon, String styleClass) {
            this.icon = icon;
            this.styleClass = styleClass;
        }
    }

    private AppDialog() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Muestra un error y espera a que el usuario lo cierre.
     *
     * @param title   título del aviso
     * @param message mensaje en español
     */
    public static void error(String title, String message) {
        open(Kind.ERROR, title, message, "Aceptar", null, false, true);
    }

    /**
     * Muestra una advertencia y espera a que el usuario la cierre.
     *
     * @param title   título del aviso
     * @param message mensaje en español
     */
    public static void warning(String title, String message) {
        open(Kind.WARNING, title, message, "Entendido", null, false, true);
    }

    /**
     * Muestra información y espera a que el usuario la cierre.
     *
     * @param title   título del aviso
     * @param message mensaje en español
     */
    public static void info(String title, String message) {
        open(Kind.INFO, title, message, "Aceptar", null, false, true);
    }

    /**
     * Muestra que algo salió bien y espera a que el usuario lo cierre.
     *
     * @param title   título del aviso
     * @param message mensaje en español
     */
    public static void success(String title, String message) {
        open(Kind.SUCCESS, title, message, "Aceptar", null, false, true);
    }

    /**
     * Muestra un aviso sin esperar respuesta (sirve desde temporizadores y
     * animaciones, donde no se puede bloquear).
     *
     * @param kind    tipo de aviso
     * @param title   título del aviso
     * @param message mensaje en español
     */
    public static void notify(Kind kind, String title, String message) {
        open(kind, title, message, "Aceptar", null, false, false);
    }

    /**
     * Pregunta antes de seguir.
     *
     * @param title      título del aviso
     * @param message    mensaje en español
     * @param acceptText texto del botón que confirma (por ejemplo «Aceptar»)
     * @param danger     {@code true} si la acción borra o no se puede deshacer (botón rojo)
     * @return {@code true} si el usuario confirma
     */
    public static boolean confirm(String title, String message, String acceptText, boolean danger) {
        return open(Kind.QUESTION, title, message, acceptText, "Cancelar", danger, true);
    }

    private static boolean open(Kind kind, String title, String message, String acceptText, String cancelText,
                                boolean danger, boolean wait) {
        boolean[] accepted = {false};
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.setTitle(title == null ? "AutoGest" : title);
        Window owner = activeWindow();
        if (owner != null) {
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            if (owner instanceof Stage ownerStage) {
                stage.getIcons().setAll(ownerStage.getIcons());
            }
        } else {
            stage.initModality(Modality.APPLICATION_MODAL);
        }

        // Encabezado: escudo y nombre, como en el resto de la aplicación.
        HBox brand = new HBox(8);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("app-alert-brand-row");
        URL logo = AppDialog.class.getResource(LOGO);
        if (logo != null) {
            ImageView shield = new ImageView(new Image(logo.toExternalForm()));
            shield.setFitWidth(22);
            shield.setPreserveRatio(true);
            shield.setSmooth(true);
            brand.getChildren().add(shield);
        }
        Label brandName = new Label("AutoGest");
        brandName.getStyleClass().add("app-alert-brand");
        brand.getChildren().add(brandName);

        // Cuerpo: ícono del tipo de aviso, título y mensaje.
        StackPane badge = new StackPane(IconSet.icon(kind.icon));
        badge.getStyleClass().addAll("app-alert-icon", kind.styleClass);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("app-alert-title");
        titleLabel.setWrapText(true);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("app-alert-message");
        messageLabel.setWrapText(true);
        VBox texts = new VBox(6, titleLabel, messageLabel);
        HBox.setHgrow(texts, Priority.ALWAYS);
        HBox body = new HBox(14, badge, texts);
        body.setAlignment(Pos.TOP_LEFT);

        // Botones: el principal a la derecha; Enter lo pulsa y Escape cancela.
        Button accept = new Button(acceptText);
        accept.getStyleClass().add("app-alert-primary");
        if (danger) {
            accept.getStyleClass().add("app-alert-danger");
        }
        accept.setDefaultButton(true);
        accept.setOnAction(event -> {
            accepted[0] = true;
            stage.close();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(10, spacer);
        actions.setAlignment(Pos.CENTER_RIGHT);
        if (cancelText != null) {
            Button cancel = new Button(cancelText);
            cancel.getStyleClass().add("app-alert-secondary");
            cancel.setCancelButton(true);
            cancel.setOnAction(event -> stage.close());
            actions.getChildren().add(cancel);
        }
        actions.getChildren().add(accept);

        VBox card = new VBox(16, brand, body, actions);
        card.getStyleClass().add("app-alert");
        StackPane root = new StackPane(card);
        root.getStyleClass().add("app-alert-root");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        URL css = AppDialog.class.getResource(STYLESHEET);
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        ThemeManager.applyTo(root);
        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });
        stage.setScene(scene);

        // La tarjeta se puede arrastrar (no hay barra de título).
        double[] drag = new double[2];
        card.setOnMousePressed(event -> {
            drag[0] = stage.getX() - event.getScreenX();
            drag[1] = stage.getY() - event.getScreenY();
        });
        card.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() + drag[0]);
            stage.setY(event.getScreenY() + drag[1]);
        });

        stage.setOnShown(event -> {
            if (owner != null) {
                stage.setX(owner.getX() + (owner.getWidth() - stage.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - stage.getHeight()) / 2.4);
            }
            appear(card);
            accept.requestFocus();
        });

        if (wait) {
            stage.showAndWait();
        } else {
            stage.show();
        }
        return accepted[0];
    }

    /** Entrada breve: la tarjeta aparece y crece un poco, sin rebote. */
    private static void appear(Region card) {
        card.setOpacity(0);
        card.setScaleX(0.96);
        card.setScaleY(0.96);
        FadeTransition fade = new FadeTransition(Duration.millis(160), card);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(Duration.millis(160), card);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);
        new ParallelTransition(fade, scale).play();
    }

    /** Ventana con el foco (o la primera visible), para centrar el aviso sobre ella. */
    private static Window activeWindow() {
        Window focused = null;
        for (Window window : Window.getWindows()) {
            if (window.isShowing() && window.isFocused() && window instanceof Stage) {
                return window;
            }
            if (focused == null && window.isShowing() && window instanceof Stage) {
                focused = window;
            }
        }
        return focused;
    }
}
