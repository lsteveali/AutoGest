package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.model.UserStatus;
import com.autogest.repository.UserRepository;
import com.autogest.util.PasswordHasher;

import java.util.List;

/**
 * Reglas de negocio del módulo de usuarios: consulta, creación, edición y
 * cambio de estado. Valida los datos antes de guardarlos y guarda siempre el
 * hash de la contraseña (nunca el texto plano). No conoce SQL ni JavaFX.
 *
 * <p>Cada método de lectura delega en {@link UserRepository}; este servicio
 * concentra las validaciones que el controlador no debe repetir.</p>
 *
 * <p>Principles: GRASP Controller, Information Expert, SRP, DIP, High Cohesion,
 * Low Coupling.</p>
 */
public class UserService {

    private static final String EMAIL_PATTERN = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";
    /** Largo mínimo de una contraseña (público: lo muestra la pantalla Seguridad). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository repository;

    /**
     * Crea el servicio con el repositorio de usuarios inyectado.
     *
     * @param repository acceso a los usuarios
     */
    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    /**
     * Busca usuarios aplicando filtros opcionales. Un filtro {@code null} o en
     * blanco no se aplica.
     *
     * @param text   texto a buscar en nombre, apellido, correo o usuario
     * @param role   rol a filtrar (o {@code null} para todos)
     * @param status estado a filtrar (o {@code null} para todos)
     * @return lista de usuarios que cumplen los filtros
     */
    public List<User> searchUsers(String text, Role role, UserStatus status) {
        return repository.search(text, role, status);
    }

    /**
     * Crea un usuario activo con una clave inicial. Valida los nombres y
     * apellidos, el correo (formato y unicidad), el rol y la clave (mínimo 8
     * caracteres con letras y números); guarda el hash y deriva el nombre de
     * usuario a partir del correo si no viene dado.
     *
     * @param user            usuario con nombres, apellidos, correo, rol y especialidad
     * @param initialPassword clave inicial en texto plano
     * @return el usuario guardado con su id y su hash
     * @throws ValidationException si algún dato no cumple las reglas
     */
    public User createUser(User user, String initialPassword) {
        validateNames(user);
        validateEmail(user, 0);
        validateRole(user);
        validatePassword(initialPassword);

        user.setEmail(user.getEmail().trim().toLowerCase());
        if (isBlank(user.getUsername())) {
            user.setUsername(buildUsername(user.getEmail()));
        } else if (repository.findByUsername(user.getUsername()) != null) {
            throw new ValidationException("Ya existe un usuario con ese nombre de usuario.");
        }
        if (user.getRole() != Role.MECHANIC) {
            user.setSpecialty(null);
        }
        user.setPasswordHash(PasswordHasher.hash(initialPassword));
        user.setStatus(UserStatus.ACTIVE);
        return repository.create(user);
    }

    /**
     * Actualiza los datos editables de un usuario (nombres, apellidos, correo,
     * rol y especialidad). No cambia la contraseña.
     *
     * @param user usuario con el id y los nuevos valores
     * @return el usuario actualizado
     * @throws NotFoundException   si el usuario no existe
     * @throws ValidationException si algún dato no cumple las reglas
     */
    public User updateUser(User user) {
        User current = requireUser(user.getId());
        validateNames(user);
        validateEmail(user, user.getId());
        validateRole(user);
        user.setEmail(user.getEmail().trim().toLowerCase());
        if (user.getRole() != Role.MECHANIC) {
            user.setSpecialty(null);
        }
        // Se conservan los datos que no se editan desde este caso de uso.
        user.setUsername(current.getUsername());
        user.setPasswordHash(current.getPasswordHash());
        user.setStatus(current.getStatus());
        repository.update(user);
        return user;
    }

