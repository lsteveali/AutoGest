package com.autogest.exception;

/**
 * Señala que un dato o una regla de negocio no se cumple. La lanzan el modelo
 * (por ejemplo, {@code Part.decreaseStock}) y los Services ante entradas
 * inválidas; el Controller la captura y muestra su mensaje al usuario.
 *
 * <p>Principles: Pure Fabrication, Protected Variations.</p>
 */
public class ValidationException extends AutoGestException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje claro para el usuario.
     *
     * @param message mensaje en español que describe la validación incumplida
     */
    public ValidationException(String message) {
        super(message);
    }

    /**
     * Crea la excepción con un mensaje y la causa original.
     *
     * @param message mensaje en español que describe la validación incumplida
     * @param cause   error original que provocó la excepción
     */
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
