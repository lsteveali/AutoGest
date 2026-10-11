package com.autogest.model;

import java.time.LocalDate;

/**
 * Cliente del taller: propietario de uno o varios vehículos que recibe servicios.
 *
 * <p>Es una entidad independiente de {@code User}: un cliente no es un usuario
 * interno del sistema, por eso no existe una superclase {@code Person}.
 * No se borra: se da de baja cambiando su estado (baja lógica).</p>
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class Customer {

    private int id;
    private DocumentType documentType;
    private String documentNumber;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String city;
    private String address;
    private String notes;
    private boolean acceptsPromotions;
    private CustomerStatus status;
    private LocalDate registrationDate;

    /**
     * Crea un cliente vacío, con {@code id} 0 y estado {@link CustomerStatus#ACTIVE}.
     */
    public Customer() {
        this.status = CustomerStatus.ACTIVE;
    }

    /**
     * Devuelve los nombres y apellidos del cliente separados por un espacio.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return nombre completo
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Indica si el cliente está activo y puede recibir nuevas órdenes.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si el estado es {@code ACTIVE}
     */
    public boolean isActive() {
        return status == CustomerStatus.ACTIVE;
    }

    /**
     * Da de baja al cliente cambiando su estado a {@code INACTIVE}; no borra su historial.
     *
     * <p>Principles: Information Expert.</p>
     */
    public void deactivate() {
        this.status = CustomerStatus.INACTIVE;
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el tipo de documento. */
    public DocumentType getDocumentType() {
        return documentType;
    }

    /** Asigna el tipo de documento. */
    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    /** Devuelve el número de documento. */
    public String getDocumentNumber() {
        return documentNumber;
    }

    /** Asigna el número de documento. */
    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
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

    /** Devuelve el teléfono de contacto. */
    public String getPhone() {
        return phone;
    }

    /** Asigna el teléfono de contacto. */
    public void setPhone(String phone) {
        this.phone = phone;
    }

    /** Devuelve el correo electrónico. */
    public String getEmail() {
        return email;
    }

    /** Asigna el correo electrónico. */
    public void setEmail(String email) {
        this.email = email;
    }

    /** Devuelve la ciudad de residencia. */
    public String getCity() {
        return city;
    }

    /** Asigna la ciudad de residencia. */
    public void setCity(String city) {
        this.city = city;
    }

    /** Devuelve la dirección. */
    public String getAddress() {
        return address;
    }

    /** Asigna la dirección. */
    public void setAddress(String address) {
        this.address = address;
    }

    /** Devuelve las notas adicionales. */
    public String getNotes() {
        return notes;
    }

    /** Asigna las notas adicionales. */
    public void setNotes(String notes) {
        this.notes = notes;
    }

    /** Indica si el cliente acepta recibir notificaciones y promociones por correo. */
    public boolean isAcceptsPromotions() {
        return acceptsPromotions;
    }

    /** Asigna si el cliente acepta recibir notificaciones y promociones por correo. */
    public void setAcceptsPromotions(boolean acceptsPromotions) {
        this.acceptsPromotions = acceptsPromotions;
    }

    /** Devuelve el estado del cliente. */
    public CustomerStatus getStatus() {
        return status;
    }

    /** Asigna el estado del cliente. */
    public void setStatus(CustomerStatus status) {
        this.status = status;
    }

    /** Devuelve la fecha de registro. */
    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    /** Asigna la fecha de registro. */
    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }
}
