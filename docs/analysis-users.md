# Análisis - Módulo Usuarios y Roles (Líder)

**Autor:** Leonarth Steve Ali Camargo (Líder) · AutoGest  
Respuestas a las preguntas de análisis de la sección 12.4 de la guía. Cada respuesta cierra con la línea de trazabilidad *Principio -> clase o método* que alimenta la sección 2.6 del documento del proyecto.

## 1. ¿Por qué `User` no hereda por rol (no existen subclases `Administrator`, `Receptionist`, `Mechanic`, `Supervisor`) y se usa una enumeración `Role`?

Porque el comportamiento que cambia entre roles es **qué acciones/pantallas se permiten**, no la estructura ni los datos fundamentales de la cuenta. Un usuario siempre tiene `firstName`, `lastName`, `email`, `username`, `passwordHash`, `role`, `status`, `specialty` y `lastAccess`. La distinción es el perfil de permisos.

**Razones para no usar herencia:**

- **Un solo tipo con atributo.** `User` es la misma entidad para todos los perfiles. Usar herencia obligaría a crear jerarquías frágiles y, con el tiempo, a multiplicar clases solo para habilitar/deshabilitar funcionalidades.
- **Relaciones más limpias.** `ServiceOrder` guarda un `User` como responsable (`responsible`). Si existieran subclases, el modelo perdería uniformidad y sería más difícil trabajar con el responsable sin preguntar el tipo.
- **Más flexible.** Si en el futuro un usuario necesita cambiar de rol, basta con asignar `user.setRole(...)`. Con herencia habría que crear un objeto nuevo o moverlo entre jerarquías.
- **Coherente con el diseño.** `Role` es una enumeración con los cuatro perfiles definidos. Los textos en español para mostrar en pantalla viven fuera del modelo (en los controladores/etiquetas).

*Trazabilidad: GRASP High Cohesion -> `User` (agrupa datos y comportamiento de cuenta); GRASP Low Coupling -> sin jerarquía innecesaria; SOLID OCP -> nuevos permisos se añaden sin crear subclases de `User`.*

## 2. ¿Por qué la contraseña se guarda como hash y por qué `User.validatePassword(String)` delega en `PasswordHasher`?

La autenticación requiere comparar lo que el usuario escribe con lo almacenado, pero **nunca debe guardarse la contraseña en texto plano**. 

- **Almacenamiento seguro.** `User` guarda únicamente `passwordHash`. `validatePassword` delega en `com.autogest.util.PasswordHasher.verify(enteredPassword, passwordHash)`, que usa **PBKDF2-HMAC-SHA256** con 600.000 iteraciones, sal de 16 bytes y comparación en tiempo constante.
- **Responsabilidad correcta.** `User` es *Information Expert* sobre sus propios datos: sabe que tiene un hash y cómo debe validarlo. El algoritmo, iteraciones, formato (`iteraciones:sal:hash`) y la comparación segura viven en `PasswordHasher` (utilidad especializada).
- **Sin lógica de hash en el modelo.** Esto mantiene `User` con alta cohesión (datos de cuenta + validación delegada) y evita acoplarlo a detalles criptográficos.

*Trazabilidad: GRASP Information Expert -> `User.validatePassword()` delega en `PasswordHasher.verify()`; GRASP Pure Fabrication -> `PasswordHasher` (utilidad criptográfica); SOLID SRP -> algoritmo separado del modelo.*

## 3. ¿Dónde debe vivir la autenticación (`login`) y por qué no en `UserService` ni en el `Controller`?

`AuthenticationService` es el responsable de **autenticar**. `UserService` se encarga de **gestión de usuarios** (alta, edición, búsqueda, conteo, etc.).

