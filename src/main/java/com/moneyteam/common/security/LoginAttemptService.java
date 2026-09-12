package com.moneyteam.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks consecutive failed logins per username and locks the account for a
 * cooling-off period once the threshold is reached.
 *
 * State is in-memory and therefore per-instance: it is lost on restart and is
 * not shared across replicas. That is adequate for the current single-instance
 * deployment, but this must move to Redis (or another shared store) before the
 * app is scaled out, otherwise an attacker can dodge the lockout by spreading
 * attempts across instances.
 */
@Service
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration lockDuration;

    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final Map<String, Instant> lockedUntil = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.security.login.max-attempts:5}") int maxAttempts,
            @Value("${app.security.login.lock-minutes:15}") long lockMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockDuration = Duration.ofMinutes(lockMinutes);
    }

    public boolean isLocked(String userName) {
        Instant until = lockedUntil.get(userName);
        if (until == null) {
            return false;
        }
        if (Instant.now().isAfter(until)) {
            // Lock expired - clear it so the user gets a fresh set of attempts.
            lockedUntil.remove(userName);
            attempts.remove(userName);
            return false;
        }
        return true;
    }

    public void recordFailure(String userName) {
        int count = attempts.computeIfAbsent(userName, k -> new AtomicInteger()).incrementAndGet();
        if (count >= maxAttempts) {
            lockedUntil.put(userName, Instant.now().plus(lockDuration));
        }
    }

    public void recordSuccess(String userName) {
        attempts.remove(userName);
        lockedUntil.remove(userName);
    }

    public Duration getLockDuration() {
        return lockDuration;
    }
}
