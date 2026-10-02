package com.KeyStone.DeliveryService.DTO.WorkOrder;

import com.KeyStone.DeliveryService.Enum.SlaState;
import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;

import java.time.Instant;

public record WorkOrderResponseDTO(
        Integer id,
        String code,
        String title,
        String description,
        Integer customerId,
        String customerName,
        Integer siteId,
        String siteName,
        Integer technicianId,
        String technicianName,
        WorkOrderPriority priority,
        WorkOrderStatus status,
        Instant slaDueDate,
        SlaState slaState,
        Instant completedAt,
        Instant closedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
