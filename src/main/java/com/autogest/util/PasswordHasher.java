package com.autogest.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Cifra y verifica contraseñas con PBKDF2-HMAC-SHA256 (decisión D-07). No usa
 * librerías externas: solo el JDK. Cada contraseña se guarda con una sal
 * aleatoria distinta, en el formato {@code iteraciones:salBase64:hashBase64}.
 *
 * <p>La verificación usa {@link MessageDigest#isEqual(byte[], byte[])} para
 * comparar en tiempo constante y no filtrar información por el tiempo de
 * respuesta. La contraseña en texto plano nunca se guarda ni se registra.</p>
 *
 * <p>Principles: SRP, Pure Fabrication, Protected Variations.</p>
 */
public final class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    /** Iteraciones de PBKDF2 (D-07; público: lo muestra la pantalla Seguridad). */
    public static final int ITERATIONS = 600_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String SEPARATOR = ":";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordHasher() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Calcula el hash de una contraseña con una sal nueva.
     *
     * @param password contraseña en texto plano
     * @return cadena {@code iteraciones:salBase64:hashBase64}
     * @throws IllegalArgumentException si la contraseña es nula o vacía
     */
    public static String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        byte[] salt = new byte[SALT_BYTES];
        SECURE_RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_BITS);

        Base64.Encoder encoder = Base64.getEncoder();
        return ITERATIONS + SEPARATOR + encoder.encodeToString(salt) + SEPARATOR + encoder.encodeToString(hash);
    }

    /**
     * Verifica si una contraseña coincide con un hash guardado.
     *
     * @param password   contraseña en texto plano a comprobar
     * @param storedHash hash guardado en el formato {@code iteraciones:sal:hash}
     * @return {@code true} si coincide; {@code false} si no coincide o el hash es inválido
     */
    public static boolean verify(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.split(SEPARATOR);
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            byte[] actual = pbkdf2(password.toCharArray(), salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            // Formato de hash corrupto: se trata como contraseña incorrecta.
            return false;
        }
    }

    /** Deriva la clave con PBKDF2 usando los parámetros indicados. */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyBits);
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }
}
