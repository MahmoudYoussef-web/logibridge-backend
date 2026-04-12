package com.logibridge.backend.order.enums;

public enum OrderStatus {
    PENDING,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    DELIVERED,
    REJECTED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING     -> next == ASSIGNED || next == CANCELLED;
            case ASSIGNED    -> next == ACCEPTED || next == REJECTED;
            case ACCEPTED    -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == DELIVERED || next == CANCELLED;
            case DELIVERED, REJECTED, CANCELLED -> false;
        };
    }
    public boolean isFinalState() {
        return this == DELIVERED || this == REJECTED || this == CANCELLED;
    }
}