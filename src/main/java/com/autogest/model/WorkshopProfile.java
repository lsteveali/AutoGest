package com.autogest.model;

/**
 * Datos del taller (mockup Datos del taller V1): información general,
 * identidad, ubicación y contacto. Se usan en facturas, comunicaciones y
 * documentos. Hay un solo perfil por instalación.
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class WorkshopProfile {

    private String name;
    private String nit;
    private String phone;
    private String email;
    private String website;
    private String workshopType;
    private String commercialName;
    private String description;
    private String address;
    private String whatsapp;
    private String city;
    private String postalCode;
    private String department;
    private String country;
    private String mainEmail;
    private String mainPhone;
    private String businessHours;

    /** Crea un perfil vacío con el país por defecto (Colombia). */
    public WorkshopProfile() {
        this.country = "Colombia";
        this.workshopType = "Taller automotriz general";
    }

    /**
     * Nombre que se muestra en documentos: el comercial si existe, si no el legal.
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return nombre para mostrar (vacío si no hay ninguno)
     */
    public String getDisplayName() {
        if (commercialName != null && !commercialName.isBlank()) {
            return commercialName;
        }
        return name == null ? "" : name;
    }

    /**
     * Indica si están los datos mínimos para emitir documentos (nombre, NIT y correo).
     *
     * @return {@code true} si el perfil está completo
     */
    public boolean isComplete() {
        return hasText(name) && hasText(nit) && hasText(email);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getWorkshopType() {
        return workshopType;
    }

    public void setWorkshopType(String workshopType) {
        this.workshopType = workshopType;
    }

    public String getCommercialName() {
        return commercialName;
    }

    public void setCommercialName(String commercialName) {
        this.commercialName = commercialName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getMainEmail() {
        return mainEmail;
    }

    public void setMainEmail(String mainEmail) {
        this.mainEmail = mainEmail;
    }

    public String getMainPhone() {
        return mainPhone;
    }

    public void setMainPhone(String mainPhone) {
        this.mainPhone = mainPhone;
    }

    public String getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(String businessHours) {
        this.businessHours = businessHours;
    }
}
