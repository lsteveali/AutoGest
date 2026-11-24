package com.autogest.controller.components;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.function.Function;

/**
 * Vuelve escribible un desplegable: al teclear, la lista se reduce a los
 * elementos que contienen lo escrito (sin importar tildes ni mayúsculas) y se
 * abre sola. Al salir del campo, si lo escrito no corresponde a ningún
 * elemento, el desplegable queda vacío.
 *
 * <p>Los elementos se cargan con {@link #setItems(ComboBox, Collection)}: la
 * lista del desplegable pasa a ser una vista filtrada.</p>
 *
 * <p>Principles: Pure Fabrication, Open/Closed (sirve para cualquier tipo).</p>
 */
public final class SearchableCombo {

    private static final String SOURCE = "autogest.searchable.source";

    private SearchableCombo() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Vuelve escribible el desplegable.
     *
     * @param box     desplegable a convertir (conserva sus elementos actuales)
     * @param display cómo se muestra cada elemento
     * @param search  texto donde se busca (por ejemplo, nombre y documento)
     * @param <T>     tipo de los elementos
     */
    public static <T> void enable(ComboBox<T> box, Function<T, String> display, Function<T, String> search) {
        ObservableList<T> source = FXCollections.observableArrayList(box.getItems());
        FilteredList<T> filtered = new FilteredList<>(source, item -> true);
        box.getProperties().put(SOURCE, source);
        box.setItems(filtered);
        box.setEditable(true);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : display.apply(value);
            }

            @Override
            public T fromString(String text) {
                String wanted = text == null ? "" : text.trim();
                return source.stream()
                        .filter(item -> display.apply(item).equalsIgnoreCase(wanted))
                        .findFirst()
                        .orElse(null);
            }
        });

        // Evita volver a filtrar mientras el propio componente cambia el texto o el valor.
        boolean[] updating = {false};
        box.getEditor().textProperty().addListener((observable, before, typed) -> {
            if (updating[0]) {
                return;
            }
            T selected = box.getValue();
            // Al elegir un elemento, el campo muestra su nombre: no se filtra por él.
            if (selected != null && display.apply(selected).equals(typed)) {
                filtered.setPredicate(item -> true);
                return;
            }
            String query = normalize(typed);
            filtered.setPredicate(item -> query.isEmpty() || normalize(search.apply(item)).contains(query));
            if (box.getEditor().isFocused() && !filtered.isEmpty() && !box.isShowing()) {
                Platform.runLater(box::show);
            }
        });

        // Al salir: lo escrito se vuelve el elemento que coincide, o el campo queda vacío.
        box.getEditor().focusedProperty().addListener((observable, before, focused) -> {
            if (focused) {
                return;
            }
            String text = box.getEditor().getText();
            T match = box.getConverter().fromString(text);
            if (match == null && filtered.size() == 1 && text != null && !text.isBlank()) {
                match = filtered.get(0);
            }
            T chosen = match;
            updating[0] = true;
            try {
                filtered.setPredicate(item -> true);
                box.setValue(chosen);
                box.getEditor().setText(box.getConverter().toString(chosen));
            } finally {
                updating[0] = false;
            }
        });
    }

    /**
     * Reemplaza los elementos de un desplegable escribible.
     *
     * @param box   desplegable preparado con {@link #enable}
     * @param items elementos nuevos
     * @param <T>   tipo de los elementos
     */
    @SuppressWarnings("unchecked")
    public static <T> void setItems(ComboBox<T> box, Collection<? extends T> items) {
        Object source = box.getProperties().get(SOURCE);
        if (source instanceof ObservableList<?> list) {
            ((ObservableList<T>) list).setAll(items);
        } else {
            box.getItems().setAll(items);
        }
    }

    /** Texto sin tildes y en minúsculas, para comparar. */
    static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
