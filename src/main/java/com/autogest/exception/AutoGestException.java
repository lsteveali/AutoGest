package com.autogest.exception;

/**
 * Excepción base del sistema AutoGest. Todas las excepciones propias del
 * proyecto heredan de esta clase para que las capas superiores puedan
 * diferenciarlas de los errores de la plataforma (por ejemplo, de JavaFX o del
 * JDK) y mostrar un mensaje controlado al usuario.
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public class AutoGestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje pensado para el usuario.
     *
     * @param message mensaje en español que describe el error
     */
    public AutoGestException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con un mensaje y la causa original.
     *
     * @param message mensaje en español que describe el error
     * @param cause   error original que provocó la excepción
     */
    public AutoGestException(String message, Throwable cause) {
        super(message, cause);
    }
}
