# Bitácora de desarrollo — AutoGest

> **Documento vivo.** Se actualiza a medida que se avanza. Cada vez que se
> termina una tarea se agrega/actualiza su estado y, cuando aporta, una entrada
> en la sección «Bitácora cronológica».
>
> Última actualización: **14/11/2026** — Líder (Leonarth Steve Ali Camargo).

---

## 1. Información general

| Campo | Valor |
|---|---|
| Proyecto | AutoGest — Sistema de gestión integral para talleres automotrices |
| Materia | Programación de Computadores III (SS462) |
| Repositorio | https://github.com/lsteveali/AutoGest.git |
| Rama de trabajo actual | `feature/notificaciones-eventos` (Sprint 6 — parte del Líder) |
| Sprint en curso | **Sprint 6 — Notificaciones**: Dev 1 entregó backend (PR #17) e interfaz (PR #18) · Líder: plantillas, avisos automáticos, Inicio y campana (Sesión 23) |
| Equipo | Líder: Leonarth Steve Ali Camargo · Dev 1: Sebastián Murgas Tejeda · Dev 2: **sin integrante vinculado** — el Líder asume todas sus responsabilidades (decisión 14/10/2026) |
| Documento guía | `AutoGest_Guia_Lider` (V2.0) |

---

## 2. Estado de entregables

### Sprint 1 — Base de presentación y arquitectura (Líder)

| Id | Entregable | Estado | Notas |
|---|---|---|---|
| L1 | `pom.xml` y estructura de paquetes | ✅ Hecho | Java 21, JavaFX 21.0.12, driver PostgreSQL, JUnit 5, Surefire, plugin JavaFX. Paquetes con `package-info`. |
| L2 | Aplicación y shell | ✅ Hecho | `AutoGestApplication` (1280x720), `main.fxml` con Sidebar/Topbar/área central. |
| L3 | Navegación | ✅ Hecho | `View`, `ViewNavigator`, `ParameterizedController` y pantallas marcadoras «En construcción». |
| L4 | Sistema de diseño | ✅ Hecho | `autogest.css` + iconos lineales `IconSet` (D-15) + fuentes Roboto Regular/Medium/Bold con `Font.loadFont` (D-18, Sesión 7). |
| L5 | Componentes reutilizables | ✅ Hecho | `StatusBadge`, `KpiCard`, `PageHeader`, `FilterBar`, `PagedTable`, `ConfirmDialog`, `ErrorDialog`, `Sidebar`, `Topbar`, `AssistantButton` + pantalla demo. |
| L6 | Utilidades y conexión de dependencias | ✅ Hecho | `util/Formatter` (+ pruebas) y esqueleto de `config/AppContext`. |
| L7 | Documentación | ✅ Hecho | `docs/architecture.md`, `docs/analysis-presentation-base.md`, `docs/architecture-class-diagram.md`. |

### Aportes de Dev 1 y Dev 2 (Sprint 1) — integrados en `develop`

> **Nota:** no hay Dev 2 vinculado. Según la guía (§10) el Líder asumió sus
> tareas del Sprint 1 (`feature/base-datos`); desde el 14/10/2026 asume además
> **todas** sus responsabilidades de los sprints siguientes (ver §4).

| Entregable | Dueño | Estado |
|---|---|---|
| Paquete `model` completo (40 clases/enums + 14 pruebas) | Dev 1 | ✅ Integrado en `develop` (PR #2 fusionado, `169d9f8`) |
| `dao/CrudOperations<T>`, `config/DatabaseConnection`, `exception/*` | Dev 2 | ✅ Integrado en `develop` (merge `66daf92`) |
| `docs/database/schema.sql`, `seed.sql` y `README.md` | Dev 2 | ✅ Integrado en `develop` (merge `66daf92`) |

### Sprint 2 — Login, sesión, permisos y Usuarios y roles (Líder)

| Id | Entregable | Estado | Notas |
|---|---|---|---|
| S2-PR1 | Backend (`PasswordHasher`, `UserDao`, `UserRepository`, `AuthenticationService`, `UserService`, `Session`, `Permissions`) | ✅ Hecho | 9 commits (`433dc91..38594b5`) y `PermissionsTest`; `mvn clean test` → 134 pruebas. **PR #3 fusionado en `develop`** (`95d2ded`). |
| S2-PR2 | Interfaz (Login, shell con sesión, Usuarios y roles) | ✅ Hecho | 6 commits; `mvn clean test` → 141 pruebas. **Fusionado en `develop` vía PR #7** (`3015e20`, visto en `git log` el 16/10/2026). |
| S2-C1 | Clientes — backend (`CustomerDao`, `CustomerRepository`, `CustomerService`) | ✅ Hecho (Dev 1) | Firmas iguales al contrato 7.5, `CustomerServiceTest` (37 pruebas) con repositorio falso. **Fusionado en `develop` vía PR #5** (`82ad096`). |
| S2-C2 | Clientes — interfaz (lista, nuevo, editar, detalle) y bloque *Customers* de `AppContext` | ✅ Hecho (Dev 1) | `CustomerListController`, `CustomerFormController`, `CustomerDetailController`, `CustomerLabels`, `customers.css`, los 4 FXML, `CustomerLabelsTest` (15 pruebas) y los docs de análisis/diagrama. **Fusionado en `develop` vía PR #8** (`d30472c`, 17/10/2026). |
| S2-V1 | Vehículos — backend alineado al contrato 7.5 (`VehicleDao`, `VehicleRepository`, `VehicleService`) | ✅ Hecho (Líder) | 5 commits previos + ajustes de la Sesión 8 (marcas, conteo por propietario, `updateMileage`, `deleteVehicle`, búsqueda por marca). |
| S2-V2 | Vehículos — interfaz (lista, nuevo, editar, detalle) | ✅ Hecho (Líder) | 4 controladores + 4 FXML reemplazan los marcadores; `VehicleLabels` y `package-info`. Verificado con `mvn clean test` (17/10/2026). Carga de FXML: prueba de humo aún pendiente de diagnóstico. |
| S2-D | Documentos del Sprint 2 (`analysis-users.md`, `users-class-diagram.md` y equivalentes de Clientes y Vehículos) | ✅ Hecho | Clientes (`docs/analysis-customers.md`, `docs/customers-class-diagram.md`, Dev 1, PR #8). Usuarios y Vehículos (`docs/analysis-users.md`, `docs/users-class-diagram.md`, `docs/analysis-vehicles.md`, `docs/vehicles-class-diagram.md`, commit `4f9b5d5`, PR #9). |

### Sprints 3–9 (hoja de ruta)

| Sprint | Contenido | Estado |
|---|---|---|
| 3 | Servicios, Inventario y Repuestos (Líder: Dashboard v1) | ✅ Líder (PR #10 y #11) · Dev 1: Inventario, repuestos, proveedores y movimientos (PR #13) |
| 4 | Órdenes de servicio (Dev 1: backend, lista y nueva orden · Líder: detalle, cambio de estado, historial y descuento de stock) | ✅ Dev 1 (PR #14) · Líder (PR #15, Sesión 21) |
| 5 | Facturación (Líder: todo el módulo) | ✅ PR #16 fusionado en `develop` (`b858e80`, 15/11/2026): facturación, PDF, avisos al cliente e Inicio con órdenes y facturas (Sesión 22) |
| 6 | Notificaciones (Dev 1: backend e interfaz · Líder: plantillas y eventos) | 🟡 Dev 1 ✅ (PR #17 y #18) · Líder hecho en `feature/notificaciones-eventos` (Sesión 23), falta prueba manual y PR |
| 7 | Reportes | ⬜ No iniciado |
| 8 | Configuración (Dev 1: Datos del taller y Preferencias · Líder: pantalla general y el resto de secciones, incluidas Copias de seguridad) | 🟡 En curso — rama `feature/configuracion` |
| 9 | Asistente AutoGest (Líder) | ⬜ No iniciado |

> **Reasignación (14/10/2026):** las columnas de Dev 2 de la hoja de ruta
> (Vehículos, Inventario y Repuestos, detalle de la orden e integración de stock,
> Facturación, Plantillas y eventos, Copias de seguridad, apoyo en Reportes y
> Asistente) pasan al Líder. Si se vincula un tercer integrante, se reparte de nuevo.

---

## 3. Bitácora cronológica

### 08/10/2026 — Sesión 1 (Líder): cierre del Sprint 1

**Contexto inicial.** Se inspeccionó el repositorio: `main`, `develop` y
`feature/base-presentacion` existían; L1–L4 estaban parcialmente hechos. El
índice de Git quedó con un conflicto remanente en `autogest.css` (producto de un
`git stash apply`) y había un `stash@{0}` con los iconos lineales. Maven no
estaba en el PATH (se usó el de IntelliJ) y Java 25 estaba instalado.

**Hecho en esta sesión:**

1. **Git.** Se resolvió el conflicto de `autogest.css` (el árbol ya coincidía con
   `HEAD`) y se incorporaron `IconSet.java` + `IconSetTest.java` desde el stash.
   No se hizo commit en ese momento, según lo pedido.
2. **L5 — Componentes reutilizables** en `controller/components`:
   - `StatusBadge` (enum `BadgeType`), `KpiCard`, `PageHeader`, `FilterBar`,
     `PagedTable<T>`, `ConfirmDialog`, `ErrorDialog`.
   - `Sidebar`, `Topbar` y `AssistantButton`.
   - `PagedTable`: encabezado, orden sobre el conjunto completo, paginación y
     texto «Mostrando 1-10 de N», columna de acciones soportada.
   - Shell conectado: `main.fxml` y `MainController` usan los componentes reales
     (se eliminaron los marcadores) y el botón flotante del Asistente.
   - Pantalla de desarrollo `fxml/demo-components.fxml` + `DemoComponentsController`.
3. **L6 — Utilidades.**
   - `util/Formatter` (`money`, `date`, `timestamp`) con `FormatterTest` (7 pruebas).
   - `config/AppContext` con el singleton y los bloques marcados por rol.
4. **CSS.** Se agregaron a `autogest.css` los estilos de los componentes nuevos
   (page-header, breadcrumb, kpi-icon, filtros, paginación, topbar/avatar,
   botón del asistente).
5. **L7 + sección 12.5.** `docs/architecture.md`,
   `docs/analysis-presentation-base.md` y `docs/architecture-class-diagram.md`.
6. **Verificación.** `mvn clean test` → **BUILD SUCCESS** (11 pruebas). Prueba de
   humo de carga de FXML (temporal, eliminada): cargan `main.fxml`, las 17
   pantallas y la demo. Chequeos de capas de la sección 13: sin `javafx` en
   model/service/repository/dao; sin `java.sql` fuera de dao/config; los `double`
   son solo geometría, nunca dinero.
7. **Git: commits y push.** Se crearon 9 commits atómicos y se subió la rama:

   | Commit | Mensaje |
   |---|---|
   | `ea57937` | `feat(ui): agrega IconSet con iconos lineales propios` |
   | `04ba6a2` | `feat(ui): agrega componentes reutilizables de presentacion` |
   | `d3fe7e9` | `feat(ui): agrega Sidebar, Topbar y boton del Asistente` |
   | `ab0fbe1` | `feat(shell): conecta Sidebar, Topbar y Asistente en el shell` |
   | `18aa33e` | `style(css): agrega estilos de los componentes de presentacion` |
   | `9b1252d` | `feat(util): agrega Formatter con formato de dinero y fechas` |
   | `04f52f2` | `feat(config): agrega el esqueleto de AppContext` |
   | `0fc0b54` | `chore(dev): agrega pantalla de demostracion de componentes` |
   | `c7233f1` | `docs: agrega arquitectura y analisis de la base de presentacion` |

8. **Pull Request.** `feature/base-presentacion` → `develop`. No se pudo crear
   por API (no hay `gh` ni token en el entorno); se prellenó y abrió la página de
   comparación de GitHub para crearlo manualmente. **Resuelto:** el PR #1 se
   creó y se fusionó (ver Sesión 2).

### 08/10/2026 — Sesión 2 (Líder): PR #1 fusionado e inicio del Sprint 2

**Confirmación y sincronización Git:**
- El PR **#1** (`feature/base-presentacion` → `develop`) quedó **fusionado** en
  `develop` como `2946d88 Merge pull request #1`.
- Se detectó que el commit de la bitácora (`dc1e876`) se subió **después** del
  merge, por lo que **no quedó incluido en `develop`**. Se recuperó el archivo en
  la rama nueva para no perderlo.
- `develop` local sincronizado con `origin/develop` (fast-forward a `2946d88`).
- Se creó la rama **`feature/login-usuarios`** desde `develop`.

**Hecho en esta sesión:**
1. `util/PasswordHasher` (PBKDF2-HMAC-SHA256, 600.000 iteraciones, sal de 16
   bytes, formato `iteraciones:sal:hash`, comparación en tiempo constante) y
   `PasswordHasherTest` (6 pruebas).
2. Se actualizó esta bitácora y se incorporó a la rama `feature/login-usuarios`.

**Bloqueo identificado:** el Sprint 2 (módulo Usuarios) necesita del Sprint 1
ajeno, que aún no existe en el repositorio:
- `model/User`, `model/Role`, `model/UserStatus` (Dev 1).
- `dao/CrudOperations`, `exception/*`, `config/DatabaseConnection` (Dev 2).

Sin ellos no compilan `UserDao`, `UserRepository`, `AuthenticationService`,
`UserService`, `Session` ni `Permissions`. **Decisión (09/10):** esperar a
Dev 1 y Dev 2; no se tocan archivos de otros roles (ver §4 y §5).

### 10/10/2026 — Sesión 3 (Líder): destrabar dependencias del Sprint 2

**Objetivo.** Integrar en `develop` el trabajo pendiente de Dev 1
(`feature/base-modelo`) y Dev 2 (`feature/base-datos`) para desbloquear el Sprint 2.

**Análisis previo.** Las dos ramas tocan archivos totalmente disjuntos (intersección
vacía): `base-datos` = `CrudOperations`, `DatabaseConnection`, `schema.sql`, `seed.sql`,
docs y `pom.xml`; `base-modelo` = solo `model/*` + pruebas. `base-modelo` está 3 commits
atrás de `develop` y **no compila por sí sola** (importa `PasswordHasher` y
`ValidationException`, que viven en `develop`); por eso su verificación correcta es
sobre `develop`.

**Hecho y verificado:**
1. `feature/base-datos` verificada de forma aislada: `mvn clean test` → **17 pruebas**.
2. Integración de ambas ramas en una rama temporal `review/integracion` (sin tocar
   `develop`): **sin conflictos**.
3. `mvn clean test` sobre la integración → **BUILD SUCCESS, 108 pruebas, 0 fallos**.
4. Chequeos de capas (sección 13): sin `javafx` en model/service/repository/dao; sin
   `java.sql` fuera de dao/config; los `double` son solo geometría (`IconSet`,
   `PagedTable`) y `durationHours` (permitido por la guía).
5. `develop` local se actualizó con la integración y se re-verificó (108 pruebas). Luego
   se dejó `develop` local **igual a `origin/develop`** para respetar el flujo de PR.

**Entorno.** Maven no está en el PATH: se usó el de IntelliJ
(`...\IntelliJ IDEA 2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd`) con
`JAVA_HOME=C:\Users\Leo\.jdks\liberica-full-21.0.12.1` (Java 21).

**Git / PR:**
- Se subió `feature/base-datos` a `origin` (no tenía upstream).
- PRs pendientes de crear y fusionar en GitHub:
  - `feature/base-datos → develop`
  - `feature/base-modelo → develop`

**Pendiente:** crear y fusionar los dos PRs; tras el merge, actualizar `develop` local
(`git pull`) y continuar con el backend del Sprint 2.

### 10/10/2026 — Sesión 4 (Líder): integración definitiva y sincronización

**Objetivo.** Cerrar la integración pendiente de `feature/base-datos` y sincronizar
la rama del Sprint 2, que aún no tenía `model/`, `exception/` ni la infraestructura.

**Hallazgos.** El PR #2 (`feature/base-modelo`) ya estaba fusionado (`169d9f8`), pero
`feature/base-datos` **no**: `develop` seguía sin `DatabaseConnection`,
`CrudOperations`, `docs/database/*` ni el ajuste del `pom.xml` (excluir los tests
`@Tag("integration")`). `develop` local estaba 8 commits atrás.

**Hecho y verificado:**
1. `develop` local sincronizado con `origin/develop` (fast-forward a `169d9f8`).
2. `mvn clean test` sobre `develop` → **108 pruebas, BUILD SUCCESS** (base verde).
3. Merge `feature/base-datos → develop` (verificado sin conflictos con `merge-tree`)
   y `mvn clean test` → **108 pruebas, BUILD SUCCESS**.
4. `develop` subido: `169d9f8..66daf92`.
5. `feature/login-usuarios` sincronizada con `develop` (`ffdb0c1`); el conflicto de la
   bitácora se resolvió tomando la versión vigente. `mvn clean test` → **108 pruebas**.
   Rama subida: `e9f3bc0..ffdb0c1`.

**Desviación del flujo de PR (justificada):** `feature/base-datos` es alcance del
Dev 2, **que no está vinculado**; no hay revisor externo ni `gh`/token en el entorno.
Siguiendo la guía (§10: el Líder asume las tareas del Dev 2), el Líder fusionó la
rama directamente a `develop` (precedente: `f8dcfce`).

**Entorno.** Maven de IntelliJ (`...\maven-plugin\lib\maven3\bin\mvn.cmd`) con
`JAVA_HOME=C:\Users\Leo\.jdks\liberica-full-21.0.12.1`.

**Pendiente:** comenzar el backend del Sprint 2 (rama `feature/login-usuarios`).

### 11/10/2026 — Sesión 5 (Líder): backend del Sprint 2 y PR1 fusionado

**Hecho y verificado:**
1. Backend del Sprint 2 sobre `feature/login-usuarios` en 9 commits atómicos
   (`433dc91..38594b5`): `Session`, `UserDao`, `UserRepository`, `AuthenticationService`,
   `UserService`, `Permissions`, cableado del bloque *Users* en `AppContext`,
   pruebas de los Services con repositorio falso y `docs/database/seed-usuarios.sql`.
2. `mvn clean test` → **134 pruebas, 0 fallos, BUILD SUCCESS**. Chequeos de capas
   (sección 13) sin hallazgos.
3. PR **#3** (`feature/login-usuarios → develop`) fusionado en GitHub → `develop`
   en `95d2ded`.

**Decisiones ante ambigüedad de la guía (ver §4):** `Session` estático, el 2.º
argumento de `login` es la contraseña en texto plano, `countAdministrators()`
cuenta administradores activos y el nombre de usuario se deriva del correo.

### 11/10/2026 — Sesión 6 (Líder): interfaz del Sprint 2

**Hecho y verificado** (6 commits atómicos):
1. **Login** (`fxml/login.fxml` + `LoginController` + `AutoGestApplication`): panel
   de marca y eslogan, correo, contraseña y «Recordarme» (D-01), Enter inicia
   sesión, mensajes bajo el formulario y botón deshabilitado mientras valida.
2. **Shell con sesión** (`MainController`, `Sidebar`, `Topbar`): la Topbar muestra
   el usuario de `Session` con «Cerrar sesión» en el menú del avatar y la Sidebar
   se filtra con la matriz de `Permissions` (D-11).
3. **Usuarios y roles** (`fxml/user/users-roles.fxml` + `UsersRolesController` +
   `UserLabels`): KPI (con «Sesiones activas» en «-», D-14), filtros, tabla,
   diálogo de alta/edición y tarjeta de roles.
4. `PermissionsTest` cubre la matriz de acceso (D-11) sin JavaFX.
5. `mvn clean test` → **141 pruebas, 0 fallos, BUILD SUCCESS**. Chequeos de capas
   (sección 13): sin `javafx` ni `java.sql` reales fuera de sus capas; los `double`
   son geometría de interfaz.

**Pendiente:** subir la rama y abrir el PR2 (`feature/login-usuarios → develop`) en
GitHub (no hay `gh`/token en el entorno).

### 14/10/2026 — Sesión 7 (Líder): revisión de V3.0 y reasignación de Dev 2

**Método.** Se compararon los zips V2.0 y V3.0 archivo por archivo (sin `.git`
disponible, por lo que el estado de ramas y PRs **no** se pudo verificar). Maven
no estaba disponible en el entorno de revisión: `mvn clean test` queda por correr
en la máquina del Líder.

**Hallazgos verificados en el código:**
1. **Roboto (D-18) resuelto.** `Roboto-Regular/Medium/Bold.ttf` + `README.md`
   (origen y licencia Apache 2.0) en `resources/css/fonts/`; `AutoGestApplication`
   los carga con `Font.loadFont` antes de la primera escena y, si falta un
   recurso, el CSS cae a «Segoe UI».
2. **Backend de Clientes (alcance Dev 1).** `CustomerDao` (consultas con
   `PreparedStatement`), `CustomerRepository` y `CustomerService`, más
   `CustomerServiceTest` y `FakeCustomerRepository` (37 pruebas nuevas; 179 `@Test`
   en total). Las firmas públicas coinciden con el contrato 7.5. Chequeos de capas
   (§13): sin `javafx` en model/service/repository/dao, sin `java.sql` fuera de
   dao/config, sin `double`.
3. **Sin cambios en** `AppContext` (el bloque *Customers* sigue vacío), pantallas
   de Clientes y Vehículos (siguen «En construcción») ni documentación.

**Decisión.** Las responsabilidades del Dev 2 pasan al Líder (ver §4). Vehículos
del Sprint 2 y, más adelante, Inventario y Repuestos quedan a su cargo.

**Pendiente:** confirmar el estado del PR2 (`feature/login-usuarios`) y del PR del
backend de Clientes; correr `mvn clean test`; ver §5.

### 16/10/2026 — Sesión 8 (Líder): backend de Vehículos alineado al contrato 7.5 e interfaz

**Estado revisado al entrar.** Rama `feature/vehiculos`, 5 commits por delante de
`origin/develop` (`4983108..1090d88`): `VehicleDao`, `VehicleRepository`,
`VehicleService`, registro del bloque *Vehicles* en `AppContext` y
`VehicleServiceTest` con `FakeVehicleRepository`. Adicionalmente estaba sin
seguimiento `controller/vehicle/VehicleLabels.java`. Las cuatro pantallas de
`fxml/vehicle/` seguían con el marcador «En construcción».

**Comparación con la guía V2.0 (contrato 7.5).** El `VehicleService` existente no
cumplía la firma publicada; faltaban `countByCustomer(int)`, `findMakes()`,
`updateMileage(int, int)`, `deleteVehicle(int)`, `countInService()` y el filtro
de marca del `searchVehicles`. Ajustes hechos:

1. `VehicleDao`: `search(text, make, status, type)` con filtro de marca por
   `ILIKE` (se conserva la sobrecarga de 3 argumentos), `countByCustomer(int)` y
   `findMakes()` (marcas distintas, orden alfabético).
2. `VehicleRepository`: delegación de los métodos nuevos y de las dos búsquedas.
3. `VehicleService`: métodos del contrato (`searchVehicles(text, make, status)`,
   `searchVehicles(text, make, status, type)`, `findByCustomer`, `countByCustomer`,
   `findMakes`, `updateMileage`, `deleteVehicle`, `countInService`) y helper
   `resolveAll` que completa el propietario de cada fila. `countInServiceVehicles()`
   queda deprecado a favor de `countInService()`.
4. **Búsqueda con clase renombrada a `searchVehiclesByType(text, status, type)`**:
   con dos sobrecargas de 3 argumentos (`VehicleStatus`/`VehicleType` como tercer
   parámetro) cualquier llamada con `null` era ambigua y no compilaba.

**Interfaz del módulo (S2-V2):** 4 controladores + `package-info` en
`controller/vehicle/` y los 4 FXML reales:

- `VehiclesController`: cabecera con «+ Nuevo vehículo», 4 KPI (registrados,
  activos, en servicio, mantenimiento próximo), filtros de texto/marca/estado/
  clase y `PagedTable` (vehículo, propietario, año, kilometraje, insignia de
  mantenimiento, insignia de estado y menú de acciones). La fila abre el detalle;
  el desplegable de marcas se refresca con `findMakes()` conservando la selección.
- `VehicleFormController` (base): arma por código el formulario completo (20
  campos en dos columnas, con propietario, mantenimientos y observaciones), lee
  el formulario y guarda con el Service, que es quien valida. Las subclases
  `VehicleNewController` y `VehicleEditController` aportan el modo y el id.
- `VehicleDetailController`: ficha con datos, insignias, acciones (Editar / Dar
  de baja / Volver) y tabla «Otros vehículos del propietario».
- `vehicle/*.fxml`: cada pantalla es un `VBox` con `fx:id="root"` y su
  controlador, igual que `users-roles.fxml`; el contenido lo construye el
  controlador (mismo patrón que ya se usaba).

**Verificación.** `mvn clean test` → **230 pruebas, 0 fallos, BUILD SUCCESS**
(antes de esta sesión: 218; +12 pruebas de `VehicleServiceTest` para los métodos
nuevos del contrato). Compilan 91 clases principales y 26 de prueba.

**Prueba de humo de FXML (`VehicleScreensSmokeTest`).** Carga los 4 FXML con el
toolkit JavaFX y arma el detalle con un vehículo real; necesita base de datos y
toolkit, así que va etiquetada `@Tag("integration")` (fuera de `mvn clean test`,
igual que `DatabaseConnectionIntegrationTest`). **Primer intento fallido:** la
ejecución se bloqueó más de 7 minutos y hubo que terminar un proceso `java`
huérfano; no se determinó la causa (posible `ErrorDialog` modal abierto por un
fallo de conexión a Supabase o cierre del toolkit). **Pendiente diagnosticar y
dejar la prueba verde con `mvn test -Pintegration`.**

**Entorno.** Maven de IntelliJ: `C:\Program Files\JetBrains\IntelliJ IDEA
2026.2.3\plugins\maven-plugin\lib\maven3\bin\mvn.cmd` (la ruta de
`AppData\Local\JetBrains` que usaba la sesión 3 ya no existe) con
`JAVA_HOME=C:\Users\Leo\.jdks\liberica-full-21.0.12.1`.

**Git:** los cambios de la sesión quedaron commiteados en la Sesión 9 (ver abajo).

### 17/10/2026 — Sesión 9 (Líder): integración del módulo de Clientes (PR #8) y subida de Vehículos

**Estado revisado al entrar.** `origin/develop` había avanzado de `3015e20` a
`d30472c` con el **PR #8** de `feature/clientes` (Dev 1): interfaz completa del
módulo (`CustomerListController`, `CustomerFormController`,
`CustomerDetailController`, `CustomerLabels`, `customers.css`, los 4 FXML de
`fxml/customer/`, `CustomerLabelsTest` y `docs/analysis-customers.md` +
`docs/customers-class-diagram.md`). `feature/vehiculos` estaba 10 commits atrás
de `origin/develop` y todavía con los cambios de la Sesión 8 sin commitear.

**Hecho y verificado:**

1. **Commits atómicos de la Sesión 8** (4 commits):
   `e61a6bd` (backend al contrato 7.5), `e23ca5f` (pruebas del contrato),
   `6a698f8` (las 4 pantallas: controladores + FXML) y `c426931` (prueba de humo
   de FXML).
2. **`develop` actualizado** (fast-forward local a `d30472c`) y **merge de
   `origin/develop` en `feature/vehiculos`** (`7ae4c9b`) con **0 conflictos**
   (verificado antes con `merge-tree`: los archivos de Clientes y Vehículos son
   disjuntos; la navegación ya tenía `View.VEHICLE_*` y la Sidebar sin cambios).
3. **`mvn clean test` → 245 pruebas, 0 fallos, BUILD SUCCESS.** El módulo de
   Clientes aporta `CustomerServiceTest` (37) y `CustomerLabelsTest` (15), y
   `VehicleServiceTest` queda en 52. El repositorio tiene 248 `@Test`: los 3
   restantes son de las clases `@Tag("integration")` (excluidas del perfil por
   defecto), así que el conteo cuadra.
4. **Chequeos de capas (§13)** sin hallazgos: el único `javafx` fuera de
   `controller/` es `AutoGestApplication` (punto de arranque); sin `java.sql`
   fuera de `dao`/`config`.
5. **Prueba de humo: bloqueo reproducido.** `mvn test -Pintegration
   -Dtest=VehicleScreensSmokeTest` no terminó dentro de 5 minutos (igual que en
   la Sesión 8) y hubo que terminar el proceso. Queda **pendiente diagnosticar**;
   no bloquea la subida porque está fuera de `mvn clean test`.

**Git:** rama subida a `origin/feature/vehiculos` (11 commits sobre `develop`).
PR3 (`feature/vehiculos → develop`) pendiente de abrir en GitHub (no hay `gh` ni
token en el entorno; se prellena la página de comparación, como en sesiones
anteriores). **Higiene Git hecha:** borradas las ramas fusionadas locales
(`base-datos`, `login-usuarios`, `base-presentacion`, `fuentes-roboto`) y
remotas (`base-datos`, `base-modelo`, `base-presentacion`, `login-usuarios`,
`clientes`), y `git stash drop` de los dos stashes obsoletos (el de iconos ya
estaba incorporado; el otro era un WIP viejo de `login-usuarios`). Quedan solo
`develop`, `main` y `feature/vehiculos`.

> **Nota (26/10/2026):** esta entrada se borró por error en `3028f40` y se
> restauró desde `8dec36b`. El PR3 se abrió y fusionó después como **PR #9**.

### 24/10/2026 — Sesión 10 (Líder): inicio del Sprint 3

**Contexto:** PR #9 (`feature/vehiculos` → `develop`) fusionado. Se crea `feature/servicios-inventario` desde `develop` actualizado y se prepara el plan del Sprint 3 (`plan/PLAN-sprint3-servicios-inventario-repuestos.md`).

**Hecho:**
1. **Base transversal (Líder):** `ServiceOfferingDao`, `ServiceOfferingRepository`, `ServiceOfferingService`, `PartDao`, `PartRepository`, `PartService` creados siguiendo el flujo `DAO → Repository → Service`.
2. **Cableado:** `AppContext` actualizado con bloques *Catalogo de servicios* e *Inventario / Repuestos* y getters `getServiceOfferingService()` / `getPartService()`.
3. **Navegación:** `View` ampliado con `SERVICE_NEW/EDIT/DETAIL` y `PART_NEW/EDIT/DETAIL`.
4. **Verificación:** `mvn clean test` → **245 pruebas, 0 fallos, BUILD SUCCESS**. Chequeos de capas (§13) sin `javafx` fuera de `controller/` ni `java.sql` fuera de `dao/config`.

**En curso / Siguiente:**
- **UI del catálogo de Servicios (Líder):** implementar lista (`ServicesController`) con KPI/filtros/`PagedTable`, alta/edición (`ServiceFormController` base + `ServiceNewController`/`ServiceEditController`), detalle (`ServiceDetailController`), `ServiceLabels`, `package-info`. Usar patrón FXML mínimo (`VBox` con `fx:id="root"`) y construcción por código. Evitar bucles infinitos en combos con `suppressReload` si aplica. Navegación con `NavigatorAware`/`ParameterizedController`.
- **Dashboard v1 (Líder):** KPIs principales tras tener base de servicios/inventario.
- **Repuestos + Movimientos de inventario (Dev 1):** UI de partes y movimientos usando `PartService`/`getPartService()` provistos por el Líder.

**Rama:** `feature/servicios-inventario` · **Distribución:** Líder (Dashboard v1 + Servicios + base Inventario) · Dev 1 (Repuestos + Movimientos).

### 25/10/2026 — Sesión 11 (Líder): UI del catálogo de Servicios

**Hecho:**
1. **Pantalla de lista** (`ServicesController` + `fxml/catalog/services.fxml`): `PageHeader` con «+ Nuevo servicio», 3 KPI (registrados, activos, inactivos), filtros de texto/categoría/estado y `PagedTable` con código+nombre, categoría, duración, precio, insignia de estado y menú de acciones (Ver detalle / Editar / Dar de baja con `ConfirmDialog`). La fila abre el detalle y el desplegable de categorías se refresca con `findCategories()` conservando la selección (`suppressReload`).
2. **Formulario compartido** (`ServiceFormController` base + `ServiceNewController` / `ServiceEditController`): patrón de `VehicleFormController` (getRoot/isEditing/title; la edición recibe el id por `receiveParameter`). Campos: código*, nombre*, categoría (editable), duración, precio*, estado y descripción; guardado vía `registerService`/`updateService` y vuelta con `navigator.goBack()`.
3. **Detalle** (`ServiceDetailController`): cabecera con nombre/categoría, acciones (Volver / Editar / Dar de baja), tarjeta con código, categoría, precio, duración, insignia de estado y descripción, más tabla «Repuestos habituales» con los `parts` del servicio.
4. **`ServiceLabels`**: `statusText`/`statusType`/`statusOptions`/`statusFromText`, `price` (delega en `Formatter.money`) y `duration`. `package-info` del paquete `controller/catalog`.
5. **Verificación:** `mvn clean test` → **245 pruebas, 0 fallos, BUILD SUCCESS**. Chequeos de capas (§13): sin `javafx` fuera de `controller/`. Detector de diseño de la guía de interfaz sin hallazgos.

**Siguiente:** Dashboard v1 (Líder); UI de Repuestos y Movimientos (Dev 1) ya desbloqueada.

### 26/10/2026 — Sesión 12 (Líder): Dashboard v1 y ajuste de la barra lateral

**Hecho:**
1. **Dashboard v1** (`controller/home/HomeController` + `package-info` + `fxml/home/home.fxml`, ahora con `ScrollPane` interno para que el contenido no se recorte en los 720px del shell):
   - Cabecera con saludo por hora del día y usuario de la sesión («Buenos días, José»), subtítulo del mockup y acción «+ Nueva orden» (D-14 aplica a los módulos pendientes).
   - 4 KPI: Clientes (con «+N este mes»), Vehículos registrados (detalle activos/en servicio), Órdenes activas y Facturación del mes con marcador «-» y su sprint (4 y 5).
   - Tarjeta «Estado del parque automotor»: diagrama de etapas con ícono, nombre y conteo (Registrados / Activos / En servicio / Mantenimiento / Inactivos) usando los `count*` de `VehicleService`.
   - Tarjeta lateral «Alertas de inventario»: repuestos en catálogo, stock bajo y agotados (`PartService.countParts/countLowStock/countOutOfStock`).
   - Tarjeta «Próximos mantenimientos»: `PagedTable` con vehículos filtrados por `Vehicle.isMaintenanceDue(today)` (D-13), ordenados por fecha; la fila abre el detalle.
   - Tarjeta «Notificaciones»: estado vacío con «Disponible en el Sprint 6».
2. **Barra lateral en blanco:** `.sidebar-item` y `.sidebar-item .icon` pasan de `#C7D2E3` a `#FFFFFF` (hover y activo ya eran blancos; el activo se distingue por el fondo `primary` y el peso 700).
3. **CSS:** nueva sección «Dashboard» (`.dashboard-scroll`, `.dashboard-row`, `.side-card`, `.stage-*`, `.stat-*`, `.card-note`).
4. **Verificación:** `mvn clean test` → **245 pruebas, 0 fallos, BUILD SUCCESS**. Detector de diseño sin hallazgos aplicables (solo advierte «Roboto», que es la fuente fijada por D-18: manda el diseño comprometido).

**Siguiente:** pruebas del backend del Sprint 3 (`ServiceOfferingServiceTest`, `PartServiceTest`) y semilla del catálogo.

### 26/10/2026 — Sesión 13 (Líder): pruebas del backend del Sprint 3

**Hecho:**
1. **`ServiceOfferingServiceTest` (29 pruebas) + `FakeServiceOfferingRepository`**, que reproduce el comportamiento del `ServiceOfferingDao` (búsqueda por nombre/código/descripción sin distinguir mayúsculas, orden por nombre, baja lógica en `delete`, categorías distintas ordenadas). Cubre registro (normalización de textos, redondeo del precio a escala 2, validaciones, código duplicado), actualización (código propio ajeno, inexistente, validación), detalle, baja lógica (`deactivateService`/`deleteService`), búsqueda por texto/estado/categoría y categorías para el desplegable.
2. **`PartServiceTest` (38 pruebas) + `FakePartRepository`** (búsqueda por nombre/código, borrado físico como el DAO, conteos por estado). Cubre registro, estado derivado (`AVAILABLE`/`LOW_STOCK` cuando stock ≤ mínimo/`OUT_OF_STOCK`), actualización, entradas/salidas/ajuste de stock (persistencia y recálculo de estado, stock insuficiente, cantidades no positivas), conteos (stock bajo, agotados, total) y búsqueda.
3. **Hallazgo:** el stock negativo no llega al `PartService`: el propio modelo lo rechaza en `Part.setCurrentStock` (protección doble; sin cambios en el código, la prueba ajustada para no duplicar cobertura del modelo).
4. **Semilla del catálogo:** ya existía en `docs/database/seed.sql` (4 servicios, 4 repuestos y su relación N a M, del Dev 2); no hizo falta agregar nada.
5. **Verificación:** `mvn clean test` → **312 pruebas, 0 fallos, BUILD SUCCESS** (antes: 245; +67 nuevas).

**Siguiente:** UI de Repuestos y Movimientos de inventario (Dev 1), ya desbloqueada.

### 26/10/2026 — Sesión 14 (Líder): logo, barra lateral, Topbar y login según los mockups

**Contexto.** El Líder agregó la carpeta `V1/` con los mockups (35 pantallas, el logo y la ilustración del carro del login). Al ejecutar la app, los textos de la barra lateral se veían grises aunque el CSS de la Sesión 12 los ponía en blanco.

**Hecho:**
1. **Bitácora corregida** (`940a19a`): se restauró la Sesión 9 y la decisión del 17/10 (borradas por error en `3028f40`), se quitó la Sesión 10 duplicada y se marcaron como hechos S2-D, el PR3 (PR #9) y los pendientes del Sprint 1.
2. **Imágenes** en `resources/images/`, generadas desde `V1/` con fondo transparente (System.Drawing): `logo-gear-32/64/256.png`, `logo-gear-light-64.png` (versión clara para el fondo navy) y `login-car.png`.
3. **Ícono de la ventana** (`7cab876`): `AutoGestApplication` carga la tuerca en 3 tamaños; si falta un recurso, se omite.
4. **Barra lateral** (`600be5d`): **causa de los textos grises:** el color y la fuente estaban en el `HBox` del ítem, y en JavaFX el `HBox` no los hereda a su `Label`, que tomaba el gris de modena. Ahora se aplican a `.sidebar-item .label` (también la negrita del ítem activo). Se agregó la tuerca junto a «AutoGest».
5. **Topbar** (`91ef6b8`): pasa al área de contenido en `main.fxml` (la Sidebar ocupa todo el alto, como en los mockups), sin franja blanca ni borde, con avatar gris claro, nombre en negrita navy y rol debajo.
6. **Login** (`29143f4`), según el mockup Login V2: panel claro con logo, eslogan e ilustración del carro; tarjeta «Bienvenido a AutoGest» con textos centrados; campos con íconos `MAIL` y `LOCK` (nuevos en `IconSet`) y botón más grande. **Contraseña:** los puntos salían gigantes porque Roboto no trae el carácter de la máscara; el campo usa ahora Segoe UI.
7. **Verificación:** `mvn clean test` → **312 pruebas, 0 fallos, BUILD SUCCESS**. Capturas de la barra lateral, la Topbar y el login con una prueba temporal de JavaFX (eliminada, sin base de datos).

**No incluido:** «¿No tienes una cuenta? Crear cuenta» del mockup (las cuentas las crea el administrador en Usuarios y roles), el punto rojo de la campana (sin datos hasta el Sprint 6) y el azul más brillante del botón del mockup (cambiaría todos los botones). La carpeta `V1/` (~80 MB) queda fuera de Git.

### 09/11/2026 — Sesión 15 (Líder): pulido de estados de interacción

**Contexto.** Revisión de las pantallas existentes con la guía «make-interfaces-feel-better» (estados de hover, presionado y foco, áreas de clic, radios anidados, ajuste de texto y movimiento), adaptada a JavaFX. Solo se tocó `autogest.css` y `ViewNavigator`; `customers.css` (Dev 1) queda igual.

**Hecho:**
1. **Botones** (`.button`, `.btn-primary/secondary/danger`): altura mínima de 40px, estado presionado (escala 0.97 y fondo más oscuro) y **anillo de foco con teclado** (`:focus-visible`). Los `.btn-*` reemplazaban el fondo de modena y con él su anillo, así que al navegar con Tab no se veía qué botón tenía el foco.
2. **Campos** (`.input`, `.input-group` del login): borde más oscuro al pasar el mouse y halo suave al enfocar; en el login, el ícono del campo enfocado se pone azul.
3. **Tablas** (`.table-autogest`): hover en las filas con datos (abren el detalle al hacer clic) y radio de la cabecera 11 en vez de 12, para que no asome sobre el borde de 1px.
4. **Controles pequeños**: botones de paginación de 30 a 36px de alto mínimo; botón de la campana con 40×40px; estados presionados en la barra lateral, la campana y el botón del Asistente (sombra algo más suave).
5. **Texto**: `-fx-wrap-text` en `.page-subtitle`, `.kpi-detail` y `.card-note`, para que los textos largos bajen de línea en vez de cortarse con «...».
6. **Entrada de pantallas** (`ViewNavigator.playEnter`): al cambiar de pantalla, el contenido aparece con un fundido de 180 ms y sube 6px.
6b. **Desplazamiento en formularios y detalles:** «Nuevo vehículo» no mostraba «Guardar» porque las pantallas de Vehículos y Servicios (nuevo, editar, detalle) eran un `VBox` sin `ScrollPane` y el shell no desplaza. Ahora van en un `ScrollPane` transparente (`.page-scroll`), con la misma barra delgada que el Dashboard. El botón flotante del Asistente tapaba «Guardar»: `ViewNavigator.reserveAssistantSpace` agrega 88px al final de toda pantalla con `ScrollPane` (también Clientes y el Dashboard), para poder subir el final del contenido por encima del botón.
7. **Verificación:** `mvn clean test` → **312 pruebas, 0 fallos**.

**No aplicable en JavaFX:** transiciones de CSS (`transition`), `text-wrap: balance/pretty` y `tabular-nums`. Roboto ya trae los dígitos con el mismo ancho.

### 11/11/2026 — Sesión 16 (Líder): filtros, barra lateral y pulido general con `/impeccable polish`

**Contexto.** Se instaló el plugin `impeccable` (solo en esta copia, alcance local) y se usó su guía `polish`, adaptada a JavaFX. El Líder reportó que el filtro «Estado» de Clientes mostraba una opción vacía arriba y que la barra lateral se veía mal.

**Hecho:**
1. **Filtro «Estado» con opción vacía.** **Causa:** la opción seleccionada («Todos») usa texto blanco de modena, y `customers.css` deja transparente el fondo de las celdas: quedaba blanco sobre blanco. Ahora `autogest.css` da colores propios a la lista abierta de todos los desplegables: tarjeta con borde y sombra, hover gris claro y la opción elegida en azul sobre `#E8F0FA`. Aplica a todos los filtros (Clientes, Vehículos, Servicios, Usuarios).
2. **Campos `.input` en todas las pantallas:** desplegables, fechas y áreas de texto con 40px de alto, sin la línea gris interna de modena; texto de ejemplo más legible (`#6B7789`) y selección de texto con el azul de la marca. Antes solo Clientes los ajustaba.
3. **Barra lateral** (`Sidebar`): cada ícono va en un espacio fijo de 24px (antes cada trazo medía distinto y los textos no quedaban alineados); ítems agrupados en «Taller» y «Gestión», con títulos que se ocultan si el rol no ve ningún ítem del grupo; el Asistente queda fijo al pie, separado por una línea tenue; ícono del carro redibujado como sedán (`IconSet.CAR`, también se usa en Vehículos y el Dashboard).
4. **Tablas** (`PagedTable` y `.table-autogest`): ancho mínimo de cada columna según su título (ya no sale «Acció...» ni «Última vi...»), fondo blanco, títulos alineados a la izquierda como sus datos y sin líneas verticales entre celdas.
5. **Barras de desplazamiento de toda la app:** delgadas, sin flechas y del color de los bordes.
6. **Verificación:** capturas con una prueba temporal de JavaFX (filtro abierto con `customers.css`, barra lateral, tabla y campos; eliminada) y `mvn clean test` → **312 pruebas, 0 fallos**.

**Sin cambios:** `customers.css` y los controladores de Clientes (Dev 1); «Última visita» y «Filtros» siguen deshabilitados a propósito (D-12).

### 12/11/2026 — Sesión 17 (Líder): inicio del Sprint 8 — Configuración y Perfil y cuenta

**Contexto.** Al revisar el paquete del Desarrollador 1 («Sprints 3 a 8», 04/11) se vio que el backend de órdenes y el de Movimientos son suyos, así que la parte del Líder del Sprint 4 queda en espera y se adelanta Configuración (ver decisiones del 12/11). Rama `feature/configuracion` desde `develop`.

**Hecho:**
1. **Pantalla general de Configuración** (`controller/settings/SettingsController` + `fxml/settings/settings.fxml`), según el mockup: tarjeta del usuario en sesión con «Editar perfil», las 8 secciones en dos columnas e «Información del sistema» (versión, estado de la base de datos comprobado en segundo plano a través del Service, Java, JavaFX y sesión). Cada sección se describe como dato; su tarjeta se habilita solo si su pantalla existe en `View` y su FXML está en el proyecto, y si no muestra «Próximamente». Las tarjetas toman el foco con Tab y se abren con clic, Enter o Espacio. Sin «Guardar cambios» ni «Restaurar» en esta pantalla: solo navega.
2. **Perfil y cuenta** (`ProfileController` + `fxml/settings/profile.fxml`, `View.SETTINGS_PROFILE`): nombres, apellidos y correo editables; usuario y rol de solo lectura; cambio de contraseña con confirmación. Las llamadas van en segundo plano (el hash PBKDF2 congelaba la ventana) y el resultado se muestra junto al botón. Lo puede abrir cualquier rol (`Permissions`) desde «Mi perfil» en el menú del avatar; la Topbar se refresca tras cada navegación para mostrar el nombre editado.
3. **`UserService.changePassword(id, actual, nueva)`**: exige la contraseña actual, aplica las reglas de una clave inicial y no permite repetirla; guarda solo el hash. 5 pruebas nuevas en `UserServiceTest` y 1 en `PermissionsTest`.
4. **`MainController`**: «Configuración» ya no redirige a Usuarios y roles; abre la pantalla general.
5. **Íconos** nuevos en `IconSet`: `USER`, `WAREHOUSE`, `SHIELD`, `CLOUD_DOWNLOAD`, `ARROW_RIGHT`, `INFO`. Estilos en `autogest.css` (sección «Configuración»).
6. **Verificación:** capturas con una prueba temporal de JavaFX (eliminada) y `mvn clean test` → **318 pruebas, 0 fallos**.

**Cuidado al integrar con Dev 1:** `View.SETTINGS_PROFILE` va junto a `HOME`, lejos de las líneas que tocan sus `View.diff`; no se crearon `controller/settings/package-info.java` ni `css/settings.css`, que vienen en su paquete.

### 12/11/2026 — Sesión 18 (Líder): Seguridad y Copias de seguridad

**Hecho:**
1. **Seguridad** (`SecurityController` + `fxml/settings/security.fxml`, `View.SETTINGS_SECURITY`), de solo consulta:
   - «Política de contraseñas»: los valores salen de `UserService.MIN_PASSWORD_LENGTH` y `PasswordHasher.ITERATIONS` (ahora públicos), así la pantalla no se desactualiza si cambian.
   - «Permisos por rol»: matriz de secciones × roles leída de `Permissions.canAccess`; nunca contradice lo que la app aplica.
   - «Último acceso de los usuarios»: tabla con estado y último acceso, los más recientes primero («Nunca» al final).
2. **Copias de seguridad** (RF-32), en capas:
   - `BackupDao`: descubre las tablas en `information_schema` (las nuevas, como `app_settings` de Dev 1, entran solas) y las lee con una conexión de solo lectura en `REPEATABLE READ`, para que la copia sea coherente.
   - `BackupRepository`: guarda un ZIP con un CSV por tabla y `manifest.properties` en `<inicio del usuario>/AutoGest/copias-de-seguridad`. CSV en UTF-8 con comillas cuando hace falta; un nulo es campo vacío y un texto vacío es `""`, para distinguirlos al restaurar. Se escribe primero a `.tmp`, así una copia a medias nunca aparece en la lista.
   - `BackupService` (`createBackup`, `listBackups`) y `BackupInfo` (modelo). Cableado en `AppContext` junto al bloque de Usuarios, con nombres completos de clase para no tocar los imports que modifican los `AppContext.diff` de Dev 1.
   - Pantalla (`BackupController` + `fxml/settings/backup.fxml`, `View.SETTINGS_BACKUP`): crear la copia en segundo plano, última copia, cantidad, carpeta con «Abrir carpeta» y la lista de copias.
3. **Pruebas:** `BackupServiceTest` (5) con un DAO falso y el repositorio real sobre una carpeta temporal (contenido de los CSV, nulos y caracteres especiales, base vacía, orden de la lista y archivos ajenos).
4. **Verificación:** capturas de Seguridad y Copias con datos reales, y una copia real de solo lectura contra Supabase (14 tablas, 28 registros) guardada fuera del proyecto; ambas pruebas temporales se eliminaron. `mvn clean test` → **323 pruebas, 0 fallos**.

**Pendiente:** restaurar una copia desde AutoGest. Por ahora la pantalla explica que los CSV se cargan desde Supabase.

### 12/11/2026 — Sesión 19 (Líder): navegación «Volver», Usuarios y roles y pulido con `/impeccable`

**Contexto.** Prueba manual del Líder: Usuarios y roles no mostraba los usuarios ni dejaba bajar, y no había forma de volver a la pantalla anterior sin usar la barra lateral.

**Hecho:**
1. **Usuarios y roles sin usuarios.** **Causa:** la pantalla era un `VBox` sin `ScrollPane`, con la tabla en `VGrow` y la tarjeta «Roles del sistema» debajo: en ventanas bajas JavaFX encogía la tabla hasta casi cero. Ahora va en un `ScrollPane` (`.page-scroll`) y la tabla toma el alto de sus filas (`PagedTable.fitHeightToRows`), así no queda un hueco con pocos usuarios ni se aplasta con muchos. Lo mismo en las tablas de Seguridad y Copias de seguridad.
2. **«Volver» y migas de pan clicables** en todas las pantallas que usan `PageHeader`: en las subpáginas aparece «← Volver» y cada paso de la ruta que es una pantalla (por ejemplo «Configuración») se vuelve un enlace. Lanzan un `NavigationEvent` que sube hasta `MainController`, que aplica los permisos; `View.byTitle` resuelve el texto a la pantalla. Se quitaron los botones «Volver» repetidos de los detalles de Vehículo y de Servicio, y se ordenaron sus migas («Servicios / Detalle del servicio», «Inicio / Vehículos»).
3. **El historial recuerda el id** (`ViewNavigator`): «Volver» desde un formulario regresa al mismo detalle, no a uno vacío.
4. **Filtros con «Todos» elegido desde el inicio** (`FilterBar`): Rol y Estado de Usuarios, Estado y Clase de Vehículos y Estado de Servicios se veían vacíos.
5. **Pulido:** columnas con ancho mínimo propio (`PagedTable.addColumn(título, valor, ancho)`) para que no se corten nombre, correo ni rol; el menú «…» de cada fila ya no muestra la flecha del `MenuButton`.
6. **Error de datos corregido:** `UserService.updateUser` borraba el último acceso del usuario editado (el `UPDATE` escribe todas las columnas y el objeto editado no lo traía). Se vio al editar el perfil del Líder. Prueba nueva en `UserServiceTest`.
7. **Verificación:** prueba temporal sobre el shell real (Configuración → Seguridad → «Volver» vuelve a Configuración; la miga «Configuración» desde Perfil también) y capturas con datos reales; eliminada. `mvn clean test` → **324 pruebas, 0 fallos**.

### 12/11/2026 — Sesión 20 (Líder): vehículos por cliente, ruta en Clientes y formulario de usuario

**Contexto.** Segunda prueba manual del Líder: la lista de Clientes mostraba «-» en Vehículos y no tenía ruta ni «Volver», y el formulario de nuevo usuario necesitaba arreglos.

**Hecho:**
1. **Vehículos por cliente.** `VehicleDao.countAllByCustomer` (una consulta con `GROUP BY customer_id`) → `VehicleRepository.countAllByCustomer` → `VehicleService.countVehiclesByCustomer`. Se eligió una consulta agrupada y no `countByCustomer` por fila: las celdas se redibujan muchas veces y serían decenas de consultas en el hilo de la interfaz. Prueba nueva en `VehicleServiceTest`.
2. **Cambio en un archivo de Dev 1, pedido por el Líder:** `CustomerListController` muestra la ruta «Inicio / Clientes» (con «Volver») y la columna Vehículos con el conteo real. Son 3 líneas de lógica más el campo y el import; «Última visita» sigue en «-» (D-12). **Sebastián: tenerlo en cuenta al actualizar tu rama.**
3. **Formulario de usuario** (`UserFormDialog`, nueva clase; sale de `UsersRolesController`):
   - Si el Service rechaza los datos, el diálogo no se cierra: el error aparece junto a los botones y lo escrito se conserva (antes se cerraba y había que escribir todo de nuevo).
   - Dos columnas, campos obligatorios marcados, confirmación de la clave inicial y textos de ejemplo.
   - Botones de la app («Cancelar» a la izquierda y «Crear usuario» / «Guardar cambios» a la derecha) y encabezado propio (`.app-dialog`).
   - Las claves usan la clase `password-field` (los puntos salían gigantes con Roboto) y el guardado va en segundo plano (el hash congelaba la ventana).
4. **Verificación:** capturas de Clientes y del formulario (vacío y con error de validación, sin escribir en la base de datos) con una prueba temporal, eliminada. `mvn clean test` → **325 pruebas, 0 fallos**.
5. **Detalle del cliente (otro cambio en un archivo de Dev 1, pedido por el Líder):** `CustomerDetailController` seguía con el indicador «Vehículos» en «-» y la tarjeta «Vehículos del cliente» siempre vacía (esperaban a `VehicleService`). Ahora una sola llamada a `VehicleService.findByCustomer` alimenta el indicador y la lista: cada vehículo muestra su placa (estilo placa amarilla), marca, modelo, clase, año, kilometraje y estado, y abre su detalle con clic, Enter o Espacio. La ruta pasa a la barra común con «Volver» y «Clientes» clicable (`PageHeader.breadcrumbBar`, nuevo método estático reutilizable). Verificado con una captura del detalle de un cliente con 2 vehículos.
6. **Un solo «Editar»** en los detalles de Vehículo y de Servicio: queda el de la cabecera y la fila inferior solo con «Dar de baja».
7. **`.impeccable/` fuera del repositorio** (ver decisiones del 12/11).

### 14/11/2026 — Sesión 21 (Líder): Sprint 4 — detalle, estados, historial y stock de la orden

**Contexto.** Dev 1 entregó su Sprint 3 (Inventario, PR #13) y su Sprint 4 (backend de órdenes, lista y nueva orden, PR #14); se revisaron sobre `develop` (373 pruebas) y quedó destrabada la parte del Líder. Rama `feature/ordenes-detalle` desde `develop`.

**Hecho:**
1. **`OrderWorkflowService`** (nuevo; separado de `ServiceOrderService` de Dev 1 como sugiere la guía para estados y costos, SRP): `changeStatus`, `saveDiagnosis`, `assignResponsible`, `addNote`, `addService`, `addPart` y `nextStatus`. Las transiciones las decide `ServiceOrder.canChangeTo` (D-04); cada cambio deja su entrada en el historial y solo se guardan las entradas nuevas.
2. **Regla de stock:** mientras la orden está En reparación, Lista o Entregada, todos sus repuestos ya están descontados. Al pasar a En reparación se descuentan todos con `InventoryService` (movimiento «Orden de servicio #0145»); antes se comprueba que alcance el stock de cada repuesto y, si una salida falla a mitad, se devuelven las ya hechas. Un repuesto agregado en reparación se descuenta en el momento. Al cancelar una orden que ya los había descontado, vuelven al inventario.
3. **Vehículo:** al entregar o cancelar, vuelve a Activo (si estaba En servicio).
4. **Pantalla `OrderDetailController`** (reemplaza el marcador `order-detail.fxml` que dejó Dev 1), según el mockup: número y estado, pasos Recibido → Diagnóstico → En reparación → Listo → Entregado, «Pasar a …» y «Cancelar orden» con confirmación (avisa cuántos repuestos se descuentan o devuelven), cliente y vehículo con enlaces, diagnóstico, servicios y repuestos (con aviso del estado en el inventario), resumen de costos, mecánico responsable e historial con notas. Sin «Generar factura» ni «Comunicación» (Sprints 5 y 6).
5. **`FormDialog`** (componente nuevo): diálogo de formulario con acción en segundo plano que no se cierra si hay error; lo usan diagnóstico, agregar servicio o repuesto y responsable.
6. **`AppContext`:** `getOrderWorkflowService()` se crea al pedirlo, junto al bloque de copias de seguridad, para no tocar las líneas que usan los `AppContext.diff` de Dev 1.
7. **Pruebas:** `OrderWorkflowServiceTest` (14) con los servicios reales de vehículos, repuestos e inventario sobre repositorios falsos: flujo completo, stock insuficiente sin cambios, cancelación con y sin devolución, transiciones inválidas, líneas nuevas, diagnóstico, responsable y notas. Capturas del detalle en Diagnóstico y En reparación con una orden de ejemplo en memoria (sin escribir en Supabase). `mvn clean test` → **387 pruebas, 0 fallos**.

**Prueba manual del Líder (14/11/2026):**
- **«Creé una orden y no aparece en la lista».** Diagnóstico: `service_orders` estaba vacía (la orden nunca se guardó). El Mazda XYZ-789 figuraba «En servicio» desde los datos de ejemplo, no por esa orden. Se reprodujo «Crear orden» con 4 servicios contra la base real dentro de una transacción que siempre se deshace, y funcionó; tampoco hay diferencias entre el SQL de líneas e historial y las tablas. La pantalla solo atrapa `AutoGestException`, así que un error inesperado quedaba solo en la consola. **Arreglo:** `AutoGestApplication.installErrorHandler` muestra en un diálogo cualquier error no atrapado, con su tipo, mensaje y causa. Si se repite, el diálogo dirá el motivo exacto. Pendiente: volver a probar.
- **Espacio en blanco en Nueva orden** (`OrderNewController`, archivo de Dev 1, cambio pedido por el Líder): las tablas de servicios y repuestos crecían y dejaban un hueco bajo la columna izquierda. Ahora toman el alto de sus filas (`fitHeightToRows`), el estado inicial pasa a la columna derecha, el resumen va abajo a todo el ancho y Observaciones se estira para que ambas columnas terminen a la misma altura. El aviso de repuestos dice cuándo se descuentan: «cuando la orden pase a En reparación».

- **Segunda prueba (14/11/2026):** la orden #0001 se creó y aparece en la lista En reparación. Ajustes pedidos en archivos de Dev 1: los botones de servicios y repuestos de Nueva orden dicen solo «Agregar»; la lista de Órdenes da ancho mínimo a Cliente, Vehículo, Responsable, Estado («En reparación» completo) y Total, lleva la placa bajo el vehículo, usa títulos cortos de fecha («Ingreso», «Entrega») y deja sin título la columna del menú «…», para caber sin barra horizontal en 1280 px.

**Límite conocido:** el cambio de estado, el historial y los movimientos de stock se guardan en llamadas separadas (no en una sola transacción entre tablas); si falla el descuento de stock, la orden no cambia de estado.

**Estado del vehículo (consulta del Líder):** hoy no hay pantalla para cambiarlo a mano. El formulario de Vehículo no tiene campo de estado; «Dar de baja» lo pasa a Inactivo y no hay forma de reactivarlo. «En servicio» lo pondrá el módulo de Órdenes (Sprint 4: al crear una orden, Dev 1; al entregarla, Líder). El Mazda CX-5 aparece «En servicio» porque así viene en los datos de ejemplo (`docs/database/seed.sql`), y desactivar un cliente no cambia el estado de sus vehículos.

### 14/11/2026 — Sesión 22 (Líder): Sprint 5 — Facturación

**Contexto.** PR #15 (Sprint 4 del Líder) fusionado. Rama `feature/facturacion` desde `origin/develop` (`e78b3d3`). Todo el módulo es del Líder. Mockups: Facturación, Nueva factura y Detalle de factura (V1).

**Hecho:**
1. **Backend** (DAO → Repository → Service, como el resto):
   - `InvoiceDao`: CRUD, búsqueda (número de factura u orden, placa, vehículo o cliente; estado; emitidas desde; método), indicadores en una sola consulta (`InvoiceSummary`, modelo nuevo), paso a vencidas con un solo `UPDATE … RETURNING id`, órdenes con factura vigente y consecutivo. Las lecturas traen con un JOIN la orden, el vehículo y el cliente: la lista sale en una consulta.
   - `InvoiceRepository` (delgado) e `InvoiceService` con las reglas: solo órdenes **Listas o Entregadas** con al menos una línea; **una factura vigente por orden**; IVA en 0 % (`TAX_RATE`, D-05); descuento como valor que no supera el total (lo valida `Invoice.calculateTotal`); vencimiento por defecto a 14 días; pago y anulación con las reglas de `Invoice` (pagar desde Pendiente o Vencida, anular solo si no está pagada); cada factura generada, pagada o anulada deja su entrada en el **historial de la orden**.
   - Número visible `FAC-2026-001` (año y consecutivo del id más alto + 1, como las órdenes).
   - `AppContext.getInvoiceService()` se crea al pedirlo, junto a los bloques del Líder, con nombres completos de clase y sin tocar imports ni el final del constructor.
2. **Pantallas** (`controller/billing`): `BillingController` (indicadores Generadas / Pagadas / Pendientes / Vencidas, filtros de texto, estado, fecha —Hoy, Esta semana, Este mes— y método, tabla con «Ver factura», «Registrar pago» y «Anular», y resumen de lo facturado, pagado y por cobrar); `InvoiceNewController` (orden, cliente y vehículo, conceptos, descuento con el resumen recalculado mientras se escribe, vencimiento y «Registrar el pago ahora»); `InvoiceDetailController` (cliente y vehículo, orden relacionada, conceptos, resumen, información de pago e historial de la factura, con «Registrar pago» y «Anular factura»). Ayudas compartidas en `BillingUi` e `InvoiceLabels`. `View.INVOICE_NEW` e `INVOICE_DETAIL` junto a HOME / SETTINGS_PROFILE. Estilos al final de `autogest.css` (sección «Facturación»), reutilizando las clases `order-*` del detalle de la orden.
3. **Detalle de la orden** (`OrderDetailController`, archivo del Líder): en órdenes Listas o Entregadas sin factura aparece «Generar factura», que abre Nueva factura con la orden elegida; si ya tiene factura vigente aparece «Ver factura FAC-…» y se oculta «Cancelar orden».
4. **Vencidas:** al abrir la lista o un detalle se pasan a Vencida las pendientes cuyo vencimiento ya pasó.
5. **Pulido con `/impeccable polish`:** fechas completas en la tabla (anchos mínimos) y tabla al alto de sus filas; tarjetas de a dos con el mismo ancho; documento y teléfono en líneas separadas; «Ver vehículo» bajo el vehículo; el descuento inválido marca el campo en rojo y dice el motivo debajo; «Guardar factura» desactivado sin orden o con un descuento inválido; el desplegable de la orden muestra «#0045 · cliente» sin cortarse.
6. **Verificación:**
   - `InvoiceServiceTest` (25) con `FakeInvoiceRepository` y el `ServiceOrderService` real: totales, descuento, pago inmediato, estados facturables, una sola factura, reemisión tras anular, vencimiento, métodos, historial, órdenes facturables, pagar, anular, vencidas, indicadores, detalle y búsqueda. `mvn clean test` → **412 pruebas, 0 fallos**.
   - SQL del `InvoiceDao` probado contra Supabase **dentro de una transacción que siempre se deshace** (crear, leer con JOIN, buscar, vencer, pagar, indicadores, anular): todo correcto y la tabla quedó vacía. Efecto visible: la secuencia de `invoices.id` avanzó en uno (las secuencias de PostgreSQL no se deshacen); el número de factura no se afecta.
   - Capturas de la lista, Nueva factura (vacía, con orden y con descuento inválido) y el detalle (pagada y vencida) con datos de ejemplo en memoria, desde un programa temporal fuera del repositorio y sin configuración de base de datos (la lista ejecuta un `UPDATE` de vencidas al cargar). No se escribió en Supabase.

**Límites conocidos:**
- `invoices` no tiene líneas propias: los conceptos son las líneas de la orden y no se editan en la factura (el mockup permite editar y borrar conceptos y descuento por línea). El descuento es uno solo, sobre el total.
- No hay columnas para observaciones, fecha de pago ni usuario que crea o paga: el historial de la factura se arma con el historial de la orden, que sí guarda fecha, hora y usuario.
- La tabla exige un `order_id` único: una factura anulada se vuelve a emitir **sobre la misma fila** con un número nuevo.
- «Cancelar orden» se oculta en la pantalla si hay factura vigente, pero `OrderWorkflowService` no lo impide por sí mismo.
- ~~«Descargar PDF»~~ (hecho el 15/11, ver abajo). «Imprimir» y «Enviar al cliente» del mockup no están (no hay `NotificationService` todavía; el PDF se puede imprimir desde el visor).

**Prueba manual del Líder (15/11/2026): «el dashboard del inicio no marca las órdenes ni las facturas».** Las tarjetas «Órdenes activas» y «Facturación del mes» del Dashboard v1 (Sesión 12) seguían con «-» y «Disponible en el Sprint 4/5»: nunca se conectaron al llegar esos módulos. **Arreglo** (`HomeController`, archivo del Líder):
- «Órdenes activas» muestra las órdenes sin entregar ni cancelar y cuántas están listas para entregar; «Facturación del mes», el valor facturado este mes (sin anuladas), cuántas facturas y cuánto se cobró (`InvoiceService.getMonthSummary`, con `InvoiceDao.summary(desde)`). Ambas tarjetas abren su módulo con `NavigationEvent`, así respetan los permisos del rol.
- Todo el tablero se lee en una sola tarea en segundo plano (antes, unas 12 consultas en el hilo de la interfaz) y los vehículos usan `getVehicleCounts` (una consulta).
- «+ Nueva orden» abre Nueva orden (abría la lista).
- «Próximos mantenimientos» ya no corta el cliente ni el vehículo.
- Verificado con una captura del Inicio con los datos reales (solo lecturas): 1 orden activa, 1 lista para entregar y $240.000 facturados y cobrados este mes. `mvn clean test` → **413 pruebas, 0 fallos**.

**PDF de la factura (15/11/2026, pedido del Líder):**
- `InvoicePdfService` (nuevo) genera la factura en A4 con **Apache PDFBox 3.0.3** (dependencia nueva en `pom.xml`): logo y número, cliente, datos de la factura (emisión, vencimiento, orden y vehículo), conceptos, subtotal, descuento, IVA y total, y el estado del pago. Una factura anulada lleva «ANULADA». Si los conceptos no caben, la tabla sigue en otra página con su encabezado; el pie dice la fecha de generación y «Página X de Y». Incrusta las fuentes Roboto del proyecto (D-18); un carácter que la fuente no tiene (un emoji en un nombre) sale como «?» en vez de romper el PDF.
- «Descargar PDF» en el detalle de la factura y en el menú «…» de la lista: pide dónde guardar (por defecto Documentos, nombre «Factura FAC-2026-001.pdf»), lo genera en segundo plano y ofrece abrirlo.
- `AppContext.getInvoicePdfService()` en el bloque de Facturación del Líder.
- Pruebas: `InvoicePdfServiceTest` (7) lee el texto del PDF generado (número, cliente, conceptos, totales, pago, anulada, varias páginas, caracteres raros, archivo guardado). Revisado también como imagen (pagada y anulada). `mvn clean test` → **420 pruebas, 0 fallos**.
- Pendiente: los datos del taller (nombre, NIT, dirección, teléfono) en el encabezado, cuando exista «Datos del taller» de Dev 1 (Sprint 8).

**PR #16 cerrado y avisos al cliente conectados (15/11/2026):**
- El PR #16 se abrió y se cerró el mismo día, a pedido del Líder, para que entrara primero el de Dev 1. La rama quedó intacta.
- Dev 1 fusionó el backend de Notificaciones (**PR #17**: `NotificationDao/Repository/Service`, 19 pruebas). Se fusionó `develop` en `feature/facturacion` **sin conflictos**: sus bloques de `AppContext` (imports, campos y constructor al final) no tocan los del Líder.
- `InvoiceService` recibe el `NotificationService` (`AppContext.getInvoiceService()` se lo pasa) y avisa al cliente: «Factura disponible» al generar una factura pendiente, «Pago recibido» al pagarla (si se paga al generarla, solo este) y «Factura vencida» una sola vez, cuando `refreshOverdue` la pasa a vencida. Un aviso que falla no deshace la factura: sin correo queda «Fallida» (la reintenta Notificaciones) y un error inesperado se ignora.
- `InvoiceServiceTest` usa el `NotificationService` real de Dev 1 sobre su `FakeNotificationRepository`: 6 pruebas nuevas (emitida, pagada, pago inmediato, vencida una sola vez, cliente sin correo, cliente inexistente). `mvn clean test` → **445 pruebas, 0 fallos**.
- No se tocó ningún archivo de Dev 1.

**Cierre del Sprint 5 (15/11/2026):** Dev 1 revisó y fusionó el PR #16 (`b858e80`); `develop` → 445 pruebas, 0 fallos. Rama `feature/facturacion` borrada (GitHub la remota, el Líder la local).

### 16/11/2026 — Sesión 23 (Líder): Sprint 6 — plantillas, avisos automáticos, Inicio y campana

**Contexto.** Dev 1 fusionó la interfaz de Notificaciones (**PR #18**: historial con indicadores y filtros, nueva notificación, detalle con envío y reintento, `NotificationLabels` y 3 pruebas). `develop` → 448 pruebas, 0 fallos. Rama `feature/notificaciones-eventos` desde `develop` (`96ff25e`). La guía no estaba a mano: el alcance de «Plantillas y eventos» se definió con el Líder (avisos automáticos con textos de plantilla, más la tarjeta del Inicio y la campana).

**Revisión de la interfaz de Dev 1** (captura con datos reales, solo lecturas; **no se tocaron sus archivos**, se le pasan como comentarios):
1. Las etiquetas de los filtros («Tipo», «Canal», «Estado») salen cortadas como «…» en 1280 px.
2. La lista y sus acciones consultan Supabase en el hilo de la interfaz (sin `Background.run`).
3. Las columnas de la tabla no tienen ancho mínimo (`addColumn(título, valor, ancho)`).

**Hecho (Líder):**
1. **`NotificationTemplate`** (nuevo, `service`): plantillas «Vehículo listo» (`SERVICE_UPDATE`) y «Recordatorio de mantenimiento» (`MAINTENANCE_REMINDER`) con campos `{cliente}`, `{vehiculo}`, `{placa}`, `{orden}`, `{fecha}` y `{taller}`. Un campo sin valor queda vacío (nunca con llaves) y un campo desconocido se rechaza.
2. **`NotificationEventService`** (nuevo): usa el `NotificationService` de Dev 1 sin modificarlo.
   - `notifyVehicleReady`: `OrderWorkflowService` lo dispara al pasar una orden a **Lista**; deja en el historial de la orden «Se avisó al cliente…» o «No se pudo avisar…». Un aviso que falla no deshace el cambio de estado.
   - Recordatorios de mantenimiento: vehículos con mantenimiento vencido o por vencer (D-13), no dados de baja y con propietario, **una vez cada 30 días por vehículo** (se reconoce por la placa en el asunto). Se envían desde el Inicio con «Enviar recordatorios»; uno que falla no detiene los demás.
3. **Inicio:** la tarjeta «Notificaciones» (antes «Disponible en el Sprint 6») muestra los 3 avisos más recientes con su estado, cuántos fallaron y cuántos vehículos necesitan recordatorio, con «Ver todas» y «Enviar recordatorios» solo para los roles con acceso a Notificaciones.
4. **Campana de la Topbar:** abre Notificaciones (con los permisos del rol) y muestra un contador de avisos fallidos o pendientes, que se actualiza en segundo plano al cambiar de pantalla.
5. **`AppContext`:** bloque «Avisos automáticos (Líder, Sprint 6)» junto a Facturación, con nombres completos y creado al pedirlo; `getOrderWorkflowService()` le pasa los avisos.
6. **Pruebas:** `NotificationEventServiceTest` (12: plantillas, vehículo listo, recordatorios por fecha, inactivos, una vez por ventana, después de la ventana, dos vehículos del mismo cliente, cliente sin correo) y 2 en `OrderWorkflowServiceTest` (aviso al pasar a Lista una sola vez; sin correo el estado cambia igual). Todas con el `NotificationService` real de Dev 1. `mvn clean test` → **462 pruebas, 0 fallos**.
7. **Captura del Inicio** con los datos reales (solo lecturas) y una sesión de administrador en memoria: contador en la campana, «Ver todas» y «3 vehículos necesitan recordatorio de mantenimiento». No se escribió en Supabase.

**Envío real de correos por Gmail (16/11/2026, pedido del Líder; rama `feature/correo-smtp`, sobre la Sesión 23):**
- Hasta ahora el canal EMAIL era simulado: con correo del cliente quedaba «Enviada» sin salir del equipo (análisis de Dev 1: envío real «fuera del alcance»).
- **`EmailSender`** (interfaz nueva) y **`SmtpEmailSender`** con **Angus Mail 2.0.3** (Jakarta Mail, dependencia nueva): SMTP con usuario y contraseña, STARTTLS y tiempo límite de 15 s. Pensado para Gmail con una **contraseña de aplicación**; firma con el nombre del taller y errores legibles («el usuario o la contraseña de aplicación no son válidos», «no hubo conexión…») sin exponer la cuenta.
- **`EmailConfig`** arma el envío desde `application.local.properties` (claves `mail.username`, `mail.password`, `mail.from.name` y, opcionales, `mail.smtp.host/port/starttls` con los valores de Gmail por defecto). **Sin cuenta configurada, el correo se sigue simulando como antes**: la app funciona igual en el equipo de Dev 1 o del profesor. `application.example.properties` explica cómo crear la contraseña de aplicación. `DatabaseConnection.loadProperties` pasa a ser visible en el paquete `config` para reutilizar la lectura del archivo.
- **`AppContext.getInstance()`** configura el envío al crear el contexto (método `configureEmail` del Líder), sin tocar el constructor ni los bloques de Dev 1.
- **Cambio en un archivo de Dev 1, pedido por el Líder:** `NotificationService` recibe un `EmailSender` opcional (`setEmailSender`) y el caso EMAIL de `deliver` lo usa: si envía, «Enviada»; si falla (sin conexión, credenciales inválidas), «Fallida» para reintentar. Sin `EmailSender` el comportamiento es el mismo de antes. Se actualizó su Javadoc de `deliver`. **Sebastián: tenerlo en cuenta al actualizar tu rama;** sus 19 pruebas pasan sin cambios.
- **Pruebas:** `SmtpEmailSenderTest` (5) contra **GreenMail 2.1.2** (servidor SMTP falso en memoria, solo pruebas: nadie recibe correos): asunto, destinatario, remitente y firma, espacios en la contraseña, contraseña inválida, servidor caído y datos faltantes. `NotificationEmailTest` (7): envío real con un `EmailSender` falso, error → Fallida, reintento cuando vuelve la conexión, simulado sin cuenta, cliente sin correo y lectura de la configuración. GreenMail traía JUnit 5.11: se excluyó en el `pom.xml` para usar el 5.12 del proyecto. `mvn clean test` → **474 pruebas, 0 fallos**.
- **Ojo al activarlo:** desde ese momento cada factura, orden Lista o recordatorio le escribe de verdad al correo del cliente guardado en Supabase. Los clientes de ejemplo usan `@example.com` (no llega a nadie), pero los creados a mano pueden tener correos reales.
- **Prueba manual del Líder (17/11/2026):** con la cuenta de Gmail configurada, el correo llegó a un buzón real.
---

## 4. Decisiones y acuerdos

| Fecha | Decisión | Motivo |
|---|---|---|
| 08/10/2026 | **Comentarios y Javadoc en español** (no en inglés, como dice la guía). Los textos de pantalla siguen en español y los commits en español. | Coherencia con el commit `a3d983b` y el `IconSet` que ya venían en español. **Ojo:** contradice la sección 5 de la guía; conviene actualizar la guía para que no se re-dispare con los demás roles. |
| 08/10/2026 | **Fuentes Roboto `.ttf` pendientes.** | Se acordó no descargarlas todavía. El CSS ya cae a «Segoe UI». Al agregarlas, cargar con `Font.loadFont` en `AutoGestApplication`. |
| 08/10/2026 | La pantalla `demo-components.fxml` **no** es una vista de la barra lateral. | Es solo para desarrollo; se abre cargando el FXML aparte. |
| 08/10/2026 | `PagedTable` ordena sobre el **conjunto completo**, no solo la página visible. | Evita el error de que la paginación y el orden se contradigan. |
| 08/10/2026 | El archivo `AGENTS.md` adjunto (GYMBROT / HTML) **se ignora**. | Pertenece a otro proyecto y no aplica a AutoGest. |
| 09/10/2026 | **Esperar a Dev 1 y Dev 2** antes de continuar el Sprint 2. | El modelo (`User`, `Role`, `UserStatus`) es de Dev 1 y la infraestructura (`CrudOperations`, excepciones, `DatabaseConnection`) es de Dev 2. No se tocan archivos de otros roles sin pedido de cambio. |
| 10/10/2026 | Integrar modelo (Dev 1) e infraestructura (Dev 2) en `develop` **vía PR** antes de continuar el Sprint 2. | Desbloquea `UserDao`/`UserRepository`/`AuthenticationService`/`UserService` sin tocar archivos de otros roles. Ambos PRs verificados con `mvn clean test` (**108 pruebas**). |
| 10/10/2026 | **`feature/base-datos` (alcance Dev 2) fusionada directo a `develop` por el Líder.** | No hay Dev 2 vinculado, ni revisor, ni tooling (`gh`/token). La guía (§10) asigna esas tareas al Líder. Merge verificado sin conflictos y con 108 pruebas verdes. |
| 14/10/2026 | **Las responsabilidades del Dev 2 pasan al Líder** (Vehículos, Inventario y Repuestos, y el resto de su columna en la hoja de ruta). | No hay tercer integrante vinculado. Los PRs del Líder los sigue revisando Sebastián; si entra un nuevo integrante se reparte de nuevo. |
| 14/10/2026 | **Idioma de comentarios y Javadoc: por resolver.** Hoy el código y el README usan español; la guía V2.0 pide inglés. | Hay que elegir una sola convención y alinear README, guía y código antes de que crezca la diferencia (Clientes ya está en español). |
| 16/10/2026 | La búsqueda con clase se publica como **`searchVehiclesByType(text, status, type)`** y la del contrato 7.5 queda en `searchVehicles(text, make, status)`. | Dos sobrecargas de 3 argumentos con enums nulos hacían ambigua toda llamada `searchVehicles(text, null, null)` y no compilaba. |
| 16/10/2026 | **`deleteVehicle(int)` es la baja lógica** (D-02) y delega en `deactivateVehicle(int)`; `countInServiceVehicles()` queda deprecado a favor de `countInService()`. | El contrato 7.5 exige esos nombres; no se duplica la lógica ni se borran filas. |
| 16/10/2026 | Las pantallas de vehículos llevan **FXML mínimo (un `VBox` con `fx:id="root"`) y el contenido se construye en el controlador**. | Mismo patrón que `users-roles.fxml` y `UsersRolesController`; reutiliza `PageHeader`, `KpiCard`, `FilterBar`, `PagedTable` y `StatusBadge`. |
| 16/10/2026 | La prueba de carga de FXML va etiquetada **`@Tag("integration")`**. | Necesita toolkit JavaFX y base de datos; no puede romper el `mvn clean test` por defecto (p. ej. con `ErrorDialog` modal). |
| 17/10/2026 | **La subida de `feature/vehiculos` (PR3) no se bloquea por la prueba de humo de FXML.** | El bloqueo de la prueba se reprodujo (17/10/2026) y está fuera de `mvn clean test`; diagnosticarla no debe frenar la integración del módulo ya verificado con 245 pruebas verdes. |
| 24/10/2026 | **Sprint 3 iniciado** en `feature/servicios-inventario`. | PR #9 (Vehículos) fusionado en `develop`. Distribución: Líder (Dashboard v1 + Servicios + base Inventario) · Dev 1 (Repuestos + Movimientos). |
| 26/10/2026 | **Textos e íconos de la barra lateral en blanco** (`#FFFFFF` en lugar de `#C7D2E3`). | Pedido del Líder: el gris no contrastaba con el fondo navy. El ítem activo sigue destacando por el fondo `primary` y el peso 700. |
| 26/10/2026 | **El color y la fuente de un ítem se aplican al `Label`, no al contenedor** (`.sidebar-item .label`). | En JavaFX un `HBox` no hereda `-fx-text-fill` ni la fuente a sus hijos; el blanco de la Sesión 12 no se veía al ejecutar. |
| 26/10/2026 | **La Topbar va dentro del área de contenido**, no en el `top` del `BorderPane`. | En los mockups la Sidebar ocupa todo el alto de la ventana. |
| 26/10/2026 | **Los mockups (`V1/`) no se suben a Git**; solo las imágenes que usa la app, convertidas a PNG en `resources/images/`. | La carpeta pesa ~80 MB y sirve solo de referencia de diseño. |
| 11/11/2026 | ~~El backend de Movimientos lo hace el Líder.~~ **Corregida el 12/11/2026.** | Se anotó sin revisar el paquete del Desarrollador 1 (04/11). |
| 11/11/2026 | ~~El Líder avanza al Sprint 4 sin esperar a Dev 1.~~ **Reemplazada el 12/11/2026.** | La parte del Líder del Sprint 4 depende de servicios de Dev 1 (ver abajo). |
| 12/11/2026 | **El backend de Movimientos (`InventoryMovementDao/Repository`, `InventoryService`) y el de órdenes (`ServiceOrderDao`, `ServiceOrderRepository`, `ServiceOrderService`) son de Dev 1**, como dice su paquete «Sprints 3 a 8» (`analysis-inventory.md`, `analysis-orders.md`). | En el Sprint 4 el Líder hace el detalle, el cambio de estado, el historial y el descuento de stock, sobre esos servicios. |
| 12/11/2026 | **El Líder adelanta el Sprint 8 (Configuración) en `feature/configuracion`** mientras Dev 1 entrega sus sprints 3 y 4. | Configuración no depende de Órdenes ni de Inventario. Las tarjetas «Datos del taller» y «Preferencias» (Dev 1) se habilitan solas cuando existan su `View` y su FXML. No se crean `controller/settings/package-info.java` ni `css/settings.css`, que vienen en el paquete de Dev 1. |
| 12/11/2026 | **`.impeccable/` y `.claude/` no van al repositorio** (están en `.gitignore`). | Son configuración local de herramientas del Líder. `.impeccable/config.json` se había subido por error en la Sesión 16 y se quitó del repositorio (el archivo sigue en el equipo del Líder). |
| 12/11/2026 | **Los commits y PR no llevan atribución a herramientas de IA** (`Co-Authored-By`, «Generated with…»). | Pedido del Líder. Se reescribió `develop` (15 commits desde `940a19a`) sin esas líneas y con el mismo contenido; las páginas de los PR #10 y #11 conservan los commits originales porque GitHub no las modifica. |
| 14/11/2026 | **Facturación: IVA en 0 % y vencimiento a 14 días** (`InvoiceService.TAX_RATE`, `DEFAULT_DUE_DAYS`). | D-05: la tasa se leerá de Configuración cuando exista el `SettingsService` de Dev 1 (su Sprint 8). Los 14 días salen del mockup (emisión 02/10, vencimiento 16/10). |
| 14/11/2026 | **Los métodos de pago se guardan como códigos** (`CASH`, `CARD`, `TRANSFER`, `OTHER`) en el texto `payment_method`. | `Invoice` usa `String` mientras no exista `PaymentMethod`; los códigos coinciden con los nombres que tendría ese enum, así el cambio no requiere migrar datos. Los nombres en español viven en `InvoiceService.methodText` / `InvoiceLabels`. |
| 14/11/2026 | **Solo se facturan órdenes Listas o Entregadas, con una factura vigente por orden;** al anular, la orden puede facturarse de nuevo y se reutiliza la misma fila. | Pedido del Líder (una orden tiene 0..1 factura) y restricción `UNIQUE (order_id)` de `invoices`. |
| 14/11/2026 | **Generar, pagar y anular dejan constancia en el historial de la orden.** | `invoices` no guarda fechas de pago ni usuarios; así queda trazabilidad sin cambiar el esquema de Dev 2. |
| 16/11/2026 | **Envío real de correos por SMTP de Gmail, opcional** (Angus Mail; credenciales en `application.local.properties`). | Gratis y suficiente para un taller (unos 500 correos al día). Sin cuenta configurada se simula como antes, así nadie depende de la cuenta del Líder para correr la app. Un servicio por API (Brevo, Resend) entrega mejor pero exige cuenta, llave y dominio verificado; WhatsApp exige la API de Meta Business. |
| 16/11/2026 | **Plantillas de aviso en el código** (`NotificationTemplate`) mientras no exista la configuración de Notificaciones. | Editarlas requiere guardarlas en `app_settings`, que llega con el `SettingsService` de Dev 1 (Sprint 8). Los eventos no cambian cuando eso pase: solo de dónde salen los textos. |
| 16/11/2026 | **Los recordatorios de mantenimiento se envían con un botón en el Inicio**, una vez cada 30 días por vehículo. | Enviarlos solos al abrir la app escribiría en la base de datos sin que nadie lo pida; con el botón el taller decide cuándo. La ventana de 30 días evita repetirlos. |
| 16/11/2026 | **El contador de la campana suma avisos fallidos y pendientes.** | Son los que necesitan atención: los fallidos se reintentan y los pendientes no han salido. Solo lo ven los roles con acceso a Notificaciones. |
| 15/11/2026 | **PDF de la factura con Apache PDFBox 3.0.3** (dependencia nueva). | Licencia Apache, sin servicios externos, permite incrustar Roboto y probar el contenido leyendo el texto del PDF. Alternativas descartadas: imprimir desde JavaFX a «Microsoft Print to PDF» (depende del equipo y no se puede probar) y librerías con licencia AGPL. |
| 14/11/2026 | **Las capturas de Facturación se toman fuera del repositorio y sin configuración de base de datos.** | La lista ejecuta el paso a vencidas (`UPDATE`) al cargar; así las capturas no escriben en Supabase. |

---

## 5. Pendientes y próximo

**Pendientes del Sprint 1:**
- [x] ~~Crear/mergear el Pull Request a `develop`~~ — PR #1 fusionado (`2946d88`).
- [x] ~~Sincronizar `develop` y crear la rama del Sprint 2~~ — hecho (`feature/login-usuarios`).
- [x] ~~Fuentes Roboto Regular/Medium/Bold en `resources/css/fonts/` + `Font.loadFont`~~ — hecho (Sesión 7).
- [x] ~~Eliminar la rama `feature/base-presentacion` (local y remota) tras el merge~~ — hecho (17/10/2026).
- [x] ~~`git stash drop` (el `stash@{0}` de iconos ya está incorporado)~~ — hecho (17/10/2026).

**✅ Dependencia resuelta — Sprint 2 destrabado (10/10/2026):**

| Necesito de | Pieza (contrato) | Estado |
|---|---|---|
| Dev 1 | `model/User`, `model/Role`, `model/UserStatus` | ✅ en `develop` (PR #2 fusionado) |
| Dev 2 | `dao/CrudOperations<T>` (firma 7.1) | ✅ en `develop` (merge `66daf92`) |
| Dev 2 | `exception/*` (`ValidationException`, `NotFoundException`, `DataAccessException`) | ✅ en `develop` |
| Dev 2 | `config/DatabaseConnection` (firma 7.1) | ✅ en `develop` (merge `66daf92`) |

Verificación: `develop` → `mvn clean test` **108 pruebas, 0 fallos**
(ver §3, Sesión 4).

**Pendiente inmediato:** ~~crear y fusionar los PRs~~ — resuelto (ver §3, Sesión 4):
`base-modelo` por PR #2 y `base-datos` fusionada por el Líder. `develop` y
`feature/login-usuarios` sincronizados y verdes.

**En curso (Líder, Sprint 2 — rama `feature/login-usuarios`):**
- [x] `PasswordHasher` (PBKDF2) + 6 pruebas.
- [x] ~~Sincronizar la rama con `develop`~~ — hecho (Sesión 4).
- [x] `UserDao`, `UserRepository`, `AuthenticationService`, `UserService`,
      `Session`, `Permissions` y pruebas — backend completo (Sesión 5).
- [x] `docs/database/seed-usuarios.sql` (hashes generados con `PasswordHasher`).
- [x] Interfaz: Login, shell con sesión (Topbar con usuario y «Cerrar sesión»,
      Sidebar con permisos) y Usuarios y roles (Sesión 6).

**En curso (Líder, Sprint 2 — rama `feature/vehiculos`):**
- [x] `VehicleDao`, `VehicleRepository`, `VehicleService`, bloque *Vehicles* de
      `AppContext` y `VehicleServiceTest` con repositorio falso (5 commits
      previos, Sesión 7).
- [x] Backend ajustado al contrato 7.5: búsqueda por marca, `countByCustomer`,
      `findMakes`, `updateMileage`, `deleteVehicle`, `countInService`
      (Sesión 8).
- [x] Interfaz: `VehicleLabels`, `VehiclesController`, `VehicleFormController`
      + `VehicleNew`/`VehicleEdit`, `VehicleDetailController`, `package-info` y
      los 4 FXML reales (Sesión 8).
- [ ] Prueba de humo de FXML (`VehicleScreensSmokeTest`, `@Tag("integration")`)
      diagnósticada y verde con `mvn test -Pintegration` — **el bloqueo se
      reprodujo el 17/10/2026** (Sesión 9).
- [x] ~~Commitear la Sesión 8 en commits atómicos y subir `feature/vehiculos`~~ —
      commits `e61a6bd..c426931` + merge `7ae4c9b` (Sesión 9).

**Cierre del Sprint 2 (Fase 3):**
- [x] ~~Subir `feature/login-usuarios` y abrir el **PR2** (`→ develop`)~~ — resuelto: el merge `3015e20` es *Merge pull request #7 from lsteveali/feature/login-usuarios* y de ahí nace `feature/vehiculos`.
- [x] ~~Abrir el **PR3** de `feature/vehiculos` (`→ develop`)~~ — resuelto: fusionado como **PR #9** (`480e33c`).
- [x] ~~Higiene Git: borrar ramas fusionadas (`base-presentacion`, `base-modelo`,
      `base-datos`, `feature/clientes`) y `git stash drop`~~ — hecho (Sesión 9):
      solo quedan `develop`, `main` y `feature/vehiculos`.
- [x] ~~Fuentes Roboto `.ttf` + `Font.loadFont` (D-18)~~ — hecho (Sesión 7).

**En curso (Líder, Sprint 3 — rama `feature/servicios-inventario`):**
- [x] Base transversal: `ServiceOfferingDao/Repository/Service` y `PartDao/Repository/Service` + cableado en `AppContext` (Sesión 10).
- [x] `View` con `SERVICE_NEW/EDIT/DETAIL` y `PART_NEW/EDIT/DETAIL` (Sesión 10).
- [x] UI del catálogo de Servicios: lista, alta/edición, detalle y `ServiceLabels` (Sesión 11, 25/10/2026).
- [x] Dashboard v1 (Sesión 12, 26/10/2026): KPIs, estado del parque, alertas de inventario, mantenimientos y placeholder de notificaciones.
- [x] Pruebas del backend del Sprint 3: `ServiceOfferingServiceTest` (29) y `PartServiceTest` (38) con repositorios falsos — **312 pruebas, 0 fallos** (Sesión 13, 26/10/2026). La semilla del catálogo ya existía en `docs/database/seed.sql`.
- [x] Logo, barra lateral, Topbar y login según los mockups (Sesión 14, 26/10/2026).
- [x] Pulido de estados de interacción: hover, presionado, foco, áreas de clic y entrada de pantallas (Sesión 15, 09/11/2026).
- [x] Barra de desplazamiento en formularios y espacio para el botón del Asistente (Sesión 15); filtros, barra lateral, tablas y campos con `/impeccable polish` (Sesión 16, 11/11/2026).
- [x] UI de Repuestos + Movimientos de inventario (Dev 1) — PR #13 (14/11/2026).

**Sprint 4 (Líder, `feature/ordenes-detalle`):**
- [x] Detalle de la orden, cambio de estado, historial, diagnóstico, responsable, líneas nuevas y descuento de stock (Sesión 21).
- [ ] Reactivar un vehículo dado de baja (pendiente de decidir; al entregar o cancelar una orden el vehículo ya vuelve a Activo).
- [x] ~~PR #15 fusionado en `develop`~~ — base de `feature/facturacion`.

**Sprint 5 (Líder, `feature/facturacion`):**
- [x] Backend, lista, nueva factura, detalle, pago, anulación y «Generar factura» en la orden (Sesión 22).
- [x] ~~Subir la rama y abrir el PR hacia `develop`~~ — PR #16 fusionado (`b858e80`, 15/11/2026).
- [x] ~~**Avisos al cliente**~~ — conectados el 15/11/2026 tras el PR #17 de Dev 1 (ver §3, Sesión 22).
- [x] ~~Volver a abrir el PR de `feature/facturacion`~~ — reabierto, revisado por Dev 1 y fusionado; `develop` → **445 pruebas, 0 fallos**.
- [ ] Tasa de IVA y plazo de pago desde Configuración de Facturación cuando exista el `SettingsService` de Dev 1 (Sprint 8).
- [ ] «Ver facturas» en el menú del detalle del cliente (`CustomerDetailController`, archivo de Dev 1) sigue desactivado: habilitarlo solo si el Líder lo pide.
- [ ] Decidir si `OrderWorkflowService` debe impedir cancelar una orden con factura vigente (hoy solo lo evita la pantalla).
- [x] ~~Descargar la factura en PDF~~ — hecho (15/11/2026, `InvoicePdfService`).
- [ ] Datos del taller en el encabezado del PDF cuando exista «Datos del taller» (Dev 1, Sprint 8).

**Sprint 6 (Líder, `feature/notificaciones-eventos`):**
- [x] Plantillas, aviso «vehículo listo», recordatorios de mantenimiento, tarjeta del Inicio y campana (Sesión 23).
- [ ] Subir la rama y abrir el PR hacia `develop` después de la prueba manual del Líder.
- [ ] Editar las plantillas desde Configuración de Notificaciones cuando exista el `SettingsService` de Dev 1 (Sprint 8); también el nombre del taller (`{taller}`).
- [ ] Comentarios para Dev 1 sobre su pantalla de Notificaciones: etiquetas de filtros cortadas, consultas en el hilo de la interfaz y anchos mínimos de columnas (ver Sesión 23). Con el correo real esto pesa más: «Nueva notificación», «Enviar ahora» y «Reintentar» esperan la respuesta de Gmail (1 a 3 s) con la ventana congelada.

**Correo real (rama `feature/correo-smtp`):**
- [x] Envío por SMTP de Gmail, opcional, con pruebas contra GreenMail (16/11/2026).
- [x] ~~Configurar la cuenta de Gmail del taller y probar la entrega~~ — hecho por el Líder (17/11/2026): el correo llegó a un buzón real.
- [ ] Fusionar el PR #20 (`feature/correo-smtp`) después de la revisión de Dev 1.
- [ ] Adjuntar el PDF de la factura al aviso «Factura disponible».

**Barrido de interfaz pendiente (lo hará el Líder):** Inventario (filtros, nombres y estados cortados; «Volver» repetido en el detalle del repuesto; «Registrar entrada» debajo de «+ Nuevo repuesto») (Órdenes ya corregida en la Sesión 21).

**En curso (Líder, Sprint 8 — rama `feature/configuracion`):**
- [x] Pantalla general de Configuración: tarjeta del usuario, secciones e información del sistema (Sesión 17).
- [x] Perfil y cuenta: datos personales y cambio de contraseña; también desde «Mi perfil» en la Topbar (Sesión 17).
- [x] Seguridad: política de contraseñas, permisos por rol y último acceso (Sesión 18).
- [x] Copias de seguridad: crear y listar (Sesión 18).
- [ ] Restaurar una copia desde AutoGest (vaciar y recargar las tablas en orden de dependencias, en una transacción).
- [ ] Configuración de Facturación y de Notificaciones (guardan en `app_settings` de Dev 1: esperan su `SettingsService`).

**Pendientes nuevos (14/10/2026):**
- [x] ~~Confirmar el estado del PR del backend de Clientes~~ — resuelto: PR **#5** (backend) y **#8** (interfaz completa) fusionados en `develop`; siguen pendientes de revisión de Sebastián los PRs del Líder.
- [x] ~~Correr `mvn clean test` sobre V3.0 y anotar el número de pruebas~~ — hecho: **245 pruebas, 0 fallos** con Clientes integrado (17/10/2026, `feature/vehiculos`, ver §3 Sesión 9).
- [x] ~~Prueba manual: `mvn javafx:run` con login real contra Supabase~~ — la hace el Líder tras cada cambio de interfaz.
- [x] ~~Sebastián: interfaz de Clientes y bloque *Customers* en `AppContext`~~ — hecho (PR #8, 17/10/2026).
- [x] ~~Líder: Vehículos (backend + interfaz), tras Clientes~~ — backend ✅ e interfaz ✅ (Sesión 9); solo queda la prueba de humo de FXML.
- [x] ~~Documentos del Sprint 2: análisis y diagrama de clases de Usuarios, Clientes y Vehículos~~ — hecho (PR #8 y commit `4f9b5d5`, PR #9).
- [ ] Decidir el idioma de comentarios y Javadoc y alinear README, guía y código.
- [ ] Definir las fechas de entrega de cada sprint (la guía las deja `[PENDIENTE]`).
- [ ] Cerrado el Sprint 2: decidir si se integra `develop` en `main` y se etiqueta `v1.0`.

---

## 6. Cómo mantener esta bitácora

1. Al **terminar** una tarea, actualizar su fila en la sección 2 (⬜ → 🟡 en
   progreso → ✅ hecho) y agregar la nota o el commit.
2. Registrar una **entrada nueva** en la sección 3 con la fecha y un resumen de
   lo hecho, lo verificado y lo que quedó pendiente.
3. Anotar en la sección 4 cualquier decisión que se desvíe de la guía.
4. Actualizar la fecha de «Última actualización» del encabezado.
