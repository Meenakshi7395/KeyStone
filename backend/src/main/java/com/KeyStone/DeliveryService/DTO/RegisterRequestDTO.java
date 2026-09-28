package com.KeyStone.DeliveryService.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration payload. Deliberately has no `role` field —
 * anyone hitting this endpoint unauthenticated becomes a CUSTOMER account
 * only. Staff accounts (DISPATCHER/TECHNICIAN/MANAGER) can only be created
 * by an existing MANAGER via POST /api/users.
 */
public record RegisterRequestDTO(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password
) {
}
