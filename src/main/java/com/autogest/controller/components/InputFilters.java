package com.autogest.controller.components;

import com.autogest.util.ContactValidator;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;

/**
 * Filtros de escritura para los campos de los formularios: impiden teclear lo
 * que el servicio igual rechazaría (por ejemplo, números en un nombre).
 *
 * <p>Principles: Pure Fabrication, DRY.</p>
 */
public final class InputFilters {

    private InputFilters() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Deja escribir solo letras (con tildes y ñ) y espacios, hasta un largo
     * máximo. Lo pegado se limpia: se quitan los números y los símbolos.
     *
     * @param field     campo de nombres o apellidos
     * @param maxLength largo máximo (el de la columna de la base de datos)
     */
    public static void lettersOnly(TextInputControl field, int maxLength) {
        field.setTextFormatter(new TextFormatter<String>(change -> {
            if (change.isAdded()) {
                StringBuilder kept = new StringBuilder();
                for (char c : change.getText().toCharArray()) {
                    if (ContactValidator.isNameCharacter(c)) {
                        kept.append(c);
                    }
                }
                change.setText(kept.toString());
                change.setCaretPosition(change.getRangeStart() + kept.length());
                change.setAnchor(change.getCaretPosition());
            }
            return change.getControlNewText().length() <= maxLength ? change : null;
        }));
    }
}
