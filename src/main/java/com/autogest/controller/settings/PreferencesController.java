package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.model.SystemPreferences;
import com.autogest.service.SettingsService;
import com.autogest.util.AppPreferences;
import com.autogest.util.Background;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Preferencias del sistema» (mockup Preferencias del sistema
 * V1): región y formato, interfaz, comportamiento, tablas y sesión. Los
 * interruptores del mockup se representan con casillas.
 *
 * <p>Principles: GRASP Controller, SRP, DIP, Low Coupling.</p>
 */
public class PreferencesController implements Initializable, NavigatorAware {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private Button restoreButton;
    private Button cancelButton;
    private Button saveButton;

    private final ComboBox<String> language = FormLayout.combo("Idioma");
    private final ComboBox<String> country = FormLayout.combo("País");
    private final ComboBox<String> sidebarMode = FormLayout.combo("Barra lateral");
    private final ComboBox<String> timeZone = FormLayout.combo("Zona horaria");
    private final ComboBox<String> currency = FormLayout.combo("Moneda");
    private final ComboBox<String> dateFormat = FormLayout.combo("Formato de fecha");
    private final ComboBox<String> timeFormat = FormLayout.combo("Formato de hora");
    private final ComboBox<String> decimalSeparator = FormLayout.combo("Separador decimal");
    private final ComboBox<Integer> pageSize = FormLayout.combo("Registros por página", String::valueOf);
    private final ComboBox<String> defaultSort = FormLayout.combo("Orden predeterminado");
    private final ComboBox<Integer> inactivity = FormLayout.combo("Tiempo de inactividad", SettingsLabels::minutesText);
    private final CheckBox confirmDelete = new CheckBox("Confirmar antes de eliminar registros");
    private final CheckBox saveConfirmation = new CheckBox("Mostrar mensajes de confirmación después de guardar");
    private final CheckBox openLastModule = new CheckBox("Abrir automáticamente el último módulo utilizado");
    private final CheckBox contextHelp = new CheckBox("Mostrar ayuda contextual");
    private final CheckBox recentFilters = new CheckBox("Guardar filtros utilizados recientemente");
    private final CheckBox extraTableInfo = new CheckBox("Mostrar información adicional en tablas");
    private final CheckBox autoLogout = new CheckBox("Cerrar sesión automáticamente después de inactividad");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        language.getItems().setAll(SettingsLabels.languages());
        country.getItems().setAll(SettingsLabels.countries());
        sidebarMode.getItems().setAll(SettingsLabels.sidebarModes());
        timeZone.getItems().setAll(SettingsLabels.timeZones());
        currency.getItems().setAll(SettingsLabels.currencies());
        dateFormat.getItems().setAll(SettingsLabels.dateFormats());
        timeFormat.getItems().setAll(SettingsLabels.timeFormats());
        decimalSeparator.getItems().setAll(SettingsLabels.decimalSeparators());
        pageSize.getItems().setAll(SettingsService.PAGE_SIZES);
        defaultSort.getItems().setAll(SettingsLabels.sortOrders());
        inactivity.getItems().setAll(SettingsLabels.inactivityMinutes());
        inactivity.disableProperty().bind(autoLogout.selectedProperty().not());

        PageHeader header = new PageHeader("Preferencias del sistema",
                "Personaliza el comportamiento general de AutoGest según las necesidades de tu taller.");
        header.setBreadcrumbs(List.of("Configuración", "Preferencias del sistema"));

        VBox left = new VBox(16, regionCard(), behaviourCard());
        VBox right = new VBox(16, tablesCard(), sessionCard());
        HBox columns = new HBox(16, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);
        // La columna derecha no baja de 300 px: sus títulos y casillas caben completos.
        right.setMinWidth(300);

        restoreButton = FormLayout.button("Restaurar valores predeterminados", "btn-secondary", this::restoreDefaults);
        cancelButton = FormLayout.button("Cancelar", "btn-secondary", this::load);
        saveButton = FormLayout.button("Guardar cambios", "btn-primary", this::save);
        root.getChildren().addAll(header, columns, FormLayout.actions(restoreButton, cancelButton, saveButton));
        Platform.runLater(this::load);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    // Las opciones que AutoGest todavía no aplica quedan desactivadas con la
    // nota «Próximamente»: la pantalla no promete algo que no pasa (hallazgo de
    // la revisión del PR #26). Se aplican: registros por página, confirmar antes
    // de eliminar, mensajes después de guardar y cierre de sesión por inactividad.

    private VBox regionCard() {
        // Dos columnas: en tres se cortaban la zona horaria y la moneda.
        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Idioma", language), FormLayout.labeled("País", country));
        FormLayout.row(grid, 1, FormLayout.labeled("Zona horaria", timeZone), FormLayout.labeled("Moneda", currency));
        FormLayout.row(grid, 2, FormLayout.labeled("Formato de fecha", dateFormat),
                FormLayout.labeled("Formato de hora", timeFormat));
        FormLayout.row(grid, 3, FormLayout.labeled("Separador decimal", decimalSeparator),
                FormLayout.labeled("Mostrar barra lateral", sidebarMode));
        disable(language, country, sidebarMode, timeZone, currency, dateFormat, timeFormat, decimalSeparator);
        return FormLayout.card("Región y formato", grid,
                comingSoon("Próximamente. Por ahora AutoGest usa español y el formato de Colombia: pesos, "
                        + "DD/MM/AAAA, 24 horas y coma decimal; la barra lateral va expandida."));
    }

    private VBox behaviourCard() {
        disable(openLastModule, contextHelp, recentFilters);
        return FormLayout.card("Comportamiento del sistema",
                wrap(confirmDelete), wrap(saveConfirmation), wrap(openLastModule), wrap(contextHelp),
                wrap(recentFilters),
                comingSoon("Las tres últimas opciones llegarán próximamente."));
    }

