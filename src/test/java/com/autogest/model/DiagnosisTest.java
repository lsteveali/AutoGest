package com.autogest.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link Diagnosis#isComplete()}.
 */
class DiagnosisTest {

    private Diagnosis diagnosis(String description, String recommendations) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setDescription(description);
        diagnosis.setRecommendations(recommendations);
        return diagnosis;
    }

    @Test
    void completeWhenBothTextsHaveContent() {
        assertTrue(diagnosis("Desgaste de frenos", "Cambiar pastillas").isComplete());
    }

    @Test
    void incompleteWhenAnyTextIsMissingOrBlank() {
        assertFalse(diagnosis(null, "Cambiar pastillas").isComplete());
        assertFalse(diagnosis("Desgaste de frenos", "   ").isComplete());
        assertFalse(new Diagnosis().isComplete());
    }
}
