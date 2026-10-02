package com.KeyStone.DeliveryService.DTO.Report;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Manager dashboard metrics (F8). Every figure respects the filters that
 * were passed (date range, site, technician, customer).
 */
public record ReportSummaryResponseDTO(
        long totalWorkOrders,
        long openWorkOrders,
        long assignedWorkOrders,
        long inProgressWorkOrders,
        long completedWorkOrders,
        long totalCustomers,
        long totalTechnicians,

        long onHoldWorkOrders,
        long closedWorkOrders,
        long cancelledWorkOrders,
        long activeWorkOrders,
        long overdueWorkOrders,
        long atRiskWorkOrders,

        // SLA compliance: finished jobs judged on completedAt vs due date,
        // plus anything still open that is already past due.
        long slaMet,
        long slaBreached,
        Double slaCompliancePercent,
        Double averageResolutionHours,

        Map<String, Long> countsByStatus,
        List<BreakdownRow> byTechnician,
        List<BreakdownRow> bySite,
        Instant generatedAt
) {

    public record BreakdownRow(
            Integer id,
            String name,
            long total,
            long active,
            long completed,
            long overdue,
            long slaMet,
            long slaBreached
    ) {
    }
}
