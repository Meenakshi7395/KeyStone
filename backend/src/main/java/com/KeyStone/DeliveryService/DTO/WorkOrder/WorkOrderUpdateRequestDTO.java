package com.KeyStone.DeliveryService.DTO.WorkOrder;

import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/** Edit a work order while it is still open (not CLOSED / CANCELLED). */
public record WorkOrderUpdateRequestDTO(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Site ID is required")
        Integer siteId,

        @NotNull(message = "Priority is required")
        WorkOrderPriority priority,

        // Optional: keep the current deadline when omitted.
        Instant slaDueDate
) {
}
