package com.KeyStone.DeliveryService.DTO.WorkOrder;

import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record WorkOrderRequestDTO(

        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Customer ID is required")
        Integer customerId,

        @NotNull(message = "Site ID is required")
        Integer siteId,

        @NotNull(message = "Priority is required")
        WorkOrderPriority priority,

        Instant slaDueDate

) {
}
