# Análisis - Módulo Vehículos (Líder)

**Autor:** Leonarth Steve Ali Camargo (Líder) · AutoGest  
Respuestas a las preguntas de análisis de la sección 12.4 de la guía. Cada respuesta cierra con la línea de trazabilidad *Principio -> clase o método* que alimenta la sección 2.6 del documento del proyecto.

## 1. ¿Por qué `Vehicle` es una entidad independiente y guarda una referencia a `Customer` en lugar de duplicar sus datos?

Un **vehículo pertenece a un cliente**. Duplicar nombre/teléfono/correo en `Vehicle` crearía inconsistencias cuando el cliente actualice sus datos.

- **Normalización.** `Vehicle.owner` es una referencia a `Customer` (relación 1 cliente → 0..* vehículos). `VehicleDetailController` muestra los datos del propietario desde esa referencia; la tabla «Otros vehículos del propietario» se obtiene vía `service.findByCustomer(owner.getId())` filtrando el actual.
- **Ciclo de vida distinto.** El cliente puede existir sin vehículos; un vehículo debe pertenecer a un cliente válido. Dar de baja un cliente o editarlo no debe obligar a replicar datos por cada vehículo.
- **Mantenibilidad.** Un solo origen de verdad para los datos del propietario.

*Trazabilidad: GRASP Information Expert / Low Coupling -> `Vehicle.owner` referencia a `Customer`; GRASP High Cohesion -> cada entidad mantiene sus propios datos.*

## 2. ¿Por qué `isMaintenanceDue(LocalDate)` es un cálculo y no un campo persistido? ¿Qué decisión del proyecto respalda esto?

La guía toma la **decisión D-13**: el aviso «Mantenimiento próximo» **no es un estado guardado**.

- **Cálculo dinámico.** `Vehicle.isMaintenanceDue(LocalDate today)` devuelve `true` si existe `nextMaintenanceDate` y no está después de `today.plusDays(30)` (incluido), usando `MAINTENANCE_WARNING_DAYS = 30`. Los controladores traducen esto a texto/insignia con `VehicleLabels.maintenanceText(...)` y `VehicleLabels.maintenanceType(...)`.
- **Razones:** evita desincronización con el tiempo (cada vez que se abre la pantalla se evalúa con la fecha actual), evita jobs de actualización para marcar/desmarcar, y es fácilmente testeable pasándole una fecha fija (`VehicleTest` lo verifica).
- **Información experta.** `Vehicle` conoce `nextMaintenanceDate` y la regla de anticipación; es el experto para este cálculo.

*Trazabilidad: GRASP Information Expert -> `Vehicle.isMaintenanceDue(LocalDate)`; decisión D-13 (cálculo, no persistido).*

## 3. ¿Por qué `updateMileage(int)` valida que no disminuya y por qué el kilometraje se actualiza con ese método en lugar de solo con setters?

- **Regla de negocio.** El kilometraje de un vehículo **no puede retroceder**. `Vehicle.updateMileage(mileage)` lanza `ValidationException("El kilometraje no puede ser menor al actual")` si `mileage < this.mileage`, permite igualdad y actualiza si es mayor o igual.
- **Intención clara.** Los setters son genéricos; `updateMileage` comunica la intención («actualizar kilometraje con regla») y concentra la validación en el modelo (*Information Expert*).
- **Consistencia.** El Service/DAO pueden usarlo cuando corresponda, y la regla queda en un único lugar.

*Trazabilidad: GRASP Information Expert -> `Vehicle.updateMileage(int)`; SOLID SRP -> regla de negocio en el modelo/dominio.*

## 4. ¿Por qué la búsqueda de vehículos acepta `make` como filtro y se implementa con `ILIKE`? ¿Cómo se evitan llamadas ambiguas entre sobrecargas?

- **Filtro por marca.** `VehicleDao.search(String text, String make, VehicleStatus status, VehicleType type)` filtra por marca con `ILIKE` sobre `make` cuando `make` no es nulo/vacío. `VehicleRepository` delega ambas versiones (`search(text, make, status)` y `search(text, make, status, type)`). `VehicleService` expone ambas sobrecargas y además `searchVehiclesByType(text, status, type)`.
- **Evitar ambigüedad.** Con dos sobrecargas de **3 argumentos** donde ambos enums podían ser nulos, llamadas como `searchVehicles(text, null, null)` eran ambiguas para el compilador. La solución fue **renombrar** la búsqueda con filtro solo por `type` a `searchVehiclesByType(text, status, type)`, dejando `searchVehicles(text, make, status)` para el caso con marca.
- **Marcas dinámicas.** `findMakes()` devuelve marcas distintas ordenadas alfabéticamente para poblar el combo del filtro sin hardcodear valores.

