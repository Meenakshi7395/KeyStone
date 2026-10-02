package com.KeyStone.DeliveryService.Controller;

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
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import com.KeyStone.DeliveryService.Service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Work orders. Controllers stay thin: role gates here, business rules and
 * ownership checks in WorkOrderService.
 */
@RestController
@RequestMapping("/api/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    public WorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    // ---------- create / read ----------

    @PostMapping
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','CUSTOMER')")
    public ResponseEntity<WorkOrderResponseDTO> create(@AuthenticationPrincipal User caller,
                                                       @Valid @RequestBody WorkOrderRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.create(caller, request));
    }

    /**
     * Filterable, paginated, role-scoped list.
     * e.g. GET /api/work-orders?status=OPEN&status=ASSIGNED&priority=HIGH&overdue=true&q=hvac&page=0&size=20&sort=slaDueDate,asc
     * Technicians only ever get their own jobs; customers only their organisation's.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WorkOrderResponseDTO>> list(
            @AuthenticationPrincipal User caller,
            @RequestParam(name = "status", required = false) List<WorkOrderStatus> statuses,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) Integer technicianId,
            @RequestParam(required = false) Boolean unassigned,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        WorkOrderFilter filter = new WorkOrderFilter(statuses, priority, technicianId, unassigned,
                siteId, customerId, overdue, q, from, to);
        return ResponseEntity.ok(workOrderService.list(caller, filter, pageable));
    }

    @GetMapping("/technician/{technicianId}")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDTO>> getByTechnician(
            @AuthenticationPrincipal User caller,
            @PathVariable Integer technicianId,
            @PageableDefault(size = 20, sort = "slaDueDate") Pageable pageable) {
        return ResponseEntity.ok(workOrderService.getByTechnician(caller, technicianId, pageable));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','DISPATCHER','MANAGER')")
    public ResponseEntity<Page<WorkOrderResponseDTO>> getByCustomer(
            @AuthenticationPrincipal User caller,
            @PathVariable Integer customerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(workOrderService.getByCustomer(caller, customerId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WorkOrderResponseDTO> getById(@AuthenticationPrincipal User caller, @PathVariable Integer id) {
        return ResponseEntity.ok(workOrderService.getById(caller, id));
    }

    // ---------- edit ----------

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderResponseDTO> update(@AuthenticationPrincipal User caller,
                                                       @PathVariable Integer id,
                                                       @Valid @RequestBody WorkOrderUpdateRequestDTO request) {
        return ResponseEntity.ok(workOrderService.update(caller, id, request));
    }

    // ---------- dispatch & lifecycle ----------

    /** POST (brief) or PUT (older clients) /api/work-orders/{id}/assign */
    @RequestMapping(value = "/{id}/assign", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderResponseDTO> assign(@AuthenticationPrincipal User caller,
                                                       @PathVariable Integer id,
                                                       @Valid @RequestBody AssignTechnicianRequestDTO request) {
        return ResponseEntity.ok(workOrderService.assignTechnician(caller, id, request));
    }

    /** POST (brief) or PUT /api/work-orders/{id}/status — 409 when the move isn't on the lifecycle diagram. */
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.POST, RequestMethod.PUT})
    @PreAuthorize("hasAnyRole('DISPATCHER','MANAGER','TECHNICIAN')")
    public ResponseEntity<WorkOrderResponseDTO> updateStatus(@AuthenticationPrincipal User caller,
                                                             @PathVariable Integer id,
                                                             @Valid @RequestBody UpdateWorkOrderStatusRequestDTO request) {
        return ResponseEntity.ok(workOrderService.updateStatus(caller, id, request));
    }

    /** Statuses the caller may move this job to right now. */
    @GetMapping("/{id}/transitions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkOrderStatus>> transitions(@AuthenticationPrincipal User caller, @PathVariable Integer id) {
        return ResponseEntity.ok(workOrderService.allowedNextStatuses(caller, id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WorkOrderHistoryResponseDTO>> history(@AuthenticationPrincipal User caller,
                                                                     @PathVariable Integer id) {
        return ResponseEntity.ok(workOrderService.getHistory(caller, id));
    }

    // ---------- parts & time ----------

    @PostMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderPartResponseDTO> addPart(@AuthenticationPrincipal User caller,
                                                            @PathVariable Integer id,
                                                            @Valid @RequestBody AddPartToWorkOrderRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.addPart(caller, id, request));
    }

    @GetMapping("/{id}/parts")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<List<WorkOrderPartResponseDTO>> getParts(@AuthenticationPrincipal User caller,
                                                                   @PathVariable Integer id) {
        return ResponseEntity.ok(workOrderService.getParts(caller, id));
    }

    @PostMapping("/{id}/time")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderTimeResponseDTO> logTime(@AuthenticationPrincipal User caller,
                                                            @PathVariable Integer id,
                                                            @Valid @RequestBody LogWorkOrderTimeRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workOrderService.logTime(caller, id, request));
    }

    @GetMapping("/{id}/time")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<List<WorkOrderTimeResponseDTO>> getTime(@AuthenticationPrincipal User caller,
                                                                  @PathVariable Integer id) {
        return ResponseEntity.ok(workOrderService.getTimeLogs(caller, id));
    }

    /** Parts cost and labour minutes rolled up on the job (F6.3). */
    @GetMapping("/{id}/totals")
    @PreAuthorize("hasAnyRole('TECHNICIAN','DISPATCHER','MANAGER')")
    public ResponseEntity<WorkOrderTotalsDTO> totals(@AuthenticationPrincipal User caller, @PathVariable Integer id) {
        return ResponseEntity.ok(workOrderService.getTotals(caller, id));
    }
}
