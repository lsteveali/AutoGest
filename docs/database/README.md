# Base de datos — AutoGest

AutoGest usa **PostgreSQL alojado en Supabase**. La aplicación se conecta por JDBC
usando las credenciales de un archivo local que **nunca** se sube al repositorio.

## 1. Configurar la conexión

1. Copia `application.example.properties` (en la raíz del proyecto) a
   `application.local.properties`.
2. Completa las credenciales del proyecto de Supabase:

   ```properties
   db.url=jdbc:postgresql://db.<proyecto>.supabase.co:5432/postgres
   db.user=<usuario>
   db.password=<contraseña>
   ```

3. Verifica que `application.local.properties` esté ignorado por Git (ya lo está:
   `.gitignore` incluye `*.local.properties`). **Nunca** lo subas ni pegues sus
   credenciales en un commit.

`config/DatabaseConnection` lee ese archivo con `db.url`, `db.user` y `db.password`,
y lanza `DataAccessException` con un mensaje claro si falta el archivo o alguna clave.

## 2. Crear el esquema y los datos de demostración

En el **editor SQL de Supabase** (o con `psql`) ejecuta, en este orden:

1. `docs/database/schema.sql` — crea las 14 tablas, restricciones, índices y activa
   Row Level Security. Es idempotente (elimina y vuelve a crear).
2. `docs/database/seed.sql` — inserta los datos de ejemplo del taller.

La cadena de ejemplo del proyecto es
`jdbc:postgresql://db.<proyecto>.supabase.co:5432/postgres`.

## 3. Prueba de conexión

`DatabaseConnectionIntegrationTest` (etiquetada como `integration`) hace un
`SELECT 1` real y **no** corre en `mvn clean test` para que el build no dependa de
credenciales.

Para ejecutarla con tus credenciales configuradas:

```bash
mvn test -DexcludedGroups= -Dgroups=integration
```

## 4. Convenciones del esquema

| Tema | Regla |
|---|---|
| Identificadores | `INT GENERATED ALWAYS AS IDENTITY` |
| Enumeraciones | `VARCHAR` + `CHECK` (se guarda el `name()` del enum) |
| Dinero | `NUMERIC(12,2)` |
| Fechas | `DATE` / `TIMESTAMP` |
| Claves foráneas | `ON DELETE RESTRICT` (se conserva el historial) |
| Seguridad | RLS activado en todas las tablas, sin políticas públicas |
