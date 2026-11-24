package com.autogest.config;

import com.autogest.dao.SettingsDao;
import com.autogest.repository.SettingsRepository;
import com.autogest.service.SettingsService;
import com.autogest.dao.NotificationDao;
import com.autogest.repository.NotificationRepository;
import com.autogest.service.NotificationService;
import com.autogest.dao.DiagnosisDao;
import com.autogest.dao.OrderHistoryDao;
import com.autogest.dao.OrderLineDao;
import com.autogest.dao.ServiceOrderDao;
import com.autogest.repository.ServiceOrderRepository;
import com.autogest.service.ServiceOrderService;
import com.autogest.dao.CustomerDao;
import com.autogest.dao.PartDao;
import com.autogest.dao.ServiceOfferingDao;
import com.autogest.dao.UserDao;
import com.autogest.dao.VehicleDao;
import com.autogest.repository.CustomerRepository;
import com.autogest.repository.PartRepository;
import com.autogest.repository.ServiceOfferingRepository;
import com.autogest.repository.UserRepository;
import com.autogest.repository.VehicleRepository;
import com.autogest.service.AuthenticationService;
import com.autogest.service.CustomerService;
import com.autogest.service.PartService;
import com.autogest.service.ServiceOfferingService;
import com.autogest.service.UserService;
import com.autogest.service.VehicleService;
import com.autogest.dao.SupplierDao;
import com.autogest.repository.InventoryMovementRepository;
import com.autogest.repository.SupplierRepository;
import com.autogest.service.InventoryService;
import com.autogest.service.SupplierService;
import com.autogest.dao.InventoryMovementDao;
/**
 * Punto único de conexión de dependencias de AutoGest. Es el único lugar donde
 * se crean los DAO, Repository y Service, y los entrega ya armados al resto del
 * sistema a través de sus getters. Así ninguna otra clase hace {@code new} de
 * una dependencia y todas reciben lo que necesitan por el constructor.
 *
 * <p>Cada rol agrega su bloque marcado (DAO -> Repository -> Service) cuando
 * implementa su módulo. Los controladores obtienen sus Services con
 * {@code AppContext.getInstance().getXxxService()}.</p>
 *
 * <p>Principles: DIP, Low Coupling, Pure Fabrication.</p>
 */
public final class AppContext {

    private static AppContext instance;

    /** Carpeta local de las copias de seguridad: {@code <inicio del usuario>/AutoGest/copias-de-seguridad}. */
    private static final java.nio.file.Path BACKUP_DIRECTORY =
            java.nio.file.Path.of(System.getProperty("user.home"), "AutoGest", "copias-de-seguridad");

    /** Fábrica de conexiones compartida por todos los DAO. */
    private final DatabaseConnection databaseConnection;

    // ===== Users (Leader) =====
    private final UserDao userDao;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    // ===== Copias de seguridad (Líder, Sprint 8) =====
    // Nombres completos de clase: así no se tocan los imports, que modifican los AppContext.diff de Dev 1.
    private final com.autogest.service.BackupService backupService;

    // ===== Seguimiento de órdenes (Líder, Sprint 4) =====
    // Se crea al pedirlo por primera vez: depende de servicios que se arman al final del
    // constructor, y así este bloque no toca las líneas que modifican los AppContext.diff de Dev 1.
    private com.autogest.service.OrderWorkflowService orderWorkflowService;

    // ===== Facturación (Líder, Sprint 5) =====
    // Se crea al pedirlo, como el seguimiento de órdenes: depende de las órdenes (final del constructor).
    private com.autogest.service.InvoiceService invoiceService;
    private final com.autogest.service.InvoicePdfService invoicePdfService =
            new com.autogest.service.InvoicePdfService();

    // ===== Avisos automáticos (Líder, Sprint 6) =====
    // Se crea al pedirlo: usa el NotificationService de Dev 1, que se arma al final del constructor.
    private com.autogest.service.NotificationEventService notificationEventService;
    // Correos de cuentas y recuperación de contraseña (Líder)
    private com.autogest.service.AccountMailer accountMailer;
    private com.autogest.service.PasswordRecoveryService passwordRecoveryService;

