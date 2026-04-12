package com.logibridge.backend.order.exception;

import com.logibridge.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class NoDeliveryUserAvailableException extends ApiException {

    public NoDeliveryUserAvailableException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "NO_DELIVERY_AVAILABLE");
    }
}