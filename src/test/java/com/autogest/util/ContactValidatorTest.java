package com.autogest.util;

import com.autogest.exception.ValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Correos, teléfonos por país y nombres solo con letras. */
class ContactValidatorTest {

    @Test
    void emailIsTrimmedAndLowercased() {
        assertEquals("ana.lopez@gmail.com", ContactValidator.email("  Ana.Lopez@Gmail.com "));
        assertEquals("jefe@autogest.com", ContactValidator.email("jefe@autogest.com"));
        assertEquals("c+taller@outlook.com.co", ContactValidator.email("c+taller@outlook.com.co"));
    }

    @Test
    void malformedEmailsAreRejected() {
        for (String bad : new String[] {"ana", "ana@", "@gmail.com", "ana@gmail", "ana gomez@gmail.com",
                "ana..gomez@gmail.com", ".ana@gmail.com", "ana@gmail..com", "ana@-gmail.com", "ana@gmail.c0m"}) {
            assertThrows(ValidationException.class, () -> ContactValidator.email(bad), bad);
        }
        assertNull(ContactValidator.optionalEmail("  "));
    }

    @Test
    void misspelledDomainSuggestsTheRightOne() {
        ValidationException error = assertThrows(ValidationException.class,
                () -> ContactValidator.email("ana@gmial.com"));
        assertTrue(error.getMessage().contains("ana@gmail.com"), error.getMessage());
        assertThrows(ValidationException.class, () -> ContactValidator.email("ana@hotmail.con"));
    }

    @Test
    void colombianPhonesFollowTheirRules() {
        assertEquals("+573001234567", ContactValidator.phone("300 123 4567"));
        assertEquals("+573001234567", ContactValidator.phone("+57 300 123 4567"));
        assertEquals("+576015551234", ContactValidator.phone("+576015551234"));
        ValidationException error = assertThrows(ValidationException.class,
                () -> ContactValidator.phone("+57 400 123 4567"));
        assertTrue(error.getMessage().contains("Colombia"), error.getMessage());
        assertThrows(ValidationException.class, () -> ContactValidator.phone("+57 300 123 456"));
        assertThrows(ValidationException.class, () -> ContactValidator.phone("300-ABC-4567"));
    }

    @Test
    void eachCountryChecksFirstDigitAndLength() {
        assertEquals("+593991234567", ContactValidator.phone("+593 99 123 4567"));
        assertEquals("+13055551234", ContactValidator.phone("+1 305 555 1234"));
        assertEquals("+34612345678", ContactValidator.phone("+34 612 34 56 78"));
        assertEquals("+5491123456789", ContactValidator.phone("+54 9 11 2345 6789"));
        assertEquals("+5511912345678", ContactValidator.phone("+55 11 91234 5678"));
        assertThrows(ValidationException.class, () -> ContactValidator.phone("+593 89 123 4567"));
        assertThrows(ValidationException.class, () -> ContactValidator.phone("+1 105 123 4567"));
        assertThrows(ValidationException.class, () -> ContactValidator.phone("+34 512 34 56 78"));
        assertNull(ContactValidator.optionalPhone("+57"));
    }

    @Test
    void storedPhonesAreSplitIntoCountryAndNumber() {
        assertEquals(PhoneCountry.ECUADOR, PhoneCountry.of("+593991234567"));
        assertEquals("991234567", PhoneCountry.nationalPart("+593991234567"));
        assertEquals(PhoneCountry.COLOMBIA, PhoneCountry.of("3001234567"));
        assertEquals(PhoneCountry.PANAMA, PhoneCountry.of("+50761234567"));
    }

    @Test
    void namesOnlyAllowLetters() {
        assertEquals("María José", ContactValidator.name("  María   José ", "Los nombres"));
        assertEquals("Peña Núñez", ContactValidator.name("Peña Núñez", "Los apellidos"));
        ValidationException digits = assertThrows(ValidationException.class,
                () -> ContactValidator.name("9765 456y7", "Los nombres"));
        assertEquals("Los nombres solo pueden tener letras.", digits.getMessage());
        assertThrows(ValidationException.class, () -> ContactValidator.name("Ana_", "Los nombres"));
        assertThrows(ValidationException.class, () -> ContactValidator.name(" ", "Los apellidos"));
    }
}
