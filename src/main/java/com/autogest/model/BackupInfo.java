package com.autogest.model;

import java.time.LocalDateTime;

/**
 * Resumen de una copia de seguridad guardada: archivo, fecha, cantidad de
 * tablas y de registros, y tamaño en bytes.
 *
 * @param fileName  nombre del archivo ZIP
 * @param createdAt fecha y hora en que se creó
 * @param tables    cantidad de tablas incluidas
 * @param rows      cantidad total de registros
 * @param sizeBytes tamaño del archivo en bytes
 */
public record BackupInfo(String fileName, LocalDateTime createdAt, int tables, long rows, long sizeBytes) {
}
