package com.autogest.service;

import com.autogest.model.Vehicle;
import com.autogest.model.VehicleCounts;
import com.autogest.model.Catalogs.VehicleStatus;
import com.autogest.model.Catalogs.VehicleType;
import com.autogest.repository.VehicleRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio falso de vehículos para las pruebas de los Services, sin base de
 * datos. Extiende {@link VehicleRepository} (clase concreta no final) y guarda
 * los vehículos en memoria. Se pasa un DAO {@code null} porque la subclase nunca
 * delega en el DAO real.
 */
class FakeVehicleRepository extends VehicleRepository {

    private final Map<Integer, Vehicle> vehicles = new LinkedHashMap<>();
    private int sequence;

    FakeVehicleRepository() {
        super(null);
    }

    @Override
    public Vehicle create(Vehicle vehicle) {
        vehicle.setId(++sequence);
        vehicles.put(vehicle.getId(), vehicle);
        return vehicle;
    }

    @Override
    public Vehicle findById(int id) {
        return vehicles.get(id);
    }

    @Override
    public List<Vehicle> findAll() {
        return new ArrayList<>(vehicles.values());
    }

    @Override
    public boolean update(Vehicle vehicle) {
        if (!vehicles.containsKey(vehicle.getId())) {
            return false;
        }
        vehicles.put(vehicle.getId(), vehicle);
        return true;
    }

    @Override
    public boolean delete(int id) {
        Vehicle vehicle = vehicles.get(id);
        if (vehicle == null) {
            return false;
        }
        vehicle.setStatus(VehicleStatus.INACTIVE);
        return true;
    }

    @Override
    public List<Vehicle> search(String text, VehicleStatus status, VehicleType type) {
        return search(text, null, status, type);
    }

    @Override
    public List<Vehicle> search(String text, String make, VehicleStatus status, VehicleType type) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        String makeNeedle = make == null ? "" : make.trim().toLowerCase();
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : vehicles.values()) {
            boolean matchesText = needle.isEmpty()
                    || contains(v.getMake(), needle)
                    || contains(v.getModel(), needle)
                    || contains(v.getLicensePlate(), needle)
                    || contains(v.getVin(), needle)
                    || containsOwner(v, needle);
            boolean matchesMake = makeNeedle.isEmpty() || contains(v.getMake(), makeNeedle);
            boolean matchesStatus = status == null || v.getStatus() == status;
            boolean matchesType = type == null || v.getVehicleType() == type;
            if (matchesText && matchesMake && matchesStatus && matchesType) {
                result.add(v);
            }
        }
        return result;
    }

    @Override
    public List<Vehicle> findByCustomer(int customerId) {
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : vehicles.values()) {
            if (v.getOwner() != null && v.getOwner().getId() == customerId) {
                result.add(v);
            }
        }
        return result;
    }

    @Override
    public boolean existsByLicensePlate(String plate) {
        return vehicles.values().stream().anyMatch(v -> plate.equals(v.getLicensePlate()));
    }

    @Override
    public boolean existsByLicensePlateExcluding(String plate, int id) {
        return vehicles.values().stream()
                .anyMatch(v -> v.getId() != id && plate.equals(v.getLicensePlate()));
    }

    @Override
    public int count() {
        return vehicles.size();
    }

    @Override
    public int countByStatus(VehicleStatus status) {
        return (int) vehicles.values().stream().filter(v -> v.getStatus() == status).count();
    }

    @Override
    public int countByType(VehicleType type) {
        return (int) vehicles.values().stream().filter(v -> v.getVehicleType() == type).count();
    }

    @Override
    public int countMaintenanceDue(LocalDate date) {
        return (int) vehicles.values().stream()
                .filter(v -> v.getNextMaintenanceDate() != null && !v.getNextMaintenanceDate().isAfter(date))
                .count();
    }

    @Override
    public VehicleCounts countSummary(LocalDate maintenanceDate) {
        return new VehicleCounts(count(), countByStatus(VehicleStatus.ACTIVE),
                countByStatus(VehicleStatus.IN_SERVICE), countMaintenanceDue(maintenanceDate));
    }

    @Override
    public Map<Integer, Integer> countAllByCustomer() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Vehicle v : vehicles.values()) {
            if (v.getOwner() != null) {
                counts.merge(v.getOwner().getId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    @Override
    public int countByCustomer(int customerId) {
        return (int) vehicles.values().stream()
                .filter(v -> v.getOwner() != null && v.getOwner().getId() == customerId)
                .count();
    }

    @Override
    public List<String> findMakes() {
        return vehicles.values().stream()
                .map(Vehicle::getMake)
                .filter(make -> make != null && !make.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    /** Comprueba si el texto está en el nombre del propietario. */
    private static boolean containsOwner(Vehicle vehicle, String needle) {
        if (vehicle.getOwner() == null) {
            return false;
        }
        String fullName = vehicle.getOwner().getFullName();
        return contains(fullName, needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
