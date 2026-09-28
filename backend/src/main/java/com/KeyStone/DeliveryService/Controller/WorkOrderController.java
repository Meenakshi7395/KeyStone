package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Part.AddPartToWorkOrderRequestDTO;
import com
        .KeyStone.DeliveryService.DTO.Part.PartResponseDTO;
import com.KeyStone.DeliveryService.DTO.WorkOrder.*;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.WorkOrderService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    // =========================================
    // CREATE WORK ORDER
    // =========================================

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<WorkOrderResponseDTO> create(
            Authentication authentication,
            @Valid @RequestBody WorkOrderRequestDTO request) {

        User caller = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                workOrderService.create(caller, request)
        );
    }


    // =========================================
    // GET ALL WORK ORDERS
    // =========================================

    @GetMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDTO>> list(
            Pageable pageable) {

        return ResponseEntity.ok(
                workOrderService.list(pageable)
        );
    }


    // =========================================
    // GET WORK ORDERS BY TECHNICIAN
    // IMPORTANT: This must appear before /{id}
    // =========================================

    @GetMapping("/technician/{technicianId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDTO>> getByTechnician(
            Authentication authentication,
            @PathVariable Integer technicianId,
            Pageable pageable) {

        User caller = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                workOrderService.getByTechnician(
                        caller,
                        technicianId,
                        pageable
                )
        );
    }


    // =========================================
    // GET WORK ORDERS BY CUSTOMER
    // =========================================

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','DISPATCHER','MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDTO>> getByCustomer(
            Authentication authentication,
            @PathVariable Integer customerId,
            Pageable pageable) {

        User caller = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                workOrderService.getByCustomer(
                        caller,
                        customerId,
                        pageable
                )
        );
    }


    // =========================================
    // GET WORK ORDER BY ID
    // =========================================

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN','CUSTOMER')")
    public ResponseEntity<WorkOrderResponseDTO> getById(
            Authentication authentication,
            @PathVariable Integer id) {

        User caller = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                workOrderService.getById(caller, id)
        );
    }


    // =========================================
    // ASSIGN TECHNICIAN
    // =========================================

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderResponseDTO> assignTechnician(
            @PathVariable Integer id,
            @Valid @RequestBody AssignTechnicianRequestDTO request) {

        return ResponseEntity.ok(
                workOrderService.assignTechnician(id, request)
        );
    }


    // =========================================
    // UPDATE STATUS
    // =========================================

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<WorkOrderResponseDTO> updateStatus(
            Authentication authentication,
            @PathVariable Integer id,
            @Valid @RequestBody UpdateWorkOrderStatusRequestDTO request) {

        User caller = (User) authentication.getPrincipal();

        return ResponseEntity.ok(
                workOrderService.updateStatus(caller, id, request)
        );
    }


    // =========================================
    // GET WORK ORDER HISTORY
    // =========================================

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<WorkOrderHistoryResponseDTO>> getHistory(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                workOrderService.getHistory(id)
        );
    }


    // =========================================
    // ADD / USE PART
    // =========================================

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<Void> addPart(
            @PathVariable Integer id,
            @Valid @RequestBody AddPartToWorkOrderRequestDTO request) {

        workOrderService.addPart(id, request);

        return ResponseEntity.ok().build();
    }


    // =========================================
    // GET PARTS USED IN WORK ORDER
    // =========================================

    @GetMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<PartResponseDTO>> getParts(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                workOrderService.getParts(id)
        );
    }


    // =========================================
    // LOG TIME
    // =========================================

    @PostMapping("/{id}/time")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderTimeResponseDTO> logTime(
            @PathVariable Integer id,
            @Valid @RequestBody LogWorkOrderTimeRequestDTO request) {

        return ResponseEntity.ok(
                workOrderService.logTime(id, request)
        );
    }


    // =========================================
    // GET TIME LOGS
    // =========================================

    @GetMapping("/{id}/time")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<List<WorkOrderTimeResponseDTO>> getTimeLogs(
            @PathVariable Integer id) {

        return ResponseEntity.ok(
                workOrderService.getTimeLogs(id)
        );
    }
}
