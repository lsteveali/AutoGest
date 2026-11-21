package com.autogest.controller.assistant;

import com.autogest.controller.assistant.AssistantAction.Plan;
import com.autogest.controller.components.IconSet;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.User;
import com.autogest.service.assistant.AssistantCatalog;
import com.autogest.service.assistant.AssistantService;
import com.autogest.util.Background;
import com.autogest.util.Session;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Predicate;

/**
 * Panel del Asistente AutoGest (Sprint 9, Líder): una conversación al costado
 * del contenido. Lo que se escribe se interpreta ({@link AssistantService}),
 * se convierte en acciones permitidas para el rol ({@link AssistantResolver})
 * y la acción principal se ejecuta sola; las demás quedan como botones. El
 * panel sigue abierto mientras se navega, así la conversación no se pierde.
 *
 * <p>Nunca guarda ni cambia datos: solo abre pantallas, filtra y precarga.</p>
 *
 * <p>Principles: GRASP Controller, Low Coupling (ejecutar una acción lo
 * decide el shell, que aplica los permisos).</p>
 */
public class AssistantPanel extends VBox {

    private final AssistantService service;
    private final AssistantResolver resolver;
    private final Predicate<AssistantAction> runner;
    private final VBox messages = new VBox(12);
    private final ScrollPane scroll = new ScrollPane(messages);
    private final TextField input = new TextField();
    private final Button send = new Button();
    private final Label engine = new Label();
    private Runnable onClose;

