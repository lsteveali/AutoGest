package com.autogest.config;

import com.autogest.dao.UserDao;
import com.autogest.repository.UserRepository;
import com.autogest.service.AuthenticationService;
import com.autogest.service.UserService;

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

    /** Fábrica de conexiones compartida por todos los DAO. */
    private final DatabaseConnection databaseConnection;

    // ===== Users (Leader) =====
    private final UserDao userDao;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;
    private final UserService userService;

    private AppContext() {
        // ===== Users (Leader) =====
        this.databaseConnection = new DatabaseConnection();
        this.userDao = new UserDao(databaseConnection);
        this.userRepository = new UserRepository(userDao);
        this.authenticationService = new AuthenticationService(userRepository);
        this.userService = new UserService(userRepository);

        // ===== Customers (Dev 1) =====
        // Sprint 2: CustomerDao -> CustomerRepository -> CustomerService

        // ===== Vehicles (Dev 2) =====
        // Sprint 2: VehicleDao -> VehicleRepository -> VehicleService

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
}
