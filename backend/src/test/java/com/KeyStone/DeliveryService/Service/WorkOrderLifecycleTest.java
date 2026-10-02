package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.KeyStone.DeliveryService.Enum.WorkOrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

/** The state machine from brief Section 07. */
class WorkOrderLifecycleTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "OPEN, ASSIGNED", "OPEN, CANCELLED",
            "ASSIGNED, IN_PROGRESS", "ASSIGNED, ON_HOLD", "ASSIGNED, CANCELLED",
            "IN_PROGRESS, ON_HOLD", "IN_PROGRESS, COMPLETED",
            "ON_HOLD, IN_PROGRESS",
            "COMPLETED, CLOSED", "COMPLETED, IN_PROGRESS"
    })
    void allowedTransitions(WorkOrderStatus from, WorkOrderStatus to) {
        assertTrue(WorkOrderLifecycle.isAllowed(from, to));
        assertDoesNotThrow(() -> WorkOrderLifecycle.requireAllowed(from, to));
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "OPEN, COMPLETED", "OPEN, IN_PROGRESS", "OPEN, CLOSED",
            "ASSIGNED, COMPLETED", "ASSIGNED, CLOSED",
            "IN_PROGRESS, CLOSED", "IN_PROGRESS, CANCELLED",
            "ON_HOLD, COMPLETED",
            "COMPLETED, CANCELLED"
    })
    void illegalJumpsAreRejected(WorkOrderStatus from, WorkOrderStatus to) {
        assertFalse(WorkOrderLifecycle.isAllowed(from, to));
        assertThrows(IllegalStateException.class, () -> WorkOrderLifecycle.requireAllowed(from, to));
    }

    @Test
    void terminalStatesCannotChange() {
        for (WorkOrderStatus to : WorkOrderStatus.values()) {
            assertThrows(IllegalStateException.class, () -> WorkOrderLifecycle.requireAllowed(CLOSED, to));
            assertThrows(IllegalStateException.class, () -> WorkOrderLifecycle.requireAllowed(CANCELLED, to));
        }
        assertTrue(WorkOrderLifecycle.nextStates(CLOSED).isEmpty());
        assertTrue(WorkOrderLifecycle.nextStates(CANCELLED).isEmpty());
    }

    @Test
    void onlyManagerCanClose() {
        assertNull(WorkOrderLifecycle.denialReason(Role.MANAGER, false, COMPLETED, CLOSED));
        assertNotNull(WorkOrderLifecycle.denialReason(Role.DISPATCHER, false, COMPLETED, CLOSED));
        assertNotNull(WorkOrderLifecycle.denialReason(Role.TECHNICIAN, true, COMPLETED, CLOSED));
    }

    @Test
    void onlyAssignedTechnicianCanStart() {
        assertNull(WorkOrderLifecycle.denialReason(Role.TECHNICIAN, true, ASSIGNED, IN_PROGRESS));
        assertNotNull(WorkOrderLifecycle.denialReason(Role.TECHNICIAN, false, ASSIGNED, IN_PROGRESS));
        assertNotNull(WorkOrderLifecycle.denialReason(Role.CUSTOMER, false, ASSIGNED, IN_PROGRESS));
    }

    @Test
    void techniciansCannotCancelOrReopen() {
        assertNotNull(WorkOrderLifecycle.denialReason(Role.TECHNICIAN, true, ASSIGNED, CANCELLED));
        assertNotNull(WorkOrderLifecycle.denialReason(Role.TECHNICIAN, true, COMPLETED, IN_PROGRESS));
        assertNull(WorkOrderLifecycle.denialReason(Role.DISPATCHER, false, ASSIGNED, CANCELLED));
    }

    @Test
    void assignedOnlyThroughAssignAction() {
        assertNotNull(WorkOrderLifecycle.denialReason(Role.MANAGER, false, OPEN, ASSIGNED));
    }
}
