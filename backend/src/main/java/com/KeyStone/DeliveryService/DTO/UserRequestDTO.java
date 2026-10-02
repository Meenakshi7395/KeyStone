package com.KeyStone.DeliveryService.DTO;

import com.KeyStone.DeliveryService.Enum.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Create / update a user (MANAGER only). On update the password is
 * optional — leave it blank to keep the current one.
 */
public record UserRequestDTO(

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        // Optional: link a CUSTOMER login to an organisation (or re-link it).
        Integer customerId
) {
}
