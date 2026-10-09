package com.autogest.controller.components;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;

/**
 * Tarjeta de indicador (KPI) con título, valor grande y detalle opcional.
 * La usan los tableros de cada módulo para mostrar métricas como total de
 * clientes o usuarios activos.
 *
 * <p>El valor y el detalle se pueden actualizar tras crearla con
 * {@link #setValue(String)} y {@link #setDetail(String)}, lo que permite
 * reutilizar la misma tarjeta al refrescar datos.</p>
 *
 * <p>Principles: Information Expert (de la pantalla), High Cohesion.</p>
 */
public class KpiCard extends VBox {

    private final Label valueLabel = new Label();
    private final Label detailLabel = new Label();

    /**
     * Crea una tarjeta de indicador sin ícono.
     *
     * @param title  nombre de la métrica (en español)
     * @param value  valor principal ya formateado (por ejemplo "$12.450.000")
     * @param detail texto de apoyo bajo el valor
     */
    public KpiCard(String title, String value, String detail) {
        this(title, value, detail, null);
    }

    /**
     * Crea una tarjeta de indicador con un ícono lineal opcional.
     *
     * @param title        nombre de la métrica (en español)
     * @param value        valor principal ya formateado
     * @param detail       texto de apoyo bajo el valor
     * @param iconPathData datos de ruta de un ícono de {@link IconSet}, o {@code null}
     */
    public KpiCard(String title, String value, String detail, String iconPathData) {
        getStyleClass().add("kpi-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        if (iconPathData != null) {
            Path icon = IconSet.icon(iconPathData);
            icon.getStyleClass().add("kpi-icon");
            header.getChildren().add(icon);
        }
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kpi-title");
        header.getChildren().add(titleLabel);

        valueLabel.getStyleClass().add("kpi-value");
        detailLabel.getStyleClass().add("kpi-detail");

        setValue(value);
        setDetail(detail);
        getChildren().addAll(header, valueLabel, detailLabel);
    }

    /**
     * Actualiza el valor principal de la tarjeta.
     *
     * @param value nuevo valor ya formateado
     */
    public void setValue(String value) {
        valueLabel.setText(value);
    }

    /**
     * Actualiza el texto de detalle de la tarjeta.
     *
     * @param detail nuevo texto de apoyo
     */
    public void setDetail(String detail) {
        detailLabel.setText(detail);
    }
}
