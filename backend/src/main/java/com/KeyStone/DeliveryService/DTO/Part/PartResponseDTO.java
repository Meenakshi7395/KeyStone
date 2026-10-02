package com.KeyStone.DeliveryService.DTO.Part;

import java.math.BigDecimal;

public record PartResponseDTO(
        Integer id,
        String name,
        Integer stockQuantity,
        String sku,
        BigDecimal unitCost
) {
}
