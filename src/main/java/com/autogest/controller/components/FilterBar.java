package com.autogest.controller.components;

import com.autogest.util.AppPreferences;
import com.autogest.util.LocalMemory;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Fila de búsqueda y filtros que se coloca sobre las tablas. Cada control
 * notifica sus cambios mediante un {@link Consumer}, de modo que la pantalla
 * solo se preocupa por recargar los datos.
 *
 * <p>El filtrado real (reglas de negocio) vive en el Service; este componente
 * solo transporta el texto o la opción elegida.</p>
 *
 * <p>Con «Guardar filtros utilizados recientemente» activo, cada barra
 * recuerda en el equipo ({@link LocalMemory}) la búsqueda y los filtros de su
 * pantalla, y los vuelve a aplicar al abrirla, sin que la pantalla haga nada.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class FilterBar extends HBox {

    /** Clave del recuadro de búsqueda en la memoria de filtros. */
    static final String SEARCH_KEY = "search";
    private static final LocalMemory MEMORY = new LocalMemory();
    /** Pantalla que se está cargando; la fija el navegador antes de cargar el FXML. */
    private static String currentScreen;

    /** Pantalla a la que pertenece esta barra (para recordar sus filtros). */
    private final String screen = currentScreen;

    /** Crea una barra vacía lista para agregar controles. */
    public FilterBar() {
        getStyleClass().add("filter-bar");
        setAlignment(Pos.CENTER_LEFT);
    }

    /**
     * Indica qué pantalla se está cargando: las barras que se creen a
     * continuación recuerdan sus filtros bajo ese nombre.
     *
     * @param screen nombre de la pantalla, o {@code null}
     */
    public static void setCurrentScreen(String screen) {
        currentScreen = screen;
    }

    /** «Guardar filtros utilizados recientemente» está activo y se sabe la pantalla. */
    private boolean remembers() {
        return screen != null && AppPreferences.current().isSaveRecentFilters();
    }

    /**
     * Devuelve el filtro guardado después de que la pantalla termine de
     * armarse (su controlador ya tiene la tabla lista para recargar).
     */
    private void restore(String key, Consumer<String> apply) {
        if (!remembers()) {
            return;
        }
        String saved = MEMORY.filter(screen, key);
        if (saved != null) {
            Platform.runLater(() -> apply.accept(saved));
        }
    }

    private void remember(String key, String value) {
        if (remembers()) {
            MEMORY.rememberFilter(screen, key, value);
        }
    }

    /**
     * Agrega un campo de búsqueda de texto.
     *
     * @param placeholder texto de ayuda del campo
     * @param onSearch    recibe el texto cada vez que cambia
     */
    public void addSearch(String placeholder, Consumer<String> onSearch) {
        TextField field = new TextField();
        field.setPromptText(placeholder);
        field.getStyleClass().addAll("input", "filter-field");
        field.textProperty().addListener((observable, oldValue, newValue) -> {
            remember(SEARCH_KEY, newValue);
            onSearch.accept(newValue);
        });
        getChildren().add(field);
        // Solo si nadie escribió antes (por ejemplo el Asistente, que llena la búsqueda).
        restore(SEARCH_KEY, saved -> {
            if (field.getText() == null || field.getText().isEmpty()) {
                field.setText(saved);
            }
        });
    }

    /**
     * Agrega una etiqueta y una lista desplegable de filtro.
     *
     * @param label    etiqueta visible del filtro (en español)
     * @param options  opciones disponibles (la primera suele ser «Todos»)
     * @param onSelect recibe la opción elegida (puede ser {@code null})
     * @return el {@link ComboBox} creado, para fijar un valor por defecto o
     *         consultar la selección actual sin recorrer el árbol de nodos
     */
    public ComboBox<String> addCombo(String label, List<String> options, Consumer<String> onSelect) {
        Label filterLabel = new Label(label);
        filterLabel.getStyleClass().add("field-label");
        // La etiqueta nunca se encoge a «…»: si falta espacio, cede el buscador.
        filterLabel.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);

        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(options);
        combo.getStyleClass().addAll("input", "filter-combo");
        // La primera opción («Todos») queda elegida desde el inicio: antes el filtro
        // se veía vacío. Se fija antes del listener para no recargar dos veces.
        if (!options.isEmpty()) {
            combo.setValue(options.get(0));
        }
        combo.valueProperty().addListener((observable, oldValue, newValue) -> {
            // La primera opción («Todos») es la de siempre: no hace falta recordarla.
            remember(label, options.isEmpty() || options.get(0).equals(newValue) ? null : newValue);
            onSelect.accept(newValue);
        });

        getChildren().addAll(filterLabel, combo);
        // Solo si el filtro sigue en su primera opción (el Asistente pudo haberlo elegido).
        restore(label, saved -> {
            boolean untouched = options.isEmpty() || options.get(0).equals(combo.getValue());
            if (untouched && combo.getItems().contains(saved)) {
                combo.setValue(saved);
            }
        });
        return combo;
    }
}
