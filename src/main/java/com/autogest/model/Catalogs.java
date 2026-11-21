package com.autogest.model;

/**
 * Catálogos del dominio: todos los valores fijos de AutoGest (roles, estados,
 * tipos, canales…) en un solo archivo, por sugerencia del docente para tener
 * menos archivos en el modelo.
 *
 * <p>Cada catálogo sigue siendo un enum <b>independiente</b>, así el
 * compilador sigue impidiendo, por ejemplo, poner un {@code Role} donde va un
 * {@code InvoiceStatus}. Se usan con su import, igual que antes:
 * {@code import com.autogest.model.Catalogs.Role;} y luego
 * {@code Role.ADMINISTRATOR}.</p>
 *
 * <p>Los valores se guardan en la base de datos como texto con el
 * {@code name()} de la constante; por eso sus nombres no cambian. Los textos
 * que ve el usuario (en español) viven en las clases {@code *Labels} de cada
 * módulo, no aquí.</p>
 *
 * <p>Principles: High Cohesion (cada enum conserva su responsabilidad),
 * Protected Variations (los nombres guardados no cambian).</p>
 */
public final class Catalogs {

    private Catalogs() {
        // Solo agrupa los catálogos: no se instancia.
    }

    // ===== Usuarios =====

    /**
     * Perfil de permisos de un {@code User}. No existen subclases de {@code User} por rol: el rol es un atributo.
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion, Low Coupling.</p>
     */
    public enum Role {

        /** Acceso completo al sistema. */
        ADMINISTRATOR,
        /** Clientes, vehículos, órdenes y facturación. */
        RECEPTIONIST,
        /** Órdenes de servicio, diagnósticos y servicios; puede ser responsable de una orden. */
        MECHANIC,
        /** Órdenes, inventario y reportes. */
        SUPERVISOR
    }

    /**
     * Estado de una cuenta de usuario.
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum UserStatus {

        /** El usuario puede iniciar sesión. */
        ACTIVE,
        /** El usuario fue dado de baja (baja lógica). */
        INACTIVE
    }

    // ===== Clientes =====

    /**
     * Estado de un cliente del taller.
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum CustomerStatus {

        /** El cliente puede recibir nuevas órdenes. */
        ACTIVE,
        /** El cliente fue dado de baja (baja lógica); conserva su historial. */
        INACTIVE
    }
    /**
     * Tipo de documento de identidad de un cliente. Valores iniciales, ampliables (decisión D-06).
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion, Protected Variations.</p>
     */
    public enum DocumentType {

        /** Cédula de ciudadanía. */
        CC,
        /** Cédula de extranjería. */
        CE,
        /** Número de identificación tributaria. */
        NIT,
        /** Pasaporte. */
        PASSPORT
    }


    // ===== Vehículos =====

    /**
     * Estado guardado de un vehículo. El aviso de mantenimiento próximo NO es un estado: se calcula con {@code Vehicle.isMaintenanceDue} (decisión D-13).
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum VehicleStatus {

        /** Vehículo disponible, sin orden activa. */
        ACTIVE,
        /** Vehículo con una orden de servicio en curso. */
        IN_SERVICE,
        /** Vehículo dado de baja. */
        INACTIVE
    }

    /**
     * Clase de vehículo. Valores iniciales (decisión D-13).
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion, Protected Variations.</p>
     */
    public enum VehicleType {

        /** Automóvil. */
        CAR,
        /** Camioneta o furgón. */
        VAN,
        /** Motocicleta. */
        MOTORCYCLE,
        /** Otro tipo. */
        OTHER
    }

    /**
     * Tipo de combustible de un vehículo. Valores iniciales, ampliables (decisión D-06).
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion, Protected Variations.</p>
     */
    public enum FuelType {

        /** Gasolina. */
        GASOLINE,
        /** Diésel. */
        DIESEL,
        /** Híbrido. */
        HYBRID,
        /** Eléctrico. */
        ELECTRIC,
        /** Gas. */
        GAS
    }
    /**
     * Tipo de transmisión de un vehículo (decisión D-13).
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum Transmission {

        /** Manual. */
        MANUAL,
        /** Automática. */
        AUTOMATIC
    }


    // ===== Servicios e inventario =====

    /**
     * Estado de un servicio del catálogo.
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum ServiceOfferingStatus {

        /** El servicio se puede agregar a nuevas órdenes. */
        ACTIVE,
        /** El servicio ya no se ofrece. */
        INACTIVE
    }

    /**
     * Nivel de existencias de un repuesto; se deriva de {@code currentStock} y {@code minimumStock}.
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum StockStatus {

        /** Existencias por encima del mínimo. */
        AVAILABLE,
        /** Existencias menores o iguales al mínimo. */
        LOW_STOCK,
        /** Sin existencias. */
        OUT_OF_STOCK
    }
    /**
     * Tipo de movimiento de inventario.
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum MovementType {

        /** Entrada: aumenta el stock. */
        ENTRY,
        /** Salida: disminuye el stock. */
        EXIT,
        /** Ajuste manual de existencias. */
        ADJUSTMENT
    }

    // ===== Órdenes de servicio =====

    /**
     * Estado de una orden de servicio. Las constantes están en el orden del flujo; las transiciones válidas las define {@code ServiceOrder.canChangeTo} (decisión D-04).
     *
     * <p>Se guarda en la base de datos como texto con el {@code name()} de la
     * constante. El texto que ve el usuario en pantalla (en español) no vive en el
     * modelo.</p>
     *
     * <p>Principles: High Cohesion.</p>
     */
    public enum OrderStatus {

        /** Vehículo recibido en el taller. */
        RECEIVED,
        /** En diagnóstico. */
        DIAGNOSIS,
        /** En reparación. */
        IN_REPAIR,
        /** Listo para entregar. */
        READY,
        /** Entregado al cliente. */
        DELIVERED,
        /** Orden cancelada. */
        CANCELLED
    }

}
