package com.KeyStone.DeliveryService.DTO.Part;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Add stock to a part (goods received). */
public record RestockRequestDTO(

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity
) {
}
