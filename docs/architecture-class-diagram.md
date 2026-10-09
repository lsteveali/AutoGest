# Diagrama de clases - Navegación, componentes y sesión (Sprint 1)

Vista resumida de las clases de presentación de la base del Sprint 1. Se
centra en la navegación, los componentes reutilizables y el shell. `Session`,
`Permissions` y los servicios de usuario se completan en el Sprint 2.

```mermaid
classDiagram
    class AutoGestApplication {
        +start(Stage) void
        +main(String[]) void
    }

    class MainController {
        -StackPane contentArea
        -Sidebar sidebar
        -Topbar topbar
        -AssistantButton assistantButton
        -ViewNavigator navigator
        +initialize(URL, ResourceBundle) void
        +getNavigator() ViewNavigator
    }

    class View {
        <<enumeration>>
        HOME
        CUSTOMERS
        VEHICLES
        ORDERS
        SERVICES
        INVENTORY
        BILLING
        NOTIFICATIONS
        REPORTS
        SETTINGS
        USERS_ROLES
        ASSISTANT
        +getFxmlPath() String
        +getTitle() String
        +getSidebarSection() View
    }

    class ViewNavigator {
        -StackPane container
        -Deque~View~ history
        +navigateTo(View) void
        +navigateWithParameter(View, int) void
        +goBack() void
        +setOnViewChange(Consumer~View~) void
    }

    class ParameterizedController {
        <<interface>>
        +receiveParameter(int) void
    }

    class Sidebar {
        +setOnNavigate(Consumer~View~) void
        +setActiveItem(View) void
    }

    class Topbar {
        +setUser(String, String) void
    }

    class AssistantButton

    class PageHeader {
        +setPrimaryAction(String, Runnable) void
        +setBreadcrumbs(List~String~) void
    }

    class KpiCard {
        +setValue(String) void
        +setDetail(String) void
    }

    class StatusBadge {
        <<utility>>
        +of(String, BadgeType) Label
    }

    class PagedTable~T~ {
        +addColumn(String, Function~T,?~) void
        +setData(List~T~) void
        +setOnSelect(Consumer~T~) void
    }

    class FilterBar {
        +addSearch(String, Consumer~String~) void
        +addCombo(String, List~String~, Consumer~String~) void
    }

    class ConfirmDialog {
        <<utility>>
        +confirm(String, String) boolean
    }

    class ErrorDialog {
        <<utility>>
        +show(String) void
    }

    class IconSet {
        <<utility>>
        +icon(String) Path
    }

    class Formatter {
        <<utility>>
        +money(BigDecimal) String
        +date(LocalDate) String
        +timestamp(LocalDateTime) String
    }

    AutoGestApplication --> MainController : carga /fxml/main.fxml
    MainController --> ViewNavigator : crea y conecta
    MainController --> Sidebar
    MainController --> Topbar
    MainController --> AssistantButton
    MainController ..> View : navega
    ViewNavigator --> View : carga FXML
    ViewNavigator ..> ParameterizedController : inyecta id
    Sidebar ..> View : item activo
    AssistantButton ..> View : ASSISTANT
    PagedTable ..> StatusBadge : usa
    PageHeader ..> KpiCard : convive en pantallas
    FilterBar ..> PagedTable : filtra datos
    ConfirmDialog ..> ErrorDialog : alternativa
    Sidebar ..> IconSet : iconos
    Topbar ..> IconSet : iconos
    KpiCard ..> IconSet : iconos
```

## Notas de diseño

- **Indirection:** `ViewNavigator` es el único punto que cambia de pantalla;
  ninguna pantalla conoce a otra.
- **Pure Fabrication:** `Sidebar`, `Topbar`, `PagedTable`, `FilterBar`,
  `IconSet` y `Formatter` son clases de apoyo que mantienen limpias las
  pantallas y el modelo.
- **DIP / Low Coupling:** `Sidebar` y `ViewNavigator` se comunican por
  `Consumer`, no por referencias directas entre sí.
- **SRP:** cada componente hace una sola cosa; los diálogos y utilidades no
  contienen reglas de ningún módulo.
- En el Sprint 2 se agregan `Session`, `Permissions`, `AuthenticationService`,
  `UserService` y sus DAO/Repository en `AppContext`.
