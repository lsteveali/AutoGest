package com.autogest.controller.assistant;

import com.autogest.controller.Permissions;
import com.autogest.controller.View;
import com.autogest.controller.assistant.AssistantAction.Plan;
import com.autogest.controller.assistant.AssistantDestinations.Entry;
import com.autogest.model.Catalogs.Role;
import com.autogest.service.assistant.AssistantIntent;
import com.autogest.service.assistant.LocalAssistantInterpreter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Convierte una {@link AssistantIntent} en un {@link Plan}: el mensaje para
 * el usuario y las acciones de navegación. Busca (solo leyendo) los registros
 * a los que se refiere el pedido y descarta cualquier pantalla que el rol no
 * pueda abrir.
 *
 * <p>Nunca guarda ni cambia datos: al crear, abre el formulario precargado y
 * el usuario decide si pulsa «Guardar».</p>
 *
 * <p>Principles: Information Expert, Indirection (las búsquedas van por
 * {@link AssistantLookup}).</p>
 */
public class AssistantResolver {

    /** Ejemplos que se muestran con la ayuda y cuando no se entiende un pedido. */
    public static final List<String> EXAMPLES = List.of(
            "órdenes en reparación",
            "facturas vencidas de este mes",
            "abre el vehículo ABC123",
            "nueva orden para ABC123",
            "repuestos con stock bajo",
            "enviar aviso de promoción para Ana Gómez");

    private static final String NOT_SAVED =
            " Revisa los datos y pulsa «Guardar»: no se guarda nada hasta que lo confirmes.";

    private final AssistantLookup lookup;

