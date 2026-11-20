package com.autogest.service;

import com.autogest.exception.ValidationException;
import com.autogest.model.BillingSettings;
import com.autogest.repository.BillingSettingsRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Casos de uso de Configuración → Facturación: consultar, validar y guardar
 * la tasa de IVA, el plazo de pago, el prefijo de la numeración, los métodos
 * de pago y la nota de la factura.
 *
 * <p>Guarda en memoria la última configuración leída ({@link #current()}),
 * porque la pantalla de nueva factura la consulta mientras se escribe el
 * descuento. Se vuelve a leer al abrir la sección y al guardar.</p>
 *
 * <p>Principles: GRASP Controller, SRP, DIP.</p>
 */
public class BillingSettingsService {

    /** IVA máximo permitido, en porcentaje. */
    public static final BigDecimal MAX_TAX = new BigDecimal("100");
    /** Plazo de pago máximo, en días. */
    public static final int MAX_DUE_DAYS = 180;
    /** Largo máximo de la nota de la factura. */
    public static final int MAX_NOTE = 300;

    private static final Pattern PREFIX = Pattern.compile("^[A-Z0-9]{1,6}$");

    private final BillingSettingsRepository repository;
    private volatile BillingSettings cached;

    /**
     * @param repository almacenamiento de la configuración
     */
    public BillingSettingsService(BillingSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Lee la configuración guardada (y renueva la copia en memoria).
     *
     * @return configuración actual
     */
    public BillingSettings load() {
        BillingSettings settings = repository.load();
        cached = settings;
        return settings;
    }

    /**
     * Configuración en memoria; la primera vez la lee de la base de datos.
     * Si la lectura falla, usa los valores de fábrica para no frenar la
     * facturación.
     *
     * @return configuración vigente
     */
    public BillingSettings current() {
        BillingSettings settings = cached;
        if (settings != null) {
            return settings;
        }
        try {
            return load();
        } catch (RuntimeException e) {
            return BillingSettings.defaults();
        }
    }

    /**
     * Valida y guarda la configuración.
     *
     * @param settings datos del formulario
     * @return la configuración guardada (normalizada)
     * @throws ValidationException si algún valor no es válido
     */
    public BillingSettings update(BillingSettings settings) {
        if (settings == null) {
            throw new ValidationException("La configuración de facturación es obligatoria");
        }
        BigDecimal tax = settings.getTaxPercent();
        if (tax.signum() < 0 || tax.compareTo(MAX_TAX) > 0) {
            throw new ValidationException("El IVA debe estar entre 0 y 100 %");
        }
        if (tax.stripTrailingZeros().scale() > 2) {
            throw new ValidationException("El IVA admite hasta dos decimales");
        }
        if (settings.getDueDays() < 0 || settings.getDueDays() > MAX_DUE_DAYS) {
            throw new ValidationException("El plazo de pago debe estar entre 0 y " + MAX_DUE_DAYS + " días");
        }
        String prefix = settings.getNumberPrefix() == null ? "" : settings.getNumberPrefix().trim().toUpperCase();
        if (!PREFIX.matcher(prefix).matches()) {
            throw new ValidationException("El prefijo de la numeración lleva de 1 a 6 letras o números, sin espacios");
        }
        settings.setNumberPrefix(prefix);
        List<String> methods = new ArrayList<>();
        for (String code : BillingSettings.ALL_PAYMENT_METHODS) {
            if (settings.getPaymentMethods().contains(code)) {
                methods.add(code);
            }
        }
        if (methods.isEmpty()) {
            throw new ValidationException("Selecciona al menos un método de pago");
        }
        settings.setPaymentMethods(methods);
        String note = settings.getInvoiceNote() == null ? null : settings.getInvoiceNote().trim();
        if (note != null && note.length() > MAX_NOTE) {
            throw new ValidationException("La nota de la factura no puede superar " + MAX_NOTE + " caracteres");
        }
        settings.setInvoiceNote(note == null || note.isEmpty() ? null : note);
        repository.save(settings);
        cached = settings;
        return settings;
    }

    /**
     * Restaura y guarda los valores de fábrica.
     *
     * @return la configuración por defecto
     */
    public BillingSettings restoreDefaults() {
        return update(BillingSettings.defaults());
    }
}
