package com.autogest.model;

import com.autogest.util.PasswordHasher;
import java.time.LocalDateTime;

/**
 * Cuenta de quien inicia sesión en AutoGest.
 *
 * <p>Sus permisos dependen del atributo {@code role}; no existen subclases por
 * rol. El responsable de una orden de servicio es un {@code User} con rol
 * {@code MECHANIC} (el Service valida ese rol, no el modelo). La contraseña se
 * guarda solo como hash.</p>
 *
 * <p>Principles: Information Expert, Low Coupling, High Cohesion.</p>
 */
public class User {

    private int id;
    private String firstName;
    private String lastName;
    private String email;
    private String username;
    private String passwordHash;
    private Role role;
    private UserStatus status;
    private String specialty;
    private LocalDateTime lastAccess;

    /**
     * Crea un usuario vacío, con {@code id} 0 y estado {@link UserStatus#ACTIVE}.
     */
    public User() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Devuelve los nombres y apellidos del usuario separados por un espacio.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return nombre completo
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Verifica si el usuario tiene el rol indicado.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @param role rol a comprobar
     * @return {@code true} si coincide con el rol del usuario
     */
    public boolean hasRole(Role role) {
        return this.role == role;
    }

    /**
     * Compara la contraseña ingresada con el hash guardado.
     *
     * <p>El cálculo del hash lo hace {@link PasswordHasher}; el usuario solo
     * sabe cuál es su hash.</p>
     *
     * <p>Principles: Information Expert, High Cohesion.</p>
     *
     * @param enteredPassword contraseña en texto plano escrita por quien inicia sesión
     * @return {@code true} si coincide con el hash guardado
     */
    public boolean validatePassword(String enteredPassword) {
        return PasswordHasher.verify(enteredPassword, passwordHash);
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve los nombres. */
    public String getFirstName() {
        return firstName;
    }

    /** Asigna los nombres. */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** Devuelve los apellidos. */
    public String getLastName() {
        return lastName;
    }

    /** Asigna los apellidos. */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /** Devuelve el correo electrónico. */
    public String getEmail() {
        return email;
    }

    /** Asigna el correo electrónico. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Devuelve el nombre de usuario. */
    public String getUsername() {
        return username;
    }

    /** Asigna el nombre de usuario. */
    public void setUsername(String username) {
        this.username = username;
    }

    /** Devuelve el hash de la contraseña (nunca el texto plano). */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** Asigna el hash de la contraseña (nunca el texto plano). */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /** Devuelve el rol del usuario. */
    public Role getRole() {
        return role;
    }

    /** Asigna el rol del usuario. */
    public void setRole(Role role) {
        this.role = role;
    }

    /** Devuelve el estado de la cuenta. */
    public UserStatus getStatus() {
        return status;
    }

    /** Asigna el estado de la cuenta. */
    public void setStatus(UserStatus status) {
        this.status = status;
    }

    /** Devuelve la especialidad (opcional; se usa con el rol MECHANIC). */
    public String getSpecialty() {
        return specialty;
    }

    /** Asigna la especialidad (opcional; se usa con el rol MECHANIC). */
    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    /** Devuelve la fecha y hora del último acceso. */
    public LocalDateTime getLastAccess() {
        return lastAccess;
    }

    /** Asigna la fecha y hora del último acceso. */
    public void setLastAccess(LocalDateTime lastAccess) {
        this.lastAccess = lastAccess;
    }
}

