package com.autogest.service.assistant;

/**
 * Elige el motor del Asistente (Sprint 9, Líder). Si hay un motor con IA
 * configurado, lo usa primero; si no hay, o si falla (sin conexión, sin
 * crédito, una respuesta que no se entiende), responde el motor local de
 * reglas. Así el Asistente funciona siempre, con o sin cuenta de IA.
 *
 * <p>Principles: Strategy, Protected Variations (la interfaz no sabe qué
 * motor respondió).</p>
 */
public class AssistantService {

    /** Motor con IA: convierte un pedido libre en una intención. */
    @FunctionalInterface
    public interface AiEngine {

        /**
         * @param text    lo que escribió el usuario
         * @param catalog módulos que el rol puede abrir
         * @return la intención
         * @throws RuntimeException si no se pudo consultar o la respuesta no sirve
         */
        AssistantIntent interpret(String text, AssistantCatalog catalog);
    }

    /**
     * Respuesta del motor.
     *
     * @param intent intención interpretada
     * @param byAi   {@code true} si la entendió el motor con IA
     * @param note   aviso para el usuario cuando la IA falló y respondió el motor local (o {@code null})
     */
    public record Result(AssistantIntent intent, boolean byAi, String note) {
    }

    private final LocalAssistantInterpreter local = new LocalAssistantInterpreter();
    private final AiEngine ai;

    /**
     * @param ai motor con IA, o {@code null} para usar solo el motor local
     */
    public AssistantService(AiEngine ai) {
        this.ai = ai;
    }

    /** {@code true} si hay un motor con IA configurado. */
    public boolean aiEnabled() {
        return ai != null;
    }

    /**
     * Interpreta un pedido.
     *
     * @param text    lo que escribió el usuario
     * @param catalog módulos que el rol puede abrir
     * @return la intención y qué motor la produjo
     */
    public Result interpret(String text, AssistantCatalog catalog) {
        if (ai != null) {
            try {
                AssistantIntent intent = ai.interpret(text, catalog);
                if (intent != null && valid(intent, catalog)) {
                    return new Result(intent, true, null);
                }
            } catch (RuntimeException e) {
                return new Result(local.interpret(text, catalog), false,
                        "No pude consultar la IA; respondí con las reglas locales.");
            }
        }
        return new Result(local.interpret(text, catalog), false, null);
    }

    /** La IA solo puede proponer módulos del catálogo del rol. */
    static boolean valid(AssistantIntent intent, AssistantCatalog catalog) {
        return switch (intent.kind()) {
            case HELP, UNKNOWN -> true;
            default -> intent.destination() != null && catalog.find(intent.destination()).isPresent();
        };
    }
}
