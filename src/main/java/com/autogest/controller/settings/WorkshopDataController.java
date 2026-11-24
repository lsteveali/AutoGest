package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PhoneField;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.model.WorkshopProfile;
import com.autogest.service.SettingsService;
import com.autogest.util.AppPreferences;
import com.autogest.util.Background;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controlador de «Datos del taller» (mockup Datos del taller V1): información
 * general, identidad, ubicación y contacto. El logo se gestiona más adelante
 * (fuera del alcance de este sprint).
 *
 * <p>Principles: GRASP Controller, SRP, DIP, Low Coupling.</p>
 */
public class WorkshopDataController implements Initializable, NavigatorAware {

    @FXML
    private VBox root;

    private ViewNavigator navigator;
    private Button cancelButton;
    private Button saveButton;

    // Los textos de ayuda describen qué escribir; antes repetían valores de ejemplo
    // («AutoGest Taller Automotriz») y en gris parecían datos ya guardados.
    private final TextField name = FormLayout.textField("Razón social del taller");
    private final TextField nit = FormLayout.textField("Con dígito de verificación");
    private final PhoneField phone = new PhoneField();
    private final TextField email = FormLayout.textField("Correo para facturas y avisos");
    private final TextField website = FormLayout.textField("Opcional");
    private final ComboBox<String> workshopType = FormLayout.combo("Tipo de taller");
    private final TextField commercialName = FormLayout.textField("Como lo conocen los clientes (opcional)");
    private final TextArea description = FormLayout.textArea("Qué servicios ofrece el taller (opcional)", 4);
    private final TextField address = FormLayout.textField("Calle, carrera y número");
    private final PhoneField whatsapp = new PhoneField();
    private final TextField city = FormLayout.textField("Ciudad");
    private final TextField postalCode = FormLayout.textField("Opcional");
    private final TextField department = FormLayout.textField("Departamento");
    private final ComboBox<String> country = FormLayout.combo("País");
    private final TextField mainEmail = FormLayout.textField("Si es distinto del correo general (opcional)");
    private final PhoneField mainPhone = new PhoneField();
    private final TextArea businessHours = FormLayout.textArea("Días y horas de atención (opcional)", 3);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        workshopType.getItems().setAll(SettingsLabels.workshopTypes());
        workshopType.setEditable(true);
        country.getItems().setAll(SettingsLabels.countries());

        PageHeader header = new PageHeader("Datos del taller",
                "Administra la información general y de contacto de tu taller.");
        header.setBreadcrumbs(List.of("Configuración", "Datos del taller"));

