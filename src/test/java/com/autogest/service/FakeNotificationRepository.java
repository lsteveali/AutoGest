package com.autogest.service;

import com.autogest.model.Catalogs.Channel;
import com.autogest.model.Notification;
import com.autogest.model.Catalogs.NotificationStatus;
import com.autogest.model.Catalogs.NotificationType;
import com.autogest.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Repositorio falso de notificaciones, en memoria. Ordena de la más reciente a
 * la más antigua (por id) como el {@code NotificationDao}.
 */
class FakeNotificationRepository extends NotificationRepository {

    private final Map<Integer, Notification> notifications = new LinkedHashMap<>();
    private int sequence;

    FakeNotificationRepository() {
        super(null, null);
    }

    @Override
    public Notification create(Notification notification) {
        notification.setId(++sequence);
        notifications.put(notification.getId(), notification);
        return notification;
    }

    @Override
    public Notification findById(int id) {
        return notifications.get(id);
    }

    @Override
    public List<Notification> findAll() {
        return newestFirst(new ArrayList<>(notifications.values()));
    }

    @Override
    public boolean update(Notification notification) {
        return notifications.replace(notification.getId(), notification) != null;
    }

    @Override
    public boolean delete(int id) {
        return notifications.remove(id) != null;
    }

    @Override
    public List<Notification> search(String text, NotificationType type, Channel channel, NotificationStatus status) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        return newestFirst(notifications.values().stream()
                .filter(n -> needle.isEmpty() || contains(n.getSubject(), needle) || contains(n.getMessage(), needle)
                        || contains(n.getCustomer().getFullName(), needle))
                .filter(n -> type == null || n.getType() == type)
                .filter(n -> channel == null || n.getChannel() == channel)
                .filter(n -> status == null || n.getStatus() == status)
                .toList());
    }

    @Override
    public List<Notification> findByCustomer(int customerId) {
        return newestFirst(notifications.values().stream()
                .filter(n -> n.getCustomer().getId() == customerId).toList());
    }

    @Override
    public List<Notification> findDueScheduled(LocalDateTime now) {
        return newestFirst(notifications.values().stream()
                .filter(n -> n.getStatus() == NotificationStatus.SCHEDULED && !n.getScheduledDate().isAfter(now))
                .toList());
    }

    @Override
    public int countByStatus(NotificationStatus status) {
        return (int) notifications.values().stream().filter(n -> n.getStatus() == status).count();
    }

    @Override
    public Map<Channel, Integer> countGroupedByChannel() {
        Map<Channel, Integer> result = new EnumMap<>(Channel.class);
        for (Channel channel : Channel.values()) {
            result.put(channel, (int) notifications.values().stream().filter(n -> n.getChannel() == channel).count());
        }
        return result;
    }

    @Override
    public Map<NotificationStatus, Integer> countGroupedByStatus() {
        Map<NotificationStatus, Integer> result = new EnumMap<>(NotificationStatus.class);
        for (NotificationStatus status : NotificationStatus.values()) {
            result.put(status, countByStatus(status));
        }
        return result;
    }

    private static List<Notification> newestFirst(List<Notification> list) {
        List<Notification> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparing(Notification::getId).reversed());
        return copy;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
