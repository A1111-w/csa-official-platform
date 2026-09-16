package com.csa.official.common.cache;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Random;

/**
 * Centralized TTL policy with bounded jitter to avoid synchronized expiry.
 */
@Component
public class CacheTtlPolicy {

    private final Random random;

    public CacheTtlPolicy() {
        this(new Random());
    }

    public CacheTtlPolicy(Random random) {
        this.random = Objects.requireNonNull(random);
    }

    public long seconds(long baseSeconds, int jitterSeconds) {
        if (baseSeconds <= 0 || jitterSeconds < 0) {
            throw new IllegalArgumentException("Cache TTL must be positive and jitter cannot be negative");
        }
        return baseSeconds + (jitterSeconds == 0 ? 0 : random.nextInt(jitterSeconds + 1));
    }

    public long competitionListSeconds() {
        return seconds(60, 30);
    }

    public long competitionDetailSeconds() {
        return seconds(120, 60);
    }

    public long competitionNullSeconds() {
        return seconds(60, 60);
    }
}
