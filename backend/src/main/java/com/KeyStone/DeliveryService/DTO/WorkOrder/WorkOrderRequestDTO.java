package com.KeyStone.DeliveryService.DTO.WorkOrder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkOrderRequestDTO(

        @NotBlank(message = "Title is required")
        String title,

        String description,

        @NotNull(message = "Customer ID is required")
        Integer customerId,

        @NotNull(message = "Site ID is required")
        Integer siteId

) {
}