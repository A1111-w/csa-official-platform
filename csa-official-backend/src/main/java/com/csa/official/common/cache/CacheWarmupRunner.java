package com.csa.official.common.cache;

import com.csa.official.modules.biz.service.CompetitionService;
import com.csa.official.modules.sys.service.PublicAboutService;
import com.csa.official.modules.sys.service.CarouselService;
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
    private final PublicAboutService aboutService;
    private final CarouselService carouselService;

    public CacheWarmupRunner(CompetitionService competitionService, PublicAboutService aboutService,
                            CarouselService carouselService) {
        this.competitionService = competitionService;
        this.aboutService = aboutService;
        this.carouselService = carouselService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void warmup() {
        warm("about", () -> aboutService.getAbout());
        warm("carousel", () -> carouselService.getPublicList());
        warm("competition", () -> competitionService.getPublicCompetitionPage(1, 10));
    }

    private void warm(String scope, Runnable loader) {
        try {
            loader.run();
            log.info("Public {} cache warmup read completed", scope);
        } catch (RuntimeException e) {
            log.warn("Public {} warmup failed; continuing with remaining scopes", scope, e);
        }
    }
}
