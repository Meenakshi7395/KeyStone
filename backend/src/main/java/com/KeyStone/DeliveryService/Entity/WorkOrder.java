package com.KeyStone.DeliveryService.Entity;

import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Human-readable code, e.g. WO-0042 (set right after the first insert).
    // Nullable so ddl-auto=update can add it to an existing table.
    @Column(unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    // Customer who owns this work order
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    // Site where the work needs to be done
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    // Technician assigned to the job (null until assigned)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private User technician;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkOrderPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkOrderStatus status;

    // SLA deadline for completing the work order
    private Instant slaDueDate;

    // Set by the SLA monitor so each alert is only sent once.
    private Instant slaRiskNotifiedAt;
    private Instant slaBreachNotifiedAt;

    // Lifecycle timestamps used for SLA compliance and reporting.
    private Instant completedAt;
    private Instant closedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = WorkOrderStatus.OPEN;
        }
        if (priority == null) {
            priority = WorkOrderPriority.MEDIUM;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
