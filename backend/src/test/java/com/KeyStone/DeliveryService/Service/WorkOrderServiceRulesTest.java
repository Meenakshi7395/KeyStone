package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Part.AddPartToWorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.UpdateWorkOrderStatusRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderPartResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderUpdateRequestDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.Part;
import com.KeyStone.DeliveryService.Entity.Site;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.PartRepository;
import com.KeyStone.DeliveryService.Repository.SiteRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderHistoryRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderPartRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderTimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Authorisation and integrity rules the brief says will be tested by calling
 * the API directly (Section 08): cross-customer access, technicians closing
 * jobs, illegal lifecycle jumps, stock never going negative.
 */
class WorkOrderServiceRulesTest {

    private WorkOrderRepository workOrders;
    private PartRepository parts;
    private WorkOrderPartRepository workOrderParts;
    private NotificationService notifications;
    private WorkOrderService service;

    private Customer acme;
    private Customer globex;
    private WorkOrder job;
    private User manager;
    private User dispatcher;
    private User assignedTech;
    private User otherTech;
    private User acmeCustomer;
    private User globexCustomer;

    @BeforeEach
    void setUp() {
        workOrders = mock(WorkOrderRepository.class);
        parts = mock(PartRepository.class);
        workOrderParts = mock(WorkOrderPartRepository.class);
        notifications = mock(NotificationService.class);
        service = new WorkOrderService(
                workOrders,
                mock(CustomerRepository.class),
                mock(SiteRepository.class),
                mock(UserRepository.class),
                mock(WorkOrderHistoryRepository.class),
                parts,
                workOrderParts,
                mock(WorkOrderTimeRepository.class),
                notifications);

        acme = customer(1, "Acme");
        globex = customer(2, "Globex");
        manager = user(10, Role.MANAGER, null);
        dispatcher = user(11, Role.DISPATCHER, null);
        assignedTech = user(20, Role.TECHNICIAN, null);
        otherTech = user(21, Role.TECHNICIAN, null);
        acmeCustomer = user(30, Role.CUSTOMER, acme);
        globexCustomer = user(31, Role.CUSTOMER, globex);

        Site site = new Site();
        site.setId(100);
        site.setName("Tower A");
        site.setCustomer(acme);

        job = new WorkOrder();
        job.setId(42);
        job.setCode("WO-0042");
        job.setTitle("Chiller fault");
        job.setCustomer(acme);
        job.setSite(site);
        job.setTechnician(assignedTech);
        job.setPriority(WorkOrderPriority.HIGH);
        job.setStatus(WorkOrderStatus.ASSIGNED);
        job.setCreatedAt(Instant.now().minus(Duration.ofHours(2)));
        job.setSlaDueDate(Instant.now().plus(Duration.ofDays(1)));

        when(workOrders.findById(42)).thenReturn(Optional.of(job));
    }

    // ---------- cross-customer access ----------

    @Test
    void customerCannotReadAnotherOrganisationsWorkOrder() {
        assertThrows(AccessDeniedException.class, () -> service.getById(globexCustomer, 42));
    }

    @Test
    void customerCanReadOwnWorkOrderButNotInternals() {
        WorkOrderResponseDTO dto = service.getById(acmeCustomer, 42);
        assertEquals("WO-0042", dto.code());
        assertThrows(AccessDeniedException.class, () -> service.getParts(acmeCustomer, 42));
        assertThrows(AccessDeniedException.class, () -> service.getTimeLogs(acmeCustomer, 42));
    }

    @Test
    void technicianCannotReadSomeoneElsesJob() {
        assertThrows(AccessDeniedException.class, () -> service.getById(otherTech, 42));
    }

    // ---------- lifecycle + roles ----------

