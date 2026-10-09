# Análisis - Base de presentación (Líder, Sprint 1)

Respuestas a las preguntas de análisis de la sección 12.5 de la guía. Cada
respuesta cierra con la línea de trazabilidad *Principio -> clase o método*
que alimenta la sección 2.6 del documento del proyecto.

## 1. ¿Por qué el Controller nunca llama al DAO ni al Repository?

Porque cada capa tiene una sola responsabilidad y solo puede depender de la
capa inmediatamente inferior. El Controller traduce eventos de pantalla en
llamadas a un Service; si necesitara datos, no sabe *cómo* se leen (SQL, JDBC,
relaciones) y no debería saberlo: eso es responsabilidad del Repository y del
DAO. Mantener esa separación evita que un cambio en la base de datos obligue a
tocar las pantallas, permite probar la lógica de negocio sin interfaz y sin
base de datos, y hace que el Controller sea reemplazable.

Además, la lógica no se duplica: las reglas y validaciones viven una sola vez
en el Service, así que todos los canales (pantalla o Asistente AutoGest) usan
el mismo código.

*Trazabilidad: GRASP Controller -> `MainController`, `ViewNavigator`; GRASP
Indirection -> `ViewNavigator` (único que cambia de pantalla); SOLID SRP ->
separación `controller` / `service` / `repository` / `dao`; SOLID DIP ->
dependencias hacia abajo documentadas en `docs/architecture.md`.*

## 2. ¿Por qué AppContext es el único lugar que crea Services, Repositories y DAO?

Porque así las dependencias se construyen en un solo punto y el resto de las
clases las recibe "listas" por el constructor (decisión D-03). Esto da dos
ventajas: (1) no hay `new` dispersos que acoplen las clases entre sí, y cambiar
la implementación de una dependencia se hace en un solo archivo; (2) las
pruebas pueden sustituir la dependencia por una subclase falsa y pasar esa
instancia al constructor, sin base de datos ni configuración real.

En el Sprint 1 `AppContext` es el esqueleto: solo existe la instancia única y
los bloques marcados por rol. Cada integrante agrega su bloque
(`CustomerDao -> CustomerRepository -> CustomerService`, etc.) sin tocar los
demás.

*Trazabilidad: SOLID DIP -> `config/AppContext.getInstance()`; GRASP Low
Coupling -> constructores de `ViewNavigator`, `Sidebar`, `Topbar`; GRASP Pure
Fabrication -> `AppContext`.*

## 3. ¿Qué duplicación evitan los componentes reutilizables?

Evitan repetir en cada pantalla la estructura de tarjetas, tablas, insignias,
cabeceras y diálogos, y —más importante— repetir los colores, tamaños y
espaciados. Todas las listas usan `PagedTable`, todas las cabeceras `PageHeader`
y todos los estados `StatusBadge`, así que la apariencia es idéntica y un cambio
de diseño (por ejemplo el color primario) se hace una sola vez en
`autogest.css`.

Si cada pantalla tuviera su propia tabla y sus propios colores habría
inconsistencias visuales, el mismo arreglo se repetiría muchas veces, y un
cambio de diseño obligaría a editar cada FXML. Con los componentes, la
apariencia vive en las clases de `controller/components` y en las clases CSS
globales.

*Trazabilidad: GRASP Pure Fabrication -> `PagedTable`, `PageHeader`,
`KpiCard`, `StatusBadge`, `FilterBar`, `ConfirmDialog`, `ErrorDialog`; SOLID
SRP -> cada componente hace una sola cosa; GRASP Protected Variations ->
`autogest.css` centraliza colores y estilos; SOLID OCP -> un componente nuevo
se agrega sin modificar los existentes.*
