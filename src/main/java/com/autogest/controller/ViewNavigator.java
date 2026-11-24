package com.autogest.controller;

import com.autogest.controller.components.FilterBar;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/**
 * Carga las pantallas de la aplicación en el área central y mantiene el
 * historial de navegación. Es la única clase que cambia de pantalla, por lo
 * que ningún controlador conoce directamente a otra pantalla.
 *
 * <p>Principles: Indirection, Pure Fabrication.</p>
 */
public class ViewNavigator {

    private static final Duration ENTER_DURATION = Duration.millis(180);
    private static final double ENTER_OFFSET = 6;
    /**
     * Relleno del contenido desplazable: 12px a la derecha para la barra y
     * 88px abajo (botón del Asistente ~48px + su margen de 24px + 16px de aire).
     */
    private static final String SCROLL_CONTENT_PADDING = "-fx-padding: 0 12 88 0;";
    private static final String FILL_CONTENT_PADDING = "-fx-padding: 0 12 0 0;";

    /** Paso del historial: la pantalla y su parámetro (id de detalle/edición o {@code null}). */
    private record Entry(View view, Integer parameter) {
    }

    private final StackPane container;
    private final Deque<Entry> history = new ArrayDeque<>();
    private Consumer<View> onViewChange;

    /**
     * Crea un navegador vinculado al contenedor central indicado.
     *
     * @param container StackPane donde se cargan las pantallas
     */
    public ViewNavigator(StackPane container) {
        this.container = container;
    }

    /** Navega a una pantalla sin parámetros. */
    public void navigateTo(View view) {
        history.push(new Entry(view, null));
        load(view, null);
    }

    /** Navega a una pantalla pasando un id como parámetro (detalle/edición). */
    public void navigateWithParameter(View view, int parameter) {
        history.push(new Entry(view, parameter));
        load(view, parameter);
    }

    /**
     * Navega a una pantalla (con su parámetro, si lo tiene) y entrega la raíz
     * ya cargada, para aplicar filtros o precargar campos (Asistente).
     *
     * @param view      pantalla destino
     * @param parameter id que recibe la pantalla, o {@code null}
     * @param onLoaded  recibe la raíz de la pantalla cargada
     */
    public void navigate(View view, Integer parameter, Consumer<Parent> onLoaded) {
        history.push(new Entry(view, parameter));
        load(view, parameter);
        if (onLoaded != null && !container.getChildren().isEmpty()
                && container.getChildren().get(0) instanceof Parent root) {
            onLoaded.accept(root);
        }
    }

    /**
     * Regresa a la pantalla visitada anteriormente, con su mismo parámetro
     * (así un detalle vuelve a abrir el mismo registro). Sin historial, va a
     * Inicio.
     */
    public void goBack() {
        if (!history.isEmpty()) {
            history.pop();
        }
        Entry previous = history.isEmpty() ? new Entry(View.HOME, null) : history.peek();
        if (history.isEmpty()) {
            history.push(previous);
        }
        load(previous.view(), previous.parameter());
    }

    /**
     * Registra un callback que se ejecuta tras cada navegación, usado para
     * resaltar el ítem activo de la Sidebar.
     *
     * @param onViewChange consumidor que recibe la pantalla mostrada
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
            // Las barras de filtros de la pantalla recuerdan sus filtros bajo este nombre.
            FilterBar.setCurrentScreen(view.name());
            Parent root;
            try {
                root = loader.load();
            } finally {
                FilterBar.setCurrentScreen(null);
            }
            Object controller = loader.getController();
            if (controller instanceof NavigatorAware aware) {
                aware.setNavigator(this);
            }
            if (parameter != null && controller instanceof ParameterizedController pc) {
                pc.receiveParameter(parameter);
            }
            reserveAssistantSpace(root);
            container.getChildren().clear();
            container.getChildren().add(root);
            playEnter(root);
            if (onViewChange != null) {
                onViewChange.accept(view);
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo cargar la pantalla " + view.getTitle(), e);
        }
    }

    /**
     * El botón flotante del Asistente (main.fxml) queda sobre la esquina
     * inferior derecha del contenido y tapaba las acciones del final de los
     * formularios («Guardar»). En las pantallas con desplazamiento se agrega
     * espacio al final del contenido para poder subirlo por encima del botón.
     * Se aplica aquí para cubrir todos los módulos (también los de otros roles)
     * sin tocar cada FXML. El estilo en línea gana a las hojas de estilo.
     */
    private void reserveAssistantSpace(Parent root) {
        if (root instanceof ScrollPane scroll && scroll.getContent() instanceof Region content) {
            String style = content.getStyle() == null ? "" : content.getStyle();
            // Las listas que llenan el alto (fitToHeight) solo dejan lugar a la barra de
            // desplazamiento: su paginación ya deja libre la esquina del botón.
            content.setStyle(style + (scroll.isFitToHeight() ? FILL_CONTENT_PADDING : SCROLL_CONTENT_PADDING));
        }
    }

    /**
     * Entrada sutil de la pantalla nueva: aparece desde opacidad 0 y sube 6px
     * en 180 ms. Evita el salto brusco al cambiar de módulo sin hacer esperar
     * al usuario (la pantalla ya está cargada y se puede usar desde el inicio).
     */
    private void playEnter(Node root) {
        root.setOpacity(0);
        root.setTranslateY(ENTER_OFFSET);

        FadeTransition fade = new FadeTransition(ENTER_DURATION, root);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(ENTER_DURATION, root);
        slide.setToY(0);

        ParallelTransition enter = new ParallelTransition(fade, slide);
        enter.setInterpolator(Interpolator.EASE_OUT);
        enter.play();
    }
}