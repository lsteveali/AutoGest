package com.autogest.repository;

import com.autogest.dao.DiagnosisDao;
import com.autogest.dao.OrderHistoryDao;
import com.autogest.dao.OrderLineDao;
import com.autogest.dao.ServiceOrderDao;
import com.autogest.model.Customer;
import com.autogest.model.Diagnosis;
import com.autogest.model.OrderHistory;
import com.autogest.model.OrderLine;
import com.autogest.model.Catalogs.OrderStatus;
import com.autogest.model.Part;
import com.autogest.model.PartLine;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/**
 * Organiza el acceso a las órdenes de servicio y completa sus relaciones
 * (vehículo con su propietario, responsable, líneas con su servicio o
 * repuesto, historial y diagnóstico) pidiéndolas a los otros repositorios.
 *
 * <p>En las listas usa un caché por llamada para no consultar dos veces el
 * mismo vehículo, cliente o usuario.</p>
 *
 * <p>Principles: Indirection, Pure Fabrication, Low Coupling, Information Expert
 * (sabe cómo armar una orden completa).</p>
 */
public class ServiceOrderRepository {

    private final ServiceOrderDao dao;
    private final OrderLineDao lineDao;
    private final OrderHistoryDao historyDao;
    private final DiagnosisDao diagnosisDao;
    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final PartRepository partRepository;

    /**
     * Recibe todos los DAO y repositorios que necesita (DIP, D-03).
     */
    public ServiceOrderRepository(ServiceOrderDao dao, OrderLineDao lineDao, OrderHistoryDao historyDao,
                                  DiagnosisDao diagnosisDao, VehicleRepository vehicleRepository,
                                  CustomerRepository customerRepository, UserRepository userRepository,
                                  ServiceOfferingRepository serviceOfferingRepository,
                                  PartRepository partRepository) {
        this.dao = dao;
        this.lineDao = lineDao;
        this.historyDao = historyDao;
        this.diagnosisDao = diagnosisDao;
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.partRepository = partRepository;
    }

    /** Guarda la orden con sus líneas e historial en una transacción. */
    public ServiceOrder create(ServiceOrder order) {
        return dao.createWithDetails(order);
    }

    /**
     * Orden completa (con historial y diagnóstico) o {@code null} si no existe.
     */
    public ServiceOrder findById(int id) {
        ServiceOrder order = dao.findById(id);
        if (order == null) {
            return null;
        }
        Resolver resolver = new Resolver();
        resolver.resolveRow(order);
        List<OrderHistory> history = historyDao.findByOrder(id);
        for (OrderHistory entry : history) {
            if (entry.getUser() != null) {
                entry.setUser(resolver.user(entry.getUser().getId()));
            }
        }
        order.setHistory(history);
        order.setDiagnosis(diagnosisDao.findByOrder(id));
        return order;
    }

    /** Todas las órdenes, de la más reciente a la más antigua. */
    public List<ServiceOrder> findAll() {
        return resolveRows(dao.findAll());
    }

    /** Búsqueda por texto, estado y responsable (0 = todos). */
    public List<ServiceOrder> search(String text, OrderStatus status, int responsibleId) {
        return resolveRows(dao.search(text, status, responsibleId));
    }

    /** Órdenes de un vehículo. */
    public List<ServiceOrder> findByVehicle(int vehicleId) {
        return resolveRows(dao.findByVehicle(vehicleId));
    }

    /** Órdenes de los vehículos de un cliente. */
    public List<ServiceOrder> findByCustomer(int customerId) {
        return resolveRows(dao.findByCustomer(customerId));
    }

    /** Actualiza la cabecera de la orden (estado, responsable, fechas...). */
    public boolean update(ServiceOrder order) {
        return dao.update(order);
    }

    /** Agrega una línea a una orden existente. */
    public void addLine(int orderId, OrderLine line) {
        lineDao.insert(orderId, line);
    }

    /** Agrega una entrada de historial a una orden existente. */
    public void addHistory(int orderId, OrderHistory entry) {
        historyDao.insert(orderId, entry);
    }

