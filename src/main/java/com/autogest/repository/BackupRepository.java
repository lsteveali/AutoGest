package com.autogest.repository;

import com.autogest.dao.TableSnapshot;
import com.autogest.exception.DataAccessException;
import com.autogest.exception.ValidationException;
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
import java.util.zip.ZipException;
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
 * <p>{@link #read(String)} hace el camino inverso para restaurar una copia.</p>
 *
 * <p>Principles: Pure Fabrication, Protected Variations (el formato del
 * archivo solo vive aquí).</p>
 */
public class BackupRepository {

    static final String MANIFEST = "manifest.properties";
    private static final String PREFIX = "autogest-copia-";
    private static final String EXTENSION = ".zip";
    private static final String TABLE_KEY = "table.";
    private static final String ROWS_KEY = ".rows";
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
                    manifest.setProperty(TABLE_KEY + table.name() + ROWS_KEY, String.valueOf(table.rows().size()));
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

    /**
     * Lee el contenido completo de una copia guardada en la carpeta. Comprueba
     * que cada tabla del manifiesto esté en el ZIP con la cantidad de filas
     * anotada, así una copia incompleta o editada a mano no se restaura.
     *
     * @param fileName nombre del archivo (sin carpeta), tal como lo da {@link #findAll()}
     * @return las tablas de la copia, en el orden del manifiesto
     * @throws ValidationException si el nombre no es una copia de la carpeta o el archivo no es válido
     * @throws DataAccessException si no se puede leer el archivo
     */
    public List<TableSnapshot> read(String fileName) {
        Path file = resolveBackup(fileName);
        try (ZipFile zip = new ZipFile(file.toFile())) {
            ZipEntry manifestEntry = zip.getEntry(MANIFEST);
            if (manifestEntry == null) {
                throw invalid(fileName, "no tiene manifiesto");
            }
            Properties manifest = new Properties();
            try (InputStream input = zip.getInputStream(manifestEntry)) {
                manifest.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            }
            List<String> names = manifest.stringPropertyNames().stream()
                    .filter(key -> key.startsWith(TABLE_KEY) && key.endsWith(ROWS_KEY))
                    .map(key -> key.substring(TABLE_KEY.length(), key.length() - ROWS_KEY.length()))
                    .sorted()
                    .toList();
            if (names.isEmpty() || names.size() != Integer.parseInt(manifest.getProperty("tables", "-1"))) {
                throw invalid(fileName, "el manifiesto no coincide con sus tablas");
            }
            List<TableSnapshot> tables = new ArrayList<>(names.size());
            for (String name : names) {
                ZipEntry entry = zip.getEntry(name + ".csv");
                if (entry == null) {
                    throw invalid(fileName, "le falta la tabla " + name);
                }
                String csv;
                try (InputStream input = zip.getInputStream(entry)) {
                    csv = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                TableSnapshot table = fromCsv(name, csv);
                long expected = Long.parseLong(manifest.getProperty(TABLE_KEY + name + ROWS_KEY));
                if (table.rows().size() != expected) {
                    throw invalid(fileName, "la tabla " + name + " tiene " + table.rows().size()
                            + " registros y el manifiesto dice " + expected);
                }
                tables.add(table);
            }
            return tables;
        } catch (ValidationException e) {
            throw e;
        } catch (ZipException e) {
            throw invalid(fileName, "el archivo está dañado");
        } catch (IOException e) {
            throw new DataAccessException("No se pudo leer la copia " + fileName + ".", e);
        } catch (RuntimeException e) {
            throw invalid(fileName, "el archivo está dañado");
        }
    }

    /** Ruta de una copia de la carpeta; rechaza nombres con rutas o que no son copias. */
    private Path resolveBackup(String fileName) {
        if (fileName == null || !fileName.startsWith(PREFIX) || !fileName.endsWith(EXTENSION)
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new ValidationException("Elige una copia de la lista.");
        }
        Path file = directory.resolve(fileName);
        if (!Files.isRegularFile(file)) {
            throw new ValidationException("La copia " + fileName + " ya no está en la carpeta.");
        }
        return file;
    }

    private static ValidationException invalid(String fileName, String reason) {
        return new ValidationException("La copia " + fileName + " no se puede restaurar: " + reason + ".");
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

    /**
     * Convierte un CSV escrito por {@link #toCsv(TableSnapshot)} en una tabla:
     * la primera fila son las columnas; un campo vacío sin comillas es nulo y
     * {@code ""} es un texto vacío.
     *
     * @throws IllegalArgumentException si el CSV está mal formado
     */
    static TableSnapshot fromCsv(String name, String csv) {
        List<List<String>> records = parseCsv(csv);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("CSV sin encabezado: " + name);
        }
        List<String> columns = records.get(0);
        if (columns.contains(null)) {
            throw new IllegalArgumentException("Columna sin nombre en " + name);
        }
        List<List<String>> rows = records.subList(1, records.size());
        for (List<String> row : rows) {
            if (row.size() != columns.size()) {
                throw new IllegalArgumentException("Fila con " + row.size() + " campos en " + name);
            }
        }
        return new TableSnapshot(name, columns, rows);
    }

    /** Separa el CSV en registros; respeta comillas, comas y saltos de línea dentro de los campos. */
    private static List<List<String>> parseCsv(String csv) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean inQuotes = false;
        int length = csv.length();
        for (int i = 0; i < length; i++) {
            char c = csv.charAt(i);
            if (inQuotes) {
                if (c != '"') {
                    field.append(c);
                } else if (i + 1 < length && csv.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    inQuotes = false;
                }
            } else if (c == '"') {
                if (quoted || field.length() > 0) {
                    throw new IllegalArgumentException("Comilla fuera de lugar");
                }
                quoted = true;
                inQuotes = true;
            } else if (c == ',') {
                record.add(fieldValue(field, quoted));
                field.setLength(0);
                quoted = false;
            } else if (c == '\r' || c == '\n') {
                record.add(fieldValue(field, quoted));
                records.add(record);
                record = new ArrayList<>();
                field.setLength(0);
                quoted = false;
                if (c == '\r' && i + 1 < length && csv.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                if (quoted) {
                    throw new IllegalArgumentException("Texto después de las comillas de cierre");
                }
                field.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("Comillas sin cerrar");
        }
        if (field.length() > 0 || quoted || !record.isEmpty()) {
            record.add(fieldValue(field, quoted));
            records.add(record);
        }
        return records;
    }

    private static String fieldValue(StringBuilder field, boolean quoted) {
        return field.length() == 0 && !quoted ? null : field.toString();
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
