package com.KeyStone.DeliveryService.DTO.WorkOrder;

import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWorkOrderStatusRequestDTO(

        @NotNull(message = "Status is required")
        WorkOrderStatus status,

        // Optional note stored on the status-history row.
        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String note
) {
}
