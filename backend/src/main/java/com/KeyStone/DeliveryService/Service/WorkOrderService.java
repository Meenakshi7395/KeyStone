package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Part.AddPartToWorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.Part.PartResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.AssignTechnicianRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.LogWorkOrderTimeRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.UpdateWorkOrderStatusRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderHistoryResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderTimeResponseDTO;

import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.Part;
import com.KeyStone.DeliveryService.Entity.Site;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Entity.WorkOrderHistory;
import com.KeyStone.DeliveryService.Entity.WorkOrderPart;
import com.KeyStone.DeliveryService.Entity.WorkOrderTime;

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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final WorkOrderHistoryRepository workOrderHistoryRepository;
    private final PartRepository partRepository;
    private final WorkOrderPartRepository workOrderPartRepository;
    private final WorkOrderTimeRepository workOrderTimeRepository;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository,
            WorkOrderHistoryRepository workOrderHistoryRepository,
            PartRepository partRepository,
            WorkOrderPartRepository workOrderPartRepository,
            WorkOrderTimeRepository workOrderTimeRepository) {

        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.workOrderHistoryRepository = workOrderHistoryRepository;
        this.partRepository = partRepository;
        this.workOrderPartRepository = workOrderPartRepository;
        this.workOrderTimeRepository = workOrderTimeRepository;
    }


    // =========================================
    // CREATE WORK ORDER
    // =========================================

    @Transactional
    public WorkOrderResponseDTO create(
            User caller,
            WorkOrderRequestDTO request) {

        Integer customerId = request.customerId();

        if (caller.getRole() == Role.CUSTOMER) {
            if (caller.getCustomer() == null) {
                throw new IllegalStateException(
                        "Link your account to an organisation before raising a request"
                );
            }
            customerId = caller.getCustomer().getId();
        }

        final Integer finalCustomerId = customerId;

        Customer customer = customerRepository
                .findById(finalCustomerId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Customer not found: "
                                        + finalCustomerId
                        )
                );

        Site site = siteRepository
                .findById(request.siteId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Site not found: "
                                        + request.siteId()
                        )
                );

        if (!site.getCustomer().getId()
                .equals(customer.getId())) {

            throw new IllegalArgumentException(
                    "Site does not belong to this customer"
            );
        }

        WorkOrder workOrder = new WorkOrder();

        workOrder.setTitle(request.title());
        workOrder.setDescription(request.description());
        workOrder.setCustomer(customer);
        workOrder.setSite(site);

        // New fields
        workOrder.setPriority(request.priority());
        workOrder.setSlaDueDate(
                request.slaDueDate() != null
                        ? request.slaDueDate()
                        : defaultSlaDueDate(request.priority())
        );

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        saveHistory(
                savedWorkOrder,
                "WORK_ORDER_CREATED",
                null,
                savedWorkOrder.getTitle()
        );

        return toResponse(savedWorkOrder);
    }

    // Server-computed SLA deadline, used whenever the caller doesn't supply
    // one explicitly. Keeps priority meaningful even when the UI/API caller
    // omits slaDueDate.
    private Instant defaultSlaDueDate(WorkOrderPriority priority) {

        Instant now = Instant.now();

        if (priority == null) {
            return now.plus(3, ChronoUnit.DAYS);
        }

        return switch (priority) {
            case CRITICAL -> now.plus(4, ChronoUnit.HOURS);
            case HIGH -> now.plus(1, ChronoUnit.DAYS);
            case MEDIUM -> now.plus(3, ChronoUnit.DAYS);
            case LOW -> now.plus(7, ChronoUnit.DAYS);
        };
    }


    // =========================================
    // GET ALL WORK ORDERS
    // =========================================

    @Transactional(readOnly = true)
    public Page<WorkOrderResponseDTO> list(
            Pageable pageable) {

        return workOrderRepository
                .findAll(pageable)
                .map(this::toResponse);
    }


    // =========================================
    // GET WORK ORDERS BY CUSTOMER
    // =========================================

    @Transactional(readOnly = true)
    public Page<WorkOrderResponseDTO> getByCustomer(
            User caller,
            Integer customerId,
            Pageable pageable) {

        assertCustomerOwnership(caller, customerId);

        customerRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Customer not found: "
                                        + customerId
                        )
                );

        return workOrderRepository
                .findByCustomerId(
                        customerId,
                        pageable
                )
                .map(this::toResponse);
    }


    // =========================================
    // GET WORK ORDERS BY TECHNICIAN
    // =========================================

    @Transactional(readOnly = true)
    public Page<WorkOrderResponseDTO> getByTechnician(
            User caller,
            Integer technicianId,
            Pageable pageable) {

        assertTechnicianOwnership(caller, technicianId);

        User technician = userRepository
                .findById(technicianId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Technician not found: "
                                        + technicianId
                        )
                );

        if (!"TECHNICIAN".equals(
                technician.getRole().name())) {

            throw new IllegalArgumentException(
                    "Selected user is not a technician"
            );
        }

        return workOrderRepository
                .findByTechnicianId(
                        technicianId,
                        pageable
                )
                .map(this::toResponse);
    }


    // =========================================
    // GET WORK ORDER BY ID
    // =========================================

    @Transactional(readOnly = true)
    public WorkOrderResponseDTO getById(
            User caller,
            Integer id) {

        WorkOrder workOrder = workOrderRepository
                .findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: " + id
                        )
                );

        assertCanViewWorkOrder(caller, workOrder);

        return toResponse(workOrder);
    }


    // =========================================
    // OWNERSHIP / SCOPING CHECKS
    // =========================================

    // Section 08 boundary: a CUSTOMER may only ever see its own
    // organisation's work orders, and a TECHNICIAN only its own assigned
    // jobs — regardless of what ID is put in the URL. DISPATCHER/MANAGER
    // are unrestricted.
    private void assertCustomerOwnership(User caller, Integer customerId) {

        if (caller.getRole() != Role.CUSTOMER) {
            return;
        }

        if (caller.getCustomer() == null
                || !caller.getCustomer().getId().equals(customerId)) {

            throw new AccessDeniedException(
                    "Cannot view another organisation's work orders"
            );
        }
    }

    private void assertTechnicianOwnership(User caller, Integer technicianId) {

        if (caller.getRole() != Role.TECHNICIAN) {
            return;
        }

        if (!caller.getId().equals(technicianId)) {
            throw new AccessDeniedException(
                    "Cannot view another technician's jobs"
            );
        }
    }

    private void assertCanViewWorkOrder(User caller, WorkOrder workOrder) {

        if (caller.getRole() == Role.TECHNICIAN) {
            if (workOrder.getTechnician() == null
                    || !workOrder.getTechnician().getId().equals(caller.getId())) {
                throw new AccessDeniedException(
                        "Cannot view a job that isn't assigned to you"
                );
            }
        }

        if (caller.getRole() == Role.CUSTOMER) {
            if (caller.getCustomer() == null
                    || !workOrder.getCustomer().getId().equals(caller.getCustomer().getId())) {
                throw new AccessDeniedException(
                        "Cannot view another organisation's work order"
                );
            }
        }
    }


    // =========================================
    // ASSIGN TECHNICIAN
    // =========================================

    @Transactional
    public WorkOrderResponseDTO assignTechnician(
            Integer workOrderId,
            AssignTechnicianRequestDTO request) {

        WorkOrder workOrder = workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: "
                                        + workOrderId
                        )
                );

        if (workOrder.getStatus() == WorkOrderStatus.COMPLETED
                || workOrder.getStatus() == WorkOrderStatus.CLOSED) {

            throw new IllegalStateException(
                    "Cannot reassign a " + workOrder.getStatus() + " work order"
            );
        }

        User technician = userRepository
                .findById(request.technicianId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Technician not found: "
                                        + request.technicianId()
                        )
                );

        if (!"TECHNICIAN".equals(
                technician.getRole().name())) {

            throw new IllegalArgumentException(
                    "Selected user is not a technician"
            );
        }

        String oldTechnician =
                workOrder.getTechnician() != null
                        ? workOrder.getTechnician().getName()
                        : null;

        workOrder.setTechnician(technician);

        // OPEN automatically becomes ASSIGNED
        if (workOrder.getStatus()
                == WorkOrderStatus.OPEN) {

            workOrder.setStatus(
                    WorkOrderStatus.ASSIGNED
            );

            saveHistory(
                    workOrder,
                    "STATUS_CHANGED",
                    WorkOrderStatus.OPEN.name(),
                    WorkOrderStatus.ASSIGNED.name()
            );
        }

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        saveHistory(
                savedWorkOrder,
                "TECHNICIAN_ASSIGNED",
                oldTechnician,
                technician.getName()
        );

        return toResponse(savedWorkOrder);
    }


    // =========================================
    // UPDATE WORK ORDER STATUS
    // =========================================

    @Transactional
    public WorkOrderResponseDTO updateStatus(
            User caller,
            Integer workOrderId,
            UpdateWorkOrderStatusRequestDTO request) {

        WorkOrder workOrder = workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: "
                                        + workOrderId
                        )
                );

        WorkOrderStatus oldStatus =
                workOrder.getStatus();

        WorkOrderStatus newStatus =
                request.status();

        validateStatusTransition(
                oldStatus,
                newStatus
        );

        assertCanTransition(caller, workOrder, newStatus);

        workOrder.setStatus(newStatus);

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        saveHistory(
                savedWorkOrder,
                "STATUS_CHANGED",
                oldStatus != null
                        ? oldStatus.name()
                        : null,
                newStatus.name()
        );

        return toResponse(savedWorkOrder);
    }


    // =========================================
    // VALIDATE STATUS TRANSITION (state machine)
    // =========================================
    private void validateStatusTransition(
            WorkOrderStatus currentStatus,
            WorkOrderStatus newStatus) {

        if (currentStatus == null) {
            throw new IllegalStateException(
                    "Current work order status is missing"
            );
        }

        if (newStatus == null) {
            throw new IllegalStateException(
                    "New work order status is required"
            );
        }

        boolean isValid;

        switch (currentStatus) {

            case OPEN:
                isValid =
                        newStatus == WorkOrderStatus.ASSIGNED;
                break;

            case ASSIGNED:
                isValid =
                        newStatus == WorkOrderStatus.IN_PROGRESS
                                || newStatus == WorkOrderStatus.ON_HOLD;
                break;

            case IN_PROGRESS:
                isValid =
                        newStatus == WorkOrderStatus.COMPLETED
                                || newStatus == WorkOrderStatus.ON_HOLD;
                break;

            case ON_HOLD:
                isValid =
                        newStatus == WorkOrderStatus.IN_PROGRESS;
                break;

            case COMPLETED:
                isValid =
                        newStatus == WorkOrderStatus.CLOSED;
                break;

            case CLOSED:
                isValid = false;
                break;

            default:
                isValid = false;
                break;
        }

        if (!isValid) {
            throw new IllegalStateException(
                    "Illegal status transition from "
                            + currentStatus
                            + " to "
                            + newStatus
            );
        }
    }

    // =========================================
    // ROLE-BASED TRANSITION AUTHORIZATION
    // (Section 07.2: only the assigned technician can start/hold/
    // resume/complete their own job; only a manager can close it out;
    // OPEN -> ASSIGNED only ever happens via the assign endpoint.)
    // =========================================
    private void assertCanTransition(User caller, WorkOrder workOrder, WorkOrderStatus newStatus) {

        if (newStatus == WorkOrderStatus.ASSIGNED) {
            throw new IllegalStateException(
                    "Use the assign-technician action to move a job to ASSIGNED"
            );
        }

        if (newStatus == WorkOrderStatus.CLOSED) {
            if (caller.getRole() != Role.MANAGER) {
                throw new AccessDeniedException("Only a manager can close a work order");
            }
            return;
        }

        if (caller.getRole() == Role.MANAGER || caller.getRole() == Role.DISPATCHER) {
            return;
        }

        if (caller.getRole() == Role.TECHNICIAN) {
            if (workOrder.getTechnician() == null
                    || !workOrder.getTechnician().getId().equals(caller.getId())) {
                throw new AccessDeniedException(
                        "Only the assigned technician can update this job's status"
                );
            }
            return;
        }

        throw new AccessDeniedException("Not permitted to change this work order's status");
    }



    // =========================================
    // GET WORK ORDER HISTORY
    // =========================================

    @Transactional(readOnly = true)
    public List<WorkOrderHistoryResponseDTO> getHistory(
            Integer workOrderId) {

        workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: "
                                        + workOrderId
                        )
                );

        return workOrderHistoryRepository
                .findByWorkOrderIdOrderByCreatedAtDesc(
                        workOrderId
                )
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }


    // =========================================
    // ADD PART TO WORK ORDER
    // =========================================

    @Transactional
    public void addPart(
            Integer workOrderId,
            AddPartToWorkOrderRequestDTO request) {

        WorkOrder workOrder = workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: "
                                        + workOrderId
                        )
                );

        Part part = partRepository
                .findById(request.partId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Part not found: "
                                        + request.partId()
                        )
                );

        if (part.getStockQuantity()
                < request.quantity()) {

            throw new IllegalArgumentException(
                    "Insufficient stock for part: "
                            + part.getName()
            );
        }

        part.setStockQuantity(
                part.getStockQuantity()
                        - request.quantity()
        );

        partRepository.save(part);

        WorkOrderPart workOrderPart = new WorkOrderPart();
        workOrderPart.setWorkOrder(workOrder);
        workOrderPart.setPart(part);
        workOrderPart.setQuantity(request.quantity());
        workOrderPartRepository.save(workOrderPart);

        saveHistory(
                workOrder,
                "PART_USED",
                part.getName(),
                "Quantity: " + request.quantity()
        );
    }


    // =========================================
    // LOG TIME AGAINST WORK ORDER
    // =========================================

    @Transactional
    public WorkOrderTimeResponseDTO logTime(
            Integer workOrderId,
            LogWorkOrderTimeRequestDTO request) {

        WorkOrder workOrder = workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: "
                                        + workOrderId
                        )
                );

        WorkOrderTime workOrderTime =
                new WorkOrderTime();

        workOrderTime.setWorkOrder(workOrder);
        workOrderTime.setHours(request.hours());
        workOrderTime.setDescription(
                request.description()
        );

        WorkOrderTime savedTime =
                workOrderTimeRepository.save(
                        workOrderTime
                );

        saveHistory(
                workOrder,
                "TIME_LOGGED",
                null,
                "Hours: " + request.hours()
        );

        return toTimeResponse(savedTime);
    }


    // =========================================
    // GET TIME LOGS FOR WORK ORDER
    // =========================================

    @Transactional(readOnly = true)
    public List<WorkOrderTimeResponseDTO> getTimeLogs(
            Integer workOrderId) {

        workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: "
                                        + workOrderId
                        )
                );

        return workOrderTimeRepository
                .findByWorkOrderIdOrderByCreatedAtDesc(
                        workOrderId
                )
                .stream()
                .map(this::toTimeResponse)
                .toList();
    }


    // =========================================
    // GET PARTS USED ON A WORK ORDER
    // =========================================

    @Transactional(readOnly = true)
    public List<PartResponseDTO> getParts(Integer workOrderId) {

        workOrderRepository.findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: " + workOrderId
                        )
                );

        return workOrderPartRepository
                .findByWorkOrderIdOrderByCreatedAtDesc(workOrderId)
                .stream()
                .map(wop -> new PartResponseDTO(
                        wop.getPart().getId(),
                        wop.getPart().getName(),
                        wop.getQuantity()
                ))
                .toList();
    }


    // =========================================
    // SAVE WORK ORDER HISTORY
    // =========================================

    private void saveHistory(
            WorkOrder workOrder,
            String action,
            String oldValue,
            String newValue) {

        WorkOrderHistory history =
                new WorkOrderHistory();

        history.setWorkOrder(workOrder);
        history.setAction(action);
        history.setOldValue(oldValue);
        history.setNewValue(newValue);

        workOrderHistoryRepository.save(history);
    }


    // =========================================
    // HISTORY ENTITY → RESPONSE DTO
    // =========================================

    private WorkOrderHistoryResponseDTO toHistoryResponse(
            WorkOrderHistory history) {

        return new WorkOrderHistoryResponseDTO(
                history.getId(),
                history.getAction(),
                history.getOldValue(),
                history.getNewValue(),
                history.getCreatedAt()
        );
    }


    // =========================================
    // TIME ENTITY → RESPONSE DTO
    // =========================================

    private WorkOrderTimeResponseDTO toTimeResponse(
            WorkOrderTime workOrderTime) {

        return new WorkOrderTimeResponseDTO(
                workOrderTime.getId(),
                workOrderTime.getWorkOrder().getId(),
                workOrderTime.getHours(),
                workOrderTime.getDescription(),
                workOrderTime.getCreatedAt()
        );
    }


    // =========================================
    // WORK ORDER ENTITY → RESPONSE DTO
    // =========================================

    private WorkOrderResponseDTO toResponse(
            WorkOrder workOrder) {

        User technician =
                workOrder.getTechnician();

        return new WorkOrderResponseDTO(
                workOrder.getId(),
                workOrder.getTitle(),
                workOrder.getDescription(),
                workOrder.getCustomer().getId(),
                workOrder.getCustomer().getName(),
                workOrder.getSite().getId(),
                workOrder.getSite().getName(),
                technician != null
                        ? technician.getId()
                        : null,
                technician != null
                        ? technician.getName()
                        : null,
                workOrder.getPriority(),
                workOrder.getStatus(),
                workOrder.getSlaDueDate(),
                workOrder.getCreatedAt(),
                workOrder.getUpdatedAt()
        );
    }
}
