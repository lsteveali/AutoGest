package com.autogest.controller.components;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Cabecera superior del shell: campana de notificaciones y el usuario actual
 * con sus iniciales, nombre y rol. En el Sprint 1 muestra un usuario
 * provisional; en el Sprint 2 el shell le pasa el usuario de {@code Session}.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class Topbar extends HBox {

    private final Label avatarLabel = new Label();
    private final Label nameLabel = new Label();
    private final Label roleLabel = new Label();

    /** Crea la cabecera con el usuario provisional «Invitado». */
    public Topbar() {
        getStyleClass().add("topbar");
        setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button notificationButton = new Button();
        notificationButton.setGraphic(IconSet.icon(IconSet.BELL));
        notificationButton.getStyleClass().add("icon-button");

        avatarLabel.getStyleClass().add("topbar-avatar");
        nameLabel.getStyleClass().add("topbar-user-name");
        roleLabel.getStyleClass().add("topbar-user-role");
        VBox userInfo = new VBox(0, nameLabel, roleLabel);

        getChildren().addAll(spacer, notificationButton, avatarLabel, userInfo);
        setUser("Invitado", "Sin sesión");
    }

    /**
     * Actualiza el usuario mostrado en la cabecera.
     *
     * @param fullName nombre completo del usuario
     * @param roleLabel rol ya traducido al español (por ejemplo «Administrador»)
     */
    public void setUser(String fullName, String roleLabel) {
        nameLabel.setText(fullName);
        this.roleLabel.setText(roleLabel);
        avatarLabel.setText(initialsOf(fullName));
    }

    /** Calcula las iniciales (primera y última palabra) del nombre. */
    private String initialsOf(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase();
    }
}
