package com.autogest.service;

import com.autogest.exception.NotFoundException;
import com.autogest.exception.ValidationException;
import com.autogest.model.Customer;
import com.autogest.model.Invoice;
import com.autogest.model.InvoiceStatus;
import com.autogest.model.InvoiceSummary;
import com.autogest.model.Notification;
import com.autogest.model.NotificationStatus;
import com.autogest.model.NotificationType;
import com.autogest.model.OrderHistory;
import com.autogest.model.OrderStatus;
import com.autogest.model.Role;
import com.autogest.model.ServiceLine;
import com.autogest.model.ServiceOffering;
import com.autogest.model.ServiceOrder;
import com.autogest.model.User;
import com.autogest.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de {@link InvoiceService} con repositorios falsos de facturas y
 * órdenes y el {@link ServiceOrderService} real (Dev 1) para el detalle.
 */
class InvoiceServiceTest {

    private FakeInvoiceRepository invoices;
    private HistoryOrderRepository orders;
    private InvoiceService service;
    private User receptionist;
    private FakeCustomerRepository customers;
    private FakeNotificationRepository notifications;
    private Customer owner;

    @BeforeEach
    void setUp() {
        invoices = new FakeInvoiceRepository();
        orders = new HistoryOrderRepository();
        ServiceOrderService orderService = new ServiceOrderService(orders, null);
        customers = new FakeCustomerRepository();
        owner = new Customer();
        owner.setDocumentType(com.autogest.model.DocumentType.CC);
        owner.setDocumentNumber("1098456789");
        owner.setFirstName("Juan");
        owner.setLastName("Pérez");
        owner.setEmail("juan@correo.com");
        customers.create(owner);
        notifications = new FakeNotificationRepository();
        // NotificationService real de Dev 1 sobre su repositorio falso.
        NotificationService notificationService = new NotificationService(notifications,
                new CustomerService(customers));
        service = new InvoiceService(invoices, orderService, orders, notificationService);
        receptionist = new User();
        receptionist.setId(7);
        receptionist.setFirstName("Laura");
        receptionist.setLastName("Gómez");
        receptionist.setRole(Role.RECEPTIONIST);
    }

    // ---------- generar ----------

    @Test
    void createsPendingInvoiceFromOrderLines() {
        ServiceOrder order = order(OrderStatus.READY, "120000", "180000");

        Invoice invoice = service.createInvoice(order.getId(), null, null, null, receptionist);

        assertEquals(InvoiceStatus.PENDING, invoice.getStatus());
        assertEquals(new BigDecimal("300000.00"), invoice.getSubtotal());
        assertEquals(new BigDecimal("0.00"), invoice.getTaxes(), "IVA en 0 % (D-05)");
        assertEquals(new BigDecimal("300000.00"), invoice.getTotal());
        assertEquals(LocalDate.now(), invoice.getIssueDate());
        assertEquals(LocalDate.now().plusDays(InvoiceService.DEFAULT_DUE_DAYS), invoice.getDueDate());
        assertEquals(InvoiceService.formatNumber(LocalDate.now().getYear(), 1), invoice.getNumber());
        assertTrue(invoice.getNumber().matches("FAC-\\d{4}-001"), invoice.getNumber());
        assertEquals(invoice, invoices.findByOrder(order.getId()));
    }

    @Test
    void discountIsSubtractedFromTotal() {
        ServiceOrder order = order(OrderStatus.DELIVERED, "120000", "180000");

        Invoice invoice = service.createInvoice(order.getId(), new BigDecimal("10000"), null, null, receptionist);

        assertEquals(new BigDecimal("10000.00"), invoice.getDiscount());
        assertEquals(new BigDecimal("290000.00"), invoice.getTotal());
    }

    @Test
    void discountCannotExceedTotal() {
        ServiceOrder order = order(OrderStatus.READY, "50000");

        assertThrows(ValidationException.class,
                () -> service.createInvoice(order.getId(), new BigDecimal("60000"), null, null, receptionist));
        assertNull(invoices.findByOrder(order.getId()), "No se guarda nada");
    }

    @Test
    void createsPaidInvoiceWhenPaymentIsRegisteredAtOnce() {
        ServiceOrder order = order(OrderStatus.READY, "80000");

        Invoice invoice = service.createInvoice(order.getId(), null, null, "TRANSFER", receptionist);

        assertEquals(InvoiceStatus.PAID, invoice.getStatus());
        assertEquals("TRANSFER", invoice.getPaymentMethod());
        assertTrue(orders.texts.get(0).contains("pagada con Transferencia"), orders.texts.get(0));
    }

