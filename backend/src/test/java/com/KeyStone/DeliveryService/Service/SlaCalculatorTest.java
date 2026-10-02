package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.Entity.WorkOrder;
import com.KeyStone.DeliveryService.Enum.SlaState;
import com.KeyStone.DeliveryService.Enum.WorkOrderPriority;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SlaCalculatorTest {

    private final Instant now = Instant.parse("2026-09-30T10:00:00Z");

    private WorkOrder wo(WorkOrderStatus status, Instant created, Instant due) {
        WorkOrder wo = new WorkOrder();
        wo.setStatus(status);
        wo.setCreatedAt(created);
        wo.setSlaDueDate(due);
        return wo;
    }

    @Test
    void defaultDeadlineFollowsPriority() {
        assertEquals(now.plus(Duration.ofHours(4)), SlaCalculator.defaultDueDate(WorkOrderPriority.CRITICAL, now));
        assertEquals(now.plus(Duration.ofDays(1)), SlaCalculator.defaultDueDate(WorkOrderPriority.HIGH, now));
        assertEquals(now.plus(Duration.ofDays(3)), SlaCalculator.defaultDueDate(WorkOrderPriority.MEDIUM, now));
        assertEquals(now.plus(Duration.ofDays(7)), SlaCalculator.defaultDueDate(WorkOrderPriority.LOW, now));
    }

    @Test
    void activeJobPastDueIsBreachedAndOverdue() {
        WorkOrder w = wo(WorkOrderStatus.IN_PROGRESS, now.minus(Duration.ofDays(2)), now.minus(Duration.ofMinutes(1)));
        assertEquals(SlaState.BREACHED, SlaCalculator.stateOf(w, now));
        assertTrue(SlaCalculator.isOverdue(w, now));
    }

    @Test
    void jobNearDeadlineIsAtRisk() {
        WorkOrder w = wo(WorkOrderStatus.ASSIGNED, now.minus(Duration.ofDays(1)), now.plus(Duration.ofMinutes(90)));
        assertEquals(SlaState.AT_RISK, SlaCalculator.stateOf(w, now));
        assertFalse(SlaCalculator.isOverdue(w, now));
    }

    @Test
    void freshJobIsOnTrack() {
        WorkOrder w = wo(WorkOrderStatus.OPEN, now, now.plus(Duration.ofDays(3)));
        assertEquals(SlaState.ON_TRACK, SlaCalculator.stateOf(w, now));
    }

    @Test
    void finishedJobsAreJudgedOnCompletionTime() {
        WorkOrder onTime = wo(WorkOrderStatus.COMPLETED, now.minus(Duration.ofDays(2)), now.minus(Duration.ofDays(1)));
        onTime.setCompletedAt(now.minus(Duration.ofDays(1)).minus(Duration.ofHours(1)));
        assertEquals(SlaState.MET, SlaCalculator.stateOf(onTime, now));
        assertFalse(SlaCalculator.isOverdue(onTime, now), "finished jobs are never overdue");

        WorkOrder late = wo(WorkOrderStatus.CLOSED, now.minus(Duration.ofDays(2)), now.minus(Duration.ofDays(1)));
        late.setCompletedAt(now.minus(Duration.ofHours(2)));
        assertEquals(SlaState.BREACHED, SlaCalculator.stateOf(late, now));
    }

    @Test
    void cancelledJobsHaveNoSla() {
        WorkOrder w = wo(WorkOrderStatus.CANCELLED, now.minus(Duration.ofDays(2)), now.minus(Duration.ofDays(1)));
        assertEquals(SlaState.NONE, SlaCalculator.stateOf(w, now));
    }
}
