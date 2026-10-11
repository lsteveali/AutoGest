package com.autogest.controller.components;

import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.model.Role;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Barra lateral de navegación oficial (sección 3). Muestra los ítems en el
 * orden definido, resalta el ítem activo y delega la navegación en un
 * {@link Consumer} que asigna el shell, para no conocer al
 * {@code ViewNavigator}.
 *
 * <p>Principles: Pure Fabrication, Information Expert (de la navegación),
 * Low Coupling.</p>
 */
public class Sidebar extends VBox {

    /** Configuración de un ítem de la barra: vista, texto e ícono. */
    private record NavItem(View view, String label, String iconData) {
    }

    private static final List<NavItem> NAV_ITEMS = List.of(
            new NavItem(View.HOME, "Inicio", IconSet.HOME),
            new NavItem(View.CUSTOMERS, "Clientes", IconSet.USERS),
            new NavItem(View.VEHICLES, "Vehículos", IconSet.CAR),
            new NavItem(View.ORDERS, "Órdenes de servicio", IconSet.FILE_TEXT),
            new NavItem(View.SERVICES, "Servicios", IconSet.WRENCH),
            new NavItem(View.INVENTORY, "Inventario", IconSet.BOX),
            new NavItem(View.BILLING, "Facturación", IconSet.CREDIT_CARD),
            new NavItem(View.NOTIFICATIONS, "Notificaciones", IconSet.BELL),
            new NavItem(View.REPORTS, "Reportes", IconSet.BAR_CHART),
            // «Configuración» abre Usuarios y roles en este sprint (solo administrador).
            new NavItem(View.SETTINGS, "Configuración", IconSet.SETTINGS),
            new NavItem(View.ASSISTANT, "Asistente AutoGest", IconSet.SPARKLES));

    private final Map<View, HBox> itemsByView = new LinkedHashMap<>();
    private Consumer<View> onNavigate;
    private View activeView;

    /** Crea la barra lateral con el orden oficial de ítems. */
    public Sidebar() {
        getStyleClass().add("sidebar");

        Label brand = new Label("AutoGest");
        brand.getStyleClass().add("app-title");
        getChildren().add(brand);

        for (NavItem item : NAV_ITEMS) {
            HBox node = createItem(item);
            itemsByView.put(item.view(), node);
            getChildren().add(node);
        }
    }

    /**
     * Registra la acción de navegación que se ejecuta al pulsar un ítem.
     *
     * @param onNavigate consumidor que recibe la vista seleccionada
     */
    public void setOnNavigate(Consumer<View> onNavigate) {
        this.onNavigate = onNavigate;
    }

    /**
     * Muestra solo los ítems permitidos por la matriz de acceso (D-11) para el
     * rol indicado. Un rol nulo (sin sesión) deja visibles solo los ítems
     * públicos que {@link Permissions} autoriza.
     *
     * @param role rol del usuario en sesión, o {@code null} si no hay sesión
     */
    public void setRole(Role role) {
        for (Map.Entry<View, HBox> entry : itemsByView.entrySet()) {
            boolean allowed = Permissions.canAccess(role, entry.getKey());
            entry.getValue().setVisible(allowed);
            entry.getValue().setManaged(allowed);
        }
    }

    /**
     * Resalta el ítem activo correspondiente a la vista mostrada. Si la vista
     * es de detalle o edición, se resalta la sección de la que depende.
     *
     * @param view vista que se está mostrando
     */
    public void setActiveItem(View view) {
        if (view == null) {
            return;
        }
        View section = view.getSidebarSection();
        if (activeView != null && itemsByView.containsKey(activeView)) {
            itemsByView.get(activeView).getStyleClass().remove("sidebar-item-active");
        }
        HBox node = itemsByView.get(section);
        if (node != null) {
            if (!node.getStyleClass().contains("sidebar-item-active")) {
                node.getStyleClass().add("sidebar-item-active");
            }
            activeView = section;
        }
    }

    private HBox createItem(NavItem item) {
        Path icon = IconSet.icon(item.iconData());
        Label label = new Label(item.label());

        HBox box = new HBox(12, icon, label);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("sidebar-item");
        box.setOnMouseClicked(event -> {
            if (onNavigate != null) {
                onNavigate.accept(item.view());
            }
        });
        return box;
    }
}