        VBox left = new VBox(16, generalCard(), locationCard());
        VBox right = new VBox(16, identityCard(), contactCard());
        HBox columns = new HBox(16, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setMaxWidth(Double.MAX_VALUE);
        right.setMaxWidth(Double.MAX_VALUE);

        cancelButton = FormLayout.button("Cancelar", "btn-secondary", this::load);
        saveButton = FormLayout.button("Guardar cambios", "btn-primary", this::save);
        root.getChildren().addAll(header, columns, FormLayout.actions(cancelButton, saveButton));
        Platform.runLater(this::load);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    private VBox generalCard() {
        // Nombre y correo a todo el ancho: son los valores más largos y en media columna se cortaban.
        GridPane grid = FormLayout.grid(2);
        FormLayout.fullRow(grid, 0, FormLayout.labeled("Nombre del taller *", name));
        FormLayout.row(grid, 1, FormLayout.labeled("NIT *", nit), FormLayout.labeled("Teléfono", phone));
        FormLayout.fullRow(grid, 2, FormLayout.labeled("Correo electrónico *", email));
        FormLayout.fullRow(grid, 3, FormLayout.labeled("Tipo de taller", workshopType));
        FormLayout.fullRow(grid, 4, FormLayout.labeled("Sitio web", website));
        return FormLayout.card("Información general", grid);
    }

    private VBox identityCard() {
        GridPane grid = FormLayout.grid(1);
        FormLayout.row(grid, 0, FormLayout.labeled("Nombre comercial", commercialName));
        FormLayout.row(grid, 1, FormLayout.labeled("Descripción del taller", description));
        return FormLayout.card("Identidad del taller", grid);
    }

    private VBox locationCard() {
        GridPane grid = FormLayout.grid(2);
        FormLayout.fullRow(grid, 0, FormLayout.labeled("Dirección", address));
        FormLayout.row(grid, 1, FormLayout.labeled("Ciudad", city), FormLayout.labeled("Departamento", department));
        FormLayout.row(grid, 2, FormLayout.labeled("País", country), FormLayout.labeled("Código postal", postalCode));
        FormLayout.row(grid, 3, FormLayout.labeled("WhatsApp", whatsapp));
        return FormLayout.card("Ubicación", grid);
    }

    private VBox contactCard() {
        GridPane grid = FormLayout.grid(2);
        FormLayout.fullRow(grid, 0, FormLayout.labeled("Correo principal", mainEmail));
        FormLayout.row(grid, 1, FormLayout.labeled("Teléfono principal", mainPhone));
        FormLayout.fullRow(grid, 2, FormLayout.labeled("Horario de atención", businessHours));
        return FormLayout.card("Información de contacto", grid);
    }

    /** Carga (o recarga, con «Cancelar») los datos guardados, en segundo plano. */
    private void load() {
        setBusy(true);
        Background.run(() -> service().getWorkshopProfile(),
                profile -> {
                    show(profile);
                    setBusy(false);
                },
                error -> {
                    setBusy(false);
                    ErrorDialog.show("No se pudieron cargar los datos del taller", errorText(error));
                });
    }

    /** Pasa los datos del taller al formulario. */
    private void show(WorkshopProfile p) {
        name.setText(FormLayout.orEmpty(p.getName()));
        nit.setText(FormLayout.orEmpty(p.getNit()));
        phone.setPhone(p.getPhone());
        email.setText(FormLayout.orEmpty(p.getEmail()));
        website.setText(FormLayout.orEmpty(p.getWebsite()));
        workshopType.setValue(p.getWorkshopType());
        commercialName.setText(FormLayout.orEmpty(p.getCommercialName()));
        description.setText(FormLayout.orEmpty(p.getDescription()));
        address.setText(FormLayout.orEmpty(p.getAddress()));
        whatsapp.setPhone(p.getWhatsapp());
        city.setText(FormLayout.orEmpty(p.getCity()));
        postalCode.setText(FormLayout.orEmpty(p.getPostalCode()));
        department.setText(FormLayout.orEmpty(p.getDepartment()));
        country.setValue(p.getCountry());
        mainEmail.setText(FormLayout.orEmpty(p.getMainEmail()));
        mainPhone.setPhone(p.getMainPhone());
        businessHours.setText(FormLayout.orEmpty(p.getBusinessHours()));
    }

    private void save() {
        WorkshopProfile p = new WorkshopProfile();
        p.setName(FormLayout.text(name));
        p.setNit(FormLayout.text(nit));
        p.setPhone(phone.getPhone());
        p.setEmail(FormLayout.text(email));
        p.setWebsite(FormLayout.text(website));
        p.setWorkshopType(workshopType.getEditor().getText());
        p.setCommercialName(FormLayout.text(commercialName));
        p.setDescription(FormLayout.text(description));
        p.setAddress(FormLayout.text(address));
        p.setWhatsapp(whatsapp.getPhone());
        p.setCity(FormLayout.text(city));
        p.setPostalCode(FormLayout.text(postalCode));
        p.setDepartment(FormLayout.text(department));
        p.setCountry(country.getValue());
        p.setMainEmail(FormLayout.text(mainEmail));
        p.setMainPhone(mainPhone.getPhone());
        p.setBusinessHours(FormLayout.text(businessHours));
        setBusy(true);
        Background.run(() -> service().updateWorkshopProfile(p),
                saved -> {
                    setBusy(false);
                    if (!AppPreferences.showSaveConfirmation()) {
                        return;
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Los datos del taller se guardaron correctamente.");
                    alert.setHeaderText(null);
                    alert.setTitle("Datos del taller");
                    alert.showAndWait();
                },
                error -> {
                    setBusy(false);
                    ErrorDialog.show("No se pudieron guardar los datos del taller", errorText(error));
                });
    }

    /** Mientras lee o guarda, los botones quedan desactivados (la ventana sigue respondiendo). */
    private void setBusy(boolean busy) {
        cancelButton.setDisable(busy);
        saveButton.setDisable(busy);
    }

    private static String errorText(RuntimeException error) {
        return error instanceof AutoGestException && error.getMessage() != null
                ? error.getMessage() : "Revisa la conexión e inténtalo de nuevo.";
    }

    private SettingsService service() {
        return AppContext.getInstance().getSettingsService();
    }
}
