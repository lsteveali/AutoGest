package com.autogest.service.assistant;

import java.util.List;
import java.util.Optional;

/**
 * Lo que el Asistente puede ofrecer a un usuario: los módulos que su rol puede
 * abrir, con las palabras con que se los nombra, los filtros de su lista (con
 * las opciones tal como se ven en pantalla) y si tienen formulario de
 * creación. Lo arma la interfaz para cada rol, así ningún motor propone algo
 * que el usuario no puede ver.
 *
 * @param destinations módulos disponibles para el rol
 */
public record AssistantCatalog(List<Destination> destinations) {

    /** Copia inmutable. */
    public AssistantCatalog {
        destinations = List.copyOf(destinations);
    }

    /**
     * Módulo disponible.
     *
     * @param code          código estable (por ejemplo {@code ORDERS})
     * @param title         nombre visible («Órdenes de servicio»)
     * @param keywords      palabras con que el usuario lo nombra, en minúsculas y sin tildes
     * @param createWords   palabras que piden crear un registro de este módulo
     *                      («nueva orden»); vacía si no tiene formulario de creación
     * @param filters       filtros de la lista (vacía si no tiene lista con filtros)
     * @param formFields    etiquetas de los campos que se pueden precargar al crear
     */
    public record Destination(String code, String title, List<String> keywords, List<String> createWords,
                              List<Filter> filters, List<String> formFields) {

        /** Copias inmutables. */
        public Destination {
            keywords = List.copyOf(keywords);
            createWords = List.copyOf(createWords);
            filters = List.copyOf(filters);
            formFields = List.copyOf(formFields);
        }

        /** Tiene formulario de creación. */
        public boolean canCreate() {
            return !createWords.isEmpty();
        }
    }

    /**
     * Filtro de una lista.
     *
     * @param label   etiqueta visible («Estado»)
     * @param options opciones visibles, sin la de «Todos»
     */
    public record Filter(String label, List<String> options) {

        /** Copia inmutable. */
        public Filter {
            options = List.copyOf(options);
        }
    }

    /**
     * @param code código del módulo
     * @return el módulo, si el rol lo puede abrir
     */
    public Optional<Destination> find(String code) {
        return destinations.stream().filter(d -> d.code().equals(code)).findFirst();
    }
}
