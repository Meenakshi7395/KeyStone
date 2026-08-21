package com.KeyStone.DeliveryService.Service;
import com.KeyStone.DeliveryService.DTO.WorkOrder.AssignTechnicianRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.UpdateWorkOrderStatusRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderHistoryResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderResponseDTO;
import com.KeyStone.DeliveryService.DTO.Part.AddPartToWorkOrderRequestDTO;

import com.KeyStone.DeliveryService.Entity.*;

import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;

import com.KeyStone.DeliveryService.Repository.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository,
            WorkOrderHistoryRepository workOrderHistoryRepository,
            PartRepository partRepository,
            WorkOrderPartRepository workOrderPartRepository
    ) {

        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
        this.workOrderHistoryRepository =
                workOrderHistoryRepository;
        this.partRepository = partRepository;
        this.workOrderPartRepository = workOrderPartRepository;
    }


    // =========================================
    // CREATE WORK ORDER
    // =========================================

    @Transactional
    public WorkOrderResponseDTO create(
            WorkOrderRequestDTO request) {

        Customer customer = customerRepository
                .findById(request.customerId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Customer not found: "
                                        + request.customerId()
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

        // Verify that the site belongs to the customer
        if (!site.getCustomer()
                .getId()
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

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        // Save history
        saveHistory(
                savedWorkOrder,
                "CREATED",
                null,
                "Work order created"
        );

        return toResponse(savedWorkOrder);
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
            Integer customerId,
            Pageable pageable) {

        // Verify customer exists
        customerRepository
                .findById(customerId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Customer not found: "
                                        + customerId
                        )
                );

        return workOrderRepository
                .findByCustomerId(customerId, pageable)
                .map(this::toResponse);
    }


    // =========================================
    // GET WORK ORDER BY ID
    // =========================================

    @Transactional(readOnly = true)
    public WorkOrderResponseDTO getById(
            Integer id) {

        WorkOrder workOrder =
                workOrderRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Work order not found: "
                                                + id
                                )
                        );

        return toResponse(workOrder);
    }


    // =========================================
    // ASSIGN TECHNICIAN TO WORK ORDER
    // =========================================

    @Transactional
    public WorkOrderResponseDTO assignTechnician(
            Integer workOrderId,
            AssignTechnicianRequestDTO request) {

        WorkOrder workOrder =
                workOrderRepository
                        .findById(workOrderId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Work order not found: "
                                                + workOrderId
                                )
                        );

        User technician =
                userRepository
                        .findById(request.technicianId())
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Technician not found: "
                                                + request.technicianId()
                                )
                        );

        // Verify TECHNICIAN role
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

        // Assign technician
        workOrder.setTechnician(technician);

        // Automatically change OPEN to ASSIGNED
        if (workOrder.getStatus()
                == WorkOrderStatus.OPEN) {

            WorkOrderStatus oldStatus =
                    workOrder.getStatus();

            workOrder.setStatus(
                    WorkOrderStatus.ASSIGNED
            );

            saveHistory(
                    workOrder,
                    "STATUS_CHANGED",
                    oldStatus.name(),
                    WorkOrderStatus.ASSIGNED.name()
            );
        }

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        // Save technician assignment history
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
            Integer workOrderId,
            UpdateWorkOrderStatusRequestDTO request) {

        WorkOrder workOrder =
                workOrderRepository
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

        workOrder.setStatus(newStatus);

        WorkOrder savedWorkOrder =
                workOrderRepository.save(workOrder);

        // Save history
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
// ADD PART TO WORK ORDER
// =========================================

    @Transactional
    public void addPart(
            Integer workOrderId,
            AddPartToWorkOrderRequestDTO request) {

        WorkOrder workOrder =
                workOrderRepository
                        .findById(workOrderId)
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Work order not found: "
                                                + workOrderId
                                )
                        );

        Part part =
                partRepository
                        .findById(request.partId())
                        .orElseThrow(() ->
                                new NoSuchElementException(
                                        "Part not found: "
                                                + request.partId()
                                )
                        );

        if (request.quantity() == null
                || request.quantity() <= 0) {

            throw new IllegalArgumentException(
                    "Quantity must be greater than 0"
            );
        }

        if (part.getStockQuantity()
                < request.quantity()) {

            throw new IllegalArgumentException(
                    "Insufficient stock"
            );
        }

        // Decrease stock
        part.setStockQuantity(
                part.getStockQuantity()
                        - request.quantity()
        );

        partRepository.save(part);

        // Save part usage
        WorkOrderPart workOrderPart =
                new WorkOrderPart();

        workOrderPart.setWorkOrder(workOrder);
        workOrderPart.setPart(part);
        workOrderPart.setQuantity(
                request.quantity()
        );

        workOrderPartRepository.save(
                workOrderPart
        );

        // Save history
        saveHistory(
                workOrder,
                "PART_USED",
                part.getName(),
                "Quantity: "
                        + request.quantity()
        );
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
    // GET WORK ORDER HISTORY
    // =========================================

    @Transactional(readOnly = true)
    public List<WorkOrderHistoryResponseDTO> getHistory(
            Integer workOrderId) {

        // Verify work order exists
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
                .map(history ->
                        new WorkOrderHistoryResponseDTO(
                                history.getId(),
                                history.getAction(),
                                history.getOldValue(),
                                history.getNewValue(),
                                history.getCreatedAt()
                        )
                )
                .toList();
    }


    // =========================================
    // ENTITY → RESPONSE DTO
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

                workOrder.getStatus(),

                workOrder.getCreatedAt(),

                workOrder.getUpdatedAt()
        );
    }
}

