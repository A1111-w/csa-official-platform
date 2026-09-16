package com.csa.official.common.cache;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CacheTtlPolicyTest {

    @Test
    void jitterStaysWithinConfiguredInclusiveRange() {
        CacheTtlPolicy policy = new CacheTtlPolicy(new Random(7));

        long ttl = policy.seconds(60, 30);

        assertThat(ttl).isBetween(60L, 90L);
    }

    @Test
    void rejectsInvalidTtlConfiguration() {
        CacheTtlPolicy policy = new CacheTtlPolicy(new Random(7));

        assertThatThrownBy(() -> policy.seconds(0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> policy.seconds(60, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