    private VBox tablesCard() {
        // Una columna: en media columna los títulos se cortaban («Registros por pá…»).
        GridPane grid = FormLayout.grid(1);
        FormLayout.row(grid, 0, FormLayout.labeled("Registros por página", pageSize));
        FormLayout.row(grid, 1, FormLayout.labeled("Orden predeterminado", defaultSort));
        disable(defaultSort, extraTableInfo);
        return FormLayout.card("Tablas y listados", grid, wrap(extraTableInfo),
                comingSoon("Registros por página se aplica a las listas al volver a abrirlas. "
                        + "El orden y la información adicional llegarán próximamente."));
    }

    private VBox sessionCard() {
        return FormLayout.card("Sesión", wrap(autoLogout),
                FormLayout.labeled("Tiempo de inactividad", inactivity));
    }

    private static void disable(javafx.scene.Node... nodes) {
        for (javafx.scene.Node node : nodes) {
            node.setDisable(true);
        }
    }

    /** Las casillas largas pasan a dos líneas en vez de cortarse con «…». */
    private static CheckBox wrap(CheckBox box) {
        box.setWrapText(true);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static javafx.scene.control.Label comingSoon(String text) {
        javafx.scene.control.Label note = new javafx.scene.control.Label(text);
        note.setWrapText(true);
        note.getStyleClass().add("settings-note");
        return note;
    }

    /** Carga (o recarga, con «Cancelar») las preferencias guardadas, en segundo plano. */
    private void load() {
        run(() -> service().getPreferences(), this::show, "No se pudieron cargar las preferencias");
    }

    /**
     * Ejecuta una lectura o escritura en segundo plano con los botones
     * desactivados; al terminar muestra el resultado o el error.
     */
    private void run(java.util.function.Supplier<SystemPreferences> work,
                     java.util.function.Consumer<SystemPreferences> done, String errorTitle) {
        setBusy(true);
        Background.run(work::get,
                result -> {
                    setBusy(false);
                    done.accept(result);
                },
                error -> {
                    setBusy(false);
                    ErrorDialog.show(errorTitle, error instanceof AutoGestException && error.getMessage() != null
                            ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo.");
                });
    }

    private void setBusy(boolean busy) {
        restoreButton.setDisable(busy);
        cancelButton.setDisable(busy);
        saveButton.setDisable(busy);
    }

    private void show(SystemPreferences p) {
        language.setValue(p.getLanguage());
        country.setValue(p.getCountry());
        sidebarMode.setValue(p.getSidebarMode());
        timeZone.setValue(p.getTimeZone());
        currency.setValue(p.getCurrency());
        dateFormat.setValue(p.getDateFormat());
        timeFormat.setValue(p.getTimeFormat());
        decimalSeparator.setValue(p.getDecimalSeparator());
        pageSize.setValue(p.getPageSize());
        defaultSort.setValue(p.getDefaultSort());
        inactivity.setValue(p.getInactivityMinutes());
        confirmDelete.setSelected(p.isConfirmBeforeDelete());
        saveConfirmation.setSelected(p.isShowSaveConfirmation());
        openLastModule.setSelected(p.isOpenLastModule());
        contextHelp.setSelected(p.isShowContextHelp());
        recentFilters.setSelected(p.isSaveRecentFilters());
        extraTableInfo.setSelected(p.isShowExtraTableInfo());
        autoLogout.setSelected(p.isAutoLogout());
    }

    private void save() {
        SystemPreferences p = new SystemPreferences();
        p.setLanguage(language.getValue());
        p.setCountry(country.getValue());
        p.setSidebarMode(sidebarMode.getValue());
        p.setTimeZone(timeZone.getValue());
        p.setCurrency(currency.getValue());
        p.setDateFormat(dateFormat.getValue());
        p.setTimeFormat(timeFormat.getValue());
        p.setDecimalSeparator(decimalSeparator.getValue());
        p.setPageSize(pageSize.getValue() == null ? 0 : pageSize.getValue());
        p.setDefaultSort(defaultSort.getValue());
        p.setInactivityMinutes(inactivity.getValue() == null ? 0 : inactivity.getValue());
        p.setConfirmBeforeDelete(confirmDelete.isSelected());
        p.setShowSaveConfirmation(saveConfirmation.isSelected());
        p.setOpenLastModule(openLastModule.isSelected());
        p.setShowContextHelp(contextHelp.isSelected());
        p.setSaveRecentFilters(recentFilters.isSelected());
        p.setShowExtraTableInfo(extraTableInfo.isSelected());
        p.setAutoLogout(autoLogout.isSelected());
        run(() -> service().updatePreferences(p),
                saved -> {
                    // Desde ya rigen en esta sesión (listas, confirmaciones, inactividad).
                    AppPreferences.update(saved);
                    info("Las preferencias se guardaron correctamente.");
                },
                "No se pudieron guardar las preferencias");
    }

    private void restoreDefaults() {
        if (!ConfirmDialog.confirm("Restaurar valores predeterminados",
                "¿Deseas volver a las preferencias de fábrica?")) {
            return;
        }
        run(() -> service().restoreDefaultPreferences(),
                restored -> {
                    show(restored);
                    AppPreferences.update(restored);
                    info("Se restauraron las preferencias predeterminadas.");
                },
                "No se pudieron restaurar las preferencias");
    }

    private static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.setTitle("Preferencias del sistema");
        alert.showAndWait();
    }

    private SettingsService service() {
        return AppContext.getInstance().getSettingsService();
    }
}
