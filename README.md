# AutoGest

Sistema de gestión integral para talleres automotrices. Aplicación de escritorio
desarrollada en Java + JavaFX que conecta clientes, vehículos, órdenes de servicio,
inventario, facturación y notificaciones en un solo sistema.

Proyecto de aula - Programación de Computadores III (SS462).

## Requisitos

- **Java 21** (o superior, compilando con `release 21`).
- **Maven 3.9+**.
- IntelliJ IDEA (recomendado).

## Cómo ejecutar

Desde la raíz del proyecto:

```bash
mvn javafx:run
```

Para compilar y ejecutar las pruebas:

```bash
mvn clean test
```

## Configuración de la base de datos

La conexión usa PostgreSQL alojado en Supabase, leído desde
`application.local.properties` (ignorado por Git).

1. Copia `application.example.properties` a `application.local.properties`.
2. Completa `db.url`, `db.user` y `db.password` con las credenciales del equipo.
3. **Nunca** subas `application.local.properties` al repositorio.

## Estructura del proyecto

```
src/main/java/com/autogest
  AutoGestApplication.java   Punto de entrada JavaFX
  config/                    DatabaseConnection, AppContext (wiring)
  controller/                Controllers, navegación y componentes reutilizables
    components/              Sidebar, Topbar, KpiCard, StatusBadge, PagedTable ...
  model/                     Entidades, enumeraciones e interfaces de dominio
  service/                   Lógica de negocio
  repository/                Acceso a datos organizado
  dao/                       Acceso técnico (SQL con PreparedStatement)
  exception/                 Excepciones personalizadas
  util/                      Formatter, PasswordHasher, Session

src/main/resources
  fxml/    css/ (css/fonts/)    images/    icons/

src/test/java/com/autogest     Pruebas JUnit 5
docs/                          Análisis y diagramas (Mermaid)
```

### Flujo entre capas

```
Controller (JavaFX) -> Service (negocio) -> Repository (datos organizados) -> DAO (SQL) -> Supabase/PostgreSQL
model: entidades compartidas por todas las capas
```

## Flujo de trabajo Git

```
main      versiones estables y presentables
  ^
develop   integración del trabajo diario
  ^
feature/<nombre>   una rama por módulo o tarea grande
```

- Prohibido hacer commits directos a `main` o `develop`, y usar `git push --force`.
- Cada integrante trabaja en su rama `feature/...` creada desde `develop`.
- Commits atómicos con Conventional Commits: tipo y ámbito en inglés, **descripción en español**
  (ej. `feat(customers): agrega formulario de nuevo cliente`).
- Cada commit se sube con `git push` inmediatamente después de crearlo.
- Al terminar una parte se abre un Pull Request hacia `develop`; se fusiona solo con
  aprobación de otro integrante y con `mvn clean test` correcto.

## Convenciones de código

- La **codificación** (clases, métodos, paquetes, columnas, archivos FXML/CSS) va en **inglés**.
- Los **comentarios, el Javadoc y los commits** van en **español**; los textos de pantalla también en español.
- Dinero con `BigDecimal` (escala 2, `RoundingMode.HALF_UP`); fechas con `LocalDate`/`LocalDateTime`.
- `javafx.*` solo en `controller`; `java.sql.*` solo en `dao` y `config`.
- Javadoc obligatorio en clases y métodos públicos, con la línea `Principles: ...`.

## Modelos 3D de la vitrina del login

La camioneta, el sedán y el deportivo se arman por código (`controller/components`). Los carros importados se convierten una sola vez a un formato compacto (`.agm`) con la herramienta del equipo:

```
java tools/ModelConverter.java <modelo.obj> tools/models/<carro>.rules src/main/resources/models/<carro>.agm
```

Las reglas (`tools/models/*.rules`) indican el largo real del carro, hacia dónde mira, qué piezas se descartan y qué papel cumple cada pieza; la que se marca como `paint` es la que cambia de color. Para agregarlo al catálogo basta una constante en `LoginAppearance.Vehicle` con su archivo.

## Créditos

- Modelo 3D «Mustang» por **danielmillsy22** ([free3d.com](https://free3d.com)), licencia de uso personal. Se usa solo en este proyecto académico; para publicar o vender AutoGest habría que reemplazarlo por un modelo con licencia libre. «Mustang» es una marca registrada de Ford Motor Company.
- Modelo 3D del deportivo («PG2.79») de **syn3d**, comprado por el equipo con licencia de uso comercial.
- Modelo 3D del clásico («Roadster») por **benzin** ([free3d.com](https://free3d.com)), licencia *Royalty Free*.
