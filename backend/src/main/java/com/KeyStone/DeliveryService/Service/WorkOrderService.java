package com.KeyStone.DeliveryService.Service;
import com.KeyStone.DeliveryService.DTO.WorkOrder.AssignTechnicianRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.UpdateWorkOrderStatusRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderResponseDTO;
import com.KeyStone.DeliveryService.Entity.Customer;
import com.KeyStone.DeliveryService.Entity.Site;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Repository.CustomerRepository;
import com.KeyStone.DeliveryService.Repository.SiteRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import com.KeyStone.DeliveryService.Repository.WorkOrderRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final CustomerRepository customerRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;

    public WorkOrderService(
            WorkOrderRepository workOrderRepository,
            CustomerRepository customerRepository,
            SiteRepository siteRepository,
            UserRepository userRepository) {

        this.workOrderRepository = workOrderRepository;
        this.customerRepository = customerRepository;
        this.siteRepository = siteRepository;
        this.userRepository = userRepository;
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

        return toResponse(
                workOrderRepository.save(workOrder)
        );
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

        WorkOrder workOrder = workOrderRepository
                .findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: " + id
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

        WorkOrder workOrder = workOrderRepository
                .findById(workOrderId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Work order not found: "
                                        + workOrderId
                        )
                );

        User technician = userRepository
                .findById(request.technicianId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Technician not found: "
                                        + request.technicianId()
                        )
                );

        // Verify that the selected user has TECHNICIAN role
        if (!"TECHNICIAN".equals(
                technician.getRole().name())) {

            throw new IllegalArgumentException(
                    "Selected user is not a technician"
            );
        }

        // Assign technician
        workOrder.setTechnician(technician);

        // Automatically change OPEN to ASSIGNED
        if (workOrder.getStatus()
                == WorkOrderStatus.OPEN) {

            workOrder.setStatus(
                    WorkOrderStatus.ASSIGNED
            );
        }

        return toResponse(
                workOrderRepository.save(workOrder)
        );
    }

    // =========================================
    // UPDATE WORK ORDER STATUS
    // =========================================
    @Transactional
    public WorkOrderResponseDTO updateStatus(
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

        workOrder.setStatus(request.status());

        return toResponse(
                workOrderRepository.save(workOrder)
        );
    }

    // =========================================
    // ENTITY → RESPONSE DTO
    // =========================================
    private WorkOrderResponseDTO toResponse(
            WorkOrder workOrder) {

        User technician = workOrder.getTechnician();

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

