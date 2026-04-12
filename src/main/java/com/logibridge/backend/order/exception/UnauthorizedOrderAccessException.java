package com.logibridge.backend.order.exception;

import com.logibridge.backend.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class UnauthorizedOrderAccessException extends ApiException {

    public UnauthorizedOrderAccessException(String message) {
        super(message, HttpStatus.FORBIDDEN, "ORDER_ACCESS_DENIED");
    }
}