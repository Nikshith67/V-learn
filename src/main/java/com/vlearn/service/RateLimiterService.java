package com.vlearn.service;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe rate limiter to secure authentication, prevent brute-force attacks,
 * and ensure smooth, balanced AI usage per user without lag or quota exhaustion.
 */
@Service
public class RateLimiterService {

    // Login: Max 5 failed attempts within 2 minutes per IP / username
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOGIN_LOCKOUT_MILLIS = 2 * 60 * 1000L;

    // Register: Max 5 registration attempts within 10 minutes per IP
    private static final int MAX_REGISTER_ATTEMPTS = 5;
    private static final long REGISTER_WINDOW_MILLIS = 10 * 60 * 1000L;

    // AI: Max 12 requests per minute per student, minimum 2-second cooldown between prompts
    private static final int MAX_AI_PER_MINUTE = 12;
    private static final long AI_MIN_INTERVAL_MILLIS = 2000L;
    private static final long AI_WINDOW_MILLIS = 60 * 1000L;

    private static class AttemptTracker {
        int count = 0;
        long lastAttemptTime = 0;
        long windowStartTime = 0;
    }

    private final Map<String, AttemptTracker> loginAttempts = new ConcurrentHashMap<>();
    private final Map<String, AttemptTracker> registerAttempts = new ConcurrentHashMap<>();
    private final Map<Long, AttemptTracker> aiUsage = new ConcurrentHashMap<>();

    // -------------------------------------------------------------------------
    // LOGIN RATE LIMITING
    // -------------------------------------------------------------------------

    public synchronized boolean isLoginBlocked(String ip, String username) {
        long now = System.currentTimeMillis();
        String key = (ip != null ? ip : "") + ":" + (username != null ? username.trim().toLowerCase() : "");
        AttemptTracker tracker = loginAttempts.get(key);
        if (tracker == null) return false;

        // If lockout expired, reset
        if (now - tracker.lastAttemptTime > LOGIN_LOCKOUT_MILLIS) {
            loginAttempts.remove(key);
            return false;
        }

        return tracker.count >= MAX_LOGIN_ATTEMPTS;
    }

    public synchronized void recordFailedLogin(String ip, String username) {
        long now = System.currentTimeMillis();
        String key = (ip != null ? ip : "") + ":" + (username != null ? username.trim().toLowerCase() : "");
        AttemptTracker tracker = loginAttempts.computeIfAbsent(key, k -> new AttemptTracker());

        if (now - tracker.lastAttemptTime > LOGIN_LOCKOUT_MILLIS) {
            tracker.count = 1;
        } else {
            tracker.count++;
        }
        tracker.lastAttemptTime = now;
    }

    public synchronized void recordSuccessfulLogin(String ip, String username) {
        String key = (ip != null ? ip : "") + ":" + (username != null ? username.trim().toLowerCase() : "");
        loginAttempts.remove(key);
    }

    // -------------------------------------------------------------------------
    // REGISTER RATE LIMITING
    // -------------------------------------------------------------------------

    public synchronized boolean isRegisterBlocked(String ip) {
        if (ip == null) return false;
        long now = System.currentTimeMillis();
        AttemptTracker tracker = registerAttempts.get(ip);
        if (tracker == null) return false;

        if (now - tracker.windowStartTime > REGISTER_WINDOW_MILLIS) {
            registerAttempts.remove(ip);
            return false;
        }

        return tracker.count >= MAX_REGISTER_ATTEMPTS;
    }

    public synchronized void recordRegisterAttempt(String ip) {
        if (ip == null) return;
        long now = System.currentTimeMillis();
        AttemptTracker tracker = registerAttempts.computeIfAbsent(ip, k -> {
            AttemptTracker t = new AttemptTracker();
            t.windowStartTime = now;
            return t;
        });

        if (now - tracker.windowStartTime > REGISTER_WINDOW_MILLIS) {
            tracker.count = 1;
            tracker.windowStartTime = now;
        } else {
            tracker.count++;
        }
    }

    // -------------------------------------------------------------------------
    // PER-USER AI USAGE RATE LIMITING & SMOOTHING
    // -------------------------------------------------------------------------

    /**
     * Checks if a user is sending AI queries too quickly (e.g. spamming clicks)
     * or has exceeded their rate limit for the minute.
     *
     * @return null if allowed, or an error message if throttled.
     */
    public synchronized String checkAiRateLimit(Long userId) {
        if (userId == null) return null;
        long now = System.currentTimeMillis();
        AttemptTracker tracker = aiUsage.computeIfAbsent(userId, k -> {
            AttemptTracker t = new AttemptTracker();
            t.windowStartTime = now;
            return t;
        });

        // 1. Check minimum interval between queries (avoid spam clicking)
        if (tracker.lastAttemptTime > 0 && (now - tracker.lastAttemptTime) < AI_MIN_INTERVAL_MILLIS) {
            return "⏳ Please wait 2 seconds between questions.";
        }

        // 2. Check window quota
        if (now - tracker.windowStartTime > AI_WINDOW_MILLIS) {
            tracker.count = 0;
            tracker.windowStartTime = now;
        }

        if (tracker.count >= MAX_AI_PER_MINUTE) {
            long secondsLeft = Math.max(1, (AI_WINDOW_MILLIS - (now - tracker.windowStartTime)) / 1000);
            return "⏳ You have reached the AI question limit for this minute. Please wait " + secondsLeft + "s.";
        }

        // Allowed - update timestamps
        tracker.count++;
        tracker.lastAttemptTime = now;
        return null;
    }
}
