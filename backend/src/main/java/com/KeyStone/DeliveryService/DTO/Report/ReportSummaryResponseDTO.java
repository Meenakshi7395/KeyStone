package com.KeyStone.DeliveryService.DTO.Report;

public record ReportSummaryResponseDTO(

        long totalWorkOrders,

        long openWorkOrders,

        long assignedWorkOrders,

        long inProgressWorkOrders,

        long completedWorkOrders,

        long totalCustomers,

        long totalTechnicians

) {
}