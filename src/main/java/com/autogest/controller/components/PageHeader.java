package com.autogest.controller.components;

import com.autogest.controller.NavigationEvent;
import com.autogest.controller.View;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Cabecera de página reutilizable: breadcrumb opcional, título, subtítulo y
 * un botón de acción principal alineado a la derecha (por ejemplo
 * «+ Nuevo cliente»). Evita que cada pantalla repita el mismo encabezado.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class PageHeader extends VBox {

    private final HBox breadcrumbRow = new HBox(6);
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

        breadcrumbRow.getStyleClass().add("breadcrumb-row");
        breadcrumbRow.setAlignment(Pos.CENTER_LEFT);
        breadcrumbRow.setVisible(false);
        breadcrumbRow.setManaged(false);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");

        VBox titles = new VBox(2, titleLabel, subtitleLabel);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        titleRow.setAlignment(Pos.CENTER_LEFT);
        titleRow.getChildren().addAll(titles, spacer);

        getChildren().addAll(breadcrumbRow, titleRow);
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
     * Muestra las migas de pan sobre el título. Cada paso (menos el último,
     * que es la pantalla actual) cuyo texto coincide con el título de una
     * pantalla se vuelve un enlace. Si hay más de un paso, se agrega «Volver»
     * al inicio. Ambos lanzan un {@link NavigationEvent} que atiende el shell.
     *
     * @param breadcrumbs pasos de la ruta; si es nulo o vacío se oculta
     */
    public void setBreadcrumbs(List<String> breadcrumbs) {
        breadcrumbRow.getChildren().clear();
        boolean show = breadcrumbs != null && !breadcrumbs.isEmpty();
        breadcrumbRow.setVisible(show);
        breadcrumbRow.setManaged(show);
        if (!show) {
            return;
        }
        if (breadcrumbs.size() > 1) {
            Button back = new Button("Volver");
            back.setGraphic(IconSet.icon(IconSet.ARROW_LEFT));
            back.getStyleClass().add("back-link");
            back.setOnAction(event -> back.fireEvent(NavigationEvent.back()));
            Region divider = new Region();
            divider.getStyleClass().add("breadcrumb-divider");
            breadcrumbRow.getChildren().addAll(back, divider);
        }
        for (int i = 0; i < breadcrumbs.size(); i++) {
            if (i > 0) {
                Label slash = new Label("/");
                slash.getStyleClass().add("breadcrumb");
                breadcrumbRow.getChildren().add(slash);
            }
            breadcrumbRow.getChildren().add(crumb(breadcrumbs.get(i), i == breadcrumbs.size() - 1));
        }
    }

    private Node crumb(String text, boolean current) {
        Optional<View> target = current ? Optional.empty() : View.byTitle(text);
        if (target.isEmpty()) {
            Label label = new Label(text);
            label.getStyleClass().add(current ? "breadcrumb-current" : "breadcrumb");
            return label;
        }
        Button link = new Button(text);
        link.getStyleClass().add("breadcrumb-link");
        link.setOnAction(event -> link.fireEvent(NavigationEvent.open(target.get())));
        return link;
    }
}
