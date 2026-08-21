
package com.KeyStone.DeliveryService.DTO.WorkOrder;

import java.time.Instant;

public record WorkOrderHistoryResponseDTO(

        Integer id,

        String action,

        String oldValue,

        String newValue,

        Instant createdAt

) {
}
