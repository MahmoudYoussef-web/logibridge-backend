package com.logibridge.backend.order.exception;

import com.logibridge.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidOrderStateException extends ApiException {

    public InvalidOrderStateException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ORDER_STATE");
    }
}