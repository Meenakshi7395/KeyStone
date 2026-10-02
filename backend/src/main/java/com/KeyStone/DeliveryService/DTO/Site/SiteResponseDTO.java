package com.KeyStone.DeliveryService.DTO.Site;

import java.time.Instant;

public record SiteResponseDTO(
        Integer id,
        String name,
        String address,
        Instant createdAt,
        Integer customerId,
        String customerName
) {
}