    @Test
    void assignedTechnicianCanStartJob() {
        WorkOrderResponseDTO dto = service.updateStatus(assignedTech, 42,
                new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.IN_PROGRESS, null));
        assertEquals(WorkOrderStatus.IN_PROGRESS, dto.status());
    }

    @Test
    void otherTechnicianCannotStartJob() {
        assertThrows(AccessDeniedException.class, () -> service.updateStatus(otherTech, 42,
                new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.IN_PROGRESS, null)));
        assertEquals(WorkOrderStatus.ASSIGNED, job.getStatus());
    }

    @Test
    void technicianCannotCloseJob() {
        job.setStatus(WorkOrderStatus.COMPLETED);
        assertThrows(AccessDeniedException.class, () -> service.updateStatus(assignedTech, 42,
                new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.CLOSED, null)));
        assertEquals(WorkOrderStatus.COMPLETED, job.getStatus());
    }

    @Test
    void dispatcherCannotCloseButManagerCan() {
        job.setStatus(WorkOrderStatus.COMPLETED);
        assertThrows(AccessDeniedException.class, () -> service.updateStatus(dispatcher, 42,
                new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.CLOSED, null)));
        WorkOrderResponseDTO dto = service.updateStatus(manager, 42,
                new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.CLOSED, "signed off"));
        assertEquals(WorkOrderStatus.CLOSED, dto.status());
        assertNotNull(job.getClosedAt());
    }

    @Test
    void illegalJumpIsRejectedWithConflict() {
        job.setStatus(WorkOrderStatus.OPEN);
        assertThrows(IllegalStateException.class, () -> service.updateStatus(manager, 42,
                new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.COMPLETED, null)));
        assertEquals(WorkOrderStatus.OPEN, job.getStatus());
    }

    @Test
    void completingRecordsCompletionTimeAndNotifiesManagers() {
        job.setStatus(WorkOrderStatus.IN_PROGRESS);
        service.updateStatus(assignedTech, 42, new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.COMPLETED, null));
        assertNotNull(job.getCompletedAt());
        verify(notifications).notifyRoles(any(), any(), any(), any(), any(), any());
    }

    @Test
    void closedJobIsImmutable() {
        job.setStatus(WorkOrderStatus.CLOSED);
        assertThrows(IllegalStateException.class, () -> service.update(manager, 42,
                new WorkOrderUpdateRequestDTO("New title", null, 100, WorkOrderPriority.LOW, null)));
        assertThrows(IllegalStateException.class, () -> service.updateStatus(manager, 42,
                new UpdateWorkOrderStatusRequestDTO(WorkOrderStatus.IN_PROGRESS, null)));
    }

    // ---------- parts: transactional stock ----------

    @Test
    void loggingPartDecrementsStock() {
        job.setStatus(WorkOrderStatus.IN_PROGRESS);
        Part part = part(7, 5);
        when(parts.findByIdForUpdate(7)).thenReturn(Optional.of(part));
        when(workOrderParts.save(any())).thenAnswer(inv -> inv.getArgument(0));

        WorkOrderPartResponseDTO line = service.addPart(assignedTech, 42, new AddPartToWorkOrderRequestDTO(7, 2));

        assertEquals(3, part.getStockQuantity());
        assertEquals(2, line.quantity());
        assertEquals(new BigDecimal("200.00"), line.lineCost());
    }

    @Test
    void stockCannotGoNegative() {
        job.setStatus(WorkOrderStatus.IN_PROGRESS);
        Part part = part(7, 1);
        when(parts.findByIdForUpdate(7)).thenReturn(Optional.of(part));

        assertThrows(IllegalStateException.class,
                () -> service.addPart(assignedTech, 42, new AddPartToWorkOrderRequestDTO(7, 2)));
        assertEquals(1, part.getStockQuantity());
        verify(workOrderParts, never()).save(any());
    }

    @Test
    void unassignedTechnicianCannotLogParts() {
        job.setStatus(WorkOrderStatus.IN_PROGRESS);
        assertThrows(AccessDeniedException.class,
                () -> service.addPart(otherTech, 42, new AddPartToWorkOrderRequestDTO(7, 1)));
    }

    // ---------- helpers ----------

    private static Customer customer(int id, String name) {
        Customer c = new Customer();
        c.setId(id);
        c.setName(name);
        c.setContactEmail(name.toLowerCase() + "@example.com");
        return c;
    }

    private static User user(int id, Role role, Customer customer) {
        User u = new User();
        u.setId(id);
        u.setName(role + " " + id);
        u.setEmail(id + "@example.com");
        u.setRole(role);
        u.setCustomer(customer);
        return u;
    }

    private static Part part(int id, int stock) {
        Part p = new Part("Contactor", stock);
        p.setId(id);
        p.setUnitCost(new BigDecimal("100.00"));
        return p;
    }
}
