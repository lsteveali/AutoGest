package com.autogest.util;

import java.util.prefs.Preferences;

/**
 * Lo que este computador recuerda entre sesiones para las Preferencias del
 * sistema: el último módulo que abrió cada usuario («Abrir automáticamente el
 * último módulo utilizado») y los filtros usados en cada lista («Guardar
 * filtros utilizados recientemente»). Se guarda con {@link Preferences} (en
 * Windows, el registro del usuario), no en la base de datos.
 *
 * <p>Si el sistema no deja leer o escribir, se comporta como si no hubiera
 * nada guardado: nunca interrumpe el uso de la aplicación.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public final class LocalMemory {

    private static final String NODE = "com/autogest/local-memory";
    private static final int MAX_KEY = Preferences.MAX_KEY_LENGTH;

    private final Preferences node;

    /** Memoria del usuario actual de este equipo. */
    public LocalMemory() {
        this(Preferences.userRoot().node(NODE));
    }

    /**
     * Memoria sobre un nodo dado (las pruebas usan uno propio).
     *
     * @param node nodo de preferencias
     */
    LocalMemory(Preferences node) {
        this.node = node;
    }

    /**
     * Último módulo que abrió el usuario.
     *
     * @param userId id del usuario
     * @return nombre del módulo, o {@code null} si no hay
     */
    public String lastModule(int userId) {
        return get("module." + userId);
    }

    /**
     * Recuerda el módulo abierto por el usuario.
     *
     * @param userId id del usuario
     * @param module nombre del módulo
     */
    public void rememberModule(int userId, String module) {
        put("module." + userId, module);
    }

    /**
     * Último valor de un filtro de una lista.
     *
     * @param screen pantalla (por ejemplo {@code ORDERS})
     * @param filter etiqueta del filtro (por ejemplo «Estado»)
     * @return valor guardado, o {@code null} si no hay
     */
    public String filter(String screen, String filter) {
        return get(filterKey(screen, filter));
    }

    /**
     * Recuerda el valor de un filtro de una lista.
     *
     * @param screen pantalla
     * @param filter etiqueta del filtro
     * @param value  valor elegido ({@code null} o vacío lo olvida)
     */
    public void rememberFilter(String screen, String filter, String value) {
        put(filterKey(screen, filter), value);
    }

    private static String filterKey(String screen, String filter) {
        String key = "filter." + screen + "." + filter;
        return key.length() > MAX_KEY ? key.substring(0, MAX_KEY) : key;
    }

    private String get(String key) {
        try {
            return node.get(key, null);
        } catch (RuntimeException error) {
            return null;
        }
    }

    private void put(String key, String value) {
        try {
            if (value == null || value.isEmpty()) {
                node.remove(key);
            } else {
                node.put(key, value.length() > Preferences.MAX_VALUE_LENGTH
                        ? value.substring(0, Preferences.MAX_VALUE_LENGTH) : value);
            }
        } catch (RuntimeException error) {
            // Sin acceso al registro: la aplicación sigue sin recordar.
        }
    }
}