*Trazabilidad: GRASP Controller/Indirection -> Service delega en Repository → DAO; SOLID OCP/claridad -> nombres explícitos evitan ambigüedad.*

## 5. ¿Por qué `deleteVehicle(int)` es baja lógica y delega en `deactivateVehicle(int)`? ¿Qué diferencia hay con el DAO?

La guía establece **decisión D-02**: las entidades con historial se dan de **baja lógica** (cambiar estado a `INACTIVE`), no se eliminan físicamente.

- **Service.** `VehicleService.deleteVehicle(int id)` delega en `deactivateVehicle(id)`. `deactivateVehicle` actualiza el estado del vehículo a `INACTIVE` (manteniendo su historial de órdenes, etc.). `countInServiceVehicles()` queda **deprecated** a favor de `countInService()`.
- **DAO/Repository.** Siguen el patrón del proyecto: `CrudOperations.delete` puede representar la baja lógica según la entidad (en vehículos se traduce a desactivación). Lo importante es que **no se borra la fila**.
- **UI.** En la lista el botón «Dar de baja» se deshabilita si ya está `INACTIVE` (previo a confirmar). En detalle también se ofrece dar de baja.

*Trazabilidad: GRASP Information Expert / SOLID SRP -> regla de baja lógica en `VehicleService`; decisión D-02.*

## 6. ¿Cómo se estructura el módulo en capas y cómo se cablea la navegación?

**Flujo:** `Controller (JavaFX) → VehicleService → VehicleRepository → VehicleDao → DatabaseConnection`

- **DAO:** Implementa `CrudOperations<Vehicle>`, consultas específicas (`search`, `countByCustomer`, `findMakes`), conteos.
- **Repository:** Fachada/delegación, el Service **nunca** llama al DAO directamente.
- **Service:** Reglas de negocio (validaciones, conteos, `resolveAll` para completar propietario en las filas, `updateMileage` con validación, búsquedas con filtros).
- **Controllers:** `VehiclesController` (lista con KPI/filtros/tabla paginada), `VehicleFormController` (base común) + `VehicleNewController`/`VehicleEditController`, `VehicleDetailController` (ficha + otros vehículos del propietario). Todos implementan `NavigatorAware` cuando necesitan navegación; los formularios/detalle que reciben parámetro implementan `ParameterizedController`.
- **Navegación:** `View` define `VEHICLES`, `VEHICLE_NEW`, `VEHICLE_EDIT`, `VEHICLE_DETAIL`. `ViewNavigator` gestiona historial. `Sidebar` ya incluye la sección Vehículos.
- **Cableado:** `AppContext` crea `VehicleDao`, `VehicleRepository`, `VehicleService` (inyectando `CustomerRepository` para completar propietarios/conteos relacionados).
- **UI reutilizable:** `PageHeader`, `KpiCard`, `FilterBar`, `PagedTable`, `StatusBadge` y `VehicleLabels` para textos/formatos.

*Trazabilidad: GRASP Controller/Indirection/Low Coupling -> flujo por capas; GRASP Pure Fabrication -> componentes reutilizables; SOLID DIP -> `VehicleService` depende de `VehicleRepository`.*

## 7. ¿Por qué el formulario construye la UI por código y los FXML son mínimos? ¿Cómo se evita el bucle infinito al filtrar por marca?

- **FXML mínimo.** Cada pantalla usa un FXML con un `VBox` raíz (`fx:id="root"`) y el **contenido se construye en el controlador** (`buildForm()`, `buildScreen()`, etc.). Esto coincide con el patrón usado en `users-roles.fxml`/`UsersRolesController` y facilita reutilizar componentes (`PageHeader`, `GridPane`, etc.).
- **Bucle infinito (bug detectado y corregido).** `VehiclesController` actualiza el `ComboBox` de marcas en `refreshMakes()` llamando a `setItems()` y `setValue()`. Esos cambios disparan el listener del filtro (`valueProperty()`) que llama a `reload()`, creando recursión infinita → congelamiento de la pantalla. La corrección añade `suppressReload` (guard) para ignorar las actualizaciones **programáticas** del combo durante `refreshMakes()`, restableciéndolo en `finally`. Commit `657cd75`.
- **Carga diferida.** Se usa `Platform.runLater(this::reload)` / `Platform.runLater(this::loadOwners)` para no bloquear la carga del FXML.

*Trazabilidad: GRASP Controller/Creator -> controladores construyen su vista con componentes reutilizables; corrección con *Protected Variations* (guard) para evitar re-entrada entre listeners.*
