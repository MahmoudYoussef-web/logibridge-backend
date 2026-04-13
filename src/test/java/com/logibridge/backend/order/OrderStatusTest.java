package com.logibridge.backend.order;

import com.logibridge.backend.order.enums.OrderStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    @Test
    void pending_can_transition_to_assigned() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.ASSIGNED)).isTrue();
    }

    @Test
    void pending_can_be_cancelled() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    void pending_cannot_go_to_delivered() {
        assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    void assigned_can_be_accepted() {
        assertThat(OrderStatus.ASSIGNED.canTransitionTo(OrderStatus.ACCEPTED)).isTrue();
    }

    @Test
    void assigned_can_be_rejected() {
        assertThat(OrderStatus.ASSIGNED.canTransitionTo(OrderStatus.REJECTED)).isTrue();
    }

    @Test
    void delivered_cannot_transition_to_anything() {
        for (OrderStatus next : OrderStatus.values()) {
            assertThat(OrderStatus.DELIVERED.canTransitionTo(next)).isFalse();
        }
    }

    @Test
    void cancelled_is_terminal() {
        for (OrderStatus next : OrderStatus.values()) {
            assertThat(OrderStatus.CANCELLED.canTransitionTo(next)).isFalse();
        }
    }

    @Test
    void rejected_is_terminal() {
        for (OrderStatus next : OrderStatus.values()) {
            assertThat(OrderStatus.REJECTED.canTransitionTo(next)).isFalse();
        }
    }
}