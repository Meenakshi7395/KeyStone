package com.KeyStone.DeliveryService.DTO.WorkOrder;

import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;

import java.time.Instant;

public record WorkOrderResponseDTO(

        Integer id,

        String title,

        String description,

        Integer customerId,

        String customerName,

        Integer siteId,

        String siteName,

        Integer technicianId,

        String technicianName,

        WorkOrderStatus status,

        Instant createdAt,

        Instant updatedAt

) {
}