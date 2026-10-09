package com.autogest.controller.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Cabecera de página reutilizable: breadcrumb opcional, título, subtítulo y
 * un botón de acción principal alineado a la derecha (por ejemplo
 * «+ Nuevo cliente»). Evita que cada pantalla repita el mismo encabezado.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class PageHeader extends VBox {

    private final Label breadcrumbLabel = new Label();
    private final HBox titleRow = new HBox(16);
    private Button primaryButton;

    /**
     * Crea la cabecera con su título y subtítulo.
     *
     * @param title    título de la página (en español)
     * @param subtitle texto descriptivo bajo el título
     */
    public PageHeader(String title, String subtitle) {
        getStyleClass().add("page-header");

        breadcrumbLabel.getStyleClass().add("breadcrumb");
        breadcrumbLabel.setVisible(false);
        breadcrumbLabel.setManaged(false);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox titles = new VBox(2, titleLabel, subtitleLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().addAll(titles, spacer);

        getChildren().addAll(breadcrumbLabel, titleRow);
    }

    /**
     * Define (o actualiza) el botón de acción principal de la cabecera.
     *
     * @param text   texto del botón (en español)
     * @param action acción que se ejecuta al pulsarlo
     */
    public void setPrimaryAction(String text, Runnable action) {
        if (primaryButton == null) {
            primaryButton = new Button();
            primaryButton.getStyleClass().add("btn-primary");
            titleRow.getChildren().add(primaryButton);
        }
        primaryButton.setText(text);
        primaryButton.setOnAction(event -> action.run());
    }

    /**
     * Muestra el breadcrumb de navegación sobre el título.
     *
     * @param breadcrumbs pasos de la ruta; si es nulo o vacío se oculta
     */
    public void setBreadcrumbs(List<String> breadcrumbs) {
        if (breadcrumbs == null || breadcrumbs.isEmpty()) {
            breadcrumbLabel.setText("");
            breadcrumbLabel.setVisible(false);
            breadcrumbLabel.setManaged(false);
            return;
        }
        breadcrumbLabel.setText(String.join("  /  ", breadcrumbs));
        breadcrumbLabel.setVisible(true);
        breadcrumbLabel.setManaged(true);
    }
}
