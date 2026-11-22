package com.autogest.model;

import java.time.LocalDate;

/**
 * Diagnóstico técnico y recomendaciones de una orden de servicio.
 *
 * <p>Principles: Information Expert, High Cohesion.</p>
 */
public class Diagnosis {

    private String description;
    private String recommendations;
    private LocalDate date;

    /**
     * Crea un diagnóstico vacío.
     */
    public Diagnosis() {
    }

    /**
     * Indica si el diagnóstico tiene descripción y recomendaciones (no vacías ni en blanco).
     *
     * <p>Principles: Information Expert.</p>
     *
     * @return {@code true} si ambos textos tienen contenido
     */
    public boolean isComplete() {
        return hasText(description) && hasText(recommendations);
    }

    /** Indica si un texto tiene contenido distinto de espacios. */
    private static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    /** Devuelve los hallazgos del mecánico. */
    public String getDescription() {
        return description;
    }

    /** Asigna los hallazgos del mecánico. */
    public void setDescription(String description) {
        this.description = description;
    }

    /** Devuelve los trabajos sugeridos. */
    public String getRecommendations() {
        return recommendations;
    }

    /** Asigna los trabajos sugeridos. */
    public void setRecommendations(String recommendations) {
        this.recommendations = recommendations;
    }

    /** Devuelve la fecha del diagnóstico. */
    public LocalDate getDate() {
        return date;
    }

    /** Asigna la fecha del diagnóstico. */
    public void setDate(LocalDate date) {
        this.date = date;
    }
}
