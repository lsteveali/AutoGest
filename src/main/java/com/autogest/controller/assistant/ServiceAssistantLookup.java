package com.autogest.controller.assistant;

import com.autogest.config.AppContext;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.ServiceOrder;
import com.autogest.model.Vehicle;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@link AssistantLookup} sobre los servicios de la aplicación. Solo usa
 * búsquedas; no escribe en la base de datos. Se llama en segundo plano.
 *
 * <p>Principles: Adapter, Low Coupling.</p>
 */
public class ServiceAssistantLookup implements AssistantLookup {

    @Override
    public Optional<Integer> vehicleByPlate(String plate) {
        return vehicle(plate).map(Vehicle::getId);
    }

    @Override
    public Optional<Integer> ownerOfPlate(String plate) {
        return vehicle(plate).map(Vehicle::getOwner).filter(owner -> owner.getId() > 0).map(Customer::getId);
    }

    @Override
    public boolean ownerIsActive(String plate) {
        return ownerOfPlate(plate)
                .map(id -> AppContext.getInstance().getCustomerService().getDetail(id).getStatus()
                        == CustomerStatus.ACTIVE)
                .orElse(true);
    }

    @Override
    public Optional<Integer> orderByNumber(String number) {
        String wanted = withoutLeadingZeros(number);
        List<ServiceOrder> orders = AppContext.getInstance().getServiceOrderService().searchOrders(number, null, 0);
        return orders.stream()
                .filter(order -> order.getNumber() != null && withoutLeadingZeros(order.getNumber()).equals(wanted))
                .map(ServiceOrder::getId)
                .findFirst();
    }

    @Override
    public Optional<Integer> invoiceByNumber(String number) {
        List<Invoice> invoices = AppContext.getInstance().getInvoiceService().searchInvoices(number, null, null, null);
        return invoices.stream()
                .filter(invoice -> number.equalsIgnoreCase(invoice.getNumber()))
                .map(Invoice::getId)
                .findFirst();
    }

    @Override
    public List<Match> customersByName(String name) {
        return AppContext.getInstance().getCustomerService().searchCustomers(name, null).stream()
                .map(customer -> new Match(customer.getId(), customer.getFullName()))
                .toList();
    }

    private static Optional<Vehicle> vehicle(String plate) {
        String wanted = plateKey(plate);
        // Se busca por las letras: la base puede guardar «GHI-101» y el usuario escribir «GHI101».
        String letters = wanted.length() >= 3 ? wanted.substring(0, 3) : wanted;
        return AppContext.getInstance().getVehicleService().searchVehicles(letters, null, null).stream()
                .filter(vehicle -> plateKey(vehicle.getLicensePlate()).equals(wanted))
                .findFirst();
    }

    /** «abc-123» y «ABC 123» son la misma placa. */
    static String plateKey(String plate) {
        return plate == null ? "" : plate.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** «0045» y «45» son el mismo número. */
    static String withoutLeadingZeros(String number) {
        String stripped = number == null ? "" : number.replaceFirst("^#?0+(?=\\d)", "").replace("#", "");
        return stripped;
    }
}
