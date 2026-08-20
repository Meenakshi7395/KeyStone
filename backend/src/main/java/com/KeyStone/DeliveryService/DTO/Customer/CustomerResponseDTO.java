package com.KeyStone.DeliveryService.DTO.Customer;

import java.time.Instant;

public record CustomerResponseDTO(
        Integer id,
        String name,
        String contactEmail,
        Instant createdAt) {
}
