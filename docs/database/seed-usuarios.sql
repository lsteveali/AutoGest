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
     '600000:8++FCWYM6t7KJoYVxO6UYA==:Ux26sZfNRDbklxefYxgb7ZbZJfTI0refVVmjGRxHIgY=',
     'ADMINISTRATOR', 'ACTIVE',   NULL,     TIMESTAMP '2026-10-09 08:15:00'),
    (2, 'Carlos', 'Martínez',  'carlos.martinez@autogest.com',   'carlos.martinez',
     '600000:eOd5Fn38dFdHnIQ7b+ykwA==:NTs7rva41YWf3pv3lwI1BwG02l2fca+q4VPqwPUvMR8=',
     'MECHANIC',      'ACTIVE',   'Frenos', TIMESTAMP '2026-10-09 09:40:00'),
    (3, 'Laura',  'Rodríguez', 'laura.rodriguez@autogest.com',   'laura.rodriguez',
     '600000:GmHyTuw52zRYZ0nYpVPpCg==:6wX+JuwnfFxVTIYdZTgHx5Extu9L3tplA5xWA5FjWWo=',
     'RECEPTIONIST',  'ACTIVE',   NULL,     TIMESTAMP '2026-10-09 07:55:00'),
    (4, 'Andrés', 'Gómez',     'andres.gomez@autogest.com',      'andres.gomez',
     '600000:mtZKCb93Vi0O5KUnFRLX7Q==:EGuaBypYb9gyceagagtByxobybnQoOnkrMu7x1r68t0=',
     'MECHANIC',      'INACTIVE', 'Motor',  TIMESTAMP '2026-08-21 16:20:00');

-- Reajuste de la secuencia de identidad ---------------------------------------
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1));
