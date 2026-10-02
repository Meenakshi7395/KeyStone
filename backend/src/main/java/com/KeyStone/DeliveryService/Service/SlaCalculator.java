package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.SlaState;
import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;

import java.time.Duration;
import java.time.Instant;

/** SLA rules in one place: default deadlines per priority and current SLA state. */
public final class SlaCalculator {

    /** A job is "at risk" once it is within this window of its deadline... */
    static final Duration AT_RISK_WINDOW = Duration.ofHours(2);
    /** ...or has used this share of its total SLA window. */
    static final double AT_RISK_FRACTION = 0.8;

    private SlaCalculator() {
    }

    public static Duration targetFor(WorkOrderPriority priority) {
        if (priority == null) {
            return Duration.ofDays(3);
        }
        return switch (priority) {
            case CRITICAL -> Duration.ofHours(4);
            case HIGH -> Duration.ofDays(1);
            case MEDIUM -> Duration.ofDays(3);
            case LOW -> Duration.ofDays(7);
        };
    }

    public static Instant defaultDueDate(WorkOrderPriority priority, Instant from) {
        return from.plus(targetFor(priority));
    }

    public static SlaState stateOf(WorkOrder wo, Instant now) {
        Instant due = wo.getSlaDueDate();
        WorkOrderStatus status = wo.getStatus();
        if (due == null || status == WorkOrderStatus.CANCELLED) {
            return SlaState.NONE;
        }
        if (status == WorkOrderStatus.COMPLETED || status == WorkOrderStatus.CLOSED) {
            Instant done = wo.getCompletedAt() != null ? wo.getCompletedAt()
                    : (wo.getUpdatedAt() != null ? wo.getUpdatedAt() : now);
            return done.isAfter(due) ? SlaState.BREACHED : SlaState.MET;
        }
        if (now.isAfter(due)) {
            return SlaState.BREACHED;
        }
        return isAtRisk(wo, now) ? SlaState.AT_RISK : SlaState.ON_TRACK;
    }

    public static boolean isAtRisk(WorkOrder wo, Instant now) {
        Instant due = wo.getSlaDueDate();
        if (due == null || wo.getStatus() == null || wo.getStatus().isFinished() || now.isAfter(due)) {
            return false;
        }
        Duration remaining = Duration.between(now, due);
        if (remaining.compareTo(AT_RISK_WINDOW) <= 0) {
            return true;
        }
        Instant start = wo.getCreatedAt();
        if (start == null) {
            return false;
        }
        long total = Duration.between(start, due).toMillis();
        long used = Duration.between(start, now).toMillis();
        return total > 0 && (double) used / total >= AT_RISK_FRACTION;
    }

    public static boolean isOverdue(WorkOrder wo, Instant now) {
        return wo.getSlaDueDate() != null
                && wo.getStatus() != null
                && !wo.getStatus().isFinished()
                && now.isAfter(wo.getSlaDueDate());
    }
}
