package com.logibridge.backend.order.enums;

public enum OrderStatus {
    PENDING,
    IN_PROGRESS,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING     -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == DELIVERED   || next == CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}