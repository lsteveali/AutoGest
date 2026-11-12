package com.autogest.controller;

/**
 * Pantallas de la aplicación: cada constante conoce su ruta FXML, el título
 * que se muestra al usuario (en español) y la sección de la barra lateral
 * que activa.
 *
 * <p>Principles: High Cohesion, Indirection.</p>
 */
public enum View {

    HOME("/fxml/home/home.fxml", "Inicio", "HOME"),
    // Pantalla personal: la abre cualquier rol (menú del avatar) y también Configuración.
    SETTINGS_PROFILE("/fxml/settings/profile.fxml", "Perfil y cuenta", "SETTINGS"),
    // Secciones de Configuración del Líder (junto a HOME para no tocar las líneas de los View.diff de Dev 1).
    SETTINGS_SECURITY("/fxml/settings/security.fxml", "Seguridad", "SETTINGS"),
    SETTINGS_BACKUP("/fxml/settings/backup.fxml", "Copias de seguridad", "SETTINGS"),
    CUSTOMERS("/fxml/customer/customers.fxml", "Clientes", "CUSTOMERS"),
    CUSTOMER_NEW("/fxml/customer/customer-new.fxml", "Nuevo cliente", "CUSTOMERS"),
    CUSTOMER_EDIT("/fxml/customer/customer-edit.fxml", "Editar cliente", "CUSTOMERS"),
    CUSTOMER_DETAIL("/fxml/customer/customer-detail.fxml", "Detalle del cliente", "CUSTOMERS"),
    VEHICLES("/fxml/vehicle/vehicles.fxml", "Vehículos", "VEHICLES"),
    VEHICLE_NEW("/fxml/vehicle/vehicle-new.fxml", "Nuevo vehículo", "VEHICLES"),
    VEHICLE_EDIT("/fxml/vehicle/vehicle-edit.fxml", "Editar vehículo", "VEHICLES"),
    VEHICLE_DETAIL("/fxml/vehicle/vehicle-detail.fxml", "Detalle del vehículo", "VEHICLES"),
    ORDERS("/fxml/order/orders.fxml", "Órdenes de servicio", "ORDERS"),
    SERVICES("/fxml/catalog/services.fxml", "Servicios", "SERVICES"),
    SERVICE_NEW("/fxml/catalog/service-new.fxml", "Nuevo servicio", "SERVICES"),
    SERVICE_EDIT("/fxml/catalog/service-edit.fxml", "Editar servicio", "SERVICES"),
    SERVICE_DETAIL("/fxml/catalog/service-detail.fxml", "Detalle del servicio", "SERVICES"),
    INVENTORY("/fxml/inventory/inventory.fxml", "Inventario", "INVENTORY"),
    PART_NEW("/fxml/inventory/part-new.fxml", "Nuevo repuesto", "INVENTORY"),
    PART_EDIT("/fxml/inventory/part-edit.fxml", "Editar repuesto", "INVENTORY"),
    PART_DETAIL("/fxml/inventory/part-detail.fxml", "Detalle del repuesto", "INVENTORY"),
    BILLING("/fxml/billing/billing.fxml", "Facturación", "BILLING"),
    NOTIFICATIONS("/fxml/notification/notifications.fxml", "Notificaciones", "NOTIFICATIONS"),
    REPORTS("/fxml/report/reports.fxml", "Reportes", "REPORTS"),
    SETTINGS("/fxml/settings/settings.fxml", "Configuración", "SETTINGS"),
    USERS_ROLES("/fxml/user/users-roles.fxml", "Usuarios y roles", "SETTINGS"),
    ASSISTANT("/fxml/assistant/assistant.fxml", "Asistente AutoGest", "ASSISTANT");

    private final String fxmlPath;
    private final String title;
    private final String sidebarSection;

    View(String fxmlPath, String title, String sidebarSection) {
        this.fxmlPath = fxmlPath;
        this.title = title;
        this.sidebarSection = sidebarSection;
    }

    /** Ruta del recurso FXML de la pantalla. */
    public String getFxmlPath() {
        return fxmlPath;
    }

    /** Título de la pantalla mostrado al usuario (español). */
    public String getTitle() {
        return title;
    }

    /** Sección de la barra lateral que se activa al mostrar esta pantalla. */
    public View getSidebarSection() {
        return valueOf(sidebarSection);
    }
}