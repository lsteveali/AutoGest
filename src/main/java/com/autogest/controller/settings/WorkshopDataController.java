package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.NavigatorAware;
import com.autogest.controller.ViewNavigator;
import com.autogest.controller.components.ErrorDialog;
import com.autogest.controller.components.FormLayout;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.AutoGestException;
import com.autogest.model.WorkshopProfile;
import com.autogest.service.SettingsService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
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

    private final TextField name = FormLayout.textField("AutoGest Taller Automotriz");
    private final TextField nit = FormLayout.textField("901.456.789-1");
    private final TextField phone = FormLayout.textField("605 555 1234");
    private final TextField email = FormLayout.textField("contacto@autogest.com");
    private final TextField website = FormLayout.textField("www.autogest.com");
    private final ComboBox<String> workshopType = FormLayout.combo("Tipo de taller");
    private final TextField commercialName = FormLayout.textField("AutoGest Taller Automotriz");
    private final TextArea description = FormLayout.textArea("Servicio especializado en mantenimiento...", 4);
    private final TextField address = FormLayout.textField("Carrera 15 # 25-40");
    private final TextField whatsapp = FormLayout.textField("+57 300 123 4567");
    private final TextField city = FormLayout.textField("Valledupar");
    private final TextField postalCode = FormLayout.textField("200001");
    private final TextField department = FormLayout.textField("Cesar");
    private final ComboBox<String> country = FormLayout.combo("País");
    private final TextField mainEmail = FormLayout.textField("contacto@autogest.com");
    private final TextField mainPhone = FormLayout.textField("605 555 1234");
    private final TextArea businessHours = FormLayout.textArea("Lunes - Viernes: 8:00 AM - 6:00 PM", 3);

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

        root.getChildren().addAll(header, columns, FormLayout.actions(
                FormLayout.button("Cancelar", "btn-secondary", this::load),
                FormLayout.button("Guardar cambios", "btn-primary", this::save)));
        Platform.runLater(this::load);
    }

    @Override
    public void setNavigator(ViewNavigator navigator) {
        this.navigator = navigator;
    }

    private VBox generalCard() {
        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Nombre del taller *", name), FormLayout.labeled("NIT *", nit));
        FormLayout.row(grid, 1, FormLayout.labeled("Teléfono", phone), FormLayout.labeled("Correo electrónico *", email));
        FormLayout.row(grid, 2, FormLayout.labeled("Sitio web", website), FormLayout.labeled("Tipo de taller", workshopType));
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
        FormLayout.row(grid, 0, FormLayout.labeled("Dirección", address), FormLayout.labeled("WhatsApp", whatsapp));
        FormLayout.row(grid, 1, FormLayout.labeled("Ciudad", city), FormLayout.labeled("Código postal", postalCode));
        FormLayout.row(grid, 2, FormLayout.labeled("Departamento", department), FormLayout.labeled("País", country));
        return FormLayout.card("Ubicación", grid);
    }

    private VBox contactCard() {
        GridPane grid = FormLayout.grid(2);
        FormLayout.row(grid, 0, FormLayout.labeled("Correo principal", mainEmail),
                FormLayout.labeled("Teléfono principal", mainPhone));
        FormLayout.fullRow(grid, 1, FormLayout.labeled("Horario de atención", businessHours));
        return FormLayout.card("Información de contacto", grid);
    }

    /** Carga (o recarga, con «Cancelar») los datos guardados. */
    private void load() {
        try {
            WorkshopProfile p = service().getWorkshopProfile();
            name.setText(FormLayout.orEmpty(p.getName()));
            nit.setText(FormLayout.orEmpty(p.getNit()));
            phone.setText(FormLayout.orEmpty(p.getPhone()));
            email.setText(FormLayout.orEmpty(p.getEmail()));
            website.setText(FormLayout.orEmpty(p.getWebsite()));
            workshopType.setValue(p.getWorkshopType());
            commercialName.setText(FormLayout.orEmpty(p.getCommercialName()));
            description.setText(FormLayout.orEmpty(p.getDescription()));
            address.setText(FormLayout.orEmpty(p.getAddress()));
            whatsapp.setText(FormLayout.orEmpty(p.getWhatsapp()));
            city.setText(FormLayout.orEmpty(p.getCity()));
            postalCode.setText(FormLayout.orEmpty(p.getPostalCode()));
            department.setText(FormLayout.orEmpty(p.getDepartment()));
            country.setValue(p.getCountry());
            mainEmail.setText(FormLayout.orEmpty(p.getMainEmail()));
            mainPhone.setText(FormLayout.orEmpty(p.getMainPhone()));
            businessHours.setText(FormLayout.orEmpty(p.getBusinessHours()));
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron cargar los datos del taller", e.getMessage());
        }
    }

    private void save() {
        WorkshopProfile p = new WorkshopProfile();
        p.setName(FormLayout.text(name));
        p.setNit(FormLayout.text(nit));
        p.setPhone(FormLayout.text(phone));
        p.setEmail(FormLayout.text(email));
        p.setWebsite(FormLayout.text(website));
        p.setWorkshopType(workshopType.getEditor().getText());
        p.setCommercialName(FormLayout.text(commercialName));
        p.setDescription(FormLayout.text(description));
        p.setAddress(FormLayout.text(address));
        p.setWhatsapp(FormLayout.text(whatsapp));
        p.setCity(FormLayout.text(city));
        p.setPostalCode(FormLayout.text(postalCode));
        p.setDepartment(FormLayout.text(department));
        p.setCountry(country.getValue());
        p.setMainEmail(FormLayout.text(mainEmail));
        p.setMainPhone(FormLayout.text(mainPhone));
        p.setBusinessHours(FormLayout.text(businessHours));
        try {
            service().updateWorkshopProfile(p);
            Alert saved = new Alert(Alert.AlertType.INFORMATION, "Los datos del taller se guardaron correctamente.");
            saved.setHeaderText(null);
            saved.setTitle("Datos del taller");
            saved.showAndWait();
        } catch (AutoGestException e) {
            ErrorDialog.show("No se pudieron guardar los datos del taller", e.getMessage());
        }
    }

    private SettingsService service() {
        return AppContext.getInstance().getSettingsService();
    }
}
