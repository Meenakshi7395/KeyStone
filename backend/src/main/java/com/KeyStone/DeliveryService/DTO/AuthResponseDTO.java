package com.KeyStone.DeliveryService.DTO;

public record AuthResponseDTO(
        String token,
        UserResponseDTO user
) {
}