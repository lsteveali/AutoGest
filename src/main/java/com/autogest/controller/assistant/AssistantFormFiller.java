package com.autogest.controller.assistant;

import com.autogest.service.assistant.LocalAssistantInterpreter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Aplica una {@link AssistantAction} sobre la pantalla ya cargada, como lo
 * haría el usuario: escribe en el buscador de la {@code FilterBar}, elige las
 * opciones de sus filtros y precarga los campos de un formulario buscando su
 * etiqueta visible ({@code .field-label}). No pulsa ningún botón: guardar
 * siempre lo decide el usuario.
 *
 * <p>Las opciones que la pantalla carga en segundo plano (clientes,
 * vehículos, marcas) se reintentan cada 200 ms durante unos segundos.</p>
 *
 * <p>Principles: Pure Fabrication, Low Coupling (funciona con todas las
 * listas y formularios sin que cada pantalla lo implemente).</p>
 */
public final class AssistantFormFiller {

    private static final Duration RETRY_EVERY = Duration.millis(200);
    private static final int MAX_ATTEMPTS = 25;

    private AssistantFormFiller() {
        // Clase de utilidad: no se instancia.
    }

    /** Un valor pendiente de aplicar. */
    private interface Pending {
        /** @return {@code true} si ya se aplicó (o no hay dónde aplicarlo). */
        boolean tryApply();
    }

    /**
     * Aplica la búsqueda, los filtros y los campos de la acción.
     *
     * @param root   raíz de la pantalla cargada
     * @param action acción del Asistente
     */
    public static void apply(Parent screen, AssistantAction action) {
        List<Pending> pending = new ArrayList<>();
        // Las pantallas son un ScrollPane: su contenido no es un hijo hasta que se dibuja, así que se busca en él.
        Parent root = screen instanceof ScrollPane scroll && scroll.getContent() instanceof Parent content
                ? content : screen;
        if (action.search() != null && !action.search().isBlank()) {
            pending.add(() -> {
                Node node = root.lookup(".filter-field");
                if (node instanceof TextField field) {
                    field.setText(action.search());
                    return true;
                }
                return false;
            });
        }
        for (Map.Entry<String, String> filter : action.filters().entrySet()) {
            pending.add(() -> selectFilter(root, filter.getKey(), filter.getValue()));
        }
        for (Map.Entry<String, String> field : action.fields().entrySet()) {
            pending.add(() -> fillField(root, field.getKey(), field.getValue()));
        }
        if (pending.isEmpty()) {
            return;
        }
        runUntilApplied(pending);
    }

    private static void runUntilApplied(List<Pending> pending) {
        pending.removeIf(Pending::tryApply);
        if (pending.isEmpty()) {
            return;
        }
        int[] attempts = {0};
        Timeline retry = new Timeline();
        retry.getKeyFrames().add(new KeyFrame(RETRY_EVERY, event -> {
            attempts[0]++;
            // En orden: un campo que depende de otro (tipo → mensaje) se aplica después.
            for (Iterator<Pending> it = pending.iterator(); it.hasNext(); ) {
                if (it.next().tryApply()) {
                    it.remove();
                }
            }
            if (pending.isEmpty() || attempts[0] >= MAX_ATTEMPTS) {
                retry.stop();
            }
        }));
        retry.setCycleCount(Timeline.INDEFINITE);
        retry.play();
    }

    // ---------- filtros de la lista ----------

    /** Elige la opción del filtro con esa etiqueta en la {@code FilterBar}. */
    static boolean selectFilter(Parent root, String label, String option) {
        Node bar = root.lookup(".filter-bar");
        if (!(bar instanceof Pane pane)) {
            // La barra aún no se arma (o la pantalla no tiene): se reintenta.
            return false;
        }
        List<Node> children = pane.getChildren();
        for (int i = 0; i + 1 < children.size(); i++) {
            if (children.get(i) instanceof Label text && same(text.getText(), label)
                    && children.get(i + 1) instanceof ComboBox<?> combo) {
                return select(combo, option);
            }
        }
        // El filtro no existe en esta pantalla: no hay nada que esperar.
        return true;
    }

    // ---------- campos del formulario ----------

    /** Precarga el campo cuya etiqueta coincide; devuelve {@code false} si aún no está listo. */
    static boolean fillField(Parent root, String label, String value) {
        for (Node node : root.lookupAll(".field-label")) {
            if (node instanceof Label text && same(text.getText(), label)) {
                Node input = inputFor(text);
                if (input instanceof TextInputControl field) {
                    if (field.isEditable() && !field.isDisabled()) {
                        field.setText(value);
                    }
                    return true;
                }
                if (input instanceof ComboBox<?> combo) {
                    return select(combo, value);
                }
                return true;
            }
        }
        // La etiqueta aún no se dibuja (pantallas que arman el formulario después) o no existe.
        return false;
    }

    /** El control que acompaña a la etiqueta: el siguiente en su contenedor (o en el del grupo etiqueta + «*»). */
    private static Node inputFor(Label label) {
        Node current = label;
        for (int level = 0; level < 3 && current.getParent() instanceof Pane parent; level++) {
            List<Node> siblings = parent.getChildren();
            int index = siblings.indexOf(current);
            for (int i = index + 1; i < siblings.size(); i++) {
                Node candidate = siblings.get(i);
                if (candidate instanceof TextInputControl || candidate instanceof ComboBox<?>) {
                    return candidate;
                }
            }
            current = parent;
        }
        return null;
    }

    /**
     * Elige en el combo la opción cuyo texto visible coincide (o contiene el
     * valor). Si todavía no tiene opciones, devuelve {@code false} para reintentar.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static boolean select(ComboBox combo, String value) {
        if (combo.isDisabled()) {
            return true;
        }
        if (combo.getItems().isEmpty()) {
            if (combo.isEditable()) {
                combo.getEditor().setText(value);
                return true;
            }
            return false;
        }
        String wanted = LocalAssistantInterpreter.normalize(value);
        Object partial = null;
        for (Object item : combo.getItems()) {
            String text = LocalAssistantInterpreter.normalize(display(combo, item));
            if (text.equals(wanted)) {
                combo.setValue(item);
                return true;
            }
            if (partial == null && text.contains(wanted)) {
                partial = item;
            }
        }
        if (partial != null) {
            combo.setValue(partial);
            return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String display(ComboBox combo, Object item) {
        return combo.getConverter() != null ? combo.getConverter().toString(item) : String.valueOf(item);
    }

    /** Misma etiqueta sin el «*» de obligatorio, sin tildes ni mayúsculas. */
    static boolean same(String label, String key) {
        String a = LocalAssistantInterpreter.normalize(label == null ? "" : label.replace("*", ""));
        String b = LocalAssistantInterpreter.normalize(key);
        return !a.isEmpty() && a.equals(b);
    }
}
