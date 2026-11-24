package com.autogest.util;

import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Tema claro u oscuro de toda la aplicación. Pone la clase {@value #DARK_CLASS}
 * en la raíz de cada ventana que se abre (la principal, los diálogos y los
 * menús) y la quita en el tema claro; los colores salen de los tokens de
 * {@code autogest.css}, que el tema oscuro redefine. Los diálogos estándar de
 * JavaFX (errores, confirmaciones) reciben también la hoja de estilos, para
 * que tomen la paleta.
 *
 * <p>Principles: Information Expert, SRP, Low Coupling.</p>
 */
public final class ThemeManager {

    /** Clase CSS del tema oscuro en la raíz de cada ventana. */
    public static final String DARK_CLASS = "app-dark";
    private static final String STYLESHEET = "/css/autogest.css";

    private static AppTheme current = AppTheme.LIGHT;
    private static boolean installed;
    private static final List<Consumer<AppTheme>> LISTENERS = new CopyOnWriteArrayList<>();

    private ThemeManager() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Empieza a aplicar el tema a todas las ventanas, las abiertas y las que
     * se abran. Se llama una vez al arrancar, en el hilo de JavaFX.
     *
     * @param initial tema guardado en este equipo
     */
    public static void install(AppTheme initial) {
        current = initial == null ? AppTheme.LIGHT : initial;
        if (installed) {
            refreshAll();
            return;
        }
        installed = true;
        Window.getWindows().addListener((ListChangeListener<Window>) change -> {
            while (change.next()) {
                change.getAddedSubList().forEach(ThemeManager::watch);
            }
        });
        Window.getWindows().forEach(ThemeManager::watch);
    }

    /** Tema actual. */
    public static AppTheme current() {
        return current;
    }

    /**
     * Cambia el tema de toda la aplicación al instante (no lo guarda).
     *
     * @param theme tema nuevo
     */
    public static void set(AppTheme theme) {
        AppTheme next = theme == null ? AppTheme.LIGHT : theme;
        if (next == current) {
            return;
        }
        current = next;
        refreshAll();
        LISTENERS.forEach(listener -> listener.accept(next));
    }

    /**
     * Avisa cada vez que cambia el tema.
     *
     * @param listener recibe el tema nuevo
     */
    public static void addListener(Consumer<AppTheme> listener) {
        LISTENERS.add(listener);
    }

    /**
     * Deja de avisar a un oyente.
     *
     * @param listener el oyente registrado
     */
    public static void removeListener(Consumer<AppTheme> listener) {
        LISTENERS.remove(listener);
    }

    /**
     * Pone o quita la clase del tema oscuro en una raíz (también sirve para
     * nodos sueltos, como las vistas previas).
     *
     * @param root raíz a la que se aplica el tema actual
     */
    public static void applyTo(Parent root) {
        if (root == null) {
            return;
        }
        root.getStyleClass().remove(DARK_CLASS);
        if (current == AppTheme.DARK) {
            root.getStyleClass().add(DARK_CLASS);
        }
    }

    private static void refreshAll() {
        for (Window window : Window.getWindows()) {
            style(window, window.getScene());
        }
    }

    /** Sigue la escena de la ventana: al iniciar sesión se cambia la escena. */
    private static void watch(Window window) {
        window.sceneProperty().addListener((observable, before, scene) -> {
            style(window, scene);
            if (scene != null) {
                scene.rootProperty().addListener((o, oldRoot, newRoot) -> style(window, scene));
            }
        });
        Scene scene = window.getScene();
        if (scene != null) {
            scene.rootProperty().addListener((o, oldRoot, newRoot) -> style(window, scene));
        }
        style(window, scene);
    }

    private static void style(Window window, Scene scene) {
        if (scene == null) {
            return;
        }
        // Los menús y listas emergentes usan la hoja de la ventana dueña.
        if (!(window instanceof PopupWindow)) {
            URL css = ThemeManager.class.getResource(STYLESHEET);
            if (css != null && !scene.getStylesheets().contains(css.toExternalForm())) {
                scene.getStylesheets().add(css.toExternalForm());
            }
        }
        applyTo(scene.getRoot());
    }
}
