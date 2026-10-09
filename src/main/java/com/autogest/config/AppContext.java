package com.autogest.config;

/**
 * Punto único de conexión de dependencias de AutoGest. Es el único lugar donde
 * se crean los DAO, Repository y Service, y los entrega ya armados al resto del
 * sistema a través de sus getters. Así ninguna otra clase hace {@code new} de
 * una dependencia y todas reciben lo que necesitan por el constructor.
 *
 * <p>En el Sprint 1 solo existe el esqueleto: cada rol agrega su bloque marcado
 * (DAO -> Repository -> Service) cuando implementa su módulo. Los
 * controladores obtienen sus Services con
 * {@code AppContext.getInstance().getXxxService()}.</p>
 *
 * <p>Principles: DIP, Low Coupling, Pure Fabrication.</p>
 */
public final class AppContext {

    private static AppContext instance;

    private AppContext() {
        // La creación de dependencias se agrega en cada bloque marcado.
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

    // ===== Customers (Dev 1) =====
    // Sprint 2: CustomerDao -> CustomerRepository -> CustomerService

    // ===== Vehicles (Dev 2) =====
    // Sprint 2: VehicleDao -> VehicleRepository -> VehicleService

    // ===== Users (Leader) =====
    // Sprint 2: UserDao -> UserRepository -> UserService / AuthenticationService

    // ===== Orders (Dev 1 / Dev 2) =====
    // Sprint 4: ServiceOrderDao -> ServiceOrderRepository -> ServiceOrderService

    // ===== Catalog (Dev 1) =====
    // Sprint 3: ServiceOfferingDao -> ServiceOfferingRepository -> CatalogService

    // ===== Inventory (Dev 2) =====
    // Sprint 3: PartDao -> PartRepository -> InventoryService
}
