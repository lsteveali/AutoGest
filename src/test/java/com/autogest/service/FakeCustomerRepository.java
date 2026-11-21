package com.autogest.service;

import com.autogest.model.Customer;
import com.autogest.model.Catalogs.CustomerStatus;
import com.autogest.repository.CustomerRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio falso de clientes para las pruebas de los Services, sin base de
 * datos. Extiende {@link CustomerRepository} (clase concreta no final) y guarda
 * los clientes en memoria. Se pasa un DAO {@code null} porque la subclase nunca
 * delega en el DAO real.
 */
class FakeCustomerRepository extends CustomerRepository {

    private final Map<Integer, Customer> customers = new LinkedHashMap<>();
    private int sequence;
    private LocalDate lastRegisteredSinceArgument;

    FakeCustomerRepository() {
        super(null);
    }

    /** Devuelve la fecha con la que se pidió el último conteo de clientes nuevos. */
    LocalDate lastRegisteredSinceArgument() {
        return lastRegisteredSinceArgument;
    }

    @Override
    public Customer create(Customer customer) {
        customer.setId(++sequence);
        customers.put(customer.getId(), customer);
        return customer;
    }

    @Override
    public Customer findById(int id) {
        return customers.get(id);
    }

    @Override
    public List<Customer> findAll() {
        return new ArrayList<>(customers.values());
    }

    @Override
    public boolean update(Customer customer) {
        if (!customers.containsKey(customer.getId())) {
            return false;
        }
        customers.put(customer.getId(), customer);
        return true;
    }

    @Override
    public boolean delete(int id) {
        Customer customer = customers.get(id);
        if (customer == null) {
            return false;
        }
        customer.deactivate();
        return true;
    }

    @Override
    public List<Customer> search(String text, CustomerStatus status) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        List<Customer> result = new ArrayList<>();
        for (Customer c : customers.values()) {
            boolean matchesText = needle.isEmpty()
                    || c.getFullName().toLowerCase().contains(needle)
                    || contains(c.getDocumentNumber(), needle)
                    || contains(c.getEmail(), needle);
            boolean matchesStatus = status == null || c.getStatus() == status;
            if (matchesText && matchesStatus) {
                result.add(c);
            }
        }
        return result;
    }

    @Override
    public boolean existsByDocument(String number) {
        return customers.values().stream().anyMatch(c -> number.equals(c.getDocumentNumber()));
    }

    @Override
    public boolean existsByDocumentExcluding(String number, int id) {
        return customers.values().stream()
                .anyMatch(c -> c.getId() != id && number.equals(c.getDocumentNumber()));
    }

    @Override
    public int count() {
        return customers.size();
    }

    @Override
    public int countByStatus(CustomerStatus status) {
        return (int) customers.values().stream().filter(c -> c.getStatus() == status).count();
    }

    @Override
    public int countRegisteredSince(LocalDate date) {
        lastRegisteredSinceArgument = date;
        return (int) customers.values().stream()
                .filter(c -> c.getRegistrationDate() != null && !c.getRegistrationDate().isBefore(date))
                .count();
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
