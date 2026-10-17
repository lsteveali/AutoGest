package com.autogest.controller.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Customer;
import com.autogest.model.CustomerStatus;
import com.autogest.model.DocumentType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de los textos y formatos de {@link CustomerLabels}.
 */
class CustomerLabelsTest {

    private Customer customer(DocumentType type, String number) {
        Customer customer = new Customer();
        customer.setDocumentType(type);
        customer.setDocumentNumber(number);
        return customer;
    }

    // ---------- estado ----------

    @Test
    void statusTextIsInSpanish() {
        assertEquals("Activo", CustomerLabels.statusText(CustomerStatus.ACTIVE));
        assertEquals("Inactivo", CustomerLabels.statusText(CustomerStatus.INACTIVE));
        assertEquals("", CustomerLabels.statusText(null));
    }

    @Test
    void statusFromTextIsTheInverseAndAllMeansNull() {
        assertEquals(CustomerStatus.ACTIVE, CustomerLabels.statusFromText("Activo"));
        assertEquals(CustomerStatus.INACTIVE, CustomerLabels.statusFromText(" inactivo "));
        assertNull(CustomerLabels.statusFromText(CustomerLabels.FILTER_ALL));
        assertNull(CustomerLabels.statusFromText(null));
        assertNull(CustomerLabels.statusFromText(""));
    }

    @Test
    void statusBadgeIsGreenForActiveAndGreyForInactive() {
        assertEquals(StatusBadge.BadgeType.SUCCESS, CustomerLabels.statusType(CustomerStatus.ACTIVE));
        assertEquals(StatusBadge.BadgeType.NEUTRAL, CustomerLabels.statusType(CustomerStatus.INACTIVE));
    }

    // ---------- tipo de documento ----------

    @Test
    void documentTypeTextAndItsInverse() {
        for (DocumentType type : DocumentType.values()) {
            assertEquals(type, CustomerLabels.documentTypeFromText(CustomerLabels.documentTypeText(type)));
        }
        assertEquals("Cédula de ciudadanía", CustomerLabels.documentTypeText(DocumentType.CC));
        assertEquals("Pasaporte", CustomerLabels.documentTypeText(DocumentType.PASSPORT));
        assertEquals("", CustomerLabels.documentTypeText(null));
        assertNull(CustomerLabels.documentTypeFromText("otro"));
        assertNull(CustomerLabels.documentTypeFromText(null));
    }

    // ---------- documento ----------

    @Test
    void identityCardsShowThousandsSeparators() {
        assertEquals("CC 1.234.567.890", CustomerLabels.documentText(customer(DocumentType.CC, "1234567890")));
        assertEquals("CC 12.345", CustomerLabels.documentText(customer(DocumentType.CC, "12345")));
        assertEquals("CC 123.456", CustomerLabels.documentText(customer(DocumentType.CC, "123456")));
        assertEquals("CE 1.098.765.432", CustomerLabels.documentText(customer(DocumentType.CE, "1098765432")));
    }

    @Test
    void taxIdAndPassportAreShownAsStored() {
        assertEquals("NIT 900123456-7", CustomerLabels.documentText(customer(DocumentType.NIT, "900123456-7")));
        assertEquals("Pasaporte AB123456", CustomerLabels.documentText(customer(DocumentType.PASSPORT, "AB123456")));
    }

    @Test
    void documentTextIsEmptyWhenDataIsMissing() {
        assertEquals("", CustomerLabels.documentText(null));
        assertEquals("", CustomerLabels.documentText(customer(null, "1234567890")));
        assertEquals("", CustomerLabels.documentText(customer(DocumentType.CC, null)));
    }

    // ---------- teléfono ----------

    @Test
    void phoneIsGroupedAsThreeThreeFour() {
        assertEquals("300 123 4567", CustomerLabels.phoneText("3001234567"));
        assertEquals("+57 300 123 4567", CustomerLabels.phoneText("+573001234567"));
    }

    @Test
    void otherPhoneFormatsAreShownAsStored() {
        assertEquals("5851234", CustomerLabels.phoneText("5851234"));
        assertEquals("+571234567", CustomerLabels.phoneText("+571234567"));
        assertEquals("", CustomerLabels.phoneText(null));
        assertEquals("", CustomerLabels.phoneText("  "));
    }

    // ---------- avatar y antigüedad ----------

    @Test
    void initialsUseFirstLetterOfFirstAndLastName() {
        Customer customer = new Customer();
        customer.setFirstName("juan carlos");
        customer.setLastName("Pérez");

        assertEquals("JP", CustomerLabels.initials(customer));
    }

    @Test
    void initialsFallBackToQuestionMark() {
        assertEquals("?", CustomerLabels.initials(null));
        assertEquals("?", CustomerLabels.initials(new Customer()));
        Customer onlyFirst = new Customer();
        onlyFirst.setFirstName("Ana");
        assertEquals("A", CustomerLabels.initials(onlyFirst));
    }

    @Test
    void memberSinceShowsMonthNameAndYearInSpanish() {
        assertEquals("Cliente desde marzo de 2024", CustomerLabels.memberSinceText(LocalDate.of(2024, 3, 10)));
        assertEquals("Cliente desde octubre de 2026", CustomerLabels.memberSinceText(LocalDate.of(2026, 10, 12)));
        assertEquals("", CustomerLabels.memberSinceText(null));
    }

    // ---------- preferencia y vehículos ----------

    @Test
    void communicationPreferenceDependsOnPromotions() {
        Customer accepts = new Customer();
        accepts.setAcceptsPromotions(true);
        Customer rejects = new Customer();

        assertEquals("Correo electrónico", CustomerLabels.communicationPreferenceText(accepts));
        assertEquals("Sin promociones", CustomerLabels.communicationPreferenceText(rejects));
        assertEquals("", CustomerLabels.communicationPreferenceText(null));
    }

    @Test
    void vehicleCountUsesSingularAndPlural() {
        assertEquals("1 vehículo", CustomerLabels.vehicleCountText(1));
        assertEquals("2 vehículos", CustomerLabels.vehicleCountText(2));
        assertEquals("0 vehículos", CustomerLabels.vehicleCountText(0));
    }

    // ---------- constantes ----------

    @Test
    void commonCitiesStartWithValleduparAndHaveEightEntries() {
        assertEquals(8, CustomerLabels.COMMON_CITIES.size());
        assertEquals("Valledupar", CustomerLabels.COMMON_CITIES.get(0));
        assertTrue(CustomerLabels.COMMON_CITIES.contains("Santa Marta"));
        assertEquals("-", CustomerLabels.PLACEHOLDER);
    }
}
