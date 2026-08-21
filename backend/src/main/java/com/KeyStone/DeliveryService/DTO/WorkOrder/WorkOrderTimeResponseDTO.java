package com.KeyStone.DeliveryService.DTO.WorkOrder;

import java.time.LocalDateTime;

public record WorkOrderTimeResponseDTO(

        Integer id,
        Integer workOrderId,
        Integer hours,
        String description,
        LocalDateTime createdAt

) {
}
