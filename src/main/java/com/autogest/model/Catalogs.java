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
}
