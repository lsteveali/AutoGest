package com.autogest.service;

import com.autogest.model.SystemPreferences;
import com.autogest.model.WorkshopProfile;
import com.autogest.repository.SettingsRepository;

/**
 * Repositorio falso de configuración: guarda en memoria el último perfil y las
 * últimas preferencias, y cuenta cuántas veces se guardó.
 */
class FakeSettingsRepository extends SettingsRepository {

    private WorkshopProfile profile = new WorkshopProfile();
    private SystemPreferences preferences = new SystemPreferences();
    private int saves;

    FakeSettingsRepository() {
        super(null);
    }

    @Override
    public WorkshopProfile loadWorkshopProfile() {
        return profile;
    }

    @Override
    public void saveWorkshopProfile(WorkshopProfile profile) {
        this.profile = profile;
        saves++;
    }

    @Override
    public SystemPreferences loadPreferences() {
        return preferences;
    }

    @Override
    public void savePreferences(SystemPreferences preferences) {
        this.preferences = preferences;
        saves++;
    }

    int saves() {
        return saves;
    }
}
