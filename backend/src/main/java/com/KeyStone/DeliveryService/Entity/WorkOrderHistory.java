package com.KeyStone.DeliveryService.Entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Append-only audit row. Rows are only ever inserted — there is no
 * update or delete path anywhere in the codebase.
 */
@Entity
@Table(name = "work_order_history")
@Getter
@Setter
public class WorkOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false, updatable = false)
    private WorkOrder workOrder;

    @Column(nullable = false, updatable = false)
    private String action;

    @Column(updatable = false)
    private String oldValue;

    @Column(updatable = false)
    private String newValue;

    // Who made the change (null for system actions such as the SLA monitor).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", updatable = false)
    private User changedBy;

    @Column(length = 1000, updatable = false)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = Instant.now();
    }
}
