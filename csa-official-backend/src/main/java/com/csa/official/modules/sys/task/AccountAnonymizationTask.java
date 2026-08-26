package com.csa.official.modules.sys.task;

import com.csa.official.modules.sys.service.AccountAnonymizationService;
import com.csa.official.modules.sys.service.ScheduledJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class AccountAnonymizationTask {

    private static final String JOB_NAME = "anonymize-expired-accounts";

    private final AccountAnonymizationService anonymizationService;
    private final ScheduledJobService scheduledJobService;
    private final int retentionDays;
    private final int batchSize;

    public AccountAnonymizationTask(
            AccountAnonymizationService anonymizationService,
            ScheduledJobService scheduledJobService,
            @Value("${csa.privacy.account-retention-days:30}") int retentionDays,
            @Value("${csa.privacy.anonymization-batch-size:200}") int batchSize) {
        this.anonymizationService = anonymizationService;
        this.scheduledJobService = scheduledJobService;
        this.retentionDays = Math.max(1, retentionDays);
        this.batchSize = Math.max(1, Math.min(batchSize, 1000));
    }

    @Scheduled(cron = "${csa.privacy.anonymization-cron:0 15 3 * * ?}")
    public void anonymizeExpiredAccounts() {
        LocalDateTime now = LocalDateTime.now();
        scheduledJobService.runOnce(JOB_NAME, now.toLocalDate().toString(), () -> {
            int count = anonymizationService.anonymizeExpired(
                    now.minusDays(retentionDays), batchSize, retentionDays);
            log.info("Expired account anonymization completed: count={}", count);
        });
    }
}
