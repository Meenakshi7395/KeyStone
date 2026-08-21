package com.KeyStone.DeliveryService.DTO.WorkOrder;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LogWorkOrderTimeRequestDTO(

        @NotNull(message = "Hours are required")
        @Min(
                value = 1,
                message = "Hours must be at least 1"
        )
        Integer hours,

        String description

) {
}
