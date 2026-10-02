package com.KeyStone.DeliveryService.DTO.WorkOrder;

import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * Query filters for GET /api/work-orders. All fields are optional.
 *
 * @param statuses     one or more statuses
 * @param priority     exact priority
 * @param technicianId assigned technician
 * @param unassigned   true = only jobs with no technician
 * @param siteId       site
 * @param customerId   customer organisation
 * @param overdue      true = only active jobs past their SLA
 * @param q            free-text search over code, title, customer, site, technician
 * @param from         created on/after this date (UTC)
 * @param to           created on/before this date (UTC)
 */
public record WorkOrderFilter(
        List<WorkOrderStatus> statuses,
        WorkOrderPriority priority,
        Integer technicianId,
        Boolean unassigned,
        Integer siteId,
        Integer customerId,
        Boolean overdue,
        String q,
        LocalDate from,
        LocalDate to
) {
    public static WorkOrderFilter empty() {
        return new WorkOrderFilter(null, null, null, null, null, null, null, null, null, null);
    }
}
