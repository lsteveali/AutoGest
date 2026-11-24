package com.autogest.controller.components;

import com.autogest.util.AppTheme;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
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

    private final Label notificationBadge = new Label();

    private Runnable onLogout;
    private Runnable onProfile;
    private Runnable onNotifications;
    private Runnable onToggleTheme;
    private final Button themeButton = new Button();

    /** Crea la cabecera con el usuario provisional «Invitado». */
    public Topbar() {
        getStyleClass().add("topbar");
        setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button notificationButton = new Button();
        notificationButton.setGraphic(IconSet.icon(IconSet.BELL));
        notificationButton.getStyleClass().add("icon-button");
        notificationButton.setOnAction(event -> {
            if (onNotifications != null) {
                onNotifications.run();
            }
        });
        // Contador sobre la campana: avisos que necesitan atención (fallidos o pendientes).
        notificationBadge.getStyleClass().add("topbar-badge");
        notificationBadge.setMouseTransparent(true);
        notificationBadge.setVisible(false);
        StackPane.setAlignment(notificationBadge, Pos.TOP_RIGHT);
        StackPane bell = new StackPane(notificationButton, notificationBadge);

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

        // Cambio de tema: el ícono muestra el tema al que se pasa (luna = oscuro).
        themeButton.getStyleClass().add("icon-button");
        themeButton.setOnAction(event -> {
            if (onToggleTheme != null) {
                onToggleTheme.run();
            }
        });
        setTheme(AppTheme.LIGHT);

        getChildren().addAll(spacer, themeButton, bell, userMenu);
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

    /**
     * Registra la acción de la campana (abrir Notificaciones).
     *
     * @param onNotifications acción a ejecutar al pulsar la campana
     */
    public void setOnNotifications(Runnable onNotifications) {
        this.onNotifications = onNotifications;
    }

    /**
     * Registra la acción del botón de tema (pasar de claro a oscuro y al revés).
     *
     * @param onToggleTheme acción a ejecutar al pulsar el botón
     */
    public void setOnToggleTheme(Runnable onToggleTheme) {
        this.onToggleTheme = onToggleTheme;
    }

    /**
     * Muestra en el botón de tema el ícono y la ayuda según el tema actual:
     * en claro, una luna («Cambiar a tema oscuro»); en oscuro, un sol.
     *
     * @param theme tema actual
     */
    public void setTheme(AppTheme theme) {
        boolean dark = theme == AppTheme.DARK;
        themeButton.setGraphic(IconSet.icon(dark ? IconSet.SUN : IconSet.MOON));
        String text = dark ? "Cambiar a tema claro" : "Cambiar a tema oscuro";
        themeButton.setTooltip(new Tooltip(text));
        themeButton.setAccessibleText(text);
    }

    /**
     * Muestra sobre la campana cuántos avisos necesitan atención; 0 la oculta.
     *
     * @param count avisos fallidos o pendientes
     */
    public void setNotificationCount(int count) {
        notificationBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        notificationBadge.setVisible(count > 0);
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
