package com.KeyStone.DeliveryService.DTO.WorkOrder;

import java.math.BigDecimal;

/** Parts cost and labour time rolled up on one work order (F6.3). */
public record WorkOrderTotalsDTO(
        Integer workOrderId,
        int partLines,
        int partUnits,
        BigDecimal partsCost,
        int labourMinutes,
        int timeEntries
) {
}
