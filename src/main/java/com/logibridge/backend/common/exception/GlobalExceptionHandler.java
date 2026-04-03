package com.logibridge.backend.common.exception;

import com.logibridge.backend.auth.exception.InvalidTokenException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    //  CUSTOM API EXCEPTIONS

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {

        log.warn("API Exception [{}]: {} path={}",
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatus()).body(
                ApiErrorResponse.of(
                        ex.getStatus().value(),
                        ex.getMessage(),
                        ex.getErrorCode(),
                        request.getRequestURI()
                )
        );
    }

    //  TOKEN EXCEPTIONS

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request
    ) {

        log.warn("Invalid token path={} msg={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return ResponseEntity.status(401).body(
                ApiErrorResponse.of(
                        401,
                        ex.getMessage(),
                        "INVALID_TOKEN",
                        request.getRequestURI()
                )
        );
    }

    //  VALIDATION

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Validation failed path={} errors={}",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(
                new ApiErrorResponse(
                        400,
                        "Validation failed",
                        "VALIDATION_ERROR",
                        request.getRequestURI(),
                        errors
                )
        );
    }

    //  SECURITY

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuth(
            AuthenticationException ex,
            HttpServletRequest request
    ) {

        log.warn("Authentication failed path={} msg={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return ResponseEntity.status(401).body(
                ApiErrorResponse.of(
                        401,
                        ex.getMessage(), //  keep original message
                        "UNAUTHORIZED",
                        request.getRequestURI()
                )
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {

        log.warn("Access denied path={} msg={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return ResponseEntity.status(403).body(
                ApiErrorResponse.of(
                        403,
                        "Access denied",
                        "FORBIDDEN",
                        request.getRequestURI()
                )
        );
    }

    //  DATABASE

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {

        log.error("Data integrity violation path={} msg={}",
                request.getRequestURI(),
                ex.getMostSpecificCause().getMessage()
        );

        return ResponseEntity.status(409).body(
                ApiErrorResponse.of(
                        409,
                        "Database constraint violation",
                        "DATA_INTEGRITY",
                        request.getRequestURI()
                )
        );
    }

    //  GENERIC

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Unexpected error path={}", request.getRequestURI(), ex);

        return ResponseEntity.status(500).body(
                ApiErrorResponse.of(
                        500,
                        "Internal server error",
                        "INTERNAL_ERROR",
                        request.getRequestURI()
                )
        );
    }
}