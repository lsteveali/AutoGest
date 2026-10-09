package com.autogest.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pruebas del formato de dinero y fechas usado en toda la aplicación.
 */
class FormatterTest {

    @Test
    void formatsWholeAmountWithoutDecimals() {
        assertEquals("$120.000", Formatter.money(new BigDecimal("120000")));
    }

    @Test
    void formatsLargeAmountWithThousandsSeparator() {
        assertEquals("$12.450.000", Formatter.money(new BigDecimal("12450000")));
    }

    @Test
    void formatsAmountWithDecimals() {
        assertEquals("$1.234,50", Formatter.money(new BigDecimal("1234.5")));
    }

    @Test
    void roundsToTwoDecimalsHalfUp() {
        assertEquals("$1.235", Formatter.money(new BigDecimal("1234.999")));
    }

    @Test
    void formatsNullAmountAsZero() {
        assertEquals("$0", Formatter.money(null));
    }

    @Test
    void formatsDate() {
        assertEquals("15/06/2026", Formatter.date(LocalDate.of(2026, 6, 15)));
    }

    @Test
    void formatsTimestamp() {
        assertEquals("15/06/2026 14:30",
                Formatter.timestamp(LocalDateTime.of(2026, 6, 15, 14, 30)));
    }
}
