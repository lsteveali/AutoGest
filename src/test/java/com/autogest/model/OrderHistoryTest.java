package com.autogest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de {@link OrderHistory#getDisplayLine()}.
 */
class OrderHistoryTest {

    @Test
    void displayLineShowsDateUserAndDescription() {
        User user = new User();
        user.setFirstName("José");
        user.setLastName("García");
        OrderHistory entry = new OrderHistory();
        entry.setTimestamp(LocalDateTime.of(2026, 6, 15, 10, 35));
        entry.setDescription("Orden creada");
        entry.setUser(user);

        assertEquals("15/06/2026 10:35 - José García: Orden creada", entry.getDisplayLine());
    }

    @Test
    void displayLineOmitsMissingUser() {
        OrderHistory entry = new OrderHistory();
        entry.setTimestamp(LocalDateTime.of(2026, 6, 15, 10, 35));
        entry.setDescription("Orden creada");

        assertEquals("15/06/2026 10:35 - Orden creada", entry.getDisplayLine());
    }
}
