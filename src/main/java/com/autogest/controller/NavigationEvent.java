package com.autogest.controller;

import javafx.event.Event;
import javafx.event.EventType;

/**
 * Pedido de navegación que sube por el árbol de nodos desde una pantalla
 * hasta el shell: «volver» o «ir a una sección». Así un componente como la
 * cabecera de página puede ofrecer «Volver» y migas de pan clicables sin
 * conocer al {@link ViewNavigator}; {@code MainController} lo atiende y
 * aplica los permisos.
 *
 * <p>Principles: Indirection, Low Coupling.</p>
 */
public class NavigationEvent extends Event {

    /** Tipo raíz de los eventos de navegación. */
    public static final EventType<NavigationEvent> ANY = new EventType<>(Event.ANY, "AUTOGEST_NAVIGATION");
    /** Volver a la pantalla anterior. */
    public static final EventType<NavigationEvent> BACK = new EventType<>(ANY, "AUTOGEST_NAVIGATION_BACK");
    /** Ir a una sección concreta. */
    public static final EventType<NavigationEvent> OPEN = new EventType<>(ANY, "AUTOGEST_NAVIGATION_OPEN");
    /** Cerrar la sesión sin preguntar (por ejemplo, tras restaurar una copia de seguridad). */
    public static final EventType<NavigationEvent> END_SESSION =
            new EventType<>(ANY, "AUTOGEST_NAVIGATION_END_SESSION");

    private final transient View view;

    private NavigationEvent(EventType<NavigationEvent> type, View view) {
        super(type);
        this.view = view;
    }

    /** Evento para volver a la pantalla anterior. */
    public static NavigationEvent back() {
        return new NavigationEvent(BACK, null);
    }

    /**
     * Evento para abrir una sección.
     *
     * @param view pantalla destino
     */
    public static NavigationEvent open(View view) {
        return new NavigationEvent(OPEN, view);
    }

    /**
     * Evento para cerrar la sesión y volver al inicio de sesión. Se usa cuando
     * los datos del usuario pueden haber cambiado por debajo, como al
     * restaurar una copia de seguridad.
     */
    public static NavigationEvent endSession() {
        return new NavigationEvent(END_SESSION, null);
    }

    /** Pantalla destino ({@code null} para «volver» y «cerrar sesión»). */
    public View getView() {
        return view;
    }
}