    /**
     * Cambia la contraseña de un usuario («Perfil y cuenta»). Exige la
     * contraseña actual, aplica las mismas reglas que una clave inicial y no
     * permite repetir la misma. Solo se guarda el hash.
     *
     * @param id              identificador del usuario
     * @param currentPassword contraseña actual en texto plano
     * @param newPassword     nueva contraseña en texto plano
     * @throws NotFoundException   si el usuario no existe
     * @throws ValidationException si la contraseña actual no coincide o la nueva no cumple las reglas
     */
    public void changePassword(int id, String currentPassword, String newPassword) {
        User user = requireUser(id);
        if (isBlank(currentPassword) || !PasswordHasher.verify(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("La contraseña actual no es correcta.");
        }
        validatePassword(newPassword);
        if (newPassword.equals(currentPassword)) {
            throw new ValidationException("La nueva contraseña debe ser distinta de la actual.");
        }
        user.setPasswordHash(PasswordHasher.hash(newPassword));
        repository.update(user);
    }

    /**
     * Cambia el estado de un usuario (activar o desactivar). No se puede
     * desactivar al último administrador activo, para no dejar el sistema sin
     * acceso administrativo.
     *
     * @param id     identificador del usuario
     * @param status nuevo estado
     * @throws NotFoundException   si el usuario no existe
     * @throws ValidationException si el estado es nulo o dejaría el sistema sin administradores
     */
    public void changeStatus(int id, UserStatus status) {
        if (status == null) {
            throw new ValidationException("Selecciona un estado.");
        }
        User user = requireUser(id);
        if (status == UserStatus.INACTIVE && user.hasRole(Role.ADMINISTRATOR)
                && user.getStatus() == UserStatus.ACTIVE && countAdministrators() <= 1) {
            throw new ValidationException("No se puede desactivar al último administrador activo.");
        }
        user.setStatus(status);
        repository.update(user);
    }

    /**
     * Devuelve los mecánicos activos (los usará el módulo de órdenes para
     * asignar el responsable).
     *
     * @return lista de usuarios ACTIVE con rol MECHANIC
     */
    public List<User> findActiveMechanics() {
        return repository.findByRoleAndStatus(Role.MECHANIC, UserStatus.ACTIVE);
    }

    /**
     * Cuenta todos los usuarios registrados.
     *
     * @return número total de usuarios
     */
    public int countUsers() {
        return repository.countAll();
    }

    /**
     * Cuenta los usuarios activos.
     *
     * @return número de usuarios ACTIVE
     */
    public int countActiveUsers() {
        return repository.countByStatus(UserStatus.ACTIVE);
    }

    /**
     * Cuenta los administradores activos.
     *
     * @return número de usuarios ADMINISTRATOR con estado ACTIVE
     */
    public int countAdministrators() {
        return repository.countByRoleAndStatus(Role.ADMINISTRATOR, UserStatus.ACTIVE);
    }

    /** Busca el usuario o lanza {@link NotFoundException}. */
    private User requireUser(int id) {
        User user = repository.findById(id);
        if (user == null) {
            throw new NotFoundException("No se encontró el usuario.");
        }
        return user;
    }

    /** Exige nombres y apellidos no vacíos. */
    private void validateNames(User user) {
        if (isBlank(user.getFirstName()) || isBlank(user.getLastName())) {
            throw new ValidationException("Los nombres y apellidos son obligatorios.");
        }
        user.setFirstName(user.getFirstName().trim());
        user.setLastName(user.getLastName().trim());
    }

    /** Exige un correo con formato válido y sin repetir. */
    private void validateEmail(User user, int currentId) {
        String email = user.getEmail();
        if (isBlank(email) || !email.trim().matches(EMAIL_PATTERN)) {
            throw new ValidationException("Ingresa un correo electrónico válido.");
        }
        User existing = repository.findByEmail(email.trim());
        if (existing != null && existing.getId() != currentId) {
            throw new ValidationException("Ya existe un usuario con ese correo electrónico.");
        }
    }

    /** Exige un rol seleccionado. */
    private void validateRole(User user) {
        if (user.getRole() == null) {
            throw new ValidationException("Selecciona un rol.");
        }
    }

    /** Exige una clave de mínimo 8 caracteres con al menos una letra y un número. */
    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new ValidationException(
                    "La clave debe tener al menos 8 caracteres, con letras y números.");
        }
    }

    /**
     * Deriva un nombre de usuario único a partir del correo (texto antes de la
     * arroba); si ya está en uso, le agrega un número.
     */
    private String buildUsername(String email) {
        String local = email.substring(0, email.indexOf('@')).replaceAll("[^a-z0-9._-]", "");
        if (local.isBlank()) {
            local = "usuario";
        }
        String candidate = local;
        int suffix = 1;
        while (repository.findByUsername(candidate) != null) {
            candidate = local + suffix++;
        }
        return candidate;
    }

    /** Indica si una cadena es nula o solo espacios. */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
