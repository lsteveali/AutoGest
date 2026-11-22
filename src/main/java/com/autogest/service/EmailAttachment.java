package com.autogest.service;

/**
 * Archivo adjunto a un correo (por ejemplo, la factura en PDF o en PNG).
 *
 * @param fileName    nombre con el que lo ve el cliente
 * @param contentType tipo MIME, por ejemplo {@code application/pdf}
 * @param content     bytes del archivo
 */
public record EmailAttachment(String fileName, String contentType, byte[] content) {

    /** Tipo MIME de los PDF. */
    public static final String PDF = "application/pdf";
    /** Tipo MIME de las imágenes PNG. */
    public static final String PNG = "image/png";
}
