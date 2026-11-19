package com.autogest.model;

/**
 * Resultado de restaurar una copia de seguridad.
 *
 * @param restoredFile copia que se restauró
 * @param safetyBackup copia automática del estado anterior, creada justo antes de restaurar
 * @param tables       cantidad de tablas recargadas
 * @param rows         cantidad total de registros cargados
 */
public record RestoreResult(String restoredFile, BackupInfo safetyBackup, int tables, long rows) {
}
