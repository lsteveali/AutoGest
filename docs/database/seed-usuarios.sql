-- =============================================================================
-- AutoGest - Usuarios de demostracion (seed-usuarios) - Sprint 2
-- Autor: Lider. Ejecutar DESPUES de schema.sql (y de seed.sql, si se usa).
--
-- Usuarios del mockup "Usuarios y roles". La clave demo NO se escribe aqui: los
-- hashes se generaron con PasswordHasher (PBKDF2-HMAC-SHA256, D-07) y la clave
-- en texto plano se comparte por el canal privado del equipo.
--
-- Se fijan los id con OVERRIDING SYSTEM VALUE para poder relacionarlos y luego
-- se reajusta la secuencia de identidad.
-- =============================================================================

INSERT INTO users (id, first_name, last_name, email, username, password_hash,
                   role, status, specialty, last_access)
OVERRIDING SYSTEM VALUE VALUES
    (1, 'José',   'García',    'jose.garcia@autogest.com',      'jose.garcia',
     '600000:2W7uJDPjGhR4h7aV3YgqQA==:Tpru4ml+1rTpRx5n6GGjYvV7xJHG2d6T7OWOcWmqq5Y=',
     'ADMINISTRATOR', 'ACTIVE',   NULL,     TIMESTAMP '2026-10-09 08:15:00'),
    (2, 'Carlos', 'Martínez',  'carlos.martinez@autogest.com',   'carlos.martinez',
     '600000:2W7uJDPjGhR4h7aV3YgqQA==:Tpru4ml+1rTpRx5n6GGjYvV7xJHG2d6T7OWOcWmqq5Y=',
     'MECHANIC',      'ACTIVE',   'Frenos', TIMESTAMP '2026-10-09 09:40:00'),
    (3, 'Laura',  'Rodríguez', 'laura.rodriguez@autogest.com',   'laura.rodriguez',
     '600000:2W7uJDPjGhR4h7aV3YgqQA==:Tpru4ml+1rTpRx5n6GGjYvV7xJHG2d6T7OWOcWmqq5Y=',
     'RECEPTIONIST',  'ACTIVE',   NULL,     TIMESTAMP '2026-10-09 07:55:00'),
    (4, 'Andrés', 'Gómez',     'andres.gomez@autogest.com',      'andres.gomez',
     '600000:2W7uJDPjGhR4h7aV3YgqQA==:Tpru4ml+1rTpRx5n6GGjYvV7xJHG2d6T7OWOcWmqq5Y=',
     'MECHANIC',      'INACTIVE', 'Motor',  TIMESTAMP '2026-08-21 16:20:00'),
    (5, 'Leo',     'Admin',     'leo@autogest.com',               'leo',
     '600000:+2Dc295nv27e2ePorn1csA==:n0jjpAbbtS+ticlAeAsLoZZmggAoYKGo6PRy3/Zq1kM=',
     'ADMINISTRATOR', 'ACTIVE',   NULL,     TIMESTAMP '2026-10-09 10:00:00');

-- Reajuste de la secuencia de identidad ---------------------------------------
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1));
