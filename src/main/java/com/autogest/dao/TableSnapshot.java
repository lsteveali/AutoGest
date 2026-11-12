package com.autogest.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Contenido de una tabla leído para una copia de seguridad: nombre, columnas
 * en orden y filas como texto ({@code null} conserva el valor nulo).
 *
 * @param name    nombre de la tabla
 * @param columns nombres de las columnas, en el orden de la tabla
 * @param rows    filas; cada una tiene un valor por columna
 */
public record TableSnapshot(String name, List<String> columns, List<List<String>> rows) {

    /** Copia defensiva e inmutable (las filas admiten valores nulos). */
    public TableSnapshot {
        columns = List.copyOf(columns);
        rows = rows.stream()
                .map(row -> Collections.unmodifiableList(new ArrayList<>(row)))
                .toList();
    }
}
