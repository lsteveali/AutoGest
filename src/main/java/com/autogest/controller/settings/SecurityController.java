package com.autogest.controller.settings;

import com.autogest.config.AppContext;
import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.controller.components.IconSet;
import com.autogest.controller.components.PageHeader;
import com.autogest.controller.components.PagedTable;
import com.autogest.controller.components.StatusBadge;
import com.autogest.controller.user.UserLabels;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.service.UserService;
import com.autogest.util.Background;
import com.autogest.util.Formatter;
import com.autogest.util.PasswordHasher;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Pantalla «Seguridad» de Configuración: política de contraseñas, permisos
 * por rol y último acceso de cada usuario. Es de solo consulta: los permisos
 * los fija la matriz D-11 en {@link Permissions} y aquí se leen de ella, para
 * que la tabla nunca contradiga lo que la aplicación realmente aplica.
 *
 * <p>Principles: GRASP Controller, Information Expert (los permisos los sabe
 * {@code Permissions}), Low Coupling.</p>
 */
public class SecurityController implements Initializable {

    /** Secciones de la barra lateral, en su orden oficial. */
    static final List<View> SECTIONS = List.of(View.HOME, View.CUSTOMERS, View.VEHICLES, View.ORDERS,
            View.SERVICES, View.INVENTORY, View.BILLING, View.NOTIFICATIONS, View.REPORTS,
            View.SETTINGS, View.ASSISTANT);

    private static final int PAGE_SIZE = 10;

    @FXML
    private VBox root;

    private PagedTable<User> accessTable;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        PageHeader header = new PageHeader("Seguridad",
                "Política de contraseñas, permisos por rol y actividad de acceso de los usuarios.");
        header.setBreadcrumbs(List.of("Configuración", "Seguridad"));

        HBox topRow = new HBox(16, buildPasswordPolicyCard(), buildPermissionsCard());
        topRow.setAlignment(Pos.TOP_LEFT);

        root.getChildren().addAll(header, topRow, buildAccessCard());
        loadUsers();
    }

    // ---------- política de contraseñas ----------

    private VBox buildPasswordPolicyCard() {
        String iterations = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CO"))
                .format(PasswordHasher.ITERATIONS);
        VBox list = new VBox(10,
                rule("Mínimo " + UserService.MIN_PASSWORD_LENGTH + " caracteres, con letras y números."),
                rule("No se puede repetir la contraseña actual al cambiarla."),
                rule("Se guardan cifradas con PBKDF2-HMAC-SHA256 (" + iterations
                        + " iteraciones y sal aleatoria): nadie puede ver la contraseña original."),
                rule("Cada usuario cambia la suya en «Perfil y cuenta»; el administrador asigna la clave "
                        + "inicial al crear la cuenta."));

        VBox card = card("Política de contraseñas", IconSet.LOCK, list);
        card.setPrefWidth(380);
        card.setMinWidth(320);
        return card;
    }

    private static HBox rule(String text) {
        Node check = IconSet.icon(IconSet.CHECK);
        check.getStyleClass().add("security-check");
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("security-rule");
        HBox row = new HBox(10, check, label);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    // ---------- permisos por rol ----------

    private VBox buildPermissionsCard() {
        GridPane matrix = new GridPane();
        matrix.getStyleClass().add("security-matrix");
        ColumnConstraints first = new ColumnConstraints();
        first.setHgrow(Priority.ALWAYS);
        first.setMinWidth(150);
        matrix.getColumnConstraints().add(first);

        Label sectionHeader = new Label("Sección");
        sectionHeader.getStyleClass().add("security-matrix-header");
        matrix.add(sectionHeader, 0, 0);
        Role[] roles = Role.values();
        for (int c = 0; c < roles.length; c++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setHalignment(HPos.CENTER);
            column.setMinWidth(96);
            matrix.getColumnConstraints().add(column);
            Label roleLabel = new Label(UserLabels.roleText(roles[c]));
            roleLabel.getStyleClass().add("security-matrix-header");
            matrix.add(roleLabel, c + 1, 0);
        }

        for (int r = 0; r < SECTIONS.size(); r++) {
            View section = SECTIONS.get(r);
            Label name = new Label(section.getTitle());
            name.getStyleClass().add("security-matrix-section");
            matrix.add(name, 0, r + 1);
            for (int c = 0; c < roles.length; c++) {
                matrix.add(accessMark(Permissions.canAccess(roles[c], section)), c + 1, r + 1);
            }
        }

        Label note = new Label("Los permisos los define la matriz de acceso del proyecto (D-11) y no se editan "
                + "desde la aplicación. «Perfil y cuenta» está disponible para todos los roles.");
        note.setWrapText(true);
        note.getStyleClass().add("settings-note");

        VBox card = card("Permisos por rol", IconSet.SHIELD, matrix, note);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private static Node accessMark(boolean allowed) {
        if (allowed) {
            Node check = IconSet.icon(IconSet.CHECK);
            check.getStyleClass().add("security-allowed");
            return check;
        }
        Label none = new Label("—");
        none.getStyleClass().add("security-denied");
        return none;
    }

    // ---------- último acceso ----------

    private VBox buildAccessCard() {
        accessTable = new PagedTable<>(PAGE_SIZE);
        accessTable.addColumn("Usuario", User::getFullName);
        accessTable.addColumn("Correo", User::getEmail);
        accessTable.addColumn("Rol", user -> UserLabels.roleText(user.getRole()));
        accessTable.addColumn("Estado", user -> StatusBadge.of(UserLabels.statusText(user.getStatus()),
                UserLabels.statusType(user.getStatus())));
        accessTable.addColumn("Último acceso", user -> lastAccessText(user.getLastAccess()));
        accessTable.setPrefHeight(420);

        return card("Último acceso de los usuarios", IconSet.USERS, accessTable);
    }

    static String lastAccessText(LocalDateTime lastAccess) {
        return lastAccess == null ? "Nunca" : Formatter.timestamp(lastAccess);
    }

    private void loadUsers() {
        Background.run(() -> AppContext.getInstance().getUserService().searchUsers("", null, null),
                users -> accessTable.setData(sortByLastAccess(users)),
                error -> accessTable.setData(List.of()));
    }

    /** Los accesos más recientes primero; quienes nunca entraron, al final. */
    static List<User> sortByLastAccess(List<User> users) {
        List<User> sorted = new ArrayList<>(users);
        sorted.sort(Comparator.comparing(User::getLastAccess,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return sorted;
    }

    // ---------- ayudas ----------

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
}
