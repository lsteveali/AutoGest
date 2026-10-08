package com.autogest.controller;

/**
 * Implemented by controllers that receive a parameter when the view is
 * opened with one (for example the id of the entity to show or edit).
 *
 * <p>Principles: Indirection, Low Coupling.</p>
 */
public interface ParameterizedController {

    /**
     * Receives the navigation parameter (usually an entity id).
     *
     * @param parameter id of the entity loaded by this screen
     */
    void receiveParameter(int parameter);
}