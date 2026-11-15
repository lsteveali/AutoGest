package com.autogest.config;

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

    }

    /**
     * Devuelve la instancia única del contexto de la aplicación.
     *
     * @return instancia compartida de {@code AppContext}
     */
    public static synchronized AppContext getInstance() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
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
                    serviceOrderService, vehicleService, inventoryService, partService, userService);
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
                    serviceOrderService, serviceOrderRepository);
        }
        return invoiceService;
    }

    /**
     * Devuelve el generador del PDF de las facturas.
     *
     * @return generador de PDF compartido
     */
    public com.autogest.service.InvoicePdfService getInvoicePdfService() {
        return invoicePdfService;
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

}