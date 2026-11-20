package com.autogest.util;

import com.autogest.model.SystemPreferences;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Preferencias del sistema vigentes en esta sesión de AutoGest (las de
 * Configuración → Preferencias del sistema, de Dev 1). Se cargan al iniciar
 * sesión y se renuevan al guardar la pantalla; quien necesita reaccionar al
 * cambio (por ejemplo, el cierre de sesión por inactividad) se suscribe.
 *
 * <p>Hasta que se cargan, o si no se pueden leer, valen las de fábrica.</p>
 *
 * <p>Principles: Pure Fabrication, Low Coupling (los componentes no dependen
 * del servicio ni de la base de datos).</p>
 */
public final class AppPreferences {

    /** Registros por página de las listas cuando no hay preferencia. */
    public static final int DEFAULT_PAGE_SIZE = 10;

    private static volatile SystemPreferences current = SystemPreferences.defaults();
    private static final List<Consumer<SystemPreferences>> LISTENERS = new CopyOnWriteArrayList<>();

    private AppPreferences() {
        // Clase de utilidad: no se instancia.
    }

    /** Preferencias vigentes. */
    public static SystemPreferences current() {
        return current;
    }

    /**
     * Cambia las preferencias vigentes y avisa a los suscriptores.
     *
     * @param preferences preferencias nuevas ({@code null} = las de fábrica)
     */
    public static void update(SystemPreferences preferences) {
        current = preferences == null ? SystemPreferences.defaults() : preferences;
        for (Consumer<SystemPreferences> listener : LISTENERS) {
            listener.accept(current);
        }
    }

    /**
     * Se suscribe a los cambios.
     *
     * @param listener recibe las preferencias nuevas
     */
    public static void addListener(Consumer<SystemPreferences> listener) {
        LISTENERS.add(listener);
    }

    /** Quita una suscripción. */
    public static void removeListener(Consumer<SystemPreferences> listener) {
        LISTENERS.remove(listener);
    }

    /** Registros por página de las listas (10, 25, 50 o 100; 10 si el valor no es válido). */
    public static int pageSize() {
        int size = current.getPageSize();
        return size > 0 ? size : DEFAULT_PAGE_SIZE;
    }

    /** Pedir confirmación antes de eliminar. */
    public static boolean confirmBeforeDelete() {
        return current.isConfirmBeforeDelete();
    }

    /** Mostrar el mensaje «se guardó correctamente». */
    public static boolean showSaveConfirmation() {
        return current.isShowSaveConfirmation();
    }

    /**
     * Minutos de inactividad antes de cerrar la sesión, o 0 si el cierre
     * automático está apagado.
     */
    public static int inactivityMinutes() {
        return current.isAutoLogout() && current.getInactivityMinutes() > 0 ? current.getInactivityMinutes() : 0;
    }
}
