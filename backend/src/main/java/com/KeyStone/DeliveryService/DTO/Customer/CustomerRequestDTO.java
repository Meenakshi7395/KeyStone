package com.KeyStone.DeliveryService.DTO.Customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerRequestDTO(
        @NotBlank String name,
        @NotBlank @Email String contactEmail) {
}