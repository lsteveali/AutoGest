package com.autogest.service;

import com.autogest.dao.SettingsDao;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@link SettingsDao} en memoria para las pruebas: la tabla {@code app_settings}
 * es un mapa ordenado. Cuenta las lecturas para comprobar las copias en memoria.
 */
class MemorySettingsDao extends SettingsDao {

    final Map<String, String> table = new TreeMap<>();
    int reads;

    MemorySettingsDao() {
        super(null);
    }

    @Override
    public Map<String, String> findByPrefix(String prefix) {
        reads++;
        Map<String, String> found = new LinkedHashMap<>();
        table.forEach((key, value) -> {
            if (key.startsWith(prefix)) {
                found.put(key, value);
            }
        });
        return found;
    }

    @Override
    public void saveAll(Map<String, String> values) {
        table.putAll(values);
    }
}
