package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Report.ReportSummaryResponseDTO;
import com.KeyStone.DeliveryService.Service.ReportService;
import com.KeyStone.DeliveryService.Service.SlaMonitor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** Dashboard & reporting — managers only (brief 3 / F8). */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('MANAGER')")
public class ReportController {

    private final ReportService reportService;
    private final SlaMonitor slaMonitor;

    public ReportController(ReportService reportService, SlaMonitor slaMonitor) {
        this.reportService = reportService;
        this.slaMonitor = slaMonitor;
    }

    /**
     * GET /api/reports/summary?from=2026-09-01&to=2026-09-30&siteId=2&technicianId=5&customerId=1
     * Status counts, overdue / at-risk, SLA compliance, average resolution time,
     * and breakdowns by technician and by site — all following the filters.
     */
    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryResponseDTO> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(required = false) Integer technicianId,
            @RequestParam(required = false) Integer customerId) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("'to' must be on or after 'from'");
        }
        return ResponseEntity.ok(reportService.getSummary(from, to, siteId, technicianId, customerId));
    }

    /** Run the SLA monitor now instead of waiting for the schedule (handy for demos). */
    @PostMapping("/sla-check")
    public ResponseEntity<SlaMonitor.Result> runSlaCheck() {
        return ResponseEntity.ok(slaMonitor.runCheck());
    }
}
