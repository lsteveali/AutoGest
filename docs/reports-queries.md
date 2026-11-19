# Consultas de apoyo para Reportes (Sprint 7, Desarrollador 1)

El Líder construye la pantalla Reportes con estos métodos (sin DTO: devuelven `Map`).
Todos los periodos validan que existan las dos fechas y que la inicial no sea posterior.

| Service | Método | Devuelve |
|---|---|---|
| ServiceOrderService | `countGroupedByStatus()` | estado → órdenes (todos los estados) |
| ServiceOrderService | `countOrdersBetween(from, to)` | órdenes ingresadas |
| ServiceOrderService | `countOrdersByMonth(from, to)` | `YearMonth` → órdenes |
| ServiceOrderService | `findTopServices(from, to, limit)` | servicio → cantidad (1 a 20) |
| ServiceOrderService | `findTopParts(from, to, limit)` | repuesto → unidades (1 a 20) |
| ServiceOrderService | `countOrdersByMechanic(from, to)` | mecánico → órdenes |
| ServiceOrderService | `estimatedRevenueBetween(from, to)` | `BigDecimal` (órdenes no canceladas) |
| InventoryService | `summarizeMovements(from, to)` | tipo de movimiento → unidades |
| NotificationService | `countByChannel()` | canal → notificaciones |
| NotificationService | `countGroupedByStatus()` | estado → notificaciones |
| NotificationService | `deliveryRate()` | % entregadas sobre intentadas |

Las agregaciones se hacen en SQL (`GROUP BY`) dentro de los DAO (Protected Variations).
