package com.KeyStone.DeliveryService.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequestDTO(
        @NotBlank @Email String userEmail,
        @NotBlank String password) {
}

