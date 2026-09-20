package com.csa.official.common.cache;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Process-local public data only. Never stores authentication or session state.
 */
@Component
public class PublicReadFallback {
    static final long TTL_MILLIS = 2_000;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicLong epoch = new AtomicLong();
    private final LongSupplier clock;

    public PublicReadFallback() {
        this(System::currentTimeMillis);
    }

    PublicReadFallback(LongSupplier clock) {
        this.clock = clock;
    }

    public String load(String key, Supplier<String> loader) {
        long generation = epoch.get();
        String cacheKey = generation + ":" + key;
        Entry entry = entries.get(cacheKey);
        long now = clock.getAsLong();
        if (entry != null && entry.expiresAt() > now) {
            return entry.value();
        }

        String loaded = loader.get();
        if (loaded != null && generation == epoch.get()) {
            entries.put(cacheKey, new Entry(loaded, now + TTL_MILLIS));
        }
        return loaded;
    }

    public void invalidate() {
        epoch.incrementAndGet();
        entries.clear();
    }

    private record Entry(String value, long expiresAt) {
    }
}
