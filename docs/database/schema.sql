-- =============================================================================
-- AutoGest - Esquema de base de datos (PostgreSQL / Supabase)
-- Autor: Desarrollador 2. Ejecutar en el editor SQL de Supabase.
--
-- Convenciones (guia, seccion 5):
--   * Identificadores: INT GENERATED ALWAYS AS IDENTITY.
--   * Enumeraciones: VARCHAR con restriccion CHECK (se guarda el name()).
--   * Dinero: NUMERIC(12,2); fechas: DATE; fecha y hora: TIMESTAMP.
--   * Claves foraneas: ON DELETE RESTRICT (se conserva el historial).
--   * Row Level Security activado en todas las tablas, sin politicas.
--
-- El script es idempotente: elimina las tablas y las vuelve a crear.
-- =============================================================================

-- Eliminacion en orden inverso a las dependencias ------------------------------
DROP TABLE IF EXISTS notifications CASCADE;
DROP TABLE IF EXISTS invoices CASCADE;
DROP TABLE IF EXISTS inventory_movements CASCADE;
DROP TABLE IF EXISTS order_lines CASCADE;
DROP TABLE IF EXISTS order_history CASCADE;
DROP TABLE IF EXISTS diagnoses CASCADE;
DROP TABLE IF EXISTS service_orders CASCADE;
DROP TABLE IF EXISTS service_offering_parts CASCADE;
DROP TABLE IF EXISTS parts CASCADE;
DROP TABLE IF EXISTS service_offerings CASCADE;
DROP TABLE IF EXISTS suppliers CASCADE;
DROP TABLE IF EXISTS vehicles CASCADE;
DROP TABLE IF EXISTS customers CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- =============================================================================
-- 1. users - Usuarios del sistema (administrador, recepcion, mecanico, supervisor)
-- =============================================================================
CREATE TABLE users (
    id             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    email          VARCHAR(150) NOT NULL UNIQUE,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL
        CHECK (role IN ('ADMINISTRATOR', 'RECEPTIONIST', 'MECHANIC', 'SUPERVISOR')),
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    specialty      VARCHAR(100),
    last_access    TIMESTAMP
);

-- =============================================================================
-- 2. customers - Clientes (propietarios de vehiculos)
-- =============================================================================
CREATE TABLE customers (
    id                  INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_type       VARCHAR(20)  NOT NULL
        CHECK (document_type IN ('CC', 'CE', 'NIT', 'PASSPORT')),
    document_number     VARCHAR(30)  NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100) NOT NULL,
    phone               VARCHAR(30),
    email               VARCHAR(150),
    city                VARCHAR(100),
    address             VARCHAR(200),
    notes               TEXT,
    accepts_promotions  BOOLEAN      NOT NULL DEFAULT FALSE,
    status              VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE')),
    registration_date   DATE         NOT NULL DEFAULT CURRENT_DATE
);

-- =============================================================================
-- 3. vehicles - Vehiculos de cada cliente (decision D-13)
-- =============================================================================
CREATE TABLE vehicles (
    id                        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id               INT NOT NULL REFERENCES customers (id) ON DELETE RESTRICT,
    make                      VARCHAR(60) NOT NULL,
    model                     VARCHAR(60) NOT NULL,
    year                      INT NOT NULL CHECK (year BETWEEN 1900 AND 2100),
    license_plate             VARCHAR(15) NOT NULL UNIQUE,
    vin                       VARCHAR(40),
    color                     VARCHAR(40),
    fuel_type                 VARCHAR(20) NOT NULL
        CHECK (fuel_type IN ('GASOLINE', 'DIESEL', 'HYBRID', 'ELECTRIC', 'GAS')),
    mileage                   INT NOT NULL DEFAULT 0 CHECK (mileage >= 0),
    status                    VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'IN_SERVICE', 'INACTIVE')),
    vehicle_type              VARCHAR(20)
        CHECK (vehicle_type IN ('CAR', 'VAN', 'MOTORCYCLE', 'OTHER')),
    transmission              VARCHAR(20)
        CHECK (transmission IN ('MANUAL', 'AUTOMATIC')),
    engine_number             VARCHAR(60),
    registration_card_number  VARCHAR(60),
    observations              TEXT,
    last_maintenance_mileage  INT CHECK (last_maintenance_mileage >= 0),
    last_maintenance_date     DATE,
    next_maintenance_mileage  INT CHECK (next_maintenance_mileage >= 0),
    next_maintenance_date     DATE
);

