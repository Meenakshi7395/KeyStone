package com.KeyStone.DeliveryService.DTO.WorkOrder;

import jakarta.validation.constraints.NotNull;

public record AssignTechnicianRequestDTO(
        @NotNull(message = "Technician ID is required")
        Integer technicianId
) {
}
