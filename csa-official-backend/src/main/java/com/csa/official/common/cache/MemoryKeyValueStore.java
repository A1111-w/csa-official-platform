package com.csa.official.common.cache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "csa.cache", name = "type", havingValue = "memory", matchIfMissing = true)
public class MemoryKeyValueStore implements KeyValueStore {

    private static final long NO_EXPIRATION = Long.MAX_VALUE;

    private final ConcurrentHashMap<String, CacheEntry> store = new ConcurrentHashMap<>();

    @Override
    public boolean hasKey(String key) {
        return getEntry(key) != null;
    }

    @Override
    public void setString(String key, String value, long timeout, TimeUnit unit) {
        store.put(key, new CacheEntry(value, expiresAt(timeout, unit)));
    }

    @Override
    public boolean setIfAbsent(String key, String value, long timeout, TimeUnit unit) {
        CacheEntry newEntry = new CacheEntry(value, expiresAt(timeout, unit));
        CacheEntry result = store.compute(key, (currentKey, currentValue) ->
                currentValue == null || isExpired(currentValue) ? newEntry : currentValue);
        return result == newEntry;
    }

    @Override
    public String getString(String key) {
        CacheEntry entry = getEntry(key);
        return entry == null ? null : entry.value();
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    @Override
    public boolean deleteIfValue(String key, String value) {
        CacheEntry current = getEntry(key);
        return current != null && current.value().equals(value) && store.remove(key, current);
    }

    @Override
    public Long increment(String key) {
        CacheEntry entry = store.compute(key, (currentKey, currentValue) -> {
            if (currentValue == null || isExpired(currentValue)) {
                return new CacheEntry("1", NO_EXPIRATION);
            }

            long nextValue = asLong(currentValue.value()) + 1;
            return new CacheEntry(Long.toString(nextValue), currentValue.expiresAt());
        });

        return entry == null ? null : asLong(entry.value());
    }

    @Override
    public Long increment(String key, long timeout, TimeUnit unit) {
        CacheEntry entry = store.compute(key, (currentKey, currentValue) -> {
            if (currentValue == null || isExpired(currentValue)) {
                return new CacheEntry("1", expiresAt(timeout, unit));
            }

            long nextValue = asLong(currentValue.value()) + 1;
            return new CacheEntry(Long.toString(nextValue), currentValue.expiresAt());
        });

        return entry == null ? null : asLong(entry.value());
    }

    @Override
    public void expire(String key, long timeout, TimeUnit unit) {
        store.computeIfPresent(key, (currentKey, currentValue) -> {
            if (isExpired(currentValue)) {
                return null;
            }
            return new CacheEntry(currentValue.value(), expiresAt(timeout, unit));
        });
    }

    private CacheEntry getEntry(String key) {
        CacheEntry entry = store.get(key);
        if (isExpired(entry)) {
            store.remove(key, entry);
            return null;
        }
        return entry;
    }

    private boolean isExpired(CacheEntry entry) {
        return entry != null
                && entry.expiresAt() != NO_EXPIRATION
                && System.currentTimeMillis() > entry.expiresAt();
    }

    private long expiresAt(long timeout, TimeUnit unit) {
        long ttlMillis = unit.toMillis(timeout);
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        return expiresAt < 0 ? NO_EXPIRATION : expiresAt;
    }

    private long asLong(String value) {
        if (value != null) {
            return Long.parseLong(value);
        }
        throw new IllegalStateException("Stored value is not numeric");
    }

    private record CacheEntry(String value, long expiresAt) {
    }
}
