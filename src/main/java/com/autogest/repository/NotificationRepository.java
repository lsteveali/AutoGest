package com.autogest.repository;

import com.autogest.dao.NotificationDao;
import com.autogest.model.Channel;
import com.autogest.model.Customer;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Organiza el acceso a las notificaciones y completa el cliente de cada una
 * (con caché por llamada en las listas).
 *
 * <p>Principles: Indirection, Pure Fabrication, Low Coupling.</p>
 */
public class NotificationRepository {

    private final NotificationDao dao;
    private final CustomerRepository customerRepository;

    /**
     * @param dao                DAO de notificaciones
     * @param customerRepository repositorio de clientes (completa {@code customer})
     */
    public NotificationRepository(NotificationDao dao, CustomerRepository customerRepository) {
        this.dao = dao;
        this.customerRepository = customerRepository;
    }

    public Notification create(Notification notification) {
        return dao.create(notification);
    }

    public Notification findById(int id) {
        Notification notification = dao.findById(id);
        if (notification != null) {
            resolveAll(List.of(notification));
        }
        return notification;
    }

    public List<Notification> findAll() {
        return resolveAll(dao.findAll());
    }

    public boolean update(Notification notification) {
        return dao.update(notification);
    }

    public boolean delete(int id) {
        return dao.delete(id);
    }

    public List<Notification> search(String text, NotificationType type, Channel channel, NotificationStatus status) {
        return resolveAll(dao.search(text, type, channel, status));
    }

    public List<Notification> findByCustomer(int customerId) {
        return resolveAll(dao.findByCustomer(customerId));
    }

    /** Programadas cuya fecha es anterior o igual a {@code now}. */
    public List<Notification> findDueScheduled(LocalDateTime now) {
        return resolveAll(dao.findDueScheduled(now));
    }

    public int countByStatus(NotificationStatus status) {
        return dao.countByStatus(status);
    }

    private List<Notification> resolveAll(List<Notification> notifications) {
        Map<Integer, Customer> cache = new HashMap<>();
        for (Notification notification : notifications) {
            if (notification.getCustomer() == null) {
                continue;
            }
            int id = notification.getCustomer().getId();
            Customer customer = cache.computeIfAbsent(id, customerRepository::findById);
            if (customer != null) {
                notification.setCustomer(customer);
            }
        }
        return notifications;
    }
}
