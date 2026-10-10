package com.autogest.exception;

/**
 * Señala que una entidad solicitada no existe. El DAO y el Repository devuelven
 * {@code null} cuando no encuentran el registro; el Service traduce ese
 * {@code null} en esta excepción, para que la interfaz muestre un mensaje
 * entendible (decisión D-08).
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public class NotFoundException extends AutoGestException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje claro para el usuario.
     *
     * @param message mensaje en español que indica qué registro no se encontró
     */
    public NotFoundException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con un mensaje y la causa original.
     *
     * @param message mensaje en español que indica qué registro no se encontró
     * @param cause   error original que provocó la excepción
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
