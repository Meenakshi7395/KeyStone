package com.KeyStone.DeliveryService.DTO;

import com.KeyStone.DeliveryService.Enum.Role;

public record UserRequestDTO(
        String name,
        String email,
        String password,
        Role role
) {
}