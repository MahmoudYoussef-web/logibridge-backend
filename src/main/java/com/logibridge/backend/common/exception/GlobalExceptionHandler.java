package com.logibridge.backend.common.exception;

import com.logibridge.backend.auth.exception.InvalidTokenException;
import com.logibridge.backend.order.exception.InvalidOrderStateException;
import com.logibridge.backend.order.exception.NoDeliveryUserAvailableException;
import com.logibridge.backend.order.exception.OrderNotFoundException;
import com.logibridge.backend.order.exception.UnauthorizedOrderAccessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException ex, HttpServletRequest request) {
        log.warn("API Exception [{}]: {} path={}", ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(
                ApiErrorResponse.of(ex.getStatus().value(), ex.getMessage(), ex.getErrorCode(), request.getRequestURI()));
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(
            InvalidTokenException ex, HttpServletRequest request) {
        return ResponseEntity.status(401).body(
                ApiErrorResponse.of(401, ex.getMessage(), "INVALID_TOKEN", request.getRequestURI()));
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(
            OrderNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(404).body(
                ApiErrorResponse.of(404, ex.getMessage(), "NOT_FOUND", request.getRequestURI()));
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidState(
            InvalidOrderStateException ex, HttpServletRequest request) {
        return ResponseEntity.status(422).body(
                ApiErrorResponse.of(422, ex.getMessage(), "INVALID_STATE", request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedOrderAccessException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorizedAccess(
            UnauthorizedOrderAccessException ex, HttpServletRequest request) {
        return ResponseEntity.status(403).body(
                ApiErrorResponse.of(403, ex.getMessage(), "FORBIDDEN", request.getRequestURI()));
    }

    @ExceptionHandler(NoDeliveryUserAvailableException.class)
    public ResponseEntity<ApiErrorResponse> handleNoDelivery(
            NoDeliveryUserAvailableException ex, HttpServletRequest request) {
        return ResponseEntity.status(503).body(
                ApiErrorResponse.of(503, ex.getMessage(), "NO_DELIVERY_AVAILABLE", request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(400, "Validation failed", "VALIDATION_ERROR", request.getRequestURI(), errors));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(
            AuthenticationException ex, HttpServletRequest request) {
        return ResponseEntity.status(401).body(
                ApiErrorResponse.of(401, ex.getMessage(), "UNAUTHORIZED", request.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        return ResponseEntity.status(403).body(
                ApiErrorResponse.of(403, "Access denied", "FORBIDDEN", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(409).body(
                ApiErrorResponse.of(409, "Database constraint violation", "DATA_INTEGRITY", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(500).body(
                ApiErrorResponse.of(500, "Internal server error", "INTERNAL_ERROR", request.getRequestURI()));
    }
}