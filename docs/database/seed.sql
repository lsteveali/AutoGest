-- =============================================================================
-- AutoGest - Datos de demostracion (seed)
-- Autor: Desarrollador 2. Ejecutar DESPUES de schema.sql en el editor de Supabase.
--
-- Son datos de ejemplo del taller (los mockups usan nombres como Juan Perez).
-- Se fijan los id con OVERRIDING SYSTEM VALUE para poder relacionarlos y luego
-- se reajustan las secuencias de identidad.
-- =============================================================================

-- Clientes --------------------------------------------------------------------
INSERT INTO customers (id, document_type, document_number, first_name, last_name,
                       phone, email, city, address, notes, accepts_promotions, status, registration_date)
OVERRIDING SYSTEM VALUE VALUES
    (1, 'CC', '1234567890', 'Juan',   'Perez',    '3001234567', 'juan.perez@example.com',   'Valledupar', 'Calle 15 # 10-20',  'Cliente frecuente', TRUE,  'ACTIVE',   DATE '2024-02-10'),
    (2, 'CC', '1098765432', 'Maria',  'Rodriguez','3017654321', 'maria.rodriguez@example.com','Valledupar','Carrera 9 # 8-45',  NULL,                FALSE, 'ACTIVE',   DATE '2024-06-22'),
    (3, 'NIT','900123456',  'Carlos', 'Gomez',    '3109876543', 'taller.gomez@example.com', 'Valledupar', 'Av. Fundacion # 5-10', NULL,             FALSE, 'INACTIVE', DATE '2025-01-15');

-- Vehiculos -------------------------------------------------------------------
INSERT INTO vehicles (id, customer_id, make, model, year, license_plate, vin, color,
                      fuel_type, mileage, status, vehicle_type, transmission,
                      engine_number, registration_card_number, observations,
                      last_maintenance_mileage, last_maintenance_date,
                      next_maintenance_mileage, next_maintenance_date)
OVERRIDING SYSTEM VALUE VALUES
    (1, 1, 'Toyota', 'Corolla',  2022, 'ABC-123', 'JTDBR32E123456789', 'Blanco', 'GASOLINE', 48500, 'ACTIVE',   'CAR', 'AUTOMATIC', '1ZZ-FE-998877', 'TC-2022-001', NULL, 40000, DATE '2025-06-12', 50000, DATE '2026-06-12'),
    (2, 1, 'Mazda',  'CX-5',     2021, 'XYZ-789', 'JM3KFBCM0M0123456', 'Gris',   'GASOLINE', 61200, 'IN_SERVICE','CAR', 'AUTOMATIC', 'SKY-778899',    'MC-2021-114', NULL, 55000, DATE '2025-09-01', 65000, DATE '2026-09-01'),
    (3, 2, 'Kia',    'Sportage', 2020, 'DEF-456', 'KNDPMCAC0L7654321', 'Negro',  'DIESEL',   87300, 'ACTIVE',   'VAN', 'MANUAL',    'D4HA-112233',   'KS-2020-230', NULL, 80000, DATE '2025-12-05', 90000, DATE '2026-12-05'),
    (4, 3, 'Honda',  'Civic',    2019, 'GHI-101', '19XFC2F5XKE123456', 'Azul',   'GASOLINE', 104800,'ACTIVE',   'CAR', 'AUTOMATIC', 'R18-445566',    'HC-2019-078', NULL, NULL, NULL, NULL, NULL);

-- Proveedores -----------------------------------------------------------------
INSERT INTO suppliers (id, name, phone, email)
OVERRIDING SYSTEM VALUE VALUES
    (1, 'AutoPartes Colombia', '3205551234', 'ventas@autopartescolombia.example.com'),
    (2, 'Frenos del Norte',    '3154449876', 'contacto@frenosdelnorte.example.com'),
    (3, 'Lubricantes Valledupar', '3007778899', 'pedidos@lubricantesvalledupar.example.com');

-- Catalogo de servicios -------------------------------------------------------
INSERT INTO service_offerings (id, code, name, category, description, duration_hours, price, status)
OVERRIDING SYSTEM VALUE VALUES
    (1, 'SRV-001', 'Cambio de aceite',            'Mantenimiento', 'Cambio de aceite y filtro.',              1.00, 120000.00, 'ACTIVE'),
    (2, 'SRV-002', 'Cambio de pastillas de freno','Frenos',        'Reemplazo de pastillas delanteras.',      2.00, 180000.00, 'ACTIVE'),
    (3, 'SRV-003', 'Alineacion y balanceo',       'Suspension',    'Alineacion y balanceo de las cuatro ruedas.', 1.50, 90000.00, 'ACTIVE'),
    (4, 'SRV-004', 'Revision general',            'Mantenimiento', 'Inspeccion general del vehiculo.',        1.00, 70000.00, 'ACTIVE');

-- Repuestos -------------------------------------------------------------------
INSERT INTO parts (id, code, name, category, current_stock, minimum_stock, unit_price, status, supplier_id)
OVERRIDING SYSTEM VALUE VALUES
    (1, 'REP-001', 'Filtro de aceite',        'Motor',      25, 10,  35000.00, 'AVAILABLE',   1),
    (2, 'REP-002', 'Pastillas de freno',      'Frenos',      8, 10, 120000.00, 'LOW_STOCK',   2),
    (3, 'REP-003', 'Bujia',                   'Motor',      40, 15,  25000.00, 'AVAILABLE',   1),
    (4, 'REP-004', 'Aceite de motor 5W-30',   'Lubricantes', 0,  5,  85000.00, 'OUT_OF_STOCK',3);

-- Repuestos asociados a cada servicio (N a M) ---------------------------------
INSERT INTO service_offering_parts (service_offering_id, part_id) VALUES
    (1, 1),   -- Cambio de aceite usa filtro de aceite
    (1, 4),   -- Cambio de aceite usa aceite de motor
    (2, 2);   -- Cambio de pastillas usa pastillas de freno

-- Reajuste de las secuencias de identidad -------------------------------------
SELECT setval(pg_get_serial_sequence('customers', 'id'),         COALESCE((SELECT MAX(id) FROM customers), 1));
SELECT setval(pg_get_serial_sequence('vehicles', 'id'),          COALESCE((SELECT MAX(id) FROM vehicles), 1));
SELECT setval(pg_get_serial_sequence('suppliers', 'id'),         COALESCE((SELECT MAX(id) FROM suppliers), 1));
SELECT setval(pg_get_serial_sequence('service_offerings', 'id'), COALESCE((SELECT MAX(id) FROM service_offerings), 1));
SELECT setval(pg_get_serial_sequence('parts', 'id'),             COALESCE((SELECT MAX(id) FROM parts), 1));
