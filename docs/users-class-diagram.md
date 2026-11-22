# Diagrama de clases del módulo Usuarios y Roles

**Autor:** Leonarth Steve Ali Camargo (Líder) · **Sprint 2** · AutoGest

El diagrama muestra las clases del módulo Usuarios/Roles en el flujo `Controller → Service → Repository → DAO`, junto a autenticación, sesión, permisos y su relación con `Role`/`UserStatus`.

```mermaid
classDiagram
    direction LR

    class CrudOperations~T~ {
        <<interface>>
        +create(T entity) T
        +findById(int id) T
        +findAll() List~T~
        +update(T entity) boolean
        +delete(int id) boolean
    }

    class Role {
        <<enumeration>>
        ADMINISTRATOR
        RECEPTIONIST
        MECHANIC
        SUPERVISOR
    }

    class UserStatus {
        <<enumeration>>
        ACTIVE
        INACTIVE
    }

    class User {
        -int id
        -String firstName
        -String lastName
        -String email
        -String username
        -String passwordHash
        -Role role
        -UserStatus status
        -String specialty
        -LocalDateTime lastAccess
        +getFullName() String
        +hasRole(Role role) boolean
        +validatePassword(String enteredPassword) boolean
    }

    class PasswordHasher {
        <<utility>>
        +hash(String plainPassword) String
        +verify(String plainPassword, String stored) boolean
    }

    class UserDao {
        -DatabaseConnection connection
        +findByEmail(String email) User
        +findByUsername(String username) User
        +search(String text, Role role, UserStatus status) List~User~
        +count() int
        +countByStatus(UserStatus status) int
        +countAdministrators() int
    }

    class UserRepository {
        -UserDao dao
        +create(User user) User
        +findById(int id) User
        +findAll() List~User~
        +update(User user) boolean
        +delete(int id) boolean
        +findByEmail(String email) User
        +findByUsername(String username) User
        +search(String text, Role role, UserStatus status) List~User~
        +count() int
        +countByStatus(UserStatus status) int
        +countAdministrators() int
    }

    class AuthenticationService {
        -UserRepository userRepository
        +authenticate(String emailOrUsername, String plainPassword) User
    }

    class UserService {
        -UserRepository userRepository
        +registerUser(User user) User
        +updateUser(User user) User
        +searchUsers(String text, Role role, UserStatus status) List~User~
        +getDetail(int id) User
        +deactivateUser(int id) void
        +countUsers() int
        +countActiveUsers() int
        +countAdministrators() int
    }

    class Session {
        <<static>>
        -User currentUser
        +setCurrentUser(User user)$ void
        +getCurrentUser()$ User
        +logout()$ void
        +isAuthenticated()$ boolean
    }

    class Permissions {
        <<utility>>
        +canAccess(User user, String section)$ boolean
    }

    class LoginController {
        +initialize(URL location, ResourceBundle resources) void
    }

    class UsersRolesController {
        +setNavigator(ViewNavigator navigator) void
        +initialize(URL location, ResourceBundle resources) void
    }

    class UserLabels {
        <<utility>>
        +roleText(Role role) String
        +statusText(UserStatus status) String
    }

    class AppContext {
        +getUserService() UserService
        +getAuthenticationService() AuthenticationService
    }

    UserDao ..|> CrudOperations : implementa
    UserDao ..> User : lee y escribe
    UserRepository --> UserDao : delega en
    UserService --> UserRepository : depende de
    AuthenticationService --> UserRepository : depende de
    User --> Role : role
    User --> UserStatus : status
    User ..> PasswordHasher : valida contraseña
    LoginController ..> AuthenticationService : autentica
    LoginController ..> Session : establece sesión
    UsersRolesController ..> UserService : usa
    UsersRolesController ..> Permissions : filtra
    UsersRolesController ..> Session : lee usuario
    UsersRolesController ..> UserLabels : textos
    LoginController ..> UserLabels : textos
    AppContext ..> UserDao : crea
    AppContext ..> UserRepository : crea
    AppContext ..> UserService : crea
    AppContext ..> AuthenticationService : crea
```

## Relaciones principales

| Relación | Descripción |
|---|---|
| `User` — `Role` / `UserStatus` | El rol y el estado son atributos (sin jerarquía por rol). Los enums se almacenan como texto (`name()`). |
| `User.validatePassword()` → `PasswordHasher.verify()` | El hash PBKDF2-HMAC-SHA256, sal, iteraciones y comparación en tiempo constante viven fuera del modelo. |
| `AuthenticationService` vs `UserService` | Autenticación separada de gestión: `authenticate` valida credenciales/estado/actualiza `lastAccess`; `UserService` gestiona CRUD/búsqueda/conteos. |
| `Controller → Service → Repository → DAO` | Dependencias hacia abajo. Los controladores nunca acceden al DAO. |
| `Session` + `Permissions` | `Session` guarda el usuario autenticado; `Permissions` aplica la matriz por rol para filtrar la navegación (UI). |
| `UserDao` implementa `CrudOperations` | Acceso genérico; consultas específicas (`findByEmail`, `findByUsername`, conteos) se añaden aparte. |
| `AppContext` cablea las dependencias | Único lugar donde se crean `UserDao`, `UserRepository`, `UserService`, `AuthenticationService`. |
