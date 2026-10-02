package com.KeyStone.DeliveryService.Controller;

import com.KeyStone.DeliveryService.DTO.Notification.NotificationResponseDTO;
import com.KeyStone.DeliveryService.Entity.User;
import com.KeyStone.DeliveryService.Service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** The signed-in user's own in-app notifications (assignment, SLA risk/breach, completion). */
@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** GET /api/notifications?unreadOnly=true&page=0&size=20 */
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDTO>> list(@AuthenticationPrincipal User caller,
                                                              @RequestParam(defaultValue = "false") boolean unreadOnly,
                                                              @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(notificationService.listFor(caller, unreadOnly, pageable));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(Map.of("unread", notificationService.unreadCount(caller)));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<NotificationResponseDTO> markRead(@AuthenticationPrincipal User caller, @PathVariable Integer id) {
        return ResponseEntity.ok(notificationService.markRead(caller, id));
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<Map<String, Integer>> markAllRead(@AuthenticationPrincipal User caller) {
        return ResponseEntity.ok(Map.of("updated", notificationService.markAllRead(caller)));
    }
}
