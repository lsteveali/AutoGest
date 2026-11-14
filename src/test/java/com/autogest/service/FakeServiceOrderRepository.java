package com.autogest.service;

import com.autogest.model.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.repository.ServiceOrderRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio falso de órdenes de servicio, en memoria. Reproduce la búsqueda
 * del {@code ServiceOrderDao} (número, placa, marca, modelo o cliente; estado;
 * responsable) y los conteos.
 */
class FakeServiceOrderRepository extends ServiceOrderRepository {

    private final Map<Integer, ServiceOrder> orders = new LinkedHashMap<>();
    private int sequence;

    FakeServiceOrderRepository() {
        super(null, null, null, null, null, null, null, null, null);
    }

    @Override
    public ServiceOrder create(ServiceOrder order) {
        order.setId(++sequence);
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public ServiceOrder findById(int id) {
        return orders.get(id);
    }

    @Override
    public List<ServiceOrder> findAll() {
        return newestFirst(new ArrayList<>(orders.values()));
    }

    @Override
    public List<ServiceOrder> search(String text, OrderStatus status, int responsibleId) {
        String needle = text == null ? "" : text.trim().replace("#", "").toLowerCase();
        List<ServiceOrder> result = new ArrayList<>();
        for (ServiceOrder order : orders.values()) {
            boolean matchesText = needle.isEmpty()
                    || contains(order.getNumber(), needle)
                    || contains(order.getVehicle().getLicensePlate(), needle)
                    || contains(order.getVehicle().getMake(), needle)
                    || contains(order.getVehicle().getModel(), needle)
                    || (order.getVehicle().getOwner() != null
                        && contains(order.getVehicle().getOwner().getFullName(), needle));
            boolean matchesStatus = status == null || order.getStatus() == status;
            boolean matchesResponsible = responsibleId <= 0
                    || (order.getResponsible() != null && order.getResponsible().getId() == responsibleId);
            if (matchesText && matchesStatus && matchesResponsible) {
                result.add(order);
            }
        }
        return newestFirst(result);
    }

    @Override
    public List<ServiceOrder> findByVehicle(int vehicleId) {
        return newestFirst(orders.values().stream().filter(o -> o.getVehicle().getId() == vehicleId).toList());
    }

    @Override
    public List<ServiceOrder> findByCustomer(int customerId) {
        return newestFirst(orders.values().stream()
                .filter(o -> o.getVehicle().getOwner() != null && o.getVehicle().getOwner().getId() == customerId)
                .toList());
    }

    @Override
    public boolean update(ServiceOrder order) {
        return orders.replace(order.getId(), order) != null;
    }

    @Override
    public int countByStatus(OrderStatus status) {
        return (int) orders.values().stream().filter(o -> o.getStatus() == status).count();
    }

    @Override
    public int countOpen() {
        return (int) orders.values().stream().filter(FakeServiceOrderRepository::isOpen).count();
    }

    @Override
    public boolean existsOpenOrderForVehicle(int vehicleId) {
        return orders.values().stream().anyMatch(o -> o.getVehicle().getId() == vehicleId && isOpen(o));
    }

    @Override
    public int nextSequence() {
        return sequence + 1;
    }

    private static boolean isOpen(ServiceOrder order) {
        return order.getStatus() != OrderStatus.DELIVERED && order.getStatus() != OrderStatus.CANCELLED;
    }

    private static List<ServiceOrder> newestFirst(List<ServiceOrder> list) {
        List<ServiceOrder> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparing(ServiceOrder::getId).reversed());
        return copy;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
