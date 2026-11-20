package com.autogest.controller.components;

import com.autogest.util.AppPreferences;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Tabla reutilizable con encabezado, orden por columna, paginación y soporte
 * para una columna de acciones. Todas las listas de los módulos (clientes,
 * vehículos, usuarios...) se construyen con este componente para que la
 * apariencia y el comportamiento sean idénticos.
 *
 * <p>Cada columna recibe una función que, a partir de una fila, devuelve un
 * {@link String} (texto) o un {@link Node} (por ejemplo una insignia o un
 * menú de acciones). El orden trabaja sobre el texto visible; las columnas de
 * nodos se ordenan por su texto de insignia cuando aplica.</p>
 *
 * <p>Principles: Pure Fabrication, Information Expert, Low Coupling.</p>
 *
 * @param <T> tipo de fila que muestra la tabla
 */
public class PagedTable<T> extends VBox {

    /** Relleno horizontal de la cabecera (12 + 12) más espacio para la flecha de orden. */
    private static final double HEADER_EXTRA = 24 + 14;
    /** Alto de la fila de títulos (13px en negrita con relleno 10). */
    private static final double HEADER_HEIGHT = 41;
    private static final int MIN_FITTED_ROWS = 3;

    /** Alto de fila cuando la tabla se ajusta a sus filas; 0 = la tabla crece. */
    private double fitRowHeight;

    private final int pageSize;
    private final TableView<T> table = new TableView<>();
    private final Label rangeLabel = new Label();
    private final Button previousButton = new Button("Anterior");
    private final Button nextButton = new Button("Siguiente");
    private final Map<TableColumn<T, ?>, Function<T, ?>> columnValueProviders = new LinkedHashMap<>();

    private List<T> allData = new ArrayList<>();
    private int currentPage;
    private Consumer<T> onSelect;

