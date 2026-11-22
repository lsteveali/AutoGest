package com.autogest.controller.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.autogest.controller.components.StatusBadge;
import com.autogest.model.Customer;
import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de los textos de la lista de órdenes (sin JavaFX).
 */
class OrderLabelsTest {

    @Test
    void statusTextRoundTripsAndColors() {
        for (OrderStatus status : OrderStatus.values()) {
            assertEquals(status, OrderLabels.statusFromText(OrderLabels.statusText(status)));
        }
        assertNull(OrderLabels.statusFromText(OrderLabels.ALL));
        assertEquals(StatusBadge.BadgeType.WARNING, OrderLabels.statusType(OrderStatus.IN_REPAIR));
        assertEquals(StatusBadge.BadgeType.DANGER, OrderLabels.statusType(OrderStatus.CANCELLED));
        assertEquals(6, OrderLabels.statusOptions().size());
    }

    @Test
    void rowTextsUseVehicleAndOwner() {
        Customer owner = new Customer();
        owner.setFirstName("Juan");
        owner.setLastName("Pérez");
        Vehicle vehicle = new Vehicle();
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setLicensePlate("ABC-123");
        vehicle.setYear(2022);
        vehicle.setMileage(48230);
        vehicle.setOwner(owner);
        ServiceOrder order = new ServiceOrder();
        order.setNumber("0145");
        order.setVehicle(vehicle);

        assertEquals("#0145", OrderLabels.number(order));
        assertEquals("Juan Pérez", OrderLabels.customer(order));
        assertEquals("Toyota Corolla", OrderLabels.vehicle(order));
        assertEquals("ABC-123", OrderLabels.plate(order));
        assertEquals("Sin asignar", OrderLabels.responsible(order));
        assertEquals("Toyota Corolla · ABC-123 · 2022 · 48.230 km", OrderLabels.vehicleOption(vehicle));
    }
}
