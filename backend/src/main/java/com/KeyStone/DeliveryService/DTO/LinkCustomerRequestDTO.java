package com.KeyStone.DeliveryService.DTO;

import jakarta.validation.constraints.NotNull;

public record LinkCustomerRequestDTO(
        @NotNull(message = "Customer ID is required") Integer customerId
) {
}
