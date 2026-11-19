package com.autogest.service;

import com.autogest.model.OrderStatus;
import com.autogest.model.ServiceOrder;
import com.autogest.repository.ServiceOrderRepository;

import com.autogest.model.OrderLine;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.stream.Stream;
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

    // ---------- consultas para Reportes (Sprint 7) ----------

    @Override
    public Map<OrderStatus, Integer> countGroupedByStatus() {
        Map<OrderStatus, Integer> result = new EnumMap<>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            result.put(status, countByStatus(status));
        }
        return result;
    }

    @Override
    public int countEnteredBetween(LocalDate from, LocalDate to) {
        return (int) between(from, to).count();
    }

    @Override
    public Map<YearMonth, Integer> countByMonth(LocalDate from, LocalDate to) {
        Map<YearMonth, Integer> result = new TreeMap<>();
        between(from, to).forEach(o -> result.merge(YearMonth.from(o.getEntryDate()), 1, Integer::sum));
        return new LinkedHashMap<>(result);
    }

    @Override
    public Map<String, Integer> findTopServices(LocalDate from, LocalDate to, int limit) {
        return top(from, to, limit, ServiceLine.class);
    }

    @Override
    public Map<String, Integer> findTopParts(LocalDate from, LocalDate to, int limit) {
        return top(from, to, limit, PartLine.class);
    }

    @Override
    public Map<String, Integer> countByResponsible(LocalDate from, LocalDate to) {
        Map<String, Integer> result = new LinkedHashMap<>();
        between(from, to).forEach(o -> result.merge(
                o.getResponsible() == null ? "Sin asignar" : o.getResponsible().getFullName(), 1, Integer::sum));
        return result;
    }

    @Override
    public BigDecimal sumEstimatedTotalBetween(LocalDate from, LocalDate to) {
        return between(from, to).filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(ServiceOrder::calculateSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Stream<ServiceOrder> between(LocalDate from, LocalDate to) {
        return orders.values().stream()
                .filter(o -> !o.getEntryDate().isBefore(from) && !o.getEntryDate().isAfter(to));
    }

    private Map<String, Integer> top(LocalDate from, LocalDate to, int limit, Class<? extends OrderLine> kind) {
        Map<String, Integer> totals = new HashMap<>();
        between(from, to).filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .flatMap(o -> o.getLines().stream())
                .filter(kind::isInstance)
                .forEach(line -> totals.merge(line.getDescription(), line.getQuantity(), Integer::sum));
        Map<String, Integer> result = new LinkedHashMap<>();
        totals.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .forEach(e -> result.put(e.getKey(), e.getValue()));
        return result;
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
