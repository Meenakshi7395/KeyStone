package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Report.ReportSummaryResponseDTO;
import com.KeyStone.DeliveryService.Service.ReportService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(
            ReportService reportService) {

        this.reportService = reportService;
    }


    // =========================================
    // GET REPORT SUMMARY
    // =========================================

    @GetMapping("/summary")
    public ResponseEntity<ReportSummaryResponseDTO> getSummary() {

        return ResponseEntity.ok(
                reportService.getSummary()
        );
    }
}
