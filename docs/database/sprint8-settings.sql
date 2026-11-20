-- =============================================================================
-- AutoGest - Sprint 8: tabla de configuración (Datos del taller y Preferencias)
-- Autor: Desarrollador 1. Ejecutar UNA vez en el editor SQL de Supabase,
-- después de schema.sql. Es idempotente (no borra datos si ya existe).
--
-- Se guarda como pares clave/valor para no crear una columna por cada ajuste:
--   workshop.name, workshop.nit, ...   -> Datos del taller
--   preferences.pageSize, ...          -> Preferencias del sistema
-- =============================================================================

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key    VARCHAR(80) PRIMARY KEY,
    setting_value  TEXT
);

ALTER TABLE app_settings ENABLE ROW LEVEL SECURITY;

-- Datos de demostración del taller (se pueden cambiar desde la aplicación)
INSERT INTO app_settings (setting_key, setting_value) VALUES
    ('workshop.name', 'AutoGest Taller Automotriz'),
    ('workshop.nit', '901.456.789-1'),
    ('workshop.email', 'contacto@autogest.com'),
    ('workshop.phone', '605 555 1234'),
    ('workshop.city', 'Valledupar'),
    ('workshop.department', 'Cesar'),
    ('workshop.country', 'Colombia')
ON CONFLICT (setting_key) DO NOTHING;
