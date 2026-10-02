package com.KeyStone.DeliveryService.DTO.WorkOrder;

import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record WorkOrderRequestDTO(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        // Ignored for CUSTOMER callers — their linked organisation is used.
        @NotNull(message = "Customer ID is required")
        Integer customerId,

        @NotNull(message = "Site ID is required")
        Integer siteId,

        @NotNull(message = "Priority is required")
        WorkOrderPriority priority,

        // Optional; derived from priority when omitted (and always for customers).
        Instant slaDueDate
) {
}
