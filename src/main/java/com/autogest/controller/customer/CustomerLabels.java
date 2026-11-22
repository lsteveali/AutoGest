package com.autogest.controller.customer;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.model.Catalogs.DocumentType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Textos y formatos que el módulo de clientes muestra en pantalla (en español):
 * estados, tipos de documento, documento con puntos de miles, teléfono, iniciales
 * del avatar, antigüedad del cliente y conteo de vehículos. Mantiene fuera del
 * modelo y de los controladores la traducción del dominio a texto, igual que
 * {@code UserLabels} en el módulo de usuarios.
 *
 * <p>Principles: Pure Fabrication, High Cohesion, SRP.</p>
 */
public final class CustomerLabels {

    /** Texto de las opciones «Todos» de los filtros. */
    public static final String FILTER_ALL = "Todos";

    /** Valor que se muestra mientras un dato todavía no existe (decisión D-12). */
    public static final String PLACEHOLDER = "-";

    /** Ciudades comunes de Colombia que ofrece el combo editable del formulario. */
    public static final List<String> COMMON_CITIES = List.of(
            "Valledupar", "Bogotá", "Medellín", "Cali",
            "Barranquilla", "Cartagena", "Bucaramanga", "Santa Marta");

    private static final DateTimeFormatter MONTH_YEAR =
            DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("es-CO"));

    private CustomerLabels() {
        // Clase de utilidad: no se instancia.
    }

    /**
     * Devuelve el texto del estado de un cliente.
     *
     * @param status estado a traducir; puede ser nulo
     * @return «Activo», «Inactivo» o cadena vacía
     */
    public static String statusText(CustomerStatus status) {
        if (status == null) {
            return "";
        }
        return status == CustomerStatus.ACTIVE ? "Activo" : "Inactivo";
    }

    /**
     * Convierte el texto de un estado (el que ve el filtro) de vuelta al valor del
     * dominio.
     *
     * @param text texto mostrado en pantalla; puede ser nulo o «Todos»
     * @return el estado correspondiente o {@code null} si no aplica ninguno (todos)
     */
    public static CustomerStatus statusFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        if ("Activo".equalsIgnoreCase(text.trim())) {
            return CustomerStatus.ACTIVE;
        }
        if ("Inactivo".equalsIgnoreCase(text.trim())) {
            return CustomerStatus.INACTIVE;
        }
        return null;
    }

    /**
     * Devuelve el tipo de insignia que corresponde al estado de un cliente.
     *
     * @param status estado a representar
     * @return {@code SUCCESS} para activo, {@code NEUTRAL} para inactivo
     */
    public static StatusBadge.BadgeType statusType(CustomerStatus status) {
        return status == CustomerStatus.ACTIVE
                ? StatusBadge.BadgeType.SUCCESS
                : StatusBadge.BadgeType.NEUTRAL;
    }

    /**
     * Devuelve el nombre completo de un tipo de documento, para el combo del
     * formulario.
     *
     * @param type tipo a traducir; puede ser nulo
     * @return por ejemplo «Cédula de ciudadanía» o cadena vacía
     */
    public static String documentTypeText(DocumentType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case CC -> "Cédula de ciudadanía";
            case CE -> "Cédula de extranjería";
            case NIT -> "NIT";
            case PASSPORT -> "Pasaporte";
        };
    }

    /**
     * Convierte el texto de un tipo de documento de vuelta al valor del dominio.
     *
     * @param text texto mostrado en el combo; puede ser nulo
     * @return el tipo correspondiente o {@code null} si no aplica ninguno
     */
    public static DocumentType documentTypeFromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (DocumentType type : DocumentType.values()) {
            if (documentTypeText(type).equalsIgnoreCase(text.trim())) {
                return type;
            }
        }
        return null;
    }

    /**
     * Devuelve el documento del cliente como se ve en las tablas, por ejemplo
     * {@code CC 1.234.567.890}. Solo {@code CC} y {@code CE} llevan puntos de miles;
     * {@code NIT} y pasaporte se muestran tal como se guardaron.
     *
     * @param customer cliente; puede ser nulo
     * @return texto del documento o cadena vacía si faltan datos
     */
    public static String documentText(Customer customer) {
        if (customer == null || customer.getDocumentType() == null || customer.getDocumentNumber() == null) {
            return "";
        }
        DocumentType type = customer.getDocumentType();
        String number = customer.getDocumentNumber();
        if ((type == DocumentType.CC || type == DocumentType.CE) && number.matches("\\d+")) {
            number = groupThousands(number);
        }
        return documentCode(type) + " " + number;
    }

    /**
     * Da formato a un teléfono guardado sin espacios: {@code 3001234567} pasa a
     * {@code 300 123 4567} y {@code +573001234567} a {@code +57 300 123 4567}. Los
     * demás formatos se muestran tal como están.
     *
     * @param phone teléfono guardado; puede ser nulo
     * @return teléfono listo para mostrar o cadena vacía
     */
    public static String phoneText(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String value = phone.trim();
        if (value.startsWith("+57") && value.substring(3).matches("\\d{10}")) {
            return "+57 " + groupTenDigits(value.substring(3));
        }
        if (value.matches("\\d{10}")) {
            return groupTenDigits(value);
        }
        return value;
    }

    /**
     * Calcula las iniciales de un cliente (primera letra del nombre y del
     * apellido) para el avatar.
     *
     * @param customer cliente; puede ser nulo
     * @return una o dos letras en mayúscula, o «?» si no hay datos
     */
    public static String initials(Customer customer) {
        if (customer == null) {
            return "?";
        }
        String initials = (initial(customer.getFirstName()) + initial(customer.getLastName())).toUpperCase(Locale.ROOT);
        return initials.isBlank() ? "?" : initials;
    }

    /**
     * Devuelve la antigüedad del cliente, por ejemplo «Cliente desde marzo de 2024».
     *
     * @param registrationDate fecha de registro; puede ser nula
     * @return texto de antigüedad o cadena vacía si no hay fecha
     */
    public static String memberSinceText(LocalDate registrationDate) {
        if (registrationDate == null) {
            return "";
        }
        return "Cliente desde " + MONTH_YEAR.format(registrationDate);
    }

    /**
     * Devuelve la preferencia de comunicación que se muestra en el detalle:
     * «Correo electrónico» si el cliente acepta notificaciones y promociones y
     * «Sin promociones» si no.
     *
     * @param customer cliente; puede ser nulo
     * @return texto de la preferencia o cadena vacía
     */
    public static String communicationPreferenceText(Customer customer) {
        if (customer == null) {
            return "";
        }
        return customer.isAcceptsPromotions() ? "Correo electrónico" : "Sin promociones";
    }

    /**
     * Devuelve el conteo de vehículos de un cliente, por ejemplo «1 vehículo» o
     * «2 vehículos».
     *
     * @param count cantidad de vehículos
     * @return texto con el conteo y la palabra en singular o plural
     */
    public static String vehicleCountText(int count) {
        return count == 1 ? "1 vehículo" : count + " vehículos";
    }

    /** Prefijo corto del tipo de documento usado en las tablas. */
    private static String documentCode(DocumentType type) {
        return switch (type) {
            case CC -> "CC";
            case CE -> "CE";
            case NIT -> "NIT";
            case PASSPORT -> "Pasaporte";
        };
    }

    /** Inserta un punto cada tres dígitos, contando desde la derecha. */
    private static String groupThousands(String digits) {
        StringBuilder grouped = new StringBuilder();
        int count = 0;
        for (int i = digits.length() - 1; i >= 0; i--) {
            if (count > 0 && count % 3 == 0) {
                grouped.append('.');
            }
            grouped.append(digits.charAt(i));
            count++;
        }
        return grouped.reverse().toString();
    }

    /** Separa diez dígitos como {@code 300 123 4567}. */
    private static String groupTenDigits(String digits) {
        return digits.substring(0, 3) + " " + digits.substring(3, 6) + " " + digits.substring(6);
    }

    /** Primera letra de un texto, o cadena vacía si no hay. */
    private static String initial(String value) {
        return value == null || value.isBlank() ? "" : value.trim().substring(0, 1);
    }
}
