package com.autogest.controller;

import com.autogest.model.Catalogs.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la matriz de acceso por rol (D-11) de {@link Permissions}. No
 * necesitan JavaFX: la decisión es lógica pura sobre el rol y la vista.
 */
class PermissionsTest {

    @Test
    void administratorCanAccessEveryView() {
        for (View view : View.values()) {
            assertTrue(Permissions.canAccess(Role.ADMINISTRATOR, view),
                    "El administrador debería acceder a " + view);
        }
    }

    @Test
    void homeAndAssistantAreAvailableToEveryRole() {
        for (Role role : Role.values()) {
            assertTrue(Permissions.canAccess(role, View.HOME), "Inicio para " + role);
            assertTrue(Permissions.canAccess(role, View.ASSISTANT), "Asistente para " + role);
        }
    }

    @Test
    void ownProfileIsAvailableToEveryRoleButSettingsIsNot() {
        for (Role role : Role.values()) {
            assertTrue(Permissions.canAccess(role, View.SETTINGS_PROFILE), "Perfil para " + role);
        }
        assertFalse(Permissions.canAccess(Role.RECEPTIONIST, View.SETTINGS));
        assertFalse(Permissions.canAccess(Role.MECHANIC, View.SETTINGS));
        assertFalse(Permissions.canAccess(Role.SUPERVISOR, View.SETTINGS));
    }

    @Test
    void receptionistSeesCustomersVehiclesOrdersAndBillingOnly() {
        assertTrue(Permissions.canAccess(Role.RECEPTIONIST, View.CUSTOMERS));
        assertTrue(Permissions.canAccess(Role.RECEPTIONIST, View.VEHICLES));
        assertTrue(Permissions.canAccess(Role.RECEPTIONIST, View.ORDERS));
        assertTrue(Permissions.canAccess(Role.RECEPTIONIST, View.BILLING));

        assertFalse(Permissions.canAccess(Role.RECEPTIONIST, View.SERVICES));
        assertFalse(Permissions.canAccess(Role.RECEPTIONIST, View.INVENTORY));
        assertFalse(Permissions.canAccess(Role.RECEPTIONIST, View.REPORTS));
        assertFalse(Permissions.canAccess(Role.RECEPTIONIST, View.USERS_ROLES));
    }

    @Test
    void mechanicSeesOrdersAndServicesOnly() {
        assertTrue(Permissions.canAccess(Role.MECHANIC, View.ORDERS));
        assertTrue(Permissions.canAccess(Role.MECHANIC, View.SERVICES));

        assertFalse(Permissions.canAccess(Role.MECHANIC, View.CUSTOMERS));
        assertFalse(Permissions.canAccess(Role.MECHANIC, View.INVENTORY));
        assertFalse(Permissions.canAccess(Role.MECHANIC, View.BILLING));
        assertFalse(Permissions.canAccess(Role.MECHANIC, View.USERS_ROLES));
    }

    @Test
    void supervisorSeesOrdersInventoryAndReportsOnly() {
        assertTrue(Permissions.canAccess(Role.SUPERVISOR, View.ORDERS));
        assertTrue(Permissions.canAccess(Role.SUPERVISOR, View.INVENTORY));
        assertTrue(Permissions.canAccess(Role.SUPERVISOR, View.REPORTS));

        assertFalse(Permissions.canAccess(Role.SUPERVISOR, View.CUSTOMERS));
        assertFalse(Permissions.canAccess(Role.SUPERVISOR, View.SERVICES));
        assertFalse(Permissions.canAccess(Role.SUPERVISOR, View.BILLING));
        assertFalse(Permissions.canAccess(Role.SUPERVISOR, View.USERS_ROLES));
    }

    @Test
    void usersAndRolesAreAdministratorOnly() {
        assertTrue(Permissions.canAccess(Role.ADMINISTRATOR, View.USERS_ROLES));
        assertFalse(Permissions.canAccess(Role.RECEPTIONIST, View.USERS_ROLES));
        assertFalse(Permissions.canAccess(Role.MECHANIC, View.USERS_ROLES));
        assertFalse(Permissions.canAccess(Role.SUPERVISOR, View.USERS_ROLES));
    }

    @Test
    void nullRoleOrViewIsDenied() {
        assertFalse(Permissions.canAccess(null, View.HOME));
        assertFalse(Permissions.canAccess(Role.ADMINISTRATOR, null));
    }
}
