package com.autogest.controller.components;

import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.model.Role;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;

import java.net.URL;
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

    /** Grupo de ítems con su título (null = sin título). */
    private record NavSection(String title, List<NavItem> items) {
    }

    /**
     * Mismo orden oficial de antes, agrupado: el día a día del taller, la
     * gestión del negocio y la administración. El Asistente va fijo al pie.
     */
    private static final List<NavSection> NAV_SECTIONS = List.of(
            new NavSection(null, List.of(
                    new NavItem(View.HOME, "Inicio", IconSet.HOME))),
            new NavSection("Taller", List.of(
                    new NavItem(View.CUSTOMERS, "Clientes", IconSet.USERS),
                    new NavItem(View.VEHICLES, "Vehículos", IconSet.CAR),
                    new NavItem(View.ORDERS, "Órdenes de servicio", IconSet.FILE_TEXT),
                    new NavItem(View.SERVICES, "Servicios", IconSet.WRENCH),
                    new NavItem(View.INVENTORY, "Inventario", IconSet.BOX))),
            new NavSection("Gestión", List.of(
                    new NavItem(View.BILLING, "Facturación", IconSet.CREDIT_CARD),
                    new NavItem(View.NOTIFICATIONS, "Notificaciones", IconSet.BELL),
                    new NavItem(View.REPORTS, "Reportes", IconSet.BAR_CHART),
                    // «Configuración» abre Usuarios y roles en este sprint (solo administrador).
                    new NavItem(View.SETTINGS, "Configuración", IconSet.SETTINGS))));

    private static final NavItem ASSISTANT_ITEM =
            new NavItem(View.ASSISTANT, "Asistente AutoGest", IconSet.SPARKLES);

    /** Lado del espacio fijo de cada ícono: alinea los textos aunque los trazos midan distinto. */
    private static final double ICON_SLOT = 24;

    /** Logotipo completo con «AutoGest» en blanco, para el fondo Onyx de la barra. */
    private static final String LOGO = "/images/logo-full-dark.png";
    /** Ancho del logotipo en la barra (la imagen tiene el doble, para pantallas nítidas). */
    private static final double LOGO_WIDTH = 180;

    private final Map<View, HBox> itemsByView = new LinkedHashMap<>();
    /** Título de cada sección con sus ítems, para ocultarlo si el rol no ve ninguno. */
    private final Map<Label, List<View>> sectionTitles = new LinkedHashMap<>();
    private Consumer<View> onNavigate;
    private View activeView;

    /** Crea la barra lateral con el orden oficial de ítems. */
    public Sidebar() {
        getStyleClass().add("sidebar");

        HBox brand = new HBox();
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("sidebar-brand");
        URL logo = Sidebar.class.getResource(LOGO);
        if (logo != null) {
            ImageView logoView = new ImageView(new Image(logo.toExternalForm()));
            logoView.setFitWidth(LOGO_WIDTH);
            logoView.setPreserveRatio(true);
            logoView.setSmooth(true);
            logoView.setAccessibleText("AutoGest");
            brand.getChildren().add(logoView);
        } else {
            // Sin la imagen, al menos el nombre.
            Label title = new Label("AutoGest");
            title.getStyleClass().add("app-title");
            brand.getChildren().add(title);
        }
        getChildren().add(brand);

        for (NavSection section : NAV_SECTIONS) {
            List<View> views = new ArrayList<>();
            if (section.title() != null) {
                Label sectionTitle = new Label(section.title());
                sectionTitle.getStyleClass().add("sidebar-section");
                getChildren().add(sectionTitle);
                sectionTitles.put(sectionTitle, views);
            }
            for (NavItem item : section.items()) {
                addItem(item);
                views.add(item.view());
            }
        }

        // El Asistente queda al pie, separado de la navegación por módulos.
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        getChildren().add(spacer);
        addItem(ASSISTANT_ITEM);
        itemsByView.get(View.ASSISTANT).getStyleClass().add("sidebar-item-footer");
    }

    private void addItem(NavItem item) {
        HBox node = createItem(item);
        itemsByView.put(item.view(), node);
        getChildren().add(node);
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
        for (Map.Entry<Label, List<View>> entry : sectionTitles.entrySet()) {
            boolean anyVisible = entry.getValue().stream().anyMatch(v -> itemsByView.get(v).isVisible());
            entry.getKey().setVisible(anyVisible);
            entry.getKey().setManaged(anyVisible);
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
        StackPane iconSlot = new StackPane(icon);
        iconSlot.setMinSize(ICON_SLOT, ICON_SLOT);
        iconSlot.setPrefSize(ICON_SLOT, ICON_SLOT);
        iconSlot.setMaxSize(ICON_SLOT, ICON_SLOT);
        Label label = new Label(item.label());

        HBox box = new HBox(12, iconSlot, label);
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
