package com.autogest.controller;

/**
 * La implementan los controladores que reciben un parámetro cuando la
 * pantalla se abre con uno (por ejemplo el id de la entidad que se muestra
 * o se edita).
 *
 * <p>Principles: Indirection, Low Coupling.</p>
 */
public interface ParameterizedController {

    /**
     * Recibe el parámetro de navegación (normalmente un id de entidad).
     *
     * @param parameter id de la entidad cargada por esta pantalla
     */
    void receiveParameter(int parameter);
}