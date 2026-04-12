package com.logibridge.backend.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateRequestException extends ApiException {

    public DuplicateRequestException(String message) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_REQUEST");
    }
}