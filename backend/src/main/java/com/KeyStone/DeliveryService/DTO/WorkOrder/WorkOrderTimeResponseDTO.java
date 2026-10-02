package com.KeyStone.DeliveryService.DTO.WorkOrder;

import java.time.LocalDateTime;

public record WorkOrderTimeResponseDTO(
        Integer id,
        Integer workOrderId,
        Integer minutes,
        Integer hours,
        String description,
        Integer loggedById,
        String loggedByName,
        LocalDateTime createdAt
) {
}
