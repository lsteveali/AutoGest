package com.autogest.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.autogest.exception.ValidationException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de las reglas propias de {@link Vehicle}.
 */
class VehicleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);

    private Vehicle vehicleWithNextMaintenance(LocalDate date) {
        Vehicle vehicle = new Vehicle();
        vehicle.setNextMaintenanceDate(date);
        return vehicle;
    }

    @Test
    void maintenanceIsDueWhen29DaysAway() {
        assertTrue(vehicleWithNextMaintenance(TODAY.plusDays(29)).isMaintenanceDue(TODAY));
    }

    @Test
    void maintenanceIsDueWhen30DaysAway() {
        assertTrue(vehicleWithNextMaintenance(TODAY.plusDays(30)).isMaintenanceDue(TODAY));
    }

    @Test
    void maintenanceIsNotDueWhen31DaysAway() {
        assertFalse(vehicleWithNextMaintenance(TODAY.plusDays(31)).isMaintenanceDue(TODAY));
    }

    @Test
    void maintenanceIsDueWhenAlreadyOverdue() {
        assertTrue(vehicleWithNextMaintenance(TODAY.minusDays(5)).isMaintenanceDue(TODAY));
    }

    @Test
    void maintenanceIsNotDueWithoutNextDate() {
        assertFalse(new Vehicle().isMaintenanceDue(TODAY));
    }

    @Test
    void updateMileageAcceptsHigherAndEqualValues() {
        Vehicle vehicle = new Vehicle();
        vehicle.setMileage(48230);

        vehicle.updateMileage(48230);
        assertEquals(48230, vehicle.getMileage());

        vehicle.updateMileage(50000);
        assertEquals(50000, vehicle.getMileage());
    }

    @Test
    void updateMileageRejectsLowerValue() {
        Vehicle vehicle = new Vehicle();
        vehicle.setMileage(48230);

        ValidationException error = assertThrows(ValidationException.class, () -> vehicle.updateMileage(40000));

        assertEquals("El kilometraje no puede ser menor al actual", error.getMessage());
        assertEquals(48230, vehicle.getMileage());
    }

    @Test
    void getShortDescriptionJoinsMakeModelAndPlate() {
        Vehicle vehicle = new Vehicle();
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setLicensePlate("ABC-123");

        assertEquals("Toyota Corolla ABC-123", vehicle.getShortDescription());
    }

    @Test
    void isInServiceOnlyForInServiceStatus() {
        Vehicle vehicle = new Vehicle();
        assertFalse(vehicle.isInService());

        vehicle.setStatus(VehicleStatus.IN_SERVICE);
        assertTrue(vehicle.isInService());
    }
}

