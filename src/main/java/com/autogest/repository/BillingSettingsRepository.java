package com.autogest.repository;

import com.autogest.dao.SettingsDao;
import com.autogest.model.BillingSettings;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Guarda la configuración de facturación en la tabla clave/valor
 * {@code app_settings} con las claves {@code billing.*}. Usa el
 * {@link SettingsDao} de Dev 1 sin cambiarlo. Un valor que falta o que no
 * se puede leer toma su valor por defecto, así una base sin configurar sigue
 * facturando como antes.
 *
 * <p>Principles: Pure Fabrication, Protected Variations (el formato clave/valor
 * no sale de aquí).</p>
 */
public class BillingSettingsRepository {

    static final String PREFIX = "billing.";
    static final String TAX = PREFIX + "taxPercent";
    static final String DUE_DAYS = PREFIX + "dueDays";
    static final String NUMBER_PREFIX = PREFIX + "numberPrefix";
    static final String METHODS = PREFIX + "paymentMethods";
    static final String NOTE = PREFIX + "invoiceNote";

    private final SettingsDao dao;

    /**
     * @param dao acceso a {@code app_settings}
     */
    public BillingSettingsRepository(SettingsDao dao) {
        this.dao = dao;
    }

    /**
     * @return la configuración guardada, con los valores por defecto donde falte
     */
    public BillingSettings load() {
        Map<String, String> values = dao.findByPrefix(PREFIX);
        BillingSettings settings = BillingSettings.defaults();
        String tax = values.get(TAX);
        if (tax != null) {
            try {
                settings.setTaxPercent(new BigDecimal(tax.trim()));
            } catch (NumberFormatException ignored) {
                // Valor dañado: queda el de fábrica.
            }
        }
        String dueDays = values.get(DUE_DAYS);
        if (dueDays != null) {
            try {
                settings.setDueDays(Integer.parseInt(dueDays.trim()));
            } catch (NumberFormatException ignored) {
                // Valor dañado: queda el de fábrica.
            }
        }
        String prefix = values.get(NUMBER_PREFIX);
        if (prefix != null && !prefix.isBlank()) {
            settings.setNumberPrefix(prefix.trim());
        }
        String methods = values.get(METHODS);
        if (methods != null) {
            List<String> codes = new ArrayList<>();
            for (String code : BillingSettings.ALL_PAYMENT_METHODS) {
                if (List.of(methods.split(",")).contains(code)) {
                    codes.add(code);
                }
            }
            if (!codes.isEmpty()) {
                settings.setPaymentMethods(codes);
            }
        }
        String note = values.get(NOTE);
        settings.setInvoiceNote(note == null || note.isBlank() ? null : note);
        return settings;
    }

    /**
     * Guarda toda la configuración en una transacción.
     *
     * @param settings configuración ya validada
     */
    public void save(BillingSettings settings) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(TAX, settings.getTaxPercent().stripTrailingZeros().toPlainString());
        values.put(DUE_DAYS, String.valueOf(settings.getDueDays()));
        values.put(NUMBER_PREFIX, settings.getNumberPrefix());
        values.put(METHODS, String.join(",", settings.getPaymentMethods()));
        values.put(NOTE, settings.getInvoiceNote() == null ? "" : settings.getInvoiceNote());
        dao.saveAll(values);
    }
}
