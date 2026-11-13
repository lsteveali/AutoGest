package com.autogest.repository;

import com.autogest.dao.TableSnapshot;
import com.autogest.exception.DataAccessException;
import com.autogest.model.BackupInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Guarda y lista las copias de seguridad en una carpeta local. Cada copia es
 * un ZIP con un CSV por tabla (UTF-8, se abre con Excel) y un
 * {@code manifest.properties} con la fecha y los totales.
 *
 * <p>Formato de los CSV: separador «,», campos entre comillas cuando tienen
 * comas, comillas o saltos de línea, y comillas internas duplicadas. Un valor
 * nulo es un campo vacío sin comillas; un texto vacío es {@code ""}, para no
 * confundirlos al restaurar.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations (el formato del
 * archivo solo vive aquí).</p>
 */
public class BackupRepository {

    static final String MANIFEST = "manifest.properties";
    private static final String PREFIX = "autogest-copia-";
    private static final String EXTENSION = ".zip";
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path directory;

    /**
     * @param directory carpeta donde se guardan las copias (se crea si no existe)
     */
    public BackupRepository(Path directory) {
        this.directory = directory;
    }

    /** Carpeta donde se guardan las copias. */
    public Path getDirectory() {
        return directory;
    }

    /**
     * Escribe una copia nueva.
     *
     * @param tables    contenido de las tablas
     * @param createdAt fecha y hora de la copia (también da el nombre del archivo)
     * @return resumen de la copia guardada
     * @throws DataAccessException si no se puede escribir el archivo
     */
    public BackupInfo save(List<TableSnapshot> tables, LocalDateTime createdAt) {
        long totalRows = tables.stream().mapToLong(table -> table.rows().size()).sum();
        Path file = directory.resolve(PREFIX + FILE_STAMP.format(createdAt) + EXTENSION);
        Path partial = directory.resolve(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(directory);
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(partial))) {
                Properties manifest = new Properties();
                manifest.setProperty("createdAt", createdAt.withNano(0).toString());
                manifest.setProperty("tables", String.valueOf(tables.size()));
                manifest.setProperty("rows", String.valueOf(totalRows));
                for (TableSnapshot table : tables) {
                    manifest.setProperty("table." + table.name() + ".rows", String.valueOf(table.rows().size()));
                    zip.putNextEntry(new ZipEntry(table.name() + ".csv"));
                    zip.write(toCsv(table).getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
                zip.putNextEntry(new ZipEntry(MANIFEST));
                writeManifest(manifest, zip);
                zip.closeEntry();
            }
            // Se escribe primero a .tmp: una copia a medias nunca aparece en la lista.
            Files.move(partial, file);
            return new BackupInfo(file.getFileName().toString(), createdAt.withNano(0), tables.size(),
                    totalRows, Files.size(file));
        } catch (IOException e) {
            try {
                Files.deleteIfExists(partial);
            } catch (IOException ignored) {
                // Se informa el error original.
            }
            throw new DataAccessException("No se pudo guardar la copia de seguridad en " + directory + ".", e);
        }
    }

    /**
     * Lista las copias guardadas, la más reciente primero. Los archivos que no
     * son copias válidas se ignoran.
     *
     * @return copias de la carpeta (vacía si la carpeta no existe)
     * @throws DataAccessException si no se puede leer la carpeta
     */
    public List<BackupInfo> findAll() {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        List<BackupInfo> backups = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, PREFIX + "*" + EXTENSION)) {
            for (Path file : files) {
                BackupInfo info = readInfo(file);
                if (info != null) {
                    backups.add(info);
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer la carpeta de copias " + directory + ".", e);
        }
        backups.sort(Comparator.comparing(BackupInfo::createdAt).reversed());
        return backups;
    }

    private static BackupInfo readInfo(Path file) {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry entry = zip.getEntry(MANIFEST);
            if (entry == null) {
                return null;
            }
            Properties manifest = new Properties();
            try (InputStream input = zip.getInputStream(entry)) {
                manifest.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            }
            return new BackupInfo(file.getFileName().toString(),
                    LocalDateTime.parse(manifest.getProperty("createdAt")),
                    Integer.parseInt(manifest.getProperty("tables", "0")),
                    Long.parseLong(manifest.getProperty("rows", "0")),
                    Files.size(file));
        } catch (IOException | RuntimeException e) {
            // Archivo dañado o ajeno: no se lista.
            return null;
        }
    }

    private static void writeManifest(Properties manifest, OutputStream output) throws IOException {
        StringWriter writer = new StringWriter();
        manifest.store(writer, "AutoGest - copia de seguridad");
        output.write(writer.toString().getBytes(StandardCharsets.UTF_8));
    }

    /** Convierte una tabla en CSV con encabezado. */
    static String toCsv(TableSnapshot table) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, table.columns());
        for (List<String> row : table.rows()) {
            appendRow(csv, row);
        }
        return csv.toString();
    }

    private static void appendRow(StringBuilder csv, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(csvField(values.get(i)));
        }
        csv.append("\r\n");
    }

    /** Campo CSV: nulo = vacío; vacío = {@code ""}; comillas si hace falta. */
    static String csvField(String value) {
        if (value == null) {
            return "";
        }
        boolean quote = value.isEmpty() || value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        return quote ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
