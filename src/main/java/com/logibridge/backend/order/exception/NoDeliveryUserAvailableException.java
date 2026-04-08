package com.logibridge.backend.order.exception;

public class NoDeliveryUserAvailableException extends RuntimeException {

    public NoDeliveryUserAvailableException(String message) {
        super(message);
    }
}