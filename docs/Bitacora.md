# Bitácora de desarrollo — AutoGest

> **Documento vivo.** Se actualiza a medida que se avanza. Cada vez que se
> termina una tarea se agrega/actualiza su estado y, cuando aporta, una entrada
> en la sección «Bitácora cronológica».
>
> Última actualización: **26/10/2026** — Líder (Leonarth Steve Ali Camargo).

---

## 1. Información general

| Campo | Valor |
|---|---|
| Proyecto | AutoGest — Sistema de gestión integral para talleres automotrices |
| Materia | Programación de Computadores III (SS462) |
| Repositorio | https://github.com/lsteveali/AutoGest.git |
| Rama de trabajo actual | `feature/servicios-inventario` (Sprint 3 — Servicios, Inventario y Repuestos) |
| Sprint en curso | **Sprint 3 — Servicios, Inventario y Repuestos** (Líder: Dashboard v1 + Servicios + Inventario base · Dev 1: Repuestos + Movimientos de inventario) |
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
| 3 | Servicios, Inventario y Repuestos (Líder: Dashboard v1) | 🟡 En curso |
| 4 | Órdenes de servicio (Líder: estados e historial) | ⬜ No iniciado |
| 5 | Facturación | ⬜ No iniciado |
| 6 | Notificaciones | ⬜ No iniciado |
| 7 | Reportes | ⬜ No iniciado |
| 8 | Configuración | ⬜ No iniciado |
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

**Pendiente:** prueba manual con `mvn javafx:run` (saludo, conteos y scroll del panel).

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

**Pendiente:** prueba manual con `mvn javafx:run` (login real y shell).

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
- [ ] UI de Repuestos + Movimientos de inventario (Dev 1).

**Pendientes nuevos (14/10/2026):**
- [x] ~~Confirmar el estado del PR del backend de Clientes~~ — resuelto: PR **#5** (backend) y **#8** (interfaz completa) fusionados en `develop`; siguen pendientes de revisión de Sebastián los PRs del Líder.
- [x] ~~Correr `mvn clean test` sobre V3.0 y anotar el número de pruebas~~ — hecho: **245 pruebas, 0 fallos** con Clientes integrado (17/10/2026, `feature/vehiculos`, ver §3 Sesión 9).
- [ ] Prueba manual: `mvn javafx:run` con login real contra Supabase (usuarios de `seed-usuarios.sql`).
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
