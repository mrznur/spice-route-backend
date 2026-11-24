package com.spiceroute.feedback.common;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Small in-memory sliding-window limiter (single server instance). Good enough for
 * public-form spam protection; swap for Redis if the backend is ever scaled out.
 */
@Component
public class SlidingWindowRateLimiter {

    private static final long LONGEST_WINDOW_MS = Duration.ofHours(1).toMillis();
    private static final int CLEANUP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

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

    private void cleanup(long now) {
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                Long last = e.getValue().peekLast();
                return last == null || last < now - LONGEST_WINDOW_MS;
            }
        });
    }
}
