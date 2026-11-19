package com.autogest.service;

import com.autogest.dao.BackupDao;
import com.autogest.dao.TableSnapshot;
import com.autogest.exception.ValidationException;
import com.autogest.model.BackupInfo;
import com.autogest.model.RestoreResult;
import com.autogest.repository.BackupRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Copias de seguridad (RF-32): crea una copia de toda la base de datos, lista
 * las copias guardadas y restaura una de ellas. Lee y carga los datos con
 * {@link BackupDao} y guarda las copias con {@link BackupRepository}; aquí
 * solo viven las reglas.
 *
 * <p>Principles: GRASP Controller (de caso de uso), DIP, SRP.</p>
 */
public class BackupService {

    /** Palabra que el usuario debe escribir para confirmar una restauración. */
    public static final String RESTORE_CONFIRMATION = "RESTAURAR";

    private final BackupDao dao;
    private final BackupRepository repository;
    private final Clock clock;

    /**
     * @param dao        lector de la base de datos
     * @param repository almacenamiento de las copias
     */
    public BackupService(BackupDao dao, BackupRepository repository) {
        this(dao, repository, Clock.systemDefaultZone());
    }

    /** Constructor con reloj, para fijar la fecha en las pruebas. */
    BackupService(BackupDao dao, BackupRepository repository, Clock clock) {
        this.dao = dao;
        this.repository = repository;
        this.clock = clock;
    }

    /**
     * Crea una copia de seguridad de todas las tablas.
     *
     * @return resumen de la copia creada
     * @throws ValidationException si la base de datos no devolvió ninguna tabla
     * @throws com.autogest.exception.DataAccessException si falla la lectura o la escritura
     */
    public BackupInfo createBackup() {
        List<TableSnapshot> tables = dao.readAllTables();
        if (tables.isEmpty()) {
            throw new ValidationException("La base de datos no tiene tablas para copiar.");
        }
        return repository.save(tables, LocalDateTime.now(clock));
    }

    /**
     * Restaura una copia: reemplaza el contenido de todas sus tablas en una
     * sola transacción. Antes guarda una copia automática del estado actual,
     * para poder volver atrás si se restauró la copia equivocada.
     *
     * @param fileName     copia a restaurar (nombre del archivo, de {@link #listBackups()})
     * @param confirmation texto que escribió el usuario; debe ser {@link #RESTORE_CONFIRMATION}
     * @return qué se restauró y la copia previa
     * @throws ValidationException si falta la confirmación o la copia no es válida
     * @throws com.autogest.exception.DataAccessException si falla la copia previa o la carga
     *         (en ese caso la base de datos no cambia)
     */
    public RestoreResult restoreBackup(String fileName, String confirmation) {
        if (confirmation == null || !RESTORE_CONFIRMATION.equals(confirmation.trim())) {
            throw new ValidationException("Escribe " + RESTORE_CONFIRMATION + " para confirmar la restauración.");
        }
        // Se lee y valida la copia completa antes de tocar nada.
        List<TableSnapshot> tables = repository.read(fileName);
        BackupInfo safetyBackup = createBackup();
        dao.restoreAllTables(tables);
        long rows = tables.stream().mapToLong(table -> table.rows().size()).sum();
        return new RestoreResult(fileName, safetyBackup, tables.size(), rows);
    }

    /**
     * @return copias guardadas, la más reciente primero
     */
    public List<BackupInfo> listBackups() {
        return repository.findAll();
    }

    /**
     * @return carpeta donde se guardan las copias
     */
    public Path getBackupDirectory() {
        return repository.getDirectory();
    }
}
