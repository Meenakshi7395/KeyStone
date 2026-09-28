package com.KeyStone.DeliveryService.DTO;

import com.KeyStone.DeliveryService.Enum.Role;

import java.time.Instant;

public record UserResponseDTO(
        Integer id,
        String name,
        String email,
        Role role,
        Instant createdAt,
        Integer customerId
) {
}