package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.model.UserStatus;
import com.autogest.util.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link UserService} con un repositorio falso, sin base de datos.
 */
class UserServiceTest {

    private static final String PASSWORD = "Clave1234";

    private FakeUserRepository repository;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = new FakeUserRepository();
        service = new UserService(repository);
    }

    @Test
    void createUserHashesPasswordAndDerivesUsername() {
        User created = service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        assertTrue(created.getId() > 0);
        assertEquals("ana.lopez", created.getUsername());
        assertEquals(UserStatus.ACTIVE, created.getStatus());
        assertNotEquals(PASSWORD, created.getPasswordHash());
        assertTrue(PasswordHasher.verify(PASSWORD, created.getPasswordHash()));
    }

    @Test
    void createUserKeepsProvidedUsername() {
        User user = newUser("ana.lopez@autogest.com", Role.RECEPTIONIST);
        user.setUsername("a.lopez");

        assertEquals("a.lopez", service.createUser(user, PASSWORD).getUsername());
    }

    @Test
    void createUserDerivesUniqueUsernameWhenTaken() {
        service.createUser(newUser("ana.lopez@uno.com", Role.RECEPTIONIST), PASSWORD);
        User second = service.createUser(newUser("ana.lopez@dos.com", Role.RECEPTIONIST), PASSWORD);

        assertEquals("ana.lopez1", second.getUsername());
    }

    @Test
    void createMechanicKeepsSpecialty() {
        User user = newUser("carlos.martinez@autogest.com", Role.MECHANIC);
        user.setSpecialty("Frenos");

        assertEquals("Frenos", service.createUser(user, PASSWORD).getSpecialty());
    }

    @Test
    void createNonMechanicClearsSpecialty() {
        User user = newUser("laura.rodriguez@autogest.com", Role.RECEPTIONIST);
        user.setSpecialty("Frenos");

        assertNull(service.createUser(user, PASSWORD).getSpecialty());
    }

    @Test
    void createUserRejectsMissingNames() {
        User user = newUser("ana.lopez@autogest.com", Role.RECEPTIONIST);
        user.setFirstName("  ");

        assertThrows(ValidationException.class, () -> service.createUser(user, PASSWORD));
    }

    @Test
    void createUserRejectsInvalidEmail() {
        assertThrows(ValidationException.class,
                () -> service.createUser(newUser("correo-invalido", Role.RECEPTIONIST), PASSWORD));
    }

    @Test
    void createUserRejectsDuplicateEmail() {
        service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        assertThrows(ValidationException.class,
                () -> service.createUser(newUser("ANA.LOPEZ@autogest.com", Role.MECHANIC), PASSWORD));
    }

    @Test
    void createUserRejectsWeakPassword() {
        User user = newUser("ana.lopez@autogest.com", Role.RECEPTIONIST);

        assertThrows(ValidationException.class, () -> service.createUser(user, "corta1"));
        assertThrows(ValidationException.class, () -> service.createUser(user, "sololetras"));
        assertThrows(ValidationException.class, () -> service.createUser(user, "12345678"));
    }

    @Test
    void createUserRejectsNullRole() {
        assertThrows(ValidationException.class,
                () -> service.createUser(newUser("ana.lopez@autogest.com", null), PASSWORD));
    }

    @Test
    void updateUserRejectsDuplicateEmailOfAnotherUser() {
        service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);
        User other = service.createUser(newUser("laura.rodriguez@autogest.com", Role.MECHANIC), PASSWORD);

        other.setEmail("ana.lopez@autogest.com");
        assertThrows(ValidationException.class, () -> service.updateUser(other));
    }

    @Test
    void updateUserKeepsPasswordAndUsername() {
        User created = service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);
        String hash = created.getPasswordHash();
        created.setFirstName("Ana María");

        User updated = service.updateUser(created);

        assertEquals("Ana María", updated.getFirstName());
        assertEquals(hash, updated.getPasswordHash());
        assertEquals("ana.lopez", updated.getUsername());
    }

    @Test
    void updateUserKeepsLastAccess() {
        User created = service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);
        java.time.LocalDateTime lastAccess = java.time.LocalDateTime.of(2026, 11, 11, 1, 3);
        repository.findById(created.getId()).setLastAccess(lastAccess);

        // Como en «Perfil y cuenta»: un objeto nuevo solo con los campos editables.
        User edited = newUser("ana.lopez@autogest.com", Role.RECEPTIONIST);
        edited.setId(created.getId());
        edited.setFirstName("Ana María");
        service.updateUser(edited);

        assertEquals(lastAccess, repository.findById(created.getId()).getLastAccess());
    }

    @Test
    void updateUserOfMissingUserThrowsNotFound() {
        User ghost = newUser("nadie@autogest.com", Role.RECEPTIONIST);
        ghost.setId(999);

        assertThrows(NotFoundException.class, () -> service.updateUser(ghost));
    }

    @Test
    void changeStatusDeactivatesAndReactivates() {
        User created = service.createUser(newUser("carlos.martinez@autogest.com", Role.MECHANIC), PASSWORD);

        service.changeStatus(created.getId(), UserStatus.INACTIVE);
        assertEquals(UserStatus.INACTIVE, repository.findById(created.getId()).getStatus());

        service.changeStatus(created.getId(), UserStatus.ACTIVE);
        assertEquals(UserStatus.ACTIVE, repository.findById(created.getId()).getStatus());
    }

    @Test
    void changeStatusRefusesToDeactivateLastActiveAdministrator() {
        User admin = service.createUser(newUser("jose.garcia@autogest.com", Role.ADMINISTRATOR), PASSWORD);

        assertThrows(ValidationException.class,
                () -> service.changeStatus(admin.getId(), UserStatus.INACTIVE));
        assertEquals(UserStatus.ACTIVE, repository.findById(admin.getId()).getStatus());
    }

    @Test
    void changeStatusAllowsDeactivatingAdministratorWhenAnotherExists() {
        service.createUser(newUser("jose.garcia@autogest.com", Role.ADMINISTRATOR), PASSWORD);
        User second = service.createUser(newUser("andres.gomez@autogest.com", Role.ADMINISTRATOR), PASSWORD);

        service.changeStatus(second.getId(), UserStatus.INACTIVE);

        assertEquals(UserStatus.INACTIVE, repository.findById(second.getId()).getStatus());
    }

    @Test
    void findActiveMechanicsReturnsOnlyActiveMechanics() {
        User activeMechanic = service.createUser(
                newUser("carlos.martinez@autogest.com", Role.MECHANIC), PASSWORD);
        User inactiveMechanic = service.createUser(
                newUser("andres.gomez@autogest.com", Role.MECHANIC), PASSWORD);
        service.changeStatus(inactiveMechanic.getId(), UserStatus.INACTIVE);
        service.createUser(newUser("laura.rodriguez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        List<User> mechanics = service.findActiveMechanics();

        assertEquals(1, mechanics.size());
        assertEquals(activeMechanic.getId(), mechanics.get(0).getId());
    }

    @Test
    void searchUsersFiltersByRoleStatusAndText() {
        service.createUser(newUser("carlos.martinez@autogest.com", Role.MECHANIC), PASSWORD);
        service.createUser(newUser("laura.rodriguez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        assertEquals(1, service.searchUsers("carlos", null, null).size());
        assertEquals(1, service.searchUsers(null, Role.RECEPTIONIST, null).size());
        assertEquals(2, service.searchUsers(null, null, UserStatus.ACTIVE).size());
        assertEquals(0, service.searchUsers(null, null, UserStatus.INACTIVE).size());
    }

    @Test
    void countsReflectTheStoredUsers() {
        service.createUser(newUser("jose.garcia@autogest.com", Role.ADMINISTRATOR), PASSWORD);
        User mechanic = service.createUser(newUser("carlos.martinez@autogest.com", Role.MECHANIC), PASSWORD);

        assertEquals(2, service.countUsers());
        assertEquals(2, service.countActiveUsers());
        assertEquals(1, service.countAdministrators());

        service.changeStatus(mechanic.getId(), UserStatus.INACTIVE);

        assertEquals(1, service.countActiveUsers());
        assertEquals(1, service.countAdministrators());
    }

    @Test
    void changePasswordStoresNewHash() {
        User user = service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        service.changePassword(user.getId(), PASSWORD, "NuevaClave99");

        String stored = repository.findById(user.getId()).getPasswordHash();
        assertTrue(PasswordHasher.verify("NuevaClave99", stored));
        assertTrue(!PasswordHasher.verify(PASSWORD, stored));
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        User user = service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        assertThrows(ValidationException.class,
                () -> service.changePassword(user.getId(), "Otra1234", "NuevaClave99"));
    }

    @Test
    void changePasswordRejectsWeakNewPassword() {
        User user = service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        assertThrows(ValidationException.class,
                () -> service.changePassword(user.getId(), PASSWORD, "corta"));
        assertThrows(ValidationException.class,
                () -> service.changePassword(user.getId(), PASSWORD, "sinnumeros"));
    }

    @Test
    void changePasswordRejectsSamePassword() {
        User user = service.createUser(newUser("ana.lopez@autogest.com", Role.RECEPTIONIST), PASSWORD);

        assertThrows(ValidationException.class,
                () -> service.changePassword(user.getId(), PASSWORD, PASSWORD));
    }

    @Test
    void changePasswordRejectsUnknownUser() {
        assertThrows(NotFoundException.class, () -> service.changePassword(999, PASSWORD, "NuevaClave99"));
    }

    private static User newUser(String email, Role role) {
        User user = new User();
        user.setFirstName("Ana");
        user.setLastName("López");
        user.setEmail(email);
        user.setRole(role);
        return user;
    }
}
