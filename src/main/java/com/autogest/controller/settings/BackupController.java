package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.exception.AutoGestException;
import com.autogest.model.BackupInfo;
import com.autogest.service.BackupService;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Pantalla «Copias de seguridad» de Configuración (RF-32): crea una copia de
 * toda la base de datos en una carpeta local y lista las copias guardadas.
 * Las reglas y el formato viven en {@code BackupService}; aquí solo se
 * dispara la acción y se muestra el resultado.
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class BackupController implements Initializable {

    private static final int PAGE_SIZE = 10;

    @FXML
    private VBox root;

    private PagedTable<BackupInfo> table;
    private Button createButton;
    private Label message;
    private Label lastBackupValue;
    private Label countValue;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Copias de seguridad",
                "Guarda una copia de toda la información de AutoGest en este equipo.");
        header.setBreadcrumbs(List.of("Configuración", "Copias de seguridad"));

        root.getChildren().addAll(header, buildCreateCard(), buildListCard());
        reload();
    }

    // ---------- crear ----------

    private VBox buildCreateCard() {
        Path folder = service().getBackupDirectory();

        Label description = new Label("La copia incluye todas las tablas de la base de datos (clientes, "
                + "vehículos, servicios, repuestos, usuarios con sus contraseñas cifradas y el resto) en un "
                + "archivo ZIP con un CSV por tabla, que también se puede abrir con Excel. Guárdala en un "
                + "lugar seguro: contiene datos personales de los clientes.");
        description.setWrapText(true);
        description.getStyleClass().add("backup-description");

        Label folderLabel = new Label("Carpeta");
        folderLabel.getStyleClass().add("settings-info-label");
        Label folderValue = new Label(folder.toString());
        folderValue.getStyleClass().add("settings-info-value");
        folderValue.setWrapText(true);
        Button openFolder = new Button("Abrir carpeta");
        openFolder.getStyleClass().add("btn-secondary");
        openFolder.setOnAction(event -> openFolder(folder));
        HBox folderRow = new HBox(12, folderLabel, folderValue, spacer(), openFolder);
        folderRow.setAlignment(Pos.CENTER_LEFT);

        lastBackupValue = new Label("-");
        lastBackupValue.getStyleClass().add("settings-info-value");
        countValue = new Label("-");
        countValue.getStyleClass().add("settings-info-value");
        HBox stats = new HBox(32, stat("Última copia", lastBackupValue), stat("Copias guardadas", countValue));

        message = new Label();
        message.setWrapText(true);
        message.getStyleClass().add("form-message");
        HBox.setHgrow(message, Priority.ALWAYS);
        message.setMaxWidth(Double.MAX_VALUE);
        createButton = new Button("Crear copia ahora");
        createButton.getStyleClass().add("btn-primary");
        createButton.setOnAction(event -> createBackup());
        HBox actions = new HBox(12, message, createButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        return card("Crear una copia", IconSet.CLOUD_DOWNLOAD, description, stats, folderRow, actions);
    }

    private void createBackup() {
        createButton.setDisable(true);
        showMessage("Creando la copia... puede tardar unos segundos.", false);
        Background.run(() -> service().createBackup(),
                info -> {
                    createButton.setDisable(false);
                    showMessage("Copia creada: " + info.fileName() + " (" + info.tables() + " tablas, "
                            + info.rows() + " registros).", false);
                    reload();
                },
                error -> {
                    createButton.setDisable(false);
                    showMessage(errorText(error), true);
                });
    }

    // ---------- lista ----------

    private VBox buildListCard() {
        table = new PagedTable<>(PAGE_SIZE);
        table.addColumn("Archivo", BackupInfo::fileName);
        table.addColumn("Fecha", info -> Formatter.timestamp(info.createdAt()));
        table.addColumn("Tablas", info -> String.valueOf(info.tables()));
        table.addColumn("Registros", info -> String.valueOf(info.rows()));
        table.addColumn("Tamaño", info -> sizeText(info.sizeBytes()));
        table.fitHeightToRows(44);

        Label note = new Label("Para restaurar una copia, por ahora, se cargan sus CSV en la base de datos "
                + "desde Supabase. La restauración desde AutoGest llegará en una próxima entrega.");
        note.setWrapText(true);
        note.getStyleClass().add("settings-note");

        return card("Copias guardadas", IconSet.FILE_TEXT, table, note);
    }

    private void reload() {
        Background.run(() -> service().listBackups(),
                backups -> {
                    table.setData(backups);
                    countValue.setText(String.valueOf(backups.size()));
                    lastBackupValue.setText(backups.isEmpty()
                            ? "Nunca" : Formatter.timestamp(backups.get(0).createdAt()));
                },
                error -> showMessage(errorText(error), true));
    }

    // ---------- ayudas ----------

    /** Tamaño legible: «850 B», «12,4 KB», «3,1 MB». */
    static String sizeText(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.forLanguageTag("es-CO"), "%.1f KB", kb);
        }
        return String.format(Locale.forLanguageTag("es-CO"), "%.1f MB", kb / 1024.0);
    }

    private void openFolder(Path folder) {
        Background.run(() -> {
                    try {
                        Files.createDirectories(folder);
                        if (!Desktop.isDesktopSupported()
                                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                            throw new IllegalStateException("unsupported");
                        }
                        Desktop.getDesktop().open(folder.toFile());
                        return Boolean.TRUE;
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                },
                ok -> { },
                error -> showMessage("No se pudo abrir la carpeta. Ábrela desde el explorador: " + folder, true));
    }

    private void showMessage(String text, boolean error) {
        message.setText(text);
        message.getStyleClass().removeAll("form-message-error", "form-message-success");
        message.getStyleClass().add(error ? "form-message-error" : "form-message-success");
    }

    private static String errorText(RuntimeException error) {
        if (error instanceof AutoGestException && error.getMessage() != null) {
            return error.getMessage();
        }
        return "No se pudo completar la operación. Revisa la conexión e inténtalo de nuevo.";
    }

    private static VBox stat(String name, Label value) {
        Label label = new Label(name);
        label.getStyleClass().add("settings-info-label");
        return new VBox(2, label, value);
    }

    private static Region spacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    private static VBox card(String title, String icon, Node... body) {
        Node iconNode = IconSet.icon(icon);
        iconNode.getStyleClass().add("security-card-icon");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");
        HBox titleRow = new HBox(10, iconNode, titleLabel);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(16, titleRow);
        card.getChildren().addAll(body);
        card.getStyleClass().add("card");
        return card;
    }

    private static BackupService service() {
        return AppContext.getInstance().getBackupService();
    }
}
