package com.KeyStone.DeliveryService.DTO.Site;

import jakarta.validation.constraints.NotBlank;

public record SiteRequestDTO(

        @NotBlank(message = "Site name is required")
        String name,

        @NotBlank(message = "Site address is required")
        String address

) {
}