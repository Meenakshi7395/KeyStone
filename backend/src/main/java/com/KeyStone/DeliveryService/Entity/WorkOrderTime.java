package com.KeyStone.DeliveryService.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order_times")
@Getter
@Setter
public class WorkOrderTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    // Labour in minutes (the source of truth).
    private Integer minutes;

    // Legacy whole-hours column kept so existing databases keep working.
    @Column(nullable = false)
    private Integer hours;

    // Note on what was done.
    @Column(length = 1000)
    private String description;

    // The technician (or staff member) who logged the time.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "logged_by_id")
    private User loggedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
