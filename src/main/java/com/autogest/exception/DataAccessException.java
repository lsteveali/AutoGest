package com.autogest.exception;

/**
 * Envuelve los errores técnicos de acceso a datos ({@link java.sql.SQLException})
 * para que la capa DAO y {@code DatabaseConnection} no expongan la tecnología de
 * persistencia hacia arriba. Los Services y los Controllers nunca ven
 * {@code SQLException}: solo este mensaje controlado.
 *
 * <p>Principles: Pure Fabrication, Protected Variations, Low Coupling.</p>
 */
public class DataAccessException extends AutoGestException {

    private static final long serialVersionUID = 1L;

    /**
     * Crea la excepción con un mensaje claro para el usuario.
     *
     * @param message mensaje en español que describe el problema de datos
     */
    public DataAccessException(String message) {
        super(message);
    }

    /**
     * Crea la excepción envolviendo el error técnico original.
     *
     * @param message mensaje en español que describe el problema de datos
     * @param cause   {@link java.sql.SQLException} u otro error original
     */
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
