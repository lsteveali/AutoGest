package com.autogest.controller.report;

import com.autogest.model.ReportData;
import com.autogest.util.Formatter;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Gráficas de Reportes. Usan un solo color (una sola serie por gráfica), el
 * valor siempre va escrito junto a la marca y los ingresos muestran el detalle
 * de cada punto al pasar el cursor.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
final class ReportCharts {

    /** Marcas máximas en el eje de fechas, para que no se encimen. */
    private static final int MAX_X_LABELS = 7;

    private ReportCharts() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Línea de ingresos del periodo.
     *
     * @param points puntos en orden de fecha
     * @return gráfica, o un mensaje si no hay ingresos
     */
    static Node revenueChart(List<ReportData.Point> points) {
        boolean empty = points.stream().allMatch(point -> point.amount().signum() == 0);
        if (empty) {
            return emptyNote("Sin facturas en este periodo.");
        }
        int last = points.size() - 1;
        NumberAxis xAxis = new NumberAxis(last == 0 ? -1 : 0, last == 0 ? 1 : last,
                Math.max(1, Math.ceil(last / (double) (MAX_X_LABELS - 1))));
        xAxis.setMinorTickVisible(false);
        xAxis.setTickMarkVisible(false);
        xAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                int index = value.intValue();
                return value.doubleValue() == index && index >= 0 && index <= last ? points.get(index).label() : "";
            }

            @Override
            public Number fromString(String text) {
                return 0;
            }
        });
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(true);
        yAxis.setMinorTickVisible(false);
        yAxis.setTickMarkVisible(false);
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return Formatter.money(BigDecimal.valueOf(value.doubleValue()));
            }

            @Override
            public Number fromString(String text) {
                return 0;
            }
        });

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (int i = 0; i < points.size(); i++) {
            series.getData().add(new XYChart.Data<>(i, points.get(i).amount()));
        }
        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.getStyleClass().add("report-chart");
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setVerticalGridLinesVisible(false);
        chart.setAlternativeRowFillVisible(false);
        chart.setPrefHeight(240);
        chart.setMinHeight(240);
        chart.getData().add(series);
        for (int i = 0; i < points.size(); i++) {
            ReportData.Point point = points.get(i);
            Tooltip tooltip = new Tooltip(point.label() + "  ·  " + Formatter.money(point.amount()));
            tooltip.setShowDelay(Duration.millis(80));
            Tooltip.install(series.getData().get(i).getNode(), tooltip);
        }
        return chart;
    }

    /**
     * Barras horizontales con nombre y valor, proporcionales al mayor.
     *
     * @param values nombre → valor
     * @param text   texto del valor
     * @param <N>    tipo numérico
     * @return barras, o un mensaje si no hay datos
     */
    static <N extends Number> Node bars(Map<String, N> values, Function<N, String> text) {
        double max = values.values().stream().mapToDouble(Number::doubleValue).max().orElse(0);
        if (max <= 0) {
            return emptyNote("Sin datos en este periodo.");
        }
        // Nombre y valor arriba, barra debajo: así los nombres largos no se cortan.
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(4);
        ColumnConstraints name = new ColumnConstraints();
        name.setHgrow(Priority.ALWAYS);
        ColumnConstraints value = new ColumnConstraints();
        value.setMinWidth(Region.USE_PREF_SIZE);
        value.setHalignment(HPos.RIGHT);
        grid.getColumnConstraints().addAll(name, value);
        int row = 0;
        for (Map.Entry<String, N> entry : values.entrySet()) {
            Label label = new Label(entry.getKey());
            label.getStyleClass().add("report-bar-name");
            label.setWrapText(true);
            Label amount = new Label(text.apply(entry.getValue()));
            amount.getStyleClass().add("report-bar-value");
            Node track = track(entry.getValue().doubleValue() / max);
            GridPane.setMargin(track, new Insets(0, 0, 8, 0));
            grid.add(label, 0, row);
            grid.add(amount, 1, row);
            grid.add(track, 0, row + 1, 2, 1);
            row += 2;
        }
        return grid;
    }

    /** Pista con el relleno proporcional; un valor mayor que 0 siempre se ve. */
    private static Node track(double ratio) {
        Region fill = new Region();
        fill.getStyleClass().add("report-bar-fill");
        fill.setMaxWidth(Region.USE_PREF_SIZE);
        StackPane track = new StackPane(fill);
        track.getStyleClass().add("report-bar-track");
        track.setAlignment(Pos.CENTER_LEFT);
        track.setMaxHeight(Region.USE_PREF_SIZE);
        fill.prefWidthProperty().bind(track.widthProperty().multiply(ratio));
        fill.setMinWidth(ratio > 0 ? 6 : 0);
        return track;
    }

    static Label emptyNote(String text) {
        Label note = new Label(text);
        note.getStyleClass().add("order-muted");
        note.setWrapText(true);
        return note;
    }
}