    // ===== Reportes (Líder, Sprint 7) =====
    // Se crea al pedirlo: usa servicios que se arman al final del constructor.
    private com.autogest.service.ReportService reportService;
    private final com.autogest.service.ReportPdfService reportPdfService =
            new com.autogest.service.ReportPdfService();

    // ===== Configuración de Facturación y de Notificaciones (Líder, Sprint 8) =====
    // Se crean al pedirlas: usan el SettingsDao de Dev 1, que se arma al final del constructor.
    private com.autogest.service.BillingSettingsService billingSettingsService;
    private com.autogest.service.NotificationSettingsService notificationSettingsService;

    // ===== Asistente AutoGest (Líder, Sprint 9) =====
    // Se crea al pedirlo: con IA si application.local.properties tiene una llave; si no, solo reglas.
    private com.autogest.service.assistant.AssistantService assistantService;

    // ===== Customers (Dev 1) =====
    private final CustomerDao customerDao;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;

    // ===== Vehicles (Dev 2) =====
    private final VehicleDao vehicleDao;
    private final VehicleRepository vehicleRepository;
    private final VehicleService vehicleService;

    // ===== Catalogo de servicios (Lider) =====
    private final ServiceOfferingDao serviceOfferingDao;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final ServiceOfferingService serviceOfferingService;

    // ===== Inventario / Repuestos (Lider) =====
    private final PartDao partDao;
    private final PartRepository partRepository;
    private final PartService partService;

    // ===== Inventory: proveedores y movimientos (Dev 1) =====
    private final SupplierDao supplierDao;
    private final SupplierRepository supplierRepository;
    private final SupplierService supplierService;
    private final InventoryMovementDao inventoryMovementDao;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final InventoryService inventoryService;

    // ===== Orders (Dev 1) =====
    private final OrderLineDao orderLineDao;
    private final OrderHistoryDao orderHistoryDao;
    private final DiagnosisDao diagnosisDao;
    private final ServiceOrderDao serviceOrderDao;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceOrderService serviceOrderService;

    // ===== Notifications (Dev 1) =====
    private final NotificationDao notificationDao;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    // ===== Settings: datos del taller y preferencias (Dev 1) =====
    private final SettingsDao settingsDao;
    private final SettingsRepository settingsRepository;
    private final SettingsService settingsService;

    private AppContext() {
        // ===== Users (Leader) =====
        this.databaseConnection = new DatabaseConnection();
        this.userDao = new UserDao(databaseConnection);
        this.userRepository = new UserRepository(userDao);
        this.authenticationService = new AuthenticationService(userRepository);
        this.userService = new UserService(userRepository);

        // ===== Copias de seguridad (Líder, Sprint 8) =====
        this.backupService = new com.autogest.service.BackupService(
                new com.autogest.dao.BackupDao(databaseConnection),
                new com.autogest.repository.BackupRepository(BACKUP_DIRECTORY));

        // ===== Customers (Dev 1) =====
        this.customerDao = new CustomerDao(databaseConnection);
        this.customerRepository = new CustomerRepository(customerDao);
        this.customerService = new CustomerService(customerRepository);

        // ===== Vehicles (Dev 2) =====
        this.vehicleDao = new VehicleDao(databaseConnection);
        this.vehicleRepository = new VehicleRepository(vehicleDao);
        this.vehicleService = new VehicleService(vehicleRepository, customerRepository);

        // ===== Catalogo de servicios (Lider) =====
        this.serviceOfferingDao = new ServiceOfferingDao(databaseConnection);
        this.serviceOfferingRepository = new ServiceOfferingRepository(serviceOfferingDao);
        this.serviceOfferingService = new ServiceOfferingService(serviceOfferingRepository);

        // ===== Inventory: proveedores (Dev 1) — antes de Repuestos =====
        this.supplierDao = new SupplierDao(databaseConnection);
        this.supplierRepository = new SupplierRepository(supplierDao);
        this.supplierService = new SupplierService(supplierRepository);

        // ===== Inventario / Repuestos (Lider) =====
        this.partDao = new PartDao(databaseConnection);
        this.partRepository = new PartRepository(partDao, supplierRepository);
        this.partService = new PartService(partRepository);

        // ===== Inventory: movimientos (Dev 1) =====
        this.inventoryMovementDao = new InventoryMovementDao(databaseConnection);
        this.inventoryMovementRepository = new InventoryMovementRepository(inventoryMovementDao, partRepository, userRepository);
        this.inventoryService = new InventoryService(inventoryMovementRepository, partService);

        // ===== Orders (Dev 1) =====
        this.orderLineDao = new OrderLineDao(databaseConnection);
        this.orderHistoryDao = new OrderHistoryDao(databaseConnection);
        this.diagnosisDao = new DiagnosisDao(databaseConnection);
        this.serviceOrderDao = new ServiceOrderDao(databaseConnection, orderLineDao, orderHistoryDao);
        this.serviceOrderRepository = new ServiceOrderRepository(serviceOrderDao, orderLineDao,
                orderHistoryDao, diagnosisDao, vehicleRepository, customerRepository, userRepository,
                serviceOfferingRepository, partRepository);
        this.serviceOrderService = new ServiceOrderService(serviceOrderRepository, vehicleService);

        // ===== Notifications (Dev 1) =====
        this.notificationDao = new NotificationDao(databaseConnection);
        this.notificationRepository = new NotificationRepository(notificationDao, customerRepository);
        this.notificationService = new NotificationService(notificationRepository, customerService);

        // ===== Settings: datos del taller y preferencias (Dev 1) =====
        this.settingsDao = new SettingsDao(databaseConnection);
        this.settingsRepository = new SettingsRepository(settingsDao);
        this.settingsService = new SettingsService(settingsRepository);

    }

