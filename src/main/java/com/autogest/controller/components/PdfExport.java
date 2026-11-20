package com.autogest.controller.components;

import com.autogest.exception.AutoGestException;
import com.autogest.util.Background;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Flujo común para guardar un PDF o una imagen PNG: pregunta dónde
 * guardarlo, lo genera en segundo plano y ofrece abrirlo. Lo usan la factura
 * y los reportes.
 *
 * <p>Principles: Pure Fabrication, DRY.</p>
 */
public final class PdfExport {

    /** Última carpeta elegida (se propone de nuevo en la misma sesión). */
    private static File lastFolder;

    private PdfExport() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Pide el archivo de destino y genera el PDF.
     *
     * @param owner    ventana dueña de los diálogos
     * @param title    título del diálogo de guardar
     * @param fileName nombre de archivo sugerido
     * @param writer   escribe el PDF en la ruta recibida; se ejecuta fuera del hilo de la interfaz
     */
    public static void save(Window owner, String title, String fileName, Consumer<Path> writer) {
        saveAs(owner, title, fileName, Kind.PDF, writer);
    }

    /**
     * Pide el archivo de destino y genera una imagen PNG (por ejemplo, la
     * factura para enviarla por chat).
     *
     * @param owner    ventana dueña de los diálogos
     * @param title    título del diálogo de guardar
     * @param fileName nombre de archivo sugerido
     * @param writer   escribe la imagen en la ruta recibida; se ejecuta fuera del hilo de la interfaz
     */
    public static void savePng(Window owner, String title, String fileName, Consumer<Path> writer) {
        saveAs(owner, title, fileName, Kind.PNG, writer);
    }

    /** Formatos que se pueden guardar, con sus textos. */
    private enum Kind {
        PDF("Documento PDF", ".pdf", "PDF guardado", "No se pudo generar el PDF", "No hay un programa para abrir PDF."),
        PNG("Imagen PNG", ".png", "Imagen guardada", "No se pudo generar la imagen",
                "No hay un programa para abrir imágenes.");

        private final String filter;
        private final String extension;
        private final String savedTitle;
        private final String errorTitle;
        private final String noProgram;

        Kind(String filter, String extension, String savedTitle, String errorTitle, String noProgram) {
            this.filter = filter;
            this.extension = extension;
            this.savedTitle = savedTitle;
            this.errorTitle = errorTitle;
            this.noProgram = noProgram;
        }
    }

    private static void saveAs(Window owner, String title, String fileName, Kind kind, Consumer<Path> writer) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(fileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(kind.filter, "*" + kind.extension));
        File folder = lastFolder != null ? lastFolder : new File(System.getProperty("user.home"), "Documents");
        if (folder.isDirectory()) {
            chooser.setInitialDirectory(folder);
        }
        File chosen = chooser.showSaveDialog(owner);
        if (chosen == null) {
            return;
        }
        File file = chosen.getName().toLowerCase(Locale.ROOT).endsWith(kind.extension)
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + kind.extension);
        lastFolder = file.getParentFile();
        Background.run(() -> {
            writer.accept(file.toPath());
            return file;
        }, saved -> {
            if (ConfirmDialog.confirm(kind.savedTitle, "Se guardó «" + saved.getName() + "» en "
                    + saved.getParent() + ". ¿Quieres abrirlo ahora?")) {
                open(saved, kind);
            }
        }, error -> ErrorDialog.show(kind.errorTitle,
                error instanceof AutoGestException ? error.getMessage() : "Inténtalo de nuevo."));
    }

    /** Abre el archivo con el programa predeterminado del sistema, fuera del hilo de la interfaz. */
    private static void open(File file, Kind kind) {
        Background.run(() -> {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new AutoGestException(
                        "Este equipo no permite abrir archivos desde AutoGest. Ábrelo desde " + file.getParent());
            }
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                throw new AutoGestException(kind.noProgram + " El archivo está en " + file.getParent(), e);
            }
            return file;
        }, opened -> { }, error -> ErrorDialog.show("No se pudo abrir el archivo", error.getMessage()));
    }
}
