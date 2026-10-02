package com.backend.ShopSphere.security;

import com.backend.ShopSphere.dto.ErrorResponse;
import com.backend.ShopSphere.exception.UnauthorizedException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        Exception customException = (Exception) request.getAttribute("exception");

        String message;
        int status = HttpServletResponse.SC_UNAUTHORIZED;

        if (customException instanceof UnauthorizedException) {
            message = customException.getMessage();
        } else if (customException != null) {
            message = customException.getMessage();
        } else if (authException != null) {
            // Handle Spring Security's default authentication exceptions
            if (authException instanceof InsufficientAuthenticationException) {
                message = "Authentication required";
            } else {
                message = authException.getMessage();
            }
        } else {
            message = "Unauthorized";
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .message(message)
                .build();

        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

    }

}