package com.autogest.service;

import com.autogest.model.Role;
import com.autogest.model.User;
import com.autogest.model.UserStatus;
import com.autogest.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repositorio falso de usuarios para las pruebas de los Services, sin base de
 * datos. Extiende {@link UserRepository} (clase concreta no final) y guarda los
 * usuarios en memoria. Se pasa un DAO {@code null} porque la subclase nunca
 * delega en el DAO real.
 */
class FakeUserRepository extends UserRepository {

    private final Map<Integer, User> users = new LinkedHashMap<>();
    private final Set<Integer> lastAccessUpdates = new HashSet<>();
    private int sequence;

    FakeUserRepository() {
        super(null);
    }

    @Override
    public User create(User user) {
        user.setId(++sequence);
        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User findById(int id) {
        return users.get(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public boolean update(User user) {
        if (!users.containsKey(user.getId())) {
            return false;
        }
        users.put(user.getId(), user);
        return true;
    }

    @Override
    public boolean delete(int id) {
        User user = users.get(id);
        if (user == null) {
            return false;
        }
        user.setStatus(UserStatus.INACTIVE);
        return true;
    }

    @Override
    public User findByEmail(String email) {
        return users.values().stream()
                .filter(u -> equalsIgnoreCase(u.getEmail(), email))
                .findFirst().orElse(null);
    }

    @Override
    public User findByUsername(String username) {
        return users.values().stream()
                .filter(u -> equalsIgnoreCase(u.getUsername(), username))
                .findFirst().orElse(null);
    }

    @Override
    public List<User> search(String text, Role role, UserStatus status) {
        String needle = text == null ? "" : text.trim().toLowerCase();
        List<User> result = new ArrayList<>();
        for (User u : users.values()) {
            boolean matchesText = needle.isEmpty()
                    || u.getFullName().toLowerCase().contains(needle)
                    || containsIgnoreCase(u.getEmail(), needle)
                    || containsIgnoreCase(u.getUsername(), needle);
            boolean matchesRole = role == null || u.getRole() == role;
            boolean matchesStatus = status == null || u.getStatus() == status;
            if (matchesText && matchesRole && matchesStatus) {
                result.add(u);
            }
        }
        return result;
    }

    @Override
    public List<User> findByRoleAndStatus(Role role, UserStatus status) {
        return users.values().stream()
                .filter(u -> u.getRole() == role && u.getStatus() == status)
                .toList();
    }

    @Override
    public int countAll() {
        return users.size();
    }

    @Override
    public int countByStatus(UserStatus status) {
        return (int) users.values().stream().filter(u -> u.getStatus() == status).count();
    }

    @Override
    public int countByRole(Role role) {
        return (int) users.values().stream().filter(u -> u.getRole() == role).count();
    }

    @Override
    public int countByRoleAndStatus(Role role, UserStatus status) {
        return (int) users.values().stream()
                .filter(u -> u.getRole() == role && u.getStatus() == status).count();
    }

    @Override
    public boolean updateLastAccess(int id) {
        lastAccessUpdates.add(id);
        return users.containsKey(id);
    }

    /** Indica si se registró el último acceso del usuario indicado. */
    boolean wasLastAccessUpdated(int id) {
        return lastAccessUpdates.contains(id);
    }

    private static boolean equalsIgnoreCase(String value, String other) {
        return value != null && other != null && value.equalsIgnoreCase(other);
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
