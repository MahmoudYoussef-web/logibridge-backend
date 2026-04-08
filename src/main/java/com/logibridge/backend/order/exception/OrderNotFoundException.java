package com.logibridge.backend.order.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderNumber) {
        super("Order not found with orderNumber: " + orderNumber);
    }
}