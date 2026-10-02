package com.KeyStone.DeliveryService.Repository;

import com.KeyStone.DeliveryService.Entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Integer recipientId, Pageable pageable);

    Page<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Integer recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Integer recipientId);

    Optional<Notification> findByIdAndRecipientId(Integer id, Integer recipientId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.recipient.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Integer userId, @Param("now") Instant now);
}
