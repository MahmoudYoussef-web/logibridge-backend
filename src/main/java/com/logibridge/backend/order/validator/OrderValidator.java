package com.logibridge.backend.order.validator;

import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.enums.OrderStatus;
import com.logibridge.backend.order.exception.InvalidOrderStateException;
import com.logibridge.backend.order.exception.UnauthorizedOrderAccessException;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

    public void validateOwnership(Order order, Long companyId) {

        if (order == null || companyId == null) {
            throw new UnauthorizedOrderAccessException("Invalid ownership validation request");
        }

        if (!order.isOwnedByCompany(companyId)) {
            throw new UnauthorizedOrderAccessException(
                    "Company " + companyId + " does not own order " + order.getOrderNumber());
        }
    }

    public void validateDeliveryAccess(Order order, Long deliveryCompanyId) {

        if (order == null || deliveryCompanyId == null) {
            throw new UnauthorizedOrderAccessException("Invalid delivery validation request");
        }

        if (!order.isAssignedToDelivery(deliveryCompanyId)) {
            throw new UnauthorizedOrderAccessException(
                    "Delivery company " + deliveryCompanyId +
                            " is not assigned to order " + order.getOrderNumber());
        }
    }

    public void validateTransition(Order order, OrderStatus target) {

        if (order == null) {
            throw new InvalidOrderStateException("Order must not be null");
        }

        if (order.getStatus() == null) {
            throw new InvalidOrderStateException("Order status is not initialized");
        }

        if (target == null) {
            throw new InvalidOrderStateException("Target status must not be null");
        }

        if (!order.getStatus().canTransitionTo(target)) {
            throw new InvalidOrderStateException(
                    "Invalid transition from " + order.getStatus() + " to " + target
            );
        }
    }
}