package com.KeyStone.DeliveryService.Service;

import com.KeyStone.DeliveryService.Enum.Role;
import com.KeyStone.DeliveryService.Enum.WorkOrderStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.KeyStone.DeliveryService.Enum.WorkOrderStatus.*;

/**
 * The governed work-order state machine (brief Section 07), kept free of
 * Spring so it can be unit-tested directly.
 *
 * <pre>
 *  OPEN ──assign──▶ ASSIGNED ──start──▶ IN_PROGRESS ──complete──▶ COMPLETED ──close──▶ CLOSED
 *    │                 │   ▲                │   ▲                     │
 *    │                 │   └──── ON_HOLD ◀──┘   └───────reopen────────┘
 *    └──cancel──▶ CANCELLED ◀──cancel──┘
 * </pre>
 */
public final class WorkOrderLifecycle {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> ALLOWED = new EnumMap<>(WorkOrderStatus.class);

    static {
        ALLOWED.put(OPEN, EnumSet.of(ASSIGNED, CANCELLED));
        ALLOWED.put(ASSIGNED, EnumSet.of(IN_PROGRESS, ON_HOLD, CANCELLED));
        ALLOWED.put(IN_PROGRESS, EnumSet.of(ON_HOLD, COMPLETED));
        ALLOWED.put(ON_HOLD, EnumSet.of(IN_PROGRESS));
        ALLOWED.put(COMPLETED, EnumSet.of(CLOSED, IN_PROGRESS));
        ALLOWED.put(CLOSED, EnumSet.noneOf(WorkOrderStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(WorkOrderStatus.class));
    }

    private WorkOrderLifecycle() {
    }

    public static boolean isAllowed(WorkOrderStatus from, WorkOrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return ALLOWED.getOrDefault(from, Set.of()).contains(to);
    }

    public static Set<WorkOrderStatus> nextStates(WorkOrderStatus from) {
        return from == null ? Set.of() : EnumSet.copyOf(nonEmpty(ALLOWED.get(from)));
    }

    /** Throws IllegalStateException (→ 409) when the jump isn't on the diagram. */
    public static void requireAllowed(WorkOrderStatus from, WorkOrderStatus to) {
        if (from == null) {
            throw new IllegalStateException("Current work order status is missing");
        }
        if (to == null) {
            throw new IllegalStateException("New work order status is required");
        }
        if (from.isTerminal()) {
            throw new IllegalStateException("Work order is " + from + " and can no longer change");
        }
        if (!isAllowed(from, to)) {
            throw new IllegalStateException("Illegal status transition from " + from + " to " + to);
        }
    }

    /**
     * Role rules on top of the state machine. Returns a reason string when the
     * caller may NOT make this move, or null when it is permitted.
     *
     * @param assignedToCaller true when the caller is the job's assigned technician
     */
    public static String denialReason(Role role, boolean assignedToCaller,
                                      WorkOrderStatus from, WorkOrderStatus to) {
        if (to == ASSIGNED) {
            return "Use the assign action to move a job to ASSIGNED";
        }
        boolean staff = role == Role.MANAGER || role == Role.DISPATCHER;
        if (to == CLOSED) {
            return role == Role.MANAGER ? null : "Only a manager can close a work order";
        }
        if (to == CANCELLED) {
            return staff ? null : "Only a dispatcher or manager can cancel a work order";
        }
        if (from == COMPLETED && to == IN_PROGRESS) {
            return staff ? null : "Only a dispatcher or manager can reopen a completed job";
        }
        if (staff) {
            return null;
        }
        if (role == Role.TECHNICIAN) {
            return assignedToCaller ? null : "Only the assigned technician can update this job's status";
        }
        return "Not permitted to change this work order's status";
    }

    private static Set<WorkOrderStatus> nonEmpty(Set<WorkOrderStatus> s) {
        return (s == null || s.isEmpty()) ? EnumSet.noneOf(WorkOrderStatus.class) : s;
    }
}
