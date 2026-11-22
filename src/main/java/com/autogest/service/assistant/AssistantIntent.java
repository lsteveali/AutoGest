package com.autogest.service.assistant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lo que el usuario le pidió al Asistente, ya interpretado y sin depender de
 * la interfaz: qué hacer, en qué módulo y con qué datos. La produce el motor
 * local de reglas o el motor con IA; la interfaz la convierte después en
 * navegación (abrir, filtrar, precargar). Nunca guarda ni cambia nada.
 *
 * @param kind        qué se quiere hacer
 * @param destination código del módulo (por ejemplo {@code ORDERS}); {@code null} si no se reconoció
 * @param search      texto para el buscador de la lista (puede ser {@code null})
 * @param filters     filtros de la lista: etiqueta visible del filtro → opción visible
 * @param reference   registro concreto: placa, número de orden o factura, nombre (puede ser {@code null})
 * @param fields      campos para precargar un formulario: etiqueta visible → valor
 * @param message     respuesta para el usuario (puede ser {@code null}; la interfaz arma una)
 */
public record AssistantIntent(Kind kind, String destination, String search, Map<String, String> filters,
                              String reference, Map<String, String> fields, String message) {

    /** Qué se quiere hacer. */
    public enum Kind {
        /** Abrir un módulo. */
        OPEN,
        /** Abrir la lista de un módulo con filtros o búsqueda. */
        FILTER,
        /** Abrir el detalle de un registro concreto. */
        RECORD,
        /** Abrir un formulario de creación precargado (sin guardar). */
        CREATE,
        /** El usuario pide ayuda o saluda. */
        HELP,
        /** No se entendió el pedido. */
        UNKNOWN
    }

    /**
     * Copias inmutables que conservan el orden (al precargar un formulario
     * importa: el tipo de aviso va antes que el mensaje). Los mapas nulos
     * quedan vacíos.
     */
    public AssistantIntent {
        filters = filters == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(filters));
        fields = fields == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /** Pedido no entendido, con un mensaje para el usuario. */
    public static AssistantIntent unknown(String message) {
        return new AssistantIntent(Kind.UNKNOWN, null, null, Map.of(), null, Map.of(), message);
    }

    /** Pedido de ayuda. */
    public static AssistantIntent help() {
        return new AssistantIntent(Kind.HELP, null, null, Map.of(), null, Map.of(), null);
    }
}
