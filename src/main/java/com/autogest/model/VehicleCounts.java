package com.autogest.model;

/**
 * Indicadores de la pantalla de vehículos, calculados en una sola consulta.
 *
 * @param total          vehículos registrados
 * @param active         vehículos en estado {@link VehicleStatus#ACTIVE}
 * @param inService      vehículos en estado {@link VehicleStatus#IN_SERVICE}
 * @param maintenanceDue vehículos con mantenimiento próximo o vencido
 */
public record VehicleCounts(int total, int active, int inService, int maintenanceDue) {
}
