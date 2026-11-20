package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.ValidationException;
import com.autogest.model.NotificationSettings;
import com.autogest.model.NotificationSettings.TemplateText;
import com.autogest.service.NotificationSettingsService;
import com.autogest.service.NotificationTemplate;
import com.autogest.util.Background;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Pantalla Configuración → Notificaciones (Sprint 8, Líder): activa o apaga
 * los avisos automáticos y edita el asunto y el mensaje de cada plantilla de
 * correo, con los campos disponibles a un clic y una vista previa con datos
 * de ejemplo. Las reglas viven en {@link NotificationSettingsService}.
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class NotificationSettingsController implements Initializable {

    @FXML
    private VBox root;

    private final CheckBox vehicleReady = new CheckBox("Avisar cuando el vehículo esté listo para entregar");
    private final CheckBox billingNotices = new CheckBox(
            "Enviar los avisos de facturación (factura disponible, pago recibido y factura vencida)");
    private final CheckBox attachInvoice = new CheckBox("Adjuntar la factura en PDF y PNG a los correos de Facturación");

    /** Textos que se están editando, por plantilla (todavía sin guardar). */
    private final Map<NotificationTemplate, TemplateText> drafts = new EnumMap<>(NotificationTemplate.class);
    private final Map<NotificationTemplate, ToggleButton> items = new EnumMap<>(NotificationTemplate.class);
    private final ToggleGroup group = new ToggleGroup();
    private final TextField subjectField = new TextField();
    private final TextArea messageArea = new TextArea();
    private final Label editorTitle = new Label();
    private final Label editorUse = new Label();
    private final Label previewSubject = new Label();
    private final Label previewMessage = new Label();
    private NotificationTemplate selected;
    private boolean showing;
    private Label message;
    private Button saveButton;
    private Button restoreButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Configuración de notificaciones",
                "Elige qué avisos se envían solos y personaliza los correos que reciben los clientes.");
        header.setBreadcrumbs(List.of("Configuración", "Notificaciones"));

        message = SettingsForm.message();
        restoreButton = SettingsForm.button("Restaurar valores de fábrica", "btn-secondary");
        restoreButton.setOnAction(event -> restoreDefaults());
        saveButton = SettingsForm.button("Guardar cambios", "btn-primary");
        saveButton.setOnAction(event -> save());

        root.getChildren().addAll(header, noticesCard(), templatesCard(),
                SettingsForm.actions(message, restoreButton, saveButton));
        subjectField.textProperty().addListener((obs, old, value) -> onEdit());
        messageArea.textProperty().addListener((obs, old, value) -> onEdit());
        load();
    }

    // ---------- tarjetas ----------

    private VBox noticesCard() {
        return SettingsForm.card("Avisos automáticos", IconSet.BELL,
                SettingsForm.note("Se envían por correo sin que nadie los pida. Los recordatorios de mantenimiento "
                        + "se siguen enviando con el botón del Inicio."),
                new VBox(12, vehicleReady, billingNotices, attachInvoice));
    }

    private VBox templatesCard() {
        VBox list = new VBox(4);
        list.getStyleClass().add("template-list");
        for (NotificationTemplate template : NotificationTemplate.values()) {
            ToggleButton item = new ToggleButton(template.getTitle());
            item.setToggleGroup(group);
            item.getStyleClass().add("template-item");
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnAction(event -> {
                if (!item.isSelected()) {
                    item.setSelected(true);
                }
                select(template);
            });
            items.put(template, item);
            list.getChildren().add(item);
        }
        list.setMinWidth(230);
        list.setPrefWidth(230);

        editorTitle.getStyleClass().add("template-editor-title");
        editorUse.getStyleClass().add("settings-note");
        editorUse.setWrapText(true);
        subjectField.setPromptText("Asunto del correo");
        messageArea.setPrefRowCount(5);
        messageArea.setWrapText(true);

        FlowPane fields = new FlowPane(6, 6);
        Label fieldsLabel = new Label("Insertar campo:");
        fieldsLabel.getStyleClass().add("settings-note");
        fields.getChildren().add(fieldsLabel);
        for (String field : NotificationTemplate.FIELDS) {
            Button chip = new Button("{" + field + "}");
            chip.getStyleClass().addAll("btn-secondary", "btn-small", "template-field");
            chip.setFocusTraversable(false);
            chip.setOnAction(event -> insertField(field));
            fields.getChildren().add(chip);
        }

        Button original = SettingsForm.button("Usar el texto original", "btn-secondary");
        original.getStyleClass().add("btn-small");
        original.setOnAction(event -> useOriginal());
        HBox titleRow = new HBox(12, new VBox(2, editorTitle, editorUse), spacer(), original);
        titleRow.setAlignment(Pos.TOP_LEFT);

        previewSubject.getStyleClass().add("template-preview-subject");
        previewSubject.setWrapText(true);
        previewMessage.getStyleClass().add("template-preview-message");
        previewMessage.setWrapText(true);
        Label previewTitle = new Label("Vista previa con datos de ejemplo");
        previewTitle.getStyleClass().add("field-label");
        VBox preview = new VBox(6, previewSubject, previewMessage);
        preview.getStyleClass().add("template-preview");

        VBox editor = new VBox(14, titleRow,
                SettingsForm.field("Asunto", subjectField, "Vacío = el asunto original."),
                SettingsForm.field("Mensaje", messageArea, null), fields,
                new VBox(6, previewTitle, preview));
        HBox.setHgrow(editor, Priority.ALWAYS);

        HBox body = new HBox(20, list, editor);
        return SettingsForm.card("Plantillas de correo", IconSet.MAIL,
                SettingsForm.note("Los campos entre llaves se completan con los datos de cada cliente. "
                        + "{taller} toma el nombre de «Datos del taller»."),
                body);
    }

    // ---------- plantillas ----------

    private void select(NotificationTemplate template) {
        keepCurrent();
        selected = template;
        items.get(template).setSelected(true);
        editorTitle.setText(template.getTitle());
        editorUse.setText(useText(template));
        TemplateText draft = drafts.get(template);
        showing = true;
        subjectField.setText(draft == null || draft.subject() == null ? template.getSubject() : draft.subject());
        messageArea.setText(draft == null ? template.getMessage() : draft.message());
        showing = false;
        updatePreview();
    }

    /** Guarda en borrador lo escrito en la plantilla abierta. */
    private void keepCurrent() {
        if (selected == null) {
            return;
        }
        String subject = subjectField.getText();
        String text = messageArea.getText();
        boolean original = (subject == null || subject.isBlank() || subject.trim().equals(selected.getSubject()))
                && text != null && text.trim().equals(selected.getMessage());
        if (original) {
            drafts.remove(selected);
        } else {
            drafts.put(selected, new TemplateText(subject, text));
        }
        markEdited(selected);
    }

    private void onEdit() {
        if (showing || selected == null) {
            return;
        }
        keepCurrent();
        updatePreview();
    }

    private void useOriginal() {
        if (selected == null) {
            return;
        }
        subjectField.setText(selected.getSubject());
        messageArea.setText(selected.getMessage());
    }

    private void insertField(String field) {
        messageArea.insertText(messageArea.getCaretPosition(), "{" + field + "}");
        messageArea.requestFocus();
    }

    private void updatePreview() {
        if (selected == null) {
            return;
        }
        previewSubject.getStyleClass().remove("template-preview-error");
        try {
            TemplateText preview = service().preview(selected, subjectField.getText(), messageArea.getText());
            previewSubject.setText(preview.subject());
            previewMessage.setText(preview.message());
        } catch (ValidationException e) {
            previewSubject.getStyleClass().add("template-preview-error");
            previewSubject.setText(e.getMessage());
            previewMessage.setText("");
        }
    }

    /** Marca en la lista las plantillas con texto propio. */
    private void markEdited(NotificationTemplate template) {
        ToggleButton item = items.get(template);
        item.setText(template.getTitle() + (drafts.containsKey(template) ? "  · editada" : ""));
    }

    /** Dónde se usa cada plantilla. */
    static String useText(NotificationTemplate template) {
        return switch (template) {
            case VEHICLE_READY -> "Se envía sola cuando la orden pasa a Lista.";
            case MAINTENANCE_DUE -> "Se envía con «Enviar recordatorios» del Inicio.";
            case INVOICE_AVAILABLE -> "Se envía sola al generar una factura pendiente; va con la factura adjunta.";
            case INVOICE_PAID -> "Se envía sola al registrar el pago; va con la factura adjunta.";
            case INVOICE_OVERDUE -> "Se envía sola cuando una factura vence; va con la factura adjunta.";
            case SERVICE_UPDATE, MAINTENANCE_INVITE, PROMOTION, GENERAL ->
                    "Mensaje que propone «Nueva notificación» para este tipo.";
        };
    }

    // ---------- datos ----------

    private void load() {
        setBusy(true);
        Background.run(() -> {
                    NotificationSettings settings = service().load();
                    service().workshopName(); // Deja listo el nombre del taller para la vista previa.
                    return settings;
                },
                settings -> {
                    show(settings);
                    setBusy(false);
                },
                error -> {
                    SettingsForm.show(message, SettingsForm.errorText(error), true);
                    show(NotificationSettings.defaults());
                    setBusy(false);
                });
    }

    private void show(NotificationSettings settings) {
        vehicleReady.setSelected(settings.isVehicleReadyEnabled());
        billingNotices.setSelected(settings.isBillingNoticesEnabled());
        attachInvoice.setSelected(settings.isAttachInvoice());
        drafts.clear();
        for (NotificationTemplate template : NotificationTemplate.values()) {
            TemplateText text = settings.getTemplate(template.name());
            if (text != null) {
                drafts.put(template, text);
            }
            markEdited(template);
        }
        NotificationTemplate current = selected == null ? NotificationTemplate.VEHICLE_READY : selected;
        selected = null;
        select(current);
    }

    private void save() {
        keepCurrent();
        NotificationSettings settings = NotificationSettings.defaults();
        settings.setVehicleReadyEnabled(vehicleReady.isSelected());
        settings.setBillingNoticesEnabled(billingNotices.isSelected());
        settings.setAttachInvoice(attachInvoice.isSelected());
        drafts.forEach((template, text) -> settings.setTemplate(template.name(), text));
        setBusy(true);
        Background.run(() -> service().update(settings),
                saved -> {
                    show(saved);
                    setBusy(false);
                    SettingsForm.show(message, "Configuración guardada. Los próximos correos usarán estos textos.", false);
                },
                error -> {
                    setBusy(false);
                    SettingsForm.show(message, SettingsForm.errorText(error), true);
                });
    }

    private void restoreDefaults() {
        if (!ConfirmDialog.confirm("Restaurar valores de fábrica",
                "Se activarán todos los avisos y las nueve plantillas volverán a su texto original. ¿Continuar?")) {
            return;
        }
        setBusy(true);
        Background.run(() -> service().restoreDefaults(),
                saved -> {
                    show(saved);
                    setBusy(false);
                    SettingsForm.show(message, "Se restauraron los valores de fábrica.", false);
                },
                error -> {
                    setBusy(false);
                    SettingsForm.show(message, SettingsForm.errorText(error), true);
                });
    }

    private void setBusy(boolean busy) {
        saveButton.setDisable(busy);
        restoreButton.setDisable(busy);
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static NotificationSettingsService service() {
        return AppContext.getInstance().getNotificationSettingsService();
    }
}
