package com.csa.official.common.cache;

import com.csa.official.common.exception.ApiErrorCode;
import com.csa.official.common.exception.CsaException;
import com.csa.official.modules.sys.service.AuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class PublicReadCache {
    private final KeyValueStore store;
    private final CacheValueCodec codec;
    private final CacheTtlPolicy ttl;
    private final PublicReadFallback fallback;
    private final AuditService audit;
    private long lastWarningAt;

    public PublicReadCache(KeyValueStore store, CacheValueCodec codec, CacheTtlPolicy ttl,
                           PublicReadFallback fallback, AuditService audit) {
        this.store = store;
        this.codec = codec;
        this.ttl = ttl;
        this.fallback = fallback;
        this.audit = audit;
    }

    public <T> T get(String scope, String key, String nullKey, TypeReference<T> type,
                     long ttlSeconds, Supplier<T> loader) {
        String generation;
        try {
            generation = generation(scope);
        } catch (RuntimeException e) {
            warn(e);
            return decode(fallback.load(key, () -> encode(loader.get())), type);
        }
        String dataKey = key + ":g" + generation;
        String missingKey = nullKey == null ? null : nullKey + ":g" + generation;
        String lockKey = "csa:lock:" + dataKey;
        String token = UUID.randomUUID().toString();
        boolean acquired = false;
        try {
            // This block only catches cache failures, never a database loader exception.
            try {
                String value = read(dataKey, missingKey);
                if (value != null) {
                    return decode(value, type);
                }
                acquired = store.setIfAbsent(lockKey, token, 4, TimeUnit.SECONDS);
                for (int attempt = 0; !acquired && attempt < 6; attempt++) {
                    pause();
                    value = read(dataKey, missingKey);
                    if (value != null) {
                        return decode(value, type);
                    }
                }
                if (!acquired) {
                    throw new RebuildBusyException();
                }
                value = read(dataKey, missingKey);
                if (value != null) {
                    return decode(value, type);
                }
            } catch (RebuildBusyException e) {
                throw new CsaException(ApiErrorCode.SERVICE_UNAVAILABLE, "Public cache is rebuilding; please retry");
            } catch (RuntimeException e) {
                if (Thread.currentThread().isInterrupted()) {
                    throw e;
                }
                warn(e);
                return decode(fallback.load(dataKey, () -> encode(loader.get())), type);
            }

            String loaded = fallback.load(dataKey, () -> encode(loader.get()));
            try {
                boolean missing = CacheValueCodec.NULL_MARKER.equals(loaded);
                store.setString(missing && missingKey != null ? missingKey : dataKey,
                        loaded, missing ? ttl.competitionNullSeconds() : ttlSeconds, TimeUnit.SECONDS);
            } catch (RuntimeException e) {
                warn(e);
            }
            return decode(loaded, type);
        } finally {
            if (acquired) {
                try {
                    store.deleteIfValue(lockKey, token);
                } catch (RuntimeException e) {
                    warn(e);
                }
            }
        }
    }

    public void invalidateAfterCommit(String scope, String targetId) {
        Runnable invalidation = () -> invalidate(scope, targetId);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invalidation.run();
                }
            });
        } else {
            invalidation.run();
        }
    }

    private void invalidate(String scope, String targetId) {
        fallback.invalidate();
        String result = "SUCCESS";
        try {
            // A random generation prevents resurrection if the generation key is evicted.
            store.setString(CacheKeys.generation(scope), UUID.randomUUID().toString(), 365, TimeUnit.DAYS);
        } catch (RuntimeException e) {
            result = "FAILED";
            log.warn("CACHE_INVALIDATION failed: scope={}, target={}; stale entries expire by TTL", scope, targetId, e);
        }
        try {
            audit.recordCacheInvalidation(scope, targetId, result,
                    Map.of("cacheVersion", CacheKeys.VERSION, "scope", scope));
        } catch (RuntimeException e) {
            log.error("CACHE_INVALIDATION audit failed: scope={}, result={}", scope, result, e);
        }
    }

    private String generation(String scope) {
        String key = CacheKeys.generation(scope);
        String value = store.getString(key);
        if (value != null) {
            return value;
        }
        String candidate = UUID.randomUUID().toString();
        if (store.setIfAbsent(key, candidate, 365, TimeUnit.DAYS)) {
            return candidate;
        }
        value = store.getString(key);
        if (value == null) {
            throw new IllegalStateException("Cache generation changed during initialization");
        }
        return value;
    }

    private String read(String key, String nullKey) {
        String value = store.getString(key);
        if (value != null) {
            return value;
        }
        return nullKey != null && CacheValueCodec.NULL_MARKER.equals(store.getString(nullKey))
                ? CacheValueCodec.NULL_MARKER : null;
    }

    private String encode(Object value) {
        return value == null ? CacheValueCodec.NULL_MARKER : codec.write(value);
    }

    private <T> T decode(String value, TypeReference<T> type) {
        return CacheValueCodec.NULL_MARKER.equals(value) ? null : codec.read(value, type);
    }

    private void pause() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CsaException(ApiErrorCode.SERVICE_UNAVAILABLE, "Public cache wait interrupted");
        }
    }

    private synchronized void warn(RuntimeException e) {
        long now = System.currentTimeMillis();
        if (now - lastWarningAt >= 60_000) {
            lastWarningAt = now;
            log.warn("Public cache unavailable; using short process-local fallback", e);
        }
    }

    private static class RebuildBusyException extends RuntimeException {
    }
}