    /**
     * @param lookup búsquedas de solo lectura
     */
    public AssistantResolver(AssistantLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * @param intent pedido interpretado
     * @param role   rol del usuario en sesión
     * @return mensaje y acciones permitidas para el rol
     */
    public Plan resolve(AssistantIntent intent, Role role) {
        Plan plan = switch (intent.kind()) {
            case HELP -> help();
            case UNKNOWN -> unknown(intent.message());
            case OPEN -> open(intent);
            case FILTER -> filter(intent);
            case RECORD -> record(intent);
            case CREATE -> create(intent);
        };
        return allowed(plan, role);
    }

    // ---------- tipos de pedido ----------

    private static Plan help() {
        return Plan.message("Puedo llevarte a cualquier módulo, abrir listas ya filtradas, abrir un registro "
                + "por su placa o número y dejarte formularios precargados (sin guardar nada). Prueba con: «"
                + String.join("», «", EXAMPLES) + "».");
    }

    private static Plan unknown(String message) {
        String text = message != null ? message
                : "No entendí el pedido. Puedes nombrar un módulo, un estado, una placa o un número de orden.";
        return Plan.message(text + " Por ejemplo: «" + EXAMPLES.get(0) + "» o «" + EXAMPLES.get(2) + "».");
    }

    private static Plan open(AssistantIntent intent) {
        Entry entry = entry(intent);
        String title = entry.main().getTitle();
        return new Plan("Abriendo " + title + ".", List.of(AssistantAction.open("Abrir " + title, entry.main())));
    }

    private static Plan filter(AssistantIntent intent) {
        Entry entry = entry(intent);
        String title = entry.main().getTitle();
        String description = describe(intent.filters(), intent.search());
        AssistantAction action = new AssistantAction("Ver " + title + " " + description, entry.main(), null,
                intent.search(), intent.filters(), Map.of());
        return new Plan("Abriendo " + title + " " + description + ".", List.of(action));
    }

    private Plan record(AssistantIntent intent) {
        Entry entry = entry(intent);
        String reference = intent.reference();
        Optional<Integer> id;
        String what;
        switch (entry.code()) {
            case LocalAssistantInterpreter.VEHICLES -> {
                id = lookup.vehicleByPlate(reference);
                what = "el vehículo " + reference;
            }
            case LocalAssistantInterpreter.ORDERS -> {
                id = lookup.orderByNumber(reference);
                what = "la orden #" + reference;
            }
            case LocalAssistantInterpreter.BILLING -> {
                id = lookup.invoiceByNumber(reference);
                what = "la factura " + reference;
            }
            case LocalAssistantInterpreter.CUSTOMERS -> {
                List<AssistantLookup.Match> matches = lookup.customersByName(reference);
                if (matches.size() == 1) {
                    AssistantLookup.Match match = matches.get(0);
                    return new Plan("Abriendo el cliente " + match.name() + ".",
                            List.of(AssistantAction.record("Abrir " + match.name(), entry.detail(), match.id())));
                }
                String message = matches.isEmpty()
                        ? "No encontré un cliente llamado «" + reference + "». Te muestro la búsqueda en Clientes."
                        : "Encontré " + matches.size() + " clientes con «" + reference + "». Te muestro la lista.";
                return new Plan(message, List.of(searchAction(entry, reference)));
            }
            default -> {
                return open(intent);
            }
        }
        if (id.isPresent() && entry.detail() != null) {
            return new Plan("Abriendo " + what + ".",
                    List.of(AssistantAction.record("Abrir " + what, entry.detail(), id.get())));
        }
        return new Plan("No encontré " + what + ". Te muestro la búsqueda en " + entry.main().getTitle() + ".",
                List.of(searchAction(entry, reference)));
    }

    private Plan create(AssistantIntent intent) {
        Entry entry = entry(intent);
        if (entry.create() == null) {
            return open(intent);
        }
        Map<String, String> data = intent.fields();
        String title = entry.create().getTitle();
        List<AssistantAction> actions = new ArrayList<>();
        StringBuilder message = new StringBuilder("Te abro «" + title + "»");
        Map<String, String> fields = new LinkedHashMap<>();
        Integer parameter = null;

        switch (entry.code()) {
            case LocalAssistantInterpreter.ORDERS -> {
                String plate = data.get(LocalAssistantInterpreter.FIELD_PLATE);
                if (plate != null) {
                    Optional<Integer> vehicle = lookup.vehicleByPlate(plate);
                    if (vehicle.isPresent() && !lookup.ownerIsActive(plate)) {
                        // «Nueva orden» solo ofrece clientes activos: se explica en vez de abrirla vacía.
                        Optional<Integer> owner = lookup.ownerOfPlate(plate);
                        List<AssistantAction> customer = owner.map(id -> List.of(AssistantAction.record(
                                "Abrir el propietario de " + plate, View.CUSTOMER_DETAIL, id))).orElse(List.of());
                        return new Plan("El propietario del vehículo " + plate + " está inactivo, y las órdenes "
                                + "nuevas solo admiten clientes activos. Actívalo en su ficha y vuelve a pedírmelo.",
                                customer);
                    }
                    if (vehicle.isPresent()) {
                        parameter = vehicle.get();
                        message.append(" con el vehículo ").append(plate);
                    } else {
                        message.append(". No encontré el vehículo ").append(plate)
                                .append(", así que va sin vehículo; también puedes registrarlo");
                        actions.add(new AssistantAction("Registrar el vehículo " + plate, View.VEHICLE_NEW, null,
                                null, Map.of(), Map.of("Placa", plate)));
                    }
                }
            }
            case LocalAssistantInterpreter.BILLING -> {
                String order = data.get(LocalAssistantInterpreter.FIELD_ORDER);
                if (order != null) {
                    Optional<Integer> id = lookup.orderByNumber(order);
                    if (id.isPresent()) {
                        parameter = id.get();
                        message.append(" con la orden #").append(order);
                    } else {
                        message.append(". No encontré la orden #").append(order).append("; elígela en la lista");
                    }
                }
            }
            case "NOTIFICATIONS" -> {
                Optional<Integer> customer = customerFor(data);
                if (customer.isPresent()) {
                    parameter = customer.get();
                    message.append(" para ").append(customerText(data));
                } else if (data.containsKey(LocalAssistantInterpreter.FIELD_CUSTOMER)
                        || data.containsKey(LocalAssistantInterpreter.FIELD_PLATE)) {
                    message.append(". No encontré a ").append(customerText(data)).append("; elige el cliente");
                }
                copy(data, fields, "Tipo");
                copy(data, fields, "Canal");
                if (data.containsKey(LocalAssistantInterpreter.FIELD_MESSAGE)) {
                    fields.put("Mensaje", data.get(LocalAssistantInterpreter.FIELD_MESSAGE));
                }
            }
            case LocalAssistantInterpreter.VEHICLES -> {
                if (data.containsKey(LocalAssistantInterpreter.FIELD_PLATE)) {
                    fields.put("Placa", data.get(LocalAssistantInterpreter.FIELD_PLATE));
                }
                if (data.containsKey(LocalAssistantInterpreter.FIELD_CUSTOMER)) {
                    fields.put("Propietario", data.get(LocalAssistantInterpreter.FIELD_CUSTOMER));
                }
            }
            case LocalAssistantInterpreter.CUSTOMERS -> {
                String name = data.get(LocalAssistantInterpreter.FIELD_CUSTOMER);
                if (name != null) {
                    String[] parts = name.trim().split("\\s+", 2);
                    fields.put("Nombres", parts[0]);
                    if (parts.length > 1) {
                        fields.put("Apellidos", parts[1]);
                    }
                }
            }
            default -> {
                // Formulario sin datos que precargar.
            }
        }
        message.append(".").append(NOT_SAVED);
        actions.add(0, new AssistantAction("Abrir «" + title + "»", entry.create(), parameter, null, Map.of(),
                fields));
        return new Plan(message.toString(), actions);
    }

    // ---------- ayudas ----------

    private Optional<Integer> customerFor(Map<String, String> data) {
        String name = data.get(LocalAssistantInterpreter.FIELD_CUSTOMER);
        if (name != null) {
            List<AssistantLookup.Match> matches = lookup.customersByName(name);
            if (matches.size() == 1) {
                return Optional.of(matches.get(0).id());
            }
        }
        String plate = data.get(LocalAssistantInterpreter.FIELD_PLATE);
        return plate == null ? Optional.empty() : lookup.ownerOfPlate(plate);
    }

    private static String customerText(Map<String, String> data) {
        String name = data.get(LocalAssistantInterpreter.FIELD_CUSTOMER);
        return name != null ? name : "el propietario de " + data.get(LocalAssistantInterpreter.FIELD_PLATE);
    }

    private static void copy(Map<String, String> from, Map<String, String> to, String key) {
        if (from.containsKey(key)) {
            to.put(key, from.get(key));
        }
    }

    private static AssistantAction searchAction(Entry entry, String text) {
        return new AssistantAction("Buscar «" + text + "» en " + entry.main().getTitle(), entry.main(), null, text,
                Map.of(), Map.of());
    }

    /** «con Estado: En reparación y la búsqueda «ABC123»». */
    static String describe(Map<String, String> filters, String search) {
        StringJoiner parts = new StringJoiner(", ");
        filters.forEach((label, option) -> parts.add(label + ": " + option));
        if (search != null && !search.isBlank()) {
            parts.add("búsqueda «" + search + "»");
        }
        return parts.length() == 0 ? "" : "con " + parts;
    }

    private static Entry entry(AssistantIntent intent) {
        return AssistantDestinations.find(intent.destination())
                .orElseThrow(() -> new IllegalArgumentException("Módulo desconocido: " + intent.destination()));
    }

    /** Quita las acciones que el rol no puede abrir; si no queda ninguna, lo explica. */
    private static Plan allowed(Plan plan, Role role) {
        List<AssistantAction> allowed = plan.actions().stream()
                .filter(action -> Permissions.canAccess(role, action.view()))
                .toList();
        if (allowed.size() == plan.actions().size()) {
            return plan;
        }
        if (allowed.isEmpty()) {
            return Plan.message("Tu rol no tiene acceso a esa sección, así que no puedo abrirla.");
        }
        return new Plan(plan.message(), allowed);
    }
}
