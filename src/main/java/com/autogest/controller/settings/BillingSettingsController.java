package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.components.ConfirmDialog;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.exception.ValidationException;
import com.autogest.model.BillingSettings;
import com.autogest.service.BillingSettingsService;
import com.autogest.service.InvoiceService;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Pantalla Configuración → Facturación (Sprint 8, Líder): tasa de IVA, plazo
 * de pago, prefijo de la numeración, métodos de pago aceptados y nota de la
 * factura. Muestra un ejemplo que se actualiza mientras se escribe. Las
 * reglas viven en {@link BillingSettingsService}.
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class BillingSettingsController implements Initializable {

    private static final BigDecimal EXAMPLE_SUBTOTAL = new BigDecimal("100000");

    @FXML
    private VBox root;

    private final TextField taxField = new TextField();
    private final TextField dueDaysField = new TextField();
    private final TextField prefixField = new TextField();
    private final TextArea noteArea = new TextArea();
    private final Map<String, CheckBox> methodBoxes = new LinkedHashMap<>();
    private final Label example = new Label();
    private final Label numbering = new Label();
    private final Label noteCount = new Label();
    private Label message;
    private Button saveButton;
    private Button restoreButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Configuración de facturación",
                "Define el IVA, el plazo de pago, la numeración y los métodos de pago de las facturas.");
        header.setBreadcrumbs(List.of("Configuración", "Facturación"));

        VBox left = new VBox(16, taxCard(), numberingCard());
        VBox right = new VBox(16, methodsCard(), noteCard());
        message = SettingsForm.message();
        restoreButton = SettingsForm.button("Restaurar valores de fábrica", "btn-secondary");
        restoreButton.setOnAction(event -> restoreDefaults());
        saveButton = SettingsForm.button("Guardar cambios", "btn-primary");
        saveButton.setOnAction(event -> save());

        root.getChildren().addAll(header, SettingsForm.columns(left, right),
                SettingsForm.actions(message, restoreButton, saveButton));
        taxField.textProperty().addListener((obs, old, value) -> updateExample());
        dueDaysField.textProperty().addListener((obs, old, value) -> updateExample());
        prefixField.textProperty().addListener((obs, old, value) -> updateNumbering());
        noteArea.textProperty().addListener((obs, old, value) -> updateNoteCount());
        load();
    }

    // ---------- tarjetas ----------

    private VBox taxCard() {
        taxField.setPromptText("0");
        dueDaysField.setPromptText(String.valueOf(BillingSettings.DEFAULT_DUE_DAYS));
        example.setWrapText(true);
        example.getStyleClass().add("billing-settings-example");
        return SettingsForm.card("Impuestos y plazo de pago", IconSet.CREDIT_CARD,
                SettingsForm.field("IVA (%)", taxField,
                        "Se aplica sobre el subtotal de las facturas nuevas. Las ya emitidas conservan su tasa."),
                SettingsForm.field("Plazo de pago (días)", dueDaysField,
                        "Días entre la emisión y el vencimiento. Se puede cambiar en cada factura."),
                example);
    }

    private VBox numberingCard() {
        prefixField.setPromptText(BillingSettings.DEFAULT_PREFIX);
        numbering.setWrapText(true);
        numbering.getStyleClass().add("billing-settings-example");
        return SettingsForm.card("Numeración", IconSet.FILE_TEXT,
                SettingsForm.field("Prefijo", prefixField, "De 1 a 6 letras o números, por ejemplo FAC o FE."),
                numbering);
    }

    private VBox methodsCard() {
        VBox boxes = new VBox(10);
        for (String code : BillingSettings.ALL_PAYMENT_METHODS) {
            CheckBox box = new CheckBox(InvoiceService.methodText(code));
            methodBoxes.put(code, box);
            boxes.getChildren().add(box);
        }
        return SettingsForm.card("Métodos de pago", IconSet.CREDIT_CARD,
                SettingsForm.note("Los que se pueden elegir al registrar un pago. Las facturas ya pagadas no cambian."),
                boxes);
    }

    private VBox noteCard() {
        noteArea.setPrefRowCount(4);
        noteArea.setWrapText(true);
        noteArea.setPromptText("Por ejemplo: Transferencias a la cuenta de ahorros Bancolombia 123-456789-00.");
        noteCount.getStyleClass().add("settings-note");
        HBox counter = new HBox(noteCount);
        counter.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        return SettingsForm.card("Nota en la factura", IconSet.EDIT,
                SettingsForm.field("Texto", noteArea,
                        "Se imprime al final del PDF y de la imagen de cada factura (condiciones, datos para transferir…)."),
                counter);
    }

    // ---------- datos ----------

    private void load() {
        setBusy(true);
        Background.run(() -> service().load(),
                settings -> {
                    show(settings);
                    setBusy(false);
                },
                error -> {
                    SettingsForm.show(message, SettingsForm.errorText(error), true);
                    setBusy(false);
                });
    }

    private void show(BillingSettings settings) {
        taxField.setText(settings.getTaxPercent().stripTrailingZeros().toPlainString().replace('.', ','));
        dueDaysField.setText(String.valueOf(settings.getDueDays()));
        prefixField.setText(settings.getNumberPrefix());
        methodBoxes.forEach((code, box) -> box.setSelected(settings.getPaymentMethods().contains(code)));
        noteArea.setText(settings.getInvoiceNote() == null ? "" : settings.getInvoiceNote());
        updateExample();
        updateNumbering();
        updateNoteCount();
    }

    private void save() {
        BillingSettings settings;
        try {
            settings = readForm();
        } catch (ValidationException e) {
            SettingsForm.show(message, e.getMessage(), true);
            return;
        }
        setBusy(true);
        Background.run(() -> service().update(settings),
                saved -> {
                    show(saved);
                    setBusy(false);
                    SettingsForm.show(message, "Configuración guardada. Se aplica a las facturas nuevas.", false);
                },
                error -> {
                    setBusy(false);
                    SettingsForm.show(message, SettingsForm.errorText(error), true);
                });
    }

    private void restoreDefaults() {
        if (!ConfirmDialog.confirm("Restaurar valores de fábrica",
                "Se volverá a IVA 0 %, 14 días de plazo, prefijo FAC, todos los métodos de pago y sin nota. "
                        + "Las facturas ya emitidas no cambian. ¿Continuar?")) {
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

    /** Lee el formulario; los números mal escritos se avisan aquí, el resto lo valida el servicio. */
    private BillingSettings readForm() {
        BillingSettings settings = new BillingSettings();
        settings.setTaxPercent(parseTax(taxField.getText()));
        settings.setDueDays(parseDays(dueDaysField.getText()));
        settings.setNumberPrefix(prefixField.getText());
        List<String> methods = new ArrayList<>();
        methodBoxes.forEach((code, box) -> {
            if (box.isSelected()) {
                methods.add(code);
            }
        });
        settings.setPaymentMethods(methods);
        settings.setInvoiceNote(noteArea.getText());
        return settings;
    }

    static BigDecimal parseTax(String text) {
        String clean = text == null ? "" : text.replace("%", "").replace(',', '.').trim();
        if (clean.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(clean);
        } catch (NumberFormatException e) {
            throw new ValidationException("Escribe el IVA como un número, por ejemplo 19 o 19,5");
        }
    }

    static int parseDays(String text) {
        String clean = text == null ? "" : text.trim();
        try {
            return Integer.parseInt(clean);
        } catch (NumberFormatException e) {
            throw new ValidationException("Escribe el plazo de pago en días, por ejemplo 14");
        }
    }

    // ---------- ejemplos ----------

    private void updateExample() {
        try {
            BigDecimal tax = EXAMPLE_SUBTOTAL.multiply(parseTax(taxField.getText()))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            int days = parseDays(dueDaysField.getText());
            example.setText("Ejemplo: una factura de " + Formatter.money(EXAMPLE_SUBTOTAL) + " emitida hoy lleva "
                    + Formatter.money(tax) + " de IVA, suma " + Formatter.money(EXAMPLE_SUBTOTAL.add(tax))
                    + " y vence el " + Formatter.date(LocalDate.now().plusDays(days)) + ".");
        } catch (ValidationException e) {
            example.setText(e.getMessage() + ".");
        }
    }

    private void updateNumbering() {
        String prefix = prefixField.getText() == null || prefixField.getText().isBlank()
                ? "FAC" : prefixField.getText().trim().toUpperCase();
        numbering.setText("Las facturas nuevas se numerarán " + prefix + "-" + LocalDate.now().getYear()
                + "-001, -002… El consecutivo continúa aunque cambie el prefijo.");
    }

    private void updateNoteCount() {
        int length = noteArea.getText() == null ? 0 : noteArea.getText().trim().length();
        noteCount.setText(length + " / " + BillingSettingsService.MAX_NOTE);
    }

    private void setBusy(boolean busy) {
        saveButton.setDisable(busy);
        restoreButton.setDisable(busy);
    }

    private static BillingSettingsService service() {
        return AppContext.getInstance().getBillingSettingsService();
    }
}
