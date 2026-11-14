package com.autogest.controller.components;

import com.autogest.exception.ValidationException;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Piezas comunes para armar formularios y fichas de detalle por código con el
 * sistema de diseño (clases {@code .card}, {@code .card-title},
 * {@code .field-label}, {@code .input}, {@code .btn-*}). Evita repetir en cada
 * controlador los mismos métodos de construcción y de lectura de campos.
 *
 * <p>Las lecturas numéricas lanzan {@link ValidationException} con un mensaje
 * en español para que el controlador lo muestre con {@link ErrorDialog}. Las
 * reglas de negocio siguen viviendo en los Services.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion, DRY.</p>
 */
public final class FormLayout {

    private FormLayout() {
        // Clase de utilidad: no se instancia.
    }

    // ---------- contenedores ----------

    /**
     * Rejilla con columnas de igual ancho.
     *
     * @param columns cantidad de columnas
     * @return rejilla con separación estándar
     */
    public static GridPane grid(int columns) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        for (int i = 0; i < columns; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / columns);
            column.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(column);
        }
        return grid;
    }

    /**
     * Agrega una fila a la rejilla: cada nodo ocupa una columna.
     *
     * @param grid  rejilla destino
     * @param row   índice de la fila
     * @param nodes nodos de la fila (normalmente salidas de {@link #labeled})
     */
    public static void row(GridPane grid, int row, Node... nodes) {
        for (int column = 0; column < nodes.length; column++) {
            grid.add(nodes[column], column, row);
        }
    }

    /**
     * Agrega un nodo que ocupa todo el ancho de la rejilla.
     */
    public static void fullRow(GridPane grid, int row, Node node) {
        grid.add(node, 0, row, Math.max(1, grid.getColumnConstraints().size()), 1);
    }

    /**
     * Tarjeta blanca con título.
     *
     * @param title   título de la tarjeta (puede ser {@code null})
     * @param content contenido debajo del título
     * @return tarjeta con la clase {@code .card}
     */
    public static VBox card(String title, Node... content) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        if (title != null) {
            Label label = new Label(title);
            label.getStyleClass().add("card-title");
            card.getChildren().add(label);
        }
        card.getChildren().addAll(content);
        return card;
    }

    /**
     * Fila de botones alineada a la derecha.
     */
    public static HBox actions(Node... buttons) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox box = new HBox(12);
        box.getChildren().add(spacer);
        box.getChildren().addAll(buttons);
        box.setAlignment(Pos.CENTER_RIGHT);
        return box;
    }

    // ---------- campos ----------

    /** Etiqueta encima del campo. */
    public static VBox labeled(String text, Node field) {
        Label label = new Label(text);
        label.getStyleClass().add("field-label");
        VBox box = new VBox(6, label, field);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    /** Campo de texto con la clase {@code .input}. */
    public static TextField textField(String prompt) {
        TextField field = new TextField();
        field.getStyleClass().add("input");
        field.setMaxWidth(Double.MAX_VALUE);
        field.setPromptText(prompt);
        return field;
    }

    /** Área de texto con ajuste de línea. */
    public static TextArea textArea(String prompt, int rows) {
        TextArea area = new TextArea();
        area.getStyleClass().add("input");
        area.setPrefRowCount(rows);
        area.setWrapText(true);
        area.setPromptText(prompt);
        return area;
    }

    /** Desplegable de textos. */
    public static ComboBox<String> combo(String prompt) {
        ComboBox<String> box = new ComboBox<>();
        box.getStyleClass().add("input");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPromptText(prompt);
        return box;
    }

    /**
     * Desplegable de objetos del dominio que se muestran con {@code toText}.
     *
     * @param prompt texto de ayuda
     * @param toText cómo se muestra cada elemento
     * @param <T>    tipo de los elementos
     * @return desplegable configurado
     */
    public static <T> ComboBox<T> combo(String prompt, Function<T, String> toText) {
        ComboBox<T> box = new ComboBox<>();
        box.getStyleClass().add("input");
        box.setMaxWidth(Double.MAX_VALUE);
        box.setPromptText(prompt);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : toText.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        });
        return box;
    }

    /** Botón con la clase de estilo indicada ({@code btn-primary}, {@code btn-secondary}, {@code btn-danger}). */
    public static Button button(String text, String styleClass, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setOnAction(event -> action.run());
        return button;
    }

    /** Par etiqueta / valor de solo lectura para las fichas de detalle. */
    public static VBox detail(String label, String value) {
        Label valueNode = new Label(value == null || value.isBlank() ? "—" : value);
        valueNode.setWrapText(true);
        return detail(label, valueNode);
    }

    /** Par etiqueta / nodo (por ejemplo una insignia) para las fichas de detalle. */
    public static VBox detail(String label, Node value) {
        Label labelNode = new Label(label);
        labelNode.getStyleClass().add("field-label");
        VBox box = new VBox(4, labelNode, value);
        GridPane.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    // ---------- lectura ----------

    /** Texto recortado del campo; {@code null} si está vacío. */
    public static String text(TextInputControl field) {
        String text = field.getText();
        return text == null || text.isBlank() ? null : text.trim();
    }

    /**
     * Lee un entero del texto.
     *
     * @param text     texto del campo
     * @param name     nombre del campo para el mensaje («La cantidad»)
     * @param required si es obligatorio
     * @return el número, o 0 si no es obligatorio y está vacío
     * @throws ValidationException si falta o no es un entero
     */
    public static int parseInt(String text, String name, boolean required) {
        if (text == null || text.isBlank()) {
            if (required) {
                throw new ValidationException(name + " es obligatorio");
            }
            return 0;
        }
        try {
            return Integer.parseInt(text.trim().replace(".", "").replace(" ", ""));
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " debe ser un número entero");
        }
    }

    /**
     * Lee un valor de dinero ({@code 120000}, {@code 120000.50} o {@code 120000,50}).
     *
     * @throws ValidationException si falta o no es un número
     */
    public static BigDecimal parseMoney(String text, String name) {
        if (text == null || text.isBlank()) {
            throw new ValidationException(name + " es obligatorio");
        }
        try {
            return new BigDecimal(text.trim().replace(" ", "").replace(",", "."));
        } catch (NumberFormatException e) {
            throw new ValidationException(name + " debe ser un número");
        }
    }

    /** Devuelve {@code value} o «» si es {@code null} (para cargar campos). */
    public static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
