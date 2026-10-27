/**
 * Interfaz del módulo Catálogo de Servicios: lista con indicadores y filtros,
 * formulario compartido entre «nuevo» y «editar», detalle del servicio y
 * {@link com.autogest.controller.catalog.ServiceLabels}, que traduce los valores
 * del dominio a texto en español.
 *
 * <p>Los controladores solo traducen eventos de la interfaz en llamadas a
 * {@code ServiceOfferingService} y muestran el resultado; las reglas de negocio
 * viven en la capa de servicio.</p>
 *
 * <p>Principles: Low Coupling, High Cohesion, Indirection.</p>
 */
package com.autogest.controller.catalog;
