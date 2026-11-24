package com.autogest.controller.components;

import com.autogest.util.PhoneCountry;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/**
 * Campo de teléfono con el país al lado: el desplegable fija el indicativo y
 * el campo solo acepta dígitos (y espacios), hasta la longitud del país. El
 * texto de ayuda muestra un número de ejemplo del país elegido.
 *
 * <p>Devuelve el teléfono como se guarda ({@code +<indicativo><número>}); las
 * reglas del primer dígito y la longitud las revisa
 * {@link com.autogest.util.ContactValidator} al guardar.</p>
 *
 * <p>Principles: Pure Fabrication, High Cohesion, Low Coupling.</p>
 */
public class PhoneField extends HBox {

    private final ComboBox<PhoneCountry> country = new ComboBox<>(FXCollections.observableArrayList(PhoneCountry.values()));
    private final TextField number = new TextField();

    /** Crea el campo con Colombia como país inicial. */
    public PhoneField() {
        super(8);
        getStyleClass().add("phone-field");
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);

        country.getStyleClass().addAll("input", "phone-country");
        country.setValue(PhoneCountry.DEFAULT);
        country.setAccessibleText("País del teléfono");
        // Cerrado muestra solo el indicativo (+57) para caber en columnas angostas;
        // la lista abierta muestra el país completo.
        country.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PhoneCountry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : "+" + item.dialCode());
            }
        });
        country.valueProperty().addListener((observable, before, now) -> refreshHint());

        number.getStyleClass().addAll("input", "phone-number");
        number.setMaxWidth(Double.MAX_VALUE);
        number.setMinWidth(96);
        HBox.setHgrow(number, Priority.ALWAYS);
        number.setAccessibleText("Número de teléfono");
        // Solo dígitos y espacios, sin pasar del largo del país elegido.
        number.setTextFormatter(new TextFormatter<String>(change -> {
            String next = change.getControlNewText();
            if (!next.matches("[0-9 ]*")) {
                return null;
            }
            return next.replace(" ", "").length() <= selected().maxDigits() ? change : null;
        }));

        getChildren().addAll(country, number);
        refreshHint();
    }

    /**
     * Teléfono como se guarda, por ejemplo {@code +573001234567}; vacío si no
     * se escribió el número.
     *
     * @return teléfono con indicativo, o "" si el número está vacío
     */
    public String getPhone() {
        String digits = number.getText() == null ? "" : number.getText().replace(" ", "");
        return digits.isEmpty() ? "" : "+" + selected().dialCode() + digits;
    }

    /**
     * Muestra un teléfono guardado: elige su país y escribe el número nacional.
     * Un número sin indicativo se muestra como de Colombia.
     *
     * @param stored teléfono guardado (puede ser {@code null})
     */
    public void setPhone(String stored) {
        country.setValue(PhoneCountry.of(stored));
        number.setText(PhoneCountry.nationalPart(stored));
    }

    /** Campo del número (para el foco y los atajos del formulario). */
    public TextField numberField() {
        return number;
    }

    private PhoneCountry selected() {
        return country.getValue() == null ? PhoneCountry.DEFAULT : country.getValue();
    }

    /** Ejemplo y regla del país elegido, en el texto de ayuda y en la ayuda emergente. */
    private void refreshHint() {
        PhoneCountry current = selected();
        number.setPromptText(current.example());
        number.setTooltip(new Tooltip("El teléfono de " + current.label() + " " + current.rule() + "."));
        country.setTooltip(new Tooltip(current.label() + " +" + current.dialCode()));
        String digits = number.getText() == null ? "" : number.getText().replace(" ", "");
        if (digits.length() > current.maxDigits()) {
            number.setText(digits.substring(0, current.maxDigits()));
        }
    }
}