    /**
     * Crea una tabla paginada. Las listas de {@value AppPreferences#DEFAULT_PAGE_SIZE}
     * filas (las de cada módulo) usan los «Registros por página» de las
     * Preferencias del sistema; las tablas pequeñas (5 u 8 filas dentro de un
     * detalle) conservan su tamaño.
     *
     * @param pageSize cantidad de filas por página (por ejemplo 10)
     */
    public PagedTable(int pageSize) {
        this.pageSize = pageSize == AppPreferences.DEFAULT_PAGE_SIZE ? AppPreferences.pageSize() : pageSize;
        getStyleClass().add("paged-table");

        table.getStyleClass().add("table-autogest");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No hay registros para mostrar"));
        VBox.setVgrow(table, Priority.ALWAYS);

        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && onSelect != null) {
                onSelect.accept(newValue);
            }
        });
        table.getSortOrder().addListener((ListChangeListener<TableColumn<T, ?>>) change -> sortAllData());

        getChildren().addAll(table, buildPaginationBar());
    }

    /**
     * Agrega una columna a la tabla.
     *
     * @param title     título de la columna (en español)
     * @param cellValue función que obtiene el valor de cada fila: un texto o un nodo
     */
    public void addColumn(String title, Function<T, ?> cellValue) {
        addColumn(title, cellValue, 0);
    }

    /**
     * Agrega una columna con un ancho mínimo propio (por ejemplo, una celda con
     * avatar y nombre que no debe cortarse en «Andrés…»).
     *
     * @param title     título de la columna (en español)
     * @param cellValue función que obtiene el valor de cada fila: un texto o un nodo
     * @param minWidth  ancho mínimo en píxeles; si es menor que el del título, manda el título
     */
    public void addColumn(String title, Function<T, ?> cellValue, double minWidth) {
        TableColumn<T, String> column = new TableColumn<>(title);
        columnValueProviders.put(column, cellValue);
        column.setCellValueFactory(cellData -> {
            Object value = cellValue.apply(cellData.getValue());
            return new javafx.beans.property.ReadOnlyStringWrapper(sortKey(value));
        });
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Object value = cellValue.apply(getTableRow().getItem());
                if (value instanceof Node node) {
                    setText(null);
                    setGraphic(node);
                } else {
                    setGraphic(null);
                    setText(value == null ? "" : value.toString());
                }
            }
        });
        column.setSortable(true);
        column.setComparator(String.CASE_INSENSITIVE_ORDER);
        // Ancho mínimo = título completo + relleno de la cabecera + flecha de orden,
        // para que el reparto de la política «constrained» no lo corte («Acció...»).
        column.setMinWidth(Math.max(minWidth, headerWidth(title)));
        table.getColumns().add(column);
    }

    /**
     * Ajusta el alto de la tabla a las filas de la página (hasta {@code pageSize}),
     * en vez de crecer para llenar el espacio. Úsalo cuando la tabla va dentro de
     * una pantalla con desplazamiento y hay contenido debajo: así no queda un
     * hueco en blanco con pocas filas ni se aplasta con muchas.
     *
     * @param rowHeight alto fijo de cada fila en píxeles
     */
    public void fitHeightToRows(double rowHeight) {
        this.fitRowHeight = rowHeight;
        table.setFixedCellSize(rowHeight);
        VBox.setVgrow(table, Priority.NEVER);
        updateFittedHeight();
    }

    private void updateFittedHeight() {
        if (fitRowHeight <= 0) {
            return;
        }
        // Mínimo de 3 filas para que el mensaje de tabla vacía tenga lugar.
        int rows = Math.max(MIN_FITTED_ROWS, table.getItems().size());
        double height = HEADER_HEIGHT + rows * fitRowHeight + 2;
        table.setMinHeight(height);
        table.setPrefHeight(height);
        table.setMaxHeight(height);
    }

    private static double headerWidth(String title) {
        Text measure = new Text(title);
        measure.setFont(Font.font("Manrope", FontWeight.BOLD, 13));
        return Math.ceil(measure.getLayoutBounds().getWidth()) + HEADER_EXTRA;
    }

    /**
     * Carga los datos completos; la paginación se calcula automáticamente y se
     * vuelve a la primera página.
     *
     * @param data filas a mostrar (puede ser nulo)
     */
    public void setData(List<T> data) {
        this.allData = data == null ? new ArrayList<>() : new ArrayList<>(data);
        this.currentPage = 0;
        applyPage();
    }

    /**
     * Registra la acción a ejecutar cuando el usuario selecciona una fila.
     *
     * @param onSelect consumidor que recibe la fila seleccionada
     */
    public void setOnSelect(Consumer<T> onSelect) {
        this.onSelect = onSelect;
    }

    /** Quita la selección actual de la tabla. */
    public void clearSelection() {
        table.getSelectionModel().clearSelection();
    }

    private HBox buildPaginationBar() {
        rangeLabel.getStyleClass().add("pagination-label");

        previousButton.getStyleClass().add("page-btn");
        previousButton.setOnAction(event -> changePage(currentPage - 1));
        nextButton.getStyleClass().add("page-btn");
        nextButton.setOnAction(event -> changePage(currentPage + 1));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(12, rangeLabel, spacer, previousButton, nextButton);
        bar.getStyleClass().add("pagination-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private void changePage(int page) {
        int totalPages = totalPages();
        if (page < 0 || page >= totalPages) {
            return;
        }
        currentPage = page;
        applyPage();
    }

    private void applyPage() {
        int from = currentPage * pageSize;
        int to = Math.min(from + pageSize, allData.size());
        table.getItems().setAll(allData.subList(from, to));
        updateFittedHeight();
        rangeLabel.setText(rangeText(from, to));
        previousButton.setDisable(currentPage == 0);
        nextButton.setDisable(currentPage >= totalPages() - 1);
    }

    private int totalPages() {
        return Math.max(1, (int) Math.ceil(allData.size() / (double) pageSize));
    }

    private String rangeText(int from, int to) {
        if (allData.isEmpty()) {
            return "Mostrando 0-0 de 0";
        }
        return "Mostrando " + (from + 1) + "-" + to + " de " + allData.size();
    }

    /**
     * Reordena el conjunto completo de datos (no solo la página visible) al
     * cambiar el orden de las columnas.
     */
    private void sortAllData() {
        if (table.getSortOrder().isEmpty()) {
            return;
        }
        Comparator<T> comparator = null;
        for (TableColumn<T, ?> column : table.getSortOrder()) {
            Comparator<T> columnComparator = comparatorFor(column);
            comparator = comparator == null ? columnComparator : comparator.thenComparing(columnComparator);
        }
        if (comparator != null) {
            allData.sort(comparator);
            currentPage = 0;
            applyPage();
        }
    }

    private Comparator<T> comparatorFor(TableColumn<T, ?> column) {
        Function<T, ?> provider = columnValueProviders.get(column);
        Comparator<T> comparator = Comparator.comparing(
                row -> sortKey(provider.apply(row)),
                String.CASE_INSENSITIVE_ORDER);
        if (column.getSortType() == TableColumn.SortType.DESCENDING) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    /** Obtiene un texto ordenable a partir del valor de una celda. */
    private String sortKey(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Label label) {
            return label.getText();
        }
        if (value instanceof Text text) {
            return text.getText();
        }
        return "";
    }
}
