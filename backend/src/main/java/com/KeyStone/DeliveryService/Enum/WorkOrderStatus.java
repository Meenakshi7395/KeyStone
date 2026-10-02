package com.KeyStone.DeliveryService.Enum;

public enum WorkOrderStatus {

    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    ON_HOLD,
    COMPLETED,
    CLOSED,
    CANCELLED;

    /** Terminal states can never transition again. */
    public boolean isTerminal() {
        return this == CLOSED || this == CANCELLED;
    }

    /** Work is finished (successfully or not) — no longer counts as active. */
    public boolean isFinished() {
        return this == COMPLETED || this == CLOSED || this == CANCELLED;
    }
}
