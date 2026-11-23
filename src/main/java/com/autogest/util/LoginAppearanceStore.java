package com.autogest.util;

import com.autogest.util.LoginAppearance.Vehicle;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Guarda la personalización del inicio de sesión en este computador con
 * {@link Preferences} (en Windows, el registro del usuario). No usa la base de
 * datos: el login la lee antes de que haya sesión, y cada equipo conserva la
 * suya. Si lo guardado no se puede leer, se usa {@link LoginAppearance#DEFAULT}.
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public final class LoginAppearanceStore {

    static final String VEHICLE_KEY = "vehicle";
    static final String COLOR_KEY = "color";
    private static final String NODE = "com/autogest/login-appearance";

    private final Preferences node;

    /** Almacén del usuario actual de este equipo. */
    public LoginAppearanceStore() {
        this(Preferences.userRoot().node(NODE));
    }

    /**
     * Almacén sobre un nodo dado (las pruebas usan uno propio).
     *
     * @param node nodo de preferencias
     */
    LoginAppearanceStore(Preferences node) {
        this.node = node;
    }

    /**
     * Lee la personalización guardada.
     *
     * @return lo guardado, o la de fábrica si no hay nada o no se puede leer
     */
    public LoginAppearance load() {
        try {
            String vehicle = node.get(VEHICLE_KEY, null);
            String color = node.get(COLOR_KEY, null);
            if (vehicle == null && color == null) {
                return LoginAppearance.DEFAULT;
            }
            return new LoginAppearance(parseVehicle(vehicle), color);
        } catch (RuntimeException error) {
            return LoginAppearance.DEFAULT;
        }
    }

    /**
     * Guarda la personalización en este equipo.
     *
     * @param appearance lo que se quiere guardar
     * @throws IllegalStateException si el sistema no permite escribirla
     */
    public void save(LoginAppearance appearance) {
        LoginAppearance value = appearance == null ? LoginAppearance.DEFAULT : appearance;
        try {
            node.put(VEHICLE_KEY, value.vehicle().name());
            node.put(COLOR_KEY, value.color());
            node.flush();
        } catch (BackingStoreException | RuntimeException error) {
            throw new IllegalStateException("No se pudo guardar la personalización en este equipo.", error);
        }
    }

    private static Vehicle parseVehicle(String text) {
        if (text == null) {
            return Vehicle.TRUCK;
        }
        if (text.equals("SEDAN")) {
            // El sedán hecho por código se reemplazó por el clásico importado.
            return Vehicle.CLASSIC;
        }
        try {
            return Vehicle.valueOf(text);
        } catch (IllegalArgumentException unknown) {
            return Vehicle.TRUCK;
        }
    }
}
