package com.logibridge.backend.order.validator;

import com.logibridge.backend.order.entity.Order;
import com.logibridge.backend.order.exception.UnauthorizedOrderAccessException;
import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

    public void validateOwnership(Order order, Long companyId) {
        if (!order.isOwnedByCompany(companyId)) {
            throw new UnauthorizedOrderAccessException(
                    "Company " + companyId + " does not own order " + order.getOrderNumber());
        }
    }

    public void validateDeliveryAccess(Order order, Long deliveryCompanyId) {
        if (!order.isAssignedToDelivery(deliveryCompanyId)) {
            throw new UnauthorizedOrderAccessException(
                    "Delivery company " + deliveryCompanyId +
                            " is not assigned to order " + order.getOrderNumber());
        }
    }
}