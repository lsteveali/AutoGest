package com.autogest.model;

/**
 * Empresa que suministra repuestos al taller.
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class Supplier {

    private int id;
    private String name;
    private String phone;
    private String email;

    /**
     * Crea un proveedor vacío, con {@code id} 0.
     */
    public Supplier() {
    }

    /**
     * Devuelve el teléfono y el correo en un solo texto, separados por « - ».
     * Si falta alguno de los dos muestra solo el otro; si faltan ambos, devuelve
     * un texto vacío.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return datos de contacto listos para mostrar
     */
    public String getContact() {
        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();
        if (hasPhone && hasEmail) {
            return phone + " - " + email;
        }
        if (hasPhone) {
            return phone;
        }
        return hasEmail ? email : "";
    }

    /** Devuelve el identificador único (0 mientras no se guarde). */
    public int getId() {
        return id;
    }

    /** Asigna el identificador único (0 mientras no se guarde). */
    public void setId(int id) {
        this.id = id;
    }

    /** Devuelve el nombre del proveedor. */
    public String getName() {
        return name;
    }

    /** Asigna el nombre del proveedor. */
    public void setName(String name) {
        this.name = name;
    }

    /** Devuelve el teléfono. */
    public String getPhone() {
        return phone;
    }

    /** Asigna el teléfono. */
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
}
