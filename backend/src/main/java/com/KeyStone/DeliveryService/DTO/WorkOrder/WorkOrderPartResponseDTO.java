package com.KeyStone.DeliveryService.DTO.WorkOrder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** One line of parts used on a work order. */
public record WorkOrderPartResponseDTO(
        Integer id,
        Integer partId,
        String name,
        String sku,
        Integer quantity,
        BigDecimal unitCost,
        BigDecimal lineCost,
        String loggedByName,
        LocalDateTime createdAt
) {
}
