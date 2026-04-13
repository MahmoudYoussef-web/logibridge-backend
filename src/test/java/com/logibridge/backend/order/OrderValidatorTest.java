package com.logibridge.backend.order;
import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.enums.OrderStatus;
import com.logibridge.backend.order.exception.InvalidOrderStateException;
import com.logibridge.backend.order.exception.UnauthorizedOrderAccessException;
import com.logibridge.backend.order.validator.OrderValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderValidatorTest {

    private OrderValidator validator;

    @BeforeEach
    void setUp() {
        validator = new OrderValidator();
    }

    @Test
    void validateOwnership_throws_when_company_does_not_own_order() {
        Order order = mock(Order.class);
        when(order.isOwnedByCompany(99L)).thenReturn(false);
        when(order.getOrderNumber()).thenReturn("ORD-001");

        assertThatThrownBy(() -> validator.validateOwnership(order, 99L))
                .isInstanceOf(UnauthorizedOrderAccessException.class);
    }

    @Test
    void validateOwnership_passes_when_company_owns_order() {
        Order order = mock(Order.class);
        when(order.isOwnedByCompany(1L)).thenReturn(true);

        assertThatCode(() -> validator.validateOwnership(order, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    void validateTransition_throws_on_invalid_transition() {
        Order order = mock(Order.class);
        when(order.getStatus()).thenReturn(OrderStatus.DELIVERED);

        assertThatThrownBy(() -> validator.validateTransition(order, OrderStatus.IN_PROGRESS))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    void validateTransition_throws_when_order_is_null() {
        assertThatThrownBy(() -> validator.validateTransition(null, OrderStatus.ACCEPTED))
                .isInstanceOf(InvalidOrderStateException.class);
    }
}
