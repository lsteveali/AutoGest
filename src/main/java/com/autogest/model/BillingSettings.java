package com.autogest.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parámetros de facturación del taller (Configuración → Facturación): tasa de
 * IVA, plazo de pago, prefijo de la numeración, métodos de pago aceptados y
 * una nota que se imprime en la factura.
 *
 * <p>Los métodos de pago son los códigos que ya guarda {@code invoices}
 * ({@code CASH}, {@code CARD}, {@code TRANSFER}, {@code OTHER}).</p>
 *
 * <p>Principles: Information Expert (sabe convertir el porcentaje en tasa).</p>
 */
public class BillingSettings {

    /** Prefijo de la numeración por defecto: {@code FAC-2026-001}. */
    public static final String DEFAULT_PREFIX = "FAC";
    /** Plazo de pago por defecto, en días (mockup: emisión 02/10, vencimiento 16/10). */
    public static final int DEFAULT_DUE_DAYS = 14;
    /** Métodos de pago que conoce AutoGest, en el orden en que se muestran. */
    public static final List<String> ALL_PAYMENT_METHODS = List.of("CASH", "CARD", "TRANSFER", "OTHER");

    private BigDecimal taxPercent = BigDecimal.ZERO;
    private int dueDays = DEFAULT_DUE_DAYS;
    private String numberPrefix = DEFAULT_PREFIX;
    private List<String> paymentMethods = new ArrayList<>(ALL_PAYMENT_METHODS);
    private String invoiceNote;

    /**
     * Valores de fábrica: IVA 0 %, 14 días, prefijo «FAC», todos los métodos
     * de pago y sin nota (los mismos que usaba AutoGest antes de esta sección).
     *
     * @return configuración nueva con los valores por defecto
     */
    public static BillingSettings defaults() {
        return new BillingSettings();
    }

    /** Tasa de IVA como fracción: 19 % → 0.19. */
    public BigDecimal getTaxRate() {
        return taxPercent.movePointLeft(2);
    }

    /** Tasa de IVA en porcentaje (19 = 19 %). */
    public BigDecimal getTaxPercent() {
        return taxPercent;
    }

    public void setTaxPercent(BigDecimal taxPercent) {
        this.taxPercent = taxPercent == null ? BigDecimal.ZERO : taxPercent;
    }

    /** Días entre la emisión y el vencimiento. */
    public int getDueDays() {
        return dueDays;
    }

    public void setDueDays(int dueDays) {
        this.dueDays = dueDays;
    }

    /** Prefijo del número de factura ({@code FAC} en {@code FAC-2026-001}). */
    public String getNumberPrefix() {
        return numberPrefix;
    }

    public void setNumberPrefix(String numberPrefix) {
        this.numberPrefix = numberPrefix;
    }

    /** Códigos de los métodos de pago aceptados, en el orden de {@link #ALL_PAYMENT_METHODS}. */
    public List<String> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<String> paymentMethods) {
        this.paymentMethods = paymentMethods == null ? new ArrayList<>() : new ArrayList<>(paymentMethods);
    }

    /** Nota que se imprime al final de la factura (condiciones, cuenta para transferencias…); puede ser {@code null}. */
    public String getInvoiceNote() {
        return invoiceNote;
    }

    public void setInvoiceNote(String invoiceNote) {
        this.invoiceNote = invoiceNote;
    }
}
