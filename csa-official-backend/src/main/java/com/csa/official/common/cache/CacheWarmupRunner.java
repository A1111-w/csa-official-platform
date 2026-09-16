package com.csa.official.common.cache;

import com.csa.official.modules.biz.service.CompetitionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Warms only the fixed public hotspot that is cheap and safe to rebuild.
 * A warmup failure must never prevent the application from becoming ready.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "csa.cache.warmup.enabled", havingValue = "true", matchIfMissing = true)
public class CacheWarmupRunner {

    private final CompetitionService competitionService;

    public CacheWarmupRunner(CompetitionService competitionService) {
        this.competitionService = competitionService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        try {
            competitionService.getPublicCompetitionPage(1, 10);
            log.info("Public competition cache warmup completed");
        } catch (RuntimeException e) {
            log.warn("Public cache warmup failed; application remains available for cache-aside reads", e);
        }
    }
}
