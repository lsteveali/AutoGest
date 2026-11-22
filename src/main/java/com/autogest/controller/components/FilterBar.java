package com.autogest.controller.components;

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
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class FilterBar extends HBox {

    /** Crea una barra vacía lista para agregar controles. */
    public FilterBar() {
        getStyleClass().add("filter-bar");
        setAlignment(Pos.CENTER_LEFT);
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
        field.textProperty().addListener((observable, oldValue, newValue) -> onSearch.accept(newValue));
        getChildren().add(field);
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
        combo.valueProperty().addListener((observable, oldValue, newValue) -> onSelect.accept(newValue));

        getChildren().addAll(filterLabel, combo);
        return combo;
    }
}
