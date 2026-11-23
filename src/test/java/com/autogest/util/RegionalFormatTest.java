package com.autogest.util;

import com.autogest.model.SystemPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Formatos regionales de «Preferencias del sistema» aplicados por {@link Formatter}. */
class RegionalFormatTest {

    @AfterEach
    void restoreFactoryPreferences() {
        AppPreferences.update(null);
    }

    private static SystemPreferences preferences() {
        return SystemPreferences.defaults();
    }

    @Test
    void factoryFormatIsColombian() {
        RegionalFormat format = RegionalFormat.of(null);

        assertEquals("$", format.currencySymbol());
        assertEquals('.', format.symbols().getGroupingSeparator());
        assertEquals(ZoneId.of("America/Bogota"), format.zone());
    }

    @Test
    void currencyAndSeparatorsFollowThePreferences() {
        SystemPreferences p = preferences();
        p.setCurrency("Dólar estadounidense (USD)");
        p.setDecimalSeparator("Punto (.)");
        AppPreferences.update(p);

        assertEquals("US$1,234.50", Formatter.money(new BigDecimal("1234.5")));
        assertEquals("US$0", Formatter.money(null));
        assertEquals("87,300", Formatter.integer(87300));
    }

    @Test
    void otherCurrenciesHaveTheirSymbol() {
        assertEquals("S/", RegionalFormat.currencySymbol("Sol peruano (PEN)"));
        assertEquals("MX$", RegionalFormat.currencySymbol("Peso mexicano (MXN)"));
        assertEquals("$", RegionalFormat.currencySymbol("algo raro"));
    }

    @Test
    void dateFormatFollowsThePreferences() {
        SystemPreferences p = preferences();
        p.setDateFormat("AAAA-MM-DD");
        AppPreferences.update(p);
        assertEquals("2026-06-15", Formatter.date(LocalDate.of(2026, 6, 15)));

        p = preferences();
        p.setDateFormat("MM/DD/AAAA");
        AppPreferences.update(p);
        assertEquals("06/15/2026", Formatter.date(LocalDate.of(2026, 6, 15)));
    }

    @Test
    void twelveHourClockShowsTheHourWithoutLeadingZero() {
        SystemPreferences p = preferences();
        p.setTimeFormat("12 horas");
        p.setTimeZone(null);
        AppPreferences.update(p);
        LocalDateTime local = LocalDateTime.of(2026, 6, 15, 14, 30);
        LocalDateTime bogota = local.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("America/Bogota")).toLocalDateTime();

        String text = Formatter.timestamp(local);

        assertTrue(text.startsWith("15/06/2026 " + (bogota.getHour() % 12 == 0 ? 12 : bogota.getHour() % 12) + ":30"),
                text);
    }

    @Test
    void timestampsMoveToTheChosenTimeZone() {
        SystemPreferences p = preferences();
        p.setTimeZone("(GMT-06:00) Ciudad de México");
        AppPreferences.update(p);
        LocalDateTime local = LocalDateTime.of(2026, 6, 15, 14, 30);
        LocalDateTime mexico = local.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("America/Mexico_City")).toLocalDateTime();

        assertEquals(String.format("15/06/2026 %02d:30", mexico.getHour()), Formatter.timestamp(local));
    }
}
