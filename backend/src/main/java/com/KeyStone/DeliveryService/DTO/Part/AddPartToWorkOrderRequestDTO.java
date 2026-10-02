package com.KeyStone.DeliveryService.DTO.Part;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddPartToWorkOrderRequestDTO(

        @NotNull(message = "Part ID is required")
        Integer partId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}
