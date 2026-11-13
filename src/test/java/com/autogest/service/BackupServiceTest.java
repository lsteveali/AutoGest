package com.autogest.service;

import com.autogest.dao.BackupDao;
import com.autogest.dao.TableSnapshot;
import com.autogest.exception.ValidationException;
import com.autogest.model.BackupInfo;
import com.autogest.repository.BackupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link BackupService} con un DAO falso (sin base de datos) y el
 * repositorio real sobre una carpeta temporal.
 */
class BackupServiceTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @TempDir
    Path folder;

    @Test
    void createBackupWritesOneCsvPerTableAndManifest() throws IOException {
        BackupService service = service(List.of(
                table("customers", List.of("id", "name"), row("1", "Ana"), row("2", "Luis")),
                table("users", List.of("id", "email"), row("1", "admin@autogest.com"))),
                "2026-11-12T10:15:30");

        BackupInfo info = service.createBackup();

        assertEquals("autogest-copia-20261112-101530.zip", info.fileName());
        assertEquals(2, info.tables());
        assertEquals(3, info.rows());
        Path file = folder.resolve(info.fileName());
        assertTrue(Files.isRegularFile(file));
        assertEquals(Files.size(file), info.sizeBytes());
        try (ZipFile zip = new ZipFile(file.toFile())) {
            assertEquals("id,name\r\n1,Ana\r\n2,Luis\r\n", entry(zip, "customers.csv"));
            assertTrue(entry(zip, "manifest.properties").contains("rows=3"));
        }
    }

    @Test
    void csvKeepsNullsEmptyTextsAndSpecialCharactersApart() throws IOException {
        BackupService service = service(List.of(table("notes", List.of("a", "b", "c", "d"),
                row(null, "", "con, coma", "dice \"hola\"\notra línea"))), "2026-11-12T10:00:00");

        BackupInfo info = service.createBackup();

        try (ZipFile zip = new ZipFile(folder.resolve(info.fileName()).toFile())) {
            assertEquals("a,b,c,d\r\n,\"\",\"con, coma\",\"dice \"\"hola\"\"\notra línea\"\r\n",
                    entry(zip, "notes.csv"));
        }
    }

    @Test
    void createBackupRejectsEmptyDatabase() {
        BackupService service = service(List.of(), "2026-11-12T10:00:00");

        assertThrows(ValidationException.class, service::createBackup);
        assertTrue(service.listBackups().isEmpty());
    }

    @Test
    void listBackupsShowsNewestFirstAndIgnoresForeignFiles() throws IOException {
        List<TableSnapshot> tables = List.of(table("users", List.of("id"), row("1")));
        service(tables, "2026-11-10T08:00:00").createBackup();
        service(tables, "2026-11-12T09:30:00").createBackup();
        Files.writeString(folder.resolve("autogest-copia-dañada.zip"), "no es un zip");
        Files.writeString(folder.resolve("notas.txt"), "otro archivo");

        List<BackupInfo> backups = service(tables, "2026-11-12T10:00:00").listBackups();

        assertEquals(2, backups.size());
        assertEquals(LocalDateTime.parse("2026-11-12T09:30:00"), backups.get(0).createdAt());
        assertEquals(LocalDateTime.parse("2026-11-10T08:00:00"), backups.get(1).createdAt());
    }

    @Test
    void listBackupsIsEmptyWhenFolderDoesNotExist() {
        BackupService service = new BackupService(new FakeBackupDao(List.of()),
                new BackupRepository(folder.resolve("no-existe")));

        assertTrue(service.listBackups().isEmpty());
        assertFalse(Files.exists(folder.resolve("no-existe")));
    }

    // ---------- ayudas ----------

    private BackupService service(List<TableSnapshot> tables, String now) {
        Clock clock = Clock.fixed(LocalDateTime.parse(now).atZone(ZONE).toInstant(), ZONE);
        return new BackupService(new FakeBackupDao(tables), new BackupRepository(folder), clock);
    }

    @SafeVarargs
    private static TableSnapshot table(String name, List<String> columns, List<String>... rows) {
        return new TableSnapshot(name, columns, Arrays.asList(rows));
    }

    private static List<String> row(String... values) {
        return Arrays.asList(values);
    }

    private static String entry(ZipFile zip, String name) throws IOException {
        try (InputStream input = zip.getInputStream(zip.getEntry(name))) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** DAO falso: devuelve tablas fijas sin conectarse a la base de datos. */
    private static final class FakeBackupDao extends BackupDao {

        private final List<TableSnapshot> tables;

        FakeBackupDao(List<TableSnapshot> tables) {
            super(null);
            this.tables = tables;
        }

        @Override
        public List<TableSnapshot> readAllTables() {
            return tables;
        }
    }
}
