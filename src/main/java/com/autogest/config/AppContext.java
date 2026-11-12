package com.autogest.config;

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

        // ===== Inventario / Repuestos (Lider) =====
        this.partDao = new PartDao(databaseConnection);
        this.partRepository = new PartRepository(partDao);
        this.partService = new PartService(partRepository);

        // ===== Orders (Dev 1 / Dev 2) =====
        // Sprint 4: ServiceOrderDao -> ServiceOrderRepository -> ServiceOrderService

        // ===== Catalog (Dev 1) =====
        // Sprint 3: ServiceOfferingDao -> ServiceOfferingRepository -> CatalogService

        // ===== Inventory (Dev 2) =====
        // Sprint 3: PartDao -> PartRepository -> InventoryService
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
}
