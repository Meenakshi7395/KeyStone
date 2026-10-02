package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.DTO.Notification.NotificationResponseDTO;
import com.KeyStone.DeliveryService.Entity.Notification;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.NotificationType;
import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Repository.NotificationRepository;
import com.KeyStone.DeliveryService.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/** In-app notifications (brief F4.2 / F7.3 — email/SMS are out of scope). */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    /** Notify one user. Joins the caller's transaction. */
    @Transactional
    public void notify(User recipient, NotificationType type, String title, String message, WorkOrder workOrder) {
        if (recipient == null) {
            return;
        }
        Notification n = new Notification();
        n.setRecipient(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setWorkOrder(workOrder);
        notificationRepository.save(n);
        log.info("Notification {} sent to user {} for work order {}", type, recipient.getId(),
                workOrder != null ? workOrder.getId() : null);
    }

    /** Notify every user with one of the given roles, plus any extra users (deduplicated). */
    @Transactional
    public void notifyRoles(Collection<Role> roles, Collection<User> extra, NotificationType type,
                            String title, String message, WorkOrder workOrder) {
        Map<Integer, User> recipients = new LinkedHashMap<>();
        for (User u : userRepository.findByRoleIn(List.copyOf(roles))) {
            recipients.put(u.getId(), u);
        }
        if (extra != null) {
            for (User u : extra) {
                if (u != null) {
                    recipients.putIfAbsent(u.getId(), u);
                }
            }
        }
        recipients.values().forEach(u -> notify(u, type, title, message, workOrder));
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponseDTO> listFor(User caller, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(caller.getId(), pageable)
                : notificationRepository.findByRecipientIdOrderByCreatedAtDesc(caller.getId(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(User caller) {
        return notificationRepository.countByRecipientIdAndReadAtIsNull(caller.getId());
    }

    @Transactional
    public NotificationResponseDTO markRead(User caller, Integer id) {
        // Scoped by recipient: another user's notification id is simply "not found".
        Notification n = notificationRepository.findByIdAndRecipientId(id, caller.getId())
                .orElseThrow(() -> new NoSuchElementException("Notification not found: " + id));
        if (n.getReadAt() == null) {
            n.setReadAt(Instant.now());
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(User caller) {
        return notificationRepository.markAllRead(caller.getId(), Instant.now());
    }

    private NotificationResponseDTO toResponse(Notification n) {
        WorkOrder wo = n.getWorkOrder();
        return new NotificationResponseDTO(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                wo != null ? wo.getId() : null,
                wo != null ? wo.getCode() : null,
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