- **Separación de casos de uso.** `AuthenticationService.authenticate(String emailOrUsername, String plainPassword)` busca el usuario por identificador, comprueba estado (`ACTIVE`), valida la contraseña con `User.validatePassword(...)`, actualiza `lastAccess` si aplica y devuelve el `User` autenticado o lanza la excepción correspondiente.
- **`UserService` distinto.** `UserService` trabaja con repositorio/DAO para CRUD y consultas, pero no es su responsabilidad orquestar el flujo de inicio de sesión ni gestionar la sesión activa.
- **`Controller` solo coordina UI.** `LoginController` lee los campos, llama a `AuthenticationService`, muestra errores y delega en `Session`/navegación. No contiene reglas de negocio ni consulta directa a DAO.
- **`Session` centraliza el usuario autenticado.** `com.autogest.controller.Session` guarda el `User` en sesión (estático) para que `Sidebar`, `Topbar` y `Permissions` lo consulten sin acoplar pantallas entre sí.

*Trazabilidad: GRASP Controller -> `AuthenticationService` orquesta autenticación; GRASP Indirection / Low Coupling -> `LoginController` depende de `AuthenticationService`, no de `UserDao`; SOLID SRP -> autenticación separado de gestión de usuarios.*

## 4. ¿Cómo se aplica el control de acceso por roles y por qué `Permissions` no vive en el modelo?

El acceso se resuelve con **matriz por rol y sección/pantalla**.

- **`Permissions` en capa de presentación/control.** `com.autogest.controller.Permissions` define qué secciones puede ver cada `Role` (HOME/ASSISTANT siempre; resto según matriz). `Sidebar` filtra sus ítems con esa matriz usando el usuario de `Session`.
- **Razón de capa.** Los permisos aquí son **permisos de navegación/UI** (qué menú mostrar). No son reglas de negocio críticas del dominio que deban imponerse desde `model`. Aun así, las operaciones sensibles siguen siendo validadas en las capas correspondientes cuando aplica.
- **Sin herencia.** El rol viene del `User.role` y se consulta directamente. No se añaden métodos de autorización al modelo para evitar acoplarlo a la estructura de pantallas.

*Trazabilidad: GRASP Protected Variations -> `Permissions` centraliza la matriz de acceso; GRASP Low Coupling -> UI filtra navegación sin mezclar reglas con `User`/`Role`.*

## 5. ¿Por qué el nombre de usuario se deriva del correo y qué reglas se aplican en `UserService`?

En el flujo de creación/gestión se toma el **prefijo del email** para generar/derivar `username` (coherente con los datos de ejemplo del proyecto). 

Las reglas de negocio relevantes para usuarios viven en `UserService`:

- **Baja lógica.** Desactivar usuario es una decisión de negocio/administración; se gestiona vía `UserService`/DAO sin borrar el registro (conservando trazabilidad).
- **Conteo de administradores activos.** `countAdministrators()` cuenta **administradores activos** (regla para proteger el sistema frente a dejarlo sin administradores).
- **Validaciones.** Unicidad de email/username, formato, rol válido y coherencia de datos se validan en la capa de servicio. El `Controller` no impone estas reglas.
- **Hash de contraseña.** La asignación/actualización de contraseña trabaja con hash mediante `PasswordHasher` (la creación desde el seed usa hashes generados con la misma utilidad).

*Trazabilidad: GRASP Information Expert / SOLID SRP -> reglas en `UserService`; GRASP Indirection -> `UserService` → `UserRepository` → `UserDao`; SOLID DIP -> dependencias por abstracción para facilitar pruebas.*

## 6. ¿Por qué `CrudOperations` se implementa en los DAO y cómo fluye la dependencia?

El flujo es **Controller → Service → Repository → DAO**.

- **`UserDao`** implementa `com.autogest.dao.CrudOperations<User>` para las operaciones genéricas (`create`, `findById`, `findAll`, `update`, `delete`). Además añade consultas específicas: `findByEmail`, `findByUsername`, búsquedas/filtrado, conteos.
- **`UserRepository`** delega en `UserDao` y actúa como fachada/abstracción para el `Service`. El `Service` **nunca** llama directamente al `DAO`.
- **`UserService`** contiene reglas de negocio, validaciones y orquestación. Depende de `UserRepository` (DIP).
- **`AppContext`** crea y cablea `UserDao`, `UserRepository`, `AuthenticationService`, `UserService` como singletons.

*Trazabilidad: GRASP Controller/Indirection -> cadena de delegación; GRASP Low Coupling -> Service aislado del acceso a datos; SOLID DIP -> `UserService` depende de `UserRepository` (no de `UserDao`).*