    /**
     * @param service  motor que interpreta los pedidos
     * @param resolver convierte la intención en acciones
     * @param runner   ejecuta una acción en el shell; devuelve {@code false} si el rol no puede abrirla
     */
    public AssistantPanel(AssistantService service, AssistantResolver resolver, Predicate<AssistantAction> runner) {
        this.service = service;
        this.resolver = resolver;
        this.runner = runner;
        getStyleClass().add("assistant-panel");
        setMaxWidth(400);
        setPrefWidth(400);
        setMinWidth(Region.USE_PREF_SIZE);

        messages.getStyleClass().add("assistant-messages");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("assistant-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        getChildren().addAll(header(), scroll, inputRow());
        welcome();
    }

    /** Acción al pulsar la «X» del encabezado. */
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    /** Lleva el foco al campo de texto. */
    public void focusInput() {
        Platform.runLater(input::requestFocus);
    }

    // ---------- armado ----------

    private Node header() {
        Node icon = IconSet.icon(IconSet.SPARKLES);
        icon.getStyleClass().add("assistant-header-icon");
        Label title = new Label("Asistente AutoGest");
        title.getStyleClass().add("assistant-title");
        engine.setText(service.aiEnabled() ? "Con IA (" + service.aiName() + ")" : "Reglas locales, sin internet");
        engine.getStyleClass().add("assistant-engine");
        VBox titles = new VBox(2, title, engine);

        Button close = new Button();
        close.setGraphic(IconSet.icon(IconSet.X));
        close.getStyleClass().add("icon-button");
        close.setAccessibleText("Cerrar el Asistente");
        close.setOnAction(event -> {
            if (onClose != null) {
                onClose.run();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(10, icon, titles, spacer, close);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("assistant-header");
        return header;
    }

    private Node inputRow() {
        input.setPromptText("Escribe qué necesitas…");
        input.getStyleClass().addAll("input", "assistant-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        input.setOnAction(event -> submit());
        send.setGraphic(IconSet.icon(IconSet.ARROW_RIGHT));
        send.getStyleClass().addAll("btn-primary", "assistant-send");
        send.setAccessibleText("Enviar");
        send.setOnAction(event -> submit());
        send.disableProperty().bind(input.textProperty().isEmpty());
        Label note = new Label("No guarda nada: los formularios quedan listos para que tú confirmes.");
        note.setWrapText(true);
        note.setMinHeight(Region.USE_PREF_SIZE);
        note.getStyleClass().add("assistant-footnote");
        HBox row = new HBox(8, input, send);
        row.setAlignment(Pos.CENTER);
        VBox box = new VBox(6, row, note);
        box.setMinHeight(Region.USE_PREF_SIZE);
        box.getStyleClass().add("assistant-input-row");
        return box;
    }

    private void welcome() {
        VBox bubble = bubble(false, "Hola. Puedo llevarte a cualquier módulo, abrir listas ya filtradas, abrir un "
                + "registro por su placa o número y dejarte formularios precargados. Prueba con:");
        FlowPane chips = new FlowPane(6, 6);
        for (String example : AssistantResolver.EXAMPLES) {
            Button chip = new Button(example);
            chip.getStyleClass().add("assistant-chip");
            chip.setOnAction(event -> {
                input.setText(example);
                submit();
            });
            chips.getChildren().add(chip);
        }
        bubble.getChildren().add(chips);
    }

    // ---------- conversación ----------

    private void submit() {
        String text = input.getText() == null ? "" : input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        input.clear();
        bubble(true, text);
        User user = Session.getCurrentUser();
        Role role = user == null ? null : user.getRole();
        if (role == null) {
            bubble(false, "Inicia sesión para usar el Asistente.");
            return;
        }
        Label thinking = new Label("Buscando…");
        thinking.getStyleClass().add("assistant-thinking");
        messages.getChildren().add(thinking);
        scrollToEnd();
        input.setDisable(true);
        AssistantCatalog catalog = AssistantDestinations.catalogFor(role);
        Background.run(() -> {
                    AssistantService.Result result = service.interpret(text, catalog);
                    Plan plan = resolver.resolve(result.intent(), role);
                    return new Object[] {plan, result.note()};
                },
                answer -> {
                    messages.getChildren().remove(thinking);
                    input.setDisable(false);
                    show((Plan) answer[0], (String) answer[1]);
                    input.requestFocus();
                },
                error -> {
                    messages.getChildren().remove(thinking);
                    input.setDisable(false);
                    bubble(false, "No pude completar la búsqueda. Revisa la conexión e inténtalo de nuevo.");
                });
    }

    /** Muestra la respuesta y ejecuta la acción principal. */
    private void show(Plan plan, String note) {
        VBox bubble = bubble(false, plan.message());
        if (note != null) {
            Label noteLabel = new Label(note);
            noteLabel.setWrapText(true);
            noteLabel.getStyleClass().add("assistant-footnote");
            bubble.getChildren().add(noteLabel);
        }
        List<AssistantAction> actions = plan.actions();
        for (int i = 0; i < actions.size(); i++) {
            AssistantAction action = actions.get(i);
            Button button = new Button(action.label());
            button.setGraphic(IconSet.icon(i == 0 ? IconSet.CHECK : IconSet.ARROW_RIGHT));
            button.getStyleClass().addAll("btn-secondary", "btn-small", "assistant-action");
            button.setWrapText(true);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(event -> run(action));
            bubble.getChildren().add(button);
        }
        if (!actions.isEmpty()) {
            run(actions.get(0));
        }
    }

    private void run(AssistantAction action) {
        if (!runner.test(action)) {
            bubble(false, "Tu rol no tiene acceso a esa sección.");
        }
    }

    private VBox bubble(boolean fromUser, String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("assistant-text");
        VBox bubble = new VBox(8, label);
        bubble.getStyleClass().addAll("assistant-bubble", fromUser ? "assistant-bubble-user" : "assistant-bubble-bot");
        bubble.setMaxWidth(fromUser ? 300 : Double.MAX_VALUE);
        HBox row = new HBox(bubble);
        row.setAlignment(fromUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        HBox.setHgrow(bubble, fromUser ? Priority.NEVER : Priority.ALWAYS);
        messages.getChildren().add(row);
        scrollToEnd();
        return bubble;
    }

    private void scrollToEnd() {
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }
}
