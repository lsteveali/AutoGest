package com.autogest.controller.billing;

import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** «Ver facturas» del cliente: una factura es del dueño del vehículo de su orden. */
class InvoiceLabelsTest {

    private static Invoice invoiceOf(Integer customerId) {
        Invoice invoice = new Invoice();
        ServiceOrder order = new ServiceOrder();
        Vehicle vehicle = new Vehicle();
        if (customerId != null) {
            Customer owner = new Customer();
            owner.setId(customerId);
            vehicle.setOwner(owner);
        }
        order.setVehicle(vehicle);
        invoice.setOrder(order);
        return invoice;
    }

    @Test
    void invoiceBelongsToTheVehicleOwner() {
        assertTrue(InvoiceLabels.belongsTo(invoiceOf(7), 7));
        assertFalse(InvoiceLabels.belongsTo(invoiceOf(7), 8));
    }

    @Test
    void invoiceWithoutOwnerOrOrderBelongsToNobody() {
        assertFalse(InvoiceLabels.belongsTo(invoiceOf(null), 7));
        assertFalse(InvoiceLabels.belongsTo(new Invoice(), 7));
    }
}
