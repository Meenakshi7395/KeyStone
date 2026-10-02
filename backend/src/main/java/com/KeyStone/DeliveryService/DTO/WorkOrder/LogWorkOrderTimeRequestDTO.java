package com.KeyStone.DeliveryService.DTO.WorkOrder;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Time entry. Send {@code minutes} (preferred). {@code hours} is still
 * accepted for older clients and converted to minutes.
 */
public record LogWorkOrderTimeRequestDTO(

        @Min(value = 1, message = "Minutes must be at least 1")
        @Max(value = 24 * 60, message = "A single entry can't exceed 24 hours")
        Integer minutes,

        @Min(value = 1, message = "Hours must be at least 1")
        @Max(value = 24, message = "A single entry can't exceed 24 hours")
        Integer hours,

        @Size(max = 1000, message = "Note must be at most 1000 characters")
        String description
) {
}
