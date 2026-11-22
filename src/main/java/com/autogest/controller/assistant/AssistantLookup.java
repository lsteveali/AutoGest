package com.autogest.controller.assistant;

import java.util.List;
import java.util.Optional;

/**
 * Búsquedas de solo lectura que necesita el Asistente para convertir una
 * referencia («ABC123», «orden 45», «Ana Gómez») en el id que reciben las
 * pantallas. Ninguna escribe en la base de datos.
 *
 * <p>Principles: DIP (el resolutor no depende de los servicios concretos;
 * las pruebas usan una versión en memoria).</p>
 */
public interface AssistantLookup {

    /** Cliente encontrado: id y nombre completo. */
    record Match(int id, String name) {
    }

    /** Id del vehículo con esa placa (sin importar guiones ni mayúsculas). */
    Optional<Integer> vehicleByPlate(String plate);

    /** Id del propietario del vehículo con esa placa. */
    Optional<Integer> ownerOfPlate(String plate);

    /**
     * Si el propietario del vehículo está activo. «Nueva orden» solo ofrece
     * clientes activos, así que con uno inactivo no se puede preseleccionar.
     */
    default boolean ownerIsActive(String plate) {
        return true;
    }

    /** Id de la orden con ese número («45» y «0045» son la misma). */
    Optional<Integer> orderByNumber(String number);

    /** Id de la factura con ese número («FAC-2026-001»). */
    Optional<Integer> invoiceByNumber(String number);

    /** Clientes cuyo nombre contiene el texto. */
    List<Match> customersByName(String name);
}