-- =============================================================================
-- 4. suppliers - Proveedores de repuestos
-- =============================================================================
CREATE TABLE suppliers (
    id     INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name   VARCHAR(120) NOT NULL,
    phone  VARCHAR(30),
    email  VARCHAR(150)
);

-- =============================================================================
-- 5. service_offerings - Catalogo de servicios
-- =============================================================================
CREATE TABLE service_offerings (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code            VARCHAR(30)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    category        VARCHAR(80),
    description     TEXT,
    duration_hours  NUMERIC(5,2) NOT NULL DEFAULT 0 CHECK (duration_hours >= 0),
    price           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (price >= 0),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

-- =============================================================================
-- 6. parts - Repuestos del inventario
-- =============================================================================
CREATE TABLE parts (
    id             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code           VARCHAR(30)  NOT NULL UNIQUE,
    name           VARCHAR(120) NOT NULL,
    category       VARCHAR(80),
    current_stock  INT NOT NULL DEFAULT 0 CHECK (current_stock >= 0),
    minimum_stock  INT NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    unit_price     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    status         VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'LOW_STOCK', 'OUT_OF_STOCK')),
    supplier_id    INT REFERENCES suppliers (id) ON DELETE RESTRICT
);

-- =============================================================================
-- 7. service_offering_parts - Repuestos asociados a un servicio (N a M)
-- =============================================================================
CREATE TABLE service_offering_parts (
    service_offering_id  INT NOT NULL REFERENCES service_offerings (id) ON DELETE RESTRICT,
    part_id              INT NOT NULL REFERENCES parts (id) ON DELETE RESTRICT,
    PRIMARY KEY (service_offering_id, part_id)
);

-- =============================================================================
-- 8. service_orders - Ordenes de servicio (nucleo operativo)
-- =============================================================================
CREATE TABLE service_orders (
    id                        INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    number                    VARCHAR(30) NOT NULL UNIQUE,
    vehicle_id                INT NOT NULL REFERENCES vehicles (id) ON DELETE RESTRICT,
    responsible_id            INT REFERENCES users (id) ON DELETE RESTRICT,
    entry_date                DATE NOT NULL DEFAULT CURRENT_DATE,
    estimated_delivery_date   DATE,
    entry_reason              VARCHAR(255) NOT NULL,
    observations              TEXT,
    status                    VARCHAR(20) NOT NULL DEFAULT 'RECEIVED'
        CHECK (status IN ('RECEIVED', 'DIAGNOSIS', 'IN_REPAIR', 'READY', 'DELIVERED', 'CANCELLED'))
);

-- =============================================================================
-- 9. diagnoses - Diagnostico de una orden (1 a 0..1)
-- =============================================================================
CREATE TABLE diagnoses (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        INT NOT NULL UNIQUE REFERENCES service_orders (id) ON DELETE RESTRICT,
    description     TEXT,
    recommendations TEXT,
    date            DATE NOT NULL DEFAULT CURRENT_DATE
);

-- =============================================================================
-- 10. order_history - Historial de cambios de una orden (auditoria)
-- =============================================================================
CREATE TABLE order_history (
    id           INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     INT NOT NULL REFERENCES service_orders (id) ON DELETE RESTRICT,
    timestamp    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description  VARCHAR(255) NOT NULL,
    user_id      INT REFERENCES users (id) ON DELETE RESTRICT
);

-- =============================================================================
-- 11. order_lines - Lineas de la orden (servicio o repuesto)
-- =============================================================================
CREATE TABLE order_lines (
    id                   INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id             INT NOT NULL REFERENCES service_orders (id) ON DELETE RESTRICT,
    line_type            VARCHAR(10) NOT NULL CHECK (line_type IN ('SERVICE', 'PART')),
    service_offering_id  INT REFERENCES service_offerings (id) ON DELETE RESTRICT,
    part_id              INT REFERENCES parts (id) ON DELETE RESTRICT,
    quantity             INT NOT NULL CHECK (quantity > 0),
    unit_price           NUMERIC(12,2) NOT NULL CHECK (unit_price >= 0),
    -- SERVICE exige un servicio; PART exige un repuesto (nunca ambos ni ninguno).
    CONSTRAINT order_lines_kind CHECK (
        (line_type = 'SERVICE' AND service_offering_id IS NOT NULL AND part_id IS NULL)
        OR (line_type = 'PART' AND part_id IS NOT NULL AND service_offering_id IS NULL)
    )
);

