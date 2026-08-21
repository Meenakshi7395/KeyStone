package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.WorkOrder.AssignTechnicianRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.UpdateWorkOrderStatusRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.WorkOrderResponseDTO;
import com.KeyStone.DeliveryService.Service.WorkOrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(
            WorkOrderService workOrderService) {

        this.workOrderService = workOrderService;
    }

    // CREATE WORK ORDER
    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderResponseDTO> create(
            @Valid @RequestBody WorkOrderRequestDTO request) {

        return ResponseEntity.ok(
                workOrderService.create(request)
        );
    }

    // GET ALL WORK ORDERS
    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDTO>> list(
            Pageable pageable) {

        return ResponseEntity.ok(
                workOrderService.list(pageable)
        );
    }

    // GET WORK ORDERS BY CUSTOMER
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Page<WorkOrderResponseDTO>> getByCustomer(
            @PathVariable Integer customerId,
            Pageable pageable) {

        return ResponseEntity.ok(
                workOrderService.getByCustomer(
                        customerId,
                        pageable
                )
        );
    }

    // GET WORK ORDER BY ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderResponseDTO> getById(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                workOrderService.getById(id)
        );
    }

    // ASSIGN TECHNICIAN TO WORK ORDER
    @PutMapping("/{id}/assign")
    public ResponseEntity<WorkOrderResponseDTO> assignTechnician(
            @PathVariable Integer id,
            @Valid @RequestBody AssignTechnicianRequestDTO request) {

        return ResponseEntity.ok(
                workOrderService.assignTechnician(id, request)
        );
    }

    // UPDATE WORK ORDER STATUS
    @PutMapping("/{id}/status")
    public ResponseEntity<WorkOrderResponseDTO> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateWorkOrderStatusRequestDTO request) {

        return ResponseEntity.ok(
                workOrderService.updateStatus(id, request)
        );
    }

}