    @Test
    void onlyReadyOrDeliveredOrdersCanBeInvoiced() {
        for (OrderStatus status : List.of(OrderStatus.RECEIVED, OrderStatus.DIAGNOSIS, OrderStatus.IN_REPAIR,
                OrderStatus.CANCELLED)) {
            ServiceOrder order = order(status, "50000");
            String message = assertThrows(ValidationException.class,
                    () -> service.createInvoice(order.getId(), null, null, null, receptionist)).getMessage();
            assertTrue(message.contains("Listas o Entregadas"), message);
        }
    }

    @Test
    void orderWithoutLinesCannotBeInvoiced() {
        ServiceOrder order = order(OrderStatus.READY);

        assertThrows(ValidationException.class,
                () -> service.createInvoice(order.getId(), null, null, null, receptionist));
    }

    @Test
    void orderGetsOnlyOneActiveInvoice() {
        ServiceOrder order = order(OrderStatus.READY, "50000");
        Invoice first = service.createInvoice(order.getId(), null, null, null, receptionist);

        String message = assertThrows(ValidationException.class,
                () -> service.createInvoice(order.getId(), null, null, null, receptionist)).getMessage();

        assertTrue(message.contains(first.getNumber()), message);
    }

    @Test
    void voidedInvoiceIsReissuedOnTheSameRowWithANewNumber() {
        ServiceOrder order = order(OrderStatus.READY, "50000");
        Invoice first = service.createInvoice(order.getId(), null, null, null, receptionist);
        String firstNumber = first.getNumber();
        service.voidInvoice(first.getId(), receptionist);

        Invoice again = service.createInvoice(order.getId(), null, null, null, receptionist);

        assertEquals(first.getId(), again.getId(), "La tabla permite una sola factura por orden");
        assertEquals(InvoiceStatus.PENDING, again.getStatus());
        assertTrue(!again.getNumber().equals(firstNumber), "Número nuevo: " + again.getNumber());
    }

    @Test
    void dueDateCannotBeInThePast() {
        ServiceOrder order = order(OrderStatus.READY, "50000");

        assertThrows(ValidationException.class, () -> service.createInvoice(order.getId(), null,
                LocalDate.now().minusDays(1), null, receptionist));
    }

    @Test
    void invalidPaymentMethodIsRejected() {
        ServiceOrder order = order(OrderStatus.READY, "50000");

        assertThrows(ValidationException.class,
                () -> service.createInvoice(order.getId(), null, null, "BITCOIN", receptionist));
        assertNull(invoices.findByOrder(order.getId()));
    }

    @Test
    void requiresUser() {
        ServiceOrder order = order(OrderStatus.READY, "50000");

        assertThrows(ValidationException.class, () -> service.createInvoice(order.getId(), null, null, null, null));
    }

    @Test
    void generatingLeavesAnEntryInTheOrderHistory() {
        ServiceOrder order = order(OrderStatus.READY, "485000");

        Invoice invoice = service.createInvoice(order.getId(), null, null, null, receptionist);

        assertEquals(1, orders.texts.size());
        assertEquals("Factura " + invoice.getNumber() + " generada por $485.000", orders.texts.get(0));
    }

    @Test
    void draftShowsTotalsWithoutSaving() {
        ServiceOrder order = order(OrderStatus.READY, "100000", "20000");

        Invoice draft = InvoiceService.buildDraft(order, new BigDecimal("5000"), null);

        assertEquals(new BigDecimal("115000.00"), draft.getTotal());
        assertEquals(0, draft.getId());
        assertNull(invoices.findByOrder(order.getId()));
    }

    // ---------- órdenes facturables ----------

    @Test
    void billableOrdersAreReadyOrDeliveredWithoutActiveInvoice() {
        ServiceOrder ready = order(OrderStatus.READY, "50000");
        ServiceOrder delivered = order(OrderStatus.DELIVERED, "50000");
        ServiceOrder invoiced = order(OrderStatus.DELIVERED, "50000");
        order(OrderStatus.IN_REPAIR, "50000");
        order(OrderStatus.READY);
        service.createInvoice(invoiced.getId(), null, null, null, receptionist);

        List<Integer> ids = service.findBillableOrders().stream().map(ServiceOrder::getId).toList();

        assertEquals(2, ids.size(), ids.toString());
        assertTrue(ids.contains(ready.getId()));
        assertTrue(ids.contains(delivered.getId()));
    }

    // ---------- pagar y anular ----------

