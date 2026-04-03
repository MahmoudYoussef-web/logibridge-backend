package com.logibridge.backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.logibridge.backend.common.exception.ApiErrorResponse;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException ex) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiErrorResponse error = ApiErrorResponse.of(
                401,
                "Authentication required",
                "UNAUTHORIZED",
                request.getRequestURI()
        );

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}