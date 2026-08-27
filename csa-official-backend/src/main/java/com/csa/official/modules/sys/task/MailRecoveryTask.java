package com.csa.official.modules.sys.task;

import com.csa.official.modules.sys.service.MailRecoveryService;
import com.csa.official.modules.sys.service.ScheduledJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class MailRecoveryTask {

    private static final String JOB_NAME = "recover-stale-mail";

    private final MailRecoveryService mailRecoveryService;
    private final ScheduledJobService scheduledJobService;
    private final int batchSize;
    private final long staleSeconds;

    public MailRecoveryTask(MailRecoveryService mailRecoveryService,
                            ScheduledJobService scheduledJobService,
                            @Value("${csa.mail.recovery-batch-size:100}") int batchSize,
                            @Value("${csa.mail.recovery-stale-seconds:90}") long staleSeconds) {
        this.mailRecoveryService = mailRecoveryService;
        this.scheduledJobService = scheduledJobService;
        this.batchSize = Math.max(1, Math.min(batchSize, 500));
        this.staleSeconds = Math.max(30, staleSeconds);
    }

    @Scheduled(cron = "${csa.mail.recovery-cron:0 * * * * ?}")
    public void recoverStaleMail() {
        LocalDateTime now = LocalDateTime.now();
        scheduledJobService.runWithLock(JOB_NAME, () -> {
            int count = mailRecoveryService.recoverStale(now.minusSeconds(staleSeconds), batchSize);
            log.info("Stale mail recovery completed: dispatched={}", count);
        });
    }
}