-- =============================================================================
-- 12. inventory_movements - Movimientos de inventario (entrada, salida, ajuste)
-- =============================================================================
CREATE TABLE inventory_movements (
    id             INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    part_id        INT NOT NULL REFERENCES parts (id) ON DELETE RESTRICT,
    movement_type  VARCHAR(15) NOT NULL CHECK (movement_type IN ('ENTRY', 'EXIT', 'ADJUSTMENT')),
    quantity       INT NOT NULL CHECK (quantity > 0),
    timestamp      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason         VARCHAR(255),
    user_id        INT REFERENCES users (id) ON DELETE RESTRICT
);

-- =============================================================================
-- 13. invoices - Facturas de una orden (1 a 0..1)
-- =============================================================================
CREATE TABLE invoices (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    number          VARCHAR(30) NOT NULL UNIQUE,
    order_id        INT NOT NULL UNIQUE REFERENCES service_orders (id) ON DELETE RESTRICT,
    issue_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date        DATE,
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    taxes           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (taxes >= 0),
    discount        NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (discount >= 0),
    total           NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'PAID', 'OVERDUE', 'VOIDED')),
    payment_method  VARCHAR(30)
);

-- =============================================================================
-- 14. notifications - Notificaciones a clientes
-- =============================================================================
CREATE TABLE notifications (
    id              INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type            VARCHAR(30) NOT NULL
        CHECK (type IN ('SERVICE_UPDATE', 'MAINTENANCE_REMINDER', 'BILLING', 'PROMOTION', 'GENERAL')),
    channel         VARCHAR(15) NOT NULL CHECK (channel IN ('EMAIL', 'SYSTEM', 'WHATSAPP')),
    subject         VARCHAR(150),
    message         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('SENT', 'PENDING', 'SCHEDULED', 'FAILED')),
    scheduled_date  TIMESTAMP,
    order_id        INT REFERENCES service_orders (id) ON DELETE RESTRICT,
    customer_id     INT NOT NULL REFERENCES customers (id) ON DELETE RESTRICT
);

-- =============================================================================
-- Indices de apoyo para llaves foraneas y busquedas frecuentes
-- =============================================================================
CREATE INDEX idx_vehicles_customer        ON vehicles (customer_id);
CREATE INDEX idx_service_orders_vehicle   ON service_orders (vehicle_id);
CREATE INDEX idx_service_orders_resp      ON service_orders (responsible_id);
CREATE INDEX idx_order_lines_order        ON order_lines (order_id);
CREATE INDEX idx_order_history_order      ON order_history (order_id);
CREATE INDEX idx_inventory_movements_part ON inventory_movements (part_id);
CREATE INDEX idx_notifications_customer   ON notifications (customer_id);

-- =============================================================================
-- Row Level Security: activado en todas las tablas, sin politicas publicas.
-- La aplicacion se conecta por JDBC con el usuario dueno de las tablas.
-- =============================================================================
ALTER TABLE users                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE customers             ENABLE ROW LEVEL SECURITY;
ALTER TABLE vehicles              ENABLE ROW LEVEL SECURITY;
ALTER TABLE suppliers             ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_offerings     ENABLE ROW LEVEL SECURITY;
ALTER TABLE parts                 ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_offering_parts ENABLE ROW LEVEL SECURITY;
ALTER TABLE service_orders        ENABLE ROW LEVEL SECURITY;
ALTER TABLE diagnoses             ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_history         ENABLE ROW LEVEL SECURITY;
ALTER TABLE order_lines           ENABLE ROW LEVEL SECURITY;
ALTER TABLE inventory_movements   ENABLE ROW LEVEL SECURITY;
ALTER TABLE invoices              ENABLE ROW LEVEL SECURITY;
ALTER TABLE notifications         ENABLE ROW LEVEL SECURITY;
