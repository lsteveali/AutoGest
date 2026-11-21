package com.autogest.controller.assistant;

import com.autogest.controller.View;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Una acción de navegación que el Asistente puede ejecutar: abrir una
 * pantalla (con el id que reciben los detalles y algunos formularios), escribir
 * en el buscador de la lista, elegir filtros y precargar campos. Ninguna
 * guarda ni cambia datos.
 *
 * @param label     texto del botón («Abrir la orden #0045»)
 * @param view      pantalla destino
 * @param parameter id que recibe la pantalla ({@code ParameterizedController}), o {@code null}
 * @param search    texto para el buscador de la lista, o {@code null}
 * @param filters   filtros de la lista: etiqueta visible → opción visible
 * @param fields    campos del formulario: etiqueta visible → valor
 */
public record AssistantAction(String label, View view, Integer parameter, String search,
                              Map<String, String> filters, Map<String, String> fields) {

    /** Copias inmutables que conservan el orden. */
    public AssistantAction {
        filters = filters == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(filters));
        fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /** Abrir una pantalla sin más. */
    public static AssistantAction open(String label, View view) {
        return new AssistantAction(label, view, null, null, Map.of(), Map.of());
    }

    /** Abrir el detalle de un registro. */
    public static AssistantAction record(String label, View view, int id) {
        return new AssistantAction(label, view, id, null, Map.of(), Map.of());
    }

    /**
     * Respuesta del Asistente: un mensaje y las acciones propuestas. La
     * primera se ejecuta sola; las demás quedan como botones.
     *
     * @param message texto para el usuario
     * @param actions acciones, la principal primero (puede estar vacía)
     */
    public record Plan(String message, List<AssistantAction> actions) {

        /** Copia inmutable. */
        public Plan {
            actions = List.copyOf(actions);
        }

        /** Solo un mensaje, sin acciones. */
        public static Plan message(String message) {
            return new Plan(message, List.of());
        }
    }
}
