package com.KeyStone.DeliveryService.DTO.Notification;

import com.KeyStone.DeliveryService.Enum.NotificationType;

import java.time.Instant;

public record NotificationResponseDTO(
        Integer id,
        NotificationType type,
        String title,
        String message,
        Integer workOrderId,
        String workOrderCode,
        Instant createdAt,
        Instant readAt
) {
}
