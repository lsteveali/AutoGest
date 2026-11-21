package com.autogest.controller;

import com.autogest.model.SystemPreferences;
import com.autogest.util.AppPreferences;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Window;
import javafx.util.Duration;

import java.util.function.Consumer;

/**
 * Cierra la sesión tras un tiempo sin usar AutoGest, según la preferencia
 * «Cerrar sesión automáticamente después de inactividad» (Preferencias del
 * sistema, de Dev 1). Cualquier movimiento del mouse, clic, tecla o
 * desplazamiento en cualquier ventana de la aplicación vuelve a empezar la
 * cuenta. Si al vencer hay un diálogo abierto, espera otro periodo: no cierra
 * la sesión con un formulario a medio llenar.
 *
 * <p>Principles: Pure Fabrication, SRP (el shell solo lo enciende y lo apaga).</p>
 */
public class InactivityTimer {

    private final PauseTransition pause = new PauseTransition();
    private final Runnable onTimeout;
    private final EventHandler<Event> activity = event -> restart();
    private final Consumer<SystemPreferences> preferencesChanged = preferences -> Platform.runLater(this::restart);
    private final ListChangeListener<Window> windowsChanged = change -> {
        while (change.next()) {
            change.getAddedSubList().forEach(this::watch);
            change.getRemoved().forEach(this::unwatch);
        }
    };

    /**
     * @param onTimeout qué hacer cuando vence el tiempo (cerrar la sesión)
     */
    public InactivityTimer(Runnable onTimeout) {
        this.onTimeout = onTimeout;
        pause.setOnFinished(event -> timeout());
    }

    /** Empieza a vigilar la actividad; se llama al abrir la sesión. */
    public void start() {
        Window.getWindows().forEach(this::watch);
        Window.getWindows().addListener(windowsChanged);
        AppPreferences.addListener(preferencesChanged);
        restart();
    }

    /** Deja de vigilar; se llama al cerrar la sesión. */
    public void stop() {
        pause.stop();
        AppPreferences.removeListener(preferencesChanged);
        Window.getWindows().removeListener(windowsChanged);
        Window.getWindows().forEach(this::unwatch);
    }

    /** Vuelve a contar desde cero con los minutos de la preferencia (0 = apagado). */
    void restart() {
        pause.stop();
        int minutes = AppPreferences.inactivityMinutes();
        if (minutes > 0) {
            pause.setDuration(Duration.minutes(minutes));
            pause.playFromStart();
        }
    }

    private void timeout() {
        long open = Window.getWindows().stream().filter(Window::isShowing).count();
        if (open > 1) {
            // Hay un diálogo abierto: no se pierde lo que se está escribiendo.
            restart();
            return;
        }
        stop();
        onTimeout.run();
    }

    private void watch(Window window) {
        window.addEventFilter(MouseEvent.MOUSE_MOVED, activity);
        window.addEventFilter(MouseEvent.MOUSE_PRESSED, activity);
        window.addEventFilter(KeyEvent.KEY_PRESSED, activity);
        window.addEventFilter(ScrollEvent.SCROLL, activity);
    }

    private void unwatch(Window window) {
        window.removeEventFilter(MouseEvent.MOUSE_MOVED, activity);
        window.removeEventFilter(MouseEvent.MOUSE_PRESSED, activity);
        window.removeEventFilter(KeyEvent.KEY_PRESSED, activity);
        window.removeEventFilter(ScrollEvent.SCROLL, activity);
    }
}
