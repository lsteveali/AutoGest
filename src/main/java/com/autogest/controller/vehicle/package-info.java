/**
 * Interfaz del módulo de vehículos: la pantalla de lista con sus indicadores y
 * filtros, el formulario compartido entre «nuevo» y «editar», el detalle del
 * vehículo y {@link com.autogest.controller.vehicle.VehicleLabels}, que traduce
 * los valores del dominio a texto en español.
 *
 * <p>Los controladores solo traducen eventos de la interfaz en llamadas al
 * {@code VehicleService} y muestran el resultado; las reglas de negocio viven en
 * la capa de servicio y no se conoce SQL ni los repositorios.</p>
 *
 * <p>Principles: Low Coupling, High Cohesion, Indirection.</p>
 */
package com.autogest.controller.vehicle;