    /**
     * Devuelve la instancia única del contexto de la aplicación.
     *
     * @return instancia compartida de {@code AppContext}
     */
    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
            instance.configureEmail();
        }
        return instance;
    }

    /**
     * Envío real de correos (Líder): si {@code application.local.properties}
     * tiene una cuenta ({@code mail.*}), las notificaciones por correo se
     * envían por SMTP, con la factura adjunta en los de Facturación; si no,
     * se siguen simulando. Se hace aquí y no en el
     * constructor para no tocar las líneas de los AppContext.diff de Dev 1.
     */
    private void configureEmail() {
        notificationService.setEmailSender(EmailConfig.fromLocalFile());
        // Correos de las cuentas (Líder): bienvenida al crear un usuario y código para
        // recuperar la contraseña. Bienvenida a los clientes nuevos, en segundo plano
        // para que «Guardar» no espere la respuesta de Gmail.
        accountMailer = new com.autogest.service.AccountMailer(EmailConfig::fromLocalFile);
        userService.setAccountMailer(accountMailer);
        customerService.setOnRegistered(customer -> Thread.ofVirtual().start(() -> {
            try {
                getNotificationEventService().welcomeCustomer(customer);
            } catch (RuntimeException ignored) {
                // Si el correo falla queda como «Fallida» en Notificaciones para reintentar.
            }
        }));
        // Los correos de Facturación llevan la factura en PDF y PNG (si está activo en Configuración).
        notificationService.setAttachmentProvider(new com.autogest.service.InvoiceAttachmentProvider(
                this::getInvoiceService, invoicePdfService,
                () -> getNotificationSettingsService().current().isAttachInvoice()));
        configureSettings();
    }

    /**
     * Configuración (Líder, Sprint 8): las plantillas toman los textos
     * editados y el nombre del taller; los PDF, los datos del taller y la nota
     * de la factura.
     */
    private void configureSettings() {
        com.autogest.service.NotificationTemplate.useTextSource(getNotificationSettingsService());
        invoicePdfService.setDocumentSources(() -> getSettingsService().getWorkshopProfile(),
                () -> getBillingSettingsService().current());
        reportPdfService.setWorkshop(() -> getSettingsService().getWorkshopProfile());
    }

    /**
     * Cierra el pool de conexiones si el contexto llegó a crearse. Se llama al
     * cerrar la aplicación.
     */
    public static synchronized void shutdown() {
        if (instance != null) {
            instance.databaseConnection.close();
            instance = null;
        }
    }

    // ===== Users (Leader) =====

    /**
     * Devuelve el servicio de autenticación (login y logout).
     *
     * @return servicio de autenticación compartido
     */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    /**
     * Devuelve el servicio de usuarios (consulta y gestión).
     *
     * @return servicio de usuarios compartido
     */
    public UserService getUserService() {
        return userService;
    }

    // ===== Recuperar contraseña (Líder) =====

    /**
     * Devuelve el servicio de «¿Olvidaste tu contraseña?» (código por correo).
     *
     * @return servicio compartido: guarda en memoria los códigos pendientes
     */
    public synchronized com.autogest.service.PasswordRecoveryService getPasswordRecoveryService() {
        if (passwordRecoveryService == null) {
            passwordRecoveryService = new com.autogest.service.PasswordRecoveryService(userRepository, userService,
                    accountMailer == null ? new com.autogest.service.AccountMailer(EmailConfig::fromLocalFile)
                            : accountMailer);
        }
        return passwordRecoveryService;
    }

    // ===== Copias de seguridad (Líder, Sprint 8) =====

    /**
     * Devuelve el servicio de copias de seguridad.
     *
     * @return servicio de copias compartido
     */
    public com.autogest.service.BackupService getBackupService() {
        return backupService;
    }

    // ===== Seguimiento de órdenes (Líder, Sprint 4) =====

    /**
     * Devuelve el servicio de seguimiento de órdenes (estado, historial,
     * diagnóstico, responsable, líneas y stock).
     *
     * @return servicio de seguimiento compartido
     */
    public synchronized com.autogest.service.OrderWorkflowService getOrderWorkflowService() {
        if (orderWorkflowService == null) {
            orderWorkflowService = new com.autogest.service.OrderWorkflowService(serviceOrderRepository,
                    serviceOrderService, vehicleService, inventoryService, partService, userService,
                    getNotificationEventService());
        }
        return orderWorkflowService;
    }

    // ===== Facturación (Líder, Sprint 5) =====

    /**
     * Devuelve el servicio de facturación (generar, pagar, anular y vencidas).
     *
     * @return servicio de facturación compartido
     */
    public synchronized com.autogest.service.InvoiceService getInvoiceService() {
        if (invoiceService == null) {
            invoiceService = new com.autogest.service.InvoiceService(
                    new com.autogest.repository.InvoiceRepository(
                            new com.autogest.dao.InvoiceDao(databaseConnection)),
                    serviceOrderService, serviceOrderRepository, notificationService);
            invoiceService.setSettings(() -> getBillingSettingsService().current());
            invoiceService.setNoticesEnabled(() -> getNotificationSettingsService().current().isBillingNoticesEnabled());
        }
        return invoiceService;
    }

    // ===== Configuración de Facturación y de Notificaciones (Líder, Sprint 8) =====

    /**
     * Devuelve la configuración de facturación (IVA, plazo, numeración,
     * métodos de pago y nota).
     *
     * @return servicio compartido
     */
    public synchronized com.autogest.service.BillingSettingsService getBillingSettingsService() {
        if (billingSettingsService == null) {
            billingSettingsService = new com.autogest.service.BillingSettingsService(
                    new com.autogest.repository.BillingSettingsRepository(settingsDao));
        }
        return billingSettingsService;
    }

    /**
     * Devuelve la configuración de notificaciones (avisos automáticos y
     * plantillas). El nombre del taller sale de «Datos del taller» (Dev 1).
     *
     * @return servicio compartido
     */
    public synchronized com.autogest.service.NotificationSettingsService getNotificationSettingsService() {
        if (notificationSettingsService == null) {
            notificationSettingsService = new com.autogest.service.NotificationSettingsService(
                    new com.autogest.repository.NotificationSettingsRepository(settingsDao),
                    () -> getSettingsService().getWorkshopProfile().getDisplayName());
        }
        return notificationSettingsService;
    }

    /**
     * Devuelve el generador del PDF de las facturas.
     *
     * @return generador de PDF compartido
     */
    public com.autogest.service.InvoicePdfService getInvoicePdfService() {
        return invoicePdfService;
    }

    // ===== Avisos automáticos (Líder, Sprint 6) =====

    /**
     * Devuelve los avisos automáticos al cliente (vehículo listo y
     * recordatorios de mantenimiento).
     *
     * @return servicio de avisos compartido
     */
    public synchronized com.autogest.service.NotificationEventService getNotificationEventService() {
        if (notificationEventService == null) {
            notificationEventService = new com.autogest.service.NotificationEventService(notificationService,
                    vehicleService);
            notificationEventService.setSettings(() -> getNotificationSettingsService().current());
        }
        return notificationEventService;
    }

    // ===== Asistente AutoGest (Líder, Sprint 9) =====

    /**
     * Devuelve el motor del Asistente: con IA si hay una llave configurada y,
     * si no, solo con las reglas locales.
     *
     * @return servicio del Asistente compartido
     */
    public synchronized com.autogest.service.assistant.AssistantService getAssistantService() {
        if (assistantService == null) {
            com.autogest.service.assistant.AssistantService.AiEngine engine;
            try {
                engine = AssistantConfig.fromLocalFile();
            } catch (RuntimeException e) {
                // Sin archivo de configuración legible: el Asistente sigue con las reglas locales.
                engine = null;
            }
            assistantService = new com.autogest.service.assistant.AssistantService(engine);
        }
        return assistantService;
    }

    // ===== Reportes (Líder, Sprint 7) =====

    /**
     * Devuelve el servicio que arma los reportes del taller.
     *
     * @return servicio de reportes compartido
     */
    public synchronized com.autogest.service.ReportService getReportService() {
        if (reportService == null) {
            reportService = new com.autogest.service.ReportService(
                    new com.autogest.repository.ReportRepository(new com.autogest.dao.ReportDao(databaseConnection)),
                    serviceOrderService, inventoryService, partService, notificationService);
        }
        return reportService;
    }

    /**
     * Devuelve el generador del PDF de los reportes.
     *
     * @return generador de PDF compartido
     */
    public com.autogest.service.ReportPdfService getReportPdfService() {
        return reportPdfService;
    }

    // ===== Customers (Dev 1) =====

    /**
     * Devuelve el servicio de clientes (registro, consulta y baja lógica).
     *
     * @return servicio de clientes compartido
     */
    public CustomerService getCustomerService() {
        return customerService;
    }

    // ===== Vehicles (Dev 2) =====

    /**
     * Devuelve el servicio de vehículos (registro, consulta y baja lógica).
     *
     * @return servicio de vehículos compartido
     */
    public VehicleService getVehicleService() {
        return vehicleService;
    }

    /**
     * Devuelve el servicio del catálogo de servicios.
     */
    public ServiceOfferingService getServiceOfferingService() {
        return serviceOfferingService;
    }

    /**
     * Devuelve el servicio de repuestos/inventario.
     */
    public PartService getPartService() {
        return partService;
    }

    // ===== Inventory (Dev 1) =====

    /**
     * Devuelve el servicio de proveedores de repuestos.
     *
     * @return servicio de proveedores compartido
     */
    public SupplierService getSupplierService() {
        return supplierService;
    }

    /**
     * Devuelve el servicio de movimientos de inventario (entradas, salidas y ajustes).
     *
     * @return servicio de inventario compartido
     */
    public InventoryService getInventoryService() {
        return inventoryService;
    }

    // ===== Orders (Dev 1) =====

    /**
     * Devuelve el servicio de órdenes de servicio.
     *
     * @return servicio de órdenes compartido
     */
    public ServiceOrderService getServiceOrderService() {
        return serviceOrderService;
    }

    // ===== Notifications (Dev 1) =====

    /**
     * Devuelve el servicio de notificaciones a clientes.
     *
     * @return servicio de notificaciones compartido
     */
    public NotificationService getNotificationService() {
        return notificationService;
    }

    // ===== Settings (Dev 1) =====

    /**
     * Devuelve el servicio de configuración (datos del taller y preferencias).
     *
     * @return servicio de configuración compartido
     */
    public SettingsService getSettingsService() {
        return settingsService;
    }

}