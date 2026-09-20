package com.csa.official.common.cache;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;

public class PublicRateLimitFallback {
    static final int MAX_KEYS = 4096;
    private final Map<String, Window> windows = new HashMap<>();
    private final LongSupplier clock;

    public PublicRateLimitFallback() {
        this(System::currentTimeMillis);
    }

    PublicRateLimitFallback(LongSupplier clock) {
        this.clock = clock;
    }

    public synchronized long increment(String key, int seconds) {
        long now = clock.getAsLong();
        Window current = windows.get(key);
        if (current != null && current.expiresAt() > now) {
            long next = current.count() == Long.MAX_VALUE ? Long.MAX_VALUE : current.count() + 1;
            windows.put(key, new Window(next, current.expiresAt()));
            return next;
        }
        windows.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        // Do not evict live counters: doing so would let new IPs reset the limiter.
        if (windows.size() >= MAX_KEYS || key.length() > 512) {
            throw new CsaException(ApiErrorCode.SERVICE_UNAVAILABLE, "Public rate limiter is busy; please retry");
        }
        windows.put(key, new Window(1, now + seconds * 1000L));
        return 1;
    }

    private record Window(long count, long expiresAt) {
    }
}
