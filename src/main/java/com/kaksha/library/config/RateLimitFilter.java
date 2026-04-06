package com.kaksha.library.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter for NFR-04 Performance Compliance.
 * Implements token bucket algorithm using Bucket4j to prevent API abuse
 * and ensure fair resource distribution under concurrent load.
 * 
 * Configuration:
 * - Capacity: 100 requests per client
 * - Refill rate: 10 tokens per second
 * - Period: 1 second
 */
@Component
@Order(1)
@Slf4j
public class RateLimitFilter implements Filter {

    @Value("${app.ratelimit.enabled:true}")
    private boolean enabled;

    @Value("${app.ratelimit.capacity:100}")
    private long capacity;

    @Value("${app.ratelimit.refill-rate:10}")
    private long refillRate;

    @Value("${app.ratelimit.refill-period:1}")
    private long refillPeriod;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Skip rate limiting for public static resources
        String path = httpRequest.getRequestURI();
        if (isPublicResource(path)) {
            chain.doFilter(request, response);
            return;
        }

        String clientId = getClientIdentifier(httpRequest);
        Bucket bucket = buckets.computeIfAbsent(clientId, k -> createBucket());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for client: {}", clientId);
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                "{\"success\":false,\"message\":\"Rate limit exceeded. Please try again later.\"}"
            );
        }
    }

    private Bucket createBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillIntervally(refillRate, Duration.ofSeconds(refillPeriod))
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    private String getClientIdentifier(HttpServletRequest request) {
        String jwt = request.getHeader("Authorization");
        if (jwt != null && jwt.startsWith("Bearer ")) {
            return jwt.substring(7);
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private boolean isPublicResource(String path) {
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/images/") ||
               path.startsWith("/uploads/") ||
               path.startsWith("/assets/") ||
               path.equals("/") ||
               path.equals("/index") ||
               path.equals("/login") ||
               path.equals("/register");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Rate limiting filter initialized with capacity: {}, refill rate: {}/{}s", 
                capacity, refillRate, refillPeriod);
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
