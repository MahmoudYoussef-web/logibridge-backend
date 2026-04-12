package com.logibridge.backend.order.exception;

import com.logibridge.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {

    public OrderNotFoundException(String orderNumber) {
        super(
                "Order not found with orderNumber: " + orderNumber,
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND"
        );
    }
}