package com.autogest.controller.components;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Cabecera superior del shell: campana de notificaciones y el usuario actual
 * con sus iniciales, nombre y rol. El shell le pasa el usuario de
 * {@code Session} y la acción «Cerrar sesión», que se muestra en el menú del
 * avatar.
 *
 * <p>Principles: Pure Fabrication, High Cohesion.</p>
 */
public class Topbar extends HBox {

    private final Label avatarLabel = new Label();
    private final Label nameLabel = new Label();
    private final Label roleLabel = new Label();

    private Runnable onLogout;
    private Runnable onProfile;

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

        HBox userMenu = new HBox(10, avatarLabel, userInfo);
        userMenu.setAlignment(Pos.CENTER_LEFT);
        userMenu.getStyleClass().add("topbar-user");
        userMenu.setCursor(Cursor.HAND);

        MenuItem profileItem = new MenuItem("Mi perfil");
        profileItem.setGraphic(IconSet.icon(IconSet.USER));
        profileItem.setOnAction(event -> {
            if (onProfile != null) {
                onProfile.run();
            }
        });
        MenuItem logoutItem = new MenuItem("Cerrar sesión");
        logoutItem.setGraphic(IconSet.icon(IconSet.LOGOUT));
        logoutItem.setOnAction(event -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        ContextMenu userContextMenu = new ContextMenu(profileItem, new SeparatorMenuItem(), logoutItem);
        userMenu.setOnMouseClicked(event ->
                userContextMenu.show(userMenu, event.getScreenX(), event.getScreenY()));

        getChildren().addAll(spacer, notificationButton, userMenu);
        setUser("Invitado", "Sin sesión");
    }

    /**
     * Registra la acción que se ejecuta al elegir «Cerrar sesión» en el menú
     * del avatar.
     *
     * @param onLogout acción de cierre de sesión
     */
    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    /**
     * Registra la acción de «Mi perfil» en el menú del avatar.
     *
     * @param onProfile acción que abre «Perfil y cuenta»
     */
    public void setOnProfile(Runnable onProfile) {
        this.onProfile = onProfile;
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
