package com.spiceroute.feedback.common;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter (single-instance).
 *
 * Two usage modes:
 *   1. tryAcquire()      — rate-limits any action (e.g. public feedback submission).
 *   2. recordFailure() / isLockedOut() / reset()
 *                        — tracks repeated failures (e.g. login attempts).
 */
@Component
public class SlidingWindowRateLimiter {

    private static final int  LOGIN_MAX_FAILURES = 10;
    private static final Duration LOGIN_WINDOW   = Duration.ofMinutes(15);
    private static final long LONGEST_WINDOW_MS  = Duration.ofHours(1).toMillis();
    private static final int  CLEANUP_THRESHOLD  = 10_000;

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    // ── General rate-limit (feedback submission, etc.) ──────────────────────

    /** Records a hit and returns true if the key is still within its limit. */
    public boolean tryAcquire(String key, int max, Duration window) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        if (hits.size() > CLEANUP_THRESHOLD) cleanup(now);

        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < cutoff) queue.pollFirst();
            if (queue.size() >= max) return false;
            queue.addLast(now);
            return true;
        }
    }

    // ── Login failure tracking ───────────────────────────────────────────────

    /**
     * Records one failed attempt for the given key (e.g. "login:{email}").
     * Does NOT block; call isLockedOut() separately to check.
     */
    public void recordFailure(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            queue.addLast(now);
        }
    }

    /**
     * Returns true when the key has accumulated LOGIN_MAX_FAILURES or more
     * failures within the LOGIN_WINDOW.
     */
    public boolean isLockedOut(String key) {
        long now = System.currentTimeMillis();
        long cutoff = now - LOGIN_WINDOW.toMillis();
        Deque<Long> queue = hits.get(key);
        if (queue == null) return false;
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < cutoff) queue.pollFirst();
            return queue.size() >= LOGIN_MAX_FAILURES;
        }
    }

    /** Clears all recorded failures for the key (call on successful login). */
    public void reset(String key) {
        hits.remove(key);
    }

    // ── Internal cleanup ────────────────────────────────────────────────────

    private void cleanup(long now) {
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long last = e.getValue().peekLast();
                return last == null || last < now - LONGEST_WINDOW_MS;
            }
        });
    }
}
