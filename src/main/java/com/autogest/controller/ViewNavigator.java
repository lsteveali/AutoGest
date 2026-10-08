package com.autogest.controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Loads application screens in the central container and keeps the
 * navigation history. It is the only class that changes screens, so no
 * controller knows another screen directly.
 *
 * <p>Principles: Indirection, Pure Fabrication.</p>
 */
public class ViewNavigator {

    private final StackPane container;
    private final Deque<View> history = new ArrayDeque<>();
    private Consumer<View> onViewChange;

    /**
     * Creates a navigator bound to the given central container.
     *
     * @param container StackPane where the screens are loaded
     */
    public ViewNavigator(StackPane container) {
        this.container = container;
    }

    /** Navigates to a view without parameters. */
    public void navigateTo(View view) {
        history.push(view);
        load(view, null);
    }

    /** Navigates to a view passing an id parameter (detail/edit screens). */
    public void navigateWithParameter(View view, int parameter) {
        history.push(view);
        load(view, parameter);
    }

    /** Returns to the previously visited view. */
    public void goBack() {
        if (history.isEmpty()) {
            return;
        }
        history.pop();
        View previous = history.isEmpty() ? View.HOME : history.peek();
        if (history.isEmpty()) {
            history.push(previous);
        }
        load(previous, null);
    }

    /**
     * Registers a callback invoked after each navigation, used to highlight
     * the active item in the Sidebar.
     *
     * @param onViewChange consumer that receives the displayed view
     */
    public void setOnViewChange(Consumer<View> onViewChange) {
        this.onViewChange = onViewChange;
    }

    private void load(View view, Integer parameter) {
        var url = getClass().getResource(view.getFxmlPath());
        if (url == null) {
            throw new IllegalStateException("No se encontró el archivo FXML de " + view.getTitle());
        }
        try {
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            if (parameter != null && loader.getController() instanceof ParameterizedController pc) {
                pc.receiveParameter(parameter);
            }
            container.getChildren().clear();
            container.getChildren().add(root);
            if (onViewChange != null) {
                onViewChange.accept(view);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la pantalla " + view.getTitle(), e);
        }
    }
}