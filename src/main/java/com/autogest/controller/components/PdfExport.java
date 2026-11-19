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
 * Flujo común para guardar un PDF: pregunta dónde guardarlo, lo genera en
 * segundo plano y ofrece abrirlo. Lo usan la factura y los reportes.
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
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.setInitialFileName(fileName);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File folder = lastFolder != null ? lastFolder : new File(System.getProperty("user.home"), "Documents");
        if (folder.isDirectory()) {
            chooser.setInitialDirectory(folder);
        }
        File chosen = chooser.showSaveDialog(owner);
        if (chosen == null) {
            return;
        }
        File file = chosen.getName().toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? chosen : new File(chosen.getParentFile(), chosen.getName() + ".pdf");
        lastFolder = file.getParentFile();
        Background.run(() -> {
            writer.accept(file.toPath());
            return file;
        }, saved -> {
            if (ConfirmDialog.confirm("PDF guardado", "Se guardó «" + saved.getName() + "» en "
                    + saved.getParent() + ". ¿Quieres abrirlo ahora?")) {
                open(saved);
            }
        }, error -> ErrorDialog.show("No se pudo generar el PDF",
                error instanceof AutoGestException ? error.getMessage() : "Inténtalo de nuevo."));
    }

    /** Abre el archivo con el programa predeterminado del sistema, fuera del hilo de la interfaz. */
    private static void open(File file) {
        Background.run(() -> {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new AutoGestException(
                        "Este equipo no permite abrir archivos desde AutoGest. Ábrelo desde " + file.getParent());
            }
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                throw new AutoGestException("No hay un programa para abrir PDF. El archivo está en "
                        + file.getParent(), e);
            }
            return file;
        }, opened -> { }, error -> ErrorDialog.show("No se pudo abrir el PDF", error.getMessage()));
    }
}