    @Test
    void marksInvoiceAsPaid() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);

        Invoice paid = service.markAsPaid(invoice.getId(), "CASH", receptionist);

        assertEquals(InvoiceStatus.PAID, paid.getStatus());
        assertEquals("CASH", invoices.findById(invoice.getId()).getPaymentMethod());
        assertTrue(orders.texts.get(1).endsWith("pagada con Efectivo"), orders.texts.get(1));
    }

    @Test
    void overdueInvoiceCanBePaid() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);
        service.refreshOverdue(LocalDate.now().plusDays(InvoiceService.DEFAULT_DUE_DAYS + 1));

        assertEquals(InvoiceStatus.PAID, service.markAsPaid(invoice.getId(), "CARD", receptionist).getStatus());
    }

    @Test
    void paidInvoiceCannotBePaidAgainNorVoided() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, "CASH",
                receptionist);

        assertThrows(ValidationException.class, () -> service.markAsPaid(invoice.getId(), "CASH", receptionist));
        assertThrows(ValidationException.class, () -> service.voidInvoice(invoice.getId(), receptionist));
    }

    @Test
    void paymentNeedsAValidMethod() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);

        assertThrows(ValidationException.class, () -> service.markAsPaid(invoice.getId(), null, receptionist));
        assertThrows(ValidationException.class, () -> service.markAsPaid(invoice.getId(), "X", receptionist));
        assertEquals(InvoiceStatus.PENDING, invoices.findById(invoice.getId()).getStatus());
    }

    @Test
    void voidsPendingInvoiceAndRecordsIt() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);

        service.voidInvoice(invoice.getId(), receptionist);

        assertEquals(InvoiceStatus.VOIDED, invoices.findById(invoice.getId()).getStatus());
        assertEquals("Factura " + invoice.getNumber() + " anulada", orders.texts.get(1));
        assertThrows(ValidationException.class, () -> service.voidInvoice(invoice.getId(), receptionist));
    }

    @Test
    void unknownInvoiceIsNotFound() {
        assertThrows(NotFoundException.class, () -> service.markAsPaid(99, "CASH", receptionist));
        assertThrows(NotFoundException.class, () -> service.getDetail(99));
    }

    // ---------- vencidas, consulta e indicadores ----------

    @Test
    void onlyPendingInvoicesPastDueBecomeOverdue() {
        LocalDate later = LocalDate.now().plusDays(InvoiceService.DEFAULT_DUE_DAYS + 1);
        Invoice pending = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);
        Invoice notDueYet = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null,
                later.plusDays(5), null, receptionist);
        Invoice paid = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, "CASH",
                receptionist);

        List<Integer> changed = service.refreshOverdue(later);

        assertEquals(List.of(pending.getId()), changed);
        assertEquals(InvoiceStatus.OVERDUE, invoices.findById(pending.getId()).getStatus());
        assertEquals(InvoiceStatus.PENDING, invoices.findById(notDueYet.getId()).getStatus());
        assertEquals(InvoiceStatus.PAID, invoices.findById(paid.getId()).getStatus());
    }

    @Test
    void summaryCountsStatusesAndAmounts() {
        service.createInvoice(order(OrderStatus.READY, "100000").getId(), null, null, "CASH", receptionist);
        service.createInvoice(order(OrderStatus.READY, "40000").getId(), null, null, null, receptionist);
        Invoice voided = service.createInvoice(order(OrderStatus.READY, "70000").getId(), null, null, null,
                receptionist);
        service.voidInvoice(voided.getId(), receptionist);

        InvoiceSummary summary = service.getSummary();

        assertEquals(3, summary.total());
        assertEquals(1, summary.paid());
        assertEquals(1, summary.pending());
        assertEquals(0, new BigDecimal("140000").compareTo(summary.billedAmount()), "Sin las anuladas");
        assertEquals(0, new BigDecimal("100000").compareTo(summary.paidAmount()));
        assertEquals(0, new BigDecimal("40000").compareTo(summary.pendingAmount()));
    }

    @Test
    void monthSummaryOnlyCountsInvoicesIssuedThisMonth() {
        Invoice old = service.createInvoice(order(OrderStatus.READY, "100000").getId(), null, null, null,
                receptionist);
        old.setIssueDate(LocalDate.now().withDayOfMonth(1).minusDays(1));
        service.createInvoice(order(OrderStatus.READY, "40000").getId(), null, null, null, receptionist);

        InvoiceSummary month = service.getMonthSummary(LocalDate.now());

        assertEquals(1, month.total());
        assertEquals(0, new BigDecimal("40000").compareTo(month.billedAmount()));
    }

    @Test
    void detailBringsTheFullOrder() {
        ServiceOrder order = order(OrderStatus.READY, "50000", "25000");
        Invoice invoice = service.createInvoice(order.getId(), null, null, null, receptionist);

        Invoice detail = service.getDetail(invoice.getId());

        assertEquals(2, detail.getOrder().getLines().size());
        assertEquals("Juan Pérez", detail.getOrder().getVehicle().getOwner().getFullName());
    }

    @Test
    void searchFiltersByStatusAndMethod() {
        service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, "CASH", receptionist);
        service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null, receptionist);

        assertEquals(1, service.searchInvoices("", InvoiceStatus.PAID, null, null).size());
        assertEquals(1, service.searchInvoices(null, null, null, "CASH").size());
        assertEquals(2, service.searchInvoices("ABC123", null, LocalDate.now(), null).size());
        assertEquals(0, service.searchInvoices(null, null, LocalDate.now().plusDays(1), null).size());
    }

    // ---------- avisos al cliente (NotificationService de Dev 1) ----------

    @Test
    void generatingSendsInvoiceIssued() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);

        List<Notification> sent = notifications.findAll();
        assertEquals(1, sent.size());
        assertEquals(NotificationType.BILLING, sent.get(0).getType());
        assertEquals(NotificationStatus.SENT, sent.get(0).getStatus());
        assertEquals("Factura " + invoice.getNumber() + " disponible", sent.get(0).getSubject());
    }

    @Test
    void payingSendsPaymentReceived() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);

        service.markAsPaid(invoice.getId(), "CASH", receptionist);

        assertEquals(2, notifications.findAll().size());
        assertTrue(subjects().contains("Pago recibido de la factura " + invoice.getNumber()), subjects().toString());
    }

    @Test
    void payingAtOnceSendsOnlyPaymentReceived() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, "CARD",
                receptionist);

        assertEquals(List.of("Pago recibido de la factura " + invoice.getNumber()), subjects());
    }

    @Test
    void overdueNoticeIsSentOnlyOnce() {
        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);
        LocalDate later = LocalDate.now().plusDays(InvoiceService.DEFAULT_DUE_DAYS + 1);

        service.refreshOverdue(later);
        service.refreshOverdue(later);

        long overdue = subjects().stream().filter(s -> s.equals("Factura " + invoice.getNumber() + " vencida")).count();
        assertEquals(1, overdue, subjects().toString());
    }

    @Test
    void customerWithoutEmailLeavesAFailedNoticeButTheInvoiceIsSaved() {
        owner.setEmail(null);

        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);

        assertEquals(invoice, invoices.findById(invoice.getId()));
        assertEquals(NotificationStatus.FAILED, notifications.findAll().get(0).getStatus());
    }

    @Test
    void noticeErrorsDoNotUndoTheInvoice() {
        owner.setId(999);

        Invoice invoice = service.createInvoice(order(OrderStatus.READY, "50000").getId(), null, null, null,
                receptionist);

        assertEquals(InvoiceStatus.PENDING, invoices.findById(invoice.getId()).getStatus());
        assertTrue(notifications.findAll().isEmpty(), "El cliente no existe: no hay aviso, pero sí factura");
    }

    private List<String> subjects() {
        return notifications.findAll().stream().map(Notification::getSubject).toList();
    }

    @Test
    void methodNamesAreInSpanish() {
        assertEquals("Efectivo", InvoiceService.methodText("CASH"));
        assertEquals("Tarjeta", InvoiceService.methodText("CARD"));
        assertEquals("Transferencia", InvoiceService.methodText("TRANSFER"));
        assertEquals("Otro", InvoiceService.methodText("OTHER"));
    }

    // ---------- ayudas ----------

    /** Orden guardada en el repositorio falso, con un servicio por cada precio. */
    private ServiceOrder order(OrderStatus status, String... prices) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(5);
        vehicle.setMake("Toyota");
        vehicle.setModel("Corolla");
        vehicle.setLicensePlate("ABC123");
        vehicle.setOwner(owner);

        ServiceOrder order = new ServiceOrder();
        order.setNumber(String.format("%04d", orders.nextSequence()));
        order.setEntryDate(LocalDate.now());
        order.setStatus(status);
        order.setVehicle(vehicle);
        for (String price : prices) {
            ServiceOffering offering = new ServiceOffering();
            offering.setName("Servicio " + price);
            ServiceLine line = new ServiceLine();
            line.setServiceOffering(offering);
            line.setQuantity(1);
            line.setUnitPrice(new BigDecimal(price));
            order.addLine(line);
        }
        orders.create(order);
        // Las facturas falsas guardan la orden: así la búsqueda por placa y cliente funciona.
        return order;
    }

    /** Repositorio falso de órdenes que además guarda el texto del historial. */
    private static final class HistoryOrderRepository extends FakeServiceOrderRepository {

        final List<String> texts = new ArrayList<>();

        @Override
        public void addHistory(int orderId, OrderHistory entry) {
            texts.add(entry.getDescription());
        }
    }
}
