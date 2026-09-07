package com.moneyteam.common.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP token-bucket throttle on the unauthenticated endpoints, which are the
 * ones worth brute-forcing. Runs ahead of the JWT filter so rejected requests
 * never reach authentication.
 *
 * Buckets are in-memory, with the same single-instance caveat as
 * {@link LoginAttemptService}.
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final Duration refillPeriod;

    public RateLimitFilter(
            @Value("${app.security.rate-limit.capacity:20}") int capacity,
            @Value("${app.security.rate-limit.refill-minutes:1}") long refillMinutes) {
        this.capacity = capacity;
        this.refillPeriod = Duration.ofMinutes(refillMinutes);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !(path.startsWith("/api/auth/")
                || path.equals("/api/users/login")
                || path.equals("/api/users/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Bucket bucket = buckets.computeIfAbsent(clientIp(request), k -> newBucket());

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private Bucket newBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity, Refill.intervally(capacity, refillPeriod)))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // Left-most entry is the originating client when behind a proxy.
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
