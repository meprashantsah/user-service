package com.prashant.user_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Protects the internal endpoints (/internal/**) used for service-to-service
 * communication. Requests must present the shared internal API key, preventing
 * direct exposure to callers who bypass the gateway.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String INTERNAL_API_KEY_HEADER = "X-Internal-Key";

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String presentedKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        if (presentedKey == null || presentedKey.isBlank() || !presentedKey.equals(internalApiKey)) {
            log.warn("Rejected internal call without valid API key from {}", request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Invalid internal API key\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}