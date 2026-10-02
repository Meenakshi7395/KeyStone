package com.KeyStone.DeliveryService.DTO.Part;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PartRequestDTO(

        @NotBlank(message = "Part name is required")
        @Size(max = 200, message = "Part name must be at most 200 characters")
        String name,

        @NotNull(message = "Stock quantity is required")
        @Min(value = 0, message = "Stock quantity cannot be negative")
        Integer stockQuantity,

        @Size(max = 64, message = "SKU must be at most 64 characters")
        String sku,

        @DecimalMin(value = "0.00", message = "Unit cost cannot be negative")
        BigDecimal unitCost
) {
}