    /** Guarda o reemplaza el diagnóstico de una orden. */
    public void saveDiagnosis(int orderId, Diagnosis diagnosis) {
        diagnosisDao.save(orderId, diagnosis);
    }

    public int countByStatus(OrderStatus status) {
        return dao.countByStatus(status);
    }

    public int countOpen() {
        return dao.countOpen();
    }

    public boolean existsOpenOrderForVehicle(int vehicleId) {
        return dao.existsOpenOrderForVehicle(vehicleId);
    }

    public int nextSequence() {
        return dao.nextSequence();
    }

    // ---------- consultas para Reportes (Sprint 7) ----------

    public Map<OrderStatus, Integer> countGroupedByStatus() {
        return dao.countGroupedByStatus();
    }

    public int countEnteredBetween(LocalDate from, LocalDate to) {
        return dao.countEnteredBetween(from, to);
    }

    public Map<YearMonth, Integer> countByMonth(LocalDate from, LocalDate to) {
        return dao.countByMonth(from, to);
    }

    public Map<String, Integer> findTopServices(LocalDate from, LocalDate to, int limit) {
        return dao.findTopServices(from, to, limit);
    }

    public Map<String, Integer> findTopParts(LocalDate from, LocalDate to, int limit) {
        return dao.findTopParts(from, to, limit);
    }

    public Map<String, Integer> countByResponsible(LocalDate from, LocalDate to) {
        return dao.countByResponsible(from, to);
    }

    public BigDecimal sumEstimatedTotalBetween(LocalDate from, LocalDate to) {
        return dao.sumEstimatedTotalBetween(from, to);
    }

    private List<ServiceOrder> resolveRows(List<ServiceOrder> orders) {
        Resolver resolver = new Resolver();
        orders.forEach(resolver::resolveRow);
        return orders;
    }

    /**
     * Completa relaciones con caché por llamada (una consulta por id distinto).
     */
    private final class Resolver {

        private final Map<Integer, Vehicle> vehicles = new HashMap<>();
        private final Map<Integer, Customer> customers = new HashMap<>();
        private final Map<Integer, User> users = new HashMap<>();
        private final Map<Integer, ServiceOffering> offerings = new HashMap<>();
        private final Map<Integer, Part> parts = new HashMap<>();

        void resolveRow(ServiceOrder order) {
            if (order.getVehicle() != null) {
                order.setVehicle(vehicle(order.getVehicle().getId()));
            }
            if (order.getResponsible() != null) {
                order.setResponsible(user(order.getResponsible().getId()));
            }
            List<OrderLine> lines = lineDao.findByOrder(order.getId());
            for (OrderLine line : lines) {
                if (line instanceof ServiceLine serviceLine && serviceLine.getServiceOffering() != null) {
                    serviceLine.setServiceOffering(cached(offerings, serviceLine.getServiceOffering().getId(),
                            serviceOfferingRepository::findById, serviceLine.getServiceOffering()));
                } else if (line instanceof PartLine partLine && partLine.getPart() != null) {
                    partLine.setPart(cached(parts, partLine.getPart().getId(),
                            partRepository::findById, partLine.getPart()));
                }
            }
            order.setLines(lines);
        }

        Vehicle vehicle(int id) {
            Vehicle minimal = new Vehicle();
            minimal.setId(id);
            Vehicle vehicle = cached(vehicles, id, vehicleRepository::findById, minimal);
            if (vehicle.getOwner() != null && vehicle.getOwner().getFirstName() == null) {
                vehicle.setOwner(cached(customers, vehicle.getOwner().getId(),
                        customerRepository::findById, vehicle.getOwner()));
            }
            return vehicle;
        }

        User user(int id) {
            User minimal = new User();
            minimal.setId(id);
            return cached(users, id, userRepository::findById, minimal);
        }

        private <T> T cached(Map<Integer, T> cache, int id, IntFunction<T> loader, T fallback) {
            T value = cache.computeIfAbsent(id, loader::apply);
            return value == null ? fallback : value;
        }
    }
}
