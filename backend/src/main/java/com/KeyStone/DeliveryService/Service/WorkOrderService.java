package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Part.AddPartToWorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.AssignTechnicianRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.LogWorkOrderTimeRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.UpdateWorkOrderStatusRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderFilter;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderHistoryResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderPartResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderTimeResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderTotalsDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderUpdateRequestDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.Part;
import com.KeyStone.DeliveryService.Entity.Site;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Entity.WorkOrderHistory;
import com.KeyStone.DeliveryService.Entity.WorkOrderPart;
import com.KeyStone.DeliveryService.Entity.WorkOrderTime;
import com.KeyStone.DeliveryService.Enum.NotificationType;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.PartRepository;
import com.KeyStone.DeliveryService.Repository.SiteRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderHistoryRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderPartRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderTimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * All work-order business rules live here: creation, the guarded lifecycle,
 * dispatch, parts/time logging and the ownership checks that stop one
 * customer or technician reaching another's data. Controllers stay thin.
 */
@Service
public class WorkOrderService {

    private static final Logger log = LoggerFactory.getLogger(WorkOrderService.class);

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
    private final PartRepository partRepository;
    private final WorkOrderPartRepository workOrderPartRepository;
    private final WorkOrderTimeRepository workOrderTimeRepository;
    private final NotificationService notificationService;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository,
            WorkOrderHistoryRepository workOrderHistoryRepository,
            PartRepository partRepository,
            WorkOrderPartRepository workOrderPartRepository,
            WorkOrderTimeRepository workOrderTimeRepository,
            NotificationService notificationService) {
        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.workOrderHistoryRepository = workOrderHistoryRepository;
        this.partRepository = partRepository;
        this.workOrderPartRepository = workOrderPartRepository;
        this.workOrderTimeRepository = workOrderTimeRepository;
        this.notificationService = notificationService;
    }

    // =========================================
    // CREATE
    // =========================================

    @Transactional
    public WorkOrderResponseDTO create(User caller, WorkOrderRequestDTO request) {

        Integer customerId = request.customerId();
        boolean isCustomer = caller.getRole() == Role.CUSTOMER;

        // A customer can only raise requests for its own organisation (F9.1).
        if (isCustomer) {
            if (caller.getCustomer() == null) {
                throw new IllegalStateException("Link your account to an organisation before raising a request");
            }
            customerId = caller.getCustomer().getId();
        }

        final Integer finalCustomerId = customerId;
        Customer customer = customerRepository.findById(finalCustomerId)
                .orElseThrow(() -> new NoSuchElementException("Customer not found: " + finalCustomerId));

        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> new NoSuchElementException("Site not found: " + request.siteId()));

        if (!site.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Site does not belong to this customer");
        }

        Instant now = Instant.now();
        Instant due = request.slaDueDate();
        if (isCustomer || due == null) {
            // Customers can't pick their own deadline; everyone else may override.
            due = SlaCalculator.defaultDueDate(request.priority(), now);
        } else if (!due.isAfter(now)) {
            throw new IllegalArgumentException("SLA due date must be in the future");
        }

        WorkOrder wo = new WorkOrder();
        wo.setTitle(request.title().trim());
        wo.setDescription(request.description());
        wo.setCustomer(customer);
        wo.setSite(site);
        wo.setPriority(request.priority());
        wo.setStatus(WorkOrderStatus.OPEN);
        wo.setSlaDueDate(due);

        WorkOrder saved = workOrderRepository.save(wo);
        saved.setCode(formatCode(saved.getId()));

        saveHistory(saved, caller, "WORK_ORDER_CREATED", null, saved.getTitle(),
                isCustomer ? "Raised through the customer portal" : null);

        log.info("Work order {} created by user {} for customer {}", saved.getCode(), caller.getId(), customer.getId());
        return toResponse(saved);
    }

    public static String formatCode(Integer id) {
        return String.format("WO-%04d", id);
    }

    // =========================================
    // LIST / GET
    // =========================================

    /** Filtered, paginated and role-scoped (technicians see their jobs, customers their organisation's). */
    @Transactional(readOnly = true)
    public Page<WorkOrderResponseDTO> list(User caller, WorkOrderFilter filter, Pageable pageable) {
        WorkOrderFilter f = filter != null ? filter : WorkOrderFilter.empty();
        return workOrderRepository.findAll(WorkOrderSpecifications.build(caller, f), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponseDTO> getByCustomer(User caller, Integer customerId, Pageable pageable) {
        assertCustomerOwnership(caller, customerId);
        if (!customerRepository.existsById(customerId)) {
            throw new NoSuchElementException("Customer not found: " + customerId);
        }
        return workOrderRepository.findByCustomerId(customerId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<WorkOrderResponseDTO> getByTechnician(User caller, Integer technicianId, Pageable pageable) {
        assertTechnicianOwnership(caller, technicianId);
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new NoSuchElementException("Technician not found: " + technicianId));
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("Selected user is not a technician");
        }
        return workOrderRepository.findByTechnicianId(technicianId, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public WorkOrderResponseDTO getById(User caller, Integer id) {
        WorkOrder wo = find(id);
        assertCanViewWorkOrder(caller, wo);
        return toResponse(wo);
    }

    // =========================================
    // EDIT (F3.3 — editable while open, immutable once closed/cancelled)
    // =========================================

    @Transactional
    public WorkOrderResponseDTO update(User caller, Integer id, WorkOrderUpdateRequestDTO request) {
        WorkOrder wo = find(id);

        if (wo.getStatus().isTerminal()) {
            throw new IllegalStateException("A " + wo.getStatus() + " work order can't be edited");
        }

        Site site = siteRepository.findById(request.siteId())
                .orElseThrow(() -> new NoSuchElementException("Site not found: " + request.siteId()));
        if (!site.getCustomer().getId().equals(wo.getCustomer().getId())) {
            throw new IllegalArgumentException("Site does not belong to this work order's customer");
        }

        List<String> changes = new ArrayList<>();
        String newTitle = request.title().trim();
        if (!newTitle.equals(wo.getTitle())) {
            changes.add("title");
            wo.setTitle(newTitle);
        }
        if (!Objects.equals(request.description(), wo.getDescription())) {
            changes.add("description");
            wo.setDescription(request.description());
        }
        if (!site.getId().equals(wo.getSite().getId())) {
            changes.add("site → " + site.getName());
            wo.setSite(site);
        }
        if (request.priority() != wo.getPriority()) {
            changes.add("priority " + wo.getPriority() + " → " + request.priority());
            wo.setPriority(request.priority());
        }
        if (request.slaDueDate() != null && !request.slaDueDate().equals(wo.getSlaDueDate())) {
            changes.add("SLA due date");
            wo.setSlaDueDate(request.slaDueDate());
            // New deadline → the SLA monitor may alert again.
            wo.setSlaRiskNotifiedAt(null);
            wo.setSlaBreachNotifiedAt(null);
        }

        if (!changes.isEmpty()) {
            saveHistory(wo, caller, "WORK_ORDER_UPDATED", null, String.join(", ", changes), null);
            log.info("Work order {} edited by user {}: {}", wo.getCode(), caller.getId(), changes);
        }
        return toResponse(wo);
    }

    // =========================================
    // ASSIGN (F4)
    // =========================================

    @Transactional
    public WorkOrderResponseDTO assignTechnician(User caller, Integer workOrderId, AssignTechnicianRequestDTO request) {
        WorkOrder wo = find(workOrderId);

        // Reassignment is allowed while the job is open (F4.3).
        if (wo.getStatus().isFinished()) {
            throw new IllegalStateException("Cannot assign a " + wo.getStatus() + " work order");
        }

        User technician = userRepository.findById(request.technicianId())
                .orElseThrow(() -> new NoSuchElementException("Technician not found: " + request.technicianId()));
        if (technician.getRole() != Role.TECHNICIAN) {
            throw new IllegalArgumentException("Selected user is not a technician");
        }
        if (wo.getTechnician() != null && wo.getTechnician().getId().equals(technician.getId())) {
            throw new IllegalStateException("Work order is already assigned to " + technician.getName());
        }

        String oldTechnician = wo.getTechnician() != null ? wo.getTechnician().getName() : null;
        wo.setTechnician(technician);

        // Assignment moves an OPEN order to ASSIGNED (F4.2).
        if (wo.getStatus() == WorkOrderStatus.OPEN) {
            wo.setStatus(WorkOrderStatus.ASSIGNED);
            saveHistory(wo, caller, "STATUS_CHANGED", WorkOrderStatus.OPEN.name(), WorkOrderStatus.ASSIGNED.name(), null);
        }
        saveHistory(wo, caller, oldTechnician == null ? "TECHNICIAN_ASSIGNED" : "TECHNICIAN_REASSIGNED",
                oldTechnician, technician.getName(), null);

        notificationService.notify(technician, NotificationType.ASSIGNED,
                "New job " + wo.getCode(),
                wo.getTitle() + " at " + wo.getSite().getName() + " (" + wo.getPriority() + ")",
                wo);

        log.info("Work order {} assigned to technician {} by user {}", wo.getCode(), technician.getId(), caller.getId());
        return toResponse(wo);
    }

    // =========================================
    // STATUS (Section 07 — guarded state machine)
    // =========================================

    @Transactional
    public WorkOrderResponseDTO updateStatus(User caller, Integer workOrderId, UpdateWorkOrderStatusRequestDTO request) {
        WorkOrder wo = find(workOrderId);
        WorkOrderStatus from = wo.getStatus();
        WorkOrderStatus to = request.status();

        // Role check first so an unauthorised caller learns nothing about the job's state.
        String denial = WorkOrderLifecycle.denialReason(caller.getRole(), isAssignedTechnician(caller, wo), from, to);
        if (denial != null) {
            if (to == WorkOrderStatus.ASSIGNED) {
                throw new IllegalStateException(denial);
            }
            throw new AccessDeniedException(denial);
        }
        WorkOrderLifecycle.requireAllowed(from, to);

        Instant now = Instant.now();
        wo.setStatus(to);
        if (to == WorkOrderStatus.COMPLETED) {
            wo.setCompletedAt(now);
        } else if (to == WorkOrderStatus.CLOSED) {
            wo.setClosedAt(now);
        } else if (from == WorkOrderStatus.COMPLETED && to == WorkOrderStatus.IN_PROGRESS) {
            wo.setCompletedAt(null); // reopened
        }

        saveHistory(wo, caller, "STATUS_CHANGED", from.name(), to.name(), blankToNull(request.note()));
        log.info("Work order {} status {} -> {} by user {}", wo.getCode(), from, to, caller.getId());

        if (to == WorkOrderStatus.COMPLETED) {
            notificationService.notifyRoles(List.of(Role.MANAGER), null, NotificationType.COMPLETED,
                    wo.getCode() + " completed", wo.getTitle() + " is ready for sign-off", wo);
        } else if (wo.getTechnician() != null && !wo.getTechnician().getId().equals(caller.getId())
                && (to == WorkOrderStatus.CANCELLED || to == WorkOrderStatus.ON_HOLD
                || (from == WorkOrderStatus.COMPLETED && to == WorkOrderStatus.IN_PROGRESS))) {
            notificationService.notify(wo.getTechnician(), NotificationType.STATUS_CHANGED,
                    wo.getCode() + " is now " + to, wo.getTitle(), wo);
        }
        return toResponse(wo);
    }

    /** Statuses the caller may move this job to right now (drives the UI's action buttons). */
    @Transactional(readOnly = true)
    public List<WorkOrderStatus> allowedNextStatuses(User caller, Integer workOrderId) {
        WorkOrder wo = find(workOrderId);
        assertCanViewWorkOrder(caller, wo);
        boolean assigned = isAssignedTechnician(caller, wo);
        return WorkOrderLifecycle.nextStates(wo.getStatus()).stream()
                .filter(to -> to != WorkOrderStatus.ASSIGNED)
                .filter(to -> WorkOrderLifecycle.denialReason(caller.getRole(), assigned, wo.getStatus(), to) == null)
                .sorted()
                .toList();
    }

    // =========================================
    // HISTORY (append-only)
    // =========================================

    @Transactional(readOnly = true)
    public List<WorkOrderHistoryResponseDTO> getHistory(User caller, Integer workOrderId) {
        WorkOrder wo = find(workOrderId);
        assertCanViewWorkOrder(caller, wo); // customers may see their own job's history (F9.2)
        return workOrderHistoryRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    // =========================================
    // PARTS (F6 — decrement stock in one transaction, never negative)
    // =========================================

    @Transactional
    public WorkOrderPartResponseDTO addPart(User caller, Integer workOrderId, AddPartToWorkOrderRequestDTO request) {
        WorkOrder wo = find(workOrderId);
        assertCanLogWork(caller, wo);

        // Locked read: concurrent requests for the same part queue here.
        Part part = partRepository.findByIdForUpdate(request.partId())
                .orElseThrow(() -> new NoSuchElementException("Part not found: " + request.partId()));

        int qty = request.quantity();
        if (part.getStockQuantity() < qty) {
            throw new IllegalStateException("Insufficient stock for " + part.getName()
                    + ": " + part.getStockQuantity() + " available, " + qty + " requested");
        }
        part.setStockQuantity(part.getStockQuantity() - qty);

        WorkOrderPart line = new WorkOrderPart();
        line.setWorkOrder(wo);
        line.setPart(part);
        line.setQuantity(qty);
        line.setUnitCost(part.getUnitCost());
        line.setLoggedBy(reference(caller));
        WorkOrderPart saved = workOrderPartRepository.save(line);

        saveHistory(wo, caller, "PART_USED", part.getName(), "Quantity: " + qty, null);
        log.info("Part {} x{} used on {} by user {} (stock now {})",
                part.getId(), qty, wo.getCode(), caller.getId(), part.getStockQuantity());
        return toPartResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderPartResponseDTO> getParts(User caller, Integer workOrderId) {
        WorkOrder wo = find(workOrderId);
        assertCanSeeInternals(caller, wo);
        return workOrderPartRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId)
                .stream()
                .map(this::toPartResponse)
                .toList();
    }

    // =========================================
    // TIME (F6.2 — minutes + optional note, recorded against the technician)
    // =========================================

    @Transactional
    public WorkOrderTimeResponseDTO logTime(User caller, Integer workOrderId, LogWorkOrderTimeRequestDTO request) {
        WorkOrder wo = find(workOrderId);
        assertCanLogWork(caller, wo);

        Integer minutes = request.minutes();
        if (minutes == null && request.hours() != null) {
            minutes = request.hours() * 60;
        }
        if (minutes == null || minutes < 1) {
            throw new IllegalArgumentException("Time must be at least 1 minute");
        }

        WorkOrderTime entry = new WorkOrderTime();
        entry.setWorkOrder(wo);
        entry.setMinutes(minutes);
        entry.setHours(Math.round(minutes / 60f));
        entry.setDescription(blankToNull(request.description()));
        entry.setLoggedBy(reference(caller));
        WorkOrderTime saved = workOrderTimeRepository.save(entry);

        saveHistory(wo, caller, "TIME_LOGGED", null, minutes + " min", blankToNull(request.description()));
        log.info("{} min logged on {} by user {}", minutes, wo.getCode(), caller.getId());
        return toTimeResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WorkOrderTimeResponseDTO> getTimeLogs(User caller, Integer workOrderId) {
        WorkOrder wo = find(workOrderId);
        assertCanSeeInternals(caller, wo);
        return workOrderTimeRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId)
                .stream()
                .map(this::toTimeResponse)
                .toList();
    }

    // =========================================
    // TOTALS (F6.3)
    // =========================================

    @Transactional(readOnly = true)
    public WorkOrderTotalsDTO getTotals(User caller, Integer workOrderId) {
        WorkOrder wo = find(workOrderId);
        assertCanSeeInternals(caller, wo);

        List<WorkOrderPart> parts = workOrderPartRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId);
        List<WorkOrderTime> times = workOrderTimeRepository.findByWorkOrderIdOrderByCreatedAtDesc(workOrderId);

        int units = 0;
        BigDecimal cost = BigDecimal.ZERO;
        for (WorkOrderPart line : parts) {
            units += line.getQuantity();
            cost = cost.add(lineCost(line));
        }
        int minutes = times.stream().mapToInt(WorkOrderService::minutesOf).sum();

        return new WorkOrderTotalsDTO(workOrderId, parts.size(), units, cost, minutes, times.size());
    }

    // =========================================
    // OWNERSHIP / SCOPING CHECKS (Section 08)
    // =========================================

    private void assertCustomerOwnership(User caller, Integer customerId) {
        if (caller.getRole() != Role.CUSTOMER) {
            return;
        }
        if (caller.getCustomer() == null || !caller.getCustomer().getId().equals(customerId)) {
            throw new AccessDeniedException("Cannot view another organisation's work orders");
        }
    }

    private void assertTechnicianOwnership(User caller, Integer technicianId) {
        if (caller.getRole() == Role.TECHNICIAN && !caller.getId().equals(technicianId)) {
            throw new AccessDeniedException("Cannot view another technician's jobs");
        }
    }

    void assertCanViewWorkOrder(User caller, WorkOrder wo) {
        if (caller.getRole() == Role.TECHNICIAN && !isAssignedTechnician(caller, wo)) {
            throw new AccessDeniedException("Cannot view a job that isn't assigned to you");
        }
        if (caller.getRole() == Role.CUSTOMER
                && (caller.getCustomer() == null || !wo.getCustomer().getId().equals(caller.getCustomer().getId()))) {
            throw new AccessDeniedException("Cannot view another organisation's work order");
        }
    }

    /** Parts and labour are internal: staff and the assigned technician only (F9.3). */
    private void assertCanSeeInternals(User caller, WorkOrder wo) {
        if (caller.getRole() == Role.CUSTOMER) {
            throw new AccessDeniedException("Parts and time logs are internal to Meridian");
        }
        assertCanViewWorkOrder(caller, wo);
    }

    /** Only staff or the assigned technician may log parts/time, and not on closed or cancelled jobs. */
    private void assertCanLogWork(User caller, WorkOrder wo) {
        Role role = caller.getRole();
        boolean staff = role == Role.MANAGER || role == Role.DISPATCHER;
        if (!staff && !isAssignedTechnician(caller, wo)) {
            throw new AccessDeniedException("Only the assigned technician or dispatch can log work on this job");
        }
        if (wo.getStatus().isTerminal()) {
            throw new IllegalStateException("Can't log work on a " + wo.getStatus() + " work order");
        }
        if (wo.getStatus() == WorkOrderStatus.OPEN) {
            throw new IllegalStateException("Assign a technician before logging work");
        }
    }

    private static boolean isAssignedTechnician(User caller, WorkOrder wo) {
        return caller.getRole() == Role.TECHNICIAN
                && wo.getTechnician() != null
                && wo.getTechnician().getId().equals(caller.getId());
    }

    // =========================================
    // HELPERS
    // =========================================

    private WorkOrder find(Integer id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Work order not found: " + id));
    }

    /** Managed reference for the caller (the principal is detached from any session). */
    private User reference(User caller) {
        return caller == null ? null : userRepository.getReferenceById(caller.getId());
    }

    private void saveHistory(WorkOrder wo, User caller, String action, String oldValue, String newValue, String note) {
        WorkOrderHistory h = new WorkOrderHistory();
        h.setWorkOrder(wo);
        h.setAction(action);
        h.setOldValue(oldValue);
        h.setNewValue(newValue);
        h.setNote(note);
        h.setChangedBy(reference(caller));
        workOrderHistoryRepository.save(h);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static int minutesOf(WorkOrderTime t) {
        if (t.getMinutes() != null) {
            return t.getMinutes();
        }
        return t.getHours() != null ? t.getHours() * 60 : 0;
    }

    private static BigDecimal lineCost(WorkOrderPart line) {
        BigDecimal unit = line.getUnitCost() != null ? line.getUnitCost()
                : (line.getPart() != null && line.getPart().getUnitCost() != null ? line.getPart().getUnitCost() : BigDecimal.ZERO);
        return unit.multiply(BigDecimal.valueOf(line.getQuantity()));
    }

    // =========================================
    // MAPPERS
    // =========================================

    public WorkOrderResponseDTO toResponse(WorkOrder wo) {
        User technician = wo.getTechnician();
        return new WorkOrderResponseDTO(
                wo.getId(),
                wo.getCode() != null ? wo.getCode() : formatCode(wo.getId()),
                wo.getTitle(),
                wo.getDescription(),
                wo.getCustomer().getId(),
                wo.getCustomer().getName(),
                wo.getSite().getId(),
                wo.getSite().getName(),
                technician != null ? technician.getId() : null,
                technician != null ? technician.getName() : null,
                wo.getPriority(),
                wo.getStatus(),
                wo.getSlaDueDate(),
                SlaCalculator.stateOf(wo, Instant.now()),
                wo.getCompletedAt(),
                wo.getClosedAt(),
                wo.getCreatedAt(),
                wo.getUpdatedAt()
        );
    }

    private WorkOrderHistoryResponseDTO toHistoryResponse(WorkOrderHistory h) {
        User by = h.getChangedBy();
        return new WorkOrderHistoryResponseDTO(
                h.getId(),
                h.getAction(),
                h.getOldValue(),
                h.getNewValue(),
                h.getNote(),
                by != null ? by.getId() : null,
                by != null ? by.getName() : "System",
                h.getCreatedAt()
        );
    }

    private WorkOrderTimeResponseDTO toTimeResponse(WorkOrderTime t) {
        User by = t.getLoggedBy();
        int minutes = minutesOf(t);
        return new WorkOrderTimeResponseDTO(
                t.getId(),
                t.getWorkOrder().getId(),
                minutes,
                t.getHours(),
                t.getDescription(),
                by != null ? by.getId() : null,
                by != null ? by.getName() : null,
                t.getCreatedAt()
        );
    }

    private WorkOrderPartResponseDTO toPartResponse(WorkOrderPart line) {
        Part part = line.getPart();
        User by = line.getLoggedBy();
        BigDecimal unit = line.getUnitCost() != null ? line.getUnitCost() : part.getUnitCost();
        return new WorkOrderPartResponseDTO(
                line.getId(),
                part.getId(),
                part.getName(),
                part.getSku(),
                line.getQuantity(),
                unit,
                lineCost(line),
                by != null ? by.getName() : null,
                line.getCreatedAt()
        );
    }
}
