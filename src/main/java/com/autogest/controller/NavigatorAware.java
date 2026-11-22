package com.autogest.controller;

/**
 * La implementan los controladores que necesitan ejecutar transiciones de
 * pantalla (por ejemplo ir de «Lista» a «Detalle») sin depender directamente
 * del controlador raíz.
 *
 * <p>El {@link ViewNavigator} inyecta el navegador al cargar la pantalla, antes
 * de entregar cualquier parámetro de navegación.</p>
 *
 * <p>Principles: Indirection, Low Coupling.</p>
 */
public interface NavigatorAware {

    /**
     * Recibe el navegador de pantallas para poder cambiar de vista.
     *
     * @param navigator navegador del área central de la aplicación
     */
    void setNavigator(ViewNavigator navigator);
}
