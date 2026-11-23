package com.autogest.controller.settings;

import com.autogest.controller.components.CarTurntable;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.util.LoginAppearance;
import com.autogest.util.LoginAppearance.Swatch;
import com.autogest.util.LoginAppearance.Vehicle;
import com.autogest.util.LoginAppearanceStore;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.net.URL;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Pantalla Configuración → Personalización: elige el vehículo y el color de
 * la vitrina del inicio de sesión, con una vista previa que gira mientras se
 * elige. Lo elegido se guarda en este computador ({@link LoginAppearanceStore})
 * al pulsar «Guardar cambios»; los demás equipos conservan la suya.
 *
 * <p>Principles: GRASP Controller, SRP.</p>
 */
public class AppearanceController implements Initializable {

    private static final double PREVIEW_WIDTH = 440;
    private static final double PREVIEW_HEIGHT = 380;

    @FXML
    private VBox root;

    private final LoginAppearanceStore store = new LoginAppearanceStore();
    private final ToggleGroup vehicleGroup = new ToggleGroup();
    private final Map<Vehicle, ToggleButton> vehicleButtons = new EnumMap<>(Vehicle.class);
    private final Map<String, Button> swatchButtons = new LinkedHashMap<>();
    private final ColorPicker picker = new ColorPicker();
    private final Label colorName = new Label();
    private LoginAppearance selected;
    private CarTurntable preview;
    private Label message;
    /** Evita que los cambios hechos por código vuelvan a disparar la selección. */
    private boolean updating;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Personalización",
                "Elige el vehículo y el color que giran en la pantalla de inicio de sesión de este equipo.");
        header.setBreadcrumbs(List.of("Configuración", "Personalización"));

        selected = store.load();
        preview = new CarTurntable(PREVIEW_WIDTH, PREVIEW_HEIGHT, selected);

        VBox left = new VBox(16, vehicleCard(), colorCard());
        message = SettingsForm.message();
        Button restore = SettingsForm.button("Restaurar valores de fábrica", "btn-secondary");
        restore.setOnAction(event -> {
            apply(LoginAppearance.DEFAULT);
            SettingsForm.show(message,
                    "Volvió la camioneta plateada. Pulsa «Guardar cambios» para dejarla en este equipo.", false);
        });
        Button save = SettingsForm.button("Guardar cambios", "btn-primary");
        save.setOnAction(event -> save());

        root.getChildren().addAll(header, SettingsForm.columns(left, previewCard()),
                SettingsForm.actions(message, restore, save));
        apply(selected);
    }

    // ---------- tarjetas ----------

    private VBox vehicleCard() {
        GridPane options = new GridPane();
        options.setHgap(10);
        Vehicle[] vehicles = Vehicle.values();
        for (Vehicle vehicle : vehicles) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / vehicles.length);
            options.getColumnConstraints().add(column);
        }
        for (Vehicle vehicle : vehicles) {
            ToggleButton button = new ToggleButton(vehicle.label());
            button.setToggleGroup(vehicleGroup);
            button.getStyleClass().add("appearance-vehicle");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> {
                if (!button.isSelected()) {
                    // Siempre hay un vehículo elegido: un segundo clic no lo quita.
                    button.setSelected(true);
                }
                choose(selected.withVehicle(vehicle));
            });
            vehicleButtons.put(vehicle, button);
            options.add(button, vehicle.ordinal(), 0);
        }
        return SettingsForm.card("Vehículo", IconSet.CAR,
                SettingsForm.note("El que gira sobre la plataforma al abrir AutoGest."), options);
    }

    private VBox colorCard() {
        FlowPane palette = new FlowPane(10, 10);
        for (Swatch swatch : LoginAppearance.PALETTE) {
            Circle dot = new Circle(13, Color.web(swatch.hex()));
            dot.getStyleClass().add("appearance-swatch-dot");
            Button button = new Button();
            button.setGraphic(dot);
            button.getStyleClass().add("appearance-swatch");
            button.setTooltip(new Tooltip(swatch.name()));
            button.setAccessibleText("Color " + swatch.name());
            button.setOnAction(event -> choose(selected.withColor(swatch.hex())));
            swatchButtons.put(swatch.hex(), button);
            palette.getChildren().add(button);
        }

        picker.getStyleClass().add("appearance-picker");
        picker.setOnAction(event -> {
            if (!updating) {
                choose(selected.withColor(hex(picker.getValue())));
            }
        });
        colorName.getStyleClass().add("appearance-color-name");

        return SettingsForm.card("Color de la pintura", IconSet.EDIT,
                SettingsForm.note("Elige uno de la paleta o cualquier otro con «Otro color»."),
                palette,
                SettingsForm.field("Otro color", picker, null),
                colorName);
    }

    private VBox previewCard() {
        StackPane stage = new StackPane(preview);
        stage.setAlignment(Pos.CENTER);
        stage.getStyleClass().add("appearance-preview");
        stage.setMinHeight(PREVIEW_HEIGHT + 24);
        return SettingsForm.card("Vista previa", IconSet.SPARKLES, stage,
                SettingsForm.note("Así se verá el inicio de sesión. Se guarda solo en este computador."));
    }

    // ---------- selección ----------

    private void choose(LoginAppearance appearance) {
        apply(appearance);
        message.setText("");
        message.getStyleClass().removeAll("form-message-error", "form-message-success");
    }

    /** Muestra una personalización en la vista previa y en los controles. */
    private void apply(LoginAppearance appearance) {
        selected = appearance;
        preview.show(appearance);
        updating = true;
        try {
            vehicleButtons.get(appearance.vehicle()).setSelected(true);
            swatchButtons.forEach((hex, button) -> {
                button.getStyleClass().remove("appearance-swatch-selected");
                if (hex.equals(appearance.color())) {
                    button.getStyleClass().add("appearance-swatch-selected");
                }
            });
            picker.setValue(Color.web(appearance.color()));
            colorName.setText("Color elegido: " + appearance.colorName() + " (" + appearance.color() + ")");
        } finally {
            updating = false;
        }
    }

    private void save() {
        try {
            store.save(selected);
            SettingsForm.show(message, "Listo. Al abrir AutoGest en este equipo se verá: "
                    + selected.vehicle().label().toLowerCase(Locale.ROOT) + ", "
                    + selected.colorName().toLowerCase(Locale.ROOT) + ".", false);
        } catch (IllegalStateException error) {
            SettingsForm.show(message, error.getMessage(), true);
        }
    }

    /** Color de JavaFX a texto {@code #RRGGBB}. */
    static String hex(Color color) {
        return String.format(Locale.ROOT, "#%02X%02X%02X",
                Math.round(color.getRed() * 255), Math.round(color.getGreen() * 255),
                Math.round(color.getBlue() * 255));
    }
}
