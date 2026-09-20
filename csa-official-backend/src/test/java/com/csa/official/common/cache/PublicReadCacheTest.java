package com.csa.official.common.cache;

import com.csa.official.modules.sys.service.AuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PublicReadCacheTest {

    private static final TypeReference<String> STRING_TYPE = new TypeReference<>() {
    };

    @Test
    void positiveAndNegativeValuesAreCachedThroughTheSameReadPath() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        PublicReadCache cache = newCache(store, new PublicReadFallback());
        AtomicInteger positiveLoads = new AtomicInteger();
        AtomicInteger negativeLoads = new AtomicInteger();

        assertThat(cache.get("test", "csa:public:test:value:v2", null, STRING_TYPE, 60,
                () -> {
                    positiveLoads.incrementAndGet();
                    return "value";
                })).isEqualTo("value");
        assertThat(cache.get("test", "csa:public:test:value:v2", null, STRING_TYPE, 60,
                () -> {
                    positiveLoads.incrementAndGet();
                    return "other";
                })).isEqualTo("value");

        assertThat(cache.get("test", "csa:public:test:missing:v2", "csa:public:test:missing:null:v2",
                STRING_TYPE, 60, () -> {
                    negativeLoads.incrementAndGet();
                    return null;
                })).isNull();
        assertThat(cache.get("test", "csa:public:test:missing:v2", "csa:public:test:missing:null:v2",
                STRING_TYPE, 60, () -> {
                    negativeLoads.incrementAndGet();
                    return "unexpected";
                })).isNull();

        assertThat(positiveLoads).hasValue(1);
        assertThat(negativeLoads).hasValue(1);
    }

    @Test
    void oneHotKeyRebuildIsSharedByConcurrentReaders() throws Exception {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        PublicReadCache cache = newCache(store, new PublicReadFallback());
        AtomicInteger loads = new AtomicInteger();
        CountDownLatch loaderEntered = new CountDownLatch(1);
        CountDownLatch releaseLoader = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        java.util.List<Future<String>> futures = new java.util.ArrayList<>();

        try {
            for (int i = 0; i < 8; i++) {
                futures.add(executor.submit(() -> cache.get(
                        "competition",
                        "csa:public:competition:detail:99:v2",
                        "csa:public:competition:detail:null:99:v2",
                        STRING_TYPE,
                        60,
                        () -> {
                            loads.incrementAndGet();
                            loaderEntered.countDown();
                            await(releaseLoader);
                            return "hot-value";
                        })));
            }

            assertThat(loaderEntered.await(2, TimeUnit.SECONDS)).isTrue();
            releaseLoader.countDown();
            for (Future<String> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("hot-value");
            }
            assertThat(loads).hasValue(1);
        } finally {
            releaseLoader.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void commitAdvancesGeneration() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        PublicReadCache cache = newCache(store, new PublicReadFallback());
        AtomicInteger loads = new AtomicInteger();
        String key = "csa:public:test:commit:v2";

        assertThat(cache.get("test", key, null, STRING_TYPE, 60, () -> {
            loads.incrementAndGet();
            return "old";
        })).isEqualTo("old");

        cache.invalidateAfterCommit("test", "commit");

        assertThat(cache.get("test", key, null, STRING_TYPE, 60, () -> {
            loads.incrementAndGet();
            return "new";
        })).isEqualTo("new");
        assertThat(loads).hasValue(2);
    }

    @Test
    void rollbackDoesNotInvalidateTheCachedValue() {
        MemoryKeyValueStore store = new MemoryKeyValueStore();
        PublicReadCache cache = newCache(store, new PublicReadFallback());
        AtomicInteger loads = new AtomicInteger();
        String key = "csa:public:test:rollback:v2";

        assertThat(cache.get("test", key, null, STRING_TYPE, 60, () -> {
            loads.incrementAndGet();
            return "before-rollback";
        })).isEqualTo("before-rollback");

        TransactionSynchronizationManager.initSynchronization();
        cache.invalidateAfterCommit("test", "rollback");
        TransactionSynchronizationManager.getSynchronizations().get(0)
                .afterCompletion(org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK);
        TransactionSynchronizationManager.clearSynchronization();

        assertThat(cache.get("test", key, null, STRING_TYPE, 60, () -> "unexpected"))
                .isEqualTo("before-rollback");
        assertThat(loads).hasValue(1);
    }

    private PublicReadCache newCache(KeyValueStore store, PublicReadFallback fallback) {
        AuditService audit = mock(AuditService.class);
        CacheTtlPolicy policy = new CacheTtlPolicy(new Random(11));
        return new PublicReadCache(
                store,
                new CacheValueCodec(new ObjectMapper().findAndRegisterModules()),
                policy,
                fallback,
                audit);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("test loader was not released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("test loader interrupted", e);
        }
    }
}
