package com.autogest.controller.assistant;

import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.controller.billing.InvoiceLabels;
import com.autogest.controller.catalog.ServiceLabels;
import com.autogest.controller.customer.CustomerLabels;
import com.autogest.controller.inventory.InventoryLabels;
import com.autogest.controller.notification.NotificationLabels;
import com.autogest.controller.order.OrderLabels;
import com.autogest.controller.user.UserLabels;
import com.autogest.controller.vehicle.VehicleLabels;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.model.Catalogs.Role;
import com.autogest.model.Catalogs.UserStatus;
import com.autogest.service.assistant.AssistantCatalog;
import com.autogest.service.assistant.AssistantCatalog.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Los módulos a los que puede llevar el Asistente: su pantalla de lista, de
 * creación y de detalle, las palabras con que se los nombra y los filtros de
 * su lista. Las opciones de los filtros salen de las mismas clases
 * {@code *Labels} que usan las pantallas, así el Asistente escribe exactamente
 * lo que se ve en cada lista.
 *
 * <p>{@link #catalogFor(Role)} deja solo lo que el rol puede abrir
 * ({@link Permissions}), así ningún motor propone un módulo vedado.</p>
 *
 * <p>Principles: Information Expert, Protected Variations.</p>
 */
public final class AssistantDestinations {

    /**
     * Un módulo.
     *
     * @param code        código estable (el que usan los motores)
     * @param main        pantalla principal (la lista)
     * @param create      formulario de creación, o {@code null}
     * @param detail      pantalla de detalle, o {@code null}
     * @param keywords    palabras que lo nombran (minúsculas, sin tildes)
     * @param createWords frases que piden crear un registro
     * @param filters     filtros de la lista, con sus opciones visibles
     */
    public record Entry(String code, View main, View create, View detail, List<String> keywords,
                        List<String> createWords, Supplier<List<Filter>> filters) {
    }

    private static final List<Entry> ENTRIES = List.of(
            entry("HOME", View.HOME, null, null,
                    List.of("inicio", "tablero", "pagina principal", "panel principal", "dashboard"), List.of(),
                    List::of),
            entry("CUSTOMERS", View.CUSTOMERS, View.CUSTOMER_NEW, View.CUSTOMER_DETAIL,
                    List.of("cliente", "clientes"),
                    List.of("nuevo cliente", "registrar cliente", "registrar un cliente", "agregar cliente",
                            "crear cliente"),
                    () -> List.of(new Filter("Estado", List.of(CustomerLabels.statusText(CustomerStatus.ACTIVE),
                            CustomerLabels.statusText(CustomerStatus.INACTIVE))))),
            entry("VEHICLES", View.VEHICLES, View.VEHICLE_NEW, View.VEHICLE_DETAIL,
                    List.of("vehiculo", "vehiculos", "carro", "carros", "moto", "motos", "auto", "autos",
                            "camioneta", "camionetas"),
                    List.of("nuevo vehiculo", "registrar vehiculo", "registrar un vehiculo", "agregar vehiculo",
                            "crear vehiculo"),
                    () -> List.of(new Filter("Estado", VehicleLabels.statusOptions()),
                            new Filter("Clase", VehicleLabels.typeOptions()))),
            entry("ORDERS", View.ORDERS, View.ORDER_NEW, View.ORDER_DETAIL,
                    List.of("orden", "ordenes", "orden de servicio", "ordenes de servicio", "trabajos"),
                    List.of("nueva orden", "crear orden", "crear una orden", "registrar orden", "recibir vehiculo",
                            "ingresar vehiculo", "ingreso de vehiculo"),
                    () -> List.of(new Filter("Estado", OrderLabels.statusOptions()))),
            entry("SERVICES", View.SERVICES, View.SERVICE_NEW, View.SERVICE_DETAIL,
                    List.of("servicios", "catalogo", "catalogo de servicios", "servicios del taller"),
                    List.of("nuevo servicio", "crear servicio", "agregar servicio"),
                    () -> List.of(new Filter("Estado", ServiceLabels.statusOptions()))),
            entry("INVENTORY", View.INVENTORY, View.PART_NEW, View.PART_DETAIL,
                    List.of("inventario", "repuesto", "repuestos", "stock", "existencias"),
                    List.of("nuevo repuesto", "crear repuesto", "agregar repuesto"),
                    () -> List.of(new Filter("Stock", InventoryLabels.stockOptions()))),
            entry("BILLING", View.BILLING, View.INVOICE_NEW, View.INVOICE_DETAIL,
                    List.of("factura", "facturas", "facturacion", "cobros"),
                    List.of("nueva factura", "facturar", "generar factura", "generar la factura", "crear factura",
                            "emitir factura"),
                    () -> List.of(new Filter("Estado", InvoiceLabels.statusOptions()),
                            new Filter("Fecha", InvoiceLabels.PERIODS.subList(1, InvoiceLabels.PERIODS.size())),
                            new Filter("Método", InvoiceLabels.methodOptions()))),
            entry("NOTIFICATIONS", View.NOTIFICATIONS, View.NOTIFICATION_NEW, View.NOTIFICATION_DETAIL,
                    List.of("notificacion", "notificaciones", "aviso", "avisos", "correos", "mensajes"),
                    List.of("nueva notificacion", "enviar aviso", "enviar un aviso", "enviar notificacion",
                            "enviar una notificacion", "enviar correo", "enviar un correo", "enviar mensaje",
                            "avisar al cliente", "programar notificacion"),
                    () -> List.of(new Filter("Tipo", NotificationLabels.typeOptions()),
                            new Filter("Canal", NotificationLabels.channelOptions()),
                            new Filter("Estado", NotificationLabels.statusOptions()))),
            entry("REPORTS", View.REPORTS, null, null,
                    List.of("reporte", "reportes", "estadisticas", "indicadores", "informe", "informes"), List.of(),
                    List::of),
            entry("USERS_ROLES", View.USERS_ROLES, null, null,
                    List.of("usuario", "usuarios", "roles", "empleados"), List.of(),
                    () -> List.of(new Filter("Rol", List.of(UserLabels.roleText(Role.ADMINISTRATOR),
                            UserLabels.roleText(Role.RECEPTIONIST), UserLabels.roleText(Role.MECHANIC),
                            UserLabels.roleText(Role.SUPERVISOR))),
                            new Filter("Estado", List.of(UserLabels.statusText(UserStatus.ACTIVE),
                                    UserLabels.statusText(UserStatus.INACTIVE))))),
            entry("SETTINGS", View.SETTINGS, null, null, List.of("configuracion", "ajustes"), List.of(), List::of),
            entry("SETTINGS_PROFILE", View.SETTINGS_PROFILE, null, null,
                    List.of("mi perfil", "perfil", "mi cuenta", "contrasena", "cambiar contrasena", "mi contrasena"),
                    List.of(), List::of),
            entry("SETTINGS_WORKSHOP", View.SETTINGS_WORKSHOP, null, null,
                    List.of("datos del taller", "nit del taller", "direccion del taller"), List.of(), List::of),
            entry("SETTINGS_PREFERENCES", View.SETTINGS_PREFERENCES, null, null,
                    List.of("preferencias", "preferencias del sistema"), List.of(), List::of),
            entry("SETTINGS_SECURITY", View.SETTINGS_SECURITY, null, null,
                    List.of("seguridad", "politica de contrasenas"), List.of(), List::of),
            entry("SETTINGS_BACKUP", View.SETTINGS_BACKUP, null, null,
                    List.of("copia de seguridad", "copias de seguridad", "respaldo", "respaldos", "backup"),
                    List.of(), List::of),
            entry("SETTINGS_BILLING", View.SETTINGS_BILLING, null, null,
                    List.of("configuracion de facturacion", "iva", "impuestos", "prefijo de factura"), List.of(),
                    List::of),
            entry("SETTINGS_NOTIFICATIONS", View.SETTINGS_NOTIFICATIONS, null, null,
                    List.of("configuracion de notificaciones", "plantillas", "plantillas de correo"), List.of(),
                    List::of));

    private AssistantDestinations() {
        // Solo datos: no se instancia.
    }

    private static Entry entry(String code, View main, View create, View detail, List<String> keywords,
                               List<String> createWords, Supplier<List<Filter>> filters) {
        return new Entry(code, main, create, detail, keywords, createWords, filters);
    }

    /** Todos los módulos, en el orden de la barra lateral. */
    public static List<Entry> all() {
        return ENTRIES;
    }

    /**
     * @param code código del módulo
     * @return el módulo, si existe
     */
    public static Optional<Entry> find(String code) {
        return ENTRIES.stream().filter(entry -> entry.code().equals(code)).findFirst();
    }

    /**
     * Catálogo para un rol: solo los módulos que puede abrir; la creación
     * solo si también puede abrir el formulario.
     *
     * @param role rol del usuario en sesión
     * @return catálogo para los motores del Asistente
     */
    public static AssistantCatalog catalogFor(Role role) {
        List<AssistantCatalog.Destination> destinations = new ArrayList<>();
        for (Entry entry : ENTRIES) {
            if (!Permissions.canAccess(role, entry.main())) {
                continue;
            }
            boolean canCreate = entry.create() != null && Permissions.canAccess(role, entry.create());
            destinations.add(new AssistantCatalog.Destination(entry.code(), entry.main().getTitle(),
                    entry.keywords(), canCreate ? entry.createWords() : List.of(), entry.filters().get(), List.of()));
        }
        return new AssistantCatalog(destinations);
    }
}
