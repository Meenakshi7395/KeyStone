package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Part.AddPartToWorkOrderRequestDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.*;
import com.KeyStone.DeliveryService.Service.WorkOrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{id}/history")
    public ResponseEntity<List<WorkOrderHistoryResponseDTO>> getHistory(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                workOrderService.getHistory(id)
        );
    }
    @PostMapping("/{id}/parts")
    public void addPart(
            @PathVariable Integer id,
            @Valid @RequestBody AddPartToWorkOrderRequestDTO request) {

        workOrderService.addPart(id, request);
    }

// =========================================
// LOG TIME AGAINST WORK ORDER
// =========================================

    @PostMapping("/{id}/time")
    public WorkOrderTimeResponseDTO logTime(
            @PathVariable Integer id,
            @Valid @RequestBody LogWorkOrderTimeRequestDTO request) {

        return workOrderService.logTime(id, request);
    }


// =========================================
// GET TIME LOGS FOR WORK ORDER
// =========================================

    @GetMapping("/{id}/time")
    public List<WorkOrderTimeResponseDTO> getTimeLogs(
            @PathVariable Integer id) {

        return workOrderService.getTimeLogs(id);
    }


}
