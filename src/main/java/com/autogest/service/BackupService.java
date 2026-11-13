package com.autogest.service;

import com.autogest.dao.BackupDao;
import com.autogest.dao.TableSnapshot;
import com.autogest.exception.ValidationException;
import com.autogest.model.BackupInfo;
import com.autogest.repository.BackupRepository;

import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Copias de seguridad (RF-32): crea una copia de toda la base de datos y lista
 * las copias guardadas. Lee los datos con {@link BackupDao} y los guarda con
 * {@link BackupRepository}; aquí solo viven las reglas.
 *
 * <p>Restaurar una copia queda para una entrega posterior: exige vaciar y
 * recargar las tablas en orden de dependencias dentro de una transacción.</p>
 *
 * <p>Principles: GRASP Controller (de caso de uso), DIP, SRP.</p>
 */
public class BackupService {

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
