
package com.KeyStone.DeliveryService.DTO.Part;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PartRequestDTO(

        @NotBlank(message = "Part name is required")
        String name,

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity

) {
}
