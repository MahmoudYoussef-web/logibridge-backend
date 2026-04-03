package com.logibridge.backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logibridge.backend.common.exception.ApiErrorResponse;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse error = ApiErrorResponse.of(
                403,
                "Access denied",
                "FORBIDDEN",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}